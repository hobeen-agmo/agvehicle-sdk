// Imu.kt — IMU 읽기 도메인 (Signal 상속, 생명주기 한 줄 연동).
//
// 두 가지 사용법 (원하는 걸 고르면 됨):
//
//   // (1) 람다 — 필요한 신호만 짧게. object/override 불필요.
//   val imu = Imu(this, intervalMs = 200)
//   imu.onAngles { a -> runOnUiThread { render(a.pitchDeg, a.rollDeg) } }
//   lifecycle.addObserver(imu)     // ON_START 자동 구독 / ON_STOP 자동 해제
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val imu = Imu(this, object : Imu.Listener {
//       override fun onAngles(a: ImuAngles) = runOnUiThread { render(a.pitchDeg, a.rollDeg) }
//       override fun onRates(r: ImuRates)   = runOnUiThread { render(r.xDegS, r.yDegS, r.zDegS) }
//   })
//
// intervalMs: 앱 콜백 최소 간격(ms, 0=전부 전달). 자격 선언 불필요(읽기).
// 콜백은 binder 스레드 — UI 갱신은 앱이 runOnUiThread로.
// 필요한 메시지만 등록/override(안 하면 no-op). 스트림별 구독·조합이 필요하면 :flow 모듈 사용.
package farm.agmo.vehicle.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Imu(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** IMU 콜백 — 필요한 메시지만 override (리스너 방식) */
    interface Listener {
        fun onAngles(angles: ImuAngles) {}
        fun onRates(rates: ImuRates) {}
        fun onAccel(accel: ImuAccel) {}
    }

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. 내부적으로 람다 등록과 동일 경로. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onAngles(listener::onAngles); onRates(listener::onRates); onAccel(listener::onAccel)
    }

    // ── 람다 편의 API — 필요한 신호만 등록(체이닝 가능). ON_START에 실제 구독된다. ──
    fun onAngles(cb: (ImuAngles) -> Unit) = msg(ImuAngles.KEYS, ImuAngles::from, cb)
    fun onRates(cb: (ImuRates) -> Unit) = msg(ImuRates.KEYS, ImuRates::from, cb)
    fun onAccel(cb: (ImuAccel) -> Unit) = msg(ImuAccel.KEYS, ImuAccel::from, cb)

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 메시지(신호 여럿) 구독 등록 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }
}
