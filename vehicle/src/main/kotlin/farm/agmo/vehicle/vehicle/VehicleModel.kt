// VehicleModel.kt — 홈 런처용 차량 상태 메시지(ID)별 데이터 클래스 (순수 — JVM 테스트).
//
// 홈화면이 필요로 하는 "엔진/히치 외" 신호를 모았다. 각 신호는 별개 CAN 메시지라
// "메시지=클래스" 원칙대로 각자 타입 클래스다.
package farm.agmo.vehicle.vehicle

/** 차속 (km/h) — CCVS1 0xFEF1 */
data class VehicleSpeed(val kmh: Double) {
    companion object { const val KEY = "VEHSPEED" }
}

/** 후방 PTO 회전수 (rpm) — 0xFE43 */
data class PtoSpeed(val rpm: Double) {
    companion object { const val KEY = "PTO" }
}

/** 배터리 전압 (V) — VEP1 0xFEF7 */
data class Battery(val volts: Double) {
    companion object { const val KEY = "BATT" }
}

/** DPF 그을음 적재율 (%) — DPF1 0xFD7B. warning은 임계값 초과로 판정 */
data class Dpf(val sootPercent: Double) {
    companion object {
        const val KEY = "DPFSOOT"
        const val WARN_THRESHOLD = 80.0   // 초과 시 "Excessive PM" 경고 (실차 기준으로 조정)
    }
    val warning: Boolean get() = sootPercent >= WARN_THRESHOLD
}

/** GPS 위경도 (deg) — VehiclePosition 0xFEF3. GPS도 CAN 버스로 받는다(안드로이드 위치 API 아님) */
data class GpsPosition(val latitude: Double, val longitude: Double) {
    companion object {
        val KEYS = listOf("GPS_LAT", "GPS_LON")
        fun from(v: Map<String, Double>): GpsPosition? {
            val lat = v["GPS_LAT"]; val lon = v["GPS_LON"]
            return if (lat != null && lon != null) GpsPosition(lat, lon) else null
        }
    }
}
