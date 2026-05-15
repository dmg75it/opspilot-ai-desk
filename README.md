# OpsPilot AI Desk

AI-powered support desk for transport and logistics field operations.

## Architecture

```
opspilot-ai-desk/
  backend/        Java 21 + Spring Boot 3.4 + PostgreSQL
  frontend/       Angular 19 + Angular Material
  docker-compose.yml
  Makefile
```

### Backend layers

```
controller → service → repository → entity
               ↓
           ai/AiClient (OpenRouter or Fake)
```

### Key design decisions

- **JWT stateless auth**: tokens stored in `localStorage`, validated per-request via filter
- **OpenRouter abstraction**: `AiClient` interface with `OpenRouterClient` (real) and `FakeAiClient` (no API key needed)
- **Audit trail via SYSTEM notes**: every status change, assignment, etc. creates a `TicketNote` with `visibility=SYSTEM`
- **Optimistic locking**: `@Version` on `Ticket` entity; JPA throws `ObjectOptimisticLockingFailureException` → 409
- **Fake AI mode**: enabled when `OPENROUTER_FAKE_MODE=true` or API key is absent
- **Token tracking**: `completion_tokens` from OpenRouter response deserialized via `@JsonProperty` and stored on each `ChatMessage`

---

## Quick start (local dev)

### Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 22+
- Docker + Docker Compose

### 1. Start the database

```bash
make db
# or: docker compose up postgres -d
```

### 2. Run the backend

```bash
cp .env.example .env
make backend
# or: cd backend && mvn spring-boot:run
```

Backend starts at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Run the frontend

```bash
cd frontend && npm install && npm start
# or: make frontend
```

Frontend starts at `http://localhost:4200`.

### Full Docker stack

```bash
make stack
```

---

## Seed users

| Email | Password | Role |
|-------|----------|------|
| admin@example.com | admin123 | ADMIN |
| operator@example.com | operator123 | OPERATOR |

---

## Environment variables

See `.env.example` for all variables. Key ones:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/opspilot` | PostgreSQL URL |
| `JWT_SECRET` | (change in prod!) | HMAC-SHA256 key |
| `OPENROUTER_API_KEY` | (empty) | OpenRouter API key |
| `OPENROUTER_FAKE_MODE` | `true` | Use fake AI (no key needed) |
| `OPENROUTER_MODEL` | `openai/gpt-3.5-turbo` | AI model |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed frontend origins |

---

## API overview

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Login (public) |
| GET | `/api/auth/me` | Current user |
| GET | `/api/tickets` | List tickets (paginated) |
| POST | `/api/tickets` | Create ticket |
| GET | `/api/tickets/:id` | Get ticket |
| PATCH | `/api/tickets/:id` | Update ticket |
| PATCH | `/api/tickets/:id/status` | Change status |
| PATCH | `/api/tickets/:id/assign` | Assign ticket |
| POST | `/api/tickets/:id/notes` | Add note |
| GET | `/api/tickets/:id/notes` | List notes |
| POST | `/api/tickets/:id/close` | Close ticket (ADMIN) |
| GET | `/api/tickets/:id/chat/session` | Get/create chat session |
| GET | `/api/tickets/:id/chat/messages` | List messages |
| POST | `/api/tickets/:id/chat/messages` | Send message |
| POST | `/api/tickets/:id/chat/summary` | Generate AI summary |
| POST | `/api/tickets/:id/chat/suggested-reply` | Generate AI reply |
| POST | `/api/tickets/:id/chat/messages/:msgId/apply-as-note` | Apply as note |
| GET | `/api/dashboard` | Dashboard stats |
| GET | `/api/admin/users` | List users (ADMIN only) |

Full OpenAPI spec at `/api-docs`.

---

## AI integration

The AI layer uses the OpenRouter API (OpenAI-compatible format).

- **Fake mode** (`OPENROUTER_FAKE_MODE=true`): deterministic responses, no external calls. Default for local dev and CI.
- **Real mode**: set `OPENROUTER_API_KEY` and `OPENROUTER_FAKE_MODE=false`.

Prompt templates are versioned in `PromptTemplates.java` (v1). AI responses never auto-modify ticket status; applying suggestions requires explicit user action.

AI calls are logged (model, elapsed time, token count) without leaking secrets.

---

## Tests

### Backend

```bash
cd backend && mvn test
```

- Unit: `TicketStatusTransitionTest`, `PromptTemplatesTest`, `SecurityControllerTest`
- Integration: `TicketControllerIT` (requires Docker for Testcontainers)
- AI real API: `OpenRouterClientIT` (requires `OPENROUTER_API_KEY` and `OPENROUTER_FAKE_MODE=false`)

```bash
# Run real OpenRouter integration test
export $(grep -v '^#' .env | xargs)
cd backend && mvn test -Dtest="OpenRouterClientIT"
```

### Frontend

```bash
cd frontend && CHROME_BIN=/usr/bin/chromium npx ng test --watch=false --browsers=ChromeHeadless
```

- `AuthService` unit tests
- `LoginComponent` unit tests

---

## Ticket status transitions

```
NEW → IN_PROGRESS, CLOSED
IN_PROGRESS → WAITING_FOR_CUSTOMER, RESOLVED, CLOSED
WAITING_FOR_CUSTOMER → IN_PROGRESS, RESOLVED, CLOSED
RESOLVED → IN_PROGRESS, CLOSED
CLOSED → (only ADMIN can reopen)
```

---

## Known limitations

1. No password reset or user self-registration (by design for benchmark scope)
2. No real-time updates (WebSocket/SSE not implemented)
3. No file attachments on tickets
4. Frontend Docker build uses Java Dockerfile from `eclipse-temurin:21` but runtime is Java 25 locally — ensure Java version alignment for Docker builds
5. Frontend tests require `CHROME_BIN` set to Chromium/Chrome path
6. Integration tests (`TicketControllerIT`) require Docker daemon running for Testcontainers
7. The Flyway V5 seed migration uses placeholder hashes; real hashes are set by `DataInitializer` on first startup
8. `OpenRouterClientIT` is skipped automatically if `OPENROUTER_API_KEY` is not set in the environment (uses `Assumptions.assumeTrue`)
