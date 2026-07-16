---
name: config-logging-conventions
description: Enforce configuration and logging conventions for the application config layer.
applyTo: '**/config/**/*.java'
---

- Preserve the existing correlation-ID filter pattern for any new configuration or middleware additions.
- Keep structured logging consistent with the current application style and request context usage.
- Ensure OpenAPI and Swagger configuration changes keep all 9 documented endpoints correctly grouped and tagged.
