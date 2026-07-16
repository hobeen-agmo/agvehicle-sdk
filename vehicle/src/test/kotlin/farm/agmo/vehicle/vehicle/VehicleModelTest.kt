// VehicleModelTest.kt — 순수 로직 유닛테스트: Dpf 경고 임계 경계, Int 변환 절삭, 다중신호 조립.
package farm.agmo.vehicle.vehicle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DpfTest {
    @Test fun warning_justBelowThreshold_isFalse() {
        assertFalse(Dpf(Dpf.WARN_THRESHOLD - 0.1).warning)
    }

    @Test fun warning_exactlyAtThreshold_isTrue() {
        // WARN_THRESHOLD 는 ">=" 비교이므로 경계값 자체가 warning
        assertTrue(Dpf(Dpf.WARN_THRESHOLD).warning)
    }

    @Test fun warning_justAboveThreshold_isTrue() {
        assertTrue(Dpf(Dpf.WARN_THRESHOLD + 0.1).warning)
    }

    @Test fun warning_zero_isFalse() {
        assertFalse(Dpf(0.0).warning)
    }
}

class GpsPositionTest {
    @Test fun from_bothKeysPresent_assembles() {
        val p = assertNotNull(GpsPosition.from(mapOf("GPS_LAT" to 37.5, "GPS_LON" to 127.0)))
        assertEquals(37.5, p.latitude)
        assertEquals(127.0, p.longitude)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(GpsPosition.from(mapOf("GPS_LAT" to 37.5)))
    }
}

class Ccvs1Test {
    private val full = mapOf(
        "VEHSPEED" to 12.3, "PARKBRAKE" to 1.0, "CRUISEACTIVE" to 0.0,
        "BRAKESWITCH" to 1.0, "CLUTCHSWITCH" to 0.0, "CRUISESETSPD" to 20.0,
    )

    @Test fun from_allSixKeysPresent_assemblesAndMapsBooleans() {
        val c = assertNotNull(Ccvs1.from(full))
        assertEquals(12.3, c.speedKmh)
        assertTrue(c.parkBrake)
        assertFalse(c.cruiseActive)
        assertTrue(c.brakeSwitch)
        assertFalse(c.clutchSwitch)
        assertEquals(20.0, c.cruiseSetKmh)
    }

    @Test fun from_missingOneOfSixKeys_isNull() {
        assertNull(Ccvs1.from(full - "CRUISESETSPD"))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(Ccvs1.from(emptyMap()))
    }
}

class Etc2Test {
    @Test fun from_positiveDecimals_truncateTowardZero() {
        val e = assertNotNull(
            Etc2.from(mapOf("GEARSEL" to 2.9, "GEARRATIO" to 1.5, "GEARCUR" to 3.9))
        )
        assertEquals(2, e.selectedGear)
        assertEquals(1.5, e.gearRatio)
        assertEquals(3, e.currentGear)
    }

    @Test fun from_negativeDecimal_truncatesTowardZero() {
        // Kotlin Double.toInt() 은 0 방향 절삭 — -1.9 → -1 (내림이 아님)
        val e = assertNotNull(
            Etc2.from(mapOf("GEARSEL" to -1.9, "GEARRATIO" to 1.0, "GEARCUR" to 0.0))
        )
        assertEquals(-1, e.selectedGear)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(Etc2.from(mapOf("GEARSEL" to 2.0, "GEARRATIO" to 1.5)))
    }
}

class WbsdTest {
    @Test fun from_allThreeKeysPresent_assembles() {
        val w = assertNotNull(
            Wbsd.from(mapOf("WBSD_SPEED" to 5.0, "WBSD_DIR" to 1.0, "KEYSWITCH" to 1.0))
        )
        assertEquals(5.0, w.speedMs)
        assertEquals(1, w.direction)
        assertTrue(w.keySwitchOn)
    }

    @Test fun from_keySwitchZero_isFalse() {
        val w = assertNotNull(
            Wbsd.from(mapOf("WBSD_SPEED" to 5.0, "WBSD_DIR" to 1.0, "KEYSWITCH" to 0.0))
        )
        assertFalse(w.keySwitchOn)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(Wbsd.from(mapOf("WBSD_SPEED" to 5.0, "WBSD_DIR" to 1.0)))
    }
}
