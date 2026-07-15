// Steer.kt — AGMO SteerMotor 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 조향각·모터 상태 읽기 (자격 불필요) — 생명주기 한 줄
//   val steer = Steer(this, object : Steer.Listener {
//       override fun onAngle(a: SteerAngle)          = runOnUiThread { render(a.deg) }
//       override fun onMotorStatus(s: SteerMotorStatus) = runOnUiThread { if (s.fault) warn() }
//   })
//   lifecycle.addObserver(steer)
//
//   // 조향 명령 (자율주행/보조조향) — 매니페스트에 USES_CONTROL 선언 필요
//   val ctrl = Steer.control(this) ?: return
//   ctrl.setAngle(12.5)      // deg — raw 변환은 (B) 확정 후 SDK가 숨김
//   ctrl.release()
//
// ⚠️ 골격 단계(C): 키/스케일/제어 프레임 모두 플레이스홀더(TODO(B)). SteerMotor는
//   CANopen류라 명령 프레임(0x600 SDO 등) 매핑이 깨끗한 소스 확정 후 필요하다.
package farm.agmo.vehicle.steer

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Steer(context: Context, private val listener: Listener) : Signal(context) {

    /** 조향 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onAngle(angle: SteerAngle) {}
        fun onMotorStatus(status: SteerMotorStatus) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribe(SteerAngle.KEY) { it.number?.let { v -> listener.onAngle(SteerAngle(v)) } },
        vehicle.subscribeMessage(SteerMotorStatus.KEYS) { SteerMotorStatus.from(it)?.let(listener::onMotorStatus) },
    )

    companion object {
        // 조향 명령 제어 키 — SDK 호환 위해 placeholder 이름 유지, 내부 프레임은 (B) 매핑.
        const val CONTROL_KEY = "STEER_CMD"   // TODO(B): 데몬 제어 신호 키 확정

        /** 조향 제어권. null = 자격 없음/보유 중/미연결 */
        fun control(context: Context): SteerControl? {
            val v = AgVehicle.shared(context)
            var handle: SteerControl? = null
            val session = v.acquire(CONTROL_KEY) { handle?.fireLost() } ?: return null
            return SteerControl(session).also { handle = it }
        }
    }
}

/** 조향각 명령 — HitchControl과 같은 세션 구조 */
class SteerControl internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    /** 목표 조향각 지시 (deg). false = 세션 상실 */
    fun setAngle(deg: Double): Boolean = session.send(toRaw(deg))

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }

    // debt: 조향각→raw 변환이 플레이스홀더(항등·반올림)다. 트리거: (B) 깨끗한 소스로
    //   실 CANopen 스케일/오프셋 확정 시 교체.
    private fun toRaw(deg: Double): Long = Math.round(deg)   // TODO(B): 실제 스케일 적용
}
