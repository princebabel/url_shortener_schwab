---
name: requirement-analysis
description: Normalizes vague feature requests (e.g., "add analytics") into a structured spec with explicit ambiguity call-outs, outputting to docs/requirement-analysis.md.
---

# Requirement Analysis

## Purpose
Transforms ambiguous or high-level product asks into a normalized, reviewable specification that captures functional scope, non-functional constraints, and open questions. This skill exists because stakeholders often describe outcomes (e.g., "track link performance") without specifying exact metrics, retention windows, or access patterns — leading to rework if implemented directly.

## When to use
- User says "add analytics", "add dashboard", "support custom domains", "enable expiration", or similar outcome-oriented requests
- A new feature request lacks concrete acceptance criteria
- Stakeholder feedback is vague ("make it faster", "better monitoring")
- Before any task-decomposition or scaffolding begins

## Inputs this skill expects
- The raw request or user story (verbatim)
- Existing `docs/requirement-analysis.md` (to append, not overwrite)
- Current API surface from live Swagger UI at `/swagger-ui.html` (9 endpoints)
- `docs/architecture.md` for context on layered architecture (controller/service/repository/entity/dto/exception/config)
- `docs/future-enhancements.md` to avoid duplicating planned work

## Process
1. Read the raw request and identify the core capability being asked for.
2. Enumerate explicit functional requirements (what the system must do).
3. Enumerate non-functional requirements (latency, retention, auth, audit, etc.).
4. List every ambiguity: missing metrics, undefined retention, unspecified auth model, unclear error behavior, pagination defaults, etc.
5. Map each requirement to existing or new endpoints among the 9 current routes (POST /api/urls, GET /api/urls/{shortCode}, GET /api/urls/{shortCode}/analytics, GET /api/urls, GET /api/urls/search, GET /api/dashboard/summary, GET /api/dashboard/top, GET /api/dashboard/recent, GET /api/v1/health).
6. Note which package layers would be touched (controller, service, repository, entity, dto, exception, config).
7. Append a dated, versioned entry to `docs/requirement-analysis.md` with sections: Request, Functional Requirements, Non-Functional Requirements, Ambiguities, Affected Endpoints, Affected Packages, Open Questions.

## Output / deliverable
An appended section in `docs/requirement-analysis.md` containing a normalized spec with explicit ambiguity call-outs. No code is generated.

## Quality checks before returning output
- [ ] Every functional requirement is testable (can be verified by an API call or DB state)
- [ ] Every ambiguity is phrased as a concrete question a stakeholder can answer
- [ ] No requirement assumes Redis/caching (it's a future enhancement, not present)
- [ ] Affected endpoints reference the actual 9 live routes
- [ ] Affected packages use exact names: controller, service, repository, entity, dto, exception, config
- [ ] Entry is appended with date/version, not overwriting prior analysis

## Example
**Request:** "Add analytics for short links"
**Output appended to docs/requirement-analysis.md:**
```
## 2025-01-15 v1.2 — Add Analytics for Short Links
### Request
"Add analytics for short links"

### Functional Requirements
- Track click count per short code (already partially implemented via redirect flow)
- Expose click count, createdAt, lastAccessedAt via GET /api/urls/{shortCode}/analytics
- Support time-windowed queries (last 24h, 7d, 30d) — NEW endpoint or query params
- Dashboard summary: total links, total clicks, top 10 by clicks, recent 10

### Non-Functional Requirements
- Analytics reads must not add >50ms p99 to redirect path
- Retention: raw click events retained 90 days; aggregates retained indefinitely
- No PII in analytics payload (correlation ID only for traceability)

### Ambiguities
- Should time-windowed analytics be a new endpoint (GET /api/urls/{shortCode}/analytics?window=7d) or separate endpoint?
- Is "lastAccessedAt" updated on every redirect or only on first?
- Are analytics visible to unauthenticated callers or only link owners? (No auth exists today)
- Should dashboard endpoints be paginated? (Currently unpaginated)

### Affected Endpoints
- GET /api/urls/{shortCode}/analytics (extend response)
- GET /api/dashboard/summary (extend response)
- GET /api/dashboard/top (verify pagination)
- GET /api/dashboard/recent (verify pagination)
- Potentially NEW: GET /api/urls/{shortCode}/analytics/window

### Affected Packages
- controller (UrlController)
- service (UrlService, UrlServiceImpl)
- repository (UrlRepository, UrlLinkRepository)
- dto (UrlAnalyticsResponse, DashboardSummaryResponse)
- entity (UrlEntity, UrlLinkEntity)

### Open Questions
- Stakeholder to confirm time-window granularity and auth model
```