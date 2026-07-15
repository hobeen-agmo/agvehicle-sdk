// OemImuModel.kt — AGMO 제조사 고유 자이로(Allynav_R70) 메시지별 데이터 (순수 — JVM 테스트 대상).
//
// 표준 :imu 모듈(각도/각속도/가속도, MTLT305 데이터 PGN)로 커버되지 않는 제조사 고유
// 신호만 여기 둔다:
//   자이로 방위/요레이트 → GyroHeading (Allynav_R70 proprietary)
//   (MTLT305 설정 PGN은 설정 write 성격 — 필요 시 OemImu.config 로 확장)
//
// ⚠️ 골격 단계(C): 키/스케일 플레이스홀더. Allynav_R70은 proprietary 프레임(0xFFCA~)이라
//   실제 키·스케일은 (B) 깨끗한 소스 확정 후 채운다. 디컴파일 추출본 미채택. — TODO(B)
//
// 🏭 AGMO 제조사 고유 (proprietary) — 표준 아님. oem 네임스페이스. docs/sdk-conventions.md 참조.
package farm.agmo.vehicle.oem.imu

/** 자이로 방위·요레이트 (deg, deg/s) — Allynav_R70 proprietary */
data class GyroHeading(val headingDeg: Double, val yawRateDegS: Double) {
    companion object {
        val KEYS = listOf("OEM_GYRO_HEADING", "OEM_GYRO_YAWRATE")   // TODO(B): 실제 키 확정
        fun from(v: Map<String, Double>): GyroHeading? {
            val h = v["OEM_GYRO_HEADING"]; val y = v["OEM_GYRO_YAWRATE"]
            return if (h != null && y != null) GyroHeading(h, y) else null
        }
    }
}
