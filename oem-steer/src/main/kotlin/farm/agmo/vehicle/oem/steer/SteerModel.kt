// SteerModel.kt — AGMO 조향모터(Keya KY170) 메시지별 데이터 (순수 — JVM 테스트 대상).
//
// 값 출처: 공급사 DBC(Keya_KY170.dbc, Vector 저작). 조향모터 = Keya KY170 CANopen 서보.
//   데몬 카탈로그 키 "keya_ky170:<signal>".
//
// ✅ 지원:
//   - Motor_Heartbeat(0x07000001) 상태/결함 → SteerMotorStatus.
//   - Motor_Response(0x05800001) CANopen SDO(Data_ID 멀티플렉서) → 조향각/속도/온도/전압.
//     데몬 mux 지원으로 Data_ID별 값이 개별 신호로 온다(각기 다른 프레임 시점에 도착).
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.steer

/** 조향각 (deg) — Keya SDO EncoderCountValue */
data class SteerAngle(val deg: Double) {
    companion object {
        const val KEY = "keya_ky170:Response_EncoderCountValue"
        fun from(v: Map<String, Double>): SteerAngle? = v[KEY]?.let { SteerAngle(it) }
    }
}

/** 모터 속도 (rpm) — Keya SDO Encoder_Speed */
data class SteerSpeed(val rpm: Double) {
    companion object {
        const val KEY = "keya_ky170:Response_Encoder_Speed"
        fun from(v: Map<String, Double>): SteerSpeed? = v[KEY]?.let { SteerSpeed(it) }
    }
}

/** 모터 온도 (raw — 스케일은 데이터시트 기준) — Keya SDO MotorTemperature */
data class SteerTemperature(val value: Double) {
    companion object {
        const val KEY = "keya_ky170:Response_MotorTemperature"
        fun from(v: Map<String, Double>): SteerTemperature? = v[KEY]?.let { SteerTemperature(it) }
    }
}

/** 전원 전압 (V) — Keya SDO PowerSupplyVoltage */
data class SteerVoltage(val volts: Double) {
    companion object {
        const val KEY = "keya_ky170:Response_PowerSupplyVoltage"
        fun from(v: Map<String, Double>): SteerVoltage? = v[KEY]?.let { SteerVoltage(it) }
    }
}

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
