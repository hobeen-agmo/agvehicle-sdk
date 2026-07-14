// Hitch.kt — 히치 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 위치 읽기 (자격 불필요) — 생명주기 한 줄
//   val hitch = Hitch(this, object : Hitch.Listener {
//       override fun onPosition(p: HitchPosition) = runOnUiThread { render(p.percent) }
//   })
//   lifecycle.addObserver(hitch)
//
//   // 제어 (매니페스트에 USES_CONTROL=HITCH_CMD 선언 필요) — 읽기와 성격이 달라 세션
//   val ctrl = Hitch.control(this) ?: return    // null = 자격 없음/보유 중/미연결
//   ctrl.onLost { runOnUiThread { lockUi() } }
//   ctrl.setPosition(50.0)                       // % — raw 변환은 SDK가 숨김
//   ctrl.release()
package farm.agmo.vehicle.hitch

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Hitch(context: Context, private val listener: Listener) : Signal(context) {

    /** 히치 위치 콜백 (읽기) */
    interface Listener {
        fun onPosition(position: HitchPosition) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribe(HitchPosition.KEY) { value ->
            value.number?.let { listener.onPosition(HitchPosition(it)) }
        },
    )

    companion object {
        /** 데몬 내장 제어 신호 key */
        const val CONTROL_KEY = "HITCH_CMD"

        /**
         * 히치 제어권을 잡는다. null이면 자격 없음(매니페스트 미선언)·상위 보유 중·미연결.
         * 읽기(구독)와 달리 제어는 단일 세션(토큰·선점)이라 생명주기가 아닌 세션 핸들로 준다.
         */
        fun control(context: Context): HitchControl? {
            val v = AgVehicle.shared(context)
            var handle: HitchControl? = null
            val session = v.acquire(CONTROL_KEY) { handle?.fireLost() } ?: return null
            return HitchControl(session).also { handle = it }
        }
    }
}

class HitchControl internal constructor(
    private val session: AgVehicle.ControlSession,
) {
    private var lostCb: (() -> Unit)? = null

    /** 히치 위치 지시 (0~100%). false = 세션 상실 — Hitch.control부터 다시 */
    fun setPosition(percent: Double): Boolean =
        session.send(HitchScale.toRaw(percent))

    /** 상위 계층(비상정지 등) 선점 통지 등록 — 즉시 제어 UI를 잠글 것 */
    fun onLost(cb: () -> Unit) { lostCb = cb }

    /** 정상 반납 — 마지막 위치 유지 */
    fun release() = session.release()

    /** 안전값(내림)으로 되돌린 뒤 반납 — 자발 비상정지 */
    fun stopAndRelease() = session.stopAndRelease()

    internal fun fireLost() { lostCb?.invoke() }
}
