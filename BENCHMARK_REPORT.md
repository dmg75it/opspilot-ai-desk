# OpsPilot AI Desk — Benchmark Report

**Date:** 2026-05-13  
**Branch:** `benchmark/superpowers`  
**Approach:** Claude Code with Superpowers plugin (brainstorming → writing-plans → dispatching-parallel-agents)

---

## 1. What Was Implemented

### Backend (Spring Boot 3.3 / Java 21)

| Component | Status | Notes |
|-----------|--------|-------|
| Maven project setup (pom.xml) | ✅ | Spring Boot 3.3.5, all required deps |
| Application configuration (application.yml) | ✅ | Externalized via env vars |
| Flyway migrations (V1 schema + V2 seed) | ✅ | 6 tables, 2 seed users |
| Entity layer (6 entities + 6 enums) | ✅ | JPA, Lombok, @Version optimistic locking |
| Repository layer (6 repositories) | ✅ | Spring Data JPA + custom JPQL queries |
| JWT authentication (JJWT 0.12.x) | ✅ | HMAC-SHA384, stateless |
| Spring Security config | ✅ | CORS, 401/403, STATELESS session |
| Auth endpoints (login + me) | ✅ | POST /api/auth/login, GET /api/auth/me |
| Ticket CRUD | ✅ | Create, list (paginated+filtered), get, update |
| Ticket status transitions | ✅ | Validated server-side, 400 on invalid |
| Ticket assignment | ✅ | POST /api/tickets/{id}/assign |
| Ticket notes | ✅ | INTERNAL, AI_SUMMARY, SYSTEM visibility |
| Ticket audit trail | ✅ | Status changes and assignments recorded |
| Optimistic locking | ✅ | @Version field, 409 on conflict |
| AI provider abstraction | ✅ | AiChatProvider interface |
| OpenRouter client | ✅ | RestTemplate, timeout, structured logging |
| Fake AI provider | ✅ | Profile `fake-ai`, deterministic responses |
| Prompt templates (versioned) | ✅ | Summarize, suggest-reply, classify |
| Chat session management | ✅ | One session per ticket |
| AI chat endpoints | ✅ | messages, summarize, suggest-reply, apply-summary |
| Dashboard endpoint | ✅ | Tickets by status/priority, recent, my tickets, AI count |
| User list endpoint (ADMIN) | ✅ | GET /api/users, role-protected |
| Global exception handler | ✅ | 400/401/403/404/409/503 |
| OpenAPI / Swagger UI | ✅ | /swagger-ui.html |
| Dockerfile | ✅ | Multi-stage, eclipse-temurin:21 |
| Unit tests | ✅ | 32 unit tests |
| Integration tests (Testcontainers) | ✅ | 10 integration tests |

**Total backend Java files:** 73  
**Total backend tests:** 42/42 passing

### Frontend (Angular 21 / TypeScript)

| Component | Status | Notes |
|-----------|--------|-------|
| Angular project scaffold | ✅ | Standalone components, SCSS, Vitest |
| Angular Material integration | ✅ | Indigo-pink theme |
| TypeScript models | ✅ | User, Ticket, Chat, Dashboard, Page |
| AuthService | ✅ | login, logout, token persistence, currentUser$ |
| TicketService | ✅ | Full CRUD + status/assign/close/notes |
| ChatService | ✅ | session, messages, summarize, suggest-reply, apply |
| DashboardService | ✅ | Dashboard API call |
| UserService | ✅ | User list (admin) |
| Auth interceptor | ✅ | Attaches Bearer token |
| Error interceptor | ✅ | 401 redirect to login |
| Auth guard | ✅ | Protects all routes |
| Role guard | ✅ | Admin route protection |
| App routing | ✅ | login, dashboard, tickets, tickets/:id, admin |
| Shell component | ✅ | Sidebar nav, top bar, router-outlet |
| Login page | ✅ | Reactive form, validation, loading state |
| Dashboard page | ✅ | Status/priority cards, tables |
| Ticket list page | ✅ | Paginated table, filters (status/priority/category) |
| Ticket create page | ✅ | Reactive form, all fields, validation |
| Ticket detail page | ✅ | Full detail, action buttons, tabs |
| Ticket notes component | ✅ | List + add form, visibility badges |
| AI chat component | ✅ | Message thread, send, AI actions, apply-as-note |
| Admin page | ✅ | User table, ADMIN-only |
| Status badge component | ✅ | Color-coded by status |
| Environment config | ✅ | apiUrl externalized |
| Dockerfile + nginx.conf | ✅ | Multi-stage, Angular Material SPA |
| Unit tests | ✅ | AuthService, LoginComponent, App |

