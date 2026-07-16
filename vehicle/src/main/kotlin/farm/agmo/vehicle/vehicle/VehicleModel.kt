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

/** CCVS1 0xFEF1 (SPN 84/70/595/597/598/86) — full message, VEHSPEED도 재포함 */
data class Ccvs1(
    val speedKmh: Double,
    val parkBrake: Boolean,
    val cruiseActive: Boolean,
    val brakeSwitch: Boolean,
    val clutchSwitch: Boolean,
    val cruiseSetKmh: Double,
) {
    companion object {
        val KEYS = listOf("VEHSPEED", "PARKBRAKE", "CRUISEACTIVE", "BRAKESWITCH", "CLUTCHSWITCH", "CRUISESETSPD")
        fun from(v: Map<String, Double>): Ccvs1? {
            val speed = v["VEHSPEED"]; val park = v["PARKBRAKE"]; val cruise = v["CRUISEACTIVE"]
            val brake = v["BRAKESWITCH"]; val clutch = v["CLUTCHSWITCH"]; val cruiseSet = v["CRUISESETSPD"]
            return if (speed != null && park != null && cruise != null && brake != null && clutch != null && cruiseSet != null)
                Ccvs1(speed, park != 0.0, cruise != 0.0, brake != 0.0, clutch != 0.0, cruiseSet)
            else null
        }
    }
}

/** EBC1 0xF001 (SPN 563/521) */
data class Ebc1(val absActive: Boolean, val brakePedalPercent: Double) {
    companion object {
        val KEYS = listOf("ABSACTIVE", "BRAKEPEDAL")
        fun from(v: Map<String, Double>): Ebc1? {
            val abs = v["ABSACTIVE"]; val pedal = v["BRAKEPEDAL"]
            return if (abs != null && pedal != null) Ebc1(abs != 0.0, pedal) else null
        }
    }
}

/** EBC2 0xFEBF (SPN 904) */
data class Ebc2(val frontAxleKmh: Double) {
    companion object {
        val KEYS = listOf("WHEELSPD_FRONT")
        fun from(v: Map<String, Double>): Ebc2? {
            val speed = v["WHEELSPD_FRONT"]
            return if (speed != null) Ebc2(speed) else null
        }
    }
}

/** ETC1 0xF002 (SPN 560/191/161) */
data class Etc1(val drivelineEngaged: Boolean, val outputShaftRpm: Double, val inputShaftRpm: Double) {
    companion object {
        val KEYS = listOf("DRIVELINE", "TRANS_OUTSPD", "TRANS_INSPD")
        fun from(v: Map<String, Double>): Etc1? {
            val driveline = v["DRIVELINE"]; val outSpd = v["TRANS_OUTSPD"]; val inSpd = v["TRANS_INSPD"]
            return if (driveline != null && outSpd != null && inSpd != null)
                Etc1(driveline != 0.0, outSpd, inSpd)
            else null
        }
    }
}

/** ETC2 0xF005 (SPN 524/526/523) */
data class Etc2(val selectedGear: Int, val gearRatio: Double, val currentGear: Int) {
    companion object {
        val KEYS = listOf("GEARSEL", "GEARRATIO", "GEARCUR")
        fun from(v: Map<String, Double>): Etc2? {
            val sel = v["GEARSEL"]; val ratio = v["GEARRATIO"]; val cur = v["GEARCUR"]
            return if (sel != null && ratio != null && cur != null)
                Etc2(sel.toInt(), ratio, cur.toInt())
            else null
        }
    }
}

/** TCO1 0xFE6C (SPN 1624) */
data class Tco1(val speedKmh: Double) {
    companion object {
        val KEYS = listOf("TACHOSPD")
        fun from(v: Map<String, Double>): Tco1? {
            val speed = v["TACHOSPD"]
            return if (speed != null) Tco1(speed) else null
        }
    }
}

/** VDC2 0xF009 (SPN 1807/1808/1809) */
data class Vdc2(val steerAngleRad: Double, val yawRateRadS: Double, val latAccMs2: Double) {
    companion object {
        val KEYS = listOf("STEERANGLE", "YAWRATE", "LATACC")
        fun from(v: Map<String, Double>): Vdc2? {
            val angle = v["STEERANGLE"]; val yaw = v["YAWRATE"]; val lat = v["LATACC"]
            return if (angle != null && yaw != null && lat != null) Vdc2(angle, yaw, lat) else null
        }
    }
}

/** WBSD 0xFE48 — wheel-based speed/direction/key switch */
data class Wbsd(val speedMs: Double, val direction: Int, val keySwitchOn: Boolean) {
    companion object {
        val KEYS = listOf("WBSD_SPEED", "WBSD_DIR", "KEYSWITCH")
        fun from(v: Map<String, Double>): Wbsd? {
            val speed = v["WBSD_SPEED"]; val dir = v["WBSD_DIR"]; val key = v["KEYSWITCH"]
            return if (speed != null && dir != null && key != null)
                Wbsd(speed, dir.toInt(), key != 0.0)
            else null
        }
    }
}

/** GBSD 0xFE49 — ground-based speed/direction */
data class Gbsd(val speedMs: Double, val direction: Int) {
    companion object {
        val KEYS = listOf("GBSD_SPEED", "GBSD_DIR")
        fun from(v: Map<String, Double>): Gbsd? {
            val speed = v["GBSD_SPEED"]; val dir = v["GBSD_DIR"]
            return if (speed != null && dir != null) Gbsd(speed, dir.toInt()) else null
        }
    }
}

