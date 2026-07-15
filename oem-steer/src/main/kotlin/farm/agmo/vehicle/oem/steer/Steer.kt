// Steer.kt — AGMO 조향모터(Keya KY170) 도메인 (읽기: Signal 상속).
//
//   val steer = Steer(this, object : Steer.Listener {
//       override fun onMotorStatus(s: SteerMotorStatus) = runOnUiThread { if (s.fault) warn(s) }
//   })
//   lifecycle.addObserver(steer)
//
// ✅ 지원: 조향모터 상태/결함(Motor_Heartbeat).
// ⏸ 보류: 조향각 읽기 + 조향 명령 — Keya CANopen SDO 멀티플렉서라 agcand 평면 코덱으로
//   표현 불가(SteerModel 참조). 조향각/제어는 데몬 멀티플렉서 지원 또는 Keya 정식 피드백/
//   명령 레이아웃 확보 후 추가한다. 여기선 상태/결함만 노출.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.steer

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Steer(context: Context, private val listener: Listener) : Signal(context) {

    /** 조향모터 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onMotorStatus(status: SteerMotorStatus) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(SteerMotorStatus.KEYS) { SteerMotorStatus.from(it)?.let(listener::onMotorStatus) },
    )
}