**Total frontend TypeScript files:** 33  
**Total frontend HTML files:** 11  
**Total frontend tests:** 18/18 passing

### Infrastructure

| Component | Status |
|-----------|--------|
| docker-compose.yml (postgres + backend + frontend profiles) | ✅ |
| .env.example (all variables documented) | ✅ |
| Makefile (up, down, run, test, build targets) | ✅ |
| .gitignore | ✅ |
| Implementation plan | ✅ |

---

## 2. What Was Not Implemented

| Item | Reason |
|------|--------|
| JWT refresh token | Not in spec; noted as limitation |
| Email notifications | Not in spec |
| File attachments | Not in spec |
| WebSocket real-time updates | Not in spec |
| Charts on dashboard | Spec marks charts as "optional" |
| Full Docker Compose end-to-end test | Not verified; individual components tested |
| Frontend Dockerfile end-to-end test | Image built, not run against live backend |

---

## 3. Assumptions Made

1. **Java version:** JDK 25 available on system; `pom.xml` targets Java 21 features (`<java.version>21</java.version>`).
2. **Angular version:** `ng new` resolved to Angular 21 (latest stable); spec says "Angular 17+", so this is compliant.
3. **Test runner:** Angular 21 defaults to Vitest instead of Karma/Jasmine. Tests use `vi.fn()` mocks.
4. **BCrypt seed passwords:** `admin123` and `operator123` hashed with BCrypt strength 12 and hardcoded in V2 migration.
5. **ChangeStatusRequest field name:** Field is `status` (matches enum name), not `newStatus`.
6. **Chat session uniqueness:** One chat session per ticket (unique constraint in DB).
7. **Status transition — CLOSED:** OPERATOR cannot transition out of CLOSED; ADMIN can re-open.
8. **AI response storage:** Each summarize/suggest-reply call creates a new `ChatMessage` with role=ASSISTANT; not idempotent.
9. **Bundle size budget:** Raised to 2 MB in `angular.json` because Angular Material alone exceeds 500 KB.
10. **Fake AI determinism:** `FakeAiChatProvider` returns static strings regardless of input.
11. **OpenRouter model:** Default `openai/gpt-3.5-turbo` (cheapest widely-available model).
12. **Docker Compose version attribute:** Warning about obsolete `version:` key is cosmetic.
13. **CORS:** Defaults to `http://localhost:4200`; override via `CORS_ALLOWED_ORIGINS`.
14. **Swagger:** Exposed without authentication for development convenience.

---

## 4. Commands Executed

### Verification commands

