# SDK 도메인 · 네임스페이스 컨벤션

AgVehicle SDK의 도메인 모듈을 **공개 표준**과 **AGMO 제조사 고유(proprietary)**로 나누는
기준과 명명 규칙을 정한다. import·모듈명만 봐도 "표준인지 제조사 고유인지"가 드러나게 하는 것이 목표다.

관련 문서: agcand `docs/signal-domains.md`(트랙·도메인), `docs/external-can.md`(외부 정의 파이프라인),
`reference/README.md`(참조 정의의 라이선스·provenance 규율).

---

## 1. 도메인 분류 원칙

| 구분 | 기준 | 예 |
|---|---|---|
| **표준(standard)** | 공개 규격(SAE J1939-71 / ISO 11783)으로 정의가 재작성 가능한 신호. 특정 제조사 소유가 아님. | 엔진(EEC1/ET1…), 히치 위치, IMU 자세, 차속/배터리/DPF |
| **AGMO 제조사 고유(proprietary)** | AGMO 차량/작업기의 고유 프레임(공개 표준에 없는 내부 제어·상태). AGMO 소유. | 커스텀 트랙터 FNR/변속/PTO/유압·4WD/AutoLift 제어, 조향모터, 살포기, 자이로 |

판단 규칙: **공개 표준 문서만으로 값을 재작성할 수 있으면 표준**, 그렇지 않고 제조사 고유
프레임이면 **oem**. 애매하면 표준으로 커버되는 부분과 고유 부분을 분리한다(예: IMU 자세=표준 `:imu`,
제조사 고유 자이로=`:oem-imu`).

---

## 2. 네임스페이스 규칙

| 구분 | Kotlin 패키지 | 의미 |
|---|---|---|
| 표준 | `farm.agmo.vehicle.<domain>` | engine/hitch/imu/vehicle/flow |
| AGMO 고유 | `farm.agmo.vehicle.oem.<domain>` | import 문에서 `.oem.`이 보여 제조사 고유임이 즉시 드러남 |

```kotlin
import farm.agmo.vehicle.engine.Engine          // 표준
import farm.agmo.vehicle.oem.tractor.Tractor     // ← .oem. = 제조사 고유
```

각 oem 모듈 소스 헤더에는 `🏭 AGMO 제조사 고유 (proprietary) — 표준 아님`을 명시한다.

---

## 3. Gradle 모듈 명명

| 구분 | 모듈 좌표 | 접두사 |
|---|---|---|
| 표준 | `:<domain>` | 없음 |
| AGMO 고유 | `:oem-<domain>` | `oem-` |

### 현재 모듈 목록

| 모듈 | 구분 | 내용 |
|---|---|---|
| `:core` | 표준(기반) | 연결·제네릭 구독/제어·카탈로그·`Signal` 베이스 |
| `:engine` | 표준 | 엔진 읽기(EEC1/수온/오일압/연료/가동시간) |
| `:hitch` | 표준 | 히치 위치 읽기 + 제어 |
| `:imu` | 표준 | 자세 읽기(각도/각속도/가속도) |
| `:vehicle` | 표준 | 홈 런처 상태(차속/PTO/배터리/DPF/GPS) |
| `:flow` | 표준(옵션) | 위 신호들의 Kotlin Flow 래퍼 |
| `:oem-tractor` | **AGMO 고유** | 커스텀 트랙터 FNR/변속/PTO/유압/ACC 읽기 + 4WD/AutoLift/히치 제어 |
| `:oem-steer` | **AGMO 고유** | 조향모터 조향각·모터 상태 읽기 + 조향 명령 |
| `:oem-spreader` | **AGMO 고유** | 살포기(작업기) 살포율/게이트/섹션 읽기 + 살포율 제어 |
| `:oem-imu` | **AGMO 고유** | 제조사 고유 자이로(Allynav_R70) + MTLT305 설정(표준 자세는 `:imu`) |

---

## 4. 도메인 ↔ 기기 / 정의 매핑

| 모듈 | 기기 | agcand 정의(참조) | provenance |
|---|---|---|---|
| `:engine`/`:hitch`/`:imu`/`:vehicle` | 표준 J1939/ISOBUS | 내장 `signal_defs`(공개 표준 재작성) | 공개 표준(SAE J1939-71 / ISO 11783) |
| `:oem-tractor` | AGMO Customized Tractor | `CAN_AGMO_Customized_Tractor` | AGMO 소유 기기. **정의 값은 AGMO 1차 CAN 소스(원본 DBC/스펙) 기준**으로 작성 — NEVONEX 디컴파일 추출본은 채택하지 않음(reference/README 규율) |
| `:oem-steer` | AGMO SteerMotor | `CAN_AGMO_SteerMotor` | 〃 |
| `:oem-spreader` | AGMO RDA_Spreader | `CAN_AGMO_RDA_Spreader` | 〃 |
| `:oem-imu` | Allynav_R70 / MTLT305 | `CAN_AGMO_Allynav_R70` / `CAN_AGMO_MTLT305` | 〃 (표준 자세 데이터는 `:imu`) |

> ⚠️ oem 모듈의 실제 CAN 키·PGN·비트·스케일 값은 **AGMO 1차 소스가 확정될 때까지 `TODO(B)`
> 플레이스홀더**로 둔다. 디컴파일 추출본(`reference/nevonex/*`)의 값을 그대로 옮기지 않는다.

---

## 5. 새 도메인 추가 절차

1. **표준 신호인가?** (공개 J1939/ISO 문서로 재작성 가능?)
   - 예 → `farm.agmo.vehicle.<domain>` + `:<domain>` 모듈. 정의는 공개 표준 근거로 작성.
2. **제조사 고유인가?**
   - 예 → `farm.agmo.vehicle.oem.<domain>` + `:oem-<domain>` 모듈.
   - 소스 헤더에 `🏭 AGMO 제조사 고유 (proprietary)` 명시.
   - 정의 값은 **AGMO 1차 소스 기준**으로 작성(디컴파일 추출본 미채택).
   - 정의 배치: 제품 defs 경로. `settings.gradle.kts`에 `:oem-<domain>` 등록.
3. 표준 모듈과 oem 모듈을 **섞지 않는다**(표준 모듈에 제조사 고유 신호를 넣지 않음).

---

## 6. 금지 (명문화)

- **타사 OEM 정의의 SDK 편입 금지**: `Audi_A4_B6`, `Komatsu`, `Claas_Implement`,
  `LINAK_Techline`, `REMO`, `WeatherStation`, `Brightness_Sensor`, `Environment_Amasense`,
  `GenericIO` 등 제3자/타사 정의는 SDK 도메인 모듈로 편입하지 않는다(gitignore 유지, 제품 미포함).
- **디컴파일 추출본 값의 무단 채택 금지**: oem 모듈 정의는 AGMO 1차 소스 또는 공개 표준을
  근거로 작성한다. `reference/nevonex/*`(NEVONEX 디컴파일 파생)의 값을 그대로 옮기지 않는다.
