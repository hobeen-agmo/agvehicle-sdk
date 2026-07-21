// OemImu.kt — AGMO 제조사 고유 자이로 읽기 도메인 (Signal 상속, 생명주기 한 줄).
//
// 두 가지 사용법 (원하는 걸 고르면 됨):
//
//   // (1) 람다 — 필요한 신호만 짧게. object/override 불필요.
//   val gyro = OemImu(this, intervalMs = 200)
//   gyro.onAngle { a -> runOnUiThread { render(a.xDeg, a.yDeg, a.zDeg) } }
//   lifecycle.addObserver(gyro)
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val gyro = OemImu(this, object : OemImu.Listener {
//       override fun onAngle(a: GyroAngle) = runOnUiThread { render(a.xDeg, a.yDeg, a.zDeg) }
//       override fun onRate(r: GyroRate)   = runOnUiThread { render(r.zDegS) }
//   })
//
// 표준 자세(MTLT305 각도/각속도/가속도)는 표준 :imu 모듈을 쓴다. 이 모듈은 제조사 고유
// 보조 자이로(Allynav R70)만 다룬다 — 표준과 섞지 않는다.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.imu

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class OemImu(
    context: Context,
    private val intervalMs: Long = 0,
) : Signal(context) {

    /** 제조사 고유 자이로/IMU진단 콜백 — 필요한 메시지만 override (리스너 방식) */
    interface Listener {
        // Allynav R70 보조 자이로
        fun onAngle(angle: GyroAngle) {}
        fun onAccel(accel: GyroAccel) {}
        fun onRate(rate: GyroRate) {}
        // Aceinna MTLT305 설정/진단 (자세 데이터는 표준 :imu)
        fun onSensorStatus(status: ImuSensorStatus) {}
        fun onFirmwareVersion(fw: ImuFirmwareVersion) {}
    }

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. 내부적으로 람다 등록과 동일 경로. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onAngle(listener::onAngle); onAccel(listener::onAccel); onRate(listener::onRate)
        onSensorStatus(listener::onSensorStatus); onFirmwareVersion(listener::onFirmwareVersion)
    }

    // ── 람다 편의 API — 필요한 신호만 등록(체이닝 가능). ON_START에 실제 구독된다. ──
    fun onAngle(cb: (GyroAngle) -> Unit) = msg(GyroAngle.KEYS, GyroAngle::from, cb)
    fun onAccel(cb: (GyroAccel) -> Unit) = msg(GyroAccel.KEYS, GyroAccel::from, cb)
    fun onRate(cb: (GyroRate) -> Unit) = msg(GyroRate.KEYS, GyroRate::from, cb)
    fun onSensorStatus(cb: (ImuSensorStatus) -> Unit) = msg(ImuSensorStatus.KEYS, ImuSensorStatus::from, cb)
    fun onFirmwareVersion(cb: (ImuFirmwareVersion) -> Unit) = msg(ImuFirmwareVersion.KEYS, ImuFirmwareVersion::from, cb)

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 메시지(신호 여럿) 구독 등록 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }
}
