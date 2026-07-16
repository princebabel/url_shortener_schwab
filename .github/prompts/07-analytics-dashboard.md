# Prompt 07 - Analytics API & Operational Dashboard

You are a Principal Software Engineer at Charles Schwab.

## Context

Read the following before implementation:

- docs/requirement-analysis.md
- docs/task-decomposition.md
- docs/architecture.md

Review the existing project.

The project currently compiles successfully.

Do not break existing functionality.

---

# Objective

Implement production-grade Analytics APIs for the URL Shortener.

This feature should provide operational visibility similar to what an SRE or Operations Engineer would expect.

---

# Feature 1

Implement

GET /api/urls/{shortCode}/analytics

Return

```json
{
  "shortCode":"abc123",
  "originalUrl":"https://google.com",
  "clickCount":125,
  "createdAt":"...",
  "expiryDate":"...",
  "lastAccessedAt":"...",
  "active":true
}
```

---

# Feature 2

Implement

GET /api/urls

Return all shortened URLs.

Support pagination.

Support sorting.

Support filtering:

- active
- expired

---

# Feature 3

Implement

GET /api/dashboard/summary

Return

```json
{
  "totalUrls":1200,
  "activeUrls":1150,
  "expiredUrls":50,
  "totalClicks":65000,
  "topShortCode":"abc123"
}
```

---

# Feature 4

Top 10 URLs

GET

/api/dashboard/top

Return Top 10 URLs ordered by click count.

---

# Feature 5

Recent URLs

GET

/api/dashboard/recent

Return latest shortened URLs.

---

# Feature 6

Search

GET

/api/urls/search

Support

- shortCode
- originalUrl
- customAlias

Allow partial matching.

---

# Small Production Enhancements

Enhance analytics by storing:

- lastAccessedAt
- lastAccessedIp
- lastUserAgent

Populate these values during redirect requests.

---

# Optional Metrics (Nice to Have)

Create service methods for:

- Average clicks per URL
- Expired percentage
- Active percentage

Expose them in the summary response if straightforward.

---

# Repository

Add efficient repository queries.

Use JPQL or derived queries where appropriate.

Avoid N+1 queries.

---

# Logging

Log:

- Analytics requests
- Dashboard requests
- Search requests

Use INFO level.

Log failures at ERROR level.

---

# Validation

Validate:

- shortCode exists

Return proper HTTP responses.

---

# Code Quality

Follow:

- Java 21
- Spring Boot 3
- SOLID
- Constructor Injection
- Clean Architecture
- Lombok

---

# Important

Modify existing files where appropriate.

Create only necessary new files.

Keep the project compiling successfully.

At completion provide:

- APIs implemented
- Repository methods added
- DTOs created
- Files modified
- Files created