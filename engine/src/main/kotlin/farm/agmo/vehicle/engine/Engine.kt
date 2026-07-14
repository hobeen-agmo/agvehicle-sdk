// Engine.kt — 엔진 읽기 도메인 (Android LocationManager 패턴 미러링).
//
//   val client = Engine.getClient(context)
//   val cb = object : EngineCallback() {
//       override fun onEec1(e: Eec1) { render(e.rpm, e.loadPercent) }
//       override fun onTemperature(t: EngineTemperature) { render(t.coolantC) }
//   }
//   client.requestUpdates(cb)
//   // ...
//   client.removeUpdates(cb)
//   val rpm = client.lastEec1?.rpm
//
// 자격 선언 불필요(읽기). 콜백은 binder 스레드 — UI 갱신은 앱이 runOnUiThread로.
package farm.agmo.vehicle.engine

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import java.util.concurrent.ConcurrentHashMap

/** 엔진 콜백 — 필요한 메시지만 override (안 하면 no-op) */
abstract class EngineCallback {
    open fun onEec1(eec1: Eec1) {}
    open fun onTemperature(temp: EngineTemperature) {}
    open fun onOilPressure(oil: EngineOilPressure) {}
    open fun onFuelLevel(fuel: FuelLevel) {}
    open fun onHours(hours: EngineHours) {}
}

object Engine {
    fun getClient(context: Context): EngineClient = EngineClient(AgVehicle.shared(context))
}

class EngineClient internal constructor(private val v: AgVehicle) {
    private val regs = ConcurrentHashMap<EngineCallback, List<AgVehicle.Subscription>>()

    @Volatile var lastEec1: Eec1? = null; private set
    @Volatile var lastTemperature: EngineTemperature? = null; private set
    @Volatile var lastOilPressure: EngineOilPressure? = null; private set
    @Volatile var lastFuelLevel: FuelLevel? = null; private set
    @Volatile var lastHours: EngineHours? = null; private set

    /** 갱신 시작. 엔진 다섯 메시지를 구독한다 (override 안 한 콜백은 no-op). */
    fun requestUpdates(callback: EngineCallback) {
        if (regs.containsKey(callback)) return
        regs[callback] = listOf(
            v.subscribeMessage(Eec1.KEYS) {
                Eec1.from(it)?.let { e -> lastEec1 = e; callback.onEec1(e) }
            },
            v.subscribeMessage(EngineTemperature.KEYS) {
                EngineTemperature.from(it)?.let { t -> lastTemperature = t; callback.onTemperature(t) }
            },
            v.subscribeMessage(EngineOilPressure.KEYS) {
                EngineOilPressure.from(it)?.let { o -> lastOilPressure = o; callback.onOilPressure(o) }
            },
            v.subscribeMessage(FuelLevel.KEYS) {
                FuelLevel.from(it)?.let { f -> lastFuelLevel = f; callback.onFuelLevel(f) }
            },
            v.subscribeMessage(EngineHours.KEYS) {
                EngineHours.from(it)?.let { h -> lastHours = h; callback.onHours(h) }
            },
        )
    }

    fun removeUpdates(callback: EngineCallback) {
        regs.remove(callback)?.forEach { it.close() }
    }
}
