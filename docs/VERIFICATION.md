# VERIFICATION — P1.5 De-risk Spike (역자세 인식 실측)

- 일자: 2026-09-27
- 기기: Samsung SM-F766N, Android 16 (SDK 36), adb serial R3KL202BBMJ
- 코드: `develop`, 테스트 `app/src/androidTest/java/com/calisvision/PoseSpikeInstrumentedTest.kt`
  (프로덕션 `RetrieverFrameSource`(→ 순차 디코딩 후 `CodecFrameSource`, 아래 "성능 개선") · `OrientationResolver` · `MediaPipePoseDetector` · `PoseLandmarkerEngine` · `AnalysisResult.assemble` 그대로 사용)
- 실행: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.calisvision.PoseSpikeInstrumentedTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`
  (마지막 플래그는 AGP의 실행 후 앱 제거로 `getExternalFilesDir` 결과가 지워지는 것을 막기 위함)
- 모델: `pose_landmarker_full.task`, MediaPipe tasks-vision 1.0.0, CPU, 신뢰도 0.5, 샘플 10fps, 긴 변 640px

## 입력

| 영상 | 내용 | 비고 |
|---|---|---|
| `wall_handstand_tune.mp4` | 13.0s, HEVC 1080×1192, 30fps, 회전 메타 없음. 측면, 가슴이 벽(옷장, 화면 오른쪽)을 향한 벽 물구나무, 손 아래·발 위 | GOP 1s |
| `wall_handstand_tune_rot180.mp4` | 위 영상 `hflip,vflip` → libx264 crf18 | GOP 250프레임(8.3s) |
| `wall_handstand_tune_meta90.mp4` | `ffmpeg -display_rotation 90 -c copy` (ffprobe: displaymatrix rotation 90) | (a) 전용 |
| `wall_handstand_holdout.mp4` | 17.7s | **P4 전용** — fixture만 기기에서 추출, 각도 미계산·미열람 |

수동 라벨: 몸 앞면(가슴)은 원본에서 이미지 +x → 기대 frontSign = +1 (shoulder→ankle = (0,−1), front = (1,0), cross = +1). 180° 회전은 모든 차 벡터를 부호 반전하고 cross(−u,−v) = cross(u,v)이므로 rot180 사본도 +1. 기대 방향: 원본 180°, rot180 사본 0°.

## 측정 결과

| 항목 | 원본(tune) | rot180 사본 | 기준 |
|---|---|---|---|
| ① 검출률(전체 131 샘플) | **100%** | 99.2% | ≥ 90% |
| ① 검출률(홀드 구간) | 100% (94 샘플, 28..121) | 100% (23 샘플, 46..68) | — |
| ② 4규칙 관절 평균 visibility (선택 측, 검출 프레임) | **0.978** (홀드 0.977) | 0.888 (홀드 0.912) | ≥ 0.5 |
| ③ OrientationResolver score 0° / 180° | 0.743 / 0.083 | 0.592 / 0.000 | — |
| ③ 선택 회전 (기대) | **0° (기대 180°) ✗** | 0° (기대 0°) ✓ | 둘 다 정확 |
| ③ 참고: 180°를 비트맵 수동 회전(Matrix)으로 했을 때 score (v2부터 프로덕션 경로) | 0.709 | 0.537 | — |
| ④ SideSelector side / frontSign | LEFT / **+1 ✓** | LEFT / +1 ✓ (side 원본과 동일) | +1 |
| ⑤ 디코드 ms/프레임 (getFrameAtTime OPTION_CLOSEST + 다운스케일) | 171–190 | 478–904 | — |
| ⑤ 추론 ms/프레임 (VIDEO 모드) | 104–121 | 107–136 | — |
| ⑤ 총 분석 시간 (resolve + process) | 41.1–47.4 s (13.0s 영상) | 85.6–149.9 s | 30s 영상 ≤ 60s |

⑤는 동일 테스트 3회 실행 범위(기기 발열에 따라 변동). 관절별 visibility: 원본 WRIST 0.96, ELBOW 1.00, SHOULDER 1.00, HIP 1.00, KNEE 0.96, ANKLE 0.95.

## 게이트 판정

| 조건 | 결과 |
|---|---|
| 검출 ≥ 90% | PASS (100%) |
| 평균 visibility ≥ 0.5 | PASS (0.978) |
| 방향 선택 정확 (원본·rot180 모두) — **ADR-0006 재정의**: 선택 회전이 원본 좌표계에서 올바른 landmark를 준다 | **PASS** — 원본 0° 선택(검출 100%, visibility 0.978, 역변환 불필요), rot180 사본 0° 선택, frontSign 둘 다 정답 |
| front 부호 정확 (+1) | PASS (원본·rot180 모두 +1) |

**판정: PASS (ADR-0006 기준, 2026-09-27 사용자 결정).** 계획서 원래 기준(원본 기대 180°)을 문자 그대로 적용하면 방향 항목 1건으로 FAIL이었다 — 원인은 인식 품질이 아니라 기대 라벨의 전제와 `setRotationDegrees(180)` 동작이며, 이 기록은 아래에 그대로 남긴다.

- MediaPipe는 뒤집힌(손 아래) 사람을 **0°에서 그대로** 잘 검출한다(검출 100%, visibility 0.98). 비트맵을 직접 180° 돌려 똑바로 세운 경우(0.709)보다 오히려 점수가 높다.
- `ImageProcessingOptions.setRotationDegrees(180)`은 이 기기/버전에서 사실상 검출을 무력화한다: 원본 홀드 94프레임 중 1프레임만 검출, rot180 사본에서도 방향 판정 10프레임 score 0.000(검출 없음). 반면 같은 프레임을 Matrix로 180° 돌려 0°로 넣으면 5/5 검출, rot180 사본 결과와 평균 좌표 차 0.003.
- 따라서 선택된 0°는 결과적으로 "올바른 좌표(원본 프레임, 역변환 불필요)"를 준다. frontSign·side도 정확. 방향 라벨(원본 = 180°)은 "검출에는 직립이 필요하다"는 가정에서 나온 것이며 실측으로 반증됐다.
- 후속(ADR-0006 Follow-ups): 180° 경로를 `setRotationDegrees` 대신 비트맵 Matrix 회전 + `(1−x,1−y)` 역변환으로 교체.

## 미확인 사항 결론

**(a) `getFrameAtTime`의 회전 메타데이터 자동 적용 — 적용된다.** meta90 사본: `METADATA_KEY_VIDEO_ROTATION = 270`, 메타 크기 1080×1192, 디코드 비트맵 **1192×1080**(원본은 1080×1192), `RetrieverFrameSource` 출력 640×580. 수동 회전 불필요. 단, `VideoInfo.width/height`는 회전 전 값이므로 종횡비는 반드시 디코드된 비트맵 크기에서 가져와야 한다(현재 `PoseLandmarkerEngine`은 이미 그렇게 함). ffmpeg display_rotation 90은 Android에서 270으로 보고됨(방향 규약 차이).

**(b) `setRotationDegrees(180)` 시 반환 좌표계 — 판정 불가(초회).** 180° 입력에서 검출이 거의 되지 않아 비교 가능 프레임이 1개뿐이었고, 그 1개도 L 대비 차 0.246, (1−x,1−y) 대비 0.231로 둘 다 크다(오검출).
- **후속 조치(ADR-0006, 재측정 v2):** `MediaPipePoseDetector`의 180° 경로를 비트맵 `Matrix.postRotate(180)` → 0° 검출 → `(1−x,1−y)` 역변환으로 교체하고 `ROTATED_COORDS_ARE_IN_ROTATED_FRAME` 플래그를 제거했다. (b) 항목은 이제 P = detectImage(원본ᵢ, 180)(원본 좌표)와 flip(L), L = detectImage(rot180 사본ᵢ, 0)를 비교한다: 홀드 10프레임 10/10 검출, |P − flip(L)| = **0.0028**(허용 0.02, PASS), 음성 대조 |P − L| = 0.388, 같은 프레임 0° 결과와의 차 0.034.
- v2 OrientationResolver score 0°/180°: 원본 0.743 / **0.709**, rot180 사본 0.592 / **0.537** → 선택은 둘 다 0°(변화 없음). 검출률·visibility·frontSign 값은 초회와 동일.

## 튜닝 영상 홀드 구간 signed θ (참고, 게이트 아님)

홀드 28..121 (94 샘플), 이동 중앙값(5) 적용 값.

| 규칙 | 평균 | 최소 | 최대 | 비고 |
|---|---|---|---|---|
| 전신 정렬 (손목-엉덩이-발목) | 176.6 | 169.8 | 186.3 | [175,185] 비율 **41.5%** |
| 골반/허리 (어깨-엉덩이-무릎) | 182.2 | 173.0 | 192.8 | |
| 어깨 열림 (팔꿈치-어깨-엉덩이) | 156.7 | 148.9 | 162.8 | Range 165–195 기준 전 구간 BELOW(어깨 닫힘) |
| 팔꿈치 펴짐 (손목-팔꿈치-어깨) | 197.6 | 190.9 | 205.3 | Range 170–190 초과(ABOVE, 무결함) |

해석 메모: 정렬 평균 −3.4°(약한 파이크 쪽), 어깨 −23°, 팔꿈치 +18°. 튜닝 영상이 "정렬된 홀드" 기준이라면 어깨·팔꿈치 부호 의미나 Range 기본값이 실제와 맞지 않을 가능성이 크다 — P2/P4 보정 대상. AC-4 게이트(hold-out)는 이 단계에서 평가하지 않았다.

### 육안 확인 (2026-09-27, 홀드 샘플 75 = 7.5s)

영상 프레임 위에 fixture landmark를 찍어 확인했다. 손목 → 팔꿈치 → 코 → 어깨 → 엉덩이 → 무릎 → 발목이 모두 신체 위 올바른 위치에 있고, 코가 어깨보다 벽 쪽(+x)이라 chest-to-wall·front +1 라벨과 일치한다.

- **어깨 156.7°는 실제 자세다.** 손이 벽에서 떨어져 있고 발만 벽에 기대 몸 전체가 벽 쪽으로 약 19° 기울었다(어깨 (0.554,0.794) → 엉덩이 (0.626,0.601), 종횡비 보정 후 18.7°). 팔은 바닥에 수직이므로 팔–몸통 각이 실제로 약 20° 닫혀 있다. "어깨 닫힘" 판정은 오탐이 아니다.
- **팔꿈치 197.6°는 과신전 방향이다.** 팔꿈치가 손목–어깨 선보다 앞쪽(벽 쪽)에 있어 P2 가정(굽힘 −, 과신전 +)과 부호가 맞는다. ABOVE는 결함 미매핑이라 오탐 없음. 팔 구간이 짧아(약 100px) landmark 몇 px 오차가 수 도로 증폭되므로 18°라는 크기는 과대 추정일 수 있다.
- **결론: 어깨·팔꿈치 Range 기본값을 바꿀 근거가 없다.** 이 튜닝 영상은 어깨가 닫힌 비스듬한 벽 물구나무라, 전신 정렬(AC-4) 검증에는 쓸 수 있지만 "정상 자세에서 어깨 규칙이 조용한지"는 검증하지 못한다 → 손을 벽 10cm 안쪽에 두고 수직에 가깝게 선 영상이 있으면 추가 확인.

## 주의 사항

- **카메라 각도**: 카메라가 바닥 근처에서 약간 올려다보는 구도(가이드의 "엉덩이 높이 삼각대"와 다름). 원근 왜곡으로 어깨·팔꿈치 각도가 실제와 다르게 측정될 수 있어 위 θ 통계를 기준값으로 쓰기 전에 가이드 구도 영상으로 재확인 필요.
- rot180 사본은 원본보다 visibility가 낮고(0.89 vs 0.98) 홀드 구간이 짧게 검출됨(23 vs 94 샘플). 재인코딩 차이 또는 직립 방향 지터로 추정 — 방향 비의존성이 완전하지 않다는 신호.
- **성능(⑤)**: 원본 13s 영상 41–47s → 30s 영상 환산 약 95–110s로 NFR(≤ 60s) **초과 추세**. 디코드가 60% 이상. `OPTION_CLOSEST`는 GOP 길이에 비례(GOP 8.3s인 rot180 사본은 프레임당 0.5–0.9s). → 아래 "성능 개선"에서 해결(30s 영상 55.5s).

## 성능 개선 — 순차 디코딩 (2026-09-27)

`RetrieverFrameSource`(샘플마다 `getFrameAtTime(OPTION_CLOSEST)` = 직전 키프레임부터 재디코드 + 전체 해상도 RGB 변환)를 `CodecFrameSource`로 교체: MediaExtractor + MediaCodec(YUV ByteBuffer 출력)로 순차 디코드하고 **샘플 프레임만** RGB 변환(BT.709 limited, chroma 최근접 복제 — 이 기기의 `getFrameAtTime`과 전체 해상도 평균 |ΔRGB| 0.03) → 회전 메타 적용 → 640px bilinear 다운스케일. `PoseLandmarkerEngine`은 디코드를 IO 스레드에서 돌리고 버퍼 2장으로 추론과 겹친다. 30s 영상은 튜닝 영상을 `ffmpeg -stream_loop 2 -t 30 -c copy`로 이어 붙인 `wall_handstand_tune_30s.mp4`(30.0s, 900프레임).

| 영상 | 디코드 ms/샘플 (IO, 추론과 병렬) | 추론 ms/프레임 | resolve / process | 총 분석 | 이전 |
|---|---|---|---|---|---|
| tune 13.0s (131 샘플) | 99.6 | 102.5 | 3.5s / 14.6s | **18.1 s** | 41.1–47.4 s |
| rot180 사본 13.0s (GOP 8.3s) | 130.8 | 158.6 | 9.5s / 22.1s | **31.6 s** | 85.6–149.9 s |
| tune_30s 30.0s (301 샘플) | 120.0 | 147.1 | 9.0s / 46.5s | **55.5 s** ≤ 60s ✓ | 환산 95–110 s |

- 1회 실행 값(연속 실행 마지막이라 추론이 발열로 102 → 147ms로 느려짐). NFR-2는 **충족, 여유 4.5s** — process 구간은 이제 추론 병목(샘플당 155ms ≈ 추론 147ms)이라 추가 단축은 GPU delegate 쪽.
- 동등성(이전 경로 대비): tune 검출 100%(동일), 평균 visibility **0.9777**(이전 0.9779), frontSign +1·side LEFT 동일, 방향 score 0.742/0.710(이전 0.743/0.709); rot180 검출 100%(이전 99.2%), visibility 0.879(이전 0.888); (a) meta90 회전 방향·크기 일치(|ΔRGB| 0.03); (b) |P − flip(L)| 0.0028.
- 홀드 구간은 tune 42..121(이전 28..121). 이전 프레임에 ±1 LSB 무작위 노이즈만 더해도 28..130 / 41..130으로 흔들리고(landmark 평균 차 0.0022, 새 경로 0.0023과 같은 수준), 즉 **`HoldSegmentDetector` 경계가 잡음 수준 입력 변화에 민감**하다 — 디코더 문제가 아니라 P2/P4 확인 사항.
- 기각한 대안(실측): `getFramesAtIndex` 배치(ARGB, 픽셀 동일) — 30 fps 영상에서 샘플 사이 프레임까지 모두 RGB 변환해 30s 65.8–67.0s(NFR 미달). 같은 방식 RGB_565 — 2배 빠르지만 visibility 0.895. SurfaceTexture/GL 변환 — 디코드 35–59ms로 가장 빠르지만 경계부 |ΔRGB| 2.5로 visibility 0.886(노이즈 ±1 LSB는 0.978 유지 → 우연 오차가 아니라 체계적 차이).
- 제약: 8-bit 4:2:0 전제(10-bit/HDR 미지원), 샘플 k는 pts ≥ k·100ms − 반 프레임인 첫 프레임(CFR 가정, VFR은 어긋날 수 있음, `displayTimeMs`는 요청 시각 유지).

## 다음 단계

1. ~~방향 게이트 재정의 여부 결정(ADR)~~ — ADR-0006으로 결정(재정의 후 PASS). 180° 경로는 Matrix 회전으로 교체.
2. ~~NFR 초과 → 순차 디코드 시험~~ — `CodecFrameSource`로 30s 55.5s(아래). 여유가 4.5s뿐이라 발열 시 초과 가능 → 추론 GPU delegate 시험(P4).
3. ~~어깨 열림·팔꿈치 Range 부호/기본값 보정~~ — 육안 확인 결과 부호 의미가 맞고 측정값은 실제 자세(어깨 닫힘·팔꿈치 과신전)라 기본값 유지. 수직 벽 물구나무(손–벽 10cm 이내) 영상으로 정상 자세 무결함 확인은 선택 사항.
6. ~~`HoldSegmentDetector` 경계가 ±1 LSB 수준 입력 변화에도 크게 흔들림(28..121 ↔ 41..130)~~ — 2단계로 교체: 기존 창 판정(15샘플, 범위 < 2%)의 최장 구간은 시드로만 쓰고, 시드의 손목·발목 중앙값 자세에서 x·y 모두 3% 이내인 프레임의 최장 연속 구간을 홀드로 반환(가시성 미달·미검출 프레임은 여전히 끊음). 창 범위는 홀드 중에도 2% 근처(1.9–2.2%)를 오가 잡음에 뒤집히지만, 중앙값 자세와의 거리는 진입(샘플 13→14에서 5.3% → 2.1%)·종료에서 여유가 크다. tune fixture 결과 **14..130**(117 샘플), 가우시안 σ 0.002 잡음 사본 24개(seed 1–24)에서 시작·끝 변동 **0 샘플**(시드 구간 자체는 같은 조건의 Python 재현에서 시작 26–51, 끝 120–130으로 흔들림) — `HoldSegmentDetectorTest.tuneFixtureBoundariesSurviveLandmarkNoise`.
4. 가이드 구도(엉덩이 높이 카메라) 영상 추가 확보.
5. P4: hold-out fixture를 테스트 리소스로 옮겨 `WallHandstandAlignmentTest`(AC-4) 실행.

---

# P4 — 검증·보정

- 일자: 2026-09-27, 기기 SM-F766N(Android 16), `develop`, 모델 `pose_landmarker_full.task` CPU
- 파라미터 동결 상태에서 실행(임계값·스무딩(중앙값 5)·`HoldSegmentDetector`·규칙 변경 없음). hold-out은 **1회만** 평가.

## AC-4 — hold-out 판정: **FAIL**

경로: `WallHandstandInstrumentedTest`(`@RequiresVideo`, 프로덕션 `DefaultVideoAnalyzer` = CodecFrameSource → OrientationResolver → PoseLandmarkerEngine → `AnalysisResult.assemble`)가 fixture를 `getExternalFilesDir("fixtures")`에 export → `adb pull` → `./gradlew testDebugUnitTest --tests "*WallHandstandAlignmentTest"`(실행됨, skip 아님).

```
hold segment 130..176: 47 samples
AlignmentGateResult(passed=false, ratio=0.19148937, holdSamples=47, reason=hold 47 < 50 samples)
```

| 항목 | hold-out (AC-4) | tune (참고) |
|---|---|---|
| 샘플 / 검출 | 177 / 177 | 131 / 131 |
| 선택 회전 · side · frontSign | 0° · LEFT · +1 | 0° · LEFT · +1 |
| 홀드 구간 (길이) | **130..176 (47) < 50 → FAIL** | 14..130 (117) |
| 스무딩 정렬 θ ∈ [175,185] (null 0) | **19.1%** < 95% | 43.6% < 95% |
| 정렬 θ 평균 / 최소 / 최대 | 190.9 / 181.2 / 197.5 | 177.7 / 169.8 / 188.6 |
| 골반/허리 평균 (최소–최대) | 189.3 (181.4–194.1) | 182.3 (173.0–192.8) |
| 어깨 열림 평균 (최소–최대) | 156.9 (150.3–160.4) | 157.4 (148.9–162.8) |
| 팔꿈치 평균 (최소–최대) | 267.5 (255.1–280.7) | 197.0 (190.9–205.3) |
| 분석 시간 (프레임 JPEG 저장 포함) | 29.9 s (17.7 s 영상) | 22.9 s (13.0 s 영상) |

홀드 구간 안 결함(`ResultViewModel`과 같은 규칙: `isInHold` ≥ 3샘플 겹침 후 `FaultEvaluator.clip`):

| 영상 | 결함 (clip된 범위, 최대 편차) |
|---|---|
| hold-out | 어깨 닫힘 `closed_shoulder` 130..176 (−29.7°), 바나나 등 `banana_back` 141..148 (+12.8°), 156..176 (+17.5°) |
| tune | 어깨 닫힘 `closed_shoulder` 14..130 (−31.1°), 파이크 `pike` 89..91 (−10.2°) |

- 홀드 구간 밖(목록 미표시): hold-out `closed_shoulder` 0..13, `pike` 0..2, `banana_back` 99..108; tune `pike` 0..7.
- hold-out 홀드 구간은 영상 마지막 샘플(176 = 17.6 s)에서 끝난다 — 13.0 s 이후만 홀드로 잡혔다. hold-out 팔꿈치 267°는 물리적으로 불가능한 값(landmark 오류 추정)이지만 ABOVE는 결함 미매핑이라 결함 목록에는 영향 없음. hold-out은 동결 규칙에 따라 더 들여다보지 않았다.
- fixture: `wall_handstand_holdout.json`은 **커밋하지 않았다**(커밋하면 `testDebugUnitTest`가 AC-4로 red). 사용자 결정 대기 — 아래 "진단" 참고.
- tune fixture 재export는 기존 fixture와 landmark 평균 |Δ| 0.0023(최대 0.051), 검출 차이 0, 홀드 14..130 동일 → 실행 간 잡음 수준이라 갱신하지 않음.

### 진단 (tune 영상만 사용)

- **tune도 같은 게이트에서 실패한다(43.6% < 95%)** — hold-out 고유 문제가 아니다. 현재 파라미터로 [175,185] 95%를 통과할 영상이 없다.
- tune 홀드 동안 스무딩 정렬 θ는 181 → 188.6(진입 직후 ~1 s, 바나나 쪽) → 183–185 유지(~3.5 s) → 170 부근(−10°, 파이크 경계)으로 ~4 s → 176.8로 끝난다. 즉 실제 자세가 홀드 중 ±9° 움직이고, 게이트 폭 ±5°를 95% 유지하지 못한다. 스무딩 전(raw) 비율도 43.6%로 같아 jitter가 원인이 아니다.
- P1.5 육안 확인과 일치: tune은 손이 벽에서 떨어져 몸이 벽 쪽으로 ~19° 기운 벽 물구나무(어깨 닫힘 −23°)이고 카메라가 바닥 근처에서 올려다보는 구도다(가이드 "엉덩이 높이"와 다름) — 2D 원근으로 정렬 각이 왜곡될 수 있다.
- tune 홀드도 영상 마지막 샘플(130)까지 이어진다 — 두 영상 모두 홀드 중에 녹화가 끝나, 홀드 길이는 "홀드 시작 후 녹화가 얼마나 이어졌나"에 좌우된다.
- 선택지(사용자 결정): (1) 가이드 구도(엉덩이 높이, 몸과 수직, 홀드 7 s 이상, 홀드가 끝난 뒤 녹화 종료)로 새 hold-out 재촬영, (2) AC-4 게이트(폭 [175,185]/95%) 재정의 ADR, (3) 현 결과를 FAIL로 확정하고 fixture 커밋(테스트 red 유지). 파라미터를 재보정한다면 새 hold-out이 필요하다(이 hold-out은 이미 평가·열람됨).
- **hold-out 육안 확인(2026-09-27, 게이트 판정 이후, 파라미터 조정 없음)**: 샘플 60·150 프레임에 landmark를 찍어 보니 **양손·손목이 화면 앞쪽 쿠션 뒤에 가려져 있다**. 전신 정렬의 기준점인 손목을 MediaPipe가 추정으로 찍으면서도 visibility를 0.84–0.94로 높게 내 가림이 걸러지지 않았고, 팔꿈치 267° 같은 불가능한 값과 홀드 끊김(13.0 s 이후만 홀드)도 여기서 나온 것으로 본다. 판정 결과(FAIL)는 그대로 두고, 입력 영상이 가이드 조건(전신이 가림 없이 보임)을 만족하지 못한 것으로 기록한다.
- **결정(사용자, 2026-09-27): hold-out 재촬영.** 조건: 손·발 가림 없음, 카메라 엉덩이 높이, 손을 벽 10cm 안쪽, 홀드 7 s 이상, 홀드가 끝나면 녹화 종료. 새 영상으로 동결된 파라미터 그대로 한 번만 재판정한다. 촬영 가이드에 "손·발이 가려지지 않게"와 "7초 이상 홀드, 끝나면 바로 촬영 종료" 항목을 추가했다.
- 주의: tune(손 보임)도 43.6%라 실제 홀드 중 자세 흔들림(±9°)만으로도 ±5°/95% 게이트는 빡빡하다. 재촬영 결과도 FAIL이면 그때 게이트 재정의(ADR)를 논의한다.
- 후속 과제: 가려진 관절을 visibility만으로 거르지 못하므로, 물리적으로 불가능한 각도(예: 팔꿈치 > 230°)나 프레임 간 급변을 "측정 불가"로 처리하는 방어 규칙을 검토한다.

## hold-out 재촬영 (holdout3) AC-4 재판정: **FAIL**

- 입력: `wall_handstand_holdout3.mp4` — 13.9 s, HEVC 1080×1192, 회전 메타 없음, avg fps 30.07(2145280/71339, 약간 VFR; `CodecFrameSource`는 CFR 가정). 첫 hold-out(쿠션에 손 가림) FAIL 이후 사용자 재촬영, `holdout2`는 발목이 프레임 밖이라 평가 전 기각.
- 촬영 조건: 손·발 가림 없음, 몸 거의 수직, 손이 벽 가까이. **주의 사항 두 가지**: (1) 카메라가 다시 바닥 근처(가이드의 엉덩이 높이 아님) — 올려다보는 원근, (2) 발이 화면 위쪽 가장자리에 닿음(발끝 y 최소 0.022, 발목 y 최소 0.037).
- 파라미터 동결(임계값·스무딩·`HoldSegmentDetector`·규칙·모델 full·CPU 변경 없음), **1회만** 평가. 경로는 위와 같다 — `WallHandstandInstrumentedTest#exportHoldoutFixture`가 입력만 `wall_handstand_holdout3.mp4`로 바꿔 `wall_handstand_holdout.json`으로 export → `adb pull` → `WallHandstandAlignmentTest`(실행됨, skip 아님).

