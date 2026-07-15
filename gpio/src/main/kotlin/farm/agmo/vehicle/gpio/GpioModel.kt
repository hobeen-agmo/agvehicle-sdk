// GpioModel.kt — GPIO 신호 키/상수 (순수 — JVM 테스트 대상).
//
// 값 출처: SeamOS GPIO_Prototyping 플러그인(Machine ID 7576) 인터페이스 이관.
//   In  Analog_IN1~4 (FLOAT, 10ms)  — ADC 입력
//   In  Digital_IN1~3 (BOOL, 10ms)   — DIO 디지털 입력
//   Out Digital_OUT1~7 (BOOL)        — DIO 디지털 출력
// GPIO는 범용 하드웨어 능력이라 표준 계열(farm.agmo.vehicle.gpio)에 둔다.
// 데몬 카탈로그 키 = 신호명 그대로("Analog_IN1" 등).
package farm.agmo.vehicle.gpio

object GpioSignals {
    const val ANALOG_IN_COUNT = 4
    const val DIGITAL_IN_COUNT = 3
    const val DIGITAL_OUT_COUNT = 7

    /** 아날로그 입력 키 (channel 1~4) */
    fun analogIn(channel: Int): String = "Analog_IN$channel"
    /** 디지털 입력 키 (channel 1~3) */
    fun digitalIn(channel: Int): String = "Digital_IN$channel"
    /** 디지털 출력 키 (channel 1~7) */
    fun digitalOut(channel: Int): String = "Digital_OUT$channel"

    val ANALOG_IN_KEYS = (1..ANALOG_IN_COUNT).map(::analogIn)
    val DIGITAL_IN_KEYS = (1..DIGITAL_IN_COUNT).map(::digitalIn)
}
