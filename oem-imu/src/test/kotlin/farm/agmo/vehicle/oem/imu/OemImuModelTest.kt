// OemImuModelTest.kt — 순수 로직 유닛테스트: 자이로/펌웨어버전 조립의 null 방어 + 버전 문자열 경계.
package farm.agmo.vehicle.oem.imu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GyroAngleTest {
    @Test fun from_allThreeKeysPresent_assembles() {
        val a = assertNotNull(
            GyroAngle.from(
                mapOf("allynav_r70:Angle_X" to 1.0, "allynav_r70:Angle_Y" to 2.0, "allynav_r70:Angle_Z" to 3.0)
            )
        )
        assertEquals(1.0, a.xDeg)
        assertEquals(2.0, a.yDeg)
        assertEquals(3.0, a.zDeg)
    }

    @Test fun from_missingOneOfThreeKeys_isNull() {
        assertNull(GyroAngle.from(mapOf("allynav_r70:Angle_X" to 1.0, "allynav_r70:Angle_Y" to 2.0)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(GyroAngle.from(emptyMap()))
    }
}

class GyroAccelTest {
    @Test fun from_allThreeKeysPresent_assembles() {
        val a = assertNotNull(
            GyroAccel.from(
                mapOf("allynav_r70:Accel_X" to 0.1, "allynav_r70:Accel_Y" to 0.2, "allynav_r70:Accel_Z" to 1.0)
            )
        )
        assertEquals(0.1, a.xG)
        assertEquals(0.2, a.yG)
        assertEquals(1.0, a.zG)
    }

    @Test fun from_missingOneOfThreeKeys_isNull() {
        assertNull(GyroAccel.from(mapOf("allynav_r70:Accel_X" to 0.1)))
    }
}

class GyroRateTest {
    @Test fun from_allThreeKeysPresent_assembles() {
        val r = assertNotNull(
            GyroRate.from(
                mapOf(
                    "allynav_r70:AngularRate_X" to 1.0,
                    "allynav_r70:AngularRate_Y" to 2.0,
                    "allynav_r70:AngularRate_Z" to 3.0,
                )
            )
        )
        assertEquals(1.0, r.xDegS)
        assertEquals(2.0, r.yDegS)
        assertEquals(3.0, r.zDegS)
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(GyroRate.from(emptyMap()))
    }
}

class ImuFirmwareVersionTest {
    private val keys = mapOf(
        "aceinna_mtlt305:Aceinna_Firm_Version_Major" to 1.0,
        "aceinna_mtlt305:Aceinna_Firm_Version_Minor" to 2.0,
        "aceinna_mtlt305:Aceinna_Firm_Version_Patch" to 3.0,
        "aceinna_mtlt305:Aceinna_Firm_Version_Stage" to 0.0,
        "aceinna_mtlt305:Aceinna_Firm_Version_Build" to 456.0,
    )

    @Test fun from_allFiveKeysPresent_assembles() {
        val v = assertNotNull(ImuFirmwareVersion.from(keys))
        assertEquals(1, v.major)
        assertEquals(2, v.minor)
        assertEquals(3, v.patch)
        assertEquals(0, v.stage)
        assertEquals(456, v.build)
    }

    @Test fun version_formatsAsMajorDotMinorDotPatch() {
        val v = assertNotNull(ImuFirmwareVersion.from(keys))
        assertEquals("1.2.3", v.version)
    }

    @Test fun version_zeroFields_formatsAsZeroDotZeroDotZero() {
        val v = ImuFirmwareVersion(major = 0, minor = 0, patch = 0, stage = 0, build = 0)
        assertEquals("0.0.0", v.version)
    }

    @Test fun from_missingOneOfFiveKeys_isNull() {
        assertNull(ImuFirmwareVersion.from(keys - "aceinna_mtlt305:Aceinna_Firm_Version_Build"))
    }
}