/** MSS 0xF022 — machine selected speed/direction */
data class Mss(val speedMs: Double, val direction: Int) {
    companion object {
        val KEYS = listOf("MSS_SPEED", "MSS_DIR")
        fun from(v: Map<String, Double>): Mss? {
            val speed = v["MSS_SPEED"]; val dir = v["MSS_DIR"]
            return if (speed != null && dir != null) Mss(speed, dir.toInt()) else null
        }
    }
}

/** VDHR 0xFEC1 (SPN 917) — hi-res distance */
data class Vdhr(val odometerKm: Double) {
    companion object {
        val KEYS = listOf("ODOMETER")
        fun from(v: Map<String, Double>): Vdhr? {
            val odo = v["ODOMETER"]
            return if (odo != null) Vdhr(odo) else null
        }
    }
}

/** VH 0xFEE7 (SPN 246/248) */
data class Vh(val vehicleHours: Double, val ptoHours: Double) {
    companion object {
        val KEYS = listOf("VEHHOURS", "PTOHOURS")
        fun from(v: Map<String, Double>): Vh? {
            val veh = v["VEHHOURS"]; val pto = v["PTOHOURS"]
            return if (veh != null && pto != null) Vh(veh, pto) else null
        }
    }
}

/** VEP1 0xFEF7 (SPN 114/167/168) — full message, BATT도 재포함 */
data class Vep1(val batteryCurrentA: Double, val alternatorVolts: Double, val batteryVolts: Double) {
    companion object {
        val KEYS = listOf("BATTCURR", "ALTVOLT", "BATT")
        fun from(v: Map<String, Double>): Vep1? {
            val curr = v["BATTCURR"]; val alt = v["ALTVOLT"]; val batt = v["BATT"]
            return if (curr != null && alt != null && batt != null) Vep1(curr, alt, batt) else null
        }
    }
}

/** 후방 PTO 0xFE43 — full message, PTO(속도)도 재포함 */
data class RearPto(val rpm: Double, val setRpm: Double, val engaged: Boolean) {
    companion object {
        val KEYS = listOf("PTO", "PTO_SETSPEED", "PTO_ENGAGED")
        fun from(v: Map<String, Double>): RearPto? {
            val rpm = v["PTO"]; val setRpm = v["PTO_SETSPEED"]; val engaged = v["PTO_ENGAGED"]
            return if (rpm != null && setRpm != null && engaged != null)
                RearPto(rpm, setRpm, engaged != 0.0)
            else null
        }
    }
}

/** 전방 PTO 0xFE44 */
data class FrontPto(val rpm: Double, val engaged: Boolean) {
    companion object {
        val KEYS = listOf("FPTO_SPEED", "FPTO_ENGAGED")
        fun from(v: Map<String, Double>): FrontPto? {
            val rpm = v["FPTO_SPEED"]; val engaged = v["FPTO_ENGAGED"]
            return if (rpm != null && engaged != null) FrontPto(rpm, engaged != 0.0) else null
        }
    }
}

/** VDS 0xFEE8 — GPS 고도/방위 + 항법 속도/피치 */
data class Vds(val altitudeM: Double, val headingDeg: Double, val speedKmh: Double, val pitchDeg: Double) {
    companion object {
        val KEYS = listOf("GPS_ALT", "GPS_HEADING", "NAV_SPEED", "NAV_PITCH")
        fun from(v: Map<String, Double>): Vds? {
            val alt = v["GPS_ALT"]; val heading = v["GPS_HEADING"]; val speed = v["NAV_SPEED"]; val pitch = v["NAV_PITCH"]
            return if (alt != null && heading != null && speed != null && pitch != null)
                Vds(alt, heading, speed, pitch)
            else null
        }
    }
}

/** AMB 0xFEF5 (SPN 108/171/172) — 기압/외기온/흡기온 */
data class Amb(val barometricKPa: Double, val ambientC: Double, val airInletC: Double) {
    companion object {
        val KEYS = listOf("BAROP", "AMBTEMP", "AIRINTEMP")
        fun from(v: Map<String, Double>): Amb? {
            val baro = v["BAROP"]; val ambient = v["AMBTEMP"]; val airIn = v["AIRINTEMP"]
            return if (baro != null && ambient != null && airIn != null) Amb(baro, ambient, airIn) else null
        }
    }
}

/** TD 0xFEE6 (SPN 959..964) — 데몬이 연도 offset(1985) 적용한 물리값을 주므로 그대로 toInt() */
data class Td(
    val seconds: Double,
    val minutes: Int,
    val hours: Int,
    val month: Int,
    val day: Double,
    val year: Int,
) {
    companion object {
        val KEYS = listOf("TIME_SEC", "TIME_MIN", "TIME_HOUR", "DATE_MONTH", "DATE_DAY", "DATE_YEAR")
        fun from(v: Map<String, Double>): Td? {
            val sec = v["TIME_SEC"]; val min = v["TIME_MIN"]; val hour = v["TIME_HOUR"]
            val month = v["DATE_MONTH"]; val day = v["DATE_DAY"]; val year = v["DATE_YEAR"]
            return if (sec != null && min != null && hour != null && month != null && day != null && year != null)
                Td(sec, min.toInt(), hour.toInt(), month.toInt(), day, year.toInt())
            else null
        }
    }
}
