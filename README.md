# AgVehicle SDK

농기계 HMI 플랫폼의 **차량 신호 SDK**. 앱에서 CAN 신호를 읽고(구독) 제어(쓰기)하는
공개 창구다. 내부적으로 시스템 서비스(agvehicled)에 `bindService`로 붙고, 서비스가
데몬(agcand)을 거쳐 CAN 버스와 통신한다 — 앱은 그 계층을 몰라도 된다.

```
앱 (이 SDK) ──bindService──▶ agvehicled ──소켓──▶ agcand ──▶ CAN 버스
```

## 모듈 구성 (core + 도메인별)

SDK는 **기반 core 하나 + 신호 도메인별 모듈 여러 개**로 나뉜다. 필요한 도메인만
가져다 쓴다 (NEVONEX의 Function Item별 의존성과 같은 모델).

> 도메인 분류(표준 vs AGMO 제조사 고유)·네임스페이스(`.oem.`)·모듈 명명(`:oem-*`) 규칙은
> [docs/sdk-conventions.md](docs/sdk-conventions.md) 참조.

| 아티팩트 | 내용 | 코루틴 |
|---|---|---|
| `farm.agmo.vehicle:core` | 연결·제네릭 구독/제어·카탈로그·`Signal` 베이스(생명주기) | 없음 |
| `farm.agmo.vehicle:imu` | 자세 읽기 — 각도/각속도/가속도 (Aceinna MTLT305) | 없음 |
| `farm.agmo.vehicle:engine` | 엔진 읽기 — EEC1(rpm)/EEC2(부하)/수온/오일압/연료/가동시간 | 없음 |
| `farm.agmo.vehicle:hitch` | 히치 위치 읽기 + 제어(`setPosition %`, raw 변환 은닉) | 없음 |
| `farm.agmo.vehicle:gpio` | 범용 GPIO — 아날로그/디지털 입력 읽기 + 디지털 출력 제어 | 없음 |
| `farm.agmo.vehicle:notification` | 앱→태블릿 알림(상태바 + 홈 종모양 미러링) | 없음 |
| `farm.agmo.vehicle:vehicle` | 홈 런처용 — 차속·PTO·배터리·DPF 읽기(4WD·AutoLift 제어는 미포함) | 없음 |
| `farm.agmo.vehicle:flow` | 위 신호들을 Kotlin `Flow`로 (옵션) | **여기만** |
| `farm.agmo.vehicle:oem-tractor` | 🏭 AGMO 커스텀 트랙터 — FNR/변속/PTO/유압/가속 읽기+제어, AutoLift | 없음 |
| `farm.agmo.vehicle:oem-steer` | 🏭 조향모터(Keya KY170) 상태/결함 읽기 | 없음 |
| `farm.agmo.vehicle:oem-spreader` | 🏭 RDA 살포기 — GNSS 읽기 + 살포 모터 제어 | 없음 |
| `farm.agmo.vehicle:oem-imu` | 🏭 보조 자이로(Allynav R70) 읽기 | 없음 |
| `farm.agmo.vehicle:oem-genericio` | 🏭 범용 CAN IO 보드 — 입력 읽기 + PWM/LED 출력 제어 | 없음 |

**콜백 기본, Flow는 옵션 (Room의 room-ktx 방식).** 코어·도메인 모듈은 콜백이라
코루틴 의존이 없다(Java 앱도 OK). 코루틴/Flow를 쓰는 앱만 `:flow`를 추가한다.
생명주기는 `lifecycle.addObserver(...)` 한 줄로 자동 연동된다.

**설계 원칙: CAN 메시지(ID) 하나 = 클래스 하나.** 한 PGN은 한 프레임으로 통째로
오므로, 그 안의 신호들(pitch+roll 등)은 한 데이터 클래스의 필드가 된다. 도메인 모듈은
core 위에 이 타입 있는 파사드를 얹은 것뿐 — 신호 조립·raw 변환 은닉·자동완성 발견성을
준다. 앱이 **자기 CAN 신호를 직접 정의**해 쓰는 동적 경우엔 도메인 모듈 없이 core의
제네릭 API를 쓴다.

