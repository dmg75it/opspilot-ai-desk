# Test Strategy

## Overview

The test suite is split across backend (JVM) and frontend (Node/Karma).
The goal is meaningful coverage of business logic and integration points, not 100% line coverage.

---

## Backend tests

### Unit tests — `src/test/java/.../unit/`

Fast, no Spring context, no database, no external calls.

| Class under test          | What is tested                                    |
|---------------------------|---------------------------------------------------|
| `TicketStatusMachine`     | All valid and invalid status transitions          |
| `PromptBuilder`           | Placeholder substitution, template loading        |
| `JwtTokenService`         | Token generation and validation                   |
| `FakeAiProvider`          | Returns expected canned responses                 |
| `TicketMapper`            | Entity <-> DTO mapping correctness                |

Run:
```bash
cd backend && mvn test -pl . -Dtest="**/unit/**"
```

### Integration tests — `src/test/java/.../integration/`

Use Testcontainers to spin up a real PostgreSQL instance.
Spring context is loaded once per test class (`@SpringBootTest`).

| Test class                     | What is tested                                          |
|--------------------------------|---------------------------------------------------------|
| `TicketApiIT`                  | Full CRUD cycle via REST, pagination, filters           |
| `AuthApiIT`                    | Login, JWT validation, protected endpoints              |
| `TicketStatusTransitionIT`     | Status change endpoint, invalid transitions return 422  |
| `AiChatApiIT`                  | Chat session lifecycle with fake provider               |
| `DashboardApiIT`               | Aggregate endpoint returns correct counts               |

Run:
```bash
cd backend && mvn test -pl . -Dtest="**/integration/**"
```

> Docker must be running for Testcontainers to work.

### Security tests

Covered inside `AuthApiIT` and other integration tests:
- Unauthenticated requests return `401`.
- `OPERATOR` accessing admin endpoints returns `403`.
- JWT with wrong secret returns `401`.
- Expired JWT returns `401`.

### Running all backend tests

```bash
make test-backend
# or
cd backend && mvn test
```

Test reports: `backend/target/surefire-reports/`

---

## Frontend tests

Framework: Jasmine + Karma (Angular default).

### Unit tests

| File                          | What is tested                              |
|-------------------------------|---------------------------------------------|
| `auth.service.spec.ts`        | Login, token storage, logout, isLoggedIn    |
| `ticket.service.spec.ts`      | HTTP calls, error propagation               |
| `auth.guard.spec.ts`          | Redirect to login when unauthenticated      |
| `ticket-list.component.spec.ts` | Renders ticket rows, pagination controls  |
| `login.component.spec.ts`     | Form validation, submit calls auth service  |

Run:
```bash
make test-frontend
# or
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless
```

Test reports: `frontend/coverage/`

---

## CI/CD expectations

In a CI pipeline:
1. Start PostgreSQL via Testcontainers (no separate service needed).
2. Run `mvn test` in `backend/`.
3. Run `npm test -- --watch=false --browsers=ChromeHeadless` in `frontend/`.
4. Fail the pipeline on any test failure.

The `AI_PROVIDER` environment variable must be set to `fake` in CI so no real API key is required.

---

## Known limitations and excluded tests

- No end-to-end (Playwright/Cypress) tests are included in the initial version.
  The README documents a manual happy-path walkthrough instead.
- OpenRouter integration is not tested with real API calls in CI.
  A separate manual smoke test step is documented in `docs/LOCAL_SETUP.md`.
