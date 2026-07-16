# Ambiguous Requirement Scenario

## Decomposition

One ambiguous decision that was resolved during this build was whether the service should require a custom alias for every create request or allow the normal flow to fall back to an auto-generated short code when no alias is supplied.

## Execution

Original ambiguous ask: the requirement baseline said the service should "accept a long URL and generate a unique short alias," but it did not fully define whether custom aliases were mandatory or optional.

Two concrete interpretations were considered:

1. Treat custom alias as mandatory and reject create requests that omit one.
2. Treat custom alias as optional; if a caller provides one, the service validates it and checks for collisions, but if no alias is supplied the service auto-generates the short code instead.

The second interpretation was chosen because it fits the existing controller and service behavior in the repository and keeps the create flow flexible for both human-friendly and machine-generated links. It also aligns with the current service tests, which cover both the happy path without a custom alias and the duplicate-alias failure path when an alias is supplied. The decision is now recorded in [docs/others/requirement-analysis.md](../others/requirement-analysis.md) under the functional requirements and in the service behavior covered by [src/test/java/com/schwab/urlshortener/service/UrlServiceImplTest.java](../../src/test/java/com/schwab/urlshortener/service/UrlServiceImplTest.java).

## Validation

The implementation was checked against the documented behavior by verifying that the create flow works without a custom alias, that collision handling still occurs for supplied aliases, and that the requirement narrative remains consistent with the current tests and service contract.
