# Production-Ready URL Shortener Service

This repository contains a Spring Boot 3 / Java 21 URL shortener backend with create, redirect, analytics, dashboard, validation, and health endpoints. The implementation is currently verified by a successful Maven build and a generated JaCoCo report.

## Current Status

- Build status: verified with Maven verify
- JaCoCo line coverage: 73.39% from the current generated report
- Test suite: 12 test files under src/test/java
- Static analysis: **configured** (SpotBugs, PMD, Checkstyle)
- Dependency vulnerability scanning: **configured** (OWASP Dependency Check)
- Authentication and rate limiting: not implemented yet

## What Is Implemented

- URL creation with optional custom alias and expiry configuration
- Redirect handling for short codes
- Analytics and dashboard summary endpoints
- Search and pagination support for URL listings
- Bean Validation on the create request DTO
- Centralized exception handling with consistent error responses
- Structured logging and correlation-ID support
- OpenAPI / Swagger documentation
- Health endpoint at /api/v1/health

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Data Access | Spring Data JPA |
| Validation | Spring Validation |
| Database | H2 (default), PostgreSQL-compatible configuration available |
| API Documentation | springdoc-openapi / Swagger UI |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |
| Monitoring | Spring Boot Actuator |
| Testing | Spring Boot Test, JUnit 5, AssertJ |

## Project Structure

```text
src/
  main/
    java/com/schwab/urlshortener/
      config/            # OpenAPI, logging, correlation-ID filter
      controller/        # REST controllers
      dto/               # Request and response models
      entity/            # JPA entities
      exception/         # Centralized exception handling
      mapper/            # Mapping helpers
      repository/        # Spring Data repositories
      service/           # Business logic and orchestration
      util/              # Utility helpers
      validation/        # URL validation logic
    resources/
      application.yml   # Runtime and environment configuration
  test/
    java/com/schwab/urlshortener/  # Unit and integration-style tests
```

## Build and Run

### Prerequisites

- Java 21
- Maven

### Commands

```bash
mvn clean verify
mvn spring-boot:run
```

The application runs on port 8081 by default.

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/urls | Create a shortened URL |
| GET | /api/urls | List and filter URLs |
| GET | /api/urls/{shortCode} | Redirect to the original destination |
| GET | /api/urls/{shortCode}/analytics | Retrieve analytics for a short URL |
| GET | /api/urls/search | Search URLs by short code, original URL, or custom alias |
| GET | /api/urls/dashboard/summary | Return dashboard summary metrics |
| GET | /api/urls/dashboard/recent | Return the most recently created URLs |
| GET | /api/urls/dashboard/top | Return the most clicked URLs |
| GET | /api/v1/health | Return health information |

## Example Requests

### Create a URL

```bash
curl -X POST http://localhost:8081/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com","customAlias":"demo"}'
```

### Redirect a Short Code

```bash
curl -i http://localhost:8081/api/urls/demo
```

### Retrieve Analytics

```bash
curl http://localhost:8081/api/urls/demo/analytics
```

### Retrieve Dashboard Summary

```bash
curl http://localhost:8081/api/urls/dashboard/summary
```

## Documentation and UI

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs
- Setup guide for new engineers: docs/setup-guide.md

## Validation and Error Handling

The create endpoint uses Bean Validation through @Valid on the request DTO. The validation rules include non-blank input, URL format checks, size constraints, and minimum expiry values. Errors are handled centrally and returned as structured error responses with status, message, and path information.

## Testing and Quality

The repository includes tests for controllers, services, validation, exception handling, config, mapper, and utility behavior. The current verified JaCoCo report shows 73.39% line coverage.

**Static Analysis & Quality Tools Configured:**
- **SpotBugs** - Static analysis for bug patterns
- **PMD** - Code quality and best practices analysis
- **Checkstyle** - Code style enforcement (Google checks)
- **OWASP Dependency Check** - Vulnerability scanning for dependencies (CVSS ≥ 7 fails build)

Run all quality checks with:
```bash
mvn verify
```

Current quality-related gaps:
- No load/performance test suite is present yet

## Security and Governance Notes

- No hardcoded secrets were found in application.yml; the configuration uses environment-variable placeholders.
- Authentication and rate limiting are not implemented in the current prototype.
- The project documentation and governance artifacts are maintained under docs/.

## Future Enhancements

The next practical improvements are:

- Redis caching for redirect lookups
- PostgreSQL as the primary runtime database in non-local environments
- Kafka-based event streaming for analytics and audit events
- Docker and Kubernetes deployment support
- JWT-based authentication and role-based access control
- Rate limiting and abuse protection
- Micrometer, Prometheus, and Grafana integration
- CI/CD automation and deployment pipelines

# 19. AI Assisted Development

GitHub Copilot Agent was used as an engineering accelerator throughout the development lifecycle. It supported requirement analysis, architecture review, implementation scaffolding, refactoring, and documentation drafting. All AI-generated outputs were reviewed, adjusted where necessary, and validated by the engineer before being retained in the repository.

# 20. Screenshots

The following placeholders can be used to capture and document the live UI and API experience:

- Swagger Home: docs/images/swagger-home.png
- Create URL API: docs/images/create-url-api.png
- Analytics API: docs/images/analytics-api.png
- Dashboard API: docs/images/dashboard-api.png
- Health API: docs/images/health-api.png


# 21. Conclusion

This project demonstrates a clean, maintainable, and enterprise-oriented approach to building a URL shortener service. It combines a clear layered architecture, strong API validation, operational logging, and AI-assisted development practices into a concise and reviewable implementation. The result is a backend solution that is suitable for technical assessment, further hardening, and future expansion into a larger production platform.
