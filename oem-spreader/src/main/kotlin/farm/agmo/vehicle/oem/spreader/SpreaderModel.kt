// SpreaderModel.kt — AGMO RDA Spreader(살포기) 메시지별 데이터 (순수 — JVM 테스트 대상).
//
// 값 출처: AGMO 1차 설계(Spreader Machine Interface, Customer=AGMO Inc., Requestor Dongseok
//   Choi 2025-02-06) + Spreader_CAN.dbc. 데몬 카탈로그 키 "agmo_spreader:<signal>".
//
// 살포기 프로토콜(문서/DBC 확정): 살포 모터 모드 제어(WRITE) + GNSS 위치·시간/상태(READ).
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.spreader

/** 살포 모터 구동 모드 — 문서: 0x81 Manual, 0x82 Spreading, 0x84 Stop */
enum class DriveMode(val code: Int) {
    MANUAL(0x81), SPREADING(0x82), STOP(0x84);
}

/** GNSS fix 상태 — 문서: 1=Single, 4=Float, 5=Fix */
enum class GnssFix(val code: Int) {
    SINGLE(1), FLOAT(4), FIX(5), UNKNOWN(-1);
    companion object { fun of(c: Int) = entries.firstOrNull { it.code == c } ?: UNKNOWN }
}

/** GNSS 위치 (deg) — 0x18FF2800. raw*1e-7 (DBC 정의 그대로). */
data class GnssPosition(val latitudeDeg: Double, val longitudeDeg: Double) {
    companion object {
        val KEYS = listOf("agmo_spreader:Latitude", "agmo_spreader:Longitude")
        fun from(v: Map<String, Double>): GnssPosition? {
            val la = v["agmo_spreader:Latitude"]; val lo = v["agmo_spreader:Longitude"]
            return if (la != null && lo != null) GnssPosition(la, lo) else null
        }
    }
}

/** GNSS 시간/상태 — 0x18FF2801. gmt=HHMMSS, fix=상태, heartbeat=증가 카운터 */
data class GnssTimeStatus(val gmt: Long, val fix: GnssFix, val heartbeat: Int) {
    companion object {
        val KEYS = listOf("agmo_spreader:GreenwichMeanTime", "agmo_spreader:GnssStatus", "agmo_spreader:HeartBeat")
        fun from(v: Map<String, Double>): GnssTimeStatus? {
            val t = v["agmo_spreader:GreenwichMeanTime"]; val s = v["agmo_spreader:GnssStatus"]; val h = v["agmo_spreader:HeartBeat"]
            return if (t != null && s != null && h != null)
                GnssTimeStatus(t.toLong(), GnssFix.of(s.toInt()), h.toInt()) else null
        }
    }
}

/** 살포 모터 제어 키 — DriveMode(byte0)+MotorEnable(byte1) 결합 16bit(MotorCommand). */
internal object SpreaderControlKeys {
    const val MOTOR = "agmo_spreader:MotorCommand"
    /** raw = DriveMode | (enable<<8) — 한 프레임에 모드+enable 원자적 송신 */
    fun encode(mode: DriveMode, enable: Boolean): Long = (mode.code or (if (enable) 1 shl 8 else 0)).toLong()
}
