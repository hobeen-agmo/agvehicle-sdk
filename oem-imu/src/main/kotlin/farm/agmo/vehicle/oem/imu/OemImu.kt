// OemImu.kt — AGMO 제조사 고유 자이로 읽기 도메인 (Signal 상속, 생명주기 한 줄).
//
//   val gyro = OemImu(this, object : OemImu.Listener {
//       override fun onAngle(a: GyroAngle) = runOnUiThread { render(a.xDeg, a.yDeg, a.zDeg) }
//       override fun onRate(r: GyroRate)   = runOnUiThread { render(r.zDegS) }
//   })
//   lifecycle.addObserver(gyro)
//
// 표준 자세(MTLT305 각도/각속도/가속도)는 표준 :imu 모듈을 쓴다. 이 모듈은 제조사 고유
// 보조 자이로(Allynav R70)만 다룬다 — 표준과 섞지 않는다.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class OemImu(context: Context, private val listener: Listener) : Signal(context) {

    /** 제조사 고유 자이로 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onAngle(angle: GyroAngle) {}
        fun onAccel(accel: GyroAccel) {}
        fun onRate(rate: GyroRate) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(GyroAngle.KEYS) { GyroAngle.from(it)?.let(listener::onAngle) },
        vehicle.subscribeMessage(GyroAccel.KEYS) { GyroAccel.from(it)?.let(listener::onAccel) },
        vehicle.subscribeMessage(GyroRate.KEYS)  { GyroRate.from(it)?.let(listener::onRate) },
    )
}
