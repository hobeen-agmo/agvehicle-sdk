// GenericIo.kt — 범용 CAN IO 보드 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 입력 읽기 (자격 불필요)
//   val io = GenericIo(this, object : GenericIo.Listener {
//       override fun onAnalogIn(ch: Int, value: Double) = runOnUiThread { render(ch, value) }
//       override fun onDigitalIn(ch: Int, on: Boolean)  = runOnUiThread { render(ch, on) }
//   })
//   lifecycle.addObserver(io)
//
//   // PWM 출력 제어 (매니페스트에 USES_CONTROL 선언 필요)
//   val pwm = GenericIo.pwm(this, side = "HS", channel = 1) ?: return
//   pwm.setDuty(50.0)   // 0~100%
//
// 표준/플랫폼 계열(범용 하드웨어). LED(RGBW) 출력은 다중 필드 프레임이라 후속(정식 IO 스펙 시).
package farm.agmo.vehicle.genericio

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class GenericIo(context: Context, private val listener: Listener) : Signal(context) {

    /** IO 보드 입력 콜백 — 필요한 것만 override. channel은 1-기반. */
    interface Listener {
        fun onAnalogIn(channel: Int, value: Double) {}
        fun onDigitalIn(channel: Int, on: Boolean) {}
        fun onSupply(mainVolts: Double, rail5V: Double) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = buildList {
        for (ch in 1..GenericIoSignals.ANALOG_IN_COUNT)
            add(vehicle.subscribe(GenericIoSignals.analogIn(ch)) { it.number?.let { v -> listener.onAnalogIn(ch, v) } })
        for (ch in 1..GenericIoSignals.DIGITAL_IN_COUNT)
            add(vehicle.subscribe(GenericIoSignals.digitalIn(ch)) { it.number?.let { v -> listener.onDigitalIn(ch, v != 0.0) } })
        add(vehicle.subscribeMessage(listOf(GenericIoSignals.SUPPLY_VOLTAGE, GenericIoSignals.SUPPLY_5V)) { m ->
            val main = m[GenericIoSignals.SUPPLY_VOLTAGE]; val r5 = m[GenericIoSignals.SUPPLY_5V]
            if (main != null && r5 != null) listener.onSupply(main, r5)
        })
    }

    companion object {
        /** PWM 출력 제어권. side="HS"/"LS", channel 1~2. null=자격 없음/보유 중/미연결/인자 오류. */
        fun pwm(context: Context, side: String, channel: Int): PwmOutput? {
            if (side != "HS" && side != "LS") return null
            if (channel !in 1..2) return null
            val v = AgVehicle.shared(context)
            var handle: PwmOutput? = null
            val session = v.acquire(GenericIoSignals.pwm(side, channel)) { handle?.fireLost() } ?: return null
            return PwmOutput(session).also { handle = it }
        }
    }
}

/** PWM 듀티(0~100%) 제어 */
class PwmOutput internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    // debt: PWM 4채널이 한 프레임(0x202)이라 한 채널만 지령 시 다른 채널 바이트는 0으로 나간다.
    //   트리거: 다채널 동시 제어가 필요하면 데몬 다중필드 write 또는 결합 신호로 승격.
    /** 듀티 지령(0~100%). false=세션 상실 */
    fun setDuty(percent: Double): Boolean = session.send(percent.toLong().coerceIn(0, 100))

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }
}
