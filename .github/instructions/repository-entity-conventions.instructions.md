---
name: repository-entity-conventions
description: Enforce repository and entity conventions for the persistence layer.
applyTo: '**/repository/**/*.java', '**/entity/**/*.java'
---

- Repositories should extend Spring Data JPA interfaces and follow the existing repository pattern.
- Avoid raw SQL or native queries unless the code comments clearly justify them.
- Entities should use Lombok annotations consistently and stay aligned with the existing model style.
- Any entity field change must be flagged for migration and backward-compatibility review because no migration tooling is assumed in this repository.
