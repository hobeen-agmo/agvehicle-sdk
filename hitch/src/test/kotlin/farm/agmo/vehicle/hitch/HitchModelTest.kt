// HitchModelTest.kt — 순수 로직 유닛테스트: HitchScale raw↔% 변환 경계 + RearHitch/FrontHitch 조립.
package farm.agmo.vehicle.hitch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HitchScaleTest {
    @Test fun toRaw_zeroPercent_isZero() {
        assertEquals(0L, HitchScale.toRaw(0.0))
    }

    @Test fun toRaw_hundredPercent_isRawMax() {
        assertEquals(250L, HitchScale.toRaw(100.0))
    }

    @Test fun toRaw_negativePercent_clampsToZero() {
        assertEquals(0L, HitchScale.toRaw(-10.0))
    }

    @Test fun toRaw_overHundredPercent_clampsToRawMax() {
        assertEquals(250L, HitchScale.toRaw(150.0))
    }

    @Test fun toRaw_midValue_truncatesTowardZero() {
        // 50 / 0.4 = 125.0 정확히 나눠떨어짐
        assertEquals(125L, HitchScale.toRaw(50.0))
    }

    @Test fun toPercent_zeroRaw_isZero() {
        assertEquals(0.0, HitchScale.toPercent(0L))
    }

    @Test fun toPercent_rawMax_isHundred() {
        assertEquals(100.0, HitchScale.toPercent(250L))
    }

    @Test fun toPercent_doesNotClamp_pastRawMax() {
        // toPercent는 단순 곱셈이라 클램프하지 않음(문서화된 동작) — raw 300 → 120%
        assertEquals(120.0, HitchScale.toPercent(300L))
    }

    @Test fun toPercent_negativeRaw_isNegativePercent() {
        assertEquals(-4.0, HitchScale.toPercent(-10L))
    }
}

class RearHitchTest {
    private val fullMap = mapOf(
        "HITCH_WORK" to 1.0,
        "HITCH_LINKFORCE" to 42.0,
        "HITCH_DRAFT" to 100.0,
    )

    @Test fun from_allKeysPresent_assembles() {
        val rh = assertNotNull(RearHitch.from(fullMap))
        assertTrue(rh.inWork)
        assertEquals(42.0, rh.linkForcePercent)
        assertEquals(100.0, rh.draftN)
    }

    @Test fun from_workZero_isFalse() {
        val rh = assertNotNull(RearHitch.from(fullMap + ("HITCH_WORK" to 0.0)))
        assertFalse(rh.inWork)
    }

    @Test fun from_workNonZeroNegative_isTrue() {
        val rh = assertNotNull(RearHitch.from(fullMap + ("HITCH_WORK" to -1.0)))
        assertTrue(rh.inWork)
    }

    @Test fun from_missingOneKey_isNull() {
        assertNull(RearHitch.from(fullMap - "HITCH_DRAFT"))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(RearHitch.from(emptyMap()))
    }
}

class FrontHitchTest {
    @Test fun from_allKeysPresent_assembles() {
        val fh = assertNotNull(FrontHitch.from(mapOf("FHITCH" to 55.0, "FHITCH_WORK" to 1.0)))
        assertEquals(55.0, fh.percent)
        assertTrue(fh.inWork)
    }

    @Test fun from_missingKey_isNull() {
        assertNull(FrontHitch.from(mapOf("FHITCH" to 55.0)))
    }

    @Test fun from_emptyMap_isNull() {
        assertNull(FrontHitch.from(emptyMap()))
    }
}
