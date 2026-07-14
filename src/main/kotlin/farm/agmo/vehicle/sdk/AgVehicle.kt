// AgVehicle.kt — 앱 개발자용 SDK 진입점 (bindService 기반).
//
// 왜 bindService인가: 외부(마켓) 앱은 platform 서명이 없어 ServiceManager(hidden API)를
// 쓸 수 없다. bindService는 공개 API라 어떤 앱이든 쓸 수 있고, 앱 프로세스 생명주기와
// 묶여 연결/해제가 자동 관리된다.
//
// 사용 (자격 선언은 앱 매니페스트 <meta-data>에 — 코드 선언 불가, 위조 방지):
//   <meta-data android:name="farm.agmo.vehicle.USES_CONTROL" android:value="HITCH_CMD"/>
//
//   val vehicle = AgVehicle.bind(context, object : AgVehicle.Listener {
//       override fun onConnected() { vehicle.subscribe("ENGTEMP") }
//       override fun onValue(v: SignalValue) { textView.text = v.text }
//   })
//   // ...
//   vehicle.close()   // onDestroy에서
package farm.agmo.vehicle.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import farm.agmo.vehicle.IAgVehicle
import farm.agmo.vehicle.IVehicleCallback

class AgVehicle private constructor(
    private val appContext: Context,
    private val listener: Listener,
) {

    /**
     * 하행 이벤트 + 연결 상태. 콜백은 binder 스레드에서 온다 — UI 갱신은 앱이 post할 것.
     * 기본 구현이 전부 no-op이라 필요한 것만 override한다.
     */
    interface Listener {
        /** 서비스 연결됨(또는 재연결됨) — 이 시점부터 subscribe/acquire 가능.
         *  재연결 시에도 불리며, 구독은 SDK가 자동 복원한다(제어권은 재획득 필요). */
        fun onConnected() {}
        /** 서비스 연결 끊김(서비스 사망 등). SDK가 자동 재바인딩을 시도한다. */
        fun onDisconnected() {}
        /** attach 거부 — 실행 충돌 차단(같은 제어신호 앱이 이미 실행 중). 앱은 종료 판단 */
        fun onRejected() {}

        fun onValue(value: SignalValue) {}
        fun onStale(key: String) {}
        fun onDevice(function: Int, present: Boolean) {}
        /** 제어권을 상위 계층(비상정지 등)에 선점당함 — 즉시 제어 UI 잠글 것 */
        fun onControlLost(key: String) {}
    }

    companion object {
        // 서비스 매니페스트의 <service> action·package와 일치해야 한다
        private const val ACTION = "farm.agmo.vehicle.BIND"
        private const val SERVICE_PKG = "farm.agmo.vehicle"

        /**
         * 서비스에 바인딩하고 핸들을 즉시 반환한다. 연결은 비동기 —
         * 실제 사용 가능 시점은 [Listener.onConnected]. bindService가 실패하면(서비스
         * 미설치 등) null.
         */
        fun bind(context: Context, listener: Listener): AgVehicle? {
            val v = AgVehicle(context.applicationContext, listener)
            val intent = Intent(ACTION).setPackage(SERVICE_PKG)
            val ok = v.appContext.bindService(intent, v.conn, Context.BIND_AUTO_CREATE)
            return if (ok) v else { v.appContext.unbindService(v.conn); null }
        }
    }

    @Volatile private var remote: IAgVehicle? = null

    /** 구독 거울 — 재연결 시 자동 복원(서비스 재동기화의 앱 쪽 짝). 진실의 원천 */
    private val wanted = linkedSetOf<String>()
    private val lock = Any()

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val r = IAgVehicle.Stub.asInterface(binder)
            remote = r
            if (!r.attach(callback)) { listener.onRejected(); return }
            // 재연결이면 원하던 구독을 복원 (제어 세션은 앱이 재획득해야 함 — 안전)
            synchronized(lock) { wanted.toList() }.forEach { runCatching { r.subscribe(it) } }
            listener.onConnected()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            remote = null
            listener.onDisconnected()
            // BIND_AUTO_CREATE라 서비스가 살아나면 onServiceConnected가 다시 불린다
        }
    }

    // 카탈로그는 청크로 오므로(oneway 버퍼 제한) done까지 누적 후 한 번에 전달
    private val catalogAcc = mutableListOf<SignalMeta>()
    private var catalogCb: ((List<SignalMeta>) -> Unit)? = null
    private var defsCb: ((Int, List<String>) -> Unit)? = null

    private val callback = object : IVehicleCallback.Stub() {
        override fun onValue(key: String, text: String, quality: String) =
            listener.onValue(SignalValue(key, text, Quality.of(quality)))
        override fun onStale(key: String) = listener.onStale(key)
        override fun onDevice(function: Int, present: Boolean) =
            listener.onDevice(function, present)
        override fun onControlLost(key: String) = listener.onControlLost(key)
        override fun onCatalog(chunk: List<String>, done: Boolean) {
            val deliver: Pair<((List<SignalMeta>) -> Unit)?, List<SignalMeta>>?
            synchronized(lock) {
                catalogAcc += chunk.mapNotNull(CatalogLine::parse)
                deliver = if (done) {
                    val snap = catalogCb to catalogAcc.toList()
                    catalogAcc.clear(); catalogCb = null
                    snap
                } else null
            }
            deliver?.let { (cb, list) -> cb?.invoke(list) }
        }
        override fun onDefsResult(accepted: Int, errors: List<String>) {
            defsCb?.invoke(accepted, errors); defsCb = null
        }
    }

    // ── 읽기 ────────────────────────────────────────────────

    /** 신호 구독. 미연결 상태에서도 호출 가능 — 연결/재연결 시 자동 반영된다. */
    fun subscribe(key: String) {
        synchronized(lock) { wanted.add(key) }
        remote?.runCatching { subscribe(key) }
    }

    fun unsubscribe(key: String) {
        synchronized(lock) { wanted.remove(key) }
        remote?.runCatching { unsubscribe(key) }
    }

    /** 신호 카탈로그 스냅샷 (전체 신호의 이름·주기·단위·쓰기 가능 여부) */
    fun requestCatalog(cb: (List<SignalMeta>) -> Unit) {
        synchronized(lock) { catalogCb = cb }
        remote?.runCatching { requestCatalog() }
    }

    // ── 쓰기 (제어) ─────────────────────────────────────────

    /**
     * 제어 세션 획득. null이면 미연결·자격 없음(매니페스트 미선언)·동급 이상 보유 중.
     * 토큰은 세션 안에 봉인 — 앱 코드에 노출하지 않아 오용(공유·저장)을 막는다.
     */
    fun acquire(key: String): ControlSession? {
        val token = remote?.runCatching { acquire(key) }?.getOrNull() ?: return null
        return if (token == 0L) null else ControlSession(key, token)
    }

    inner class ControlSession internal constructor(val key: String, private val token: Long) {
        /** 제어 명령. false = 세션 상실(선점·재획득·연결끊김) — acquire부터 다시 */
        fun send(value: Long): Boolean =
            remote?.runCatching { command(key, value, token) }?.getOrDefault(false) ?: false

        /** 정상 반납 — 마지막 명령값 유지 */
        fun release() { remote?.runCatching { release(key, token) } }

        /** 안전값 강제 송신 후 반납 — 앱 스스로의 비상 정지 */
        fun stopAndRelease() { remote?.runCatching { stopAndRelease(key, token) } }
    }

    // ── 외부 CAN 정의 (설치 시 1회) ──────────────────────────

    /** 자기 CAN 신호 정의(JSON) 등록. 반환: 수락 신호 수(미연결 시 0). 상세는 cb */
    fun registerDefs(defsJson: String, cb: ((accepted: Int, errors: List<String>) -> Unit)? = null): Int {
        defsCb = cb
        return remote?.runCatching { registerDefs(defsJson) }?.getOrDefault(0) ?: 0
    }

    fun unregisterDefs() { remote?.runCatching { unregisterDefs() } }

    /** 바인딩 해제 — 앱 onDestroy에서 호출. 제어 세션은 프로세스 종료로 서비스가 회수 */
    fun close() {
        remote = null
        runCatching { appContext.unbindService(conn) }
    }
}
