---
name: api-contract-design
description: Designs or updates the OpenAPI contract via springdoc annotations on controllers; updates docs/api-summary.md; flags breaking vs additive changes for the 9 live endpoints.
---

# API Contract Design

## Purpose
Ensures the API contract (exposed via live Swagger UI at `/swagger-ui.html`) remains the single source of truth. All contract changes are made through springdoc-openapi annotations on controller methods, and `docs/api-summary.md` is kept in sync. This skill exists because hand-maintained OpenAPI files drift from code; annotation-driven contracts stay accurate.

## When to use
- Adding a new endpoint (new controller method or new controller)
- Changing request/response shape of any of the 9 existing endpoints
- Adding query parameters, path variables, or headers
- Changing HTTP status codes or error response formats
- User says "change the API to…", "add a field to…", "new endpoint for…"

## Inputs this skill expects
- Current controller classes: `UrlController` (8 endpoints), `UrlShortenerController` (1 endpoint)
- Current DTOs in `dto/`: `CreateUrlRequest`, `UrlResponse`, `UrlAnalyticsResponse`, `DashboardSummaryResponse`, `BaseResponse`, `ErrorResponse`, `CreateShortUrlRequest`, `CreateShortUrlResponse`
- `docs/api-summary.md` (to update)
- `docs/engineering-decisions.md` (for context on prior contract decisions)
- Live Swagger UI at `/swagger-ui.html` for verification

## Process
1. Identify which of the 9 endpoints are affected (or if a 10th is added).
2. For each affected endpoint, determine if the change is **additive** (new optional field, new optional query param, new endpoint) or **breaking** (removed/renamed field, changed type, removed endpoint, changed required status, changed status code).
3. Update springdoc annotations on the controller method: `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema` on DTOs.
4. Update DTO classes with `@Schema` annotations for new/changed fields.
5. Verify the live Swagger UI renders the updated contract correctly.
6. Update `docs/api-summary.md` table to match the new contract (method, endpoint, purpose, request/response summary).
7. If breaking, add an entry to `docs/engineering-decisions.md` with rationale and migration note.

## Output / deliverable
- Updated controller annotations and DTO `@Schema` annotations (code)
- Updated `docs/api-summary.md` (markdown table)
- If breaking: appended entry in `docs/engineering-decisions.md`

## Quality checks before returning output
- [ ] Every changed endpoint has updated `@Operation`/`@ApiResponse`/`@Parameter` annotations
- [ ] Every new/changed DTO field has `@Schema(description=…, example=…)` 
- [ ] Additive changes don't break existing clients (optional fields, new endpoints only)
- [ ] Breaking changes are explicitly flagged and documented in engineering-decisions.md
- [ ] `docs/api-summary.md` table matches the 9 (or 10) live endpoints exactly
- [ ] No Redis/caching assumptions in contract (not implemented)
- [ ] Swagger UI at `/swagger-ui.html` renders without errors

## Example
**Change:** Add optional `window` query param to GET /api/urls/{shortCode}/analytics
**Controller update (UrlController.java):**
```java
@GetMapping("/{shortCode}/analytics")
@Operation(summary = "Get analytics for a short URL", parameters = {
    @Parameter(name = "window", description = "Time window: 24h, 7d, 30d", schema = @Schema(type = "string", allowableValues = {"24h","7d","30d"}))
})
public ResponseEntity<UrlAnalyticsResponse> getAnalytics(
    @PathVariable String shortCode,
    @RequestParam(required = false) String window) { … }
```
**DTO update (UrlAnalyticsResponse.java):**
```java
@Schema(description = "Click count within the requested window")
private Long windowedClickCount;
```
**docs/api-summary.md update:**
| GET | /api/urls/{shortCode}/analytics | Retrieve analytics for a short URL (optional `window` query param: 24h, 7d, 30d) |
**Breaking?** No — additive optional param and new response field.