// AgVehicle.kt — SDK core: 서비스와의 단일 연결 + 신호 멀티플렉싱 (bindService 기반).
//
// 왜 bindService인가: 외부(마켓) 앱은 platform 서명이 없어 ServiceManager(hidden API)를
// 쓸 수 없다. bindService는 공개 API라 어떤 앱이든 쓸 수 있고, 앱 프로세스 생명주기와
// 묶여 연결/해제가 자동 관리된다.
//
// 왜 프로세스 단일 연결(shared)인가: 서비스는 uid 하나당 콜백 하나만 기억한다
// (clients[uid]). 도메인 모듈들(hitch/engine/…)이 각자 bindService·attach하면 서로의
// 콜백을 덮어쓴다. 그래서 한 프로세스는 [shared] 연결 하나만 두고, 도메인 모듈과 앱은
// 그 위에 key별 콜백을 등록해 나눠 쓴다(멀티플렉싱).
//
// 앱이 직접 쓰는 법(동적/제네릭 신호):
//   val v = AgVehicle.shared(context)
//   val sub = v.subscribe("mypkg:MY_SIGNAL") { value -> ... }
//   val ctrl = v.acquire("HITCH_CMD")
// 타입 있는 편의는 도메인 모듈(farm.agmo.vehicle:hitch 등)이 이 위에 얹는다.
package farm.agmo.vehicle.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import farm.agmo.vehicle.IAgVehicle
import farm.agmo.vehicle.IVehicleCallback
import java.util.concurrent.CopyOnWriteArrayList

class AgVehicle private constructor(private val appContext: Context) {

    /** 연결 상태 관찰 — 도메인 모듈/앱이 재연결 시점을 알기 위해 등록 */
    interface ConnectionListener {
        fun onConnected() {}      // 이 시점부터 구독/제어 가능 (재연결 시에도 호출)
        fun onDisconnected() {}   // 서비스 사망 등 — SDK가 자동 재바인딩
        fun onRejected() {}       // attach 거부 — 실행 충돌 차단(앱 종료 판단)
    }

    /** 구독 핸들 — close()로 해제. 도메인 모듈은 이걸 들고 있다가 stop 시 닫는다 */
    fun interface Subscription { fun close() }

    companion object {
        private const val ACTION = "farm.agmo.vehicle.BIND"
        private const val SERVICE_PKG = "farm.agmo.vehicle"

        @Volatile private var instance: AgVehicle? = null

        /**
         * 프로세스 공유 연결. 첫 호출에 bindService를 걸고, 이후 같은 인스턴스를 준다.
         * 도메인 모듈과 앱이 모두 이걸 통해 서비스에 얹힌다(연결은 하나).
         */
        fun shared(context: Context): AgVehicle =
            instance ?: synchronized(this) {
                instance ?: AgVehicle(context.applicationContext).also {
                    it.bind(); instance = it
                }
            }
    }

    @Volatile private var remote: IAgVehicle? = null
    @Volatile var isConnected: Boolean = false; private set

    private val connListeners = CopyOnWriteArrayList<ConnectionListener>()
    private val lock = Any()

    // key → 그 신호를 원하는 콜백들. 서비스엔 key당 SUB 1회만 나간다(refcount).
    private val valueSubs = HashMap<String, MutableList<(SignalValue) -> Unit>>()
    private val staleSubs = HashMap<String, MutableList<(String) -> Unit>>()
    // 제어권 보유 key → onControlLost 콜백 (선점 통지 라우팅)
    private val controlLost = HashMap<String, () -> Unit>()
    private val deviceCbs = CopyOnWriteArrayList<(Int, Boolean) -> Unit>()

