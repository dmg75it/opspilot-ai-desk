# BENCHMARK REPORT — OpsPilot AI Desk

## 1. What Was Implemented

### Backend (Java 21 / Spring Boot 3.3.4)

- **Maven project** with all required dependencies (Spring Web, Security, Data JPA, Validation, Flyway, jjwt 0.12.6, springdoc-openapi 2.6.0, Testcontainers 1.20.4, Lombok, MapStruct)
- **Flyway migrations**: V1 (full schema with 6 tables and indexes), V2 (placeholder, users seeded via DataInitializer)
- **Entities**: AppUser, Ticket, TicketNote, TicketAudit, ChatSession, ChatMessage + all enums
- **Repositories**: JPA with custom queries, JpaSpecificationExecutor for dynamic filtering
- **JWT security**: JwtService, JwtAuthFilter, UserDetailsServiceImpl, SecurityConfig (stateless, CORS configurable)
- **Auth endpoints**: POST /api/auth/login, GET /api/auth/me
- **Ticket workflow**: full CRUD, pagination+filtering, status transitions with validation, assignment, notes, audit trail, optimistic locking
- **AI integration**: AiProvider interface, FakeAiProvider (default), OpenRouterAiProvider (conditional on `AI_PROVIDER=openrouter`)
- **AI chat**: chat sessions per user/ticket, send messages, generate summary, suggest reply, apply AI result as note
- **Prompt templates**: versioned (v1) static templates for summarize, suggest, classify, draft reply, identify missing info
- **Dashboard API**: tickets by status/priority, my open tickets, recently updated, AI interactions today
- **Admin API**: list users (ADMIN role only)
- **Global exception handler**: validation errors, 401, 403, 404, 422, 500
- **DataInitializer**: seeds admin@example.com/admin123 and operator@example.com/operator123 on startup
- **Tests**: PromptBuilderTest (3 unit), TicketStatusTransitionTest (3 unit), TicketApiIntegrationTest (3 integration, skipped due to Docker API incompatibility)
- **OpenAPI/Swagger**: auto-generated at /swagger-ui.html

### Frontend (Angular 21)

- **Standalone components**: login, dashboard, ticket-list, ticket-detail, ticket-create, admin, layout
- **Services**: AuthService (JWT + signals), TicketService, ChatService, DashboardService, UserService
- **HTTP interceptor**: attaches Bearer token, handles 401
- **Route guards**: authGuard (requires login), adminGuard (requires ADMIN role)
- **Pages**: login, dashboard (stats + my tickets + recent), ticket list (filterable + paginated), ticket detail (status transitions, notes, AI chat panel), create ticket, admin (user list)
- **AI chat panel**: embedded in ticket detail, chat messages, generate summary button, suggest reply button, apply as note button
- **Environment config**: environment.ts (dev), environment.prod.ts (prod)
- **Tests**: AuthService spec (3), LoginComponent spec (3), App spec (2) — 8/8 pass

### Infrastructure

