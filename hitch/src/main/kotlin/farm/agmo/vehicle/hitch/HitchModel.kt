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
