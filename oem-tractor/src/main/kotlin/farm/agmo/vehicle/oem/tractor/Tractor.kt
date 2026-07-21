// Tractor.kt — AGMO Customized Tractor 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 상태 읽기 (자격 불필요) — 생명주기 한 줄
//
//   // (1) 람다 — 필요한 신호만 짧게. object/override 불필요.
//   val tractor = Tractor(this, intervalMs = 200)
//   tractor.onFnr { f -> runOnUiThread { render(f.direction) } }
//   tractor.onRangeShift { r -> runOnUiThread { render(r.gear) } }
//   lifecycle.addObserver(tractor)
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val tractor = Tractor(this, object : Tractor.Listener {
//       override fun onFnr(f: Fnr)                = runOnUiThread { render(f.direction) }
//       override fun onRangeShift(r: RangeShift)  = runOnUiThread { render(r.gear) }
//       override fun onPto(p: Pto)                = runOnUiThread { render(p.mode) }
//   }, intervalMs = 200)
//   lifecycle.addObserver(tractor)
//
// intervalMs: 앱 콜백 최소 간격(ms, 0=전부 전달). 자격 선언 불필요(읽기).
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
import kotlin.math.roundToLong

class Tractor(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

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
        /** 신호 끊김(quality=DISCONNECTED) 알림 — key는 데몬 신호명. 필요하면 override */
        fun onStale(key: String) {}
    }

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. 내부적으로 람다 등록과 동일 경로. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onFnr(listener::onFnr); onRangeShift(listener::onRangeShift); onPto(listener::onPto)
        onHydraulic(listener::onHydraulic); onAccelerator(listener::onAccelerator)
        onFnrDiag(listener::onFnrDiag); onRangeShiftDiag(listener::onRangeShiftDiag)
        onHydraulicDiag(listener::onHydraulicDiag); onAcceleratorDiag(listener::onAcceleratorDiag)
        onStale(listener::onStale)
    }

    // ── 람다 편의 API — 필요한 신호만 등록(체이닝 가능). ON_START에 실제 구독된다. ──
    fun onFnr(cb: (Fnr) -> Unit) = msg(Fnr.KEYS, Fnr::from, cb)
    fun onRangeShift(cb: (RangeShift) -> Unit) = msg(RangeShift.KEYS, RangeShift::from, cb)
    fun onPto(cb: (Pto) -> Unit) = msg(Pto.KEYS, Pto::from, cb)
    fun onHydraulic(cb: (Hydraulic) -> Unit) = msg(Hydraulic.KEYS, Hydraulic::from, cb)
    fun onAccelerator(cb: (Accelerator) -> Unit) = msg(Accelerator.KEYS, Accelerator::from, cb)
    fun onFnrDiag(cb: (FnrDiag) -> Unit) = msg(FnrDiag.KEYS, FnrDiag::from, cb)
    fun onRangeShiftDiag(cb: (RangeShiftDiag) -> Unit) = msg(RangeShiftDiag.KEYS, RangeShiftDiag::from, cb)
    fun onHydraulicDiag(cb: (HydraulicDiag) -> Unit) = msg(HydraulicDiag.KEYS, HydraulicDiag::from, cb)
    fun onAcceleratorDiag(cb: (AcceleratorDiag) -> Unit) = msg(AcceleratorDiag.KEYS, AcceleratorDiag::from, cb)

    /** 신호 끊김(quality=DISCONNECTED) 알림 등록 — STALE_KEYS(읽기 메시지 전체) 각각에 배선. */
    fun onStale(cb: (String) -> Unit) = apply {
        STALE_KEYS.forEach { key -> regs += { vehicle.onStale(key) { cb(key) } } }
    }

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 메시지(신호 여럿) 구독 등록 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }

    companion object {
        /** 스테일 감시 대상 신호 키(읽기 메시지 전체) — 테스트 대상 */
        internal val STALE_KEYS: List<String> = (
            Fnr.KEYS + RangeShift.KEYS + Pto.KEYS + Hydraulic.KEYS + Accelerator.KEYS +
                FnrDiag.KEYS + RangeShiftDiag.KEYS + HydraulicDiag.KEYS + AcceleratorDiag.KEYS
            ).distinct()

        /** 전후진 제어권. set(FnrDirection). null=자격 없음/보유 중/미연결 */
        fun fnrControl(context: Context): FnrControl? =
            acquire(context, TractorControlKeys.FNR, ::FnrControl)

        /** 변속 레인지 제어권. set(RangeGear) */
        fun rangeShiftControl(context: Context): RangeShiftControl? =
            acquire(context, TractorControlKeys.RANGE_SHIFT, ::RangeShiftControl)

        /** PTO 제어권. set(PtoMode) */
        fun ptoControl(context: Context): PtoControl? =
            acquire(context, TractorControlKeys.PTO, ::PtoControl)

        /** 유압 히치 제어권. setPosition(0~100%). AutoLift 앱 기능도 이걸 사용. */
        fun hitchControl(context: Context): HitchControl? =
            acquire(context, TractorControlKeys.HITCH, ::HitchControl)

        /** 가속 제어권. setPercent(0~100%) */
        fun acceleratorControl(context: Context): AcceleratorControl? =
            acquire(context, TractorControlKeys.ACCELERATOR, ::AcceleratorControl)

        /** AutoLift 앱 기능 세션 — 히치 제어권(AD_HYD_CMD)을 잡아 반환. null=자격없음/보유중/미연결 */
        fun autoLift(context: Context): AutoLift? =
            hitchControl(context)?.let { AutoLift(AgVehicle.shared(context), it) }

        /**
         * 제어권 획득 + 선점 통지 배선. handle을 먼저 null로 잡고 acquire의 onControlLost
         * 람다가 그 handle(생성 후 채워짐)의 fireLost()를 부르게 해 상위 선점 시 앱에 통지한다
         * (Hitch/Gpio/Spreader와 같은 handle 패턴 — Tractor는 제어 타입이 5개라 제네릭으로 공통화).
         */
        private fun <T : TractorControl> acquire(context: Context, key: String, factory: (AgVehicle.ControlSession) -> T): T? {
            val v = AgVehicle.shared(context)
            var handle: T? = null
            val session = v.acquire(key) { handle?.fireLost() } ?: return null
            return factory(session).also { handle = it }
        }
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

/** 0~100% 직접값 → raw 클램프 변환 (Hitch/Accelerator 공통, 절단 대신 반올림) — 테스트 대상 */
internal fun percentToRaw(percent: Double): Long = percent.roundToLong().coerceIn(0, 100)

/** 유압 히치 위치 지령 (0~100%). AD_HYD_CMD가 0~100 직접값이라 raw=클램프된 정수%. */
class HitchControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    fun setPosition(percent: Double): Boolean = send(percentToRaw(percent))
}

/** 가속 지령 (0~100%) */
class AcceleratorControl internal constructor(s: AgVehicle.ControlSession) : TractorControl(s) {
    fun setPercent(percent: Double): Boolean = send(percentToRaw(percent))
}
