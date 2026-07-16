// SpreaderModelTest.kt — 순수 로직 유닛테스트: GnssFix enum 매핑 전수 + MotorCommand 비트 인코딩.
package farm.agmo.vehicle.oem.spreader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GnssFixTest {
    @Test fun of_allDocumentedCodes_mapExactly() {
        assertEquals(GnssFix.SINGLE, GnssFix.of(1))
        assertEquals(GnssFix.FLOAT, GnssFix.of(4))
        assertEquals(GnssFix.FIX, GnssFix.of(5))
    }

    @Test fun of_unknownCode_fallsBackToUnknown() {
        assertEquals(GnssFix.UNKNOWN, GnssFix.of(0))
        assertEquals(GnssFix.UNKNOWN, GnssFix.of(2))
        assertEquals(GnssFix.UNKNOWN, GnssFix.of(-1))
    }
}

class SpreaderControlKeysEncodeTest {
    @Test fun encode_manualDisabled_isBareModeCode() {
        assertEquals(0x81L, SpreaderControlKeys.encode(DriveMode.MANUAL, enable = false))
    }

    @Test fun encode_manualEnabled_setsEnableBit8() {
        assertEquals(0x181L, SpreaderControlKeys.encode(DriveMode.MANUAL, enable = true))
    }

    @Test fun encode_spreadingEnabled_combinesModeAndEnableBit() {
        assertEquals(0x182L, SpreaderControlKeys.encode(DriveMode.SPREADING, enable = true))
    }

    @Test fun encode_spreadingDisabled_isBareModeCode() {
        assertEquals(0x82L, SpreaderControlKeys.encode(DriveMode.SPREADING, enable = false))
    }

    @Test fun encode_stopEnabled_combinesModeAndEnableBit() {
        assertEquals(0x184L, SpreaderControlKeys.encode(DriveMode.STOP, enable = true))
    }

    @Test fun encode_stopDisabled_isBareModeCode() {
        assertEquals(0x84L, SpreaderControlKeys.encode(DriveMode.STOP, enable = false))
    }
}

class GnssPositionTest {
    @Test fun from_bothKeysPresent_assembles() {
        val p = assertNotNull(
            GnssPosition.from(mapOf("agmo_spreader:Latitude" to 37.1, "agmo_spreader:Longitude" to 127.2))
        )
        assertEquals(37.1, p.latitudeDeg)
        assertEquals(127.2, p.longitudeDeg)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(GnssPosition.from(mapOf("agmo_spreader:Latitude" to 37.1)))
    }
}

class GnssTimeStatusTest {
    @Test fun from_allThreeKeysPresent_assembles() {
        val s = assertNotNull(
            GnssTimeStatus.from(
                mapOf(
                    "agmo_spreader:GreenwichMeanTime" to 123456.0,
                    "agmo_spreader:GnssStatus" to 5.0,
                    "agmo_spreader:HeartBeat" to 7.0,
                )
            )
        )
        assertEquals(123456L, s.gmt)
        assertEquals(GnssFix.FIX, s.fix)
        assertEquals(7, s.heartbeat)
    }

    @Test fun from_missingOneOfThreeKeys_isNull() {
        assertNull(
            GnssTimeStatus.from(
                mapOf("agmo_spreader:GreenwichMeanTime" to 123456.0, "agmo_spreader:GnssStatus" to 5.0)
            )
        )
    }
}
