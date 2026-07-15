// OemImu.kt — AGMO 제조사 고유 자이로 읽기 도메인 (Signal 상속, 생명주기 한 줄).
//
//   val gyro = OemImu(this, object : OemImu.Listener {
//       override fun onGyro(g: GyroHeading) = runOnUiThread { render(g.headingDeg, g.yawRateDegS) }
//   })
//   lifecycle.addObserver(gyro)
//
// 표준 자세(각도/각속도/가속도)는 표준 :imu 모듈(farm.agmo.vehicle.imu)을 쓴다. 이 모듈은
// 제조사 고유 자이로(Allynav_R70)만 다룬다 — 표준과 섞지 않는다.
//
// ⚠️ 골격 단계(C): 키 플레이스홀더(TODO(B)).
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class OemImu(context: Context, private val listener: Listener) : Signal(context) {

    /** 제조사 고유 자이로 콜백 — 필요한 것만 override */
    interface Listener {
        fun onGyro(gyro: GyroHeading) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(GyroHeading.KEYS) { GyroHeading.from(it)?.let(listener::onGyro) },
    )
}
