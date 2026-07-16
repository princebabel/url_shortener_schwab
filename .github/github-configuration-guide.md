# GitHub Copilot Configuration Guide

This repository’s .github folder contains the configuration that shapes how GitHub Copilot behaves in this project. Some pieces run automatically in the background, while others are meant to be used by hand when an engineer wants a specific workflow started. This document is the single place to see what each file does and why it exists.

## The Four Types of Files, in Plain Terms

### Agents
Agents are like choosing a specific expert before you start a chat, such as a greenfield builder or a brownfield maintainer. They give the conversation a particular role, priorities, and working style.

### Instructions
Instructions are rules that apply automatically and invisibly based on the file you are editing. They help keep the work aligned with the project’s structure and standards without you having to ask for them each time.

### Prompts
Prompts are ready-made task instructions that an engineer pastes in when they want to start a specific kind of work. They are useful for larger workflows such as planning, architecture, or submission prep.

### Skills
Skills are capabilities an agent can reach for on its own during a task when the situation looks like it needs that kind of help. They are used automatically when the agent decides they fit the current work.

## Inventory of Files Under .github

| Folder | File name | Location | Triggered by | Scope | Purpose |
|---|---|---|---|---|---|
| .github | copilot-instructions.md | .github/copilot-instructions.md | Automatically, always-on | Whole workspace | Gives Copilot the repository’s core ground truth so it follows the project’s stack, package layout, endpoint expectations, and documentation rules in every conversation. |
| agents | coding-assistant-task-router.agent.md | .github/agents/coding-assistant-task-router.agent.md | Manually via agent picker | Request triage and agent selection | Gives the workflow an initial routing step that inspects the codebase and recommends whether the task should be handled as greenfield or brownfield before implementation starts. |
| agents | coding-assistant-brownfield.agent.md | .github/agents/coding-assistant-brownfield.agent.md | Manually via agent picker | Brownfield maintenance and refactoring work | Gives the brownfield workflow a role focused on inspecting existing code carefully, preserving behavior, and adding regression safety before making changes. |
| agents | coding-assistant-greenfield.agent.md | .github/agents/coding-assistant-greenfield.agent.md | Manually via agent picker | Greenfield feature-building work | Gives the greenfield workflow a role focused on creating new capabilities from scratch while following the project’s architecture and endpoint rules. |
| instructions | config-logging-conventions.instructions.md | .github/instructions/config-logging-conventions.instructions.md | Automatically, by file path match | Java config and logging classes only | Keeps logging and correlation-ID behavior consistent when new config or middleware code is added. |
| instructions | controller-conventions.instructions.md | .github/instructions/controller-conventions.instructions.md | Automatically, by file path match | REST controllers only | Ensures controller code stays thin, uses Swagger annotations, returns DTOs, and routes errors through the central exception handling flow. |
| instructions | documentation-conventions.instructions.md | .github/instructions/documentation-conventions.instructions.md | Automatically, by file path match | Documentation Markdown files only | Keeps architecture, API summary, and traceability documents aligned with the project’s real implementation and current workflow practices. |
| instructions | dto-validation-conventions.instructions.md | .github/instructions/dto-validation-conventions.instructions.md | Automatically, by file path match | DTO classes only | Makes request and response models follow the project’s validation and naming conventions. |
| instructions | exception-handling-conventions.instructions.md | .github/instructions/exception-handling-conventions.instructions.md | Automatically, by file path match | Exception classes only | Ensures the project uses centralized exception handling and consistent error responses rather than silent local catches. |
| instructions | repository-entity-conventions.instructions.md | .github/instructions/repository-entity-conventions.instructions.md | Automatically, by file path match | Repository and entity classes only | Keeps persistence code aligned with Spring Data JPA conventions and makes entity changes visible for migration review. |
| instructions | security-conventions.instructions.md | .github/instructions/security-conventions.instructions.md | Automatically, by file path match | Controllers and config classes only | Guards against redirect and SSRF risks, protects secrets, and keeps logging and validation aligned with security expectations. |
| instructions | service-conventions.instructions.md | .github/instructions/service-conventions.instructions.md | Automatically, by file path match | Service classes only | Keeps business logic and validation orchestration in services and supports constructor-based testability. |
| instructions | test-conventions.instructions.md | .github/instructions/test-conventions.instructions.md | Automatically, by file path match | Test classes only | Pushes the team to cover real edge cases and to treat the current test gap as a genuine engineering concern. |
| prompts | 01-requirement-analysis.md | .github/prompts/01-requirement-analysis.md | Manually, pasted/referenced by engineer | Requirement analysis workflow | Starts a structured review of a feature request and produces a professional requirements document without writing code. |
| prompts | 02-task-decomposition.md | .github/prompts/02-task-decomposition.md | Manually, pasted/referenced by engineer | Planning and execution workflow | Turns a normalized requirement into a phased engineering plan and backlog-style task breakdown. |
| prompts | 03-architecture-design.md | .github/prompts/03-architecture-design.md | Manually, pasted/referenced by engineer | Architecture design workflow | Produces an architecture document for the URL shortener that covers principles, components, security, reliability, and deployment. |
| prompts | 04-backend-bootstrap.md | .github/prompts/04-backend-bootstrap.md | Manually, pasted/referenced by engineer | Project bootstrapping workflow | Generates the initial Spring Boot project structure and core configuration files for the service. |
| prompts | 05-url-creation.md | .github/prompts/05-url-creation.md | Manually, pasted/referenced by engineer | URL-creation feature build | Implements the URL creation flow, including entities, DTOs, repository, service, controller, and validation. |
| prompts | 06-url-redirection.md | .github/prompts/06-url-redirection.md | Manually, pasted/referenced by engineer | URL-redirection feature build | Implements redirect behavior, click tracking, and the related controller and service logic. |
| prompts | 07-analytics-dashboard.md | .github/prompts/07-analytics-dashboard.md | Manually, pasted/referenced by engineer | Analytics and dashboard feature build | Implements analytics APIs and operational dashboard endpoints for listing, searching, and summarizing links. |
| prompts | 08-enterprise-quality.md | .github/prompts/08-enterprise-quality.md | Manually, pasted/referenced by engineer | Production-readiness and quality workflow | Improves the project’s quality, logging, correlation IDs, Swagger docs, health checks, and validation without changing core business behavior. |
| prompts | 09-submission-package.md | .github/prompts/09-submission-package.md | Manually, pasted/referenced by engineer | Final submission and documentation workflow | Prepares the final engineering package, including README and supporting design and API documentation. |
| skills/api-contract-design | SKILL.md | .github/skills/api-contract-design/SKILL.md | Automatically, by task-intent match | API contract and Swagger documentation | Helps keep the live Swagger contract and the API summary documentation accurate when endpoints or payloads change. |
| skills/codebase-impact-analysis | SKILL.md | .github/skills/codebase-impact-analysis/SKILL.md | Automatically, by task-intent match | Brownfield change safety | Maps the full controller-to-service-to-repository chain before changes are made so regressions can be avoided. |
| skills/documentation-sync | SKILL.md | .github/skills/documentation-sync/SKILL.md | Automatically, by task-intent match | Documentation maintenance | Keeps architecture, API, and engineering decision documents aligned with the implemented code. |
| skills/refactor-safety-check | SKILL.md | .github/skills/refactor-safety-check/SKILL.md | Automatically, by task-intent match | Change review and regression prevention | Reviews a proposed diff for layer-boundary issues, contract drift, and other refactor-related risks. |
| skills/regression-test-generation | SKILL.md | .github/skills/regression-test-generation/SKILL.md | Automatically, by task-intent match | Brownfield test safety | Adds regression tests that capture current behavior before an existing feature is changed. |
| skills/requirement-analysis | SKILL.md | .github/skills/requirement-analysis/SKILL.md | Automatically, by task-intent match | Requirement clarification work | Turns vague requests into structured requirements with explicit missing details and affected endpoints. |
| skills/security-review | SKILL.md | .github/skills/security-review/SKILL.md | Automatically, by task-intent match | Security review work | Checks redirect safety, validation, logging, secrets handling, and rate-limiting gaps for the service. |
| skills/spring-boot-scaffolding | SKILL.md | .github/skills/spring-boot-scaffolding/SKILL.md | Automatically, by task-intent match | New Spring Boot layer generation | Generates new controller, service, repository, entity, DTO, and exception code in the project’s expected structure. |
| skills/task-decomposition | SKILL.md | .github/skills/task-decomposition/SKILL.md | Automatically, by task-intent match | Project planning work | Breaks a normalized requirement into ordered engineering tasks with risk and acceptance criteria. |
| skills/test-generation | SKILL.md | .github/skills/test-generation/SKILL.md | Automatically, by task-intent match | Test writing work | Produces unit and integration tests for services, repositories, controllers, and concurrency cases. |

