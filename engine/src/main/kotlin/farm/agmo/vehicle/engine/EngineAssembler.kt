// EngineAssembler.kt — 여러 신호를 하나의 EngineSample로 조립 (순수 — JVM 테스트 대상).
//
// 도메인 모듈의 또 다른 핵심 가치: 따로 도착하는 신호를 모아 타입 있는 한 덩어리로 준다.
// 엔진 수온(ENGTEMP)과 RPM(ENGRPM)은 서로 다른 주기로 각각 도착한다. 제네릭 core는
// 그 조립을 앱에 떠넘기지만, 도메인 모듈이 최신값을 유지하며 한 번에 조립한다.
package farm.agmo.vehicle.engine

/** 엔진 상태 한 덩어리 — 아직 안 온 값은 null */
data class EngineSample(
    val coolantTempC: Double? = null,   // ENGTEMP (°C)
    val rpm: Double? = null,            // ENGRPM (rpm)
) {
    /** 두 값이 모두 도착했는가 (완전한 한 프레임인가) */
    val complete: Boolean get() = coolantTempC != null && rpm != null
}

/** 도착한 신호를 최신값으로 누적. update()가 갱신된 스냅샷을 돌려준다. */
class EngineAssembler {
    private var sample = EngineSample()

    /** 내장 신호 key */
    companion object {
        const val KEY_TEMP = "ENGTEMP"
        const val KEY_RPM = "ENGRPM"
        val KEYS = listOf(KEY_TEMP, KEY_RPM)
    }

    /** 신호 1건 반영 → 갱신된 스냅샷. 관심 밖 key면 이전 스냅샷 그대로 */
    fun update(key: String, number: Double?): EngineSample {
        sample = when (key) {
            KEY_TEMP -> sample.copy(coolantTempC = number)
            KEY_RPM -> sample.copy(rpm = number)
            else -> sample
        }
        return sample
    }
}
