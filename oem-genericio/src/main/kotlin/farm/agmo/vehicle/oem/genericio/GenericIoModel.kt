// GenericIoModel.kt — 범용 CAN IO 보드 신호 키/상수 (순수 — JVM 테스트 대상).
//
// 값 출처: 범용 IO 보드 CAN 레지스터 맵(GenericIO). 특정 제조사 고유 로직 아닌 일반적 하드웨어
//   IO 추상화. 레이아웃 참조 = NEVONEX GenericIO 정의. 소유자(AGMO) 진행 지시. 소스가 NEVONEX
//   참조라 provenance 구분 위해 oem 계열(farm.agmo.vehicle.oem.genericio). 키 "genericio:<signal>".
//
// 구성: 아날로그입력 4(0x200) / 공급전압·디지털입력 4(0x201) / PWM 4(0x202, write) /
//       LED RGBW 2채널(0x203~0x204, write, 결합 신호).
package farm.agmo.vehicle.oem.genericio

object GenericIoSignals {
    const val ANALOG_IN_COUNT = 4
    const val DIGITAL_IN_COUNT = 4

    fun analogIn(channel: Int): String = "genericio:AnalogIN$channel"
    fun digitalIn(channel: Int): String = "genericio:DigitalIn$channel"

    const val SUPPLY_VOLTAGE = "genericio:AnalogSupplyVoltage"
    const val SUPPLY_5V = "genericio:AnalogSupply5V"

    /** PWM 출력 키. side: "HS"/"LS", channel 1~2 */
    fun pwm(side: String, channel: Int): String = "genericio:PWM_${side}$channel"

    /** LED 결합 명령 키. led 1~2 */
    fun led(led: Int): String = "genericio:LED$led"

    /** LED1(0x203): Channel(0|32)+R(32)+G(40)+B(48)+W(56) → 64bit 결합 raw */
    fun encodeLed1(channel: Long, r: Int, g: Int, b: Int, w: Int): Long =
        (channel and 0xFFFFFFFFL) or
            ((r.toLong() and 0xFF) shl 32) or ((g.toLong() and 0xFF) shl 40) or
            ((b.toLong() and 0xFF) shl 48) or ((w.toLong() and 0xFF) shl 56)

    /** LED2(0x204): Channel(0|8)+R(8)+G(16)+B(24)+W(32) → 40bit 결합 raw */
    fun encodeLed2(channel: Int, r: Int, g: Int, b: Int, w: Int): Long =
        (channel.toLong() and 0xFF) or
            ((r.toLong() and 0xFF) shl 8) or ((g.toLong() and 0xFF) shl 16) or
            ((b.toLong() and 0xFF) shl 24) or ((w.toLong() and 0xFF) shl 32)
}
