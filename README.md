# OpsPilot AI Desk

AI-powered support desk for field operations teams in transport and logistics.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.3.4, Maven, PostgreSQL, Flyway, JWT, OpenAPI
- **Frontend**: Angular 21, TypeScript, standalone components
- **AI**: OpenRouter (real) or Fake provider (default, no API key needed)
- **Infra**: Docker Compose, Testcontainers

## Quick Start

### Prerequisites

- Java 21 (`/opt/platform/jdk-21.0.7` or equivalent)
- Node 18+ and npm
- Docker and Docker Compose
- Maven 3.6+

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env as needed (DB password, JWT secret, OpenRouter key)
```

### 2. Start PostgreSQL

```bash
make up
# or: docker compose up -d postgres
```

### 3. Start backend

```bash
make backend
# or: cd backend && JAVA_HOME=/opt/platform/jdk-21.0.7 mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Start frontend

```bash
make frontend
# or: cd frontend && npm install && npm start
```

The frontend starts on `http://localhost:4200`.

### Full-stack with Docker

To run the entire stack (PostgreSQL + backend + frontend) via Docker:

```bash
make full-stack-up
# or: docker compose --profile full-stack up --build -d
```

Services:
- PostgreSQL: port 5432
- Backend API: `http://localhost:8080`
- Frontend: `http://localhost:4200` (nginx on port 80, proxies `/api` to backend)

```bash
make down        # stop all services
make logs        # follow logs
```

### Seed credentials

| Email | Password | Role |
|-------|----------|------|
| admin@example.com | admin123 | ADMIN |
| operator@example.com | operator123 | OPERATOR |

## AI Configuration

By default `AI_PROVIDER=fake` — no API key needed. Responses are pre-scripted.

For real AI via OpenRouter:

```env
AI_PROVIDER=openrouter
OPENROUTER_API_KEY=your-key-here
OPENROUTER_MODEL=openai/gpt-3.5-turbo
```

## Running Tests

```bash
# Backend unit tests
make test-backend
# or: cd backend && JAVA_HOME=/opt/platform/jdk-21.0.7 mvn test

# Frontend tests
make test-frontend
# or: cd frontend && npm test -- --watch=false
```

**Backend results**: 6 pass, 3 skipped (integration tests skip when Docker API < 1.40).
**Frontend results**: 8/8 pass.

## Architecture

```
opspilot-ai-desk/
  backend/           Java 21 / Spring Boot 3.3.x
    controller/      REST controllers
    service/         Business logic
    repository/      JPA repositories
    entity/          JPA entities
    dto/             Request/Response records
    security/        JWT filter, UserDetailsService
    integration/     AI provider abstraction (Fake + OpenRouter)
    config/          SecurityConfig, GlobalExceptionHandler, DataInitializer
  frontend/          Angular 21 standalone components
    pages/           login, dashboard, ticket-list, ticket-detail, ticket-create, admin
    services/        auth, ticket, dashboard, chat, user
    guards/          auth, admin
    interceptors/    JWT auth interceptor
    models/          TypeScript interfaces
  docker-compose.yml PostgreSQL 16
  docs/              Architecture, API, AI integration, test strategy, local setup
```

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/auth/login | Login, returns JWT |
| GET | /api/auth/me | Current user info |
| POST | /api/tickets | Create ticket |
| GET | /api/tickets | List tickets (paginated, filtered) |
| GET | /api/tickets/{id} | Get ticket |
| PUT | /api/tickets/{id} | Update ticket |
| PATCH | /api/tickets/{id}/status | Change status |
| PATCH | /api/tickets/{id}/assign | Assign operator |
| POST | /api/tickets/{id}/notes | Add note |
| GET | /api/tickets/{id}/notes | Get notes |
| POST | /api/tickets/{id}/chat/session | Start/get AI chat session |
| POST | /api/tickets/{id}/chat/summary | Generate AI summary |
| POST | /api/tickets/{id}/chat/suggest-reply | Suggest customer reply |
| POST | /api/chat/sessions/{id}/messages | Send AI message |
| GET | /api/dashboard | Dashboard stats |
| GET | /api/admin/users | List users (ADMIN only) |

## Ticket Status Transitions

```
NEW -> IN_PROGRESS, CLOSED
IN_PROGRESS -> WAITING_FOR_CUSTOMER, RESOLVED, CLOSED
WAITING_FOR_CUSTOMER -> IN_PROGRESS, RESOLVED, CLOSED
RESOLVED -> CLOSED, IN_PROGRESS
CLOSED -> (none)
```

## Assumptions

1. Angular 21 used (latest stable) instead of Angular 17+ — fully compatible.
2. DataInitializer seeds users on startup instead of SQL migration to use BCrypt hashing.
3. Chat sessions are per-user per-ticket (one session per user/ticket pair).
4. AI responses do not auto-modify ticket state — explicit user action required.
5. JWT secret minimum length enforced by default config (256-bit dev key).
6. CORS allows `http://localhost:4200` by default, configurable via `CORS_ALLOWED_ORIGINS`.
7. Fake AI provider returns deterministic responses based on keywords.
8. Integration tests skip when Docker API incompatibility detected (docker-java 3.3.x vs Docker 29.x).

## Known Limitations

- Integration tests skip in environments with Docker 26+ due to docker-java API version mismatch (client uses API 1.32, Docker 29.x requires minimum 1.40). Unit tests pass.
- `zone.js` must be listed in `polyfills` in `angular.json` for production builds to work (Angular error `NG0908`). Already fixed.
- No user registration endpoint — users are seeded or manually inserted.
- Ticket assignment: operators can self-assign or unassign; ADMIN can assign to any user via dropdown.
- No email notifications.
- No file attachments on tickets.
- No real-time updates (no WebSocket/SSE).
- MapStruct mappers not used (inline mapping in service layer for simplicity).
