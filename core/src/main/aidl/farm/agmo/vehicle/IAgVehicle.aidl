// IAgVehicle.aidl — 앱↔서비스 단일 진입 인터페이스 (bindService가 돌려주는 바인더)
//
// 외부 앱은 이 한 인터페이스만 본다. 서비스는 내부에서 uid로 호출자를 식별하고
// (Binder.getCallingUid — 위조 불가) PolicyEngine으로 라우팅한다.
// 읽기/쓰기/정의를 한 면에 모은 이유: bindService는 바인더 하나를 반환하고,
// 앱 대면 공격면은 어차피 uid 게이트로 막으므로 인터페이스를 쪼갤 실익이 적다.
package farm.agmo.vehicle;

import farm.agmo.vehicle.IVehicleCallback;

interface IAgVehicle {
    // ── 접속 ──
    /**
     * 콜백 등록 + 자격 판정. 충돌 차단(1차 방어) 시 false.
     * 자격 선언(uses-control 등)은 여기서 받지 않는다 — 앱 매니페스트 <meta-data>에서
     * 서비스가 PackageManager로 직접 읽는다(자기 신고 방지). 키:
     *   farm.agmo.vehicle.USES_CONTROL / OPT_CONTROL / CONFLICT_WITH (쉼표 구분)
     */
    boolean attach(in IVehicleCallback cb);

    // ── 읽기 ──
    void subscribe(String key);
    void unsubscribe(String key);
    /** 신호 카탈로그 요청 → onCatalog 청크 콜백 */
    void requestCatalog();

    // ── 쓰기(제어) ──
    /** 세션 획득. 성공 시 토큰(비0), 자격 없음/보유 중이면 0 */
    long acquire(String key);
    /** 명령. 토큰 불일치·비보유자는 서비스가 즉시 거부(false) */
    boolean command(String key, long value, long token);
    /** 정상 반납 — 마지막값 유지 */
    void release(String key, long token);
    /** 안전값 강제 송신 후 반납 — 앱 자발 비상정지 */
    void stopAndRelease(String key, long token);

    // ── 외부 CAN 정의(설치 시 1회) ──
    /** 자기 CAN 신호 정의(JSON) 등록. 반환: 수락 신호 수. 상세는 onDefsResult */
    int registerDefs(String defsJson);
    void unregisterDefs();
}
