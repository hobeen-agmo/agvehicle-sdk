// TractorModelTest.kt — 순수 로직 유닛테스트: FNR/RangeShift/PTO enum 코드 매핑 전수 + from() 방어.
package farm.agmo.vehicle.oem.tractor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FnrDirectionTest {
    @Test fun of_allDocumentedCodes_mapExactly() {
        assertEquals(FnrDirection.REVERSE, FnrDirection.of(1))
        assertEquals(FnrDirection.NEUTRAL, FnrDirection.of(2))
        assertEquals(FnrDirection.FORWARD, FnrDirection.of(3))
    }

    @Test fun of_unknownCode_fallsBackToUnknown() {
        assertEquals(FnrDirection.UNKNOWN, FnrDirection.of(0))
        assertEquals(FnrDirection.UNKNOWN, FnrDirection.of(4))
        assertEquals(FnrDirection.UNKNOWN, FnrDirection.of(-1))
    }
}

class RangeGearTest {
    @Test fun of_allDocumentedCodes_mapExactly() {
        assertEquals(RangeGear.LOW, RangeGear.of(1))
        assertEquals(RangeGear.MID, RangeGear.of(2))
        assertEquals(RangeGear.HIGH, RangeGear.of(3))
    }

    @Test fun of_unknownCode_fallsBackToUnknown() {
        assertEquals(RangeGear.UNKNOWN, RangeGear.of(0))
        assertEquals(RangeGear.UNKNOWN, RangeGear.of(99))
    }
}

class PtoModeTest {
    @Test fun of_allDocumentedCodes_mapExactly() {
        assertEquals(PtoMode.OFF, PtoMode.of(1))
        assertEquals(PtoMode.ON, PtoMode.of(2))
        assertEquals(PtoMode.AUTO_ON, PtoMode.of(3))
    }

    @Test fun of_unknownCode_fallsBackToUnknown() {
        assertEquals(PtoMode.UNKNOWN, PtoMode.of(0))
        assertEquals(PtoMode.UNKNOWN, PtoMode.of(-5))
    }
}

class FnrTest {
    @Test fun from_bothKeysPresent_assembles() {
        val f = assertNotNull(
            Fnr.from(mapOf("agmo_customized_tractor:TRZ_FNR_STATE" to 3.0, "agmo_customized_tractor:TRZ_FNR_AUTO" to 1.0))
        )
        assertEquals(FnrDirection.FORWARD, f.direction)
        assertTrue(f.auto)
    }

    @Test fun from_autoZero_isFalse() {
        val f = assertNotNull(
            Fnr.from(mapOf("agmo_customized_tractor:TRZ_FNR_STATE" to 1.0, "agmo_customized_tractor:TRZ_FNR_AUTO" to 0.0))
        )
        assertFalse(f.auto)
    }

    @Test fun from_unmappedStateCode_yieldsUnknownDirectionButStillAssembles() {
        val f = assertNotNull(
            Fnr.from(mapOf("agmo_customized_tractor:TRZ_FNR_STATE" to 9.0, "agmo_customized_tractor:TRZ_FNR_AUTO" to 0.0))
        )
        assertEquals(FnrDirection.UNKNOWN, f.direction)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(Fnr.from(mapOf("agmo_customized_tractor:TRZ_FNR_STATE" to 3.0)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(Fnr.from(emptyMap()))
    }
}

class TractorControlKeysTest {
    @Test fun controlKeys_allPrefixedWithAppNamespace() {
        val keys = listOf(
            TractorControlKeys.FNR, TractorControlKeys.RANGE_SHIFT, TractorControlKeys.PTO,
            TractorControlKeys.HITCH, TractorControlKeys.ACCELERATOR,
        )
        keys.forEach { assertTrue(it.startsWith("agmo_customized_tractor:")) }
    }
}
