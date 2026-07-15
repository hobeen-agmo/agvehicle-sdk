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

// ── Aceinna MTLT305 설정/진단 (표준 자세는 표준 :imu가 담당) ──

/** MTLT305 센서 health (0x18FF5480). 각 비트의 정상/결함 극성은 Aceinna MTLT305 데이터시트 기준. */
data class ImuSensorStatus(
    val master: Boolean, val hardware: Boolean, val software: Boolean,
    val sensor: Boolean, val algoInit: Boolean, val overRange: Boolean,
) {
    companion object {
        val KEYS = listOf(
            "aceinna_mtlt305:Aceinna_Master_Status", "aceinna_mtlt305:Aceinna_Hardware_Status",
            "aceinna_mtlt305:Aceinna_Software_Status", "aceinna_mtlt305:Aceinna_Sensor_Status",
            "aceinna_mtlt305:Aceinna_Algo_Init", "aceinna_mtlt305:Aceinna_Sensor_Over_Range",
        )
        fun from(v: Map<String, Double>): ImuSensorStatus? {
            val m = v["aceinna_mtlt305:Aceinna_Master_Status"]; val h = v["aceinna_mtlt305:Aceinna_Hardware_Status"]
            val s = v["aceinna_mtlt305:Aceinna_Software_Status"]; val se = v["aceinna_mtlt305:Aceinna_Sensor_Status"]
            val a = v["aceinna_mtlt305:Aceinna_Algo_Init"]; val o = v["aceinna_mtlt305:Aceinna_Sensor_Over_Range"]
            return if (m != null && h != null && s != null && se != null && a != null && o != null)
                ImuSensorStatus(m != 0.0, h != 0.0, s != 0.0, se != 0.0, a != 0.0, o != 0.0) else null
        }
    }
}

/** MTLT305 펌웨어 버전 (0x18FEDA80) */
data class ImuFirmwareVersion(
    val major: Int, val minor: Int, val patch: Int, val stage: Int, val build: Int,
) {
    val version: String get() = "$major.$minor.$patch"
    companion object {
        val KEYS = listOf(
            "aceinna_mtlt305:Aceinna_Firm_Version_Major", "aceinna_mtlt305:Aceinna_Firm_Version_Minor",
            "aceinna_mtlt305:Aceinna_Firm_Version_Patch", "aceinna_mtlt305:Aceinna_Firm_Version_Stage",
            "aceinna_mtlt305:Aceinna_Firm_Version_Build",
        )
        fun from(v: Map<String, Double>): ImuFirmwareVersion? {
            val mj = v["aceinna_mtlt305:Aceinna_Firm_Version_Major"]; val mn = v["aceinna_mtlt305:Aceinna_Firm_Version_Minor"]
            val p = v["aceinna_mtlt305:Aceinna_Firm_Version_Patch"]; val st = v["aceinna_mtlt305:Aceinna_Firm_Version_Stage"]
            val b = v["aceinna_mtlt305:Aceinna_Firm_Version_Build"]
            return if (mj != null && mn != null && p != null && st != null && b != null)
                ImuFirmwareVersion(mj.toInt(), mn.toInt(), p.toInt(), st.toInt(), b.toInt()) else null
        }
    }
}