```kotlin
// IMU — 메시지(ID)별 클래스
data class ImuAngles(val pitchDeg: Double, val rollDeg: Double)          // 0xF029
data class ImuRates(val xDegS: Double, val yDegS: Double, val zDegS: Double)  // 0xF02A
data class ImuAccel(val xMs2: Double, val yMs2: Double, val zMs2: Double)     // 0xF02D
// ENGINE
data class Eec2(val loadPercent: Double, val accelPedalPercent: Double)  // 0xF003
```

## 설치 (Gradle)

GitHub Packages(Maven)에서 받는다. 필요한 도메인 모듈만 넣으면 core는 자동으로 딸려온다.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/hobeen-agmo/agvehicle-sdk")
            credentials {   // read:packages 토큰
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.token").get()
            }
        }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("farm.agmo.vehicle:imu:0.1.0")      // core 자동 포함
    implementation("farm.agmo.vehicle:engine:0.1.0")
    implementation("farm.agmo.vehicle:hitch:0.1.0")
    // 동적 신호만 쓸 거면: implementation("farm.agmo.vehicle:core:0.1.0")
}
```

## 사용 — 콜백 (기본, 코루틴 불필요)

도메인 객체를 리스너와 함께 만들고 `lifecycle.addObserver` 한 줄이면 끝 —
ON_START에 자동 구독, ON_STOP에 자동 해제된다.

`Engine`/`Vehicle`/`Hitch`는 생성자 3번째 인자로 `intervalMs`(기본 0)를 받는다.
Cuttlefish 같은 소프트웨어 렌더 환경에서 UI가 CAN 원시 신호 레이트로 매번
재구성/리드로우하면 CPU가 급증하는데, 사람이 읽는 계기판은 5Hz(200ms)면 충분하다.
`intervalMs`로 앱 콜백 빈도 상한을 걸어 리드로우를 줄인다(0이면 오는 값 전부 전달 —
하위호환).

```kotlin
// IMU 자세 (자격 불필요)
val imu = Imu(this, object : Imu.Listener {
    override fun onAngles(a: ImuAngles) = runOnUiThread { render(a.pitchDeg, a.rollDeg) }
    override fun onRates(r: ImuRates)   = runOnUiThread { render(r.xDegS, r.yDegS, r.zDegS) }
    // 필요한 것만 override — 안 하면 no-op
})
lifecycle.addObserver(imu)     // ← 생명주기 한 줄

// 엔진 (자격 불필요) — intervalMs=200 → 최대 5Hz로 콜백(리드로우 상한)
val engine = Engine(this, object : Engine.Listener {
    override fun onEec1(e: Eec1) = runOnUiThread { render(e.rpm, e.loadPercent) }
    override fun onTemperature(t: EngineTemperature) = runOnUiThread { render(t.coolantC) }
}, intervalMs = 200)
lifecycle.addObserver(engine)

// 히치 — 위치 읽기(리스너) + 제어(세션)
val hitch = Hitch(this, object : Hitch.Listener {
    override fun onPosition(p: HitchPosition) = runOnUiThread { render(p.percent) }
}, intervalMs = 200)
lifecycle.addObserver(hitch)

val ctrl = Hitch.control(this) ?: return     // null = 자격 없음 / 상위 보유 중 / 미연결
ctrl.onLost { runOnUiThread { lockUi() } }   // 상위 계층 선점 시
ctrl.setPosition(50.0)                       // % — raw 변환은 SDK가 처리
ctrl.release()                               // 정상 반납(마지막 위치 유지)
```

## 사용 — Flow (옵션, `:flow` 모듈)

코루틴을 쓰면 스트림별 구독·조합이 된다. `repeatOnLifecycle`이 표준 생명주기.

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        ImuFlow.angles(this@MainActivity).collect { render(it.pitchDeg, it.rollDeg) }
    }
}
// 조합 — 두 신호 합쳐서
combine(ImuFlow.angles(ctx), EngineFlow.eec1(ctx)) { a, e -> Dashboard(a, e) }
    .collect { render(it) }
```

