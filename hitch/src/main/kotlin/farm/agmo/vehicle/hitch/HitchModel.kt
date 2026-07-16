// HitchModel.kt — 히치 메시지(ID)별 데이터 + 스케일 (순수 — JVM 테스트 대상).
//
// 히치 도메인은 두 메시지(ID):
//   HITCH     (0xFE45, read)  → HitchPosition(percent)    현재 위치
//   HITCH_CMD (0xEF80, write) → HitchControl.setPosition() 지시
package farm.agmo.vehicle.hitch

/** 현재 히치 위치 (0~100%) — PGN 0xFE45 */
data class HitchPosition(val percent: Double) {
    companion object {
        const val KEY = "HITCH"
    }
}

/**
 * 히치 위치 %↔raw 변환. 도메인 모듈의 핵심 가치 중 하나: raw 변환을 앱에서 숨긴다.
 * 근거: 데몬 내장 신호 HITCH_CMD — resolution 0.4 %/bit, 8비트(raw 0..250 = 0..100%).
 */
object HitchScale {
    const val RESOLUTION = 0.4      // %/bit
    const val RAW_MAX = 250L        // 100%

    /** 위치 %(0~100) → 데몬 raw. 범위 밖은 클램프(안전) */
    fun toRaw(percent: Double): Long =
        (percent / RESOLUTION).toLong().coerceIn(0, RAW_MAX)

    /** 데몬 raw → 위치 % */
    fun toPercent(raw: Long): Double = raw * RESOLUTION
}

/**
 * 후방 히치 상세 (in-work/link force/draft) — PGN 0xFE45(J1939). 위치(%)는 ISOBUS 트랙의
 * HITCH(HitchPosition)가 별도 담당 — 여기엔 HITCH 키를 포함하지 않는다(트랙 분리, signal_defs.cpp 242행 주석).
 */
data class RearHitch(val inWork: Boolean, val linkForcePercent: Double, val draftN: Double) {
    companion object {
        val KEYS = listOf("HITCH_WORK", "HITCH_LINKFORCE", "HITCH_DRAFT")
        fun from(v: Map<String, Double>): RearHitch? {
            val work = v["HITCH_WORK"]; val force = v["HITCH_LINKFORCE"]; val draft = v["HITCH_DRAFT"]
            return if (work != null && force != null && draft != null)
                RearHitch(work != 0.0, force, draft) else null
        }
    }
}

/** 전방 히치 위치/작업 상태 — PGN 0xFE46 */
data class FrontHitch(val percent: Double, val inWork: Boolean) {
    companion object {
        val KEYS = listOf("FHITCH", "FHITCH_WORK")
        fun from(v: Map<String, Double>): FrontHitch? {
            val p = v["FHITCH"]; val work = v["FHITCH_WORK"]
            return if (p != null && work != null) FrontHitch(p, work != 0.0) else null
        }
    }
}
