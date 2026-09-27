# ADR-0004: 관절 추출 라이브러리 — MediaPipe Pose Landmarker

## Status
Accepted

## Date
2026-09-26

## Context
온디바이스로 33개 관절 키포인트를 추출할 라이브러리가 필요하다. 물구나무는 역자세라 landmark 신뢰도와 방향(0°/180°) 판단이 특히 중요하다.

## Decision
`com.google.mediapipe:tasks-vision:1.0.0`의 Pose Landmarker(`pose_landmarker_full.task`)를 채택한다. 방향 결정은 영상 중간 50% 구간 샘플을 IMAGE 모드로 0°/180° 각각 추론해 `0.7·visibility + 0.3·prior` 점수로 고르고, 본 처리는 확정된 회전값으로 VIDEO 모드 1회 실행한다. 이 결정은 De-risk Spike(P1.5) 게이트 통과를 조건으로 확정된다.

## Drivers
- 33 landmark + visibility/presence 제공
- `RunningMode.VIDEO` 트래킹, lite/full/heavy 모델 및 CPU/GPU delegate 선택 가능
- `ImageProcessingOptions.setRotationDegrees` 등 회전 처리 API 보유(AAR javap로 확인)

## Alternatives
- ML Kit Pose Detection(base/accurate) — 같은 BlazePose 계열이라 역자세 우위 근거 없음, 모델 2종뿐, delegate 제어 적음
- TFLite MoveNet / RTMPose 직접 — 17 keypoint(발끝·뒤꿈치·귀 일부 없음), 전후처리·트래킹 직접 구현 필요

## Why chosen
역자세 대응 수단(모델 종류, delegate, 회전 옵션)이 가장 많고, API 시그니처를 AAR에서 직접 확인해 불확실성이 낮다.

## Consequences
- 네이티브 라이브러리로 APK 용량이 증가한다.
- 모델 파일(~9MB)을 저장소에 커밋하거나 별도 다운로드 스크립트가 필요하다(본 프로젝트는 커밋 선택, ADR-0005 참조).
- minSdk 28 확정에 영향(B1' 폴백 대비).

## Follow-ups
- P1.5 게이트(검출률 ≥90%, 평균 visibility ≥0.5, 방향 선택 정확, front 부호 정확도 100%) 실패 시 heavy 모델·GPU delegate 또는 RTMPose/TFLite로 재평가한다.
- P1.5 실측(2026-09-27, SM-F766N, `docs/VERIFICATION.md`): 검출률 100%(원본 131/131) / 99.2%(rot180 사본), 평균 visibility 0.978 / 0.888, frontSign +1 둘 다 정답. 방향은 두 영상 모두 0° 선택(score 원본 0.743 vs 180° 0.083, 사본 0.592 vs 0.000) — `setRotationDegrees(180)`은 검출을 거의 무력화(홀드 94프레임 중 1), 비트맵 Matrix 회전 시 score 0.709 / 0.537. 방향 기준은 ADR-0006으로 재정의되어 게이트 PASS, 본 ADR 확정. 성능은 13s 영상 41–47s(디코드 171–190 ms/프레임, 추론 104–121 ms/프레임)로 NFR-2 초과 추세.
- P4 비교(2026-09-27, SM-F766N, `ModelDelegateBenchmarkTest`, 표는 `docs/VERIFICATION.md` "모델·가속 비교"): 30 s 영상 총 분석 full-CPU 39.9 s(연속 3회 42.0/42.1/43.0 s), full-GPU 35.9 s(40.6/40.7/41.5 s, 추론 105→54 ms, Δθ ≤ 0.99°), heavy-CPU 84.7 s, heavy-GPU 41.1 s. heavy는 visibility 0.66–0.69로 떨어지고 tune 홀드를 잃거나 줄임(팔꿈치 Δθ 5.6°) → 기각. GPU는 디코드 병목으로 총 이득 3–10%뿐이라 **full-CPU 유지**. `MediaPipePoseDetector`에 모델 경로·delegate 인자와 GPU 생성 실패 시 CPU 폴백을 두어 재평가 가능.
- 디코드(샘플당 ~120 ms)를 줄이면 추론이 다시 병목이 되므로 그때 GPU delegate(추론 절반)를 재검토한다.
