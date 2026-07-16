# ADR-002: Layered Architecture — Four-Layer Controller → Service → Repository → Database

## Status
Accepted

## Context
The URL Shortener service requires a maintainable, testable, and scalable codebase. The team needed an architectural pattern that enforces separation of concerns, enables independent testing of each layer, and aligns with Spring Boot conventions while avoiding the "anemic domain model" anti-pattern where business logic leaks into controllers or repositories.

## Decision
We adopt a **strict four-layer architecture** with unidirectional dependencies:

```
┌─────────────────────────────────────────────────────────────┐
│  Controller Layer (com.schwab.urlshortener.controller)      │
│  UrlController, UrlShortenerController                      │
│  → Handles HTTP, validation, serialization, status codes    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (depends on interfaces)
┌─────────────────────────────────────────────────────────────┐
│  Service Layer (com.schwab.urlshortener.service)            │
│  UrlService, UrlShortenerService                            │
│  → Business logic, validation orchestration, transactions   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (depends on interfaces)
┌─────────────────────────────────────────────────────────────┐
│  Repository Layer (com.schwab.urlshortener.repository)      │
│  UrlRepository, UrlLinkRepository (extend JpaRepository)    │
│  → Data access, query derivation, pagination                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (depends on JPA)
┌─────────────────────────────────────────────────────────────┐
│  Database Layer (com.schwab.urlshortener.entity)            │
│  UrlEntity, UrlLinkEntity                                   │
│  → JPA mappings, constraints, relationships                 │
└─────────────────────────────────────────────────────────────┘
```

**Dependency Rule**: Each layer depends **only** on the layer immediately below it. No layer skips. Controllers never call repositories directly. Services never handle HTTP. Repositories never contain business logic.

## Implementation Conventions

### Controller Layer
- **Package**: `com.schwab.urlshortener.controller`
- **Classes**: `UrlController` (main API), `UrlShortenerController` (health endpoint)
- **Responsibilities**:
  - HTTP request/response mapping via `@RestController`
  - Request validation via `@Valid` on DTO parameters
  - Delegation to service interfaces (constructor-injected)
  - HTTP status code selection (`ResponseEntity`)
  - OpenAPI documentation via springdoc annotations (`@Operation`, `@ApiResponses`)
- **Prohibited**: Business logic, database queries, transaction management, entity manipulation

### Service Layer
- **Package**: `com.schwab.urlshortener.service`
- **Classes**: `UrlService` (interface), `UrlServiceImpl` (implementation), `UrlShortenerService` (health)
- **Responsibilities**:
  - Business rule enforcement (alias uniqueness, URL validity, expiration)
  - Transaction boundary management (`@Transactional`)
  - Orchestration of multiple repository calls
  - Domain event publishing (future extensibility)
  - Mapping between DTOs and entities (via mappers or manual)
- **Prohibited**: HTTP concepts (`HttpServletRequest`, `ResponseEntity`), JPA criteria queries, DDL operations

### Repository Layer
- **Package**: `com.schwab.urlshortener.repository`
- **Interfaces**: `UrlRepository extends JpaRepository<UrlEntity, Long>`, `UrlLinkRepository extends JpaRepository<UrlLinkEntity, Long>`
- **Responsibilities**:
  - CRUD operations via Spring Data JPA inheritance
  - Custom query methods (`findByShortCode`, `findByAlias`, `existsByShortCode`)
  - Pagination and sorting via `Pageable`
  - No business logic, no DTOs, no HTTP concepts
- **Prohibited**: `@Transactional` (managed by service), entity-to-DTO mapping, validation logic

### Entity Layer
- **Package**: `com.schwab.urlshortener.entity`
- **Classes**: `UrlEntity`, `UrlLinkEntity`
- **Responsibilities**:
  - JPA mapping annotations (`@Entity`, `@Table`, `@Id`, `@Column`, `@OneToMany`)
  - Database constraints (`@UniqueConstraint`, `@Index`)
  - Relationship mapping (`@ManyToOne`, `@JoinColumn`)
  - Lifecycle callbacks (`@PrePersist`, `@PreUpdate`) for audit fields only
