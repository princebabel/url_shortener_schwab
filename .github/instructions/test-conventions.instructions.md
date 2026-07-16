---
name: test-conventions
description: Enforce testing expectations for the URL shortener service.
applyTo: '**/src/test/**/*.java'
---

- Follow Spring Boot test conventions and prefer realistic integration coverage for the full create-to-redirect flow.
- Explicitly cover edge cases including invalid URL, alias collision, expired or missing short code, and concurrent creation.
- Keep each test focused on one assertion or behavior rather than overloading it with multiple scenarios.
- Treat the current test coverage gap as a real issue and add targeted regression tests when behavior changes.
