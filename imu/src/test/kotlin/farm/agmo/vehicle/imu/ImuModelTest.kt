// ImuModelTest.kt — 순수 로직 유닛테스트: 3개 PGN 조립 함수의 null/빈 입력 방어.
package farm.agmo.vehicle.imu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ImuAnglesTest {
    @Test fun from_allKeysPresent_assembles() {
        val a = assertNotNull(ImuAngles.from(mapOf("IMU_PITCH" to 1.5, "IMU_ROLL" to -2.5)))
        assertEquals(1.5, a.pitchDeg)
        assertEquals(-2.5, a.rollDeg)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(ImuAngles.from(mapOf("IMU_PITCH" to 1.5)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(ImuAngles.from(emptyMap()))
    }
}

class ImuRatesTest {
    @Test fun from_allKeysPresent_assembles() {
        val r = assertNotNull(
            ImuRates.from(mapOf("IMU_GYROX" to 1.0, "IMU_GYROY" to 2.0, "IMU_GYROZ" to 3.0))
        )
        assertEquals(1.0, r.xDegS)
        assertEquals(2.0, r.yDegS)
        assertEquals(3.0, r.zDegS)
    }

    @Test fun from_missingOneOfThreeKeys_isNull() {
        assertNull(ImuRates.from(mapOf("IMU_GYROX" to 1.0, "IMU_GYROY" to 2.0)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(ImuRates.from(emptyMap()))
    }
}

class ImuAccelTest {
    @Test fun from_allKeysPresent_assembles() {
        val a = assertNotNull(
            ImuAccel.from(mapOf("IMU_ACCX" to 0.1, "IMU_ACCY" to 0.2, "IMU_ACCZ" to 9.8))
        )
        assertEquals(0.1, a.xMs2)
        assertEquals(0.2, a.yMs2)
        assertEquals(9.8, a.zMs2)
    }

    @Test fun from_missingOneOfThreeKeys_isNull() {
        assertNull(ImuAccel.from(mapOf("IMU_ACCX" to 0.1, "IMU_ACCZ" to 9.8)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(ImuAccel.from(emptyMap()))
    }
}
