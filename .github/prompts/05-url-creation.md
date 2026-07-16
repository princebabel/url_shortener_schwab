You are a Senior Java Engineer and Spring Boot Architect.

Read:

docs/requirement-analysis.md

docs/architecture.md

Implement Feature 1: URL Creation.

Generate production-ready Spring Boot code.

Create the following:

1. URL Entity
- id
- originalUrl
- shortCode
- customAlias
- createdAt
- expiryDate
- clickCount
- active

2. DTOs
- CreateUrlRequest
- UrlResponse

3. Repository

4. Service Interface

5. Service Implementation

Requirements:

- Validate URL format
- Generate unique short code
- If custom alias exists, validate uniqueness
- Calculate expiry date
- Initialize click count to zero

6. REST Controller

POST /api/urls

Return

201 Created

with UrlResponse

7. Validation

Use Bean Validation annotations.

8. Exception Handling

Handle

Invalid URL

Duplicate Alias

Database Error

Generate complete production-ready code.

Follow clean code principles.

Use constructor injection.

Use Lombok.

Create files directly in the workspace.