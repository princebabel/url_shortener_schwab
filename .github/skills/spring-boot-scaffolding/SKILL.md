---
name: spring-boot-scaffolding
description: Generates controller/service/repository/entity/dto classes matching exact package conventions (controller/service/repository/entity/dto/exception/config), Lombok + constructor injection, and centralized exception handling via exception/GlobalExceptionHandler.
---

# Spring Boot Scaffolding

## Purpose
Produces new Spring Boot components that conform exactly to this project's established conventions: package structure, Lombok annotations, constructor injection, DTO pattern, and centralized exception handling. This skill exists because inconsistent layering, field injection, or ad-hoc try/catch blocks create maintenance debt and break the uniform architecture.

## When to use
- Adding a new endpoint (requires controller + service + repository + entity + dto)
- Adding a new entity/table (requires entity + repository + migration)
- User says "scaffold a new…", "generate the boilerplate for…", "create the layers for…"
- After task-decomposition identifies the concrete classes needed

## Inputs this skill expects
- Task list from `docs/task-decomposition.md` (specific classes to create)
- Existing package conventions:
  - `com.schwab.urlshortener.controller` — REST controllers, `@RestController`, `@RequestMapping`, `@Valid`, `@Tag`
  - `com.schwab.urlshortener.service` — interfaces + `*Impl` classes, `@Service`, `@Transactional`
  - `com.schwab.urlshortener.repository` — interfaces extending `JpaRepository`, custom `@Query` methods
  - `com.schwab.urlshortener.entity` — `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Index`
  - `com.schwab.urlshortener.dto` — request/response records or Lombok `@Data`/`@Builder` classes, `@Schema`
  - `com.schwab.urlshortener.exception` — custom exceptions extending `RuntimeException`, handled by `GlobalExceptionHandler`
  - `com.schwab.urlshortener.config` — `@Configuration` classes (OpenAPI, logging, correlation ID)
- Lombok annotations in use: `@RequiredArgsConstructor`, `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Constructor injection everywhere (no `@Autowired` on fields)
- PostgreSQL via Spring Data JPA (Hibernate dialect in application.yml)
- No Redis/caching layer (future enhancement only)

## Process
1. Read the task list to identify each class to create (entity, repository, dto, service, controller).
2. Generate the **entity** first: `@Entity`, `@Table(name="…")`, `@Id`, `@GeneratedValue`, fields with `@Column`, indexes via `@Table(indexes=…)`, Lombok `@Data`/`@Builder`/`@NoArgsConstructor`/`@AllArgsConstructor`.
3. Generate the **repository**: interface extending `JpaRepository<Entity, Long>`, custom query methods named `findBy…` or `@Query` for complex reads.
4. Generate **DTOs**: request DTOs with `@NotBlank`, `@Size`, `@Pattern` validation; response DTOs with `@Schema`; use records or `@Data`/`@Builder`.
5. Generate **service interface** + **impl**: interface in `service/`, impl in `service/` (or `service/impl/` if that convention emerges), `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly=true)` for queries, `@Transactional` for writes. Delegate to repository. Throw custom exceptions from `exception/` (e.g., `ShortCodeNotFoundException`, `DuplicateAliasException`).
6. Generate **controller**: `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`, `@Valid` on request body, `@Tag` for Swagger grouping, delegate to service interface. Return `ResponseEntity<Dto>` with proper status codes. **Never** catch exceptions — let `GlobalExceptionHandler` handle them.
7. If new exception types needed: create in `exception/`, extend `RuntimeException`, add `@ExceptionHandler` in `GlobalExceptionHandler`.
8. Verify compilation: `mvn clean compile`.

## Output / deliverable
- New `.java` files in the correct packages under `src/main/java/com/schwab/urlshortener/`
- All classes compile and follow conventions

## Quality checks before returning output
- [ ] Package names exactly match: controller, service, repository, entity, dto, exception, config
- [ ] Lombok `@RequiredArgsConstructor` on all Spring components (controller, service, repository not needed)
- [ ] Constructor injection only — no field `@Autowired`
- [ ] Entities use `@Builder`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- [ ] DTOs use validation annotations (`@NotBlank`, `@Size`, `@Pattern`, `@Valid`)
- [ ] Service methods throw custom exceptions from `exception/` — no raw `throw new RuntimeException()`
- [ ] Controllers have zero try/catch — all errors flow to `GlobalExceptionHandler`
- [ ] Swagger annotations (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`) present
- [ ] No Redis/caching references
- [ ] `mvn clean compile` succeeds

## Example
**Task:** Scaffold "Create URL with custom alias" (already exists, but shows pattern)
**Entity (UrlEntity.java):**
```java
@Entity
@Table(name = "urls", indexes = @Index(name = "idx_short_code", columnList = "shortCode", unique = true))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UrlEntity {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false, unique = true, length = 16) private String shortCode;
    @Column(nullable = false, length = 2048) private String originalUrl;
    @Column(length = 64) private String customAlias;
    @Column(nullable = false) private Long clickCount = 0L;
    @Column(nullable = false) private Boolean active = true;
    @Column(nullable = false) private Instant createdAt;
    private Instant expiresAt;
}
```
**Repository (UrlRepository.java):**
```java
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByShortCode(String shortCode);
    Optional<UrlEntity> findByCustomAlias(String customAlias);
    boolean existsByShortCode(String shortCode);
}
```
**DTO (CreateUrlRequest.java):**
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateUrlRequest {
    @NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String originalUrl;
    @Size(min = 3, max = 64) @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    private String customAlias;
}
```
**Service (UrlServiceImpl.java):**
```java
@Service @RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepository;
    private final IdGenerator idGenerator;

    @Override @Transactional
    public UrlResponse createUrl(CreateUrlRequest request) {
        if (request.getCustomAlias() != null && urlRepository.existsByCustomAlias(request.getCustomAlias())) {
            throw new DuplicateAliasException("Alias already in use: " + request.getCustomAlias());
        }
        String shortCode = request.getCustomAlias() != null ? request.getCustomAlias() : idGenerator.generate();
        UrlEntity entity = UrlEntity.builder()
            .shortCode(shortCode)
            .originalUrl(request.getOriginalUrl())
            .customAlias(request.getCustomAlias())
            .createdAt(Instant.now())
            .build();
        return toResponse(urlRepository.save(entity));
    }
    // …
}
```
**Controller (UrlController.java):**
```java
@RestController @RequestMapping("/api/urls") @RequiredArgsConstructor
@Tag(name = "URLs", description = "Create, manage, and resolve short URLs")
public class UrlController {
    private final UrlService urlService;

    @PostMapping
    @Operation(summary = "Create a short URL")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<UrlResponse> createUrl(@Valid @RequestBody CreateUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.createUrl(request));
    }
    // …
}
```