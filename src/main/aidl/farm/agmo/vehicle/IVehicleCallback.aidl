// IVehicleCallback.aidl — 서비스→앱 하행 콜백.
// oneway: 서비스가 느린 앱을 기다리지 않는다(데몬 MSG_DONTWAIT와 같은 원칙).
package farm.agmo.vehicle;

oneway interface IVehicleCallback {
    /** 구독 신호 값. quality: OK | DISCONNECTED | IMPLAUSIBLE */
    void onValue(String key, String text, String quality);
    /** 신호 단위 staleness — timeout_count×update_ms 창 초과 */
    void onStale(String key);
    /** 장치(function 코드) presence 변화 */
    void onDevice(int function, boolean present);
    /** 제어 세션이 상위 계층에 선점당함 — 즉시 제어 UI 잠글 것 */
    void onControlLost(String key);
    /**
     * requestCatalog 응답 — 21컬럼 평문 라인, 청크 분할(done=true가 마지막).
     * 청크 이유: oneway 콜백은 프로세스당 async 버퍼가 작아 신호 수백 개를
     * 한 트랜잭션에 실으면 TransactionTooLargeException으로 유실된다.
     */
    void onCatalog(in List<String> chunk, boolean done);
    /** registerDefs 결과 — 수락 수와 라인별 오류 */
    void onDefsResult(int accepted, in List<String> errors);
}
