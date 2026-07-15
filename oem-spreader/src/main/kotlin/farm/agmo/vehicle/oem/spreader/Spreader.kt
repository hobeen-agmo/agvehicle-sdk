// Spreader.kt — AGMO RDA_Spreader 도메인 (읽기: Signal 상속 / 쓰기: 제어 세션).
//
//   // 상태 읽기 (자격 불필요) — 생명주기 한 줄
//   val spreader = Spreader(this, object : Spreader.Listener {
//       override fun onRate(r: SpreadRate)   = runOnUiThread { render(r.kgPerHa) }
//       override fun onGate(g: GateStatus)   = runOnUiThread { render(g.openPercent) }
//   })
//   lifecycle.addObserver(spreader)
//
//   // 살포율 제어 — 매니페스트에 USES_CONTROL 선언 필요
//   val ctrl = Spreader.rateControl(this) ?: return
//   ctrl.setRate(120.0)   // kg/ha — raw 변환은 (B) 확정 후 SDK가 숨김
//   ctrl.release()
//
// ⚠️ 골격 단계(C): 키/스케일/제어 프레임 플레이스홀더(TODO(B)).
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.spreader

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Spreader(context: Context, private val listener: Listener) : Signal(context) {

    /** 살포기 상태 콜백 — 필요한 메시지만 override */
    interface Listener {
        fun onRate(rate: SpreadRate) {}
        fun onGate(gate: GateStatus) {}
        fun onSections(sections: SectionStatus) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribe(SpreadRate.KEY)   { it.number?.let { v -> listener.onRate(SpreadRate(v)) } },
        vehicle.subscribe(GateStatus.KEY)   { it.number?.let { v -> listener.onGate(GateStatus(v)) } },
        vehicle.subscribe(SectionStatus.KEY) { it.number?.let { v -> listener.onSections(SectionStatus(v.toLong())) } },
    )

    companion object {
        // 살포율 제어 키 — SDK 호환 위해 placeholder 이름 유지, 내부 프레임은 (B) 매핑.
        const val RATE_CONTROL_KEY = "SPREADER_RATE_CMD"   // TODO(B): 데몬 제어 신호 키 확정

        /** 살포율 제어권. null = 자격 없음/보유 중/미연결 */
        fun rateControl(context: Context): SpreaderRateControl? {
            val v = AgVehicle.shared(context)
            var handle: SpreaderRateControl? = null
            val session = v.acquire(RATE_CONTROL_KEY) { handle?.fireLost() } ?: return null
            return SpreaderRateControl(session).also { handle = it }
        }
    }
}

/** 살포율(kg/ha) 제어 — HitchControl과 같은 세션 구조 */
class SpreaderRateControl internal constructor(private val session: AgVehicle.ControlSession) {
    private var lostCb: (() -> Unit)? = null

    /** 목표 살포율 지시 (kg/ha). false = 세션 상실 */
    fun setRate(kgPerHa: Double): Boolean = session.send(toRaw(kgPerHa))

    fun onLost(cb: () -> Unit) { lostCb = cb }
    fun release() = session.release()
    fun stopAndRelease() = session.stopAndRelease()
    internal fun fireLost() { lostCb?.invoke() }

    // debt: 살포율→raw 변환이 플레이스홀더(항등·반올림)다. 트리거: (B) 깨끗한 소스로
    //   실 resolution/오프셋 확정 시 교체.
    private fun toRaw(kgPerHa: Double): Long = Math.round(kgPerHa)   // TODO(B): 실제 스케일 적용
}
