// Imu.kt — IMU 읽기 도메인 (Android LocationManager/FusedLocationProvider 패턴 미러링).
//
//   val client = Imu.getClient(context)              // getFusedLocationProviderClient 대응
//   val cb = object : ImuCallback() {                // LocationCallback 대응
//       override fun onAngles(a: ImuAngles) { render(a.pitchDeg, a.rollDeg) }
//       override fun onRates(r: ImuRates)   { render(r.xDegS, r.yDegS, r.zDegS) }
//   }
//   client.requestUpdates(cb)                        // requestLocationUpdates 대응
//   // ...
//   client.removeUpdates(cb)                         // removeLocationUpdates 대응
//   val last = client.lastAngles                     // lastLocation 대응(캐시)
//
// 자격 선언 불필요(읽기). 콜백은 binder 스레드 — UI 갱신은 앱이 runOnUiThread로.
package farm.agmo.vehicle.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import java.util.concurrent.ConcurrentHashMap

/** IMU 콜백 — 필요한 메시지만 override (안 하면 no-op). LocationCallback 대응 */
abstract class ImuCallback {
    open fun onAngles(angles: ImuAngles) {}
    open fun onRates(rates: ImuRates) {}
    open fun onAccel(accel: ImuAccel) {}
}

object Imu {
    /** IMU 클라이언트 획득 (LocationServices.getFusedLocationProviderClient 대응) */
    fun getClient(context: Context): ImuClient = ImuClient(AgVehicle.shared(context))
}

class ImuClient internal constructor(private val v: AgVehicle) {
    // 콜백 객체 → 그 등록으로 생긴 구독 핸들들 (removeUpdates가 참조로 해제)
    private val regs = ConcurrentHashMap<ImuCallback, List<AgVehicle.Subscription>>()

    /** 마지막으로 받은 값 (없으면 null) — lastLocation 대응(이 프로세스 캐시) */
    @Volatile var lastAngles: ImuAngles? = null; private set
    @Volatile var lastRates: ImuRates? = null; private set
    @Volatile var lastAccel: ImuAccel? = null; private set

    /** 갱신 시작. 같은 콜백 재등록은 무시. IMU 세 메시지를 구독한다. */
    fun requestUpdates(callback: ImuCallback) {
        if (regs.containsKey(callback)) return
        regs[callback] = listOf(
            v.subscribeMessage(ImuAngles.KEYS) {
                ImuAngles.from(it)?.let { a -> lastAngles = a; callback.onAngles(a) }
            },
            v.subscribeMessage(ImuRates.KEYS) {
                ImuRates.from(it)?.let { r -> lastRates = r; callback.onRates(r) }
            },
            v.subscribeMessage(ImuAccel.KEYS) {
                ImuAccel.from(it)?.let { a -> lastAccel = a; callback.onAccel(a) }
            },
        )
    }

    /** 갱신 중단 — requestUpdates에 넘긴 콜백과 같은 객체를 넘겨야 한다 */
    fun removeUpdates(callback: ImuCallback) {
        regs.remove(callback)?.forEach { it.close() }
    }
}
