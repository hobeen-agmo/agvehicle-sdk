// Spreader.kt — AGMO RDA Spreader(살포기) 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
// GNSS 읽기 — 두 가지 사용법 (원하는 걸 고르면 됨):
//
//   // (1) 람다 — 필요한 신호만 짧게. object/override 불필요. (자격 불필요)
//   val spreader = Spreader(this, intervalMs = 200)
//   spreader.onPosition { p -> runOnUiThread { render(p.latitudeDeg, p.longitudeDeg) } }
//   lifecycle.addObserver(spreader)
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val spreader = Spreader(this, object : Spreader.Listener {
//       override fun onPosition(p: GnssPosition) = runOnUiThread { render(p.latitudeDeg, p.longitudeDeg) }
//       override fun onTimeStatus(t: GnssTimeStatus) = runOnUiThread { render(t.fix) }
//   })
//
//   // 살포 모터 제어 (매니페스트에 USES_CONTROL 선언 필요)
//   val motor = Spreader.motorControl(this) ?: return
//   motor.set(DriveMode.SPREADING, enable = true)   // 모드+enable 원자적 송신
//   motor.set(DriveMode.STOP, enable = false)
//   motor.release()
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.spreader

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Spreader(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** 살포기 GNSS 콜백 — 필요한 메시지만 override (리스너 방식) */
    interface Listener {
        fun onPosition(position: GnssPosition) {}
        fun onTimeStatus(timeStatus: GnssTimeStatus) {}
    }

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. 내부적으로 람다 등록과 동일 경로. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onPosition(listener::onPosition); onTimeStatus(listener::onTimeStatus)
    }

    // ── 람다 편의 API — 필요한 신호만 등록(체이닝 가능). ON_START에 실제 구독된다. ──
    fun onPosition(cb: (GnssPosition) -> Unit) = msg(GnssPosition.KEYS, GnssPosition::from, cb)
    fun onTimeStatus(cb: (GnssTimeStatus) -> Unit) = msg(GnssTimeStatus.KEYS, GnssTimeStatus::from, cb)

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 메시지(신호 여럿) 구독 등록 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }

    companion object {
        /** 살포 모터 제어권. null = 자격 없음/보유 중/미연결 */
        fun motorControl(context: Context): SpreaderMotorControl? {
            val v = AgVehicle.shared(context)
            var handle: SpreaderMotorControl? = null
            val session = v.acquire(SpreaderControlKeys.MOTOR) { handle?.fireLost() } ?: return null
            return SpreaderMotorControl(session).also { handle = it }
        }
    }
}

/** 살포 모터 제어 — 모드(Manual/Spreading/Stop) + enable를 한 프레임에 함께 지령 */
class SpreaderMotorControl internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    /** 모드+enable 지령. false = 세션 상실 — motorControl부터 다시 */
    fun set(mode: DriveMode, enable: Boolean): Boolean =
        session.send(SpreaderControlKeys.encode(mode, enable))

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }
}