```
hold segment 24..139: 116 samples
AlignmentGateResult(passed=false, ratio=0.61206895, holdSamples=116, reason=aligned ratio 0.61206895 < 0.95)
```

| 항목 | holdout3 |
|---|---|
| 샘플 / 검출 | 140 / 140 (100%) |
| 선택 회전 · side · frontSign | 0° · LEFT · +1 |
| 6규칙 관절 평균 visibility (LEFT, 전체 / 홀드) | 0.985 / 0.987 — WRIST 0.978, ELBOW 0.996, SHOULDER 0.999, HIP 0.999, KNEE 0.986, ANKLE 0.963 (홀드) |
| 발목 visibility (LEFT, 홀드 평균 / 최소) | 0.963 / 0.943 (반대쪽 RIGHT 0.534 — 가려진 쪽) |
| 홀드 구간 (길이) | 24..139 (116) ≥ 50 ✓ — 영상 마지막 샘플(139 = 13.9 s)에서 끝남 |
| 스무딩 정렬 θ ∈ [175,185] (null 0) | **61.2%** < 95% → FAIL (raw도 61.2%) |
| 정렬 θ 평균 / 최소 / 최대 | 184.9 / 181.9 / 189.2 |
| 골반/허리 평균 (최소–최대) | 187.0 (185.1–189.6) |
| 어깨 열림 평균 (최소–최대) | 158.2 (153.4–168.1) |
| 팔꿈치 평균 (최소–최대) | 198.2 (190.2–203.6) — 첫 hold-out의 267° 같은 불가능한 값 없음 |

