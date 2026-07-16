---
name: codebase-impact-analysis
description: For brownfield changes, traces the controller→service→repository→entity chain for the touched class, maps data flow across the 9 live endpoints, and produces an impact map before any code is edited.
---

# Codebase Impact Analysis

## Purpose
Prevents unintended side effects by mapping the full call chain and data flow before modifying any existing class. This skill exists because changes to shared components (e.g., `UrlEntity`, `GlobalExceptionHandler`, `CorrelationIdFilter`, `IdGenerator`) or service methods called by multiple controllers can ripple across the 9 endpoints in non-obvious ways.

## When to use
- User says "change…", "modify…", "refactor…", "update…" on any existing class
- Before editing: entity, repository, service, controller, exception, config, or util classes
- When a change might affect more than one endpoint
- Before spring-boot-scaffolding modifies shared code
- When regression-test-generation needs to know what to pin

## Inputs this skill expects
- The fully qualified class name or file path to be changed
- Current project structure (controller/, service/, repository/, entity/, dto/, exception/, config/, util/)
- The 9 live endpoints and their controller mappings:
  - `UrlController`: POST /api/urls, GET /api/urls/{shortCode}, GET /api/urls/{shortCode}/analytics, GET /api/urls, GET /api/urls/search, GET /api/dashboard/summary, GET /api/dashboard/top, GET /api/dashboard/recent
  - `UrlShortenerController`: GET /api/v1/health
- `docs/architecture.md` for layer diagram
- `docs/api-summary.md` for endpoint list
- IDE "Find Usages" / call hierarchy (or grep for class name across packages)

## Process
1. Identify the target class and its package layer.
2. Trace **upward** (callers): Which controllers call this service? Which services use this repository? Which repositories query this entity? Which DTOs map to/from this entity?
3. Trace **downward** (dependencies): What does this class call? (Other services, repositories, util classes, config beans)
4. Map **endpoint impact**: For each of the 9 endpoints, mark Affected/Unaffected based on whether the call chain touches the target class.
5. Identify **shared infrastructure impact**: Does the class touch `GlobalExceptionHandler`, `CorrelationIdFilter`, `OpenApiConfig`, `IdGenerator`, `application.yml`?
6. Identify **data model impact**: If entity changes, which repositories, services, DTOs, controllers, and Flyway/Liquibase migrations (if any) are affected?
7. Produce a concise impact map (markdown) with: Target Class, Layer, Direct Callers, Endpoints Affected (list of 9), Shared Infra Touched, Data Model Changes, Risk Level (Low/Medium/High), Recommended Regression Tests.

## Output / deliverable
An impact map (markdown) printed to the conversation — **no file written**. The user (or regression-test-generation skill) uses this to decide test scope.

## Quality checks before returning output
- [ ] All 9 endpoints explicitly marked Affected/Unaffected
- [ ] Call chain traced through actual package layers (controller→service→repository→entity)
- [ ] Shared infra (GlobalExceptionHandler, CorrelationIdFilter, IdGenerator, OpenApiConfig) explicitly checked
- [ ] Data model changes (entity fields, repository queries) mapped to DTOs and controllers
- [ ] Risk level justified (High = touches shared infra or changes public contract of ≥2 endpoints)
- [ ] No Redis/caching assumptions
- [ ] Output is a readable markdown table + bullet summary, not a wall of text

## Example
**Target:** `UrlEntity` (add `expiresAt` field)
```
## Impact Map — UrlEntity (entity layer)

### Target Class
`com.schwab.urlshortener.entity.UrlEntity` — JPA entity for short URL mappings

### Layer
entity

### Direct Callers (Upward Trace)
- UrlRepository (JpaRepository<UrlEntity, Long> + custom queries)
- UrlLinkRepository (if UrlLinkEntity references UrlEntity)
- UrlServiceImpl (maps entity ↔ DTO)
- UrlController (via service)

### Endpoints Affected (9 total)
| Endpoint | Affected? | Reason |
|----------|-----------|--------|
| POST /api/urls | Yes | Create saves new entity with expiresAt |
| GET /api/urls/{shortCode} | Yes | Redirect reads entity; may check expiresAt |
| GET /api/urls/{shortCode}/analytics | Yes | Analytics reads entity; may include expiresAt |
| GET /api/urls | Yes | List returns entities; may filter by expiresAt |
| GET /api/urls/search | Yes | Search queries entity; may include expiresAt |
| GET /api/dashboard/summary | Maybe | Summary aggregates; if expiresAt affects active count |
| GET /api/dashboard/top | Maybe | Top URLs query; if expired links excluded |
| GET /api/dashboard/recent | Maybe | Recent URLs query; if expired links excluded |
| GET /api/v1/health | No | Health check doesn't touch UrlEntity |

### Shared Infrastructure Touched
- None directly (but IdGenerator unaffected)

### Data Model Changes
- New column `expires_at` on `urls` table
- Requires DB migration (Flyway/Liquibase) — not currently in project
- UrlResponse DTO may need `expiresAt` field
- CreateUrlRequest may need optional `expiresAt` input

### Risk Level
**Medium** — touches 6-8 endpoints, requires DB migration, changes public DTO

### Recommended Regression Tests
- POST /api/urls with and without expiresAt
- GET /api/urls/{shortCode} with expired vs active link
- GET /api/urls list with expired filter
- Dashboard endpoints with mixed expired/active data
```