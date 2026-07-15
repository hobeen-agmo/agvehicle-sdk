// Tractor.kt — AGMO Customized Tractor 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 상태 읽기 (자격 불필요) — 생명주기 한 줄
//   val tractor = Tractor(this, object : Tractor.Listener {
//       override fun onFnr(f: Fnr)          = runOnUiThread { render(f.direction) }
//       override fun onShift(s: Shift)      = runOnUiThread { render(s.gear) }
//       override fun onHydraulic(h: Hydraulic) = runOnUiThread { render(h.pressure) }
//   })
//   lifecycle.addObserver(tractor)          // ON_START 자동 구독 / ON_STOP 자동 해제
//
//   // 제어 (매니페스트에 USES_CONTROL 선언 필요) — 읽기와 성격이 달라 세션 핸들로
//   val fourwd = Tractor.fourWd(this) ?: return   // null = 자격 없음/보유 중/미연결
//   fourwd.set(true)                              // 4WD ON
//   fourwd.release()
//
// ⚠️ 골격 단계(C): 읽기 키는 플레이스홀더(TractorModel의 TODO(B)). 제어 키는 데몬의
//   기존 placeholder 제어 신호(FOURWD_CMD 등, 0xEF01~)를 그대로 쓴다 — 이 키 이름은
//   SDK 호환을 위해 유지하고, 내부 PGN/raw는 (B)에서 실 제조사 프레임으로 매핑된다.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 공개 J1939/ISO 표준 도메인이 아니다. 표준 모듈
//   (engine/hitch/imu/vehicle)과 섞지 않으며 oem 네임스페이스로 분리된다. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.tractor

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Tractor(context: Context, private val listener: Listener) : Signal(context) {

    /** 트랙터 상태 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onFnr(fnr: Fnr) {}
        fun onShift(shift: Shift) {}
        fun onPto(pto: PtoState) {}
        fun onHydraulic(hydraulic: Hydraulic) {}
        fun onAcc(acc: Acc) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribe(Fnr.KEY)        { it.number?.let { v -> Fnr.from(mapOf(Fnr.KEY to v))?.let(listener::onFnr) } },
        vehicle.subscribe(Shift.KEY)      { it.number?.let { v -> listener.onShift(Shift(v.toInt())) } },
        vehicle.subscribeMessage(PtoState.KEYS) { PtoState.from(it)?.let(listener::onPto) },
        vehicle.subscribe(Hydraulic.KEY)  { it.number?.let { v -> listener.onHydraulic(Hydraulic(v)) } },
        vehicle.subscribe(Acc.KEY)        { it.number?.let { v -> listener.onAcc(Acc(v)) } },
    )

    companion object {
        // 데몬 내장 제어 신호 key — SDK 호환 위해 기존 이름 유지(내부 PGN/raw는 (B) 매핑).
        const val FOURWD_KEY = "FOURWD_CMD"
        const val AUTOLIFT_TURN_KEY = "AUTOLIFT_TURN_CMD"
        const val AUTOLIFT_REV_KEY = "AUTOLIFT_REV_CMD"
        const val HITCH_KEY = "HITCH_CMD"

        /** 4WD 토글 제어권. set(true/false). null = 자격 없음/보유 중/미연결 */
        fun fourWd(context: Context): TractorToggle? = TractorToggle.acquire(context, FOURWD_KEY)

        /** 선회 시 자동 상승 토글 */
        fun autoLiftOnTurn(context: Context): TractorToggle? = TractorToggle.acquire(context, AUTOLIFT_TURN_KEY)

        /** 후진 시 자동 상승 토글 */
        fun autoLiftOnReverse(context: Context): TractorToggle? = TractorToggle.acquire(context, AUTOLIFT_REV_KEY)

        /** 히치 제어권 — 위치(%) 지시 */
        fun hitch(context: Context): TractorHitchControl? {
            val v = AgVehicle.shared(context)
            var handle: TractorHitchControl? = null
            val session = v.acquire(HITCH_KEY) { handle?.fireLost() } ?: return null
            return TractorHitchControl(session).also { handle = it }
        }
    }
}

/** ON/OFF 제어 토글 (히치 제어와 같은 세션 구조 — 토큰·선점). 4WD·AutoLift에 사용 */
class TractorToggle internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    /** 켜기(true)/끄기(false). false = 세션 상실 — 다시 acquire */
    fun set(on: Boolean): Boolean = session.send(if (on) 1L else 0L)

    /** 상위 계층 선점 통지 — 즉시 UI 잠글 것 */
    fun onLost(cb: () -> Unit) { lostCb = cb }

    /** 반납 (마지막 상태 유지) */
    fun release() = session.release()

    /** 안전값(off)으로 되돌린 뒤 반납 */
    fun stopAndRelease() = session.stopAndRelease()

    internal fun fireLost() { lostCb?.invoke() }

    companion object {
        internal fun acquire(context: Context, key: String): TractorToggle? {
            val v = AgVehicle.shared(context)
            var handle: TractorToggle? = null
            val session = v.acquire(key) { handle?.fireLost() } ?: return null
            return TractorToggle(session).also { handle = it }
        }
    }
}

/** 히치 위치(%) 제어 — HitchControl과 같은 구조 */
class TractorHitchControl internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    /** 히치 위치 지시 (0~100%). false = 세션 상실 */
    fun setPosition(percent: Double): Boolean = session.send(toRaw(percent))

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }

    // debt: 위치%→raw 변환이 플레이스홀더(항등·클램프만)다. 트리거: (B) 깨끗한 소스로
    //   실 resolution/RAW_MAX 확정 시 hitch 모듈의 HitchScale처럼 교체.
    private fun toRaw(percent: Double): Long =
        percent.toLong().coerceIn(0, 100)   // TODO(B): 실제 스케일 적용
}