- **Prohibited**: Business logic, validation annotations (Bean Validation lives on DTOs), service/repository references

## Cross-Cutting Concerns (Outside Layers)
- **Configuration** (`config/`): `OpenApiConfig`, `LoggingConfig`, `CorrelationIdFilter`
- **Exception Handling** (`exception/`): `GlobalExceptionHandler` (`@ControllerAdvice`)
- **DTOs** (`dto/`): Request/response contracts, validation annotations
- **Mappers** (`mapper/`): Entity↔DTO conversion (MapStruct or manual)
- **Utilities** (`util/`): `ShortCodeGenerator`, `UrlValidator`
- **Validation** (`validation/`): Custom validators (`@ValidUrl`)

## Rationale
1. **Testability**: Each layer can be unit-tested in isolation with mocks. Controllers tested with `@WebMvcTest` + mocked services. Services tested with `@ExtendWith(MockitoExtension)` + mocked repositories. Repositories tested with `@DataJpaTest` + Testcontainers.
2. **Replaceability**: Repository implementations can swap (JPA → R2DBC → JDBC) without touching services. Controllers can swap (REST → GraphQL → gRPC) without touching services.
3. **Team Parallelism**: Frontend/backend teams can work against controller contracts. Database team owns entities/repositories. Business logic team owns services.
4. **Spring Boot Alignment**: Matches Spring's `@Controller`/`@Service`/`@Repository` stereotype semantics and component scanning defaults.
5. **Onboarding Clarity**: New developers find code by responsibility: "Where is URL validation?" → `UrlValidator` (util). "Where is alias uniqueness enforced?" → `UrlServiceImpl.createShortUrl()` (service).

## Consequences
### Positive
- Clear ownership boundaries
- High unit test coverage achievable (current gap documented in README)
- Easy to introduce cross-cutting concerns (caching, metrics) at service layer
- Migration to modular monolith or microservices follows natural boundaries

### Negative
- **Boilerplate**: DTO→Entity→DTO mapping at controller/service boundaries
- **Anemic Entities**: Entities are data carriers; behavior lives in services (intentional, per Spring conventions)
- **Layer Leakage Risk**: Developers may bypass layers under time pressure (mitigated by code review, ArchUnit tests — future enhancement)

## Mitigations
- **MapStruct Mappers**: Reduce mapping boilerplate (configured in `pom.xml`, generated at compile time)
- **ArchUnit Tests** (planned): Enforce layer dependencies automatically in CI
- **Code Review Checklist**: "Does this controller call a repository?" → reject

## Alternatives Considered
| Architecture | Rejected Because |
|--------------|------------------|
| Three-layer (Controller/Service/DAO) | DAO conflates repository and entity concerns; Spring Data JPA makes repository a first-class concept |
| Hexagonal/Ports & Adapters | Over-engineering for current scope; Spring Boot's inversion of control already provides port-like boundaries |
| Modular Monolith (Spring Modulith) | Premature; current package structure supports future modularization without framework overhead |
| Active Record (entities with save()) | Couples domain to persistence; violates Spring Data JPA philosophy; hard to test |

## Related Decisions
- ADR-001: Database Selection (repository layer abstracts H2/PostgreSQL)
- ADR-003: DTO Pattern (controllers never expose entities)
- ADR-004: Global Exception Handling (service exceptions mapped at controller boundary)
- ADR-005: Validation Strategy (validation at DTO/controller boundary, not entity)

## References
- `src/main/java/com/schwab/urlshortener/controller/UrlController.java`
- `src/main/java/com/schwab/urlshortener/service/UrlService.java`
- `src/main/java/com/schwab/urlshortener/service/UrlServiceImpl.java`
- `src/main/java/com/schwab/urlshortener/repository/UrlRepository.java`
- `src/main/java/com/schwab/urlshortener/entity/UrlEntity.java`
- Spring Boot Documentation: [Organizing Code](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.organizing-your-code)
- Martin Fowler: [Layered Architecture](https://martinfowler.com/bliki/PresentationDomainDataLayering.html)