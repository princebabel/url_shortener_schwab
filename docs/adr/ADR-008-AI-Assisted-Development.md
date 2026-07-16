# ADR-008: AI-Assisted Development

## Status
Accepted

## Context
The URL Shortener project was developed with significant assistance from GitHub Copilot (Agent Mode) using the Nemotron-3-Ultra model. AI assistance was used across the full development lifecycle: requirement analysis, architecture design, code generation, test creation, documentation, and refactoring.

Key context:
- **Agent Mode**: Copilot acted as an autonomous agent with tool access (file read/write, terminal, search, browser)
- **Custom Agents**: Three specialized agents configured — Task Router (request triage), Greenfield Engineer (new features), and Brownfield Engineer (existing code modifications)
- **Skills System**: 10 project-specific skills encoded domain knowledge (API contract design, security review, test generation, etc.)
- **Human-in-the-loop**: All AI-generated code was reviewed, tested, and approved by a human developer before commit

## Decision
We formally adopt AI-assisted development as a supported practice for this project with the following guardrails:

### 1. Agent Mode with Defined Scope
- Use **Task Router** first for new or ambiguous requests so the work is classified before implementation begins
- Use **Greenfield Engineer** for new features (requirement analysis → implementation → tests)
- Use **Brownfield Engineer** for modifications (impact analysis → regression tests → safe changes)
- Use **Explore** agent for read-only codebase investigation
- Agents invoke only their declared skills; no ad-hoc tool use beyond scope

### 2. Human Review Gates
| Gate | Required Review |
|------|-----------------|
| Requirement Analysis | Human approves `docs/requirement-analysis.md` before task decomposition |
| Task Decomposition | Human approves `docs/task-decomposition.md` before implementation |
| Code Generation | Human reviews every file create/edit before commit |
| Test Generation | Human runs tests and verifies coverage targets (JaCoCo 80%) |
| Documentation Sync | Human verifies `docs/` updates match implementation |

### 3. Responsible AI Practices
- **No secrets in prompts**: Never include API keys, passwords, or PII in agent prompts
- **No production data in context**: Agents operate on code/config only; no live DB queries
- **Deterministic verification**: All AI-generated code must pass `mvn verify` (compile, test, JaCoCo)
- **Audit trail**: `docs/ai-traceability.md` records every AI-assisted change with prompt summary, files touched, and human reviewer
- **Skill versioning**: Skills in `.github/skills/` are version-controlled; changes require human review

### 4. Prohibited Uses
- ❌ Generating production secrets or certificates
- ❌ Direct production database mutations via agent tools
- ❌ Bypassing code review by committing agent output directly
- ❌ Using agent for security-critical code (auth, crypto, authorization) without expert human review
- ❌ Delegating architectural decisions without human sign-off

### 5. Tooling Configuration
- **Model**: Nemotron-3-Ultra (via GitHub Copilot)
- **Agents**: `.github/agents/coding-assistant-task-router.agent.md`, `.github/agents/coding-assistant-greenfield.agent.md`, `.github/agents/coding-assistant-brownfield.agent.md`
- **Skills**: `.github/skills/*/SKILL.md` (10 project skills + 2 global)
- **Instructions**: `.github/instructions/*.instructions.md` (layer-specific conventions)
- **Traceability**: `docs/ai-traceability.md` updated after each AI-assisted session

## Consequences

### Positive
- **Velocity**: ~70% reduction in boilerplate (controllers, DTOs, repositories, tests, ADRs)
- **Consistency**: Skills enforce project conventions (Lombok, constructor injection, layered architecture)
- **Coverage**: Test-generation skill targets known gaps (README: "Unit Testing to be expanded")
- **Documentation**: ADRs, architecture docs, API summaries generated from implementation, not speculation
- **Knowledge capture**: Skills encode tribal knowledge (exception hierarchy, validation patterns, etc.)

### Negative
- **Over-reliance risk**: Developers may skip understanding generated code
- **Hallucination risk**: Agent may invent APIs or patterns not in codebase
- **Context drift**: Long sessions may lose alignment with current code state
- **Skill maintenance burden**: Skills must be updated when conventions change

### Mitigations
| Risk | Mitigation |
|------|------------|
| Over-reliance | Mandatory human review gates; "explain this code" spot checks |
| Hallucination | Agents restricted to workspace tools; no external API calls for code gen |
| Context drift | `Explore` agent for fresh context; `codebase-impact-analysis` skill before changes |
| Skill drift | `documentation-sync` skill flags outdated docs; skills reviewed quarterly |

## Alternatives Considered

| Alternative | Rejected Because |
|-------------|------------------|
| No AI assistance | Slower delivery; inconsistent patterns; test gap persists |
| Copilot Chat only (no agents) | No autonomous multi-step workflows; no skill system |
| Local LLM (Ollama, etc.) | No IDE integration; no tool access; higher setup cost |
| AI for tests only | Misses value in scaffolding, docs, ADRs, requirement analysis |

## Implementation References
- `.github/agents/coding-assistant-task-router.agent.md`
- `.github/agents/coding-assistant-greenfield.agent.md`
- `.github/agents/coding-assistant-brownfield.agent.md`
- `.github/skills/*/SKILL.md` (10 project skills)
- `.github/instructions/*.instructions.md` (8 layer-specific conventions)
- `docs/ai-traceability.md` (audit log)
- `docs/others/requirement-analysis.md`, `docs/others/task-decomposition.md` (human gates)

## Related ADRs
- ADR-002: Layered Architecture (enforced by scaffolding skill)
- ADR-003: DTO Pattern (enforced by dto-validation-conventions)
- ADR-004: Global Exception Handling (enforced by exception-handling-conventions)
- ADR-005: Validation Strategy (enforced by validation skill)
- ADR-006: OpenAPI Documentation (enforced by api-contract-design skill)
- ADR-007: Logging and Correlation ID (enforced by config-logging-conventions)

## Date
2026-07-16

## Authors
GitHub Copilot (AI-assisted), reviewed by human developer