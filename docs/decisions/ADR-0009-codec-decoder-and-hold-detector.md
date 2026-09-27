# ADR-0009: 프레임 샘플링을 MediaCodec 순차 디코더로, 홀드 검출을 2단계(시드 + 중앙값 자세 거리)로 재설계

## Status
Accepted

## Date
2026-09-27

## Context
**(a) 프레임 샘플링.** 계획(§ 프레임 추출)은 기본 경로 B1 `MediaMetadataRetriever.getFrameAtTime(k·100ms, OPTION_CLOSEST)`(`RetrieverFrameSource`)을 쓰고, NFR-2(30 s 영상 ≤ 60 s) 초과 시 문서화된 폴백 B1' `getFramesAtIndex`로 바꾸도록 했다(NFR-4의 minSdk 28 근거도 이 폴백). P1.5 실측(SM-F766N, `docs/VERIFICATION.md`)에서 B1은 디코드가 전체의 60% 이상이었다: 디코드 171–190 ms/프레임, 13 s 영상 41.1–47.4 s → 30 s 환산 95–110 s로 NFR 초과. `OPTION_CLOSEST`는 샘플마다 직전 키프레임부터 다시 디코드하고 전체 해상도로 RGB 변환하므로 GOP 길이에 비례해 느려진다(GOP 8.3 s인 rot180 사본 0.5–0.9 s/프레임).

**(b) 홀드 구간 검출.** `HoldSegmentDetector`는 15샘플 창의 손목·발목 범위가 모두 < 2%인 창이 덮는 최장 구간을 홀드로 잡았다. 디코더 교체 후 tune 홀드가 28..121 → 42..121로 바뀌어 조사한 결과, 이전 경로 프레임에 ±1 LSB 무작위 노이즈만 더해도 28..130 / 41..130으로 흔들렸다(landmark 평균 차 0.0022, 새 디코더와의 차 0.0023과 같은 수준). 창 범위가 홀드 중에도 2% 근처(1.9–2.2%)를 오가 잡음에 판정이 뒤집히는 것이 원인이다 — 디코더 문제가 아니라 검출기의 경계 민감도 문제.

## Decision
**(a) `CodecFrameSource`로 교체(커밋 `7ba3185`).** 계획의 B1' `getFramesAtIndex`를 쓰지 않고 `MediaExtractor` + `MediaCodec`(YUV ByteBuffer 출력)으로 한 번 순차 디코드하며 **샘플 프레임만** CPU에서 RGB로 변환한다(BT.709 limited range, chroma 최근접 복제) → 회전 메타 적용 → 긴 변 640 px bilinear 다운스케일. `PoseLandmarkerEngine`은 디코드를 IO 스레드에서 돌리고 버퍼 2장으로 추론과 겹친다. 샘플 k는 pts ≥ k·100 ms − 반 프레임인 첫 프레임, `displayTimeMs`는 요청 시각을 유지한다. `RetrieverFrameSource`는 삭제.

**(b) `HoldSegmentDetector` 2단계 재설계(커밋 `b6872fb`).** 기존 창 판정(15샘플, 범위 < 2%)의 최장 구간은 **시드**로만 쓴다. 시드에서 손목·발목이 모두 보이는 쪽을 고르고 그 관절들의 **중앙값 자세**를 구한 뒤, 손목·발목이 중앙값 자세에서 x·y 모두 **3%**(`HOLD_TOLERANCE`, 프레임 높이 기준 Chebyshev 거리) 이내인 프레임의 최장 연속 구간을 홀드로 반환한다. 가시성 미달·미검출 프레임은 여전히 구간을 끊는다.

## Drivers
- NFR-2(30 s ≤ 60 s) 충족이 필수, 그리고 여유 확보(발열 시 추론이 102 → 147 ms로 느려짐).
- 인식 품질 유지 — 디코더가 바뀌어도 landmark visibility·방향 판정·각도가 이전 경로와 같아야 한다(파이프라인 하류는 그대로 두기 위해).
- 홀드 구간은 AC-4 게이트와 결과 화면 "홀드 안 결함" 목록의 기준이므로, 잡음 수준 입력 변화에 경계가 흔들리면 안 된다.
- 가장 작은 변경 — `FrameSource` 인터페이스와 검출기 시그니처는 유지.

