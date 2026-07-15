// OemImuModel.kt — AGMO 제조사 고유 자이로(Allynav R70) 메시지별 데이터 (순수 — JVM 테스트 대상).
//
// 표준 :imu 모듈(각도/각속도/가속도, MTLT305 데이터 PGN)로 커버되지 않는 제조사 고유
// 신호. Allynav R70 보조 자이로(공급사 DBC 소스):
//   자세각(deg)    → GyroAngle    (allynav_r70 0x18FFCA9A)
//   가속도(g)      → GyroAccel    (allynav_r70 0x18FFCB9A)
//   각속도(deg/s)  → GyroRate     (allynav_r70 0x18FFCC9A)
//
// 값 출처: 공급사 DBC(ALLYNAV_R70.dbc, Vector 저작). 데몬이 스케일 적용한 물리값을 전달하므로
//   SDK는 조립만 한다. (Aceinna MTLT305 설정/진단은 후속으로 이 모듈에 추가.)
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.imu

/** 자이로 자세각 (deg) — Allynav R70 0x18FFCA9A */
data class GyroAngle(val xDeg: Double, val yDeg: Double, val zDeg: Double) {
    companion object {
        val KEYS = listOf("allynav_r70:Angle_X", "allynav_r70:Angle_Y", "allynav_r70:Angle_Z")
        fun from(v: Map<String, Double>): GyroAngle? {
            val x = v["allynav_r70:Angle_X"]; val y = v["allynav_r70:Angle_Y"]; val z = v["allynav_r70:Angle_Z"]
            return if (x != null && y != null && z != null) GyroAngle(x, y, z) else null
        }
    }
}

/** 자이로 가속도 (g) — Allynav R70 0x18FFCB9A */
data class GyroAccel(val xG: Double, val yG: Double, val zG: Double) {
    companion object {
        val KEYS = listOf("allynav_r70:Accel_X", "allynav_r70:Accel_Y", "allynav_r70:Accel_Z")
        fun from(v: Map<String, Double>): GyroAccel? {
            val x = v["allynav_r70:Accel_X"]; val y = v["allynav_r70:Accel_Y"]; val z = v["allynav_r70:Accel_Z"]
            return if (x != null && y != null && z != null) GyroAccel(x, y, z) else null
        }
    }
}

/** 자이로 각속도 (deg/s) — Allynav R70 0x18FFCC9A */
data class GyroRate(val xDegS: Double, val yDegS: Double, val zDegS: Double) {
    companion object {
        val KEYS = listOf("allynav_r70:AngularRate_X", "allynav_r70:AngularRate_Y", "allynav_r70:AngularRate_Z")
        fun from(v: Map<String, Double>): GyroRate? {
            val x = v["allynav_r70:AngularRate_X"]; val y = v["allynav_r70:AngularRate_Y"]; val z = v["allynav_r70:AngularRate_Z"]
            return if (x != null && y != null && z != null) GyroRate(x, y, z) else null
        }
    }
}
