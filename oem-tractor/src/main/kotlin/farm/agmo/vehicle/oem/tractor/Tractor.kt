// Tractor.kt — AGMO Customized Tractor 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 상태 읽기 (자격 불필요) — 생명주기 한 줄
//   val tractor = Tractor(this, object : Tractor.Listener {
//       override fun onFnr(f: Fnr)                = runOnUiThread { render(f.direction) }
//       override fun onRangeShift(r: RangeShift)  = runOnUiThread { render(r.gear) }
//       override fun onPto(p: Pto)                = runOnUiThread { render(p.mode) }
//   })
//   lifecycle.addObserver(tractor)
//
//   // 제어 (매니페스트에 USES_CONTROL 선언 필요) — 서브시스템별 세션
//   val fnr = Tractor.fnrControl(this) ?: return
//   fnr.set(FnrDirection.FORWARD)     // 수동 지령(mode byte=Manual 기본). false=세션 상실
//   fnr.release()
//   val hitch = Tractor.hitchControl(this) ?: return
//   hitch.setPosition(80.0)           // 유압 히치 0~100%
//
// 제어 프로토콜(문서/DBC 확정): FNR / RangeShift(SFT) / PTO / Hitch(HYD %) / Accelerator(%).
//   각 _Command는 value(byte0)+mode(byte1). SDK는 value 신호를 지령하며 mode는 데몬 기본
//   0=Manual(수동 오버라이드) — 앱 제어의 정상 의미. (auto 모드 활성은 이 setter 범위 밖.)
//
// ⚠️ 4WD: AGMO 트랙터 CAN 프로토콜에 대응 신호 없음(로컬 DBC·설계문서·NEVONEX 추출·SeamOS
//   카탈로그 4곳 전수검색 0건). 런처 FOURWD 토글은 CAN 미연결 — 제어 메서드를 두지 않는다.
// ⚠️ Auto Lift on Turn/Reverse: 별도 CAN 신호가 아니라 Hitch(setPosition)로 선회·후진 시
//   히치를 자동 상승시키는 앱 레벨 기능. 조향각(STEERANGLE)·FNR을 구독해 트리거하는
//   AutoLift 헬퍼로 노출(Tractor.autoLift). 자세한 로직은 AutoLift.kt 참조.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.tractor

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Tractor(context: Context, private val listener: Listener) : Signal(context) {

    /** 트랙터 상태 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onFnr(fnr: Fnr) {}
        fun onRangeShift(rangeShift: RangeShift) {}
        fun onPto(pto: Pto) {}
        fun onHydraulic(hydraulic: Hydraulic) {}
        fun onAccelerator(accelerator: Accelerator) {}
        fun onFnrDiag(fnrDiag: FnrDiag) {}
        fun onRangeShiftDiag(rangeShiftDiag: RangeShiftDiag) {}
        fun onHydraulicDiag(hydraulicDiag: HydraulicDiag) {}
        fun onAcceleratorDiag(acceleratorDiag: AcceleratorDiag) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(Fnr.KEYS)         { Fnr.from(it)?.let(listener::onFnr) },
        vehicle.subscribeMessage(RangeShift.KEYS)  { RangeShift.from(it)?.let(listener::onRangeShift) },
        vehicle.subscribeMessage(Pto.KEYS)         { Pto.from(it)?.let(listener::onPto) },
        vehicle.subscribeMessage(Hydraulic.KEYS)   { Hydraulic.from(it)?.let(listener::onHydraulic) },
        vehicle.subscribeMessage(Accelerator.KEYS) { Accelerator.from(it)?.let(listener::onAccelerator) },
        vehicle.subscribeMessage(FnrDiag.KEYS)         { FnrDiag.from(it)?.let(listener::onFnrDiag) },
        vehicle.subscribeMessage(RangeShiftDiag.KEYS)  { RangeShiftDiag.from(it)?.let(listener::onRangeShiftDiag) },
        vehicle.subscribeMessage(HydraulicDiag.KEYS)   { HydraulicDiag.from(it)?.let(listener::onHydraulicDiag) },
        vehicle.subscribeMessage(AcceleratorDiag.KEYS) { AcceleratorDiag.from(it)?.let(listener::onAcceleratorDiag) },
    )

    companion object {
        /** 전후진 제어권. set(FnrDirection). null=자격 없음/보유 중/미연결 */
        fun fnrControl(context: Context): FnrControl? =
            acquire(context, TractorControlKeys.FNR)?.let(::FnrControl)

        /** 변속 레인지 제어권. set(RangeGear) */
        fun rangeShiftControl(context: Context): RangeShiftControl? =
            acquire(context, TractorControlKeys.RANGE_SHIFT)?.let(::RangeShiftControl)

        /** PTO 제어권. set(PtoMode) */
        fun ptoControl(context: Context): PtoControl? =
            acquire(context, TractorControlKeys.PTO)?.let(::PtoControl)

        /** 유압 히치 제어권. setPosition(0~100%). AutoLift 앱 기능도 이걸 사용. */
        fun hitchControl(context: Context): HitchControl? =
            acquire(context, TractorControlKeys.HITCH)?.let(::HitchControl)

        /** 가속 제어권. setPercent(0~100%) */
        fun acceleratorControl(context: Context): AcceleratorControl? =
            acquire(context, TractorControlKeys.ACCELERATOR)?.let(::AcceleratorControl)

        /** AutoLift 앱 기능 세션 — 히치 제어권(AD_HYD_CMD)을 잡아 반환. null=자격없음/보유중/미연결 */
        fun autoLift(context: Context): AutoLift? =
            hitchControl(context)?.let { AutoLift(AgVehicle.shared(context), it) }

        private fun acquire(context: Context, key: String): AgVehicle.ControlSession? =
            AgVehicle.shared(context).acquire(key)
    }
}

/** 제어 세션 공통 골격 (토큰·선점은 core ControlSession이 관리) */
abstract class TractorControl internal constructor(
    protected val session: AgVehicle.ControlSession,
) {
    private var lostCb: (() -> Unit)? = null
    /** 상위 계층 선점 통지 — 즉시 UI 잠글 것 */
    fun onLost(cb: () -> Unit) { lostCb = cb }
    /** 정상 반납 */
    fun release() = session.release()
    /** 안전값 송신 후 반납(데몬 정의에 safe 센티넬이 있을 때) */
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }
    protected fun send(raw: Long): Boolean = session.send(raw)
}

/** 전후진 지령 — 수동(mode=Manual) 값 지령 */
class FnrControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    /** false = 세션 상실 — fnrControl부터 다시 */
    fun set(direction: FnrDirection): Boolean = send(direction.code.toLong())
}

/** 변속 레인지 지령 */
class RangeShiftControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    fun set(gear: RangeGear): Boolean = send(gear.code.toLong())
}

/** PTO 지령 */
class PtoControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    fun set(mode: PtoMode): Boolean = send(mode.code.toLong())
}

/** 유압 히치 위치 지령 (0~100%). AD_HYD_CMD가 0~100 직접값이라 raw=클램프된 정수%. */
class HitchControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    fun setPosition(percent: Double): Boolean = send(percent.toLong().coerceIn(0, 100))
}

/** 가속 지령 (0~100%) */
class AcceleratorControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    fun setPercent(percent: Double): Boolean = send(percent.toLong().coerceIn(0, 100))
}
