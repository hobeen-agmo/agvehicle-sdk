// Vehicle.kt — 홈 런처용 차량 상태 도메인 (읽기: Signal / 토글: 제어 세션).
//
// 두 가지 사용법:
//
//   // (1) 람다 — 필요한 신호만
//   val vehicle = Vehicle(this, intervalMs = 200)
//   vehicle.onSpeed { s -> runOnUiThread { render(s.kmh) } }
//   lifecycle.addObserver(vehicle)
//
//   // (2) 리스너 — 여러 신호를 한 객체에서 override
//   val vehicle = Vehicle(this, object : Vehicle.Listener {
//       override fun onSpeed(s: VehicleSpeed) = runOnUiThread { render(s.kmh) }
//       override fun onBattery(b: Battery)    = runOnUiThread { render(b.volts) }
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
    private val intervalMs: Long = 0,
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

    /** 리스너 방식 — 여러 신호를 한 객체에서 override. */
    constructor(context: Context, listener: Listener, intervalMs: Long = 0) : this(context, intervalMs) {
        onSpeed(listener::onSpeed); onPto(listener::onPto); onBattery(listener::onBattery); onDpf(listener::onDpf)
        onPosition(listener::onPosition); onCcvs1(listener::onCcvs1); onEbc1(listener::onEbc1); onEbc2(listener::onEbc2)
        onEtc1(listener::onEtc1); onEtc2(listener::onEtc2); onTco1(listener::onTco1); onVdc2(listener::onVdc2)
        onWbsd(listener::onWbsd); onGbsd(listener::onGbsd); onMss(listener::onMss); onVdhr(listener::onVdhr)
        onVh(listener::onVh); onVep1(listener::onVep1); onRearPto(listener::onRearPto); onFrontPto(listener::onFrontPto)
        onVds(listener::onVds); onGnssCourseSpeed(listener::onGnssCourseSpeed); onGnssQuality(listener::onGnssQuality)
        onAmb(listener::onAmb); onTd(listener::onTd)
    }

    // ── 람다 편의 API ──
    fun onSpeed(cb: (VehicleSpeed) -> Unit) = value(VehicleSpeed.KEY, ::VehicleSpeed, cb)
    fun onPto(cb: (PtoSpeed) -> Unit) = value(PtoSpeed.KEY, ::PtoSpeed, cb)
    fun onBattery(cb: (Battery) -> Unit) = value(Battery.KEY, ::Battery, cb)
    fun onDpf(cb: (Dpf) -> Unit) = value(Dpf.KEY, ::Dpf, cb)
    fun onPosition(cb: (GpsPosition) -> Unit) = msg(GpsPosition.KEYS, GpsPosition::from, cb)
    fun onCcvs1(cb: (Ccvs1) -> Unit) = msg(Ccvs1.KEYS, Ccvs1::from, cb)
    fun onEbc1(cb: (Ebc1) -> Unit) = msg(Ebc1.KEYS, Ebc1::from, cb)
    fun onEbc2(cb: (Ebc2) -> Unit) = msg(Ebc2.KEYS, Ebc2::from, cb)
    fun onEtc1(cb: (Etc1) -> Unit) = msg(Etc1.KEYS, Etc1::from, cb)
    fun onEtc2(cb: (Etc2) -> Unit) = msg(Etc2.KEYS, Etc2::from, cb)
    fun onTco1(cb: (Tco1) -> Unit) = msg(Tco1.KEYS, Tco1::from, cb)
    fun onVdc2(cb: (Vdc2) -> Unit) = msg(Vdc2.KEYS, Vdc2::from, cb)
    fun onWbsd(cb: (Wbsd) -> Unit) = msg(Wbsd.KEYS, Wbsd::from, cb)
    fun onGbsd(cb: (Gbsd) -> Unit) = msg(Gbsd.KEYS, Gbsd::from, cb)
    fun onMss(cb: (Mss) -> Unit) = msg(Mss.KEYS, Mss::from, cb)
    fun onVdhr(cb: (Vdhr) -> Unit) = msg(Vdhr.KEYS, Vdhr::from, cb)
    fun onVh(cb: (Vh) -> Unit) = msg(Vh.KEYS, Vh::from, cb)
    fun onVep1(cb: (Vep1) -> Unit) = msg(Vep1.KEYS, Vep1::from, cb)
    fun onRearPto(cb: (RearPto) -> Unit) = msg(RearPto.KEYS, RearPto::from, cb)
    fun onFrontPto(cb: (FrontPto) -> Unit) = msg(FrontPto.KEYS, FrontPto::from, cb)
    fun onVds(cb: (Vds) -> Unit) = msg(Vds.KEYS, Vds::from, cb)
    fun onGnssCourseSpeed(cb: (GnssCourseSpeed) -> Unit) = msg(GnssCourseSpeed.KEYS, GnssCourseSpeed::from, cb)
    fun onGnssQuality(cb: (GnssQuality) -> Unit) = msg(GnssQuality.KEYS, GnssQuality::from, cb)
    fun onAmb(cb: (Amb) -> Unit) = msg(Amb.KEYS, Amb::from, cb)
    fun onTd(cb: (Td) -> Unit) = msg(Td.KEYS, Td::from, cb)

    private val regs = mutableListOf<() -> AgVehicle.Subscription>()

    /** 단일 신호(숫자) 구독 — number를 wrap으로 감싸 콜백. */
    private fun <T> value(key: String, wrap: (Double) -> T, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribe(key, intervalMs) { it.number?.let { n -> cb(wrap(n)) } } }
    }

    /** 메시지(신호 여럿) 구독 — parse가 null이면 그 프레임은 건너뛴다. */
    private fun <T> msg(keys: List<String>, parse: (Map<String, Double>) -> T?, cb: (T) -> Unit) = apply {
        regs += { vehicle.subscribeMessage(keys, intervalMs) { m -> parse(m)?.let(cb) } }
    }

    override fun subscriptions(): List<AgVehicle.Subscription> = regs.map { it() }
}
