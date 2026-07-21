// GenericIo.kt — 범용 CAN IO 보드 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 입력 읽기 (자격 불필요) — 람다 or 리스너
//   val io = GenericIo(this, intervalMs = 200)
//   io.onAnalogIn { ch, v -> runOnUiThread { render(ch, v) } }
//     .onSupply { main, r5 -> runOnUiThread { render(main, r5) } }
//   lifecycle.addObserver(io)
//
//   // PWM 출력 제어 (매니페스트에 USES_CONTROL 선언 필요)
//   val pwm = GenericIo.pwm(this, side = "HS", channel = 1) ?: return
//   pwm.setDuty(50.0)                       // 0~100%
//   // LED 출력 제어 (RGBW 한 프레임에 결합 송신)
//   val led = GenericIo.led(this, led = 1) ?: return
//   led.setColor(channel = 0, r = 255, g = 0, b = 0, w = 0)
//
// 범용 하드웨어 IO — 소스가 NEVONEX GenericIO 참조라 provenance 구분 위해 oem 계열.
package farm.agmo.vehicle.oem.genericio

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class GenericIo(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** IO 보드 입력 콜백 — 필요한 것만 override. channel은 1-기반. */
    interface Listener {
        fun onAnalogIn(channel: Int, value: Double) {}
        fun onDigitalIn(channel: Int, on: Boolean) {}
        fun onSupply(mainVolts: Double, rail5V: Double) {}
    }

    /** 리스너 방식 — 콜백들을 한 객체에서 override. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onAnalogIn(listener::onAnalogIn)
        onDigitalIn(listener::onDigitalIn)
        onSupply(listener::onSupply)
    }

    // ── 람다 편의 API — 채널 콜백은 (channel, …) 인자 포함. ──
    /** 아날로그 입력 (모든 채널). cb(channel, value) */
    fun onAnalogIn(cb: (channel: Int, value: Double) -> Unit) = apply {
        for (ch in 1..GenericIoSignals.ANALOG_IN_COUNT)
            regs += { vehicle.subscribe(GenericIoSignals.analogIn(ch), intervalMs) { it.number?.let { v -> cb(ch, v) } } }
    }

    /** 디지털 입력 (모든 채널). cb(channel, on) */
    fun onDigitalIn(cb: (channel: Int, on: Boolean) -> Unit) = apply {
        for (ch in 1..GenericIoSignals.DIGITAL_IN_COUNT)
            regs += { vehicle.subscribe(GenericIoSignals.digitalIn(ch), intervalMs) { it.number?.let { v -> cb(ch, v != 0.0) } } }
    }

    /** 전원 전압(메인 · 5V 레일). cb(mainVolts, rail5V) */
    fun onSupply(cb: (mainVolts: Double, rail5V: Double) -> Unit) = apply {
        regs += {
            vehicle.subscribeMessage(listOf(GenericIoSignals.SUPPLY_VOLTAGE, GenericIoSignals.SUPPLY_5V), intervalMs) { m ->
                val main = m[GenericIoSignals.SUPPLY_VOLTAGE]; val r5 = m[GenericIoSignals.SUPPLY_5V]
                if (main != null && r5 != null) cb(main, r5)
            }
        }
    }

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }

    companion object {
        /** PWM 출력 제어권. side="HS"/"LS", channel 1~2. */
        fun pwm(context: Context, side: String, channel: Int): PwmOutput? {
            if (side != "HS" && side != "LS") return null
            if (channel !in 1..2) return null
            val v = AgVehicle.shared(context)
            var handle: PwmOutput? = null
            val session = v.acquire(GenericIoSignals.pwm(side, channel)) { handle?.fireLost() } ?: return null
            return PwmOutput(session).also { handle = it }
        }

        /** LED 출력 제어권. led 1~2. */
        fun led(context: Context, led: Int): LedOutput? {
            if (led !in 1..2) return null
            val v = AgVehicle.shared(context)
            var handle: LedOutput? = null
            val session = v.acquire(GenericIoSignals.led(led)) { handle?.fireLost() } ?: return null
            return LedOutput(led, session).also { handle = it }
        }
    }
}

/** PWM 듀티(0~100%) 제어 */
class PwmOutput internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    // debt: PWM 4채널이 한 프레임(0x202)이라 한 채널만 지령 시 다른 채널 바이트는 0으로 나간다.
    //   트리거: 다채널 동시 제어 필요 시 데몬 다중필드 write 또는 결합 신호로 승격.
    /** 듀티 지령(0~100%). false=세션 상실 */
    fun setDuty(percent: Double): Boolean = session.send(percent.toLong().coerceIn(0, 100))

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }
}

/** LED RGBW 제어 — Channel+RGBW를 결합 신호로 한 프레임에 원자 송신 */
class LedOutput internal constructor(private val led: Int, private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    // debt: LED1은 64bit 결합값인데 현재 명령 경로가 double(52bit 정밀도)를 거쳐 상위 비트
    //   (Blue<<48/White<<56)에서 정밀도 손실 가능. 트리거: 데몬 write 경로에 wide-integer
    //   변형 추가(정수 신호는 double 거치지 않도록) 시 정확 송신.
    /** RGBW+채널 색 지령. false=세션 상실 */
    fun setColor(channel: Int, r: Int, g: Int, b: Int, w: Int): Boolean {
        val raw = if (led == 1) GenericIoSignals.encodeLed1(channel.toLong(), r, g, b, w)
                  else GenericIoSignals.encodeLed2(channel, r, g, b, w)
        return session.send(raw)
    }

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }
}