## Alternatives
(a) 프레임 샘플링 — 모두 실측(`docs/VERIFICATION.md` "성능 개선"):
- **B1' `getFramesAtIndex` 배치(ARGB)** — 계획의 폴백. 픽셀은 동일하지만 30 fps 영상에서 샘플 사이 프레임까지 전부 RGB 변환해 30 s 영상 **65.8–67.0 s**로 NFR 미달. 기각.
- **같은 방식 RGB_565** — 2배 빠르지만 visibility **0.895**로 저하. 기각.
- **SurfaceTexture/GL 변환** — 디코드 35–59 ms로 가장 빠르지만 경계부 |ΔRGB| 2.5로 visibility **0.886**(±1 LSB 노이즈로는 0.978 유지 → 우연 오차가 아닌 체계적 차이). 기각.

(b) 홀드 검출:
- **창 판정 임계값 조정(2% → 2.5% 등)** — 경계가 다른 값 근처로 옮겨 갈 뿐 "범위가 임계값 근처를 오가는" 구조는 그대로. 기각.
- **창 판정 결과에 히스테리시스/형태학 연산** — 파라미터가 늘고 시드 자체의 흔들림(Python 재현에서 시작 26–51, 끝 120–130)을 없애지 못한다. 기각.

## Why chosen
(a) `CodecFrameSource`는 필요한 샘플만 변환해 `getFramesAtIndex`의 전 프레임 변환 비용을 피하고, CPU BT.709 변환으로 `getFrameAtTime`과 픽셀 수준 동등성(|ΔRGB| 0.03)을 지켜 하류 결과를 바꾸지 않는다. 실측: tune 13 s **41.1–47.4 s → 18.1 s**, rot180 사본 85.6–149.9 s → 31.6 s, 30 s 영상 **55.5 s**(당시 발열 상태; P4 재측정 full-CPU 연속 3회 42.0–43.0 s). 동등성: tune 검출 100%, 평균 visibility 0.9777(이전 0.9779), frontSign +1·side LEFT 동일, 방향 score 0.742/0.710(이전 0.743/0.709), meta90 회전 일치, 180° 역변환 |P − flip(L)| 0.0028.

(b) 중앙값 자세와의 거리는 진입·종료에서 여유가 크다(샘플 13→14에서 5.3% → 2.1%) — 임계값 근처를 오가지 않는다. tune fixture 결과 **14..130**(117 샘플), 가우시안 σ 0.002 잡음 사본 24개(seed 1–24)에서 시작·끝 변동 **0 샘플**(`HoldSegmentDetectorTest.tuneFixtureBoundariesSurviveLandmarkNoise`). 같은 조건에서 시드 구간은 시작 26–51, 끝 120–130으로 흔들린다.

## Consequences
- NFR-2 PASS(`docs/VERIFICATION.md` "NFR"). process 구간은 이제 디코드(샘플당 ~120 ms, IO 스레드)와 추론이 비슷한 수준이라 GPU delegate 이득이 작다(미채택, VERIFICATION "모델·가속 비교").
- **제약**: 8-bit 4:2:0 전제(10-bit/HDR 미지원), CFR 가정 — VFR 영상은 샘플 시각이 어긋날 수 있다(holdout3는 avg 30.07 fps로 약간 VFR). 코덱별 YUV 레이아웃(stride·slice height·color format) 처리 코드가 앱에 들어온다.
- 계획의 B1' 폴백은 쓰지 않는다. minSdk 28(NFR-4)의 근거 중 "`getFramesAtIndex`를 분기 없이 쓰기" 항목은 더 이상 해당하지 않지만, minSdk는 변경하지 않는다.
- 홀드 구간 정의가 바뀌어 tune 홀드가 28..121(94) → 14..130(117)으로 넓어졌다. 결과 화면의 홀드 안 결함 목록과 AC-4 게이트가 이 구간을 쓴다.
- 홀드가 영상 끝까지 이어지면(tune·holdout3 모두) 홀드 길이는 녹화 종료 시점에 좌우된다 — 검출기 문제가 아니라 촬영 가이드("홀드가 끝나면 녹화 종료")로 다룬다.
- 단일 기기(SM-F766N, Android 16) 실측. 디코더 성능·색 변환 동등성은 기기 의존.

## Follow-ups
- 10-bit/HDR 또는 VFR 영상이 들어오면 입력 거부 또는 pts 기반 샘플링을 검토한다.
- 다른 기기(특히 다른 SoC 디코더)에서 |ΔRGB|·visibility 동등성과 NFR-2를 재측정한다.
- 홀드가 여러 번(내려왔다 다시 올라감) 있는 영상에서 최장 구간만 고르는 동작이 적절한지 확인한다.
