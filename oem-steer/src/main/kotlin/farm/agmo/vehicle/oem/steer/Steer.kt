// Steer.kt — AGMO 조향모터(Keya KY170) 도메인 (읽기: Signal 상속).
//
//   val steer = Steer(this, object : Steer.Listener {
//       override fun onMotorStatus(s: SteerMotorStatus) = runOnUiThread { if (s.fault) warn(s) }
//   })
//   lifecycle.addObserver(steer)
//
// ✅ 지원: 조향모터 상태/결함(Motor_Heartbeat) + 조향각/속도/온도/전압(Motor_Response SDO, mux).
// ⏸ 보류: 조향 명령 — Keya SDO 명령(Motor_Request 0x06000001)의 실제 인코딩(멀티플렉서 코드별
//   payload)은 정식 명령 레이아웃 확보 후 추가. 여기선 읽기만 노출.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.steer

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Steer(context: Context, private val listener: Listener) : Signal(context) {

    /** 조향모터 콜백 — 필요한 메시지만 override. SDO 값은 각기 다른 시점에 도착. */
    interface Listener {
        fun onMotorStatus(status: SteerMotorStatus) {}
        fun onAngle(angle: SteerAngle) {}
        fun onSpeed(speed: SteerSpeed) {}
        fun onTemperature(temperature: SteerTemperature) {}
        fun onVoltage(voltage: SteerVoltage) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(SteerMotorStatus.KEYS) { SteerMotorStatus.from(it)?.let(listener::onMotorStatus) },
        vehicle.subscribe(SteerAngle.KEY)       { it.number?.let { v -> listener.onAngle(SteerAngle(v)) } },
        vehicle.subscribe(SteerSpeed.KEY)       { it.number?.let { v -> listener.onSpeed(SteerSpeed(v)) } },
        vehicle.subscribe(SteerTemperature.KEY) { it.number?.let { v -> listener.onTemperature(SteerTemperature(v)) } },
        vehicle.subscribe(SteerVoltage.KEY)     { it.number?.let { v -> listener.onVoltage(SteerVoltage(v)) } },
    )
}
