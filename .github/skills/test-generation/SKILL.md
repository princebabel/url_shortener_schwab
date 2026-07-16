---
name: test-generation
description: Generates JUnit5 + Mockito unit tests and Testcontainers integration tests for Spring Boot 3 / Java 21 / PostgreSQL; explicitly targets the known test gap (README: "Unit Testing (to be expanded)") with edge cases: invalid URL, alias collision, expired/missing short code, concurrent creation.
---

# Test Generation

## Purpose
Fills the explicitly acknowledged test coverage gap (README lists "Unit Testing (to be expanded)") by producing both unit tests (JUnit5 + Mockito) for service logic and integration tests (Testcontainers + PostgreSQL) for repository and controller layers. This skill exists because the current codebase has minimal tests; every new feature and bug fix must add tests to prevent regressions.

## When to use
- After spring-boot-scaffolding creates new classes
- After any bug fix (to add regression test)
- User says "write tests for…", "add test coverage for…", "test the… endpoint"
- Before merging any change that touches service or controller logic
- When regression-test-generation pins current behavior

## Inputs this skill expects
- Classes to test: service impls (`UrlServiceImpl`, `UrlShortenerServiceImpl`), controllers (`UrlController`, `UrlShortenerController`), repositories (`UrlRepository`, `UrlLinkRepository`)
- Existing test: `src/test/java/com/schwab/urlshortener/UrlShortenerApplicationTests.java` (context load only)
- Test dependencies: `spring-boot-starter-test`, `testcontainers`, `postgresql` module
- Edge cases to cover (per README gap):
  - Invalid URL format (validation)
  - Alias collision (DuplicateAliasException)
  - Expired link (ExpiredUrlException)
  - Missing short code (ShortCodeNotFoundException)
  - Inactive link (InactiveUrlException)
  - Concurrent short code generation (IdGenerator thread-safety)
  - Pagination/sorting edge cases (empty page, oversized page, invalid sort field)
- `docs/task-decomposition.md` for acceptance criteria to test against

## Process
1. For each service method, write a **unit test** class (`*ServiceImplTest.java`) using `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks` service. Test happy path + every exception-throwing branch.
2. For each repository custom query, write an **integration test** (`*RepositoryIntegrationTest.java`) using `@SpringBootTest`, `@AutoConfigureTestDatabase(replace=Replace.NONE)`, Testcontainers PostgreSQL (`@Container static PostgreSQLContainer<?> postgres`). Verify query returns expected rows.
3. For each controller endpoint, write a **controller integration test** (`*ControllerIntegrationTest.java`) using `@SpringBootTest(webEnvironment=RANDOM_PORT)`, `TestRestTemplate` or `WebTestClient`. Test 2xx, 4xx, 5xx responses per endpoint.
4. For concurrent scenarios (IdGenerator), write a **concurrency test** using `ExecutorService` + `CountDownLatch` to verify uniqueness under load.
5. Place unit tests in `src/test/java/com/schwab/urlshortener/service/`, integration tests in `src/test/java/com/schwab/urlshortener/repository/` and `controller/`.
6. Run `mvn test` to verify all pass.

## Output / deliverable
- Unit test files: `src/test/java/com/schwab/urlshortener/service/*ServiceImplTest.java`
- Repository integration tests: `src/test/java/com/schwab/urlshortener/repository/*RepositoryIntegrationTest.java`
- Controller integration tests: `src/test/java/com/schwab/urlshortener/controller/*ControllerIntegrationTest.java`
- Concurrency test: `src/test/java/com/schwab/urlshortener/util/IdGeneratorConcurrencyTest.java`

## Quality checks before returning output
- [ ] Every service method has a unit test covering happy path + each custom exception
- [ ] Every repository custom query has an integration test with Testcontainers
- [ ] Every controller endpoint has integration tests for 200, 400, 404, 409, 500 as applicable
- [ ] Edge cases explicitly covered: invalid URL, alias collision, expired, missing, inactive, concurrent
- [ ] Tests use `@DirtiesContext` or unique test data to avoid interference
- [ ] No test depends on Redis/caching (not implemented)
- [ ] `mvn test` passes with zero failures
- [ ] Test names follow `methodName_condition_expectedResult` pattern

## Example
**Unit test for UrlServiceImpl.createUrl (alias collision):**
```java
@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {
    @Mock UrlRepository urlRepository;
    @Mock IdGenerator idGenerator;
    @InjectMocks UrlServiceImpl urlService;

    @Test
    void createUrl_whenCustomAliasExists_throwsDuplicateAliasException() {
        // given
        CreateUrlRequest request = CreateUrlRequest.builder()
            .originalUrl("https://example.com")
            .customAlias("my-alias")
            .build();
        when(urlRepository.existsByCustomAlias("my-alias")).thenReturn(true);

        // when/then
        assertThatThrownBy(() -> urlService.createUrl(request))
            .isInstanceOf(DuplicateAliasException.class)
            .hasMessageContaining("my-alias");
        verify(urlRepository).existsByCustomAlias("my-alias");
        verifyNoMoreInteractions(urlRepository, idGenerator);
    }

    @Test
    void createUrl_whenValid_returnsResponse() {
        // given
        CreateUrlRequest request = CreateUrlRequest.builder()
            .originalUrl("https://example.com")
            .build();
        when(idGenerator.generate()).thenReturn("abc123");
        when(urlRepository.save(any(UrlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        UrlResponse response = urlService.createUrl(request);

        // then
        assertThat(response.getShortCode()).isEqualTo("abc123");
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com");
        verify(urlRepository).save(argThat(e -> e.getShortCode().equals("abc123")));
    }
}
```
**Integration test for GET /api/urls/{shortCode}/analytics:**
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class UrlControllerIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("urlshortener")
        .withUsername("test")
        .withPassword("test");

    @Autowired TestRestTemplate restTemplate;
    @Autowired UrlRepository urlRepository;

    @Test
    void getAnalytics_whenShortCodeExists_returns200WithClickCount() {
        // given
        UrlEntity saved = urlRepository.save(UrlEntity.builder()
            .shortCode("test123")
            .originalUrl("https://example.com")
            .clickCount(5L)
            .active(true)
            .createdAt(Instant.now())
            .build());

        // when
        ResponseEntity<UrlAnalyticsResponse> resp = restTemplate.getForEntity(
            "/api/urls/test123/analytics", UrlAnalyticsResponse.class);

        // then
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getClickCount()).isEqualTo(5L);
    }
}
```