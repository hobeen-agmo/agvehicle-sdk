// Engine.kt — 엔진 읽기 도메인 모듈 (타입 있는 파사드, core 위에 얹힘).
//
//   val engine = Engine.start(context) { sample ->
//       if (sample.complete) render(sample.coolantTempC!!, sample.rpm!!)
//   }
//   // ...
//   engine.stop()
//
// 자격 선언 불필요(읽기 전용). core의 공유 연결 위에 ENGTEMP/ENGRPM를 구독하고,
// 따로 오는 두 값을 EngineSample로 조립해 넘긴다.
package farm.agmo.vehicle.engine

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle

object Engine {
    /**
     * 엔진 신호 스트림 시작. onSample은 값이 하나 갱신될 때마다 최신 스냅샷으로 호출된다
     * (수온·RPM이 서로 다른 주기로 오므로 sample.complete로 완전성 확인).
     * 콜백은 binder 스레드 — UI 갱신은 앱이 post할 것.
     */
    fun start(context: Context, onSample: (EngineSample) -> Unit): EngineStream {
        val v = AgVehicle.shared(context)
        val assembler = EngineAssembler()
        val subs = EngineAssembler.KEYS.map { key ->
            v.subscribe(key) { value -> onSample(assembler.update(key, value.number)) }
        }
        return EngineStream(subs)
    }
}

class EngineStream internal constructor(
    private val subs: List<AgVehicle.Subscription>,
) {
    /** 구독 해제 — 더는 값을 받지 않는다 */
    fun stop() = subs.forEach { it.close() }
}
