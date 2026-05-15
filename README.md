# OpsPilot AI Desk

AI Support Desk for field operations teams — Spring Boot 3.3 + Angular 17 + PostgreSQL 16.

## Quick Start

### Prerequisites

- JDK 21 at `/opt/platform/jdk-21.0.7` (or set `JAVA_HOME` accordingly)
- Node 22, npm 10+
- Docker + Docker Compose

### Local development

```bash
make db-up          # Start PostgreSQL on :5432
make backend-run    # Start Spring Boot on :8080
make frontend-run   # Start Angular dev server on :4200
```

Visit http://localhost:4200

**Default login credentials:**
- `admin@example.com` / `admin123` (ADMIN role)
- `operator@example.com` / `operator123` (OPERATOR role)

### Environment variables

Copy `.env.example` to `.env` and adjust values:

```bash
cp .env.example .env
```

Key variables:
- `AI_PROVIDER=fake|openrouter` — default: `fake` (no external calls)
- `OPENROUTER_API_KEY` — required only when `AI_PROVIDER=openrouter`
- `OPENROUTER_MODEL` — default: `openai/gpt-3.5-turbo`
- `CORS_ALLOWED_ORIGINS` — default: `http://localhost:4200`
- `JWT_SECRET` — change in production

### Full Docker stack

```bash
cp .env.example .env
make stack-up   # docker compose --profile fullstack up --build
```

Services start on:
- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- PostgreSQL: localhost:5432

### Tests

```bash
make backend-test    # Unit + integration tests (requires Docker for Testcontainers)
make frontend-test   # Angular unit tests (requires Chromium)
```

## Architecture

```
Angular 17 SPA --> Spring Boot 3.3 API --> PostgreSQL 16
                         |
                   AiClient interface
                   +-- FakeAiClient (default)
                   +-- OpenRouterClient (when AI_PROVIDER=openrouter)
```

- **Authentication:** JWT tokens, BCrypt password hashing, two roles (ADMIN, OPERATOR)
- **Tickets:** Full lifecycle with status transitions, notes, audit trail, optimistic locking
- **AI chat:** Per-ticket chat session, summary generation, suggested reply, notes integration
- **Schema:** Managed by Flyway (V1-V8 migrations)
- **API docs:** Springdoc OpenAPI — http://localhost:8080/swagger-ui.html

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/auth/login | Login, returns JWT |
| GET | /api/auth/me | Current user |
| GET | /api/tickets | List tickets (paginated) |
| POST | /api/tickets | Create ticket |
| GET | /api/tickets/{id} | Get ticket |
| PATCH | /api/tickets/{id} | Update ticket |
| POST | /api/tickets/{id}/status | Change status |
| POST | /api/tickets/{id}/assign | Assign operator |
| GET | /api/tickets/{id}/notes | List notes |
| POST | /api/tickets/{id}/notes | Add note |
| GET | /api/tickets/{id}/ai/session | Get/start AI session |
| POST | /api/tickets/{id}/ai/messages | Send AI message |
| POST | /api/tickets/{id}/ai/summary | Generate summary |
| POST | /api/tickets/{id}/ai/suggested-reply | Generate reply |
| POST | /api/tickets/{id}/ai/apply-summary | Apply as note |
| GET | /api/dashboard | Dashboard data |
| GET | /api/users | List users (ADMIN) |

## AI Integration

- **Default:** `FakeAiClient` — returns deterministic fake responses, no external API calls
- **OpenRouter:** Set `AI_PROVIDER=openrouter` and `OPENROUTER_API_KEY`
- AI responses never automatically modify ticket status
- Applying an AI suggestion requires explicit user action
- Prompt templates versioned in `PromptTemplates.java`
- All AI calls logged with model, elapsed time (no key exposure)

## Known Limitations

- Users cannot self-register; seeded users only (admin and operator)
- No real-time updates (no WebSocket); page reload required for new data
- Frontend date formatting uses Angular `DatePipe` — timezone is client-local
- Docker frontend build requires npm ci (slow on first run)
- OpenRouter free-tier models may have rate limits
- Frontend bundle size exceeds Angular default budget (835 kB vs 500 kB); acceptable for this scope

## Verification

End-to-end happy path:

```bash
# 1. Start stack
make db-up && make backend-run

# 2. Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 3. Create ticket
curl -s -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test ticket","description":"Test description","priority":"HIGH","category":"DELIVERY"}' | python3 -m json.tool

# 4. Run tests
make backend-test
```
