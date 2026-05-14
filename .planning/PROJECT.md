# OpsPilot AI Desk

## What This Is

OpsPilot AI Desk is a production-oriented web application for transport and logistics field support teams. Operators submit and track operational tickets, communicate with an AI assistant powered by OpenRouter, and managers have visibility over team workload and ticket status.

## Core Value

Field operators can create tickets, get AI-powered assistance for resolution, and track ticket status from creation to close — all through a secure, auditable interface.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] JWT-based authentication with ADMIN and OPERATOR roles
- [ ] Ticket lifecycle management (create, update, status transitions, close)
- [ ] Ticket notes (INTERNAL, AI_SUMMARY, SYSTEM visibility types)
- [ ] AI chat assistant per ticket via OpenRouter (with fake/mock mode)
- [ ] Dashboard with ticket statistics and AI interaction counts
- [ ] Full Angular frontend with route guards and reactive forms
- [ ] Docker/Compose dev environment with PostgreSQL
- [ ] Flyway migrations with seeded users
- [ ] Integration and unit tests
- [ ] Architecture and setup documentation

### Out of Scope

- Real-time websocket notifications — deferred, out of scope for v1
- Mobile application — web-first
- Multi-tenant / organization management — out of scope for v1
- Email notifications — out of scope for v1
- File attachments to tickets — out of scope for v1

## Context

Greenfield full-stack monorepo project. This is a benchmark project to evaluate Claude Code workflows and agent-driven approaches.

**Stack (mandated by CLAUDE.md):**
- Backend: Java 21, Spring Boot 3.3.x+, Maven, Lombok, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Testcontainers, JUnit 5, OpenAPI/Swagger
- Frontend: Angular 17+, TypeScript, standalone components, reactive forms, HTTP interceptors
- AI: OpenRouter via backend service abstraction (never call from frontend)
- Auth: JWT-based, two roles (ADMIN, OPERATOR), local users in DB

**Known seed users:**
- admin@example.com / admin123 / ADMIN
- operator@example.com / operator123 / OPERATOR

**OpenRouter config (externalized):**
- OPENROUTER_API_KEY, OPENROUTER_BASE_URL, OPENROUTER_MODEL, OPENROUTER_MAX_TOKENS, OPENROUTER_TEMPERATURE, OPENROUTER_TIMEOUT_SECONDS
- Fake/mock AI provider for tests and local dev (AI_PROVIDER=fake|openrouter)

## Constraints

- **Tech Stack**: Java 21 + Spring Boot 3.3.x + Angular 17+ — mandated, no alternatives
- **Architecture**: Layered (controller / DTO / service / repository / entity / mapper / config / security / integration)
- **Security**: Hash passwords, never log JWT tokens or OpenRouter keys, validate all requests, enforce CORS
- **Observability**: Structured logs for auth events, ticket changes, AI requests — no sensitive data
- **Testing**: Unit tests for status transitions + AI prompt builder; integration tests for ticket API; security tests; frontend unit tests for at least one service and component
- **AI Safety**: AI responses must never auto-modify ticket status; applying AI suggestions requires explicit user action; secrets never returned to frontend

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| OpenRouter via backend abstraction only | Security — API key never exposed to browser | — Pending |
| Fake AI provider for tests | Zero external dependencies in CI | — Pending |
| JWT stored client-side (localStorage or cookie) | Standard SPA pattern | — Pending |
| Flyway for schema management | Auditable, repeatable migrations | — Pending |
| Testcontainers for integration tests | Real PostgreSQL in CI without mocks | — Pending |
| Optimistic locking on tickets | Prevent concurrent update conflicts | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-14 after initialization*
