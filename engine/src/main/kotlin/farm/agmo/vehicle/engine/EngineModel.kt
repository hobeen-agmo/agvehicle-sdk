// EngineModel.kt — 엔진 메시지(ID)별 데이터 클래스 (순수 — JVM 테스트 대상).
//
// "CAN 메시지(ID) 하나 = 클래스 하나" 원칙. 엔진 신호는 여러 J1939 메시지에 흩어져 있다:
//   EEC1 (0xF004)  → Eec1(rpm, loadPercent)       — 다중신호 한 메시지
//   ET1  (0xFEEE)  → EngineTemperature(coolantC)
//   EFL/P1 (0xFEEF)→ EngineOilPressure(kPa)
//   DD   (0xFEFC)  → FuelLevel(percent)
//   HOURS(0xFEE5)  → EngineHours(hours)
// 앱이 엔진 대시보드를 만들면 필요한 메시지만 구독한다.
package farm.agmo.vehicle.engine

/** 엔진 EEC1 (0xF004) — 회전수 + 부하 (한 메시지의 두 신호) */
data class Eec1(val rpm: Double, val loadPercent: Double) {
    companion object {
        val KEYS = listOf("ENGRPM", "ENGLOAD")
        fun from(v: Map<String, Double>): Eec1? {
            val r = v["ENGRPM"]; val l = v["ENGLOAD"]
            return if (r != null && l != null) Eec1(r, l) else null
        }
    }
}

/** 엔진 냉각수온 (0xFEEE) */
data class EngineTemperature(val coolantC: Double) {
    companion object {
        val KEYS = listOf("ENGTEMP")
        fun from(v: Map<String, Double>): EngineTemperature? =
            v["ENGTEMP"]?.let { EngineTemperature(it) }
    }
}

/** 엔진 오일압 (0xFEEF) */
data class EngineOilPressure(val kPa: Double) {
    companion object {
        val KEYS = listOf("ENGOILP")
        fun from(v: Map<String, Double>): EngineOilPressure? =
            v["ENGOILP"]?.let { EngineOilPressure(it) }
    }
}

/** 연료량 (0xFEFC) */
data class FuelLevel(val percent: Double) {
    companion object {
        val KEYS = listOf("FUELLVL")
        fun from(v: Map<String, Double>): FuelLevel? =
            v["FUELLVL"]?.let { FuelLevel(it) }
    }
}

/** 누적 엔진 가동시간 (0xFEE5) */
data class EngineHours(val hours: Double) {
    companion object {
        val KEYS = listOf("ENGHOURS")
        fun from(v: Map<String, Double>): EngineHours? =
            v["ENGHOURS"]?.let { EngineHours(it) }
    }
}
