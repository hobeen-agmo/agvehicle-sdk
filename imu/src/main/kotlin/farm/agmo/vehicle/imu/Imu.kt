// Imu.kt — IMU 읽기 도메인 모듈 (메시지별 파사드, core 위에 얹힘).
//
//   val sub = Imu.angles(context) { a -> render(a.pitchDeg, a.rollDeg) }
//   // ...
//   sub.close()
//
// 자격 선언 불필요(읽기 전용). 각 메시지(PGN)의 신호들이 모이면 타입 있는 한 덩어리로
// 넘긴다(core.subscribeMessage가 조립). 콜백은 binder 스레드 — UI 갱신은 앱이 post할 것.
package farm.agmo.vehicle.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle

object Imu {
    /** 각도(pitch/roll) 스트림. PGN 0xF029 */
    fun angles(context: Context, onSample: (ImuAngles) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(ImuAngles.KEYS) {
            ImuAngles.from(it)?.let(onSample)
        }

    /** 각속도(gyro x/y/z) 스트림. PGN 0xF02A */
    fun rates(context: Context, onSample: (ImuRates) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(ImuRates.KEYS) {
            ImuRates.from(it)?.let(onSample)
        }

    /** 가속도(accel x/y/z) 스트림. PGN 0xF02D */
    fun accel(context: Context, onSample: (ImuAccel) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(ImuAccel.KEYS) {
            ImuAccel.from(it)?.let(onSample)
        }
}
