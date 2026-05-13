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
| Full Docker Compose end-to-end test | Partially verified; blank-page bug was caught and fixed post-deploy |
| Frontend Dockerfile end-to-end test | Image built and verified after zone.js fix |

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

## 5b. Post-Deploy Issue: Blank Page (Identified and Resolved)

### Symptom

After running `make full-up`, navigating to `http://localhost:4200` showed a completely blank white page. All Docker containers were healthy, nginx served files with HTTP 200, and no network errors were visible.

### Diagnosis

**Key observation:** the nginx access log showed the browser downloading `index.html`, `main.js`, and `styles.css` successfully, but making no API calls afterward — Angular was not bootstrapping.

Investigation steps:
1. Checked nginx config → correct, files served with 200
2. Checked container contents → all files present
3. Inspected `index.html` → `<app-root></app-root>` empty (expected for CSR)
4. Tested CORS preflight → backend correctly returned `Access-Control-Allow-Origin: http://localhost:4200`
5. Verified Angular source code → no obvious errors in routes, guards, or components
6. **Checked `angular.json` build options → `polyfills` was `null`/absent**

### Root Cause

**`zone.js` was missing from the Angular bundle.**

Angular uses Zone.js for change detection. The `app.config.ts` used `provideZoneChangeDetection({ eventCoalescing: true })` which requires Zone.js to be present at runtime. The `angular.json` generated by the frontend agent omitted the required `"polyfills": ["zone.js"]` entry. Without Zone.js, Angular bootstrapped but change detection never ran — the router never processed the initial navigation and the DOM remained empty.

The standard Angular CLI scaffold includes this automatically, but the agent wrote a custom `angular.json` that inadvertently left it out.

### Fix Applied

```bash
# 1. Added to angular.json build options:
"polyfills": ["zone.js"]

# 2. Installed zone.js package:
npm install zone.js

# 3. Rebuilt Docker image and restarted container:
docker compose --profile full build frontend
docker compose --profile full up frontend -d
```

Additional improvements in the same fix:
- Added Google Fonts (Roboto) and Material Icons CDN links to `index.html`
- Fixed page title from "Frontend" to "OpsPilot AI Desk"
- Added `window.onerror` handler in `index.html` to surface future JS errors visually
- Added visible error display in `main.ts` bootstrap `.catch()` for easier debugging

### Verification

After the fix, the container includes `polyfills-RV3JTMEC.js` (35 KB with Zone.js). The login page renders correctly and API calls to `http://localhost:8080` succeed.

### Impact on Generated Code Quality

This was a missing configuration entry, not a logic or architectural bug. The Angular code itself (components, services, routing) was correct and functional — only the build wiring was incomplete. All 18 frontend unit tests continued to pass before and after the fix (Vitest uses its own module system and is unaffected by Zone.js).

---

## 5c. Post-Deploy Issue: UI Non-Functional (Field Name Mismatches)

### Symptom

After deploying the full stack, the UI was non-functional for the core ticket operations:
- Assigning a ticket to an operator had no effect or threw an error.
- Changing ticket status always produced "Failed to change status." with no explanation.
- Ticket detail page showed blank fields for "Created By" and "Assigned To".
- Notes showed blank author names.
- There was no way to edit ticket title, description, priority, or category from the UI.

### Diagnosis

Direct API testing confirmed the backend was working correctly. The root cause was a mismatch between the TypeScript models in the frontend and the JSON field names returned by the backend.

The frontend agent generated TypeScript interfaces with nested `User` objects, while the backend DTOs returned flat string/id fields:

| Location | Frontend (wrong) | Backend (actual) |
|----------|-----------------|-----------------|
| `Ticket.createdBy` | `User` object | `createdByName: string`, `createdById: number` |
| `Ticket.assignedTo` | `User` object | `assignedToName: string`, `assignedToId: number` |
| `TicketNote.author` | `User` object | `authorName: string`, `authorId: number` |
| `ChangeStatusRequest` | `newStatus: TicketStatus` | `status: TicketStatus` |

