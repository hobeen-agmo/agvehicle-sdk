// SpreaderModel.kt — AGMO RDA_Spreader 메시지(ID)별 데이터 클래스 (순수 — JVM 테스트 대상).
//
// "CAN 메시지(ID) 하나 = 클래스 하나" 원칙. 살포기(작업기) 신호:
//   살포율 → SpreadRate(현재 kg/ha)
//   게이트 → GateStatus(개폐 위치 %)
//   섹션   → SectionStatus(섹션별 on/off 비트)
//
// ⚠️ 골격 단계(C): 키/스케일 플레이스홀더. 실제 키·raw 스케일은 (B) 깨끗한 소스
//   (AGMO 1차 CAN 스펙) 확정 후 채운다. 디컴파일 추출본 미채택(reference/README 규율). — TODO(B)
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.spreader

/** 현재 살포율 (kg/ha) */
data class SpreadRate(val kgPerHa: Double) {
    companion object {
        const val KEY = "SPREADER_RATE"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): SpreadRate? =
            v[KEY]?.let { SpreadRate(it) }
    }
}

/** 게이트 개폐 위치 (0~100%) */
data class GateStatus(val openPercent: Double) {
    companion object {
        const val KEY = "SPREADER_GATE"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): GateStatus? =
            v[KEY]?.let { GateStatus(it) }
    }
}

/** 섹션 on/off 상태 (비트마스크 — 섹션 i가 켜졌으면 bit i=1) */
data class SectionStatus(val mask: Long) {
    fun isOn(section: Int): Boolean = section in 0..63 && (mask shr section) and 1L == 1L

    companion object {
        const val KEY = "SPREADER_SECTIONS"   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): SectionStatus? =
            v[KEY]?.let { SectionStatus(it.toLong()) }
    }
}
