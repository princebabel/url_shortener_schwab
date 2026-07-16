---
name: refactor-safety-check
description: Performs a diff-level review of proposed changes against existing layered-architecture conventions (controller/service/repository/entity/dto/exception/config), Lombok + constructor injection style, and the 9 live endpoint contracts — flags any accidental contract or behavior drift.
---

# Refactor Safety Check

## Purpose
Acts as a final gate before merge: compares proposed code changes against the project's established architectural conventions and the live API contract (9 endpoints via Swagger UI). This skill exists because refactors often unintentionally change response shapes, status codes, validation rules, or layer boundaries — breaking clients or creating inconsistency.

## When to use
- Before merging any PR that modifies existing classes
- User says "review this refactor", "check for breaking changes", "validate the diff"
- After spring-boot-scaffolding or manual edits to controller, service, repository, entity, dto, exception, config
- When codebase-impact-analysis shows Medium/High risk

## Inputs this skill expects
- The diff / proposed changes (from PR or local edits)
- Current conventions:
  - Package layers: controller → service → repository → entity (no skipping, no reverse deps)
  - Lombok: `@RequiredArgsConstructor` on Spring beans, `@Data`/`@Builder` on DTOs/entities
  - Constructor injection only (no field `@Autowired`)
  - Validation: `@Valid` on controller request bodies, annotations on DTO fields
  - Exceptions: custom exceptions in `exception/`, handled only in `GlobalExceptionHandler`
  - Controllers: zero try/catch, delegate to service interface
  - Swagger: `@Tag` on class, `@Operation`/`@ApiResponse`/`@Parameter`/`@Schema` on methods/DTOs
- Live API contract: 9 endpoints at `/swagger-ui.html`
- `docs/api-summary.md` for endpoint summary
- `docs/engineering-decisions.md` for prior contract decisions

## Process
1. **Layer Boundary Check**: Verify no controller calls repository directly; no service calls controller; no entity leaks into controller responses (DTOs used).
2. **Injection Style Check**: All Spring beans (`@RestController`, `@Service`, `@Component`) use `@RequiredArgsConstructor` + `final` fields. No `@Autowired` on fields.
3. **DTO/Entity Separation**: Controllers return DTOs (from `dto/`), never entities. Entities never leave service layer.
4. **Validation Check**: Request DTOs have `@NotBlank`, `@Size`, `@Pattern`, etc. Controller has `@Valid`. No manual validation in service.
5. **Exception Handling Check**: Services throw custom exceptions from `exception/`. Controllers have no try/catch. `GlobalExceptionHandler` has `@ExceptionHandler` for each custom exception.
6. **Contract Drift Check**: For each of the 9 endpoints touched by the diff:
   - HTTP method unchanged?
   - Path unchanged?
   - Request body fields: no required field removed, no type changed, no validation relaxed?
   - Response body fields: no field removed, no type changed?
   - Status codes: 200/201/302/400/404/409/500 unchanged?
   - Query params: no required param added, no param removed?
7. **Swagger Annotation Check**: Changed endpoints have updated `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`.
8. **Lombok/Builder Check**: New DTOs/entities use `@Builder`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`.
9. **Config/Infra Check**: No changes to `CorrelationIdFilter`, `OpenApiConfig`, `GlobalExceptionHandler` unless intentional and documented.

## Output / deliverable
A safety check report (markdown) printed to conversation with: **PASS/FAIL** per check, list of any **FAIL** items with file:line references, and **Overall: SAFE / UNSAFE — DO NOT MERGE**.

## Quality checks before returning output
- [ ] All 9 endpoint contracts explicitly verified against diff
- [ ] Layer boundaries verified (no controller→repository, no entity in controller)
- [ ] Injection style verified (constructor only)
- [ ] Validation annotations present on all request DTOs
- [ ] Exception flow verified (service throws custom, controller no try/catch, handler catches)
- [ ] Swagger annotations updated for changed endpoints
- [ ] No Redis/caching references introduced
- [ ] Overall verdict is binary: SAFE or UNSAFE

## Example
**Diff:** PR modifies `UrlServiceImpl.redirect()` to return `UrlResponse` instead of `String` (target URL)
```
## Refactor Safety Check — PR #42

### Layer Boundaries
✅ PASS — Controller calls service interface; service calls repository; entity not exposed

### Injection Style
✅ PASS — @RequiredArgsConstructor on UrlServiceImpl, UrlController

### DTO/Entity Separation
❌ FAIL — UrlController.redirect() now returns UrlResponse (DTO) but HTTP 302 requires Location header with String URL.
   File: UrlController.java:35 — return type changed from ResponseEntity<Void> to ResponseEntity<UrlResponse>
   Impact: Breaks 302 redirect contract; clients expect Location header, not JSON body.

### Validation
✅ PASS — CreateUrlRequest unchanged

### Exception Handling
✅ PASS — Service throws ShortCodeNotFoundException, InactiveUrlException; GlobalExceptionHandler handles

### Contract Drift (9 endpoints)
| Endpoint | Method | Path | Request | Response | Status | Params | Verdict |
|----------|--------|------|---------|----------|--------|--------|---------|
| POST /api/urls | POST | /api/urls | ✅ | ✅ | ✅ | ✅ | PASS |
| GET /api/urls/{shortCode} | GET | /api/urls/{shortCode} | — | ❌ String→DTO | ❌ 302→200? | — | **FAIL** |
| GET /api/urls/{shortCode}/analytics | GET | /api/urls/{shortCode}/analytics | — | ✅ | ✅ | ✅ | PASS |
| GET /api/urls | GET | /api/urls | — | ✅ | ✅ | ✅ | PASS |
| GET /api/urls/search | GET | /api/urls/search | — | ✅ | ✅ | ✅ | PASS |
| GET /api/dashboard/summary | GET | /api/dashboard/summary | — | ✅ | ✅ | — | PASS |
| GET /api/dashboard/top | GET | /api/dashboard/top | — | ✅ | ✅ | — | PASS |
| GET /api/dashboard/recent | GET | /api/dashboard/recent | — | ✅ | ✅ | — | PASS |
| GET /api/v1/health | GET | /api/v1/health | — | ✅ | ✅ | — | PASS |

### Swagger Annotations
⚠️ PARTIAL — UrlController.redirect() missing updated @ApiResponse for new 200/DTO response

### Lombok/Builder
✅ PASS — No new DTOs/entities

### Config/Infra
✅ PASS — No changes to GlobalExceptionHandler, CorrelationIdFilter, OpenApiConfig

### Overall: UNSAFE — DO NOT MERGE
**Blocking Issue:** GET /api/urls/{shortCode} contract broken — 302 redirect with Location header changed to 200 JSON response. This breaks all existing clients (browsers, HTTP clients). Revert return type to ResponseEntity<Void> with Location header.
```