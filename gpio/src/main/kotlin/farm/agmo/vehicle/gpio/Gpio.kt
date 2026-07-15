// Gpio.kt — GPIO 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 입력 읽기 (자격 불필요) — 생명주기 한 줄
//   val gpio = Gpio(this, object : Gpio.Listener {
//       override fun onAnalogIn(ch: Int, value: Double)  = runOnUiThread { render(ch, value) }
//       override fun onDigitalIn(ch: Int, on: Boolean)   = runOnUiThread { render(ch, on) }
//   })
//   lifecycle.addObserver(gpio)
//
//   // 디지털 출력 제어 (매니페스트에 USES_CONTROL 선언 필요) — 출력 핀별 세션
//   val out = Gpio.digitalOut(this, channel = 3) ?: return
//   out.set(true)      // Digital_OUT3 ON. false=세션 상실
//   out.release()
//
// 출처: SeamOS GPIO_Prototyping 이관(표준 계열). 실제 핀맵은 데몬 gpio_config에서 결정.
package farm.agmo.vehicle.gpio

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Gpio(context: Context, private val listener: Listener) : Signal(context) {

    /** GPIO 입력 콜백 — 필요한 것만 override. channel은 1-기반. */
    interface Listener {
        fun onAnalogIn(channel: Int, value: Double) {}
        fun onDigitalIn(channel: Int, on: Boolean) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = buildList {
        for (ch in 1..GpioSignals.ANALOG_IN_COUNT) {
            add(vehicle.subscribe(GpioSignals.analogIn(ch)) { it.number?.let { v -> listener.onAnalogIn(ch, v) } })
        }
        for (ch in 1..GpioSignals.DIGITAL_IN_COUNT) {
            add(vehicle.subscribe(GpioSignals.digitalIn(ch)) { it.number?.let { v -> listener.onDigitalIn(ch, v != 0.0) } })
        }
    }

    companion object {
        /**
         * 디지털 출력 제어권 (channel 1~7). null = 자격 없음/보유 중/미연결/잘못된 channel.
         * ⚠️ 핀맵(어느 출력이 무슨 릴레이인지)은 데몬 gpio_config에서 결정된다(하드웨어 스펙 대기).
         */
        fun digitalOut(context: Context, channel: Int): GpioOutput? {
            if (channel !in 1..GpioSignals.DIGITAL_OUT_COUNT) return null
            val v = AgVehicle.shared(context)
            var handle: GpioOutput? = null
            val session = v.acquire(GpioSignals.digitalOut(channel)) { handle?.fireLost() } ?: return null
            return GpioOutput(session).also { handle = it }
        }
    }
}

/** 디지털 출력 제어 — ON/OFF (Toggle 구조: 토큰·선점) */
class GpioOutput internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    /** 켜기(true)/끄기(false). false 반환 = 세션 상실 — digitalOut부터 다시 */
    fun set(on: Boolean): Boolean = session.send(if (on) 1L else 0L)

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    /** 안전값(off)으로 되돌린 뒤 반납 */
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }
}
