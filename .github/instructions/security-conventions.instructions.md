---
name: security-conventions
description: Enforce security and input-validation expectations for controllers and config.
applyTo: '**/controller/**/*.java', '**/config/**/*.java'
---

- Validate original URLs against open-redirect and SSRF risks before persisting or redirecting them.
- Keep secrets and credentials out of code and application.yml; use environment-variable placeholders instead.
- Avoid collecting or exposing PII beyond what is already correlation-ID-scoped in logs.
- Flag rate-limiting gaps because no rate-limiting layer exists in the current implementation.