```bash
# Backend compile
cd backend && mvn compile -q
# → EXIT:0

# Backend test suite
cd backend && mvn test
# → Tests run: 42, Failures: 0, Errors: 0

# Frontend build
cd frontend && npm run build
# → Application bundle generation complete (1.05 MB, budget warning only)

# Frontend tests
cd frontend && npm test -- --watch=false
# → Test Files: 3 passed (3), Tests: 18 passed (18)

# Full stack smoke test
docker compose up postgres -d
FAKE_AI=true mvn spring-boot:run -Dspring-boot.run.profiles=fake-ai

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
# → 200 OK with JWT token

# Ticket creation
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","description":"Test desc","priority":"HIGH","category":"DELIVERY"}'
# → 201 Created

# Status transition (valid)
curl -X POST http://localhost:8080/api/tickets/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
# → 200 OK, status: "IN_PROGRESS"

# Status transition (invalid) 
curl -X POST http://localhost:8080/api/tickets/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED"}'
# → 400 Bad Request, message: "Invalid status transition from IN_PROGRESS to CLOSED"

# 401 without token
curl http://localhost:8080/api/tickets
# → HTTP 401

# 403 wrong role
curl http://localhost:8080/api/users -H "Authorization: Bearer $OPERATOR_TOKEN"
# → HTTP 403

# AI chat (fake mode)
curl -X POST http://localhost:8080/api/tickets/1/chat/summarize \
  -H "Authorization: Bearer $TOKEN"
# → 200 OK, model: "fake-ai", role: "ASSISTANT"
```

---

## 5. Test / Build Results

| Suite | Passed | Failed | Total |
|-------|--------|--------|-------|
| Backend unit tests | 32 | 0 | 32 |
| Backend integration tests (Testcontainers) | 10 | 0 | 10 |
| **Backend total** | **42** | **0** | **42** |
| Frontend unit tests (Vitest) | 18 | 0 | 18 |
| **Frontend total** | **18** | **0** | **18** |
| **Grand total** | **60** | **0** | **60** |

**Backend build:** `mvn package` → SUCCESS  
**Frontend build:** `npm run build` → SUCCESS (bundle budget warning only)  
**Flyway migrations:** Applied successfully (V1 + V2)  
**Seed users:** Verified via login API

---

## 6. Known Limitations

1. **No JWT refresh token** — sessions expire after 24 hours.
2. **No real-time updates** — UI requires manual refresh to see changes.
3. **Fake AI is static** — responses are hardcoded strings, not context-aware.
4. **No pagination on notes/messages** — all notes and chat messages are fetched in one call.
5. **No file upload support** — notes and tickets are text-only.
6. **Admin cannot re-open CLOSED tickets via UI** — the API supports it but no UI button exists.
7. **Frontend bundle size** — 1.05 MB initial load (196 KB gzip); acceptable for internal tools.
8. **Swagger is unauthenticated** — fine for development, should be secured in production.
9. **Single PostgreSQL instance** — no read replica or connection pooling (HikariCP defaults).
10. **OpenRouter error handling** — returns 503 to client; no retry logic implemented.

---

## 7. Suggested Next Improvements

### High priority
- Add JWT refresh token endpoint
- Implement WebSocket or SSE for real-time ticket updates
- Add pagination to notes and chat messages
- Secure Swagger UI behind authentication in production profiles

### Medium priority
- Add file attachment support (S3/MinIO)
- Implement email notifications (Spring Mail + templates)
- Add Prometheus metrics endpoint (`spring-boot-starter-actuator`)
- Add OpenTelemetry tracing for AI call spans

### Low priority
- Add charts to dashboard (Chart.js or ngx-charts)
- Implement admin user management (create/deactivate users)
- Add full-text search on ticket title/description
- Add bulk ticket operations (mass assign, mass close)
- Add dark mode to frontend
- Add E2E tests (Playwright or Cypress)

---

## Approach Notes

This implementation used the **Superpowers plugin for Claude Code** with the following skill chain:

1. `brainstorming` → recognized CLAUDE.md as complete spec, proceeded directly
2. `writing-plans` → produced `docs/superpowers/plans/2026-05-13-opspilot-ai-desk.md`
3. `dispatching-parallel-agents` → launched two concurrent agents:
   - **Backend agent:** implemented all 73 Java files, fixed 2 runtime bugs (401 vs 403, LazyInitializationException), achieved 42/42 tests
   - **Frontend agent:** implemented all 33 TypeScript files, 11 HTML templates, 18/18 tests
4. Main agent: foundation files, integration verification, smoke testing, documentation

**Total wall-clock time for parallel implementation:** ~60 minutes (backend agent took ~60 min, frontend ran concurrently in ~13 min)
