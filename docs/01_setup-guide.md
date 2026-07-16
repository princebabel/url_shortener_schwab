# Project Setup Guide

This guide helps a new engineer quickly clone, configure, build, run, test, and validate the URL shortener service. It is intended to be concise, practical, and aligned with the current repository implementation.

---

# 1. System Requirements

| Requirement | Minimum/Recommended Version | Notes |
|---|---:|---|
| Windows 10/11, macOS, or Linux | Latest supported release | Any modern desktop OS is suitable. |
| Java | 21 | Required to build and run the application. |
| Maven | 3.9+ | Used for build, test, and packaging. |
| Git | Latest | Required for cloning and version control. |
| VS Code | Recommended | Best experience for Java, Spring Boot, and Markdown workflows. |
| GitHub Copilot Extension | Optional | Useful for AI-assisted development and documentation tasks. |
| Postman | Optional | Helpful for manual API testing. |
| Modern Browser | Latest | Needed for Swagger UI and local web testing. |

---

# 2. Recommended VS Code Extensions

| Extension Name | Purpose |
|---|---|
| Java Extension Pack | Java language support, debugging, testing, and project navigation. |
| Spring Boot Extension Pack | Spring Boot project support, run/debug configuration, and Spring-specific tooling. |
| GitHub Copilot | AI-assisted code completion and refactoring. |
| GitHub Copilot Chat | Interactive engineering support in the editor. |
| Markdown Preview | Preview of project documentation and setup guides. |
| REST Client | Quick manual API requests from VS Code. |
| Mermaid Preview | Preview architecture and workflow diagrams in Markdown. |

---

# 3. Clone Repository

Open a terminal and run:

```bash
git clone <repository-url>
cd <project-folder>
```

Example:

```bash
git clone https://github.com/<org>/<repo>.git
cd url_shortener_schwab
```

---

# 4. Verify Prerequisites

Run the following commands:

```bash
java -version
mvn -version
git --version
```

Expected results:

- `java -version` should report Java 21.
- `mvn -version` should report Maven 3.9 or newer.
- `git --version` should return a valid Git version.

> Note: If any command is not found, install the missing tool before continuing.

---

# 5. Build Project

From the project root, run:

```bash
mvn clean install
```

What happens:

- Maven compiles the Java sources.
- Test classes are executed.
- The application artifact is packaged.
- The build verifies the current repository state.

If the build succeeds, you are ready to run the service.

---

# 6. Run Application

You can start the application in either of these ways:

### Option A: From the terminal

```bash
mvn spring-boot:run
```

### Option B: From VS Code

Open the main class and use the Run/Debug entry for the Spring Boot application.

The main entry point is:

- [src/main/java/com/schwab/urlshortener/UrlShortenerApplication.java](../src/main/java/com/schwab/urlshortener/UrlShortenerApplication.java)

### Verify successful startup

Look for Spring Boot startup logs in the terminal and confirm the application binds to the configured port.

The default port is 8081.

---

# 7. Verify Application

The application is configured to run on port 8081 by default.

## Health Endpoint

Open:

```text
http://localhost:8081/api/v1/health
```

Expected result:

- HTTP 200 response from the health endpoint.

## Swagger UI

Open:

```text
http://localhost:8081/swagger-ui.html
```

Expected result:

- Swagger UI loads successfully and lists the available API operations.

## OpenAPI JSON

Open:

```text
http://localhost:8081/v3/api-docs
```

Expected result:

- JSON document describing the live API contract.

## H2 Console

The application uses an embedded H2 database by default. The H2 console is not enabled in the active configuration, so it is not required for normal development.

---

# 8. Create First Short URL

Use the following curl request:

```bash
curl -X POST http://localhost:8081/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com","customAlias":"demo"}'
```

Expected response:

- HTTP 201 Created
- A JSON response containing the shortened code and related metadata

Example shape:

```json
{
  "shortCode": "demo",
  "originalUrl": "https://example.com",
  "active": true
}
```

---

# 9. Verify Redirect

Open the shortened URL in a browser:

```text
http://localhost:8081/api/urls/demo
```

Expected behavior:

- The browser is redirected to the original destination.
- The redirect target is the URL you created earlier.

> Note: The service uses the short code returned by the create call.

---

# 10. Verify Analytics

Use the analytics endpoint:

```bash
curl http://localhost:8081/api/urls/demo/analytics
```

Expected result:

- A JSON response with analytics information such as the short code, original URL, click count, and active state.

---

# 11. Verify Dashboard

The dashboard endpoints are available at:

| Endpoint | Purpose |
|---|---|
| /api/urls/dashboard/summary | Returns summary metrics. |
| /api/urls/dashboard/recent | Returns the most recently created URLs. |
| /api/urls/dashboard/top | Returns the most frequently accessed URLs. |

Example requests:

```bash
curl http://localhost:8081/api/urls/dashboard/summary
curl http://localhost:8081/api/urls/dashboard/recent
curl http://localhost:8081/api/urls/dashboard/top
```

Expected result:

- Each endpoint returns a JSON payload with dashboard-oriented information.

