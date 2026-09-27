# ADR-0007: Montage(원티드) 디자인 토큰 기반 Compose 테마

## Status
Accepted

## Date
2026-09-27

## Context
CalisVision은 지금까지 Material 3 기본 `lightColorScheme()`/`darkColorScheme()`만 쓰는 빈 테마였다. 분석 결과 화면(각도·결함·홀드 구간·스켈레톤 오버레이)을 만들기 전에 색·타이포·간격 체계를 확정해야 한다. 한국어 UI에 맞는 검증된 디자인 시스템을 원하고, 사용자 결정(2026-09-27)은 **"Montage 토큰만 가져오고 라이브러리 의존성은 두지 않는다"**이다.

제약:
- 온디바이스·오프라인 앱: 매니페스트에 INTERNET 권한 0개 유지(ADR-0001, ADR-0005).
- 툴체인이 최신(AGP 9.4, Kotlin 2.4.20, Compose BOM 2026.09.00, compileSdk 37)이라 외부 UI 라이브러리의 바이너리 호환성이 불확실.
- 라이선스가 명확해야 한다(재배포·APK 번들).

## Decision
원티드 Montage 디자인 시스템(https://github.com/wanteddev/montage-android v3.7.0, MIT, https://montage.wanted.co.kr)의 **토큰 값만** Kotlin으로 전사해 `app/src/main/java/com/calisvision/ui/theme/`에 둔다.

| 파일 | 내용 | 원본(montage-android v3.7.0) |
|---|---|---|
| `MontageTokens.kt` | `MontageAtomic`(아토믹 팔레트), `MontageColors` + `MontageLightColors`/`MontageDarkColors`(시맨틱), `MontageOpacity`, `MontageSpacing`, `MontageRadius`, `MontageShadow` | `res/values/design_system_atomic_colors.xml`, `res/values{,-night}/design_system_semantic_colors.xml`, `res/values/dimens.xml`, `theme/Shape.kt`, `base/WantedDropShadow.kt` |
| `MontageTypography.kt` | display1~caption2 × Regular/Medium/Bold (크기·lineHeight·letterSpacing·굵기 그대로), `Pretendard` FontFamily | `theme/Typography.kt` |
| `CalisColors.kt` | 앱 전용 역할(아래 표) | Montage 시맨틱 토큰 재사용 |
| `Theme.kt` | `CalisVisionTheme` + `CalisTheme.colors/appColors/typography/spacing/radius/shadow` | — |

`CalisVisionTheme`은 시스템 다크 모드를 따르고, Montage 토큰을 CompositionLocal로 제공하면서 동시에 Material 3 `colorScheme`/`typography`/`shapes`에 매핑해 기본 M3 컴포넌트도 Montage와 일관되게 보이게 한다.

M3 색 매핑(주요): primary/secondary/tertiary = `primaryNormal`, on* = `staticWhite`, background/surface = `backgroundNormalNormal`, onBackground/onSurface = `labelNormal`, surfaceVariant = `backgroundNormalAlternative`, onSurfaceVariant = `labelAlternative`, surfaceContainer* = `backgroundElevated*`, error = `statusNegative`, errorContainer = `backgroundStatusNegative`, outline = `lineNormalNormal`, outlineVariant = `lineNormalAlternative`, inverse* = `inverse*`, scrim = `materialDimmer`. (Montage 자체 매핑은 `error = backgroundNormalNormal`, `onPrimary = labelNormal`이라 M3 버튼 대비가 깨져 따르지 않았다.)

M3 타이포 매핑: display{Large,Medium,Small} = display{1,2,3}Bold, headline{Large,Medium,Small} = title{1,2,3}Bold, titleLarge = heading1Bold, titleMedium = headline2Bold, titleSmall = label1Bold, body{Large,Medium,Small} = body1/body2/caption1 Regular, label{Large,Medium,Small} = label1/caption1/caption2 Medium.

앱 전용 역할(`CalisTheme.appColors`):

| 역할 | Light | Dark | 근거 토큰 |
|---|---|---|---|
| `poseOk`, `jointNormal` | `#00BF40` | `#1ED45A` | status.positive (green 50 / 60) |
| `fault` | `#FF4242` | `#FF6363` | status.negative (red 50 / 60) |
| `faultBackground` | `#14FF4242` | `#14FF6363` | background.status.negative |
| `warning` | `#FF9200` | `#FFA938` | status.cautionary (orange 50 / 60) |
| `warningBackground` | `#14FF9200` | `#14FFA938` | background.status.cautionary |
| `holdSegmentHighlight` | `#383366FF` | `#385B84FF` | primary_normal_opacity22 |
| `skeletonLine` | `#E0FFFFFF` | `#E0FFFFFF` | static_white_opacity88 (영상 프레임 위라 테마 무관) |

서체: Montage는 Pretendard(JP 1.301, 굵기별 약 3.7MB OTF)를 쓴다. 풀 한글 Pretendard 1.3.9 static OTF 4종은 6.3MB, Variable TTF는 6.7MB라 예산(~2MB) 초과이고, 서브셋은 OFL 수정본이라 Reserved Font Name "Pretendard"를 쓸 수 없어 이름 변경이 필요하다. 그래서 저자가 공식 배포하는 **Pretendard Std 1.3.9**(라틴·그리스·키릴, 한글 없음) static OTF 4종(400/500/600/700)을 **무수정** 번들한다. 합계 1,272,556 bytes, 출처 https://github.com/orioncactus/pretendard/releases/tag/v1.3.9 (`PretendardStd-1.3.9.zip`). 한글 글리프는 시스템 서체(Noto Sans CJK KR 등)로 폴백된다.

라이선스 고지는 `docs/THIRD_PARTY_NOTICES.md`(Montage MIT 전문, Pretendard Std OFL 1.1 전문)와 토큰 파일 헤더 주석에 둔다.

## Drivers
1. 라이브러리 의존성 0 — 툴체인 호환성·권한·빌드 소스(JitPack) 리스크 제거 (사용자 결정)
2. 라이선스 명확성 — MIT(토큰), OFL 1.1(서체), 재배포 조건 충족
3. 한국어 사용자에게 익숙하고 light/dark 시맨틱이 완비된 검증된 체계
4. 값의 충실도 — 원본 리소스에서 그대로 전사, 임의 값 없음
5. APK 크기(서체 ≤ ~2MB)

## Alternatives
- **Toss TDS** — 완성도는 높지만 디자인 자산 IP가 토스에 있고 "앱인토스(Apps-in-Toss)" 미니앱용으로만 제공되어 독립 앱에서 쓸 권리가 없다. 기각.
- **montage-android 라이브러리 직접 의존** — 컴포넌트까지 쓸 수 있으나 JitPack 배포, Compose 1.12/Kotlin 2.4 대비 바이너리 호환 불확실, Lottie·Glide 등 전이 의존성(라이브러리 자체 매니페스트는 권한 없음, 전이 의존성 매니페스트 병합 결과는 별도 검증 필요 → INTERNET 유입 가능성), 풀 Pretendard 15MB 번들. INTERNET 0 제약과 충돌할 수 있어 기각.
- **순수 Material 3** — 의존성·작업량 최소지만 M3 기본 팔레트/타입 스케일은 한국어 UI 기준 행간·자간이 맞지 않고, 상태 색(positive/cautionary/negative) 체계를 직접 설계해야 한다. 기각.

## Why chosen
토큰 전사는 Montage의 시각 언어(색·타입·간격)를 얻으면서 라이브러리 채택의 위험(호환성·권한·크기)을 전부 피한다. Material 3 위에 매핑하므로 기존 M3 컴포넌트를 그대로 쓰고, 필요할 때만 `CalisTheme.*`로 세밀한 토큰에 접근한다. MIT라 값 복제·재배포에 제약이 없다.

## Consequences
- (+) 외부 의존성 0, INTERNET 권한 0 유지. 토큰이 코드에 있어 IDE 탐색·리팩터가 쉽다.
- (+) 앱 역할 색(fault/warning/pose OK 등)이 Montage 상태 색과 일관되고 light/dark 모두 정의됨.
- (−) Montage 업스트림 변경은 자동 반영되지 않는다. 수동 재동기화 필요.
- (−) 컴포넌트(버튼·칩·바텀시트 등)는 없으므로 M3 컴포넌트를 토큰으로 스타일링해야 한다.
- (−) 한글은 Pretendard가 아닌 시스템 서체로 렌더링되어 라틴/숫자와 서체가 섞인다(숫자·각도 표시는 Pretendard Std).
- (−) debug APK +891,701 bytes(95,924,936 → 96,816,637, 서체 원본 1.27MB 압축 후).
- (!) `*_opacityNN` 보조 색 전체는 전사하지 않았다(참조하는 것만 사용). 필요 시 원본에서 추가.

## Follow-ups
- 한글 Pretendard가 필요해지면: KS X 1001 2350자 서브셋을 OFL에 따라 **다른 이름**(예: "CalisSans")으로 재명명해 번들하는 안을 검토(예상 4종 합계 ~2MB).
- Montage 새 릴리스 시 `design_system_*_colors.xml`, `Typography.kt` diff로 토큰 재동기화.
- 분석 화면 구현 시 `CalisTheme.appColors` 역할이 부족하면(예: 관절별 색) 이 ADR에 역할 추가.
- 스켈레톤 오버레이의 가독성(밝은 배경 영상 위 흰 선)을 실기기에서 확인, 필요 시 외곽선(`staticBlack` opacity) 추가.
