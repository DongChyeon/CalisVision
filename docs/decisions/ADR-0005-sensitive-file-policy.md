# ADR-0005: 민감 파일 비추적 정책

## Status
Accepted

## Date
2026-09-26

## Context
서버가 없는 프로젝트라도 민감 파일이 존재한다: 사용자 신체가 담긴 운동/테스트 영상, 앱 서명키, 로컬 환경설정. 이들이 실수로 git에 커밋되면 프라이버시 침해나 서명키 유출로 이어질 수 있다.

## Decision
`.gitignore`로 다음을 제외한다: 영상 파일(대소문자 확장자 `*.mp4 *.MP4 *.mov *.MOV *.m4v *.avi *.mkv *.webm`) 및 `test-videos/`, 서명 관련(`*.jks *.keystore keystore.properties`), 로컬 설정(`local.properties`), 표준 Android 빌드 산출물(`build/ .gradle/ .idea/ *.iml captures/`). `.omc/` 디렉터리는 사용자 결정(2026-09-26)에 따라 비추적으로 유지한다.

MediaPipe 모델 파일(`pose_landmarker_full.task`, ~9MB)은 위 정책의 예외로 **`app/src/main/assets/`에 커밋한다**(사용자 결정: 다운로드 스크립트 대신 저장소 직접 커밋). 테스트는 실제 영상 대신 landmark JSON fixture만 커밋하고, 기기 영상은 앱 전용 외부 디렉터리(`/sdcard/Android/data/com.calisvision/files/test-videos/`)에만 `adb push`한다.

## Drivers
- 신체가 담긴 영상의 프라이버시 보호
- 서명키(release keystore) 유출 방지
- 저장소 크기 관리(대용량 영상 배제)

## Alternatives
- Git LFS로 영상/모델 관리
- 별도 private 저장소 분리
- 모델 파일도 다운로드 스크립트로 배제

## Why chosen
백엔드가 없는 1인 개발 프로젝트에서는 `.gitignore` + fixture 커밋 조합이 가장 단순하고, 모델 파일은 빌드 재현성을 위해 직접 커밋하는 편이 다운로드 스크립트 유지보수보다 낫다고 판단했다(사용자 결정).

## Consequences
- 기준 영상(튜닝/hold-out)은 개발자 로컬에만 존재하고, 재현은 커밋된 fixture JSON으로 한다.
- 저장소에 ~9MB 모델 파일이 포함되어 clone 크기가 커진다.
- 영상 계측 테스트는 `@RequiresVideo` 어노테이션으로 분리하고, 영상 부재 시 skip한다.

## Follow-ups
- pre-commit hook으로 대용량 파일/영상 확장자 커밋을 자동 차단하는 방안을 추가한다.
- CI 환경에서는 서명키를 환경변수로 주입하고 `keystore.properties`는 생성하지 않는다.
