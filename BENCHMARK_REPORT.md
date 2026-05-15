# Benchmark Report: OpsPilot AI Desk — feature-dev standard

Approach: `feature-dev:feature-dev` skill, spec-driven from `CLAUDE.md`.

---

## 1. What was implemented

### Backend (Spring Boot 3.4.4 / Java 25)

| Feature | Details |
|---------|---------|
| JWT authentication | Login endpoint, token issuing/validation, `/me` endpoint, `JwtAuthenticationFilter` |
| Role-based authorization | `ADMIN` and `OPERATOR` roles, `@PreAuthorize` on sensitive endpoints |
| Seed users | `admin@example.com / admin123` and `operator@example.com / operator123` via `DataInitializer` |
| Ticket CRUD | Create, list (paginated + filterable), get by id, update metadata |
| Ticket workflow | Status transitions with validation, assign operator, add note, close ticket |
| Optimistic locking | `@Version` field on `Ticket`, 409 response on conflict |
| Audit trail | Every status/assignment change creates a `SYSTEM` `TicketNote` |
| Ticket notes | `INTERNAL`, `AI_SUMMARY`, `SYSTEM` visibility |
| AI chat | Per-ticket `ChatSession`, send message, list messages |
| AI summary | Generate ticket summary via `/chat/summary` |
| AI suggested reply | Generate customer-facing reply via `/chat/suggested-reply` |
| Apply AI as note | Attach any AI message as `AI_SUMMARY` note via `/apply-as-note` |
| OpenRouter client | Real HTTP client using `RestClient`, configurable model/timeout/tokens/temperature; token count deserialized via `@JsonProperty` (snake_case → camelCase) |
| Fake AI client | `FakeAiClient` — deterministic responses, no external calls, active when `OPENROUTER_FAKE_MODE=true` or API key absent |
| Dashboard | Stats by status, by priority, my open tickets, recently updated, AI interactions today |
| Admin endpoint | List all users (ADMIN only) |
| OpenAPI/Swagger | Auto-generated at `/swagger-ui.html` and `/api-docs` |
| Flyway migrations | V1 users, V2 tickets, V3 notes, V4 chat, V5 seed placeholders |
| CORS configuration | Configurable via `CORS_ALLOWED_ORIGINS` |
| Structured logging | Auth events, ticket changes, status transitions, AI request start/end/error |

### Frontend (Angular 19 + Angular Material)

| Page/Feature | Details |
|-------------|---------|
| Login | Reactive form, error display, password toggle, JWT persistence |
| Dashboard | Status/priority counts, my open tickets table, recently updated table, AI interactions counter |
| Ticket list | Paginated table, filter by status/priority, click-through to detail |
| Ticket detail | Tabbed view: info + status change, notes, AI chat panel |
| Create ticket | Form with validation, priority/category selectors |
| AI chat panel | Send messages, generate summary/reply buttons, apply-as-note per message, loading states, error display |
| Admin page | User list table (ADMIN only) |
| Auth service | Token storage, signal-based current user, logout |
| HTTP interceptor | Adds Bearer token, handles 401 redirect |
| Route guards | `authGuard` (login required), `adminGuard` (ADMIN role) |
| StatusChip component | Reusable color-coded chip for ticket status |
| Nav shell | Side nav with role-conditional links |

### Infrastructure

- `docker-compose.yml` with PostgreSQL 16 and fullstack profile
- `Makefile` with `make db / backend / frontend / stack / test-be / test-fe`
- `.env.example` with all required variables documented
- `Dockerfile` for both backend and frontend (nginx)

---

## 2. What was NOT implemented

| Item | Reason |
|------|--------|
| User registration / password reset | Not in spec; two hardcoded seed users are sufficient for the benchmark |
| Real-time updates (WebSocket/SSE) | Not in spec |
| File attachments | Not in spec |
| Email notifications | Not in spec |
| Charts on dashboard | Spec says "optional" |
| Full end-to-end Docker build verification | Java 25 vs Docker image Java 21 mismatch not resolved in this session |

