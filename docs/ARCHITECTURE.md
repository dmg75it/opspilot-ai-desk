# Architecture Overview

## System overview

OpsPilot AI Desk is a monorepo web application for transport/logistics field support teams.
It consists of three main runtime components:

```
┌──────────────────────┐        ┌───────────────────────────┐
│   Angular Frontend   │──HTTP──│   Spring Boot Backend API  │
│   (port 4200)        │        │   (port 8080)              │
└──────────────────────┘        └───────────┬───────────────┘
                                            │
                              ┌─────────────┴──────────────┐
                              │                            │
                     ┌────────▼────────┐       ┌──────────▼──────────┐
                     │   PostgreSQL    │       │  OpenRouter AI API   │
                     │   (port 5432)   │       │  (external, HTTPS)  │
                     └─────────────────┘       └─────────────────────┘
```

## Technology stack

| Layer      | Technology                              |
|------------|-----------------------------------------|
| Frontend   | Angular 17+, TypeScript, standalone components |
| Backend    | Java 21, Spring Boot 3.3+, Maven        |
| Persistence| PostgreSQL 16, Spring Data JPA, Flyway  |
| Security   | Spring Security, JWT (stateless)        |
| AI         | OpenRouter (configurable), fake provider for dev/test |
| Infra      | Docker Compose, Makefile                |

## Backend layer structure

```
src/main/java/io/opspilot/desk/
  config/          Spring configuration beans, CORS, security, OpenAPI
  security/        JWT filter, token service, UserDetails adapter
  controller/      REST controllers (thin, delegate to services)
  dto/             Request and response records/classes
  service/         Business logic
  repository/      Spring Data JPA interfaces
  entity/          JPA entities
  mapper/          Entity <-> DTO mappers
  integration/     OpenRouter client abstraction and implementations
  exception/       Global exception handler, custom exceptions
```

### Key design decisions

- **Stateless JWT auth**: no server-side session. The JWT carries user id, email, and roles.
- **Role-based access**: `ADMIN` and `OPERATOR`. Spring Security method-level annotations enforce roles.
- **Optimistic locking**: tickets include a `@Version` field. Concurrent edits return HTTP 409.
- **Status machine**: status transitions are validated in the service layer, not in the database.
- **Auditable notes**: every significant ticket change is recorded as a `SYSTEM` note in addition to normal updates.
- **AI abstraction**: an `AiProvider` interface allows swapping between `OpenRouterAiProvider` and `FakeAiProvider` at runtime via the `AI_PROVIDER` environment variable.
- **Prompt versioning**: prompt templates live in `src/main/resources/prompts/` as text files with a version suffix. The service loads them by name.

## Frontend structure

```
src/
  app/
    core/           Auth service, HTTP interceptor, guards, models
    features/
      auth/         Login page
      dashboard/    Dashboard page
      tickets/      Ticket list, detail, create
      ai-chat/      AI chat panel (used inside ticket detail)
      admin/        Admin user list
    shared/         Reusable components, pipes, directives
  environments/     environment.ts / environment.prod.ts
```

### Key design decisions

- **Standalone components**: no shared NgModule overhead.
- **Reactive forms**: all forms use `ReactiveFormsModule`.
- **HTTP interceptor**: attaches the Bearer token to every request and redirects to login on 401.
- **Route guards**: `AuthGuard` for authenticated routes, `RoleGuard` for admin-only routes.
- **Environment config**: API base URL is read from `environment.ts` so it can be replaced at build time.

## Data model summary

```
users           -- application users (ADMIN | OPERATOR)
tickets         -- operational issues
ticket_notes    -- internal comments, AI summaries, system events
ai_sessions     -- one session per ticket per user
ai_messages     -- chat messages within a session
audit_log       -- append-only log of ticket state changes
```

Flyway migrations live in `backend/src/main/resources/db/migration/`.
