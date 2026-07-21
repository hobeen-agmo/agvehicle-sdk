// Steer.kt — AGMO 조향모터(Keya KY170) 도메인 (읽기: Signal 상속).
//
// 두 가지 사용법 (원하는 걸 고르면 됨):
//
//   // (1) 람다 — 필요한 신호만 짧게. object/override 불필요.
//   val steer = Steer(this, intervalMs = 200)
//   steer.onMotorStatus { s -> runOnUiThread { if (s.fault) warn(s) } }
//   lifecycle.addObserver(steer)
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val steer = Steer(this, object : Steer.Listener {
//       override fun onMotorStatus(s: SteerMotorStatus) = runOnUiThread { if (s.fault) warn(s) }
//   }, intervalMs = 200)
//   lifecycle.addObserver(steer)
//
// intervalMs: 앱 콜백 최소 간격(ms, 0=전부 전달). 자격 선언 불필요(읽기).
//
// ✅ 지원: 조향모터 상태/결함(Motor_Heartbeat) + 조향각/속도/온도/전압(Motor_Response SDO, mux).
// ⏸ 보류: 조향 명령 — Keya SDO 명령(Motor_Request 0x06000001)의 실제 인코딩(멀티플렉서 코드별
//   payload)은 정식 명령 레이아웃 확보 후 추가. 여기선 읽기만 노출.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.steer

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Steer(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** 조향모터 콜백 — 필요한 메시지만 override. SDO 값은 각기 다른 시점에 도착. */
    interface Listener {
        fun onMotorStatus(status: SteerMotorStatus) {}
        fun onAngle(angle: SteerAngle) {}
        fun onSpeed(speed: SteerSpeed) {}
        fun onTemperature(temperature: SteerTemperature) {}
        fun onVoltage(voltage: SteerVoltage) {}
    }

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. 내부적으로 람다 등록과 동일 경로. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onMotorStatus(listener::onMotorStatus); onAngle(listener::onAngle); onSpeed(listener::onSpeed)
        onTemperature(listener::onTemperature); onVoltage(listener::onVoltage)
    }

    // ── 람다 편의 API — 필요한 신호만 등록(체이닝 가능). ON_START에 실제 구독된다. ──
    fun onMotorStatus(cb: (SteerMotorStatus) -> Unit) = msg(SteerMotorStatus.KEYS, SteerMotorStatus::from, cb)
    fun onAngle(cb: (SteerAngle) -> Unit) = value(SteerAngle.KEY, ::SteerAngle, cb)
    fun onSpeed(cb: (SteerSpeed) -> Unit) = value(SteerSpeed.KEY, ::SteerSpeed, cb)
    fun onTemperature(cb: (SteerTemperature) -> Unit) = value(SteerTemperature.KEY, ::SteerTemperature, cb)
    fun onVoltage(cb: (SteerVoltage) -> Unit) = value(SteerVoltage.KEY, ::SteerVoltage, cb)

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 단일 신호(숫자) 구독 — number를 wrap으로 감싸 콜백. */
    private fun <T> value(key: String, wrap: (Double) -> T, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribe(key, intervalMs) { it.number?.let { n -> cb(wrap(n)) } } }
    }

    /** 메시지(신호 여럿) 구독 등록 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }
}
