---
name: task-decomposition
description: Breaks a normalized spec (from requirement-analysis) into ordered, dependent tasks with risk levels and acceptance criteria, outputting to docs/task-decomposition.md.
---

# Task Decomposition

## Purpose
Converts a validated requirement spec into a concrete, executable task list that respects layer dependencies (controller → service → repository → entity), risk levels, and acceptance criteria. This skill exists because jumping straight to code without a task plan leads to missed edge cases, wrong layer ordering, and untracked scope creep.

## When to use
- After requirement-analysis produces a normalized spec in `docs/requirement-analysis.md`
- User says "break this down", "plan the work", "what are the tasks for…"
- Before any spring-boot-scaffolding or test-generation begins
- When a feature spans multiple layers (which is almost always)

## Inputs this skill expects
- A normalized spec section from `docs/requirement-analysis.md` (latest entry)
- Current project structure: controller/, service/, repository/, entity/, dto/, exception/, config/
- The 9 live endpoints as integration touchpoints
- `docs/task-decomposition.md` (to append, not overwrite)
- `docs/architecture.md` for layer dependency rules

## Process
1. Read the latest requirement-analysis entry and identify all affected layers.
2. For each layer, create tasks in dependency order: entity → repository → dto → service → controller → config (if OpenAPI annotations change) → test.
3. For each task, assign: Task ID, Title, Layer, Dependencies (Task IDs), Risk (Low/Medium/High), Acceptance Criteria (verifiable).
4. Flag tasks that touch shared code (e.g., GlobalExceptionHandler in exception/, CorrelationIdFilter in config/) as High risk.
5. Flag tasks that change response shapes of existing endpoints as Medium risk (breaking change potential).
6. Append a dated task list to `docs/task-decomposition.md` using the established markdown table format.

## Output / deliverable
An appended task table in `docs/task-decomposition.md` with columns: Task ID, Title, Layer, Dependencies, Risk, Acceptance Criteria.

## Quality checks before returning output
- [ ] Tasks follow layer dependency order (entity before repository before service before controller)
- [ ] Every task has a verifiable acceptance criterion (e.g., "GET /api/urls/{shortCode}/analytics returns 200 with clickCount field")
- [ ] Risk levels are justified (High = touches shared infra or changes public contract)
- [ ] No task assumes Redis/caching exists
- [ ] Task IDs are unique and sortable (e.g., T-01, T-02)
- [ ] Appended to existing file, not overwritten

## Example
**Input:** Requirement analysis for "Add time-windowed analytics" (from requirement-analysis example)
**Output appended to docs/task-decomposition.md:**
```
## 2025-01-15 v1.2 — Time-Windowed Analytics Tasks

| Task ID | Title | Layer | Dependencies | Risk | Acceptance Criteria |
|---------|-------|-------|--------------|------|---------------------|
| T-01 | Add `window` query param to UrlAnalyticsResponse DTO | dto | — | Low | DTO compiles; field documented with @Schema |
| T-02 | Extend UrlLinkRepository with time-windowed click count query | repository | T-01 | Medium | Custom query returns click count for given shortCode + window; integration test passes |
| T-03 | Add time-windowed logic to UrlServiceImpl.getAnalytics | service | T-02 | Medium | Service returns windowed counts; unit test covers 24h/7d/30d |
| T-04 | Update UrlController GET /api/urls/{shortCode}/analytics to accept `window` param | controller | T-03 | Medium | Endpoint returns 200 with windowed data; Swagger shows new param |
| T-05 | Add integration test for windowed analytics endpoint | test | T-04 | Low | Testcontainers test verifies 200 + correct counts for each window |
| T-06 | Update docs/api-summary.md with new query param | docs | T-04 | Low | api-summary.md reflects window param on analytics endpoint |
```