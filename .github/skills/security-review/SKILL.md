---
name: security-review
description: Reviews input validation, open-redirect/SSRF protection on 302 redirect flow (GET /api/urls/{shortCode}), PII leakage in analytics/logs, secrets in application.yml, and flags rate limiting as a gap (per Future Enhancements).
---

# Security Review

## Purpose
Systematically evaluates the codebase for security weaknesses specific to a URL shortener: open redirects on the 302 flow, SSRF via destination URLs, input validation gaps, PII exposure in logs/analytics, hardcoded secrets, and missing rate limiting. This skill exists because URL shorteners are high-value targets for phishing, malware distribution, and reconnaissance; the redirect endpoint is the primary attack surface.

## When to use
- Before any release or PR merge
- User says "security review", "check for vulnerabilities", "audit the redirect", "validate input"
- After adding/modifying the redirect endpoint (GET /api/urls/{shortCode})
- After changing validation on CreateUrlRequest
- After modifying logging or analytics DTOs
- When docs/future-enhancements.md mentions rate limiting (to confirm still a gap)

## Inputs this skill expects
- `UrlController.redirect()` — the 302 redirect flow (primary attack surface)
- `CreateUrlRequest` validation annotations (`@NotBlank`, `@Pattern`, `@Size`)
- `UrlEntity.originalUrl` — stored destination URL
- `GlobalExceptionHandler` — error response shapes (no stack traces in prod)
- `CorrelationIdFilter` (config/) — logging scope (correlation ID only, no PII)
- `application.yml` — datasource credentials (must be env vars, not hardcoded)
- `docs/future-enhancements.md` — confirms rate limiting is not implemented
- Swagger UI at `/swagger-ui.html` — verify no sensitive fields exposed in schemas

## Process
1. **Open Redirect / SSRF on Redirect (GET /api/urls/{shortCode})**:
   - Verify `UrlService.redirect()` validates the stored `originalUrl` against an allowlist or at minimum confirms it's HTTP/HTTPS (no `file:`, `ftp:`, `javascript:`, `data:` schemes).
   - Confirm no user-supplied input influences the redirect target at request time (target comes only from DB).
   - Check that `originalUrl` was validated on CREATE (POST /api/urls) via `@Pattern(regexp="^https?://.*")`.
   - Flag if redirect allows localhost/private IPs (SSRF risk) — recommend blocklist or allowlist.

2. **Input Validation (POST /api/urls)**:
   - Verify `CreateUrlRequest.originalUrl` has `@NotBlank`, `@Size(max=2048)`, `@Pattern(regexp="^https?://.*")`.
   - Verify `customAlias` has `@Size(min=3,max=64)`, `@Pattern(regexp="^[a-zA-Z0-9_-]+$")` (no script chars).
   - Confirm validation triggers 400 via `GlobalExceptionHandler` before service logic runs.

3. **PII / Data Leakage**:
   - Check `UrlAnalyticsResponse` and `DashboardSummaryResponse` — no user identifiers, emails, IPs, full URLs in analytics (only shortCode, clickCount, timestamps).
   - Check `CorrelationIdFilter` — logs only correlation ID, request path, method, status; no request bodies, no headers with auth tokens.
   - Verify `application.yml` uses `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` — no literals.

4. **Error Handling**:
   - Confirm `GlobalExceptionHandler` returns generic messages in production (no stack traces, no SQL errors).
   - Verify 404 for missing short code doesn't reveal enumeration info beyond "not found".

5. **Rate Limiting Gap**:
   - Confirm no `@RateLimiter` or `Bucket4j` or gateway-level limiting on POST /api/urls.
   - Note in review: "Rate limiting not implemented — tracked in docs/future-enhancements.md".

6. **Secrets Management**:
   - Verify no hardcoded JWT secrets, API keys, or DB passwords in any `.java`, `.yml`, `.properties`.
   - Confirm `application.yml` only has placeholders.

## Output / deliverable
A security review note appended to `docs/engineering-decisions.md` (or a dedicated `docs/security-review-<date>.md`) with sections: Redirect/SSRF, Input Validation, PII/Logging, Error Handling, Rate Limiting, Secrets, Overall Risk (Low/Medium/High), Action Items.

## Quality checks before returning output
- [ ] Redirect target validated on CREATE (not just on redirect)
- [ ] No user-controlled redirect parameter at request time
- [ ] SSRF mitigations considered (private IP blocklist or allowlist)
- [ ] Validation annotations present on all request DTO fields
- [ ] Analytics/logs contain no PII beyond correlation ID
- [ ] No hardcoded secrets in any config or code
- [ ] Rate limiting explicitly flagged as gap (matches future-enhancements.md)
- [ ] Error responses don't leak stack traces or SQL

## Example
**Review of GET /api/urls/{shortCode} redirect flow:**
```
## Security Review — 2025-01-15 — Redirect Flow & Input Validation

### Redirect / SSRF (GET /api/urls/{shortCode})
- **Finding:** `UrlService.redirect()` returns stored `originalUrl` directly for 302 Location header.
- **Mitigation in place:** `CreateUrlRequest.originalUrl` validated with `@Pattern(regexp="^https?://.*")` — blocks `javascript:`, `file:`, `data:` schemes.
- **Gap:** No blocklist for private IPs (10.x, 192.168.x, 127.0.0.1, 169.254.x) — SSRF risk if internal services are reachable.
- **Recommendation:** Add allowlist of permitted domains or blocklist of private CIDRs in `UrlValidator` before save.

### Input Validation (POST /api/urls)
- `originalUrl`: @NotBlank, @Size(max=2048), @Pattern("^https?://.*") ✓
- `customAlias`: @Size(3-64), @Pattern("^[a-zA-Z0-9_-]+$") ✓ (prevents XSS chars)
- Validation enforced by @Valid + GlobalExceptionHandler → 400 before service ✓

### PII / Logging
- UrlAnalyticsResponse: shortCode, clickCount, createdAt, lastAccessedAt — no PII ✓
- DashboardSummaryResponse: aggregates only — no PII ✓
- CorrelationIdFilter: logs correlationId, method, path, status — no bodies/headers ✓

### Secrets
- application.yml: ${DB_URL}, ${DB_USERNAME}, ${DB_PASSWORD} — no literals ✓

### Rate Limiting
- Not implemented — tracked in docs/future-enhancements.md ✓

### Overall Risk: Medium (SSRF via private IPs)
### Action Items:
1. Add private IP blocklist in UrlValidator before entity save
2. Consider allowlist for production deployment
3. Implement rate limiting on POST /api/urls (future enhancement)
```