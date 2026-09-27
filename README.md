# CalisVision

맨몸운동 고난도 동작의 자세 결함을 영상으로 찾아 주는 안드로이드 앱입니다. 1차 버전은 **물구나무 서기**를 지원합니다.

측면에서 찍은 물구나무 영상을 고르면 앱이 폰 안에서 관절을 찾고 전신 정렬·골반·어깨·팔꿈치 네 가지 각도를 0.1초 단위로 잽니다. 바나나 등, 파이크, 어깨 닫힘처럼 기준을 벗어난 자세는 결함으로 표시하고 교정 힌트를 보여 줍니다. 영상은 폰 밖으로 나가지 않습니다.

## 주요 기능

- **촬영 가이드**: 측면 90°, 엉덩이 높이 삼각대, 2–3m 거리, 손·발이 가려지지 않게, 7초 이상 홀드. 가이드를 거쳐야 갤러리가 열립니다.
- **온디바이스 분석**: MediaPipe Pose Landmarker로 관절 33개를 찾고 규칙 기반으로 각도와 결함을 판정합니다. 서버, 계정, 네트워크 권한이 없습니다.
- **결과 화면**: 프레임 위에 관절과 손목–발목 기준선을 그리고 타임라인을 한 칸(0.1초)씩 넘기며 각도 네 개를 확인합니다. 홀드 구간 안의 결함만 목록에 올리고 누르면 설명과 교정 힌트가 나옵니다.
- **임계값 설정**: 규칙마다 허용 범위를 바꿀 수 있고 바꾼 값은 다시 분석하지 않고 결과에 바로 반영됩니다.

| 규칙 | 관절 | 기본 기준 | 결함 |
|---|---|---|---|
| 전신 정렬 | 손목–엉덩이–발목 | 180° ±10° | 바나나 등 / 파이크 |
| 골반·허리 | 어깨–엉덩이–무릎 | 180° ±15° | 골반 전방경사 / 파이크 |
| 어깨 열림 | 팔꿈치–어깨–엉덩이 | 165–195° | 어깨 닫힘 |
| 팔꿈치 펴짐 | 손목–팔꿈치–어깨 | 170–190° | 팔꿈치 굽힘 |

## 기술 스택

