// ModelTest.kt — 순수 로직 유닛테스트: Quality/SignalValue 값 변환 + CatalogLine 21컬럼 파서.
package farm.agmo.vehicle.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QualityTest {
    @Test fun of_knownTokens_mapExactly() {
        assertEquals(Quality.OK, Quality.of("OK"))
        assertEquals(Quality.DISCONNECTED, Quality.of("DISCONNECTED"))
        assertEquals(Quality.IMPLAUSIBLE, Quality.of("IMPLAUSIBLE"))
    }

    @Test fun of_unknownToken_fallsBackToDisconnected() {
        assertEquals(Quality.DISCONNECTED, Quality.of("GARBAGE"))
    }

    @Test fun of_emptyString_fallsBackToDisconnected() {
        assertEquals(Quality.DISCONNECTED, Quality.of(""))
    }

    @Test fun of_lowercaseKnownToken_isCaseSensitiveAndFallsBack() {
        // enum name 매칭은 대소문자 구분 — 소문자 "ok"는 미지 토큰 취급
        assertEquals(Quality.DISCONNECTED, Quality.of("ok"))
    }
}

class SignalValueTest {
    @Test fun number_parsesLeadingNumericToken() {
        assertEquals(52.4, SignalValue("k", "52.4 %", Quality.OK).number)
        assertEquals(1450.0, SignalValue("k", "1450 rpm", Quality.OK).number)
    }

    @Test fun number_negativeValue_parses() {
        assertEquals(-5.5, SignalValue("k", "-5.5 C", Quality.OK).number)
    }

    @Test fun number_noUnitSuffix_stillParses() {
        assertEquals(42.0, SignalValue("k", "42", Quality.OK).number)
    }

    @Test fun number_nonNumericText_isNull() {
        assertNull(SignalValue("k", "abc", Quality.OK).number)
    }

    @Test fun number_emptyText_isNull() {
        assertNull(SignalValue("k", "", Quality.OK).number)
    }

    @Test fun unit_extractsTrailingToken() {
        assertEquals("%", SignalValue("k", "52.4 %", Quality.OK).unit)
        assertEquals("rpm", SignalValue("k", "1450 rpm", Quality.OK).unit)
    }

    @Test fun unit_missingSuffix_isNull() {
        assertNull(SignalValue("k", "42", Quality.OK).unit)
    }

    @Test fun unit_emptyText_isNull() {
        assertNull(SignalValue("k", "", Quality.OK).unit)
    }
}

class CatalogLineTest {
    // 21컬럼 정상 라인 (owner message bus idKind id access updateMs timeoutCount healCount
    //  byteOrder signal startBit length type signed resolution offset na npl safe unit)
    private val validLine =
        "platform EEC1 vehicle pgn 0xF004 read 100 5 3 little ENGRPM 0 16 uint16 0 0.125 0 0xFFFF 0xFFFE - rpm"

    @Test fun parse_validLine_parsesAllFields() {
        val meta = assertNotNull(CatalogLine.parse(validLine))
        assertEquals("platform", meta.owner)
        assertEquals("EEC1", meta.message)
        assertEquals("vehicle", meta.bus)
        assertEquals("pgn", meta.idKind)
        assertEquals(0xF004L, meta.id)
        assertEquals("read", meta.access)
        assertEquals(100, meta.updateMs)
        assertEquals(5, meta.timeoutCount)
        assertEquals(3, meta.healCount)
        assertEquals("little", meta.byteOrder)
        assertEquals("ENGRPM", meta.signal)
        assertEquals(0, meta.startBit)
        assertEquals(16, meta.length)
        assertEquals("uint16", meta.type)
        assertFalse(meta.signed)
        assertEquals(0.125, meta.resolution)
        assertEquals(0.0, meta.offset)
        assertEquals(0xFFFFL, meta.na)
        assertEquals(0xFFFEL, meta.npl)
        assertNull(meta.safe)
        assertEquals("rpm", meta.unit)
    }

    @Test fun parse_platformOwner_keyIsBareSignalName() {
        val meta = assertNotNull(CatalogLine.parse(validLine))
        assertEquals("ENGRPM", meta.key)
    }

    @Test fun parse_nonPlatformOwner_keyIsPrefixedWithOwner() {
        val line = validLine.replaceFirst("platform", "agmo_customized_tractor")
        val meta = assertNotNull(CatalogLine.parse(line))
        assertEquals("agmo_customized_tractor:ENGRPM", meta.key)
    }

    @Test fun parse_writeAccess_isWritable() {
        val line = validLine.replaceFirst(" read ", " write ")
        val meta = assertNotNull(CatalogLine.parse(line))
        assertTrue(meta.writable)
    }

    @Test fun parse_readAccess_isNotWritable() {
        val meta = assertNotNull(CatalogLine.parse(validLine))
        assertFalse(meta.writable)
    }

    @Test fun parse_signedFlagOne_mapsToTrue() {
        // signed 컬럼(15번째 필드, index 14)만 "1"로 치환
        val fields = validLine.split(" ").toMutableList()
        fields[14] = "1"
        val meta = assertNotNull(CatalogLine.parse(fields.joinToString(" ")))
        assertTrue(meta.signed)
    }

    @Test fun parse_dashPlaceholders_becomeNull() {
        val meta = assertNotNull(CatalogLine.parse(validLine))
        assertNull(meta.safe) // 원본 라인의 safe 컬럼이 "-"
    }

    @Test fun parse_typeDash_becomesNull() {
        val fields = validLine.split(" ").toMutableList()
        fields[13] = "-"
        val meta = assertNotNull(CatalogLine.parse(fields.joinToString(" ")))
        assertNull(meta.type)
    }

    @Test fun parse_tooFewColumns_returnsNull() {
        val short = validLine.split(" ").dropLast(1).joinToString(" ")
        assertNull(CatalogLine.parse(short))
    }

    @Test fun parse_tooManyColumns_returnsNull() {
        val long = "$validLine extra"
        assertNull(CatalogLine.parse(long))
    }

    @Test fun parse_emptyLine_returnsNull() {
        assertNull(CatalogLine.parse(""))
    }

    @Test fun parse_malformedId_neverThrowsAndReturnsNull() {
        val fields = validLine.split(" ").toMutableList()
        fields[4] = "not_hex"
        assertNull(CatalogLine.parse(fields.joinToString(" ")))
    }

    @Test fun parse_malformedNumericColumn_neverThrowsAndReturnsNull() {
        val fields = validLine.split(" ").toMutableList()
        fields[6] = "not_a_number" // updateMs
        assertNull(CatalogLine.parse(fields.joinToString(" ")))
    }

    @Test fun parse_idWithLowercase0xPrefix_parses() {
        val fields = validLine.split(" ").toMutableList()
        fields[4] = "0xf004"
        val meta = assertNotNull(CatalogLine.parse(fields.joinToString(" ")))
        assertEquals(0xF004L, meta.id)
    }

    @Test fun parse_idWithUppercase0XPrefix_parses() {
        val fields = validLine.split(" ").toMutableList()
        fields[4] = "0XF004"
        val meta = assertNotNull(CatalogLine.parse(fields.joinToString(" ")))
        assertEquals(0xF004L, meta.id)
    }

    @Test fun parse_naNplSafeWithoutPrefix_parses() {
        val fields = validLine.split(" ").toMutableList()
        fields[17] = "FFFF" // na, no 0x prefix
        val meta = assertNotNull(CatalogLine.parse(fields.joinToString(" ")))
        assertEquals(0xFFFFL, meta.na)
    }
}
