---
name: service-conventions
description: Enforce service-layer conventions for business logic and validation.
applyTo: '**/service/**/*.java'
---

- Put business logic and validation orchestration in services; keep them focused on use-case behavior.
- Services may call repository interfaces only and should not inline raw JPA queries or direct persistence logic.
- Define clear transaction boundaries with @Transactional where the operation needs atomicity.
- Keep services unit-testable in isolation through constructor-injected collaborators and mocks.
