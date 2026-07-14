// Engine.kt — 엔진 읽기 도메인 (Signal 상속, 생명주기 한 줄 연동).
//
//   val engine = Engine(this, object : Engine.Listener {
//       override fun onEec1(e: Eec1) = runOnUiThread { render(e.rpm, e.loadPercent) }
//       override fun onTemperature(t: EngineTemperature) = runOnUiThread { render(t.coolantC) }
//   })
//   lifecycle.addObserver(engine)
//
// 자격 선언 불필요(읽기). 필요한 메시지만 override(안 하면 no-op).
package farm.agmo.vehicle.engine

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Engine(context: Context, private val listener: Listener) : Signal(context) {

    /** 엔진 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onEec1(eec1: Eec1) {}
        fun onTemperature(temp: EngineTemperature) {}
        fun onOilPressure(oil: EngineOilPressure) {}
        fun onFuelLevel(fuel: FuelLevel) {}
        fun onHours(hours: EngineHours) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribeMessage(Eec1.KEYS) { Eec1.from(it)?.let(listener::onEec1) },
        vehicle.subscribeMessage(EngineTemperature.KEYS) { EngineTemperature.from(it)?.let(listener::onTemperature) },
        vehicle.subscribeMessage(EngineOilPressure.KEYS) { EngineOilPressure.from(it)?.let(listener::onOilPressure) },
        vehicle.subscribeMessage(FuelLevel.KEYS) { FuelLevel.from(it)?.let(listener::onFuelLevel) },
        vehicle.subscribeMessage(EngineHours.KEYS) { EngineHours.from(it)?.let(listener::onHours) },
    )
}
