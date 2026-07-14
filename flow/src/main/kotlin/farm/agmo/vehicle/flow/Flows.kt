// Flows.kt — 신호를 Kotlin Flow로 받는 옵션 API (코루틴 의존은 이 모듈에만).
//
// 콜백 코어(Signal/Listener)는 코루틴 없이 돌지만, 코루틴을 쓰는 앱은 이 모듈을 추가해
// 스트림별 구독·조합을 얻는다 (Room의 room-ktx와 같은 계층 분리):
//
//   lifecycleScope.launch {
//       repeatOnLifecycle(Lifecycle.State.STARTED) {           // 표준 생명주기
//           ImuFlow.angles(context).collect { render(it) }     // 필요한 스트림만
//       }
//   }
//   // 조합도 공짜
//   combine(ImuFlow.angles(ctx), EngineFlow.eec1(ctx)) { a, e -> Dashboard(a, e) }.collect { … }
//
// 각 Flow는 core.subscribeMessage를 callbackFlow로 감싼 것 — 마지막 수집자가 사라지면
// awaitClose에서 구독을 해제한다(콜드 스트림). buffer(CONFLATED): 고빈도(IMU 100Hz)에
// 수집이 밀리면 최신값만 유지(프레임 밀림 방지).
package farm.agmo.vehicle.flow

import android.content.Context
import farm.agmo.vehicle.engine.*
import farm.agmo.vehicle.hitch.HitchPosition
import farm.agmo.vehicle.imu.*
import farm.agmo.vehicle.vehicle.*
import farm.agmo.vehicle.sdk.AgVehicle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

// 한 메시지(신호 여럿)를 조립해 타입 T로 흘리는 공통 빌더
private fun <T> messageFlow(
    context: Context,
    keys: List<String>,
    assemble: (Map<String, Double>) -> T?,
): Flow<T> = callbackFlow {
    val sub = AgVehicle.shared(context).subscribeMessage(keys) { values ->
        assemble(values)?.let { trySend(it) }
    }
    awaitClose { sub.close() }
}.buffer(1, BufferOverflow.DROP_OLDEST)   // 최신값 우선(고빈도 대비)

// 단일 신호를 타입 T로
private fun <T> signalFlow(
    context: Context,
    key: String,
    map: (Double) -> T,
): Flow<T> = callbackFlow {
    val sub = AgVehicle.shared(context).subscribe(key) { v -> v.number?.let { trySend(map(it)) } }
    awaitClose { sub.close() }
}.buffer(1, BufferOverflow.DROP_OLDEST)

object ImuFlow {
    fun angles(context: Context): Flow<ImuAngles> = messageFlow(context, ImuAngles.KEYS, ImuAngles::from)
    fun rates(context: Context): Flow<ImuRates> = messageFlow(context, ImuRates.KEYS, ImuRates::from)
    fun accel(context: Context): Flow<ImuAccel> = messageFlow(context, ImuAccel.KEYS, ImuAccel::from)
}

object EngineFlow {
    fun eec1(context: Context): Flow<Eec1> = messageFlow(context, Eec1.KEYS, Eec1::from)
    fun temperature(context: Context): Flow<EngineTemperature> =
        messageFlow(context, EngineTemperature.KEYS, EngineTemperature::from)
    fun oilPressure(context: Context): Flow<EngineOilPressure> =
        messageFlow(context, EngineOilPressure.KEYS, EngineOilPressure::from)
    fun fuelLevel(context: Context): Flow<FuelLevel> = messageFlow(context, FuelLevel.KEYS, FuelLevel::from)
    fun hours(context: Context): Flow<EngineHours> = messageFlow(context, EngineHours.KEYS, EngineHours::from)
}

object HitchFlow {
    fun position(context: Context): Flow<HitchPosition> =
        signalFlow(context, HitchPosition.KEY, ::HitchPosition)
}

object VehicleFlow {
    fun speed(context: Context): Flow<VehicleSpeed> = signalFlow(context, VehicleSpeed.KEY, ::VehicleSpeed)
    fun pto(context: Context): Flow<PtoSpeed> = signalFlow(context, PtoSpeed.KEY, ::PtoSpeed)
    fun battery(context: Context): Flow<Battery> = signalFlow(context, Battery.KEY, ::Battery)
    fun dpf(context: Context): Flow<Dpf> = signalFlow(context, Dpf.KEY, ::Dpf)
    fun position(context: Context): Flow<GpsPosition> = messageFlow(context, GpsPosition.KEYS, GpsPosition::from)
}
