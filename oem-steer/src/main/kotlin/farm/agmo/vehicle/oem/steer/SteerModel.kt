// SteerModel.kt — AGMO 조향모터(Keya KY170) 메시지별 데이터 (순수 — JVM 테스트 대상).
//
// 값 출처: 공급사 DBC(Keya_KY170.dbc, Vector 저작). 조향모터 = Keya KY170 CANopen 서보.
//   데몬 카탈로그 키 "keya_ky170:<signal>".
//
// ✅ 지원: Motor_Heartbeat(0x07000001) 상태/결함 비트 → SteerMotorStatus.
// ⏸ 보류: 조향각/전류/속도 — Keya는 이를 CANopen SDO 멀티플렉서(Motor_Response 0x05800001,
//   Data_ID로 값 선택)로 주고, DBC상 위치도 미지정(placeholder)이다. agcand 평면 코덱은
//   멀티플렉스 미지원 → 정확히 표현 불가. 안전상(조향각) 비트위치 추측 금지. Keya 데이터시트/
//   정식 피드백 레이아웃 확보 또는 데몬 멀티플렉서 지원 후 추가. (TODO: 질의 중)
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.steer

/**
 * 조향모터 상태/결함 — Keya KY170 Motor_Heartbeat(0x07000001).
 * 각 플래그 = 해당 결함 발생(1). fault = 하나라도 발생.
 */
data class SteerMotorStatus(
    val hallFailure: Boolean,
    val canDisconnected: Boolean,
    val motorStalled: Boolean,
    val disabled: Boolean,
    val overvoltage: Boolean,
    val hardwareProtection: Boolean,
    val e2prom: Boolean,
    val undervoltage: Boolean,
    val overcurrent: Boolean,
    val modeFailure: Boolean,
) {
    /** 결함이 하나라도 있으면 true */
    val fault: Boolean
        get() = hallFailure || canDisconnected || motorStalled || disabled || overvoltage ||
                hardwareProtection || e2prom || undervoltage || overcurrent || modeFailure

    companion object {
        private const val P = "keya_ky170:Heartbeat_"
        val KEYS = listOf(
            "${P}HallFailure", "${P}CANdisconnected", "${P}MotorStalled", "${P}Disabled",
            "${P}Overvoltage", "${P}HardwareProtection", "${P}E2PROM", "${P}Undervoltage",
            "${P}Overcurrent", "${P}ModeFailure",
        )
        fun from(v: Map<String, Double>): SteerMotorStatus? {
            fun b(n: String) = v["$P$n"]?.let { it != 0.0 }
            val hall = b("HallFailure"); val can = b("CANdisconnected"); val stall = b("MotorStalled")
            val dis = b("Disabled"); val ov = b("Overvoltage"); val hw = b("HardwareProtection")
            val ee = b("E2PROM"); val uv = b("Undervoltage"); val oc = b("Overcurrent"); val mf = b("ModeFailure")
            return if (listOf(hall, can, stall, dis, ov, hw, ee, uv, oc, mf).all { it != null })
                SteerMotorStatus(hall!!, can!!, stall!!, dis!!, ov!!, hw!!, ee!!, uv!!, oc!!, mf!!) else null
        }
    }
}