- 홀드 안 결함(clip): 어깨 닫힘 `closed_shoulder` 31..139 (−26.6°) 하나. 홀드 밖: `closed_shoulder` 0..4, 8..12.
- 정렬 θ는 전 구간 181.9–189.2로 **모두 175 위(바나나 쪽)** 이다. 실패 샘플 45개는 홀드 진입 직후(~1.5 s, 186–189°)와 끝부분(~2 s, 185.4–189°)에 몰려 있고, 가운데 ~7 s는 182–185.7°로 대부분 게이트 안이다.
- landmark 육안 확인(판정 이후, 파라미터 조정 없음): 홀드 샘플 62(6.2 s)·101(10.1 s)에 LEFT 손목·팔꿈치·어깨·엉덩이·무릎·발목을 찍은 결과 **6개 모두 몸 위에 놓인다**(손목은 바닥의 손, 발목은 화면 위 가장자리의 발). 첫 hold-out 같은 가림·오검출은 보이지 않는다. 앞쪽 이불이 손 바로 아래까지 올라와 있지만 손목은 보인다.
- fixture: **커밋하지 않았다**(커밋하면 `testDebugUnitTest`가 AC-4로 red). 사용자 결정 대기 — 게이트 재정의(ADR) 여부. 입력 변경(`HOLDOUT_VIDEO`)만 커밋.

