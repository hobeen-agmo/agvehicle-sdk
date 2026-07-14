# AgVehicle SDK

농기계 HMI 플랫폼의 **차량 신호 SDK**. 앱에서 CAN 신호를 읽고(구독) 제어(쓰기)하는
유일한 공개 창구다. 내부적으로 시스템 서비스(agvehicled)에 `bindService`로 붙고,
서비스가 데몬(agcand)을 거쳐 CAN 버스와 통신한다 — 앱은 그 계층을 몰라도 된다.

```
앱 (이 SDK) ──bindService──▶ agvehicled ──소켓──▶ agcand ──▶ CAN 버스
```

## 설치 (Gradle)

GitHub Packages(Maven)에서 받는다:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/hobeen-agmo/agvehicle-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.token").get()   // read:packages 토큰
            }
        }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("farm.agmo.vehicle:agvehicle-sdk:0.1.0")
}
```

## 자격 선언 (매니페스트 필수)

제어(쓰기)를 쓰려면 앱 매니페스트에 선언해야 한다. **코드로는 선언할 수 없다** —
서비스가 설치 시 고정된 매니페스트를 PackageManager로 직접 읽어 위조를 막는다.

```xml
<application>
    <meta-data android:name="farm.agmo.vehicle.USES_CONTROL"
               android:value="HITCH_CMD,PTO_CMD" />   <!-- 쉼표 구분 -->
</application>
```

| 키 | 뜻 |
|---|---|
| `USES_CONTROL` | 반드시 필요한 제어 신호 — 같은 신호를 쓰는 앱과 동시 실행 차단 |
| `OPT_CONTROL` | 있으면 좋은 제어 신호 — 겹치면 그 기능만 degrade(차단 안 함) |
| `CONFLICT_WITH` | 공존 불가한 앱의 패키지명 — 신호 무관 차단 |

## 사용

```kotlin
class MainActivity : Activity(), AgVehicle.Listener {
    private lateinit var vehicle: AgVehicle
    private var hitch: AgVehicle.ControlSession? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        vehicle = AgVehicle.bind(this, this) ?: run {
            finish(); return   // 서비스 미설치
        }
    }

    // 연결 완료 시점 — 여기서부터 구독/제어 가능 (재연결 시에도 호출)
    override fun onConnected() {
        vehicle.subscribe("ENGTEMP")           // 내장 신호는 bare 이름
        vehicle.subscribe("mypkg:MY_SIGNAL")   // 외부 신호는 owner:signal
        vehicle.requestCatalog { metas ->      // 전체 신호 목록
            metas.filter { it.writable }.forEach { println(it.key) }
        }
    }

    // 콜백은 binder 스레드 — UI 갱신은 runOnUiThread
    override fun onValue(v: SignalValue) = runOnUiThread {
        textView.text = "${v.key}: ${v.text}"   // "92.5 degC"
        // 계산용은 v.number / v.unit 으로 분해
    }
    override fun onStale(key: String) { /* 신호 끊김 — 값 신뢰 불가 */ }
    override fun onControlLost(key: String) = runOnUiThread {
        hitch = null; lockControlUi()           // 상위 계층에 선점당함 — 즉시 잠금
    }

    fun onHitchButton() {
        hitch = vehicle.acquire("HITCH_CMD")    // null = 자격 없음 / 보유 중
        hitch?.send(125)                        // raw 값. false = 세션 상실
    }

    override fun onDestroy() {
        hitch?.release()                        // 정상 반납 = 마지막값 유지
        vehicle.close()
        super.onDestroy()
    }
}
```

### 제어 반납 두 가지

- `release()` — 정상 반납. **마지막 명령값 유지** (동작을 이어감)
- `stopAndRelease()` — 안전값으로 되돌린 뒤 반납 (앱 자발 비상정지)
- 앱이 그냥 죽어도 서비스가 STOP 선행 후 세션을 회수한다 (안전 기본값)

## 외부 CAN 신호 정의

자기 CAN 신호를 등록하면 다른 앱도 카탈로그에서 보고 구독할 수 있다(설치 시 1회):

```kotlin
val accepted = vehicle.registerDefs(defsJson) { accepted, errors ->
    errors.forEach { println("거부: $it") }   // 부분 수용 — 유효한 것만 등록
}
```

정의 형식(JSON)과 컬럼 의미는 플랫폼 개발자 문서 참조. 이미 정의된 신호는
재정의 불가(선점·불변) — 먼저 등록한 소유자가 이긴다.

## 동작 원리 (알아두면 좋은 것)

- **재연결 자동 복원**: 서비스가 재시작되면 SDK가 자동 재바인딩하고 구독을 복원한다.
  제어 세션은 안전을 위해 **재획득이 필요**하다(자동 복원하지 않음).
- **느린 앱 보호**: 콜백은 oneway라 앱이 느려도 서비스·다른 앱에 영향 없다.
- **토큰 은닉**: 제어 토큰은 `ControlSession` 안에 봉인돼 앱 코드에 노출되지 않는다.

## 개발 (SDK 자체)

```sh
./gradlew :assembleRelease        # AAR 빌드 → build/outputs/aar/
./gradlew publish                 # GitHub Packages 배포 (GITHUB_ACTOR/GITHUB_TOKEN)
```

순수 모델(`Model.kt`)은 Android 무의존이라 JVM에서 단독 테스트된다.
AIDL(`src/main/aidl/`)이 이 저장소의 **정본** — 서비스(agvehicled)가 같은 파일을
동기화해 쓴다. 계약을 바꾸면 양쪽을 함께 올려야 한다.