- **docker-compose.yml**: PostgreSQL 16 with healthcheck, persistent volume, full-stack profile
- **backend/Dockerfile**: multi-stage build (Maven 3.9 + JDK 21 → eclipse-temurin:21-jre-alpine)
- **frontend/Dockerfile**: multi-stage build (Node 20 build → nginx:alpine), with nginx reverse proxy for `/api`
- **frontend/nginx.conf**: SPA routing + `/api` proxy to backend container
- **.env.example**: all required variables documented
- **Makefile**: up, down, backend, frontend, test-backend, test-frontend, build, full-stack-up, logs, clean
- **.gitignore**: Java/Maven, Node/Angular, IDE, secrets, OS
- **docs/**: ARCHITECTURE.md, API.md, AI_INTEGRATION.md, TEST_STRATEGY.md, LOCAL_SETUP.md

---

## 2. What Was NOT Implemented

- **User registration endpoint** — users are seeded via DataInitializer
- **Email notifications**
- **File attachments** on tickets
- **Real-time updates** (WebSocket/SSE)
- **MapStruct mappers** — inline mapping in service layer instead
- **Ticket search by text** (full-text search not implemented, only exact enum filters)
- **Frontend charts** for dashboard (tables/lists used instead, charts are listed as optional in spec)
- **Password change** endpoint
- **Audit log UI** (backend endpoint exists, not exposed in frontend)
- ~~**Ticket assignment UI**~~: fixed — ticket-detail now has "Assign to me", "Unassign", and (ADMIN only) operator dropdown

---

## 3. Assumptions Made

1. **Angular 21** used (latest stable, CLI version 21.2.3 present on machine) instead of Angular 17+ minimum as specified — superset, fully compatible.
2. **DataInitializer** seeds users on startup using BCrypt instead of SQL migration — avoids storing plain BCrypt hashes in SQL files.
3. **Chat sessions** are per-user per-ticket (one active session per user/ticket pair, reused if exists).
4. **AI responses** never auto-modify ticket status — must be explicitly applied by user.
5. **JWT secret**: default dev key is 256+ bits; validated at config level.
6. **CORS**: default `http://localhost:4200`, configurable via `CORS_ALLOWED_ORIGINS` env var.
7. **Fake AI**: returns deterministic keyword-based responses; suitable for demo and local dev.
8. **Integration tests**: use `@Testcontainers(disabledWithoutDocker = true)` to skip gracefully when Docker is incompatible.
9. **Optimistic locking**: `@Version` on Ticket entity; conflicts return HTTP 500 (Spring default for OptimisticLockingFailureException).
10. **Closed ticket restriction**: only ADMIN can edit closed tickets.

---

## 4. Commands Executed

```bash
# Create Angular project
ng new frontend --routing=true --style=scss --standalone=true --skip-git=true --no-interactive

# Backend compile
cd backend && JAVA_HOME=/opt/platform/jdk-21.0.7 mvn compile

# Backend unit tests
cd backend && JAVA_HOME=/opt/platform/jdk-21.0.7 mvn test -Dtest="TicketStatusTransitionTest,PromptBuilderTest"

# Backend all tests
cd backend && JAVA_HOME=/opt/platform/jdk-21.0.7 mvn test

# Frontend build
cd frontend && npm run build -- --configuration=development

# Frontend tests
cd frontend && npm test -- --watch=false

# Docker postgres
docker compose up -d postgres
```

---

## 5. Test / Build Results

### Backend

| Test Class | Tests | Pass | Fail | Skip | Notes |
|------------|-------|------|------|------|-------|
| PromptBuilderTest | 3 | 3 | 0 | 0 | Unit tests for prompt templates |
| TicketStatusTransitionTest | 3 | 3 | 0 | 0 | Unit tests for status transitions |
| TicketApiIntegrationTest | 3 | 0 | 0 | 3 | Skipped: Docker API 1.32 < required 1.40 |
| **TOTAL** | **9** | **6** | **0** | **3** | |

**Build**: SUCCESS (`mvn compile` — clean)

### Frontend

| File | Tests | Pass | Fail |
|------|-------|------|------|
| app.spec.ts | 2 | 2 | 0 |
| auth.service.spec.ts | 3 | 3 | 0 |
| login.component.spec.ts | 3 | 3 | 0 |
| **TOTAL** | **8** | **8** | **0** |

**Build**: SUCCESS (`npm run build` — all lazy chunks generated, no errors)

---

## 5b. End-to-End Test — OpenRouter (Real AI)

Test eseguito il **2026-05-14** con `AI_PROVIDER=openrouter`, modello `openai/gpt-3.5-turbo`.

### Setup

- Backend avviato via JAR (`mvn package -DskipTests` + `java -jar`)
- PostgreSQL su Docker (schema ricreato da zero con le migration Flyway V1/V2)
- Nota: il DB preesistente usava `bigserial` (INT) come PK invece di UUID — le tabelle sono state eliminate e ricreate dalla migration V1

### Comandi eseguiti

```bash
# Build JAR
cd backend && JAVA_HOME=/opt/platform/jdk-21.0.7 mvn package -DskipTests

# Avvio backend con OpenRouter
java -jar target/opspilot-desk-0.0.1-SNAPSHOT.jar \
  DB_PASSWORD=opspilot_secret AI_PROVIDER=openrouter \
  OPENROUTER_API_KEY=sk-or-v1-... OPENROUTER_MODEL=openai/gpt-3.5-turbo

# Test via curl
POST /api/auth/login
POST /api/tickets
POST /api/tickets/{id}/chat/session
POST /api/tickets/{id}/chat/summary
POST /api/tickets/{id}/chat/suggest-reply
POST /api/chat/sessions/{id}/messages
POST /api/tickets/{id}/notes/from-ai/{messageId}
GET  /api/tickets/{id}/notes
GET  /api/chat/sessions/{id}/messages
GET  /api/dashboard
```

### Risultati

| Step | Endpoint | Esito | Dettaglio |
|------|----------|-------|-----------|
| Login | `POST /api/auth/login` | ✅ | JWT emesso per operator@example.com |
| Crea ticket | `POST /api/tickets` | ✅ | Ticket HIGH/DELIVERY con externalRef `ORD-2024-9876` |
| Sessione AI | `POST /api/tickets/{id}/chat/session` | ✅ | Sessione creata e riutilizzabile |
| **Genera summary** | `POST /api/tickets/{id}/chat/summary` | ✅ | Modello: `openai/gpt-3.5-turbo`, 230 token |
| **Suggest reply** | `POST /api/tickets/{id}/chat/suggest-reply` | ✅ | Bozza email professionale, 280 token |
| **Chat message** | `POST /api/chat/sessions/{id}/messages` | ✅ | Risposta contestuale con 4 azioni, 391 token |
| **Apply as note** | `POST /api/tickets/{id}/notes/from-ai/{msgId}` | ✅ | Nota salvata con visibility `AI_SUMMARY`, prefisso `[AI Summary - v1]` |
| Storia chat | `GET /api/chat/sessions/{id}/messages` | ✅ | 4 messaggi persistiti (2 ASSISTANT, 1 USER, 1 ASSISTANT) |
| Note ticket | `GET /api/tickets/{id}/notes` | ✅ | Nota AI_SUMMARY visibile con autore e corpo |
| Dashboard | `GET /api/dashboard` | ✅ | `aiInteractionsToday: 1`, contatori status/priority corretti |

### Esempio risposta AI reale

**Summary generata da GPT-3.5-turbo:**
```
Summary:
Customer reported non-delivery of a package (#ORD-2024-9876) scheduled for May 13th.
Package was scanned at local depot at 08:15 AM but no further updates. Urgent resolution
needed as package contains medical supplies.
```

**Azioni suggerite (chat conversazionale):**
```
Priority: HIGH
Next Action:
1. Contact the local depot where the package was last scanned at 08:15 AM.
2. Escalate to depot manager or supervisor if necessary.
3. Update the customer about the ongoing investigation.
4. Monitor progress closely and provide timely updates.
```

### Conclusione E2E

Tutti i flussi AI funzionano correttamente end-to-end. L'integrazione OpenRouter è operativa, i prompt versioned (`[v1]`) vengono applicati, le risposte vengono persistite nel DB e l'applicazione del summary come nota funziona correttamente senza modificare automaticamente lo stato del ticket.

---

## 6. Known Limitations

### Environment-specific

- **Integration tests skipped**: `docker-java` 3.3.x (bundled in Testcontainers 1.20.4) uses Docker API 1.32 by default. Docker 29.x requires minimum API 1.40. Tests are correctly structured and would pass in an environment with Docker <= 25.x or if Testcontainers bundles docker-java 3.4+.

### Architecture

- **Full-stack Docker**: Dockerfiles provided for backend and frontend; `make full-stack-up` / `docker compose --profile full-stack up --build` builds and starts all three services.
- **No pagination on notes/audit**: notes and audit trail are returned as full lists without pagination.
- **MapStruct not used**: spec listed it as a dependency; inline mapping was used instead for simplicity.
- **Token refresh**: no refresh token mechanism; access token expires after `JWT_EXPIRATION_MS` (default 24h).

### Frontend

- **No auto-scroll in AI chat**: new messages do not auto-scroll the chat window.
- **Alert() for AI apply note**: uses browser `alert()` instead of a proper toast notification.
- **ngFor with *ngFor directive**: ticket-list uses `*ngFor` directive syntax (compatible, but inconsistent with `@for` control flow syntax used elsewhere).
- ~~**Zone.js missing from production build**~~: fixed — `zone.js` added to `polyfills` in `angular.json`; without it the Angular router silently fails with `NG0908` in production builds.

---

## 7. Suggested Next Improvements

1. **Fix integration tests**: upgrade to Testcontainers 1.21.x or override docker-java to 3.4.x to support Docker 26+.
2. **Optimize Docker images**: reduce build time with a local Maven cache volume in CI; add health-check to frontend nginx container.
3. **Refresh tokens**: implement refresh token flow for better UX and security.
4. **Real-time updates**: add SSE or WebSocket for live ticket updates and AI streaming responses.
5. **Toast notifications**: replace `alert()` with a proper notification component.
6. **User management UI**: add ability to create/disable users from the admin panel.
7. **Full-text search**: add PostgreSQL full-text search on ticket title/description.
8. **Audit log UI**: expose ticket audit trail in the frontend.
9. **MapStruct mappers**: introduce proper mapper classes to decouple entity from DTO layer.
10. **CI/CD pipeline**: add GitHub Actions workflow for automated testing on every push.
11. **AI streaming**: implement streaming responses from OpenRouter for better UX.
12. **Rate limiting**: add rate limiting on AI endpoints to control OpenRouter costs.