## 모델·가속 비교 (full/heavy × CPU/GPU)

`ModelDelegateBenchmarkTest#compare`(프로덕션 CodecFrameSource → OrientationResolver → PoseLandmarkerEngine → assemble, 프레임 JPEG 저장 없음, 설정 사이 30 s 휴지). heavy는 `pose_landmarker_heavy.task`(float16 latest, 29.2 MB)를 테스트용으로만 `<external files>/models/`에 push(미커밋). GPU는 `BaseOptions.setDelegate(Delegate.GPU)` — 생성 실패 시 CPU 폴백·로그(`PoseDetector` 태그), 이 기기에서는 네 설정 모두 요청한 delegate로 생성됨(폴백 없음). Δθ = tune 홀드(full-CPU 기준 14..130)에서 스무딩 θ의 full-CPU 대비 평균 |Δ|.

| 설정 | 검출률 | 평균 visibility (tune / 30s) | 추론 ms/프레임 (tune / 30s) | 총 분석 tune 13 s | 총 분석 30 s | tune 홀드 | Δθ 정렬 / 골반 / 어깨 / 팔꿈치 |
|---|---|---|---|---|---|---|---|
| **full-CPU (현행)** | 100% | 0.978 / 0.978 | 112 / 105 | 21.8 s | **39.9 s** | 14..130 | 기준 |
| full-GPU | 100% | 0.978 / 0.977 | 52 / 54 | 18.5 s | 35.9 s | 14..130 | 0.26 / 0.41 / 0.55 / 0.99° |
| heavy-CPU | 100% | 0.660 / 0.656 | 245 / 249 | 42.5 s | 84.7 s ✗ | **없음** | 0.76 / 0.27 / 1.79 / 5.65° |
| heavy-GPU | 100% | 0.688 / 0.687 | 82 / 77 | 21.1 s | 41.1 s | 18..55 | 1.05 / 1.80 / 1.86 / 5.59° |

