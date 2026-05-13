# OpsPilot AI Desk

A production-oriented web application for transport/logistics field support teams. Operators can manage tickets, assign work, track status transitions, and get AI-assisted suggestions via OpenRouter.

---

## Architecture

```
opspilot-ai-desk/
├── backend/          Spring Boot 3.3 REST API (Java 21)
├── frontend/         Angular 21 SPA (TypeScript, Angular Material)
├── docker-compose.yml
├── Makefile
└── .env.example
```

**Backend layers:** controller → service → repository → entity, with dedicated `ai/`, `security/`, `config/`, `exception/` packages.

**Frontend structure:** `core/` (models, services, guards, interceptors) + `pages/` (feature components) + `layout/` (shell) + `shared/` (reusable components).

**AI integration:** `AiChatProvider` interface with two implementations — `OpenRouterChatProvider` (production) and `FakeAiChatProvider` (development/test). Selection is controlled by the `FAKE_AI=true` env variable and the `fake-ai` Spring profile.

---

## Prerequisites

- Docker and Docker Compose
- Java 21+ (JDK)
- Maven 3.9+
- Node 18+ / npm
- Angular CLI (`npm i -g @angular/cli`)

---

## Quick Start

### 1. Start PostgreSQL

```bash
docker compose up postgres -d
```

### 2. Run backend

```bash
cp .env.example .env          # edit OPENROUTER_API_KEY if needed
make backend-run              # real AI mode
# or
make backend-run-fake         # fake AI mode (no API key needed)
```

Backend starts at `http://localhost:8080`. Flyway applies migrations automatically.

### 3. Run frontend

```bash
make frontend-install         # install npm deps (first time)
make frontend-run             # starts at http://localhost:4200
```

### 4. Login

| Email | Password | Role |
|-------|----------|------|
| admin@example.com | admin123 | ADMIN |
| operator@example.com | operator123 | OPERATOR |

---

## Makefile Targets

| Command | Description |
|---------|-------------|
| `make up` | Start PostgreSQL |
| `make down` | Stop Docker services |
| `make full-up` | Start full stack (PostgreSQL + backend + frontend) via Docker |
| `make backend-run` | Run backend with real AI |
| `make backend-run-fake` | Run backend with fake AI provider |
| `make backend-test` | Run backend unit + integration tests |
| `make frontend-run` | Start frontend dev server |
| `make frontend-test` | Run frontend unit tests |
| `make test` | Run all tests |

---

## Environment Variables

See `.env.example` for the full list. Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | dev default | Min 32-char secret for JWT signing |
| `OPENROUTER_API_KEY` | (empty) | Your OpenRouter API key |
| `OPENROUTER_MODEL` | `openai/gpt-3.5-turbo` | Model to use |
| `FAKE_AI` | `false` | Set `true` for fake AI mode |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated allowed origins |

---

## API Overview

Base URL: `http://localhost:8080`

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate, get JWT |
| GET | `/api/auth/me` | Current user profile |

### Tickets
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tickets` | List with pagination & filters |
| POST | `/api/tickets` | Create ticket |
| GET | `/api/tickets/{id}` | Get ticket |
| PUT | `/api/tickets/{id}` | Update ticket |
| POST | `/api/tickets/{id}/status` | Change status |
| POST | `/api/tickets/{id}/assign` | Assign operator |
| POST | `/api/tickets/{id}/close` | Close ticket |
| GET | `/api/tickets/{id}/notes` | List notes |
| POST | `/api/tickets/{id}/notes` | Add note |

### AI Chat
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tickets/{id}/chat/session` | Get or create chat session |
| POST | `/api/tickets/{id}/chat/messages` | Send message |
| GET | `/api/tickets/{id}/chat/messages` | List messages |
| POST | `/api/tickets/{id}/chat/summarize` | Generate summary |
| POST | `/api/tickets/{id}/chat/suggest-reply` | Generate reply suggestion |
| POST | `/api/tickets/{id}/chat/apply-summary/{msgId}` | Save AI summary as note |

### Other
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard` | Dashboard stats |
| GET | `/api/users` | User list (ADMIN only) |

---

## Status Transitions

```
NEW ──────────────────► IN_PROGRESS
 │                           │
 └──► CLOSED      ◄──────────┼──► WAITING_FOR_CUSTOMER
                             │
                         RESOLVED ──► CLOSED
```

Enforced server-side by `TicketStatusTransitionValidator`. Closed tickets cannot be edited by OPERATOR role.

---

## AI Integration Notes

- All AI calls go through the `AiChatProvider` interface — the frontend never calls OpenRouter directly.
- Prompt templates are versioned in `PromptTemplates.java`.
- AI responses are stored as `ChatMessage` records for auditing.
- AI suggestions do **not** automatically modify ticket state — operators must explicitly apply them.
- If OpenRouter fails, the API returns HTTP 503 with a clear error message.
- API keys and JWT secrets are never logged. Structured logs record model name and elapsed time only.

---

## Test Strategy

**Backend (42 tests):**
- Unit tests: `JwtUtilTest`, `TicketStatusTransitionTest`, `PromptTemplatesTest`
- Integration tests (Testcontainers + real PostgreSQL): `AuthApiIntegrationTest`, `TicketApiIntegrationTest`, `SecurityIntegrationTest`

**Frontend (18 tests):**
- Unit tests: `AuthService`, `LoginComponent`, root app component

Run all tests:
```bash
make test
```

---

## Assumptions

1. Java 25 JDK used (fully backward-compatible with Java 21 features as configured in pom.xml).
2. Angular 21 was resolved by `ng new` (latest stable at build time); the spec mentioned "Angular 17+" so this is compliant.
3. Frontend uses Vitest (Angular 21's default test runner) instead of Karma/Jasmine.
4. BCrypt strength 12 for password hashing; seed passwords are hardcoded in V2 migration.
5. `ChangeStatusRequest` uses field name `status` (not `newStatus`) — API consumers should use this name.
6. Docker Compose `version:` attribute warning is benign (newer Compose ignores it).
7. The bundle size warning (1.05 MB vs 500 KB budget) is expected with Angular Material and has been raised to 2 MB in `angular.json`.
8. Chat sessions are unique per ticket (one session per ticket, created on demand).

---

## Known Limitations

1. **No refresh token** — JWT expires after 24h; users must re-login.
2. **No email notifications** — status changes and assignments are not emailed.
3. **No file attachments** — notes are text-only.
4. **No real-time updates** — clients must refresh to see changes from other users.
5. **Single-node only** — no distributed session or cache.
6. **Fake AI responses are static** — FakeAiChatProvider returns deterministic text regardless of ticket content.
7. **Frontend Docker image** — multi-stage Nginx build is provided but not tested end-to-end in CI.

---

## Happy Path (end-to-end)

```bash
# 1. Start infrastructure
docker compose up postgres -d
make backend-run-fake   # in terminal 1
make frontend-run        # in terminal 2

# 2. Login as operator at http://localhost:4200
#    email: operator@example.com / password: operator123

# 3. Create a ticket from the "New Ticket" page

# 4. Open the ticket detail, change status to IN_PROGRESS

# 5. Open the AI Chat tab → click "Summarize Ticket"

# 6. Click "Apply as Note" on the AI response

# 7. Add a manual note in the Notes tab

# 8. Login as admin, assign the ticket to the operator

# 9. Close the ticket
```
