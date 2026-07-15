// TractorModel.kt — AGMO Customized Tractor 메시지(ID)별 데이터 클래스 (순수 — JVM 테스트 대상).
//
// "CAN 메시지(ID) 하나 = 클래스 하나" 원칙(엔진/IMU 모듈과 동일). AGMO 커스텀 트랙터의
// 상태 신호는 여러 CAN 메시지로 흩어져 온다:
//   FNR  → Fnr(전/중립/후진 상태)
//   SFT  → Shift(변속단)
//   PTO  → PtoState(PTO 결합·회전)     (홈 런처 PtoSpeed(rpm)와 다른 트랙터 고유 상태)
//   HYD  → Hydraulic(유압)
//   ACC  → Acc(가속/스로틀)
//
// ⚠️ 골격 단계(C): 키/스케일은 플레이스홀더다. 실제 agcand 카탈로그 키와 raw 스케일은
//   깨끗한 소스(AGMO 1차 CAN DBC/스펙)가 확정된 뒤 (B)에서 채운다 — TODO(B) 표식.
//   NEVONEX 디컴파일 추출본의 신호명/비트값을 그대로 옮기지 않는다(reference/README 규율).
package farm.agmo.vehicle.tractor

/** 전후진(FNR) 방향 */
enum class FnrDirection { FORWARD, NEUTRAL, REVERSE, UNKNOWN }

/** 전후진 상태 — FNR 메시지 */
data class Fnr(val direction: FnrDirection) {
    companion object {
        // TODO(B): 실제 agcand 신호 키로 확정. 현재는 플레이스홀더(추출본 미사용).
        const val KEY = "TRACTOR_FNR"

        /** 데몬 물리값 → 방향. TODO(B): 실제 상태 코드 매핑 확정 후 교체 */
        fun from(v: Map<String, Double>): Fnr? =
            v[KEY]?.let { Fnr(decodeDirection(it)) }

        // debt: FNR 상태 코드→enum 매핑이 플레이스홀더다. 트리거: (B) 깨끗한 소스 확정.
        private fun decodeDirection(@Suppress("UNUSED_PARAMETER") code: Double): FnrDirection =
            FnrDirection.UNKNOWN
    }
}

/** 변속단(SFT) 상태 */
data class Shift(val gear: Int) {
    companion object {
        const val KEY = "TRACTOR_SHIFT"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): Shift? =
            v[KEY]?.let { Shift(it.toInt()) }
    }
}

/** PTO 결합/회전 상태(트랙터 고유) — 홈 런처의 PtoSpeed(rpm)와 별개 메시지 */
data class PtoState(val engaged: Boolean, val rpm: Double) {
    companion object {
        val KEYS = listOf("TRACTOR_PTO_ENGAGED", "TRACTOR_PTO_RPM")   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): PtoState? {
            val eng = v["TRACTOR_PTO_ENGAGED"]; val rpm = v["TRACTOR_PTO_RPM"]
            return if (eng != null && rpm != null) PtoState(eng != 0.0, rpm) else null
        }
    }
}

/** 유압(HYD) 상태 */
data class Hydraulic(val pressure: Double) {
    companion object {
        const val KEY = "TRACTOR_HYD"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): Hydraulic? =
            v[KEY]?.let { Hydraulic(it) }
    }
}

/** 가속/스로틀(ACC) 상태 */
data class Acc(val percent: Double) {
    companion object {
        const val KEY = "TRACTOR_ACC"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): Acc? =
            v[KEY]?.let { Acc(it) }
    }
}
