// Engine.kt — 엔진 읽기 도메인 (Signal 상속, 생명주기 한 줄 연동).
//
// 두 가지 사용법 (원하는 걸 고르면 됨):
//
//   // (1) 람다 — 필요한 신호만 짧게. object/override 불필요.
//   val engine = Engine(this, intervalMs = 200)          // 5Hz
//   engine.onEec1 { e -> runOnUiThread { render(e.rpm) } }
//   lifecycle.addObserver(engine)
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val engine = Engine(this, object : Engine.Listener {
//       override fun onEec1(e: Eec1) = runOnUiThread { render(e.rpm) }
//       override fun onTemperature(t: EngineTemperature) = runOnUiThread { render(t.coolantC) }
//   }, intervalMs = 200)
//   lifecycle.addObserver(engine)
//
// intervalMs: 앱 콜백 최소 간격(ms, 0=전부 전달). 자격 선언 불필요(읽기).
package farm.agmo.vehicle.engine

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Engine(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** 엔진 콜백 — 필요한 메시지만 override (리스너 방식) */
    interface Listener {
        fun onEec1(eec1: Eec1) {}
        fun onTemperature(temp: EngineTemperature) {}
        fun onOilPressure(oil: EngineOilPressure) {}
        fun onFuelLevel(fuel: FuelLevel) {}
        fun onHours(hours: EngineHours) {}
        fun onEngineTorque(torque: EngineTorque) {}
        fun onEec2(eec2: Eec2) {}
        fun onEec3(eec3: Eec3) {}
        fun onEt1(et1: Et1) {}
        fun onEflp1(eflp1: Eflp1) {}
        fun onIc1(ic1: Ic1) {}
        fun onLfe(lfe: Lfe) {}
        fun onLfc(lfc: Lfc) {}
        fun onHoursRevolutions(hours: Hours) {}
        fun onDd(dd: Dd) {}
        fun onDpf1(dpf1: Dpf1) {}
        fun onDefTank(defTank: DefTank) {}
    }

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. 내부적으로 람다 등록과 동일 경로. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onEec1(listener::onEec1); onTemperature(listener::onTemperature)
        onOilPressure(listener::onOilPressure); onFuelLevel(listener::onFuelLevel)
        onHours(listener::onHours); onEngineTorque(listener::onEngineTorque)
        onEec2(listener::onEec2); onEec3(listener::onEec3); onEt1(listener::onEt1)
        onEflp1(listener::onEflp1); onIc1(listener::onIc1); onLfe(listener::onLfe)
        onLfc(listener::onLfc); onHoursRevolutions(listener::onHoursRevolutions)
        onDd(listener::onDd); onDpf1(listener::onDpf1); onDefTank(listener::onDefTank)
    }

    // ── 람다 편의 API — 필요한 신호만 등록(체이닝 가능). ON_START에 실제 구독된다. ──
    fun onEec1(cb: (Eec1) -> Unit) = msg(Eec1.KEYS, Eec1::from, cb)
    fun onTemperature(cb: (EngineTemperature) -> Unit) = msg(EngineTemperature.KEYS, EngineTemperature::from, cb)
    fun onOilPressure(cb: (EngineOilPressure) -> Unit) = msg(EngineOilPressure.KEYS, EngineOilPressure::from, cb)
    fun onFuelLevel(cb: (FuelLevel) -> Unit) = msg(FuelLevel.KEYS, FuelLevel::from, cb)
    fun onHours(cb: (EngineHours) -> Unit) = msg(EngineHours.KEYS, EngineHours::from, cb)
    fun onEngineTorque(cb: (EngineTorque) -> Unit) = msg(EngineTorque.KEYS, EngineTorque::from, cb)
    fun onEec2(cb: (Eec2) -> Unit) = msg(Eec2.KEYS, Eec2::from, cb)
    fun onEec3(cb: (Eec3) -> Unit) = msg(Eec3.KEYS, Eec3::from, cb)
    fun onEt1(cb: (Et1) -> Unit) = msg(Et1.KEYS, Et1::from, cb)
    fun onEflp1(cb: (Eflp1) -> Unit) = msg(Eflp1.KEYS, Eflp1::from, cb)
    fun onIc1(cb: (Ic1) -> Unit) = msg(Ic1.KEYS, Ic1::from, cb)
    fun onLfe(cb: (Lfe) -> Unit) = msg(Lfe.KEYS, Lfe::from, cb)
    fun onLfc(cb: (Lfc) -> Unit) = msg(Lfc.KEYS, Lfc::from, cb)
    fun onHoursRevolutions(cb: (Hours) -> Unit) = msg(Hours.KEYS, Hours::from, cb)
    fun onDd(cb: (Dd) -> Unit) = msg(Dd.KEYS, Dd::from, cb)
    fun onDpf1(cb: (Dpf1) -> Unit) = msg(Dpf1.KEYS, Dpf1::from, cb)
    fun onDefTank(cb: (DefTank) -> Unit) = msg(DefTank.KEYS, DefTank::from, cb)

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 메시지(신호 여럿) 구독 등록 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }
}