| 항목 | 버전 |
|---|---|
| Kotlin / AGP / Gradle | 2.4.20 / 9.4.1 / 9.6.1 |
| Jetpack Compose BOM | 2026.09.00 (Material 3, Navigation Compose, DataStore) |
| MediaPipe Tasks Vision | 1.0.0 (`pose_landmarker_full.task`, CPU) |
| SDK | minSdk 28, targetSdk 36, compileSdk 37 |
| 디자인 | 원티드 [Montage](https://montage.wanted.co.kr) 토큰(MIT), Pretendard 1.3.9(OFL) |

## 빌드와 실행

JDK 17과 Android SDK(build-tools 36 이상)가 필요합니다. 저장소 루트에 `local.properties`를 만들고 SDK 경로를 적어 주세요. 이 파일은 git에 올라가지 않습니다.

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

```bash
./gradlew assembleDebug          # 디버그 APK 빌드
./gradlew installDebug           # 연결된 기기에 설치
```

릴리스 서명은 `keystore.properties`(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`)가 있을 때만 켜집니다. keystore와 이 파일도 git에 올리지 않습니다.

## 테스트

```bash
./gradlew testDebugUnitTest      # JVM 단위 테스트 (기준 영상 관절 데이터 포함)

# 기기 UI 테스트 (영상이 필요한 테스트 제외)
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.calisvision.test.RequiresVideo
```

영상이 필요한 계측 테스트(`@RequiresVideo`)는 기기의 `/sdcard/Android/data/com.calisvision/files/test-videos/`에 영상을 넣어야 돌아갑니다. 연결 테스트가 끝나면 앱과 함께 이 폴더가 지워지므로, 다시 쓰려면 `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`를 붙여 주세요.

## 프로젝트 구조

```
app/src/main/java/com/calisvision/
├─ domain/      각도 계산, 규칙·지식베이스, 스무딩·홀드 검출·결함 판정 (순수 Kotlin, Android 의존 없음)
│  ├─ geometry/   signed 각도, 몸 앞쪽 방향, 측면 선택
│  ├─ rules/      Exercise, PoseRule, AngleThreshold
│  ├─ knowledge/  물구나무 4개 규칙과 결함 설명·교정 힌트, 동작 카탈로그
│  └─ analysis/   스무딩, 홀드 검출, 결함 판정, 기준 영상 판정
├─ pose/        MediaPipe 추론, 영상 방향(0°/180°) 결정
├─ video/       MediaCodec 순차 디코딩, 분석 파이프라인
├─ data/        의존성 컨테이너, 분석 결과 저장소(메모리), 임계값 저장소(DataStore)
└─ ui/          Compose 화면(홈·가이드·분석·결과·설정), Montage 토큰 테마
```

## 문서

| 문서 | 내용 |
|---|---|
| [docs/PRD.md](docs/PRD.md) | 목표, 사용자, 범위, 핵심 흐름 |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | 기능·비기능 요구사항과 수용 기준(AC-1~16, AC-9b) |
| [docs/decisions/](docs/decisions/) | 설계 결정 기록(ADR-0001~0009) |
| [docs/VERIFICATION.md](docs/VERIFICATION.md) | 실기기 검증 결과, AC 대조표, 성능·메모리 측정 |
| [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md) | Montage, Pretendard 라이선스 |

## 검증 결과

Galaxy Z Flip7(SM-F766N, Android 16) 한 대에서 측정했습니다.

- 수용 기준 17개 중 16개 통과. 앱 내 촬영(AC-9b)은 아직 만들지 않았습니다.
- 30초 영상 분석 42–43초(목표 60초 이하), Java 힙 최대 34.8MB(목표 256MB 이하).
- APK에 INTERNET 권한이 없고 비행기 모드에서도 분석이 끝까지 됩니다.
- 기준 영상 판정(AC-4)은 결과를 본 뒤 "곧게 선 기준 영상에서 정렬 결함이 나오지 않을 것"으로 다시 정의해 통과했습니다. 원래 기준(정렬각 175–185° 안에 95%)으로는 61.2%로 통과하지 못했습니다. 경위는 [ADR-0008](docs/decisions/ADR-0008-ac4-reference-hold-gate.md)에 있습니다.

## 알려진 한계

- 기준 영상이 모두 바닥 가까이에서 올려다본 구도라 원근 때문에 각도가 틀어질 수 있습니다.
- 손목이 가려져도 MediaPipe가 visibility를 높게 내는 경우가 있어 가림을 완전히 걸러 내지 못합니다.
- 고정 프레임레이트, 8-bit 영상을 전제로 합니다. HDR·10-bit 영상은 지원하지 않습니다.
- 분석 결과는 메모리에만 두기 때문에 앱을 닫으면 사라집니다.

## 다음 단계

- 앱 내 촬영(AC-9b)
- 가려진 관절과 불가능한 각도를 "측정 불가"로 거르는 규칙
- 프론트레버·플란체 등 다른 동작 추가(동작 카탈로그에 규칙만 더하면 되도록 설계됨)

## 민감 정보

운동 영상(`*.mp4`, `*.mov` 등, `test-videos/`), 서명 키(`*.jks`, `*.keystore`, `keystore.properties`), `local.properties`는 git에서 제외합니다([ADR-0005](docs/decisions/ADR-0005-sensitive-file-policy.md)). 포즈 모델 파일(`pose_landmarker_full.task`)은 오프라인에서 동작하도록 저장소에 포함합니다.

## 라이선스

디자인 토큰은 원티드 Montage(MIT), 서체는 Pretendard(SIL OFL 1.1)를 사용합니다. 자세한 고지는 [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)를 참고하세요.
