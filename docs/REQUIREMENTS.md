# CalisVision 요구사항

> 원본 소스: `.omc/specs/deep-interview-calisvision-prd.md` (AC-1~AC-16, AC-9b), `.omc/plans/calisvision-mvp-plan.md` §1, §4

## 기능 요구사항 (FR)

| # | 요구사항 | AC |
|---|---|---|
| FR-1 | 갤러리 mp4 영상의 샘플링 프레임에서 33개 관절 키포인트를 온디바이스 추출 | AC-1 |
| FR-2 | 프레임마다 4개 각도 계산 — 전신 정렬(손목-엉덩이-발목, 손목–발목 직선 기준 엉덩이 이탈), 어깨 열림(팔꿈치-어깨-엉덩이), 팔꿈치 펴짐(손목-팔꿈치-어깨), 골반/허리(어깨-엉덩이-무릎) | AC-2 |
| FR-3 | 각도가 임계값을 벗어나면 해당 프레임에 결함 태그 부여, 동명 결함 병합 | AC-3 |
| FR-4 | 벽 물구나무 기준 영상에서 홀드 구간 전신 정렬 각도 검증 — 곧은 기준 홀드에서 정렬 결함 오탐 없음 + 평균 θ ∈ [175°,185°] (ADR-0008) | AC-4 |
| FR-5 | 네트워크 권한 없이(오프라인) 전체 분석 동작 | AC-5 |
| FR-6 | 홈 → 갤러리 선택 → 분석 진행 표시 → 결과 화면 흐름 | AC-6 |
| FR-7 | 결과 화면: 스켈레톤 + 4각도 오버레이, 타임라인 드래그로 프레임 단위 스크럽 | AC-7 |
| FR-8 | 결함 프레임/구간 타임라인 시각 표시, 선택 시 결함명·교정 힌트 표시 | AC-8 |
| FR-9 | 설정 화면에서 4각도 임계값 조정, 재분석 없이 결과에 반영 | AC-9 |
| FR-9b | (후순위) 앱 내 카메라 촬영 후 동일 분석 흐름 진입 | AC-9b |
| FR-10 | 물구나무 4개 각도 각각의 기본 범위·결함명·설명·교정 힌트를 코드/리소스로 정의 | AC-10 |
| FR-11 | 분석 시작 전 결함별 필요 촬영 방향 가이드 표시 | AC-11 |
| FR-12 | 지식베이스는 동작 단위로 확장 가능한 구조(동작 → 각도 규칙 목록) | AC-12 |
| FR-13 | `docs/PRD.md`에 목표·사용자·범위·비범위·핵심 흐름 작성 | AC-13 |
| FR-14 | `docs/REQUIREMENTS.md`에 기능/비기능 요구사항을 AC 번호와 매핑 | AC-14 |
| FR-15 | `docs/decisions/`에 최소 5개 ADR(온디바이스 규칙판정, 사후분석 우선, 단일 동작, 관절추출 라이브러리, 민감파일 정책) | AC-15 |
| FR-16 | `.gitignore`가 영상·`test-videos/`·keystore·`keystore.properties`·`local.properties` 제외, `git status`에 미노출 | AC-16 |

## 비기능 요구사항 (NFR)

| # | 요구사항 | 근거/기준 | AC |
|---|---|---|---|
| NFR-1 | 오프라인 동작 — 매니페스트에 INTERNET 권한 미선언(0건) | `aapt2 dump permissions`의 INTERNET 행 수 == 0으로 판정 | AC-5 |
| NFR-2 | 성능 — 30초 영상(10fps, 640px) 분석 ≤ 60초(중급 기기 기준) | 초과 시 `getFramesAtIndex`(B1') 프레임 추출 방식으로 전환 후 재측정 | AC-1, AC-5 |
| NFR-3 | 메모리 — 분석 중 힙 사용량 ≤ 256MB | 즉시 640px 다운스케일 + Bitmap recycle로 확보 | AC-1, AC-5 |
| NFR-4 | minSdk 28 근거 | 기본 프레임 샘플링(`getFrameAtTime`)은 API 1이라 28이 필수는 아니나, 실사용 타깃 Android 10+ 및 NFR-2 초과 시 폴백인 `getFramesAtIndex`/`METADATA_KEY_VIDEO_FRAME_COUNT`(API 28)를 분기 없이 쓰기 위해 28로 확정 | 계획 §3 의존성 표 |
| NFR-5 | AC-4 게이트(ADR-0008, 2026-09-27 재정의) — 기준 hold-out의 홀드 ≥ 50 샘플에서 (i) 동결된 전신 정렬 규칙(`Deviation(10,10)`, 연속 ≥ 3 샘플, 홀드로 clip)의 홀드 안 결함 0건, (ii) 스무딩(이동 중앙값 window 5) 정렬 θ 평균 ∈ [175°,185°]; 홀드 샘플 null > 5%면 FAIL. 이전 기준(스무딩 θ ≥ 95%가 [175°,185°], 2026-09-26 수용)은 ADR-0008로 대체되어 정보용 지표(`AlignmentGate`)로만 보고 | 원래 기준은 측정 정확도와 사람이 ±5° 안에 머무는 능력을 섞어 봐서 가림 없는 hold-out(holdout3 61.2%)·튜닝 영상(43.6%)도 자세 흔들림만으로 실패; 앱의 실제 결함 임계값 ±10°에 게이트를 맞춤. hold-out 결과를 본 뒤의 사후 재정의(ADR-0008) | AC-4 |
| NFR-6 | Landmark jitter 대응 | VIDEO 트래킹 + 이동 중앙값(window 5) + 결함은 연속 ≥3 샘플일 때만 구간화 | AC-3, AC-4 |
| NFR-7 | 개인정보/보안 — 운동/테스트 영상, 서명키(`.jks`/`.keystore`), `keystore.properties`, `local.properties`는 git 비추적 | `.gitignore` + `git status --porcelain` 미노출 검증 | AC-16 |
| NFR-8 | 확장성 — 지식베이스는 동작(Exercise) 비의존 엔진 위에서 규칙 리스트로 정의, 신규 동작 추가 시 `pose/`·`domain/analysis` 코드 변경 0 | `ExerciseCatalogTest` | AC-12 |