발열(`ModelDelegateBenchmarkTest#thermal30s`, 30 s 영상 연속 3회, CPU 3회 직후 GPU 3회):

| 설정 | 30 s 총 분석 min / median / max | 추론 ms/프레임 |
|---|---|---|
| full-CPU | **42.0 / 42.1 / 43.0 s** | 112–115 |
| full-GPU | 40.6 / 40.7 / 41.5 s | 63–64 |

- **채택 없음 — full-CPU 유지.** NFR-2(30 s ≤ 60 s)는 현행 CPU로 여유 17 s(최대 43.0 s)로 충족. GPU는 추론을 절반으로 줄이지만 총 분석은 1–4 s(3–10%)만 줄어든다 — process 구간이 이제 디코드(샘플당 ~120 ms, IO 스레드) 병목이라서. 각도 일치는 기준(~1°) 안이지만 이득이 작아 "가장 작은 변경" 원칙에 따라 채택하지 않았다.
- heavy는 두 delegate 모두 4규칙 관절 visibility가 0.66–0.69로 떨어져(가시성 기준 미달 프레임 증가) tune 홀드를 잃거나(CPU) 크게 줄이고(GPU 18..55), 팔꿈치 Δθ 5.6°. heavy-CPU는 NFR 초과. 기각.
- P1.5 기록의 30 s 55.5 s는 연속 실행 끝의 발열 상태 값(추론 147 ms)이었고, 이번 측정은 추론 105–115 ms 상태다.
- `MediaPipePoseDetector`는 모델 경로·delegate 인자(기본 full·CPU)와 CPU 폴백을 갖는다 — 벤치마크용 seam이며 프로덕션 기본값은 변화 없음.