    private fun bind(): Boolean {
        val intent = Intent(ACTION).setPackage(SERVICE_PKG)
        return appContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val r = IAgVehicle.Stub.asInterface(binder)
            remote = r
            if (!r.attach(callback)) {
                connListeners.forEach { it.onRejected() }
                return
            }
            isConnected = true
            // 재연결이면 원하던 구독을 복원(제어 세션은 안전을 위해 재획득 필요)
            synchronized(lock) { valueSubs.keys.toList() }.forEach { runCatching { r.subscribe(it) } }
            connListeners.forEach { it.onConnected() }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            remote = null
            isConnected = false
            synchronized(lock) { controlLost.clear() }   // 세션은 무효 — 재획득 필요
            connListeners.forEach { it.onDisconnected() }
            // BIND_AUTO_CREATE라 서비스가 살아나면 onServiceConnected가 다시 불린다
        }
    }

    // 카탈로그는 청크로 오므로(oneway 버퍼 제한) done까지 누적 후 한 번에 전달
    private val catalogAcc = mutableListOf<SignalMeta>()
    private var catalogCb: ((List<SignalMeta>) -> Unit)? = null
    private var defsCb: ((Int, List<String>) -> Unit)? = null

    private val callback = object : IVehicleCallback.Stub() {
        override fun onValue(key: String, text: String, quality: String) {
            val v = SignalValue(key, text, Quality.of(quality))
            synchronized(lock) { valueSubs[key]?.toList() }?.forEach { it(v) }
        }
        override fun onStale(key: String) {
            synchronized(lock) { staleSubs[key]?.toList() }?.forEach { it(key) }
        }
        override fun onDevice(function: Int, present: Boolean) =
            deviceCbs.forEach { it(function, present) }
        override fun onControlLost(key: String) {
            synchronized(lock) { controlLost[key] }?.invoke()
        }
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

    // ── 연결 관찰 ───────────────────────────────────────────
    fun addConnectionListener(l: ConnectionListener) {
        connListeners.add(l)
        if (isConnected) l.onConnected()   // 늦게 붙은 관찰자도 현재 상태를 즉시 받음
    }
    fun removeConnectionListener(l: ConnectionListener) = connListeners.remove(l)

    fun onDevice(cb: (function: Int, present: Boolean) -> Unit): Subscription {
        deviceCbs.add(cb)
        return Subscription { deviceCbs.remove(cb) }
    }

    // ── 읽기 (구독) ─────────────────────────────────────────

    /**
     * 신호 구독. 반환 핸들을 close()하면 해제된다. 같은 key를 여러 곳이 구독해도
     * 서비스엔 SUB 1회만 나가고, 마지막 구독자가 닫힐 때 UNSUB 1회 나간다(refcount).
     * 미연결 상태에서 호출해도 되며 연결/재연결 시 자동 반영된다.
     *
     * @param sampleMs 앱 콜백 최소 간격(ms). 0(기본)이면 오는 값을 전부 전달(하위호환).
     *   0보다 크면 최대 sampleMs마다 한 번만 최신값을 전달해 UI 숫자 깜빡임을 줄인다
     *   (leading-edge 게이트: 값이 들어온 순간 now-last>=sampleMs면 통과, 타이머 없음).
     *   게이트 상태는 이 구독 호출마다 독립이라 신호별로 다른 주기를 걸 수 있다.
     */
    fun subscribe(key: String, sampleMs: Long = 0, onValue: (SignalValue) -> Unit): Subscription {
        val cb = if (sampleMs > 0) gate(sampleMs, onValue) else onValue
        val first = synchronized(lock) {
            val list = valueSubs.getOrPut(key) { mutableListOf() }
            val wasEmpty = list.isEmpty()
            list.add(cb)
            wasEmpty
        }
        if (first) remote?.runCatching { subscribe(key) }
        return Subscription {
            val last = synchronized(lock) {
                val list = valueSubs[key] ?: return@Subscription
                list.remove(cb)
                if (list.isEmpty()) { valueSubs.remove(key); true } else false
            }
            if (last) remote?.runCatching { unsubscribe(key) }
        }
    }

    /**
     * 한 메시지(신호 여럿)를 함께 구독한다. 신호가 하나라도 갱신되면 그 메시지의
     * 최신값 맵을 넘긴다 — 도메인 모듈이 타입 있는 "ID별 클래스"로 조립하는 토대.
     * (한 CAN 메시지의 신호들은 한 프레임으로 같이 오지만 콜백 스레드가 다를 수 있어
     *  최신값 누적은 동기화한다.) 반환 핸들 close()로 전체 해제.
     *
     * @param sampleMs 앱 콜백 최소 간격(ms). 0(기본)이면 갱신될 때마다 전달(하위호환).
     *   0보다 크면 개별 신호는 전속력으로 받아 최신값에 누적하되, 앱 콜백은 최대
     *   sampleMs마다 최신 스냅샷 1회만 호출한다(leading-edge, 타이머 없음). 게이트는
     *   이 구독 인스턴스 전용이라 RPM은 100ms, 다른 메시지는 다른 주기로 독립 선언 가능.
     */
    fun subscribeMessage(keys: List<String>, sampleMs: Long = 0, onValues: (Map<String, Double>) -> Unit): Subscription {
        val deliver = if (sampleMs > 0) gate(sampleMs, onValues) else onValues
        val latest = HashMap<String, Double>()
        val mlock = Any()
        val subs = keys.map { key ->
            subscribe(key) { value ->
                val snapshot: Map<String, Double> = synchronized(mlock) {
                    value.number?.let { latest[key] = it }
                    HashMap(latest)
                }
                deliver(snapshot)
            }
        }
        return Subscription { subs.forEach { it.close() } }
    }

    // leading-edge 스로틀: 값이 들어온 순간 마지막 통과로부터 sampleMs가 지났으면 전달.
    // 타이머·코루틴 없이 단조 시계(SystemClock.elapsedRealtime)만 비교한다. RPM처럼 연속
    // 갱신되는 신호는 게이트가 열릴 때마다 다음 값이 통과해 실제 ~sampleMs 간격으로 흐른다.
    private fun <T> gate(sampleMs: Long, downstream: (T) -> Unit): (T) -> Unit {
        val glock = Any()
        var last = 0L
        return { value ->
            val pass = synchronized(glock) {
                val now = SystemClock.elapsedRealtime()
                if (now - last >= sampleMs) { last = now; true } else false
            }
            if (pass) downstream(value)
        }
    }

    /** 신호 staleness(값 끊김) 알림 구독 — 값 구독과 별개로 등록 */
    fun onStale(key: String, cb: (String) -> Unit): Subscription {
        synchronized(lock) { staleSubs.getOrPut(key) { mutableListOf() }.add(cb) }
        return Subscription {
            synchronized(lock) {
                staleSubs[key]?.remove(cb)
                if (staleSubs[key]?.isEmpty() == true) staleSubs.remove(key)
            }
        }
    }

    /** 신호 카탈로그 스냅샷 (전체 신호의 이름·주기·단위·쓰기 가능 여부) */
    fun requestCatalog(cb: (List<SignalMeta>) -> Unit) {
        synchronized(lock) { catalogCb = cb }
        remote?.runCatching { requestCatalog() }
    }

    // ── 쓰기 (제어) ─────────────────────────────────────────

    /**
     * 제어 세션 획득. null이면 미연결·자격 없음(매니페스트 미선언)·동급 이상 보유 중.
     * onControlLost는 이 key가 상위 계층에 선점당하면 호출된다(등록 안 하면 무시).
     * 토큰은 세션 안에 봉인 — 앱 코드에 노출하지 않아 오용을 막는다.
     */
    fun acquire(key: String, onControlLost: (() -> Unit)? = null): ControlSession? {
        val token = remote?.runCatching { acquire(key) }?.getOrNull() ?: return null
        if (token == 0L) return null
        onControlLost?.let { synchronized(lock) { controlLost[key] = it } }
        return ControlSession(key, token)
    }

    inner class ControlSession internal constructor(val key: String, private val token: Long) {
        /** 제어 명령(raw). false = 세션 상실(선점·연결끊김) — acquire부터 다시 */
        fun send(value: Long): Boolean =
            remote?.runCatching { command(key, value, token) }?.getOrDefault(false) ?: false

        /** 정상 반납 — 마지막 명령값 유지 */
        fun release() {
            remote?.runCatching { release(key, token) }
            synchronized(lock) { controlLost.remove(key) }
        }

        /** 안전값 강제 송신 후 반납 — 앱 스스로의 비상 정지 */
        fun stopAndRelease() {
            remote?.runCatching { stopAndRelease(key, token) }
            synchronized(lock) { controlLost.remove(key) }
        }
    }

    // ── 외부 CAN 정의 (설치 시 1회) ──────────────────────────

    /** 자기 CAN 신호 정의(JSON) 등록. 반환: 수락 신호 수(미연결 시 0). 상세는 cb */
    fun registerDefs(defsJson: String, cb: ((accepted: Int, errors: List<String>) -> Unit)? = null): Int {
        defsCb = cb
        return remote?.runCatching { registerDefs(defsJson) }?.getOrDefault(0) ?: 0
    }

    fun unregisterDefs() { remote?.runCatching { unregisterDefs() } }
}
