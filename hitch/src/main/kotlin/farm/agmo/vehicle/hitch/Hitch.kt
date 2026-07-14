// Hitch.kt — 히치 제어 도메인 모듈 (타입 있는 파사드, core 위에 얹힘).
//
//   val hitch = Hitch.control(context) ?: return   // null = 자격 없음/보유 중/미연결
//   hitch.setPosition(50.0)                         // % — raw 변환은 SDK가 숨김
//   hitch.onLost { lockUi() }
//   hitch.release()
//
// 자격 선언은 앱 매니페스트에 필요:
//   <meta-data android:name="farm.agmo.vehicle.USES_CONTROL" android:value="HITCH_CMD"/>
package farm.agmo.vehicle.hitch

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle

object Hitch {
    /** 데몬 내장 제어 신호 key (agcand signal_defs.cpp) */
    const val KEY = "HITCH_CMD"

    /**
     * 히치 제어권을 잡는다. null이면 자격 없음(매니페스트 미선언)·상위 보유 중·미연결.
     * 반환된 핸들로 위치를 %로 지시한다.
     */
    fun control(context: Context): HitchControl? {
        val v = AgVehicle.shared(context)
        // 선점 통지는 acquire 시점에 등록해야 하므로, 아직 안 만들어진 핸들의 콜백을
        // 지연 참조로 넘긴다(핸들 생성 직후 배선 완료).
        var handle: HitchControl? = null
        val session = v.acquire(KEY) { handle?.fireLost() } ?: return null
        return HitchControl(session).also { handle = it }
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
