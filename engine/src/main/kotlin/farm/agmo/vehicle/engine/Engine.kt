// Engine.kt — 엔진 읽기 도메인 모듈 (메시지별 파사드, core 위에 얹힘).
//
//   val sub = Engine.eec1(context) { e -> render(e.rpm, e.loadPercent) }
//   val t   = Engine.temperature(context) { render(it.coolantC) }
//   // ...
//   sub.close(); t.close()
//
// 자격 선언 불필요(읽기 전용). 각 메시지(PGN)를 구독해 모이면 타입 있는 한 덩어리로 넘긴다.
// 콜백은 binder 스레드 — UI 갱신은 앱이 post할 것.
package farm.agmo.vehicle.engine

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle

object Engine {
    /** EEC1 — 회전수 + 부하 (0xF004) */
    fun eec1(context: Context, onSample: (Eec1) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(Eec1.KEYS) { Eec1.from(it)?.let(onSample) }

    /** 냉각수온 (0xFEEE) */
    fun temperature(context: Context, onSample: (EngineTemperature) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(EngineTemperature.KEYS) {
            EngineTemperature.from(it)?.let(onSample)
        }

    /** 오일압 (0xFEEF) */
    fun oilPressure(context: Context, onSample: (EngineOilPressure) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(EngineOilPressure.KEYS) {
            EngineOilPressure.from(it)?.let(onSample)
        }

    /** 연료량 (0xFEFC) */
    fun fuelLevel(context: Context, onSample: (FuelLevel) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(FuelLevel.KEYS) {
            FuelLevel.from(it)?.let(onSample)
        }

    /** 누적 가동시간 (0xFEE5) */
    fun hours(context: Context, onSample: (EngineHours) -> Unit): AgVehicle.Subscription =
        AgVehicle.shared(context).subscribeMessage(EngineHours.KEYS) {
            EngineHours.from(it)?.let(onSample)
        }
}
