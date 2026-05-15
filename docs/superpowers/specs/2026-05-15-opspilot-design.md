# OpsPilot AI Desk — Design Document

**Date:** 2026-05-15  
**Branch:** benchmark/superpowers-default  
**Approach:** Backend-first sequential (Approach A)

---

## 1. Architecture Overview

```
Browser (Angular 19)
  → HTTP/HTTPS (JWT Bearer token)
  → Spring Security filter chain
  → REST Controllers
  → Services
  → JPA Repositories / AiClient
  → PostgreSQL 16 / OpenRouter API
```

**Monorepo layout:**
```
opspilot-ai-desk/
  backend/          ← Spring Boot 3.3.x, Java 21, Maven
  frontend/         ← Angular 19, TypeScript, Angular Material
  docker-compose.yml
  .env.example
  Makefile
  docs/
    superpowers/
      specs/
```

**Key architectural decisions:**
- JWT authentication: issued at login, validated by `JwtFilter` on every request
- AI provider isolated behind `AiClient` interface; `FakeAiClient` is the default (no external calls), `OpenRouterClient` activated by setting env var `AI_PROVIDER=openrouter`
- Flyway manages all schema migrations; `DataInitializer` updates BCrypt password hashes at startup
- CORS fully configurable via `application.yml`
- Maven uses `JAVA_HOME=/opt/platform/jdk-21.0.7` (Java 21 at non-standard path)

---

## 2. Backend Design

**Java package:** `io.opspilot.desk`

**Package structure:**
```
config/          ← SecurityConfig, CorsConfig, OpenApiConfig
security/        ← JwtFilter, JwtService, UserDetailsServiceImpl
controller/      ← AuthController, TicketController, NoteController,
                    AiController, DashboardController, UserController
dto/             ← request/response records per domain
service/         ← AuthService, TicketService, NoteService, AiService,
                    DashboardService, AuditService
repository/      ← JPA repositories (Spring Data)
entity/          ← User, Ticket, TicketNote, AuditLog, ChatSession, ChatMessage
ai/              ← AiClient (interface), FakeAiClient, OpenRouterClient, PromptTemplates
```

**Key libraries:**
- Spring Boot 3.3.x
- JJWT 0.12.x
- Springdoc OpenAPI 2.x (Swagger UI at `/swagger-ui.html`)
- Flyway + PostgreSQL driver
- Lombok, Validation API, MapStruct (or manual mapping)
- Testcontainers + JUnit 5

**Status transition matrix** (validated in `TicketService`):
```
NEW              → IN_PROGRESS, CLOSED
IN_PROGRESS      → WAITING_FOR_CUSTOMER, RESOLVED, CLOSED
WAITING_FOR_CUSTOMER → IN_PROGRESS, RESOLVED, CLOSED
RESOLVED         → CLOSED, IN_PROGRESS
CLOSED           → (none, ADMIN only can reopen to IN_PROGRESS)
```

**Optimistic locking:** `@Version` on `Ticket` entity.

**Audit trail:** `AuditLog` table populated by `AuditService`, called from `TicketService` on status change, assignment, and close.

**Prompt versioning:** `PromptTemplates.java` holds named string constants for each AI prompt type. Version is tracked by class git history.

---

## 3. Frontend Design

**Framework:** Angular 19, standalone components, Angular Material

**Structure:**
```
src/app/
  core/
    auth/         ← AuthService, JwtInterceptor, AuthGuard, AdminGuard
    models/       ← TypeScript interfaces: Ticket, User, ChatMessage, ...
    services/     ← TicketService, NoteService, AiService, DashboardService
  features/
    login/        ← LoginComponent (reactive form)
    dashboard/    ← DashboardComponent (status cards + priority cards + tables)
    tickets/      ← TicketListComponent, TicketDetailComponent, CreateTicketComponent
    ai-chat/      ← AiChatPanelComponent (embedded in TicketDetail)
    admin/        ← UserListComponent
  shared/
    components/   ← LoadingSpinnerComponent, ErrorBannerComponent
    layout/       ← NavbarComponent, SidenavComponent
  environments/   ← environment.ts, environment.prod.ts
```

**Key behaviors:**
- `JwtInterceptor` adds `Authorization: Bearer <token>` to every API request
- Token stored in `localStorage`; `AuthService` restores state on page refresh
- `AuthGuard` redirects unauthenticated users to `/login`
- `AdminGuard` blocks non-ADMIN users from `/admin` routes
- `proxy.conf.json` forwards `/api/**` to `localhost:8080` in dev (avoids CORS)
- Global error interceptor shows `ErrorBannerComponent` on 4xx/5xx
- Loading state managed with `BehaviorSubject<boolean>` in each service

---

## 4. Data Model

