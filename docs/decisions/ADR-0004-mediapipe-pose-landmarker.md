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
- P4에서 full vs heavy × CPU vs GPU 비교표를 작성한다.
