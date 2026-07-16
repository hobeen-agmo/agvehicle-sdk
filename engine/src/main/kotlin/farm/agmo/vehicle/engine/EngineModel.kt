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

/**
 * 엔진 잔여 토크 (0xF004, SPN 513/512).
 * EEC1 메시지지만 클래스명 `Eec1` 은 기존(레거시) [Eec1] 이 선점 — 잔여 토크만 별도 클래스로 배치.
 */
data class EngineTorque(val actualPercent: Double, val demandPercent: Double) {
    companion object {
        val KEYS = listOf("ENGTORQUE", "ENGTORQUE_DMD")
        fun from(v: Map<String, Double>): EngineTorque? {
            val a = v["ENGTORQUE"]; val d = v["ENGTORQUE_DMD"]
            return if (a != null && d != null) EngineTorque(a, d) else null
        }
    }
}

/** 엔진 EEC2 (0xF003, SPN 92/91) — 부하율 + 가속페달 위치 */
data class Eec2(val loadPercent: Double, val accelPedalPercent: Double) {
    companion object {
        val KEYS = listOf("ENGLOAD", "ACCELPEDAL")
        fun from(v: Map<String, Double>): Eec2? {
            val l = v["ENGLOAD"]; val a = v["ACCELPEDAL"]
            return if (l != null && a != null) Eec2(l, a) else null
        }
    }
}

/** 엔진 EEC3 (0xFEDF, SPN 514/515) — 마찰토크 + 희망 회전수 */
data class Eec3(val frictionPercent: Double, val desiredRpm: Double) {
    companion object {
        val KEYS = listOf("ENGFRICTION", "ENGDESSPD")
        fun from(v: Map<String, Double>): Eec3? {
            val f = v["ENGFRICTION"]; val d = v["ENGDESSPD"]
            return if (f != null && d != null) Eec3(f, d) else null
        }
    }
}

/** 엔진 ET1 (0xFEEE, SPN 110/174/175) — 냉각수온 + 연료온 + 오일온 */
data class Et1(val coolantC: Double, val fuelC: Double, val oilC: Double) {
    companion object {
        val KEYS = listOf("ENGTEMP", "FUELTEMP", "ENGOILTEMP")
        fun from(v: Map<String, Double>): Et1? {
            val c = v["ENGTEMP"]; val f = v["FUELTEMP"]; val o = v["ENGOILTEMP"]
            return if (c != null && f != null && o != null) Et1(c, f, o) else null
        }
    }
}

/** 엔진 EFL/P1 (0xFEEF, SPN 94/98/100/109/111) — 연료압 + 오일수위 + 오일압 + 냉각수압 + 냉각수위 */
data class Eflp1(
    val fuelDeliveryKPa: Double,
    val oilLevelPercent: Double,
    val oilKPa: Double,
    val coolantKPa: Double,
    val coolantLevelPercent: Double,
) {
    companion object {
        val KEYS = listOf("FUELDLVP", "ENGOILLVL", "ENGOILP", "COOLANTP", "COOLANTLVL")
        fun from(v: Map<String, Double>): Eflp1? {
            val fd = v["FUELDLVP"]; val ol = v["ENGOILLVL"]; val op = v["ENGOILP"]
            val cp = v["COOLANTP"]; val cl = v["COOLANTLVL"]
            return if (fd != null && ol != null && op != null && cp != null && cl != null)
                Eflp1(fd, ol, op, cp, cl) else null
        }
    }
}

/** 엔진 IC1 (0xFEF6, SPN 102/105/173) — 부스트압 + 흡기온 + 배기온 */
data class Ic1(val boostKPa: Double, val intakeC: Double, val exhaustC: Double) {
    companion object {
        val KEYS = listOf("BOOSTP", "INTAKETEMP", "EXHTEMP")
        fun from(v: Map<String, Double>): Ic1? {
            val b = v["BOOSTP"]; val i = v["INTAKETEMP"]; val e = v["EXHTEMP"]
            return if (b != null && i != null && e != null) Ic1(b, i, e) else null
        }
    }
}

/** 엔진 LFE (0xFEF2, SPN 183/184/185/51) — 순간연비율 + 순간연비 + 평균연비 + 스로틀 */
data class Lfe(
    val rateLh: Double,
    val economyKmL: Double,
    val economyAvgKmL: Double,
    val throttlePercent: Double,
) {
    companion object {
        val KEYS = listOf("FUELRATE", "FUELECON", "FUELECON_AVG", "THROTTLE")
        fun from(v: Map<String, Double>): Lfe? {
            val r = v["FUELRATE"]; val e = v["FUELECON"]; val a = v["FUELECON_AVG"]; val t = v["THROTTLE"]
            return if (r != null && e != null && a != null && t != null) Lfe(r, e, a, t) else null
        }
    }
}

/** 엔진 LFC (0xFEE9, SPN 182/250) — 구간 연료소모 + 누적 연료소모 */
data class Lfc(val tripL: Double, val totalL: Double) {
    companion object {
        val KEYS = listOf("TRIPFUEL", "TOTALFUEL")
        fun from(v: Map<String, Double>): Lfc? {
            val t = v["TRIPFUEL"]; val to = v["TOTALFUEL"]
            return if (t != null && to != null) Lfc(t, to) else null
        }
    }
}

/** 엔진 HOURS (0xFEE5, SPN 247/249) — 누적 가동시간 + 누적 회전수 */
data class Hours(val hours: Double, val revolutions: Double) {
    companion object {
        val KEYS = listOf("ENGHOURS", "ENGREVS")
        fun from(v: Map<String, Double>): Hours? {
            val h = v["ENGHOURS"]; val r = v["ENGREVS"]
            return if (h != null && r != null) Hours(h, r) else null
        }
    }
}

/**
 * 엔진 DD (0xFEFC, SPN 80/96/38) — 워셔액 + 연료량 + 연료량2.
 * WASHFLUID 는 elec 도메인 소속이나 PGN 이 DD 라 여기 배치.
 */
data class Dd(val washFluidPercent: Double, val fuelPercent: Double, val fuel2Percent: Double) {
    companion object {
        val KEYS = listOf("WASHFLUID", "FUELLVL", "FUELLVL2")
        fun from(v: Map<String, Double>): Dd? {
            val w = v["WASHFLUID"]; val f = v["FUELLVL"]; val f2 = v["FUELLVL2"]
            return if (w != null && f != null && f2 != null) Dd(w, f, f2) else null
        }
    }
}

/** 엔진 DPF1 (0xFD7B, SPN 3719/3720) — 매연 적재량 + 회분 적재량 */
data class Dpf1(val sootPercent: Double, val ashPercent: Double) {
    companion object {
        val KEYS = listOf("DPFSOOT", "DPFASH")
        fun from(v: Map<String, Double>): Dpf1? {
            val s = v["DPFSOOT"]; val a = v["DPFASH"]
            return if (s != null && a != null) Dpf1(s, a) else null
        }
    }
}

/** DEF 탱크 잔량 (0xFE56, SPN 1761) */
data class DefTank(val levelPercent: Double) {
    companion object {
        val KEYS = listOf("DEFLVL")
        fun from(v: Map<String, Double>): DefTank? =
            v["DEFLVL"]?.let { DefTank(it) }
    }
}
