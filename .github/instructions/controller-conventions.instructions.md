---
name: controller-conventions
description: Enforce controller-layer conventions for the URL shortener service.
applyTo: '**/controller/**/*.java'
---

- Every public endpoint must include springdoc-openapi annotations such as @Operation and @ApiResponse.
- Controllers orchestrate only: keep business logic, validation orchestration, and repository access out of the controller layer.
- Return DTOs from dto/ rather than entities; do not expose persistence models directly through REST responses.
- Route exceptions through the exception/ package handler rather than handling them locally in controllers.
