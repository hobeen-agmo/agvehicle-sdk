// Hitch.kt — 히치 도메인 (읽기: Android LocationManager 패턴 / 쓰기: 제어 세션).
//
//   val client = Hitch.getClient(context)
//
//   // 위치 읽기 (자격 불필요)
//   val cb = object : HitchCallback() {
//       override fun onPosition(p: HitchPosition) { render(p.percent) }
//   }
//   client.requestUpdates(cb)
//   client.removeUpdates(cb)
//   val last = client.lastPosition
//
//   // 제어 (매니페스트에 USES_CONTROL=HITCH_CMD 선언 필요) — 읽기와 성격이 달라 세션으로
//   val ctrl = client.acquireControl() ?: return   // null = 자격 없음/보유 중/미연결
//   ctrl.onLost { lockUi() }
//   ctrl.setPosition(50.0)                          // % — raw 변환은 SDK가 숨김
//   ctrl.release()
package farm.agmo.vehicle.hitch

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import java.util.concurrent.ConcurrentHashMap

/** 히치 위치 콜백 (읽기) */
abstract class HitchCallback {
    open fun onPosition(position: HitchPosition) {}
}

object Hitch {
    fun getClient(context: Context): HitchClient = HitchClient(AgVehicle.shared(context))
}

class HitchClient internal constructor(private val v: AgVehicle) {
    private val regs = ConcurrentHashMap<HitchCallback, AgVehicle.Subscription>()

    @Volatile var lastPosition: HitchPosition? = null; private set

    /** 위치(%) 갱신 시작. 자격 불필요(읽기). */
    fun requestUpdates(callback: HitchCallback) {
        if (regs.containsKey(callback)) return
        regs[callback] = v.subscribe(HitchPosition.KEY) { value ->
            value.number?.let { p -> val pos = HitchPosition(p); lastPosition = pos; callback.onPosition(pos) }
        }
    }

    fun removeUpdates(callback: HitchCallback) {
        regs.remove(callback)?.close()
    }

    /**
     * 히치 제어권을 잡는다. null이면 자격 없음(매니페스트 미선언)·상위 보유 중·미연결.
     * 읽기(구독)와 달리 제어는 단일 세션이라 콜백 등록이 아닌 세션 핸들로 준다.
     */
    fun acquireControl(): HitchControl? {
        var handle: HitchControl? = null
        val session = v.acquire(HITCH_CMD) { handle?.fireLost() } ?: return null
        return HitchControl(session).also { handle = it }
    }

    private companion object { const val HITCH_CMD = "HITCH_CMD" }
}

class HitchControl internal constructor(
    private val session: AgVehicle.ControlSession,
) {
    private var lostCb: (() -> Unit)? = null

    /** 히치 위치 지시 (0~100%). false = 세션 상실 — acquireControl부터 다시 */
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