## How These Work Together

1. An engineer opens Copilot Chat and selects the Task Router agent from [agents/coding-assistant-task-router.agent.md](agents/coding-assistant-task-router.agent.md) when they need a quick triage decision before implementation. 2. If the work is classified as greenfield, the engineer can switch to the Greenfield Engineer agent from [agents/coding-assistant-greenfield.agent.md](agents/coding-assistant-greenfield.agent.md). 3. If the work is classified as brownfield, the engineer can switch to the Brownfield Engineer agent from [agents/coding-assistant-brownfield.agent.md](agents/coding-assistant-brownfield.agent.md). 4. While the engineer edits files such as controllers or services, the matching instructions from [instructions/controller-conventions.instructions.md](instructions/controller-conventions.instructions.md) or [instructions/service-conventions.instructions.md](instructions/service-conventions.instructions.md) apply automatically in the background. 5. If the work is still unclear, the engineer can paste [prompts/01-requirement-analysis.md](prompts/01-requirement-analysis.md) to start a requirement review. 6. As the conversation progresses, the agent may reach for a skill such as [skills/requirement-analysis/SKILL.md](skills/requirement-analysis/SKILL.md) or [skills/spring-boot-scaffolding/SKILL.md](skills/spring-boot-scaffolding/SKILL.md) because the task matches its purpose.

## Keeping This Document Current

This guide should be regenerated or updated whenever a file is added to, removed from, or substantially changed within .github so it stays accurate and useful.


