// Engine.kt — 엔진 읽기 도메인 (Signal 상속, 생명주기 한 줄 연동).
//
//   val engine = Engine(this, object : Engine.Listener {
//       override fun onEec1(e: Eec1) = runOnUiThread { render(e.rpm) }     // 부하는 onEec2로
//       override fun onEec2(e: Eec2) = runOnUiThread { render(e.loadPercent) }
//       override fun onTemperature(t: EngineTemperature) = runOnUiThread { render(t.coolantC) }
//   })
//   lifecycle.addObserver(engine)
//
// 자격 선언 불필요(읽기). 필요한 메시지만 override(안 하면 no-op).
package farm.agmo.vehicle.engine

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Engine(
    context: Context,
    private val listener: Listener,
    private val intervalMs: Long = 0,   // 앱 콜백 최소 간격(ms). 0=전부 전달, >0=최대 그 간격마다 최신값 1회
) : Signal(context) {

    /** 엔진 콜백 — 필요한 메시지만 override */
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

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(Eec1.KEYS, intervalMs) { Eec1.from(it)?.let(listener::onEec1) },
        vehicle.subscribeMessage(EngineTemperature.KEYS, intervalMs) { EngineTemperature.from(it)?.let(listener::onTemperature) },
        vehicle.subscribeMessage(EngineOilPressure.KEYS, intervalMs) { EngineOilPressure.from(it)?.let(listener::onOilPressure) },
        vehicle.subscribeMessage(FuelLevel.KEYS, intervalMs) { FuelLevel.from(it)?.let(listener::onFuelLevel) },
        vehicle.subscribeMessage(EngineHours.KEYS, intervalMs) { EngineHours.from(it)?.let(listener::onHours) },
        vehicle.subscribeMessage(EngineTorque.KEYS, intervalMs) { EngineTorque.from(it)?.let(listener::onEngineTorque) },
        vehicle.subscribeMessage(Eec2.KEYS, intervalMs) { Eec2.from(it)?.let(listener::onEec2) },
        vehicle.subscribeMessage(Eec3.KEYS, intervalMs) { Eec3.from(it)?.let(listener::onEec3) },
        vehicle.subscribeMessage(Et1.KEYS, intervalMs) { Et1.from(it)?.let(listener::onEt1) },
        vehicle.subscribeMessage(Eflp1.KEYS, intervalMs) { Eflp1.from(it)?.let(listener::onEflp1) },
        vehicle.subscribeMessage(Ic1.KEYS, intervalMs) { Ic1.from(it)?.let(listener::onIc1) },
        vehicle.subscribeMessage(Lfe.KEYS, intervalMs) { Lfe.from(it)?.let(listener::onLfe) },
        vehicle.subscribeMessage(Lfc.KEYS, intervalMs) { Lfc.from(it)?.let(listener::onLfc) },
        vehicle.subscribeMessage(Hours.KEYS, intervalMs) { Hours.from(it)?.let(listener::onHoursRevolutions) },
        vehicle.subscribeMessage(Dd.KEYS, intervalMs) { Dd.from(it)?.let(listener::onDd) },
        vehicle.subscribeMessage(Dpf1.KEYS, intervalMs) { Dpf1.from(it)?.let(listener::onDpf1) },
        vehicle.subscribeMessage(DefTank.KEYS, intervalMs) { DefTank.from(it)?.let(listener::onDefTank) },
    )
}
