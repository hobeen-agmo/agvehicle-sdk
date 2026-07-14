// HitchScale.kt — 히치 위치 %↔raw 변환 (순수 — Android 무의존, JVM 테스트 대상).
//
// 도메인 모듈의 핵심 가치 중 하나: raw 변환을 앱에서 숨긴다.
// 제네릭 core는 command(key, raw)로 raw Long을 받는데, 앱이 percent/0.4 같은 스케일을
// 직접 다루면 실수·중복이 생긴다. 여기서 한 번 정의하고 검증한다.
//
// 근거: 데몬 내장 신호 테이블(agcand signal_defs.cpp)의 HITCH_CMD —
//   resolution 0.4 %/bit, offset 0, 8비트(raw 0..250 = 0..100%).
package farm.agmo.vehicle.hitch

object HitchScale {
    const val RESOLUTION = 0.4      // %/bit
    const val RAW_MAX = 250L        // 100% (0.4×250)

    /** 위치 %(0~100) → 데몬 raw. 범위를 벗어나면 클램프(안전) */
    fun toRaw(percent: Double): Long =
        (percent / RESOLUTION).toLong().coerceIn(0, RAW_MAX)

    /** 데몬 raw → 위치 % (표시·역변환용) */
    fun toPercent(raw: Long): Double = raw * RESOLUTION
}
