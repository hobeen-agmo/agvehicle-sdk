// Vehicle.kt — 홈 런처용 차량 상태 도메인 (읽기: Signal / 토글: 제어 세션).
//
//   // 읽기 — 생명주기 한 줄
//   val vehicle = Vehicle(this, object : Vehicle.Listener {
//       override fun onSpeed(s: VehicleSpeed) = runOnUiThread { render(s.kmh) }
//       override fun onBattery(b: Battery)    = runOnUiThread { render(b.volts) }
//       override fun onDpf(d: Dpf)            = runOnUiThread { if (d.warning) showWarn() }
//   })
//   lifecycle.addObserver(vehicle)
//
// 제어(4WD/AutoLift)는 이 도메인에 없다 — 4WD는 실차 CAN 미매핑(미지원), AutoLift는
// 별도 CAN 신호가 아니라 조향각·FNR·히치 조합의 앱 레벨 기능(oem-tractor의 Tractor.autoLift).
package farm.agmo.vehicle.vehicle

import android.content.Context
import farm.agmo.vehicle.sdk.AgVehicle
import farm.agmo.vehicle.sdk.Signal

class Vehicle(
    context: Context,
    private val listener: Listener,
    private val intervalMs: Long = 0,   // 앱 콜백 최소 간격(ms). 0=전부 전달, >0=최대 그 간격마다 최신값 1회
) : Signal(context) {

    /** 차량 상태 콜백 — 필요한 것만 override */
    interface Listener {
        fun onSpeed(speed: VehicleSpeed) {}
        fun onPto(pto: PtoSpeed) {}
        fun onBattery(battery: Battery) {}
        fun onDpf(dpf: Dpf) {}
        fun onPosition(position: GpsPosition) {}
        fun onCcvs1(ccvs1: Ccvs1) {}
        fun onEbc1(ebc1: Ebc1) {}
        fun onEbc2(ebc2: Ebc2) {}
        fun onEtc1(etc1: Etc1) {}
        fun onEtc2(etc2: Etc2) {}
        fun onTco1(tco1: Tco1) {}
        fun onVdc2(vdc2: Vdc2) {}
        fun onWbsd(wbsd: Wbsd) {}
        fun onGbsd(gbsd: Gbsd) {}
        fun onMss(mss: Mss) {}
        fun onVdhr(vdhr: Vdhr) {}
        fun onVh(vh: Vh) {}
        fun onVep1(vep1: Vep1) {}
        fun onRearPto(rearPto: RearPto) {}
        fun onFrontPto(frontPto: FrontPto) {}
        fun onVds(vds: Vds) {}
        fun onGnssCourseSpeed(courseSpeed: GnssCourseSpeed) {}
        fun onGnssQuality(quality: GnssQuality) {}
        fun onAmb(amb: Amb) {}
        fun onTd(td: Td) {}
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = listOf(
        vehicle.subscribe(VehicleSpeed.KEY, intervalMs) { it.number?.let { v -> listener.onSpeed(VehicleSpeed(v)) } },
        vehicle.subscribe(PtoSpeed.KEY, intervalMs)     { it.number?.let { v -> listener.onPto(PtoSpeed(v)) } },
        vehicle.subscribe(Battery.KEY, intervalMs)      { it.number?.let { v -> listener.onBattery(Battery(v)) } },
        vehicle.subscribe(Dpf.KEY, intervalMs)          { it.number?.let { v -> listener.onDpf(Dpf(v)) } },
        vehicle.subscribeMessage(GpsPosition.KEYS, intervalMs) { GpsPosition.from(it)?.let(listener::onPosition) },
        vehicle.subscribeMessage(Ccvs1.KEYS, intervalMs)    { Ccvs1.from(it)?.let(listener::onCcvs1) },
        vehicle.subscribeMessage(Ebc1.KEYS, intervalMs)     { Ebc1.from(it)?.let(listener::onEbc1) },
        vehicle.subscribeMessage(Ebc2.KEYS, intervalMs)     { Ebc2.from(it)?.let(listener::onEbc2) },
        vehicle.subscribeMessage(Etc1.KEYS, intervalMs)     { Etc1.from(it)?.let(listener::onEtc1) },
        vehicle.subscribeMessage(Etc2.KEYS, intervalMs)     { Etc2.from(it)?.let(listener::onEtc2) },
        vehicle.subscribeMessage(Tco1.KEYS, intervalMs)     { Tco1.from(it)?.let(listener::onTco1) },
        vehicle.subscribeMessage(Vdc2.KEYS, intervalMs)     { Vdc2.from(it)?.let(listener::onVdc2) },
        vehicle.subscribeMessage(Wbsd.KEYS, intervalMs)     { Wbsd.from(it)?.let(listener::onWbsd) },
        vehicle.subscribeMessage(Gbsd.KEYS, intervalMs)     { Gbsd.from(it)?.let(listener::onGbsd) },
        vehicle.subscribeMessage(Mss.KEYS, intervalMs)      { Mss.from(it)?.let(listener::onMss) },
        vehicle.subscribeMessage(Vdhr.KEYS, intervalMs)     { Vdhr.from(it)?.let(listener::onVdhr) },
        vehicle.subscribeMessage(Vh.KEYS, intervalMs)       { Vh.from(it)?.let(listener::onVh) },
        vehicle.subscribeMessage(Vep1.KEYS, intervalMs)     { Vep1.from(it)?.let(listener::onVep1) },
        vehicle.subscribeMessage(RearPto.KEYS, intervalMs)  { RearPto.from(it)?.let(listener::onRearPto) },
        vehicle.subscribeMessage(FrontPto.KEYS, intervalMs) { FrontPto.from(it)?.let(listener::onFrontPto) },
        vehicle.subscribeMessage(Vds.KEYS, intervalMs)      { Vds.from(it)?.let(listener::onVds) },
        vehicle.subscribeMessage(GnssCourseSpeed.KEYS, intervalMs) { GnssCourseSpeed.from(it)?.let(listener::onGnssCourseSpeed) },
        vehicle.subscribeMessage(GnssQuality.KEYS, intervalMs)     { GnssQuality.from(it)?.let(listener::onGnssQuality) },
        vehicle.subscribeMessage(Amb.KEYS, intervalMs)      { Amb.from(it)?.let(listener::onAmb) },
        vehicle.subscribeMessage(Td.KEYS, intervalMs)       { Td.from(it)?.let(listener::onTd) },
    )
}
