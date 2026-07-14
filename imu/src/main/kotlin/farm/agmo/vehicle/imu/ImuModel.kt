// ImuModel.kt — IMU 메시지(ID)별 데이터 클래스 + 조립 (순수 — JVM 테스트 대상).
//
// "CAN 메시지(ID) 하나 = 클래스 하나" 원칙. IMU는 3개 메시지(PGN)로 온다:
//   0xF029 각도 → ImuAngles(pitch, roll)
//   0xF02A 각속도 → ImuRates(x, y, z)
//   0xF02D 가속도 → ImuAccel(x, y, z)
// 한 PGN의 신호들은 한 프레임으로 같이 오므로, 필요한 키가 다 모이면 한 덩어리로 만든다.
package farm.agmo.vehicle.imu

/** 각도 (deg) — PGN 0xF029 */
data class ImuAngles(val pitchDeg: Double, val rollDeg: Double) {
    companion object {
        val KEYS = listOf("IMU_PITCH", "IMU_ROLL")
        fun from(v: Map<String, Double>): ImuAngles? {
            val p = v["IMU_PITCH"]; val r = v["IMU_ROLL"]
            return if (p != null && r != null) ImuAngles(p, r) else null
        }
    }
}

/** 각속도 (deg/s) — PGN 0xF02A */
data class ImuRates(val xDegS: Double, val yDegS: Double, val zDegS: Double) {
    companion object {
        val KEYS = listOf("IMU_GYROX", "IMU_GYROY", "IMU_GYROZ")
        fun from(v: Map<String, Double>): ImuRates? {
            val x = v["IMU_GYROX"]; val y = v["IMU_GYROY"]; val z = v["IMU_GYROZ"]
            return if (x != null && y != null && z != null) ImuRates(x, y, z) else null
        }
    }
}

/** 가속도 (m/s²) — PGN 0xF02D */
data class ImuAccel(val xMs2: Double, val yMs2: Double, val zMs2: Double) {
    companion object {
        val KEYS = listOf("IMU_ACCX", "IMU_ACCY", "IMU_ACCZ")
        fun from(v: Map<String, Double>): ImuAccel? {
            val x = v["IMU_ACCX"]; val y = v["IMU_ACCY"]; val z = v["IMU_ACCZ"]
            return if (x != null && y != null && z != null) ImuAccel(x, y, z) else null
        }
    }
}
