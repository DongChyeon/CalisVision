# VERIFICATION — P1.5 De-risk Spike (역자세 인식 실측)

- 일자: 2026-09-27
- 기기: Samsung SM-F766N, Android 16 (SDK 36), adb serial R3KL202BBMJ
- 코드: `develop`, 테스트 `app/src/androidTest/java/com/calisvision/PoseSpikeInstrumentedTest.kt`
  (프로덕션 `RetrieverFrameSource` · `OrientationResolver` · `MediaPipePoseDetector` · `PoseLandmarkerEngine` · `AnalysisResult.assemble` 그대로 사용)
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

## 주의 사항

- **카메라 각도**: 카메라가 바닥 근처에서 약간 올려다보는 구도(가이드의 "엉덩이 높이 삼각대"와 다름). 원근 왜곡으로 어깨·팔꿈치 각도가 실제와 다르게 측정될 수 있어 위 θ 통계를 기준값으로 쓰기 전에 가이드 구도 영상으로 재확인 필요.
- rot180 사본은 원본보다 visibility가 낮고(0.89 vs 0.98) 홀드 구간이 짧게 검출됨(23 vs 94 샘플). 재인코딩 차이 또는 직립 방향 지터로 추정 — 방향 비의존성이 완전하지 않다는 신호.
- **성능(⑤)**: 원본 13s 영상 41–47s → 30s 영상 환산 약 95–110s로 NFR(≤ 60s) **초과 추세**. 디코드가 60% 이상. `OPTION_CLOSEST`는 GOP 길이에 비례(GOP 8.3s인 rot180 사본은 프레임당 0.5–0.9s). B1'(`getFramesAtIndex`) 전환 검토 필요.

## 다음 단계

1. ~~방향 게이트 재정의 여부 결정(ADR)~~ — ADR-0006으로 결정(재정의 후 PASS). 180° 경로는 Matrix 회전으로 교체.
2. NFR 초과 → B1' 디코드(`getFramesAtIndex` 또는 MediaCodec 순차 디코드) 시험, 추론 GPU delegate 시험.
3. 어깨 열림·팔꿈치 Range의 부호/기본값을 튜닝 fixture(`app/src/test/resources/fixtures/wall_handstand_tune.json`)로 P2/P4에서 보정.
4. 가이드 구도(엉덩이 높이 카메라) 영상 추가 확보.
5. P4: hold-out fixture를 테스트 리소스로 옮겨 `WallHandstandAlignmentTest`(AC-4) 실행.
