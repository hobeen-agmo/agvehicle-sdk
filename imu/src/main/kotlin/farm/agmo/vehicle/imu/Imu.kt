// Imu.kt — IMU 읽기 도메인 (Signal 상속, 생명주기 한 줄 연동).
//
//   val imu = Imu(this, object : Imu.Listener {
//       override fun onAngles(a: ImuAngles) = runOnUiThread { render(a.pitchDeg, a.rollDeg) }
//       override fun onRates(r: ImuRates)   = runOnUiThread { render(r.xDegS, r.yDegS, r.zDegS) }
//   })
//   lifecycle.addObserver(imu)     // ON_START 자동 구독 / ON_STOP 자동 해제
//
// 자격 선언 불필요(읽기). 콜백은 binder 스레드 — UI 갱신은 앱이 runOnUiThread로.
// 필요한 메시지만 override(안 하면 no-op). 스트림별 구독·조합이 필요하면 :flow 모듈 사용.
package farm.agmo.vehicle.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Imu(context: Context, private val listener: Listener) : Signal(context) {

    /** IMU 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onAngles(angles: ImuAngles) {}
        fun onRates(rates: ImuRates) {}
        fun onAccel(accel: ImuAccel) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(ImuAngles.KEYS) { ImuAngles.from(it)?.let(listener::onAngles) },
        vehicle.subscribeMessage(ImuRates.KEYS)  { ImuRates.from(it)?.let(listener::onRates) },
        vehicle.subscribeMessage(ImuAccel.KEYS)  { ImuAccel.from(it)?.let(listener::onAccel) },
    )
}
