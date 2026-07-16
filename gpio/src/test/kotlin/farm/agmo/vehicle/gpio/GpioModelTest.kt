// GpioModelTest.kt — 순수 로직 유닛테스트: 채널 키 조립 경계(1/최대/0/음수) + KEYS 리스트 일관성.
package farm.agmo.vehicle.gpio

import kotlin.test.Test
import kotlin.test.assertEquals

class GpioSignalsTest {
    @Test fun analogIn_channelOne_isFirstKey() {
        assertEquals("Analog_IN1", GpioSignals.analogIn(1))
    }

    @Test fun analogIn_channelAtCount_isLastKey() {
        assertEquals("Analog_IN${GpioSignals.ANALOG_IN_COUNT}", GpioSignals.analogIn(GpioSignals.ANALOG_IN_COUNT))
    }

    @Test fun analogIn_channelZero_stillFormats_noRangeCheck() {
        // 함수 자체는 범위 검증을 하지 않음(문서화된 동작) — 호출자가 KEYS 상수로 범위를 지킨다
        assertEquals("Analog_IN0", GpioSignals.analogIn(0))
    }

    @Test fun analogIn_negativeChannel_stillFormats() {
        assertEquals("Analog_IN-1", GpioSignals.analogIn(-1))
    }

    @Test fun digitalIn_channelOne_isFirstKey() {
        assertEquals("Digital_IN1", GpioSignals.digitalIn(1))
    }

    @Test fun digitalIn_channelAtCount_isLastKey() {
        assertEquals("Digital_IN${GpioSignals.DIGITAL_IN_COUNT}", GpioSignals.digitalIn(GpioSignals.DIGITAL_IN_COUNT))
    }

    @Test fun digitalOut_channelOne_isFirstKey() {
        assertEquals("Digital_OUT1", GpioSignals.digitalOut(1))
    }

    @Test fun digitalOut_channelAtCount_isLastKey() {
        assertEquals("Digital_OUT${GpioSignals.DIGITAL_OUT_COUNT}", GpioSignals.digitalOut(GpioSignals.DIGITAL_OUT_COUNT))
    }

    @Test fun analogInKeys_coversOneThroughCountInOrder() {
        assertEquals((1..GpioSignals.ANALOG_IN_COUNT).map { "Analog_IN$it" }, GpioSignals.ANALOG_IN_KEYS)
    }

    @Test fun digitalInKeys_coversOneThroughCountInOrder() {
        assertEquals((1..GpioSignals.DIGITAL_IN_COUNT).map { "Digital_IN$it" }, GpioSignals.DIGITAL_IN_KEYS)
    }
}