---

# 12. Project Structure

A simplified view of the repository is shown below:

```text
.github/                # Agent, instruction, prompt, and skill configuration
 docs/                   # Architecture, ADRs, API docs, requirements, and setup guides
 src/
   main/
     java/com/schwab/urlshortener/
       config/           # OpenAPI, logging, and correlation-ID support
       controller/       # REST controllers
       dto/              # Request and response DTOs
       entity/          # JPA entities
       exception/       # Centralized exception handling
       mapper/          # Mapping logic
       repository/      # Spring Data repositories
       service/         # Business logic
       util/            # Utility helpers
       validation/      # URL validation logic
     resources/
       application.yml  # Runtime configuration
   test/
     java/com/schwab/urlshortener/  # Unit and integration-style tests
```

Key packages:

- `controller` contains the REST entry points.
- `service` holds business logic and orchestration.
- `repository` manages persistence access.
- `entity` stores JPA mappings.
- `dto` carries request and response payloads.
- `config` contains OpenAPI, logging, and request-filter support.
- `exception` contains centralized error handling.
- `docs` stores architecture and engineering documentation.
- `.github` contains Copilot configuration, prompts, instructions, and skills.

---

# 13. Development Workflow

A recommended engineering workflow for this repository is:

```text
Requirement
↓
Architecture
↓
Implementation
↓
Validation
↓
Testing
↓
Documentation
↓
Commit
```

Recommended practice:

1. Review the requirements and architecture docs first.
2. Implement the smallest change that satisfies the requirement.
3. Validate behavior manually or through tests.
4. Update documentation if the contract or behavior changes.
5. Commit with a clear message.

---

# 14. Running Tests

Run the test suite with:

```bash
mvn test
```

Expected result:

- The suite completes successfully.
- The current repository baseline includes multiple test classes covering controllers, services, validation, exceptions, and configuration.

---

# 15. Troubleshooting

| Problem | Likely Cause | Solution |
|---|---|---|
| Java not found | Java 21 is not installed or not on PATH | Install Java 21 and verify with `java -version`. |
| Maven not found | Maven is not installed or not on PATH | Install Maven 3.9+ and verify with `mvn -version`. |
| Port already in use | Another process is using port 8081 | Stop the conflicting process or change the port via `SERVER_PORT`. |
| Swagger not loading | Application not running or port mismatch | Confirm the app is running and that the URL uses the correct port. |
| Build failure | Missing dependency or compile issue | Re-run `mvn clean install` and inspect the error output. |
| Dependency download issues | Network or repository access problem | Retry later or check Maven settings and network connectivity. |
| H2 database reset | The in-memory H2 database restarts with the app | Recreate data by re-running the app or re-submitting requests. |
| Validation errors | Request body does not meet DTO constraints | Ensure the payload uses a valid URL and a supported alias format. |
| 409 Conflict | Duplicate alias supplied | Choose a new alias or omit the custom alias. |
| 500 Internal Server Error | Unexpected runtime problem | Review application logs and inspect the exception details. |

---

# 16. Common Development Commands

| Command | Purpose |
|---|---|
| `mvn clean` | Cleans the build output. |
| `mvn install` | Builds and installs the package. |
| `mvn test` | Runs the test suite. |
| `mvn spring-boot:run` | Starts the application. |
| `mvn package` | Packages the application. |
| `git status` | Shows modified files. |
| `git pull` | Updates the local branch from the remote. |
| `git branch` | Lists local branches. |

---

# 17. Best Practices

- Follow the layered architecture and keep responsibilities clear.
- Keep controllers lightweight and delegate work to services.
- Place business logic in services rather than in controllers.
- Validate request payloads at the boundary.
- Use DTOs to keep API contracts stable.
- Handle exceptions centrally and return consistent responses.
- Keep Swagger and documentation updated when the API changes.
- Write meaningful commit messages.
- Run tests before committing significant changes.

---

# 18. AI-Assisted Development

GitHub Copilot is intended to support this repository as an engineering accelerator rather than a substitute for human judgment. In this project, it can help with:

- Agent Mode for multi-step development tasks
- Prompt-based requirements and planning workflows
- Skills for scaffolding, testing, and documentation
- Instruction-driven conventions for controllers, services, DTOs, and tests

Before committing any AI-assisted change:

- Review the generated content.
- Validate the behavior manually or through tests.
- Ensure the documentation reflects the implemented behavior.
- Avoid relying on AI output without human verification.

---

# 19. Next Steps

Use this checklist as a quick onboarding checklist:

- [ ] Clone the repository
- [ ] Verify Java 21 is installed
- [ ] Verify Maven is installed
- [ ] Build the project with `mvn clean install`
- [ ] Run the application with `mvn spring-boot:run`
- [ ] Open the Swagger UI
- [ ] Create a short URL
- [ ] Verify the redirect behavior
- [ ] Verify analytics and dashboard endpoints
- [ ] Run the test suite with `mvn test`
- [ ] Read the README and architecture documentation
- [ ] Review the ADR documents under the docs folder

---