## 메모리 (NFR 힙 ≤ 256MB)

`PoseSpikeInstrumentedTest#spike`(프로덕션 CodecFrameSource → OrientationResolver → PoseLandmarkerEngine 경로, `wall_handstand_tune_30s.mp4` 포함 분석) 실행 중 `adb shell dumpsys meminfo com.calisvision`을 1초 간격으로 폴링, 각 지표의 실행 전체 최고치를 기록.

| 항목 | 최고치 |
|---|---|
| Java Heap (PSS) | 34.8 MB (35,608 KB) |
| Native Heap (PSS) | 85.4 MB (87,472 KB) — MediaPipe·비트맵 |
| Graphics | 75.1 MB (76,872 KB) |
| TOTAL PSS | 236.0 MB (241,650 KB) |

- 기기: SM-F766N(Android 16, R3KL202BBMJ). 힙 상한: `dalvik.vm.heapgrowthlimit` 256m, `dalvik.vm.heapsize` 512m.
- **PASS** — NFR의 "힙 ≤ 256MB"는 Java heap 기준. 최고치 34.8MB로 상한(256MB) 대비 여유 221MB. Native heap·Graphics는 별도 네이티브 할당(참고용)이며, TOTAL PSS도 236MB로 256MB 내에 든다.

## 비행기 모드 (§6-7, AC-5 수동)

