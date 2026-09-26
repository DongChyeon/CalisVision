# Architecture Decision Records (ADR)

이 디렉터리는 CalisVision의 주요 아키텍처 결정을 기록한다.

## 네이밍 규칙

`ADR-NNNN-slug.md` (예: `ADR-0001-on-device-rule-based.md`). `NNNN`은 4자리 순번, `slug`는 결정 내용을 요약한 kebab-case 영문.

## 템플릿

```markdown
## Status
Proposed | Accepted | Deprecated | Superseded

## Date
YYYY-MM-DD

## Context
이 결정이 필요해진 배경, 문제, 제약.

## Decision
채택한 결정 내용.

## Drivers
결정에 영향을 준 주요 요인(우선순위 순).

## Alternatives
검토했던 대안들.

## Why chosen
현재 결정을 선택한 이유(대안 대비).

## Consequences
이 결정으로 발생하는 장단점, 트레이드오프.

## Follow-ups
후속 조치, 재검토 조건, 열린 질문.
```

새 ADR을 작성할 때는 위 9개 H2 섹션을 모두 포함해야 한다.
