// Gpio.kt — GPIO 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 입력 읽기 (자격 불필요) — 람다 or 리스너, 생명주기 한 줄
//   val gpio = Gpio(this, intervalMs = 200)
//   gpio.onAnalogIn { ch, v -> runOnUiThread { render(ch, v) } }
//       .onDigitalIn { ch, on -> runOnUiThread { render(ch, on) } }
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

class Gpio(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** GPIO 입력 콜백 — 필요한 것만 override. channel은 1-기반. */
    interface Listener {
        fun onAnalogIn(channel: Int, value: Double) {}
        fun onDigitalIn(channel: Int, on: Boolean) {}
    }

    /** 리스너 방식 — 두 콜백을 한 객체에서 override. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onAnalogIn(listener::onAnalogIn)
        onDigitalIn(listener::onDigitalIn)
    }

    // ── 람다 편의 API — 채널 전체를 하나의 람다로 받는다(cb에 channel 인자 포함). ──
    /** 아날로그 입력 (모든 채널). cb(channel, value) */
    fun onAnalogIn(cb: (channel: Int, value: Double) -> Unit) = apply {
        for (ch in 1..GpioSignals.ANALOG_IN_COUNT)
            regs += { vehicle.subscribe(GpioSignals.analogIn(ch), intervalMs) { it.number?.let { v -> cb(ch, v) } } }
    }

    /** 디지털 입력 (모든 채널). cb(channel, on) */
    fun onDigitalIn(cb: (channel: Int, on: Boolean) -> Unit) = apply {
        for (ch in 1..GpioSignals.DIGITAL_IN_COUNT)
            regs += { vehicle.subscribe(GpioSignals.digitalIn(ch), intervalMs) { it.number?.let { v -> cb(ch, v != 0.0) } } }
    }

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }

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