`adb shell cmd connectivity airplane-mode enable` + `adb shell svc wifi disable`(이 기기는 비행기 모드에서도 Wi-Fi 연결을 유지해 첫 시도 때 `ping 8.8.8.8` 성공 → 두 번째부터 Wi-Fi도 끔) → `Active default network: none`, `ping: Network is unreachable` 확인 → `DeviceSmokeTest`(실제 `DefaultVideoAnalyzer`로 tune 영상 홈→가이드→선택→분석→결과→결함 시트) → trap으로 복구(`airplane=0 wifi=1` 확인).

- 결과: **PASS** — `DeviceSmokeTest` 1 tests, 0 failures, 47.5 s, 테스트 종료 시점에도 `Active default network: none`.
- (중간 1회 실패는 화면 잠금으로 Compose 계층이 없던 것 — 잠금 해제 후 재실행. 네트워크와 무관.)

## APK

- `app-debug.apk` 101,769,659 B(97.1 MB) — 4개 ABI의 `libmediapipe_tasks_jni.so`(arm64 11.0 MB, x86 15.6 MB, x86_64 13.7 MB, armeabi-v7a 7.7 MB) + 모델 9.4 MB가 대부분. ABI split/릴리스 축소는 미적용.
- `aapt2 dump permissions`: `com.calisvision.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`만 존재, **INTERNET 행 0**.

## AC 대조표 (2026-09-27, 커밋 `459cae6`·`3a23e3c` 기준)

