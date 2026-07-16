---
name: documentation-sync
description: After any code change, updates docs/architecture.md, docs/api-summary.md, and docs/engineering-decisions.md to match actual implementation; flags docs/future-enhancements.md items if a change implements one (e.g., adding caching removes "Redis caching" from the list).
---

# Documentation Sync

## Purpose
Keeps the project's living documentation in sync with the actual codebase. This skill exists because architecture diagrams, API summaries, and decision logs drift silently when code changes — leading to onboarding confusion and incorrect assumptions during reviews.

## When to use
- After any merged change to controller, service, repository, entity, dto, exception, config
- After spring-boot-scaffolding adds new classes
- After api-contract-design changes endpoint contracts
- After security-review adds mitigations
- User says "update docs", "sync documentation", "docs are out of date"
- Before release or PR merge (as a checklist item)

## Inputs this skill expects
- Changed files (from git diff or PR)
- Current documentation files:
  - `docs/architecture.md` — layer diagram, request flows, component descriptions
  - `docs/api-summary.md` — markdown table of 9 endpoints (method, path, purpose, request/response)
  - `docs/engineering-decisions.md` — dated decision log with rationale
  - `docs/future-enhancements.md` — checklist of planned work
- Live Swagger UI at `/swagger-ui.html` (source of truth for API contract)
- `docs/requirement-analysis.md` and `docs/task-decomposition.md` (for context)

## Process
1. **Identify scope**: From changed files, determine which doc(s) need updates:
   - Controller/service/repository/entity/dto changes → `architecture.md` (component list), `api-summary.md` (if endpoints changed)
   - New endpoint or contract change → `api-summary.md` (mandatory), `engineering-decisions.md` (if breaking)
   - Exception/config/validation changes → `engineering-decisions.md`
   - Implementation of a future-enhancement item → `future-enhancements.md` (mark done)

2. **Update `docs/architecture.md`**:
   - Mermaid diagram reflects current packages (controller, service, repository, entity, dto, exception, config)
   - Request flow sequences match current controller→service→repository calls
   - Component descriptions match actual class names and responsibilities

3. **Update `docs/api-summary.md`**:
   - Table has exactly the 9 (or current count) live endpoints
   - Method, path, purpose, request/response summary match Swagger UI
   - Query params, path variables, status codes accurate

4. **Update `docs/engineering-decisions.md`**:
   - Append new entry: `## YYYY-MM-DD — <short title>`
   - Sections: Context, Decision, Rationale, Alternatives Considered, Consequences
   - Link to PR/commit if applicable

5. **Update `docs/future-enhancements.md`**:
   - If a change implements a listed item (e.g., "Redis caching for redirects"), move it to "Completed" section with date
   - If a change makes a future item obsolete or changes its scope, update the description

6. **Verify**: Open Swagger UI at `/swagger-ui.html` and confirm `docs/api-summary.md` matches.

## Output / deliverable
- Updated `docs/architecture.md`
- Updated `docs/api-summary.md`
- Updated `docs/engineering-decisions.md` (appended)
- Updated `docs/future-enhancements.md` (if applicable)
- No code changes

## Quality checks before returning output
- [ ] `docs/api-summary.md` table matches live Swagger UI exactly (9+ endpoints)
- [ ] `docs/architecture.md` Mermaid diagram renders and matches package structure
- [ ] `docs/engineering-decisions.md` has new dated entry for any contract/arch decision
- [ ] `docs/future-enhancements.md` reflects completed items (no stale "TODO" for done work)
- [ ] No documentation references Redis/caching as implemented (only as future)
- [ ] All file paths use correct case and relative paths from repo root
- [ ] No placeholder/lipsum content — every section reflects actual code

## Example
**Change:** Added `expiresAt` field to `UrlEntity`, `CreateUrlRequest`, `UrlResponse`, and `GET /api/urls/{shortCode}` now returns 410 Gone for expired links.

**Updates:**

`docs/api-summary.md`:
```markdown
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /api/urls | Create a shortened URL (optional `expiresAt` in request) |
| GET | /api/urls/{shortCode} | Redirect to original URL (returns 410 if expired) |
| GET | /api/urls/{shortCode}/analytics | Retrieve analytics for a short URL |
| GET | /api/urls | List and filter shortened URLs (supports `expired` filter) |
| GET | /api/urls/search | Search URLs by short code, original URL, or custom alias |
| GET | /api/dashboard/summary | Retrieve dashboard summary metrics |
| GET | /api/dashboard/top | List top URLs by click count |
| GET | /api/dashboard/recent | List most recently created URLs |
| GET | /api/v1/health | Health check |
```

`docs/engineering-decisions.md`:
```markdown
## 2025-01-15 — Link Expiration Support

### Context
Stakeholders requested automatic link expiration to support time-limited campaigns.

### Decision
Added optional `expiresAt` (ISO-8601) to `CreateUrlRequest` and `UrlEntity`. Redirect endpoint returns 410 Gone with `ExpiredUrlException` when `expiresAt` is past. List endpoint supports `expired=true/false` filter.

### Rationale
- 410 Gone is semantically correct for permanently removed resources (RFC 7231)
- Optional field maintains backward compatibility
- Filter on list endpoint enables cleanup workflows

### Alternatives Considered
- Soft delete with `active=false` — doesn't convey "expired" semantics
- 404 Not Found — loses distinction between "never existed" and "expired"

### Consequences
- Clients must handle 410 on redirect (previously only 302/404)
- Database migration required for `expires_at` column
- Analytics unchanged (expired links still have click history)
```

`docs/future-enhancements.md`:
```markdown
## Completed
- [x] 2025-01-15 — Link expiration (expiresAt field, 410 Gone, expired filter)

## Planned
- [ ] Redis caching for redirects
- [ ] Rate limiting on POST /api/urls
- [ ] User authentication and authorization
- [ ] QR code generation
- [ ] Kubernetes deployment
```
```