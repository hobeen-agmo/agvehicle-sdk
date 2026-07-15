// TractorModel.kt — AGMO Customized Tractor 메시지별 데이터 (순수 — JVM 테스트 대상).
//
// 값 출처: AGMO 1차 설계(NEVONEX Machine Interface Requirement, Customer=AGMO Inc.,
//   Requestor Dongseok Choi 2025-02-06) + AGMO_CustomizedTractor.dbc. 데몬 카탈로그 키는
//   "agmo_customized_tractor:<signal>". 데몬이 스케일 적용한 물리값을 전달한다.
//
// 상태 읽기(0x4xx): FNR/RangeShift(SFT)/PTO/Hydraulic(HYD)/Accelerator(ACC).
//   각 메시지에 State/Mode(auto) + 진단 전압(SIG1_V/SIG2_V) 포함.
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.tractor

private const val APP = "agmo_customized_tractor"

/** 전후진(FNR) 방향 — 문서: 1=R, 2=N, 3=F */
enum class FnrDirection(val code: Int) {
    REVERSE(1), NEUTRAL(2), FORWARD(3), UNKNOWN(-1);
    companion object { fun of(c: Int) = entries.firstOrNull { it.code == c } ?: UNKNOWN }
}

/** 변속 레인지(RangeShift/SFT) — 문서: 1=Low, 2=Mid, 3=High */
enum class RangeGear(val code: Int) {
    LOW(1), MID(2), HIGH(3), UNKNOWN(-1);
    companion object { fun of(c: Int) = entries.firstOrNull { it.code == c } ?: UNKNOWN }
}

/** PTO 상태 — 문서: 1=OFF, 2=ON, 3=Auto ON */
enum class PtoMode(val code: Int) {
    OFF(1), ON(2), AUTO_ON(3), UNKNOWN(-1);
    companion object { fun of(c: Int) = entries.firstOrNull { it.code == c } ?: UNKNOWN }
}

/** 전후진 상태 (0x410) — 방향 + auto 모드 여부 */
data class Fnr(val direction: FnrDirection, val auto: Boolean) {
    companion object {
        val KEYS = listOf("$APP:TRZ_FNR_STATE", "$APP:TRZ_FNR_AUTO")
        fun from(v: Map<String, Double>): Fnr? {
            val s = v["$APP:TRZ_FNR_STATE"]; val a = v["$APP:TRZ_FNR_AUTO"]
            return if (s != null && a != null) Fnr(FnrDirection.of(s.toInt()), a != 0.0) else null
        }
    }
}

/** 변속 레인지 상태 (0x420) */
data class RangeShift(val gear: RangeGear, val auto: Boolean) {
    companion object {
        val KEYS = listOf("$APP:TRZ_SFT_STATE", "$APP:TRZ_SFT_AUTO")
        fun from(v: Map<String, Double>): RangeShift? {
            val s = v["$APP:TRZ_SFT_STATE"]; val a = v["$APP:TRZ_SFT_AUTO"]
            return if (s != null && a != null) RangeShift(RangeGear.of(s.toInt()), a != 0.0) else null
        }
    }
}

/** PTO 상태 (0x430) */
data class Pto(val mode: PtoMode, val auto: Boolean) {
    companion object {
        val KEYS = listOf("$APP:TRZ_PTO_STATE", "$APP:TRZ_PTO_AUTO")
        fun from(v: Map<String, Double>): Pto? {
            val s = v["$APP:TRZ_PTO_STATE"]; val a = v["$APP:TRZ_PTO_AUTO"]
            return if (s != null && a != null) Pto(PtoMode.of(s.toInt()), a != 0.0) else null
        }
    }
}

/** 유압/히치 피드백 (0x480) — 슬라이드 전압(V) + auto. 목표 위치%는 제어(setHitch) 쪽. */
data class Hydraulic(val sig1V: Double, val sig2V: Double, val auto: Boolean) {
    companion object {
        val KEYS = listOf("$APP:TRZ_HYD_SIG1_V", "$APP:TRZ_HYD_SIG2_V", "$APP:TRZ_HYD_AUTO")
        fun from(v: Map<String, Double>): Hydraulic? {
            val s1 = v["$APP:TRZ_HYD_SIG1_V"]; val s2 = v["$APP:TRZ_HYD_SIG2_V"]; val a = v["$APP:TRZ_HYD_AUTO"]
            return if (s1 != null && s2 != null && a != null) Hydraulic(s1, s2, a != 0.0) else null
        }
    }
}

/** 가속 피드백 (0x490) — 페달 전압(V) + auto */
data class Accelerator(val sig1V: Double, val sig2V: Double, val auto: Boolean) {
    companion object {
        val KEYS = listOf("$APP:TRZ_ACC_SIG1_V", "$APP:TRZ_ACC_SIG2_V", "$APP:TRZ_ACC_AUTO")
        fun from(v: Map<String, Double>): Accelerator? {
            val s1 = v["$APP:TRZ_ACC_SIG1_V"]; val s2 = v["$APP:TRZ_ACC_SIG2_V"]; val a = v["$APP:TRZ_ACC_AUTO"]
            return if (s1 != null && s2 != null && a != null) Accelerator(s1, s2, a != 0.0) else null
        }
    }
}

/** 제어(WRITE) 신호 키 — 각 *_CMD 값 신호(byte0). mode(byte1)는 데몬 기본 0=Manual. */
internal object TractorControlKeys {
    const val FNR = "$APP:AD_FNR_CMD"
    const val RANGE_SHIFT = "$APP:AD_SFT_CMD"
    const val PTO = "$APP:AD_PTO_CMD"
    const val HITCH = "$APP:AD_HYD_CMD"       // 유압 히치 슬라이드 0~100%
    const val ACCELERATOR = "$APP:AD_ACC_CMD"
}
