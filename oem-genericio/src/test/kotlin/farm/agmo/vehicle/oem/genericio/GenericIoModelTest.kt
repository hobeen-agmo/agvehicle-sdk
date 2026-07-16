// GenericIoModelTest.kt — 순수 로직 유닛테스트: LED 결합 raw 비트 인코딩 극한 경계(0/0xFF/오버플로/음수).
package farm.agmo.vehicle.oem.genericio

import kotlin.test.Test
import kotlin.test.assertEquals

class GenericIoSignalsKeyTest {
    @Test fun analogIn_formatsWithNamespacePrefix() {
        assertEquals("genericio:AnalogIN1", GenericIoSignals.analogIn(1))
    }

    @Test fun digitalIn_formatsWithNamespacePrefix() {
        assertEquals("genericio:DigitalIn1", GenericIoSignals.digitalIn(1))
    }

    @Test fun pwm_highSideAndLowSide_formatDistinctly() {
        assertEquals("genericio:PWM_HS1", GenericIoSignals.pwm("HS", 1))
        assertEquals("genericio:PWM_LS2", GenericIoSignals.pwm("LS", 2))
    }

    @Test fun led_formatsWithNamespacePrefix() {
        assertEquals("genericio:LED1", GenericIoSignals.led(1))
        assertEquals("genericio:LED2", GenericIoSignals.led(2))
    }
}

class EncodeLed1Test {
    @Test fun encodeLed1_allZero_isZero() {
        assertEquals(0L, GenericIoSignals.encodeLed1(0L, 0, 0, 0, 0))
    }

    @Test fun encodeLed1_channelMaxAndAllBytesMax_setsAllSixtyFourBits() {
        assertEquals(-1L, GenericIoSignals.encodeLed1(0xFFFFFFFFL, 0xFF, 0xFF, 0xFF, 0xFF))
    }

    @Test fun encodeLed1_channelOverflowsThirtyTwoBits_isMaskedToLower32() {
        // channel and 0xFFFFFFFFL 이므로 33번째 비트는 사라진다
        assertEquals(0L, GenericIoSignals.encodeLed1(0x100000000L, 0, 0, 0, 0))
    }

    @Test fun encodeLed1_byteOverflow256_wrapsToZero() {
        // r.toLong() and 0xFF 이므로 256(0x100)은 0으로 마스킹된다
        assertEquals(0L, GenericIoSignals.encodeLed1(0L, 256, 0, 0, 0))
    }

    @Test fun encodeLed1_negativeByte_masksToMaxByteValue() {
        // (-1).toLong() and 0xFF == 0xFF — 부호 확장 후 하위 바이트만 남는다
        val encoded = GenericIoSignals.encodeLed1(0L, -1, 0, 0, 0)
        assertEquals(0xFFL shl 32, encoded)
    }

    @Test fun encodeLed1_eachChannelOccupiesDistinctByteLane() {
        val r = GenericIoSignals.encodeLed1(0L, 0xAB, 0, 0, 0)
        val g = GenericIoSignals.encodeLed1(0L, 0, 0xAB, 0, 0)
        val b = GenericIoSignals.encodeLed1(0L, 0, 0, 0xAB, 0)
        val w = GenericIoSignals.encodeLed1(0L, 0, 0, 0, 0xAB)
        assertEquals(0xABL shl 32, r)
        assertEquals(0xABL shl 40, g)
        assertEquals(0xABL shl 48, b)
        assertEquals(0xABL shl 56, w)
    }
}

class EncodeLed2Test {
    @Test fun encodeLed2_allZero_isZero() {
        assertEquals(0L, GenericIoSignals.encodeLed2(0, 0, 0, 0, 0))
    }

    @Test fun encodeLed2_allFieldsMax_setsAllFortyBits() {
        assertEquals(0xFFFFFFFFFFL, GenericIoSignals.encodeLed2(0xFF, 0xFF, 0xFF, 0xFF, 0xFF))
    }

    @Test fun encodeLed2_channelOverflow256_wrapsToZero() {
        assertEquals(0L, GenericIoSignals.encodeLed2(256, 0, 0, 0, 0))
    }

    @Test fun encodeLed2_negativeByte_masksToMaxByteValue() {
        val encoded = GenericIoSignals.encodeLed2(0, -1, 0, 0, 0)
        assertEquals(0xFFL shl 8, encoded)
    }

    @Test fun encodeLed2_eachFieldOccupiesDistinctByteLane() {
        val ch = GenericIoSignals.encodeLed2(0xAB, 0, 0, 0, 0)
        val r = GenericIoSignals.encodeLed2(0, 0xAB, 0, 0, 0)
        val g = GenericIoSignals.encodeLed2(0, 0, 0xAB, 0, 0)
        val b = GenericIoSignals.encodeLed2(0, 0, 0, 0xAB, 0)
        val w = GenericIoSignals.encodeLed2(0, 0, 0, 0, 0xAB)
        assertEquals(0xABL, ch)
        assertEquals(0xABL shl 8, r)
        assertEquals(0xABL shl 16, g)
        assertEquals(0xABL shl 24, b)
        assertEquals(0xABL shl 32, w)
    }
}