| AC | 상태 | 근거 |
|---|---|---|
| AC-1 검출 ≥ 90% | PASS | tune 131/131, hold-out 177/177, holdout3 140/140, tune_30s 301/301 (`WallHandstandInstrumentedTest`, `ModelDelegateBenchmarkTest`); rot180 사본 100% (P1.5) |
| AC-2 4규칙 각도/null | PASS | `AngleTimelineTest`, 결과 패널 4행(`DeviceSmokeTest` 스크린샷) |
| AC-3 FaultSegment ≥ 3샘플·부호별·병합 | PASS | `FaultEvaluatorTest`, `FaultSheetTest.mergedPikeShowsBothRuleAngles` |
| AC-4 hold-out 정렬 게이트 | **FAIL** | 재촬영 holdout3: 홀드 24..139 = 116 샘플 ✓, θ∈[175,185] **61.2%** < 95%(평균 184.9°, 181.9–189.2) — 위 "hold-out 재촬영 (holdout3)". 첫 hold-out: 홀드 47 < 50, 19.1%(손 가림). fixture 미커밋(사용자 결정) |
| AC-5 INTERNET 0 · 비행기 모드 | PASS | aapt2 INTERNET 0; 비행기 모드+Wi-Fi off `DeviceSmokeTest` PASS |
| AC-6 화면 전환 | PASS | `NavigationFlowTest.homeToGuideToPickerToResult` |
| AC-7 스크러버 1 step = 1 샘플, 동기 | PASS | `ResultViewModelTest.stepMovesExactlyOneSampleAndKeepsFrameSkeletonAnglesInSync`, `scrubberCellWidthIsOneSample`; 이미지·스켈레톤 겹침은 `DeviceSmokeTest` 스크린샷 육안 |
| AC-8 결함 띠·탭 시 결함명·힌트 | PASS | `FaultSheetTest.tappingBananaOpensSheetWithHint` |
| AC-9 임계값 변경 → 재분석 0회 | PASS | `ResultViewModelTest.thresholdChangeUpdatesFaultsWithoutReanalysis` |
| AC-9b 앱 내 촬영 | 미검증 | 후순위 — 촬영 기능 미구현 |
| AC-10 knowledge non-blank | PASS | `HandstandKnowledgeTest` |
| AC-11 선택기 전 가이드 | PASS | `NavigationFlowTest` |
| AC-12 dummy Exercise 확장 | PASS | `ExerciseCatalogTest` |
| AC-13 PRD 5개 섹션 | PASS | §6-1 `grep -c` = 5 |
| AC-14 REQUIREMENTS AC 참조 | PASS | §6-1 루프 출력 없음 |
| AC-15 ADR ≥ 5, 9개 섹션 | PASS | ADR 7개, `grep -L` 출력 없음 |
| AC-16 민감 파일 무시 | PASS | `git check-ignore` 10행, porcelain grep exit=1 |

요약(holdout3 재판정 반영): PASS 15, FAIL 1(AC-4), 수동 0(AC-5 비행기 모드·AC-7 육안은 수행 완료로 PASS에 포함), 미검증 1(AC-9b).

전역 검사: `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` green(JVM 73 tests, skip 1 = hold-out fixture 없는 `WallHandstandAlignmentTest`), domain 경계 import 0, INTERNET 0. 영상 없는 instrumented(`notAnnotation=RequiresVideo`): SmokeTest 1 · FaultSheetTest 2 · NavigationFlowTest 3, 실패 0.

## NFR

- NFR-2 30 s 영상 ≤ 60 s: **PASS** — full-CPU 연속 3회 42.0 / 42.1 / 43.0 s(여유 17 s), 단일 39.9 s. 실제 앱 경로(프레임 JPEG 저장 포함) 13 s tune 22.9 s, 17.7 s hold-out 29.9 s.
- 힙 ≤ 256 MB: **PASS** — Java Heap 최고치 34.8 MB(상한 256 MB, 여유 221 MB). 상세는 위 "메모리 (NFR 힙 ≤ 256MB)".

## 알려진 한계

- **AC-4 FAIL** — 재촬영 holdout3도 정렬 게이트 미달(61.2%, 홀드 116 샘플은 충족). 세 영상 모두 [175,185]/95% 미달 → 게이트 재정의(ADR) 여부 사용자 결정.
- **낮은 카메라 각도**: tune·hold-out·holdout3 모두 바닥 근처에서 올려다보는 구도(가이드의 엉덩이 높이와 다름) — 2D 원근으로 각도가 왜곡될 수 있다. tune은 손이 벽에서 떨어져 몸이 ~19° 기운 자세라 어깨 닫힘이 전 구간 결함으로 나온다.
- hold-out 팔꿈치 θ 255–281°(물리적으로 불가능) — landmark 오류로 추정, 원인 미조사(hold-out 동결).
- **단일 기기**(SM-F766N, Android 16)에서만 측정. 성능·`setRotationDegrees` 동작·GPU 가용성은 기기 의존.
- **CFR·8-bit 4:2:0 가정**(`CodecFrameSource`): VFR은 샘플 시각이 어긋날 수 있고 10-bit/HDR 미지원.
- **결과는 메모리에만 보관**(`AnalysisSessionStore`) — 프로세스 종료 시 사라짐. 프레임 JPEG는 캐시 디렉터리.
- heavy 모델은 이 영상에서 visibility가 떨어져 부적합, GPU는 이득이 작아 미채택(위 "모델·가속 비교").
- §6-8 별도 lane verifier/code-reviewer의 AC 대조 승인은 이 문서 작성 이후 단계.
