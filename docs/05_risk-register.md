# Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| No authentication or rate-limiting layer for management endpoints | High | High | Keep the current limitation explicit in the documentation, add guardrails before broader exposure, and treat admin endpoints as prototype-only until access controls are introduced. |
| Open redirect risk on destination URLs | Medium | High | Validate redirect targets conservatively, keep the validation strategy explicit, and review redirect handling as a security-sensitive path. |
| Alias collision under concurrency | Medium | Medium | Preserve deterministic uniqueness checks, add regression tests around concurrent creation paths, and keep any uniqueness behavior documented and reviewed. |
| No caching at scale for redirect traffic | High | Medium | Document the current limitation, treat the redirect path as a hot path for future optimization, and add a cache strategy only after traffic and latency behavior are measured. |
| Coverage gaps in the test suite | Medium | Medium | Continue expanding regression tests around controllers, services, and error cases and use JaCoCo as a quality gate. |
| Documentation drift as the implementation changes | Medium | Medium | Keep architecture, API, traceability, and engineering-metrics docs synchronized with the codebase and review them during each significant change. |