---

## 3. Assumptions made

1. **Java version**: The spec says Java 21 but the installed JDK is Java 25. Spring Boot and Lombok versions were upgraded to be compatible (Spring Boot 3.4.4, Lombok 1.18.46, jjwt 0.12.6).
2. **Fake AI default**: `OPENROUTER_FAKE_MODE=true` is the default so the application runs without any external API key. The spec implies this but does not mandate it explicitly.
3. **One session per ticket**: The spec says "start or retrieve chat session for a ticket" — interpreted as one `ChatSession` per ticket (UNIQUE constraint on `ticket_id`).
4. **Seed users via DataInitializer**: Flyway V5 inserts placeholder password hashes; a Spring Boot `ApplicationRunner` replaces them with proper BCrypt hashes on first startup, avoiding hardcoded hashes in SQL.
5. **ADMIN can bypass closed-ticket restriction**: The spec says "closed tickets cannot be edited except by ADMIN" — interpreted to mean ADMIN can change status and edit metadata on closed tickets.
6. **Status transition for CLOSED**: The spec does not define allowed transitions out of CLOSED. Assumption: ADMIN can force any transition; regular operators cannot change status of closed tickets.
7. **Angular 19**: The spec says "Angular 17+ or recent stable". Angular 19 was chosen as the latest stable version.

---

## 4. Commands executed

```bash
# Infrastructure
docker compose up postgres -d

# Backend — compile and unit tests
cd backend
mvn compile
mvn test -Dtest="TicketStatusTransitionTest,PromptTemplatesTest,SecurityControllerTest"

# Backend — real OpenRouter integration test
export $(grep -v '^#' .env | xargs)
mvn test -Dtest="OpenRouterClientIT"

# Backend — run
mvn spring-boot:run

# Frontend — scaffold
npx @angular/cli@19 new frontend --style=scss --routing=true --standalone --skip-git --no-interactive
npm install @angular/material@19 --save
npm install @angular/animations@19 --save

# Frontend — build
npx ng build --configuration=development

# Frontend — tests
CHROME_BIN=/usr/bin/chromium npx ng test --watch=false --browsers=ChromeHeadless
```

---

## 5. Test / build results

### Backend unit tests

```
Tests run: 17, Failures: 0, Errors: 0 — TicketStatusTransitionTest
Tests run:  4, Failures: 0, Errors: 0 — PromptTemplatesTest
Tests run:  3, Failures: 0, Errors: 0 — SecurityControllerTest
────────────────────────────────────────
TOTAL: 24 PASS, 0 FAIL
BUILD SUCCESS
```

### Backend integration tests

`TicketControllerIT` uses Testcontainers + `@ServiceConnection`. Requires Docker daemon running.
When Docker is available: covers create ticket, list, validation errors, role-based access.

### Real OpenRouter API tests (`OpenRouterClientIT`)