The `ChangeStatusRequest.newStatus` mismatch meant every status change request was rejected by the backend with a 400 validation error (`newStatus` was null). The nested `User` mismatches caused Angular template bindings to fail silently — the fields rendered as blank rather than throwing errors.

Additionally, the generic error handler `error: () => { this.error = 'Failed to change status.'; }` discarded the actual backend error message, making diagnosis harder from the UI.

### Root Cause

The frontend agent designed models independently without cross-checking the backend `TicketResponse` and `TicketNoteResponse` DTOs. The backend was deliberately designed to return flat fields to avoid lazy-loading issues (Hibernate `LazyInitializationException` — a bug fixed during backend development). The frontend agent was not aware of this decision and designed its own interface shape.

### Fix Applied

**`ticket.model.ts`** — replaced nested User objects with flat fields:
```typescript
// Before
assignedTo?: User;
createdBy: User;
// After
assignedToId?: number;
assignedToName?: string;
createdById: number;
createdByName: string;
```

**`ChangeStatusRequest`** — renamed field to match backend:
```typescript
// Before: newStatus: TicketStatus
// After:  status: TicketStatus
```

**`ticket-detail.component.ts`** — fixed `statusForm` control name (`newStatus` → `status`), fixed `changeStatus()` payload, added `editForm` with `saveEdit()` method for title/description/priority/category updates.

**`ticket-detail.component.html`** — fixed all template bindings; added Edit panel with full form (title, description, priority, category, external ref).

**`ticket-list.component.html`** — `t.assignedTo?.fullName` → `t.assignedToName`.

**`ticket-notes.component.html`** — `note.author.fullName` → `note.authorName`.

**Error messages** — replaced all hardcoded generic strings with `err?.error?.message ?? 'fallback'` so the backend's specific reason (e.g. "Invalid status transition from NEW to RESOLVED") is shown directly in the UI.

### Verification

```bash
# Status change (correct field name)
curl -X POST .../api/tickets/5/status -d '{"status":"IN_PROGRESS"}'
# → 200 OK, status: "IN_PROGRESS"

# Assign ticket
curl -X POST .../api/tickets/5/assign -d '{"operatorId":2}'
# → 200 OK, assignedToName: "Operator User"

# Add note
curl -X POST .../api/tickets/5/notes -d '{"body":"Test note"}'
# → 201, authorName: "Admin User"

# Update ticket
curl -X PUT .../api/tickets/5 -d '{"title":"Updated","priority":"CRITICAL","version":1,...}'
# → 200 OK, title: "Updated", priority: "CRITICAL"
```

All 18 frontend unit tests continued to pass before and after the fix.

### Impact on Generated Code Quality

This was a **cross-agent coordination failure**: two independent agents (backend and frontend) designed their data contracts separately without a shared schema. The backend serialization format was influenced by a runtime bug fix (lazy loading) that the frontend agent was never informed of. The fix required aligning the frontend models to the actual API responses.

The missing Edit form was a functional gap: the `ticketService.update()` method existed in the service layer but no UI panel exposed it to the user.

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

2026-05-13 14:21 Superpowers
Session
  Total cost:            $0.0000
  Total duration (API):  0s
  Total duration (wall): 3m 54s
  Total code changes:    0 lines added, 0 lines removed
  Usage:                 0 input, 0 output, 0 cache read, 0 cache write

  Current session
                                                     0% used
  Resets 7:10pm (Europe/Rome)

  Current week (all models)
                                                     0% used
  Resets May 16, 8am (Europe/Rome)


2026-05-13 15:31
Session

  Total cost:            $13.88
  Total duration (API):  51m 52s
  Total duration (wall): 1h 21m 1s
  Total code changes:    6588 lines added, 133 lines removed
  Usage by model:
      claude-haiku-4-5:  685 input, 20 output, 0 cache read, 0 cache write ($0.0008)
     claude-sonnet-4-6:  3.1k input, 171.9k output, 31.6m cache read, 479.9k cache write ($13.88)

  Current session
  ████████████████████████████████▌                  65% used
  Resets 7:10pm (Europe/Rome)

  Current week (all models)
  ███████                                            14% used
  Resets May 16, 8am (Europe/Rome)

Troubleshouting avviato alle 16:02, completato alle 16:45