**Flyway migrations:**
```
V1__create_users.sql
V2__create_tickets.sql
V3__create_ticket_notes.sql
V4__create_audit_log.sql
V5__create_chat_sessions.sql
V6__create_chat_messages.sql
V7__seed_users.sql
```

**Entities:**

| Entity | Key fields |
|---|---|
| `User` | id (UUID), email (unique), password (BCrypt hash), role (ADMIN/OPERATOR), active |
| `Ticket` | id (UUID), externalRef (unique nullable), title (max 150), description (max 5000), status, priority, category, assignedTo (FK User), createdBy (FK User), createdAt, updatedAt, resolvedAt, version |
| `TicketNote` | id (UUID), ticket (FK), author (FK User), body, visibility (INTERNAL/AI_SUMMARY/SYSTEM), createdAt |
| `AuditLog` | id (UUID), ticket (FK), actor (FK User), action, oldValue, newValue, createdAt |
| `ChatSession` | id (UUID), ticket (FK, unique — one session per ticket), createdAt |
| `ChatMessage` | id (UUID), session (FK), role (SYSTEM/USER/ASSISTANT), content, model, promptTokens, completionTokens, createdAt, error (boolean), errorMessage |

**Notes:**
- `external_ref` uses a partial unique index: `CREATE UNIQUE INDEX ON tickets (external_ref) WHERE external_ref IS NOT NULL`
- `DataInitializer`: at startup, reads seed users and updates password hashes with real BCrypt (V7 seeds use placeholder hashes for Flyway checksum stability)

---

## 5. AI Integration

**Interface:**
```java
public interface AiClient {
    AiResponse chat(AiRequest request);
}
```

**Implementations:**
- `FakeAiClient` — returns deterministic canned responses; default in all environments unless overridden
- `OpenRouterClient` — calls `OPENROUTER_BASE_URL/chat/completions`; activated by env var `AI_PROVIDER=openrouter`

**Configuration (externalized):**
```yaml
ai:
  provider: ${AI_PROVIDER:fake}         # env var: AI_PROVIDER=fake | openrouter
  openrouter:
    api-key: ${OPENROUTER_API_KEY:}
    base-url: ${OPENROUTER_BASE_URL:https://openrouter.ai/api/v1}
    model: ${OPENROUTER_MODEL:openai/gpt-4o-mini}
    timeout-seconds: 30
    max-tokens: 1024
    temperature: 0.7
```

**`.env.example` includerà:**
```
AI_PROVIDER=fake
OPENROUTER_API_KEY=
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_MODEL=openai/gpt-4o-mini
```

**Security:** API key never logged, never returned to frontend. Logs show model name and elapsed time only.

**Prompt types (versioned in `PromptTemplates.java`):**
- `SUMMARIZE_TICKET`
- `SUGGEST_NEXT_ACTION`
- `DRAFT_CUSTOMER_REPLY`
- `IDENTIFY_MISSING_INFO`
- `CLASSIFY_PRIORITY_CATEGORY`

---

## 6. Testing Strategy

**Backend:**
| Type | Target | Tool |
|---|---|---|
| Unit | `TicketStatusTransitionService` — transition matrix | JUnit 5 |
| Unit | `PromptBuilder` — prompt assembly | JUnit 5 |
| Unit | `JwtService` — issue/validate | JUnit 5 |
| Integration | `TicketControllerIT` — CRUD, auth, status change | Testcontainers + PostgreSQL |
| Security | Protected endpoints — 401/403 without valid token | Testcontainers |

**Frontend:**
| Type | Target | Tool |
|---|---|---|
| Unit | `AuthService` — login, token, guard behavior | Karma/Jest |
| Unit | `TicketListComponent` — rendering, filter | Karma/Jest |

---

## 7. Local Dev & Docker

**Starting the stack (3 terminals):**
```bash
make db-up        # docker compose up -d postgres → localhost:5432
make backend-run  # JAVA_HOME=/opt/platform/jdk-21.0.7 mvn spring-boot:run → localhost:8080
make frontend-run # cd frontend && ng serve → localhost:4200 (proxied to :8080)
```

**Full Docker stack:**
```bash
make stack-up     # docker compose --profile fullstack up
                  # PostgreSQL + Spring Boot JAR + Nginx serving Angular build
```

**Makefile targets:**
```makefile
db-up, db-down, backend-run, backend-test, frontend-run, frontend-test,
frontend-build, stack-up, stack-down, clean
```

---

## 8. Known Constraints

- Java 21 is at `/opt/platform/jdk-21.0.7/` (non-standard path); all Maven invocations must set `JAVA_HOME` explicitly
- Integration tests require Docker (Testcontainers)
- Frontend tests require `CHROME_BIN=/usr/bin/chromium` (or equivalent)
- AI chat only works end-to-end when `OPENROUTER_API_KEY` is set and `AI_PROVIDER=openrouter`; all other environments use `FakeAiClient`