Executed against `openai/gpt-3.5-turbo` via OpenRouter. Test is automatically skipped if `OPENROUTER_API_KEY` is absent.

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.62 s
BUILD SUCCESS
```

| Test | Tokens | Elapsed |
|------|--------|---------|
| `chat_simpleQuestion_returnsNonEmptyResponse` | 4 | ~1.2s |
| `chat_ticketSummaryPrompt_returnsUsefulResponse` | 52 | ~1.8s |
| `chat_suggestedReplyPrompt_returnsProfessionalText` | 148 | ~3.9s |
| `chat_multiTurn_maintainsContext` | 15 | ~1.3s |

Sample output — summary prompt:
```
Summary: Customer's package has been in transit for 3 days with no updates.
Recommended next action: Contact the shipping carrier to inquire about the
status and provide an update to the customer.
```

Sample output — suggested reply:
```
Subject: Update on Your Parcel Delivery Inquiry
Dear [Customer's Name],
We acknowledge your concern regarding the delayed delivery of your parcel.
Our team is actively investigating the issue...
```

**Bug found and fixed during this run**: OpenRouter returns token usage with snake_case keys
(`completion_tokens`, `prompt_tokens`), but the Jackson record used camelCase field names.
Fixed by adding `@JsonProperty` annotations to the `Usage` record in `OpenRouterClient.java`.

### Frontend build

```
Application bundle generation complete. [~10s]
Output: frontend/dist/frontend
No TypeScript or template errors.
```

### Frontend unit tests

```
Executed 11 of 11 SUCCESS
TOTAL: 11 SUCCESS
```

Tests cover: `AuthService` (login, logout, isAdmin, token storage) and `LoginComponent` (form validity, rendering).

---

## 8. Post-benchmark bug fixes

Bug rilevati durante il collaudo reale dell'applicazione dopo il commit iniziale.

### Bug 1 — Docker volume stale: password authentication failed

**Sintomo**: Il backend non si avviava, log `FATAL: password authentication failed for user "opspilot"`.
**Causa**: Il volume Docker `opspilot-ai-desk_postgres_data` era stato creato in una sessione precedente con credenziali diverse. Al riavvio dello stack, PostgreSQL rifiutava le credenziali configurate in `docker-compose.yml`.
**Fix**: `docker compose down && docker volume rm opspilot-ai-desk_postgres_data`, poi riavvio completo. Il volume viene ricreato con le credenziali corrette.
**File**: nessuna modifica al codice; problema operativo di stato del volume Docker.

---

### Bug 2 — Material Icons non caricate: artefatti di testo nella sidebar

**Sintomo**: Le voci di menu mostravano le prime due lettere del nome dell'icona ("da", "co", "ad") invece del simbolo grafico.
**Causa**: `frontend/src/index.html` non includeva il link al font Google Material Icons. Senza il font, `<mat-icon>` renderizza il testo della ligatura (es. "dashboard") che viene troncato dal contenitore a dimensione fissa.
**Fix**: Aggiunti i tag `<link>` per Material Icons e Roboto in `index.html`:
```html
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap" rel="stylesheet">
```
**File**: `frontend/src/index.html`

---

### Bug 3 — Nessuna UI per assegnare un ticket

**Sintomo**: Non era possibile assegnare o auto-assegnarsi un ticket dall'interfaccia.
**Causa**: Il backend esponeva già `PATCH /api/tickets/:id/assign` e il `TicketService` Angular aveva già il metodo `assign()`, ma la card di assegnazione mancava completamente dal template del ticket detail.
**Fix**: Aggiunta card "Assign Ticket" nel tab Details di `TicketDetailComponent` con:
- Pulsante **Assign to me** per tutti gli utenti autenticati (quando il ticket non è già assegnato all'utente corrente)
- Pulsante **Unassign** quando il ticket è assegnato
- Dropdown con tutti gli utenti + pulsante **Assign** visibile solo agli ADMIN (da `/api/admin/users`)

**File**: `frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.ts`

---

### Bug 4 — Tab Notes non si aggiorna dopo "Apply as note" dalla chat AI

**Sintomo**: Dopo aver applicato un messaggio AI come nota tramite il pulsante nella chat, il tab Notes non mostrava la nuova nota. Era necessario tornare alla dashboard e riaprire il ticket.
**Causa**: `ChatPanelComponent.applyAsNote()` chiamava il backend correttamente, ma non notificava il componente parent. `TicketDetailComponent` caricava le note solo in `ngOnInit`, senza alcun meccanismo di aggiornamento esterno.
**Fix**: Aggiunto `noteAdded = output<void>()` in `ChatPanelComponent`; l'output viene emesso al successo di `applyAsNote()`. Nel parent, il binding `(noteAdded)="loadNotes()"` ricarica le note immediatamente.
**File**:
- `frontend/src/app/features/chat/chat-panel/chat-panel.component.ts`
- `frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.ts`

---

## 6. Known limitations

1. **Java version mismatch**: Spec targets Java 21; local JDK is Java 25. Docker image uses `eclipse-temurin:21` — the Dockerfile will compile with Java 21 inside Docker, but local `mvn` runs on Java 25. This is fine for development but should be unified in CI.
2. **Frontend tests require `CHROME_BIN`**: The default `ChromeHeadless` browser is not auto-detected. Must run with `CHROME_BIN=/usr/bin/chromium`.
3. **Flyway V5 placeholder hashes**: The SQL seed migration uses a placeholder string for password hashes. `DataInitializer` replaces them on first startup. If the initializer is skipped (e.g. `spring.main.web-application-type=none` tests), users cannot log in.
4. **No pagination on notes/chat messages**: Notes and chat messages are returned as full lists. For tickets with many messages, this could be slow.
5. **No HTTPS configuration**: TLS termination is left to a reverse proxy (e.g. nginx in Docker).
6. **AI token counting**: Token estimate is taken from OpenRouter's `completion_tokens` (fixed via `@JsonProperty`). For the `FakeAiClient` it is approximated as `content.length / 4`.
7. **`OpenRouterClientIT` skipped in CI without key**: The test uses `Assumptions.assumeTrue` so it degrades gracefully to skipped rather than failing when no API key is present.

---

## 7. Suggested next improvements

1. ~~**Integrate real OpenRouter call end-to-end**~~ Done: `OpenRouterClientIT` tests 4 scenarios against the live API.
2. **Add Testcontainers-based integration tests for chat and dashboard** endpoints to reach higher API coverage.
3. **Add WebSocket/SSE** for real-time ticket and chat updates.
4. **Dockerize with Java 21** to match the spec; add a CI pipeline (GitHub Actions).
5. **Add pagination** to notes and chat message lists.
6. **Implement user management** (invite, deactivate) for the admin page.
7. **Add frontend E2E tests** with Playwright or Cypress covering the login → create ticket → AI summary happy path.
8. **Extract prompt templates to configuration** (YAML or DB) to allow non-code updates.
9. **Add rate limiting** on AI endpoints to prevent runaway token usage.
10. **Implement refresh tokens** to avoid forcing re-login after JWT expiry.


2026-05-15 10:55  feature-dev standard
Session

  Total cost:            $0.0000
  Total duration (API):  0s
  Total duration (wall): 13m 12s
  Total code changes:    0 lines added, 0 lines removed
  Usage:                 0 input, 0 output, 0 cache read, 0 cache write

  Current session
                                                     0% used
  Resets 3:30pm (Europe/Rome)

  Current week (all models)
  ████▌                                              9% used
  Resets May 15, 1pm (Europe/Rome)


2026-05-15 11:40
Session

  Total cost:            $7.22
  Total duration (API):  25m 11s
  Total duration (wall): 1h 2m 2s
  Total code changes:    4977 lines added, 98 lines removed
  Usage by model:
     claude-sonnet-4-6:  5.7k input, 110.3k output, 16.1m cache read, 193.6k cache write ($7.22)
      claude-haiku-4-5:  370 input, 22 output, 0 cache read, 0 cache write ($0.0005)

  Current session
  ████████████████                                   32% used
  Resets 3:30pm (Europe/Rome)

  Current week (all models)
  ██████                                             12% used
  Resets May 15, 1pm (Europe/Rome)


2026-05-15 12:13  troubleshouting
Session

  Total cost:            $8.39
  Total duration (API):  29m 0s
  Total duration (wall): 1h 33m 11s
  Total code changes:    5100 lines added, 102 lines removed
  Usage by model:
     claude-sonnet-4-6:  6.8k input, 122.4k output, 18.6m cache read, 253.5k cache write ($8.39)
      claude-haiku-4-5:  370 input, 22 output, 0 cache read, 0 cache write ($0.0005)

  Current session
  ██████████████████▌                                37% used
  Resets 3:30pm (Europe/Rome)

  Current week (all models)
  ██████                                             12% used
  Resets May 15, 1pm (Europe/Rome)
