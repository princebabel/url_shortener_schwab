---
name: regression-test-generation
description: For brownfield changes, writes tests that pin current behavior of affected controller/service methods before the change, then verifies they still pass (or are deliberately updated) after — partially closing the "tests to be expanded" gap noted in README.
---

# Regression Test Generation

## Purpose
Creates a safety net for brownfield changes by capturing the *current* behavior of affected code as executable tests before any modification. This skill exists because the README explicitly acknowledges "Unit Testing (to be expanded)" as a known gap; every change is an opportunity to add coverage and prove no unintended regression.

## When to use
- Immediately after codebase-impact-analysis identifies affected endpoints/methods
- Before any modification to existing service, controller, or repository code
- User says "add regression tests for…", "pin current behavior of…", "test before refactor"
- When a bug fix is applied (to prevent reoccurrence)

## Inputs this skill expects
- Impact map from `codebase-impact-analysis` (endpoints affected, methods touched)
- Current implementation classes: `UrlServiceImpl`, `UrlController`, `UrlRepository`, `UrlShortenerServiceImpl`, `UrlShortenerController`
- Existing test structure: `src/test/java/com/schwab/urlshortener/` (currently minimal)
- Test dependencies: JUnit5, Mockito, Testcontainers, Spring Boot Test
- `docs/task-decomposition.md` acceptance criteria (if available for the change)

## Process
1. Read the impact map to list every **service method** and **controller endpoint** affected.
2. For each affected **service method**, write a **unit test** (`*ServiceImplRegressionTest.java`) that:
   - Uses `@ExtendWith(MockitoExtension.class)`, mocks repositories
   - Calls the method with representative inputs (happy path + each exception branch)
   - Asserts exact current return values / exception types / side effects (repository calls)
   - Names test: `methodName_currentBehavior_description`
3. For each affected **controller endpoint**, write an **integration test** (`*ControllerRegressionTest.java`) that:
   - Uses `@SpringBootTest(webEnvironment=RANDOM_PORT)`, `TestRestTemplate`
   - Hits the real endpoint with test data (seeded via `@BeforeEach` + repository)
   - Asserts current status code, response body shape, headers
   - Names test: `endpointMethod_currentBehavior_description`
4. For each affected **repository custom query**, write an **integration test** (`*RepositoryRegressionTest.java`) with Testcontainers.
5. Run all new tests **before the change** — they must pass (green = current behavior captured).
6. After the change, run tests again:
   - If they pass → no regression
   - If they fail → either the change intentionally alters behavior (update test assertions) or there's a regression (fix code)
7. Place tests in `src/test/java/com/schwab/urlshortener/service/`, `controller/`, `repository/`.

## Output / deliverable
- New regression test files in `src/test/java/com/schwab/urlshortener/` (service/, controller/, repository/)
- Tests that pass before and after the change (or are deliberately updated with clear commit message)

## Quality checks before returning output
- [ ] Every affected service method has a regression unit test
- [ ] Every affected controller endpoint has a regression integration test
- [ ] Every affected repository query has a regression integration test
- [ ] Tests assert **current** behavior (not desired future behavior)
- [ ] Test names include `currentBehavior` to distinguish from feature tests
- [ ] Tests use isolated test data (no cross-test pollution)
- [ ] `mvn test` passes before the change (proves tests capture current behavior)
- [ ] No Redis/caching assumptions
- [ ] Explicitly noted in PR/commit: "Regression tests added per regression-test-generation skill — closes part of README test gap"

## Example
**Change:** Modify `UrlServiceImpl.redirect()` to check `expiresAt` and throw `ExpiredUrlException`
**Impact map says:** Affected endpoints: GET /api/urls/{shortCode}, GET /api/urls/{shortCode}/analytics, GET /api/urls (list), GET /api/dashboard/*
**Regression tests written BEFORE change:**

```java
// src/test/java/com/schwab/urlshortener/service/UrlServiceImplRegressionTest.java
@ExtendWith(MockitoExtension.class)
class UrlServiceImplRegressionTest {
    @Mock UrlRepository urlRepository;
    @Mock IdGenerator idGenerator;
    @InjectMocks UrlServiceImpl urlService;

    @Test
    void redirect_currentBehavior_returnsTargetUrlAndIncrementsClickCount() {
        // given: active, non-expired link
        UrlEntity entity = UrlEntity.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com")
            .clickCount(0L)
            .active(true)
            .createdAt(Instant.now().minusSeconds(3600))
            .build(); // no expiresAt = never expires
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));
        when(urlRepository.save(any(UrlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        String target = urlService.redirect("abc123");

        // then: current behavior returns URL, increments clickCount
        assertThat(target).isEqualTo("https://example.com");
        verify(urlRepository).save(argThat(e -> e.getClickCount() == 1));
    }

    @Test
    void redirect_currentBehavior_throwsShortCodeNotFoundException_whenMissing() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> urlService.redirect("missing"))
            .isInstanceOf(ShortCodeNotFoundException.class);
    }

    @Test
    void redirect_currentBehavior_throwsInactiveUrlException_whenInactive() {
        UrlEntity entity = UrlEntity.builder()
            .shortCode("inactive")
            .originalUrl("https://example.com")
            .active(false)
            .build();
        when(urlRepository.findByShortCode("inactive")).thenReturn(Optional.of(entity));
        assertThatThrownBy(() -> urlService.redirect("inactive"))
            .isInstanceOf(InactiveUrlException.class);
    }
}
```

```java
// src/test/java/com/schwab/urlshortener/controller/UrlControllerRegressionTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class UrlControllerRegressionTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    @Autowired TestRestTemplate restTemplate;
    @Autowired UrlRepository urlRepository;

    @Test
    void GET_api_urls_shortCode_currentBehavior_returns302WithLocationHeader() {
        urlRepository.save(UrlEntity.builder()
            .shortCode("test123")
            .originalUrl("https://example.com")
            .active(true)
            .clickCount(0L)
            .createdAt(Instant.now())
            .build());

        ResponseEntity<Void> resp = restTemplate.getForEntity("/api/urls/test123", Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString()).isEqualTo("https://example.com");
    }
}
```
**After change:** Run tests. `redirect_currentBehavior_returnsTargetUrlAndIncrementsClickCount` now fails (new `ExpiredUrlException` thrown for expired links). Update test to reflect new expected behavior for expired case, add new test for expired scenario. Commit: "feat: add expiration check; update regression tests".