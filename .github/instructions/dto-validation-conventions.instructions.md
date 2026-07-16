---
name: dto-validation-conventions
description: Enforce DTO and validation conventions for request and response models.
applyTo: '**/dto/**/*.java'
---

- Request DTOs should use Bean Validation annotations such as @NotNull and @Pattern for URL format and related constraints.
- Response DTOs should not expose entity internals directly.
- Use naming conventions matching the existing style, such as <Thing>Request and <Thing>Response.
