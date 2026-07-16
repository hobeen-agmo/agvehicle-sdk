// AutoLift.kt — 앱 레벨 자동 히치 상승 (별도 CAN 신호가 아니라 실존 신호 조합).
//
//   Turn : 조향각 |STEERANGLE|(SPN 1807, VDC2 0xF009, rad)이 임계 초과 → 히치 상승
//   Rev  : FNR(TRZ_FNR_STATE) == Reverse → 히치 상승
//   상승 : 실존 유압 히치 제어(AD_HYD_CMD, HitchControl.setPosition)로 100% 상승.
//
// 조향각/FNR을 구독하다가 조건 성립 시 히치를 1회 상승(중복 억제)한다. 두 조건은 독립
// 토글(onTurn/onReverse)이며 OR로 합쳐진다. Tractor.autoLift(context)로 생성.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스.
package farm.agmo.vehicle.oem.tractor

import farm.agmo.vehicle.sdk.AgVehicle
import kotlin.math.abs

class AutoLift internal constructor(
    private val vehicle: AgVehicle,
    private val hitch: HitchControl,
) {
    /** 선회 시 자동 상승 사용 여부 */
    @Volatile var onTurn: Boolean = false
    /** 후진 시 자동 상승 사용 여부 */
    @Volatile var onReverse: Boolean = false

    private val subs = mutableListOf<AgVehicle.Subscription>()
    private var steerRad: Double? = null
    private var reverse = false
    private var raised = false            // 이미 상승 명령을 낸 상태 — 중복 억제

    /** 조향각/FNR 구독 시작. 중복 호출 안전. */
    fun start() {
        if (subs.isNotEmpty()) return
        subs += vehicle.subscribe(STEER_KEY) { v ->
            v.number?.let { steerRad = it; evaluate() }
        }
        subs += vehicle.subscribeMessage(Fnr.KEYS) { m ->
            Fnr.from(m)?.let { reverse = it.direction == FnrDirection.REVERSE; evaluate() }
        }
    }

    /** 구독 해제 (재무장 — 다음 start에서 다시 트리거 가능). */
    fun stop() {
        subs.forEach { it.close() }
        subs.clear()
        raised = false
    }

    /** 구독 해제 + 히치 제어권 반납. 완전 종료(onStop) 시 호출. */
    fun release() {
        stop()
        hitch.release()
    }

    /** 조향각/FNR 갱신마다 트리거 평가 — 조건 성립 & 미상승이면 1회 상승. */
    private fun evaluate() {
        val trigger = shouldTrigger(steerRad, reverse, onTurn, onReverse)
        when {
            trigger && !raised -> if (hitch.setPosition(RAISE_PERCENT)) raised = true
            !trigger           -> raised = false   // 조건 해제 시 재무장
        }
    }

    companion object {
        /** 표준 J1939 조향핸들각 (SPN 1807, VDC2 0xF009) — 데몬 내장 실존 신호. */
        const val STEER_KEY = "STEERANGLE"

        /**
         * 상승 트리거 판정 — 입력을 인자로 받는 순수 함수(AgVehicle/HitchControl 의존 없음, 테스트 대상).
         * OR: (onTurn && |steerRad| > TURN_THRESHOLD_RAD) || (onReverse && reverse)
         */
        internal fun shouldTrigger(steerRad: Double?, reverse: Boolean, onTurn: Boolean, onReverse: Boolean): Boolean {
            val turnTrig = onTurn && (steerRad?.let { abs(it) > TURN_THRESHOLD_RAD } ?: false)
            val revTrig = onReverse && reverse
            return turnTrig || revTrig
        }

        // 선회 판정 임계(rad). STEERANGLE은 조향'핸들'각이라 실차 조향비(≈15~20:1)에 따라
        // 노면 바퀴각과 다르다. 기본값은 바퀴각 ~20° 상당의 보수적 값.
        // debt: 실차 조향비로 임계 재보정(또는 oem-steer Keya 엔코더 실각으로 교체), 필드테스트 시점
        const val TURN_THRESHOLD_RAD = 0.35

        /** 상승 목표 위치(%) — 유압 히치 최대 상승. */
        const val RAISE_PERCENT = 100.0
    }
}
