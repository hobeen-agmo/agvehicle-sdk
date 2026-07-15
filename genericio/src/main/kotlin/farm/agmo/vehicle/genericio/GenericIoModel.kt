// GenericIoModel.kt — 범용 CAN IO 보드 신호 키/상수 (순수 — JVM 테스트 대상).
//
// 값 출처: 범용 IO 보드 CAN 레지스터 맵(레이아웃 참조 = NEVONEX GenericIO 정의). 범용 하드웨어
//   추상화라 표준/플랫폼 계열(farm.agmo.vehicle.genericio). 데몬 카탈로그 키 "genericio:<signal>".
//
// 구성: 아날로그입력 4(0x200) / 공급전압·디지털입력 4(0x201) / PWM 4(0x202, write) /
//       LED RGBW 2채널(0x203~0x204, write). SDK는 아날로그/디지털 읽기 + PWM 제어를 노출한다.
package farm.agmo.vehicle.genericio

object GenericIoSignals {
    const val ANALOG_IN_COUNT = 4
    const val DIGITAL_IN_COUNT = 4

    fun analogIn(channel: Int): String = "genericio:AnalogIN$channel"
    fun digitalIn(channel: Int): String = "genericio:DigitalIn$channel"

    const val SUPPLY_VOLTAGE = "genericio:AnalogSupplyVoltage"
    const val SUPPLY_5V = "genericio:AnalogSupply5V"

    /** PWM 출력 키. side: "HS"(high-side)/"LS"(low-side), channel 1~2 */
    fun pwm(side: String, channel: Int): String = "genericio:PWM_${side}$channel"
}
