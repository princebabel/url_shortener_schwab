---
name: exception-handling-conventions
description: Enforce centralized exception handling conventions.
applyTo: '**/exception/**/*.java'
---

- Handle exceptions centrally through an @ControllerAdvice-style handler rather than scattered local try/catch blocks.
- Preserve a consistent error response shape across the API.
- Do not introduce silent catch-and-ignore blocks anywhere in the codebase.
- Use custom exceptions for domain cases such as alias collision, short code not found, and expired links.
