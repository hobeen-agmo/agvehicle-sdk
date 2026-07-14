// Signal.kt — 도메인 모듈의 베이스 (연결·구독·생명주기 자동연동).
//
// 각 도메인(Imu/Engine/Hitch)은 이걸 상속해 subscriptions()에서 "무슨 메시지를
// 구독해 리스너에 어떻게 넘길지"만 선언한다. 연결(shared)·해제·중복방지는 여기서 처리.
//
// [한 줄 생명주기] DefaultLifecycleObserver라 lifecycle.addObserver(signal) 한 줄이면
// ON_START에 자동 구독, ON_STOP에 자동 해제된다 (안드로이드 표준). 수동이 필요하면
// start()/stop()을 직접 불러도 된다.
//
//   val imu = Imu(this, object : Imu.Listener {
//       override fun onAngles(a: ImuAngles) = runOnUiThread { render(a) }
//   })
//   lifecycle.addObserver(imu)     // ← 이 한 줄이면 끝 (onCreate/onPause 수동 배선 불필요)
//
// 코루틴 의존 없음 — Flow가 필요하면 farm.agmo.vehicle:flow 모듈을 옵션으로 추가.
package farm.agmo.vehicle.sdk

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

abstract class Signal(context: Context) : DefaultLifecycleObserver {

    /** 프로세스 공유 연결 — 모든 도메인이 이 하나를 나눠 쓴다 */
    protected val vehicle: AgVehicle = AgVehicle.shared(context)

    private val subs = mutableListOf<AgVehicle.Subscription>()
    private var active = false

    /**
     * 구독할 메시지/신호를 선언한다 (start 시 1회 호출). 반환한 Subscription들은
     * stop 때 자동 해제된다. 보통 vehicle.subscribeMessage(keys){…} 또는 subscribe(key){…}를 나열.
     */
    protected abstract fun subscriptions(): List<AgVehicle.Subscription>

    /** 연결·구독 시작. 중복 호출 안전. (addObserver를 쓰면 직접 부를 일 없음) */
    fun start() {
        if (active) return
        subs += subscriptions()
        active = true
    }

    /** 구독 해제. start와 짝. */
    fun stop() {
        subs.forEach { it.close() }
        subs.clear()
        active = false
    }

    // ── 생명주기 자동연동 (addObserver 한 줄) ──────────────
    override fun onStart(owner: LifecycleOwner) = start()
    override fun onStop(owner: LifecycleOwner) = stop()
}
