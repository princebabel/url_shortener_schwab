# Prompt 06 - URL Redirection & Click Tracking

You are a Principal Software Engineer at Charles Schwab.

## Context

Read:

- docs/requirement-analysis.md
- docs/task-decomposition.md
- docs/architecture.md

Review the existing implementation before making changes.

The project currently compiles successfully.

Do not modify existing APIs unless required.

---

## Objective

Implement URL redirection and click tracking.

---

## Functional Requirements

Implement:

GET /{shortCode}

Behavior:

1. Look up the URL using the short code.
2. Return HTTP 404 if not found.
3. Return HTTP 410 if expired.
4. Return HTTP 410 if inactive.
5. Increment click count atomically.
6. Update last accessed timestamp.
7. Log the access.
8. Return HTTP 302 (Found) redirecting to the original URL.

---

## Analytics

Capture:

- clickCount
- lastAccessedAt

Update these values whenever a redirect succeeds.

---

## Repository

Add methods as required.

Optimize queries where appropriate.

---

## Service

Implement redirect business logic.

Validate:

- Short code exists
- URL is active
- URL is not expired

Increment analytics safely.

---

## Controller

Create:

GET /{shortCode}

Return:

302 Found

using Spring's RedirectView or ResponseEntity with Location header.

---

## Exception Handling

Handle:

- Short code not found
- Expired URL
- Inactive URL

Return meaningful responses.

---

## Logging

Log:

- Redirect request
- Short code
- Destination URL
- Failure reason

---

## Code Quality

Use:

- Constructor Injection
- SOLID principles
- Java 21
- Spring Boot 3
- Clean Architecture
- Production-quality code

---

## Important

Modify existing files where required.

Create new files only if necessary.

Keep the project compiling.

At completion provide a summary of:

- Files modified
- Files created
- APIs implemented