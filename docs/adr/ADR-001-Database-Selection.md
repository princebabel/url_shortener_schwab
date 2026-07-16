# ADR-001: Database Selection — H2 with PostgreSQL Compatibility Mode for Local Development

## Status
Accepted

## Context
The URL Shortener service requires a relational database for persisting shortened URLs, their metadata, and analytics data. The team needed a database solution that satisfies three competing constraints: zero-configuration local development, production-grade PostgreSQL compatibility, and minimal operational overhead for CI/CD pipelines.

## Decision
We will use **H2 Database in PostgreSQL compatibility mode** (`MODE=PostgreSQL`) for local development and testing, with a documented migration path to **PostgreSQL** for staging and production environments.

The `application.yml` configures the datasource as follows:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
```

## Rationale
1. **Zero-Configuration Developer Experience**: H2 runs in-memory with no external dependencies, enabling `./mvnw spring-boot:run` to work immediately on any machine with a JDK. No Docker, no local PostgreSQL installation, no port conflicts.

2. **PostgreSQL Compatibility Mode**: The `MODE=PostgreSQL` flag makes H2 emulate PostgreSQL behavior for:
   - Case-insensitive identifier handling (unquoted identifiers folded to lowercase)
   - `SERIAL`/`BIGSERIAL` type mapping to identity columns
   - `TEXT` type support without length limits
   - `RETURNING` clause support for INSERT statements
   - Standard-compliant `LIMIT`/`OFFSET` pagination syntax

3. **Schema Portability**: JPA/Hibernate DDL generation (`ddl-auto: create-drop`) produces identical schema structures on both H2 and PostgreSQL. The `UrlEntity` and `UrlLinkEntity` mappings use standard `@Entity`, `@Table`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` annotations that translate directly.

4. **CI/CD Efficiency**: GitHub Actions and similar runners execute tests against H2 in-memory in seconds, avoiding the 30-60 second startup penalty of spinning up a PostgreSQL container.

5. **Production Parity**: The migration path is explicit — changing the datasource URL, driver class, and dialect in `application-prod.yml` (or environment-specific configuration) is the only required change. No schema migration scripts are needed because Hibernate generates compatible DDL.

## Consequences
### Positive
- Instant local startup (< 3 seconds)
- Zero infrastructure dependencies for developers
- Fast test execution in CI
- Single schema definition via JPA entities
- Clear, documented path to production PostgreSQL

### Negative
- H2 does not perfectly replicate PostgreSQL's advanced features (partial indexes, materialized views, advisory locks, `pg_trgm` extension)
- Some PostgreSQL-specific functions (`gen_random_uuid()`, `now() AT TIME ZONE`) require vendor-specific Hibernate dialects or native queries
- Connection pooling behavior differs (H2 in-memory has no pool saturation scenarios)
- `MODE=PostgreSQL` does not guarantee 100% behavioral parity — integration tests against real PostgreSQL remain essential before production deployment

## Mitigations
- **Testcontainers Integration Tests**: A separate test profile (`@ActiveProfiles("integration")`) spins up a real PostgreSQL container for critical path validation
- **Dialect Abstraction**: All repository queries use Spring Data JPA method naming or `@Query` with JPQL, avoiding native SQL where possible
- **Documentation**: The `application-prod.yml.template` (to be created) documents the exact three-property change required for production

## Alternatives Considered
| Option | Rejected Because |
|--------|------------------|
| Pure H2 (default mode) | Schema incompatibility with PostgreSQL; `MODE=PostgreSQL` is a single property change |
| PostgreSQL in Docker for local dev | Adds 30s+ startup, port management, Docker Desktop dependency, CI complexity |
| SQLite | No PostgreSQL compatibility mode; type system differences cause subtle bugs |
| Testcontainers for all local runs | Too slow for inner-loop development; better reserved for CI/integration profile |

## Related Decisions
- ADR-002: Layered Architecture (repository layer abstracts database access)
- ADR-003: DTO Pattern (entities never exposed beyond repository layer)
- ADR-004: Global Exception Handling (database exceptions mapped to standard error responses)

## References
- `src/main/resources/application.yml` — H2 datasource configuration
- `src/main/java/com/schwab/urlshortener/entity/UrlEntity.java` — JPA entity mapping
- `src/main/java/com/schwab/urlshortener/entity/UrlLinkEntity.java` — JPA entity mapping
- Spring Boot Documentation: [Connecting to a Database](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.datasource)
- H2 Documentation: [PostgreSQL Compatibility Mode](https://h2database.com/html/features.html#compatibility_modes)