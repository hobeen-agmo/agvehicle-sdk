// SteerModel.kt — AGMO SteerMotor 메시지(ID)별 데이터 클래스 (순수 — JVM 테스트 대상).
//
// "CAN 메시지(ID) 하나 = 클래스 하나" 원칙. 조향모터 신호:
//   조향각    → SteerAngle(deg)
//   모터 상태 → SteerMotorStatus(전류·온도·결함)
//
// ⚠️ 골격 단계(C): 키/스케일 플레이스홀더. SteerMotor는 CANopen류(0x580/0x600/0x700)라
//   SDO/PDO 매핑이 필요하며, 실제 인덱스·서브인덱스·스케일은 (B) 깨끗한 소스 확정 후 채운다.
//   NEVONEX 디컴파일 추출본의 값을 그대로 옮기지 않는다(reference/README 규율). — TODO(B)
package farm.agmo.vehicle.steer

/** 조향각 (deg, 중앙 0 기준 ±) */
data class SteerAngle(val deg: Double) {
    companion object {
        const val KEY = "STEER_ANGLE"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): SteerAngle? =
            v[KEY]?.let { SteerAngle(it) }
    }
}

/** 조향 모터 상태 — 전류(A)·온도(℃)·결함 플래그 */
data class SteerMotorStatus(val currentA: Double, val temperatureC: Double, val fault: Boolean) {
    companion object {
        val KEYS = listOf("STEER_CURRENT", "STEER_TEMP", "STEER_FAULT")   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): SteerMotorStatus? {
            val cur = v["STEER_CURRENT"]; val temp = v["STEER_TEMP"]; val fault = v["STEER_FAULT"]
            return if (cur != null && temp != null && fault != null)
                SteerMotorStatus(cur, temp, fault != 0.0) else null
        }
    }
}