## 사용 — core 제네릭 (동적 신호 / 도메인 모듈 없는 신호)

```kotlin
val v = AgVehicle.shared(this)
v.addConnectionListener(object : AgVehicle.ConnectionListener {
    override fun onConnected() {
        val sub = v.subscribe("mypkg:MY_SIGNAL", intervalMs = 200) { value ->
            runOnUiThread { textView.text = value.text }   // "52.4 %"
        }
        v.requestCatalog { metas -> /* 전체 신호 목록 */ }
    }
})
val ctrl = v.acquire("SOME_CMD") { lockUi() }   // null = 자격/보유/미연결
ctrl?.send(125)                                  // raw
```

- `shared(context)`는 **프로세스당 연결 하나**를 준다 — 도메인 모듈들과 앱이 공유한다
  (앱마다 여러 연결을 만들면 서비스가 콜백을 덮어쓴다)
- 콜백은 binder 스레드 — UI 갱신은 앱이 `runOnUiThread`로
- 재연결 시 구독은 자동 복원, **제어 세션은 안전을 위해 재획득**이 필요
- 제어 토큰은 세션 핸들 안에 봉인 — 앱 코드에 노출되지 않는다
- `subscribe`/`subscribeMessage`의 `intervalMs`(기본 0)는 leading-edge 스로틀 —
  0이면 오는 값을 전부 전달(하위호환), 0보다 크면 최대 그 간격마다 최신값 1회만
  전달한다(타이머 없이 값이 들어온 시점에 판정). 게이트는 구독 인스턴스별 독립이라
  신호마다 다른 주기를 걸 수 있다

## 자격 선언 (제어 쓸 때)

코드로는 선언할 수 없다 — 서비스가 설치 시 고정된 매니페스트를 PackageManager로
직접 읽어 위조를 막는다.

```xml
<application>
    <meta-data android:name="farm.agmo.vehicle.USES_CONTROL"
               android:value="HITCH_CMD,PTO_CMD" />   <!-- 쉼표 구분 -->
</application>
```

| 키 | 뜻 |
|---|---|
| `USES_CONTROL` | 반드시 필요한 제어 신호 — 같은 신호를 쓰는 앱과 동시 실행 차단 |
| `OPT_CONTROL` | 있으면 좋은 제어 신호 — 겹치면 그 기능만 degrade |
| `CONFLICT_WITH` | 공존 불가한 앱 패키지명 — 신호 무관 차단 |

## 새 도메인 모듈 추가하기

내장 신호가 데몬에 있으면(agcand `signal_defs.cpp`) 도메인 모듈은 얇다:
1. 순수 로직 — 메시지별 데이터 클래스 + `from(map)` 팩토리(`ImuModel`), 변환(`HitchScale`). Android 무의존 → JVM 테스트
2. 파사드 — `Xxx.start(context)`/`Xxx.control(context)`가 `AgVehicle.shared` 위에 구독/제어
3. `settings.gradle.kts`에 모듈 include + `build.gradle.kts`(api project(":core"))

IMU 등 신규 도메인은 데몬 신호 테이블에 신호를 먼저 추가한 뒤 같은 틀로 만든다.

## 개발 (SDK 자체)

```sh
./gradlew :core:assembleRelease :hitch:assembleRelease :engine:assembleRelease
./gradlew publish        # GitHub Packages 배포 (GITHUB_ACTOR/GITHUB_TOKEN)
```

순수 로직(`Model.kt`/`ImuModel.kt`/`EngineModel.kt`/`HitchModel.kt`)은 Android 무의존이라
JVM에서 단독 테스트된다. AIDL(`core/src/main/aidl/`)이 이 저장소의 **정본** —
서비스(agvehicled)가 같은 파일을 동기화해 쓴다. 계약을 바꾸면 양쪽을 함께 올린다.
