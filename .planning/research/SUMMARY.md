# Project Research Summary

**Project:** OpsPilot AI Desk
**Domain:** Transport/Logistics Field Operations Support Desk with AI Assistance
**Researched:** 2026-05-14
**Confidence:** HIGH

## Executive Summary

OpsPilot AI Desk is a production-oriented internal support desk for field operations teams, combining a ticket workflow engine with an AI assistant backed by OpenRouter. The mandated stack (Java 21, Spring Boot 3.3.x, Angular 17+, PostgreSQL) is fully specified — no technology selection is needed, only correct implementation of well-documented patterns. The architecture follows a strict layered backend (controller / service / repository / entity) with an `AiChatService` interface abstracted behind two implementations: a real OpenRouter client and a fake for tests. The frontend is a standalone-component Angular SPA with functional interceptors and lazy-loaded routes.

The recommended build order is infrastructure first, then auth, then the ticket core, then AI chat, then the frontend in parallel with late backend phases, finishing with polish and tests. Every phase has hard dependencies on the previous one: nothing works without auth, nothing can be tested reliably without the fake AI provider, and the frontend shell must exist before building ticket UI. Status transition enforcement is a state machine that must be server-side from day one — it cannot be retrofitted.

The dominant risks are implementation-level, not design-level. The five most dangerous pitfalls are: the JWT filter being double-registered (silent, hard to debug), JPA entities escaping into JSON responses (causes N+1 queries or LazyInitializationException), Flyway migration files being edited after first run (breaks all environments), the AI call holding a Tomcat thread without virtual threads enabled (causes starvation under load), and the optimistic locking version field not being included in the update DTO (silently neutralises concurrency protection). All five are preventable by following the patterns documented in STACK.md and ARCHITECTURE.md exactly.

---

## Key Findings

### Recommended Stack

The stack is mandated by the project spec and confirmed by research. Java 21 virtual threads (`spring.threads.virtual.enabled=true`) are the correct mitigation for the blocking OpenRouter HTTP call — no async/reactive complexity needed. Spring Boot 3.3.x ships Spring Security 6, which removed `WebSecurityConfigurerAdapter` and `antMatchers()` — any tutorial pre-Boot 3.x must be discarded. Angular 18 (or 17) with standalone components uses functional interceptors and `CanActivateFn` guards — class-based patterns from Angular 14 and earlier do not apply.

**Core technologies with version pins:**

| Technology | Version | Load-bearing note |
|------------|---------|-------------------|
| Java | 21 | Virtual threads for AI I/O blocking |
| Spring Boot | 3.3.11 | SecurityFilterChain bean pattern; no WebSecurityConfigurerAdapter |
| jjwt | 0.12.6 | `parseSignedClaims()` not `parseClaimsJws()` — breaking change from 0.11.x |
| Flyway | 10.x (Boot BOM) | `flyway-database-postgresql` required explicitly; two-underscore separator |
| Angular | 17 or 18 | Standalone components; functional interceptors; no NgModule required |
| springdoc-openapi | 2.8.17 | Must permit `/v3/api-docs/**` and `/swagger-ui/**` in SecurityConfig |
| MapStruct | 1.5.5.Final | Lombok must appear BEFORE MapStruct in `annotationProcessorPaths` |
| PostgreSQL | 16 | `postgres:16-alpine` in Docker |
| Testcontainers | 1.20.x | `@ServiceConnection` eliminates `@DynamicPropertySource` boilerplate |

**Key API pattern decisions:**
- Use `RestClient` (not `RestTemplate`, not `WebClient`) for the synchronous OpenRouter HTTP client.
- Use `SecurityFilterChain` bean, not `WebSecurityConfigurerAdapter`.
- Use `requestMatchers()` and `authorizeHttpRequests()`, not `antMatchers()` or `authorizeRequests()`.
- Use `@ConditionalOnProperty(name="ai.provider", havingValue="openrouter")` for the real client; `havingValue="fake"` for tests.
- Configure CORS in `SecurityConfig` only — `WebMvcConfigurer` CORS is never reached for secured endpoints.

### Expected Features

Research confirms the spec covers all table-stakes features for a logistics support desk. The key additions from feature research are UX patterns and priority ordering.

**Must have (table stakes):**
- Ticket list with status/priority/assignee columns and multi-select filters persisted in URL
- Ticket detail with three-zone layout: header / conversation thread / properties sidebar
- Activity/audit timeline on every ticket (every status change, note, assignment logged)
- Optimistic locking with a clear "someone else updated this" 409 error — not silent overwrite
- Role-based UI: ADMIN sees/does things OPERATOR does not
- Loading states on every async action (field ops have unreliable connectivity)
- Error messages that explain what to do, not raw HTTP codes

**Must have (AI panel UX):**
- Collapsible side drawer inside ticket detail — never a separate page
- Five one-click quick-action buttons: Summarize, Suggest Next Action, Draft Reply, Identify Missing Info, Classify
- "Apply as Note" button on every AI response — the explicit human-in-the-loop action
- Inline error state with retry button on AI responses (OpenRouter failures must be recoverable)

**Should have (differentiators):**
- Classification suggestion showing current vs AI-suggested values with one-click accept
- Conversation context continuity (prior messages included in each API call)
- Prompt template version stored with each AI message record (audit traceability)
- Dashboard cards that link through to filtered ticket lists

**Defer to v2+:**
- SLA timers and breach alerts
- Per-operator performance metrics
- File attachments
- Real-time websocket updates (polling acceptable for v1)
- Bulk ticket operations

**Status transition rules (server-side, non-negotiable):**
```
NEW               -> IN_PROGRESS, WAITING_FOR_CUSTOMER
IN_PROGRESS       -> WAITING_FOR_CUSTOMER, RESOLVED
WAITING_FOR_CUSTOMER -> IN_PROGRESS, RESOLVED
RESOLVED          -> CLOSED, IN_PROGRESS (reopen)
CLOSED            -> terminal for OPERATOR; ADMIN can reopen to IN_PROGRESS
```

### Architecture Approach

The backend is a strict layered architecture: controllers take requests and return DTOs, services hold all business logic, repositories hold queries, entities never cross the controller boundary. The AI integration is behind an interface (`AiChatService`) with two implementations selected at startup via `@ConditionalOnProperty`. The prompt templates live in a single versioned class (`PromptTemplates`) so they are unit-testable and traceable in git history.

**Major components and responsibilities:**

1. **SecurityConfig + JwtAuthFilter + JwtService** — Stateless JWT authentication; filter registered manually in the chain (not via `@Component`); CORS configured here only
2. **TicketService** — CRUD, state machine validation (static `ALLOWED` transition map), optimistic lock handling (catch `ObjectOptimisticLockingFailureException` → HTTP 409), closed-ticket edit guard
3. **AiChatService interface / OpenRouterAiChatService / FakeAiChatService** — AI abstraction; fake is default for tests; real client uses `RestClient` with virtual threads; message flow: save USER msg → load history (last N) → call AI → save ASSISTANT msg
4. **PromptTemplates** — Versioned prompt constants; five templates for field ops; never inline in controllers or frontend
5. **Flyway migrations V1–V5** — Schema authority; `ddl-auto=validate` in all environments; files are immutable once applied
6. **Angular core/** — AuthService (JWT decode client-side, signal-based state), authInterceptor (functional, root-registered), errorInterceptor, authGuard + adminGuard (separate guards, login route never guarded)
7. **AiChatPanelComponent** — Child of TicketDetailComponent; not routed; receives `ticketId` as `@Input()`; "Apply to Ticket" is the only path for AI content to reach ticket notes

**Database schema — five tables plus audit_log:**
- All timestamps `TIMESTAMPTZ`; all enums `VARCHAR` with `@Enumerated(EnumType.STRING)` on entities
- `version INTEGER NOT NULL DEFAULT 0` on `tickets` for optimistic locking
- Composite index `(ticket_id, created_at DESC)` on both `ticket_notes` and `audit_log` from V1

### Critical Pitfalls

Top five silent/high-consequence pitfalls (no compile error, significant runtime consequences):

1. **JWT filter double-registration** — `@Component` on `JwtAuthFilter` registers it twice. Prevention: instantiate inside `SecurityFilterChain` via constructor injection; no `@Component`.

2. **JPA entities in JSON responses** — Lazy collections trigger N+1 queries or `LazyInitializationException`. Prevention: always map to DTOs in the service layer; `spring.jpa.open-in-view=false` from day one.

3. **Optimistic locking silently disabled** — `@Version` does nothing if `version` is absent from the update DTO. Prevention: `version` is a required field in every update request DTO; never re-fetch before saving.

4. **AI call blocking Tomcat threads** — Synchronous 60-second OpenRouter call holds a thread for its duration; starvation at ~14 concurrent AI users with default 200-thread pool. Prevention: `spring.threads.virtual.enabled=true` — one line, must be set explicitly.

5. **Flyway migration edited after first run** — Any edit to an applied `V__` file causes `checksum mismatch` on startup, breaking all environments. Prevention: migration files are immutable once committed; `spring.flyway.clean-disabled=true` in all non-dev profiles.

**Additional must-not-miss:**
- CORS pre-flight rejected by Spring Security: configure `CorsConfigurationSource` bean and wire it into `.cors()` in the filter chain.
- Context window accumulation: send only the last N messages (default 20) to OpenRouter; never the full unbounded history.
- Route guard infinite redirect loop: never apply `canActivate` to the `/login` route.
- `@Builder` + Jackson deserialization failure: add `@Jacksonized` to all DTO classes using `@Builder`.
- Docker Compose networking: use Compose service name (`db`), not `localhost`, in the datasource URL; use `condition: service_healthy`.

---

## Implications for Roadmap

Suggested phase structure based on dependency analysis from ARCHITECTURE.md build order section.

### Phase 1: Infrastructure and Auth Foundation
**Rationale:** Auth is a hard dependency for every other backend feature. CORS and JWT filter pattern must be correct here — both are sources of critical pitfalls.
**Delivers:** Docker Compose + PostgreSQL, Flyway migrations V1–V2 (users + tickets schema), JwtService, JwtAuthFilter (correctly registered), SecurityConfig with CORS, AuthController (login + me), seed users.
**Addresses:** Authentication, role-based authorization backbone.
**Avoids:** JWT double-registration, CORS pre-flight rejection, Spring Security 6 API removals.
**Research flag:** Standard patterns — skip research phase. STACK.md and ARCHITECTURE.md provide exact code.

### Phase 2: Ticket Core (Backend)
**Rationale:** Tickets are the central entity. State machine and optimistic locking must be correct here — retrofitting is expensive.
**Delivers:** Ticket entity with `@Version`, TicketService (CRUD + state machine + closed-ticket guard + 409 handler), TicketController, TicketMapper, TicketNote flow, AuditLog on status change, Flyway V3–V4, Testcontainers integration tests.
**Addresses:** All ticket CRUD features, status transitions, internal notes, audit trail.
**Avoids:** JPA entities in responses (`open-in-view=false` day one), optimistic locking DTO omission, unpaginated list endpoint.
**Research flag:** Standard patterns — skip research phase.

### Phase 3: AI Chat (Backend)
**Rationale:** AI integration depends on stable ticket entity. Fake provider must come first to unblock frontend and tests. Virtual threads must be enabled before real AI calls.
**Delivers:** ChatSession + ChatMessage entities/repos, AiChatService interface, FakeAiChatService (default for all tests), PromptTemplates class (5 versioned templates), AiChatController, OpenRouterAiChatService, context window truncation logic, OpenRouterProperties configuration.
**Addresses:** AI chat, all five quick-action prompts, apply-as-note endpoint, AI message audit trail.
**Avoids:** API key in logs, thread blocking (virtual threads), context window overflow, fake provider not activated in tests.
**Research flag:** Context window budget heuristic may need validation against the configured model's actual context limit during planning.

### Phase 4: Dashboard (Backend)
**Rationale:** Read-only aggregation on top of the complete ticket schema. No novel patterns.
**Delivers:** DashboardService (counts by status/priority, my open tickets, critical/high >24h, AI interactions today), DashboardController, DashboardStatsResponse DTO.
**Addresses:** Dashboard spec.
**Research flag:** Standard patterns — skip research phase.

### Phase 5: Frontend Shell and Auth
**Rationale:** Angular shell and auth pages are independent of ticket/AI backend. Interceptors and guards must be set up correctly here — the route guard infinite loop pitfall lives in this phase.
**Delivers:** Angular scaffold, AppComponent + router-outlet, environment files, proxy.conf.json, AuthService (signal-based, sessionStorage), authInterceptor (functional, root-registered), errorInterceptor, authGuard + adminGuard (separate), login page with reactive form, lazy-loaded route config.
**Addresses:** Frontend authentication, route protection, role-based UI.
**Avoids:** Interceptor at component level, route guard infinite loop, localhost hardcoded in prod environment.
**Research flag:** Standard patterns — STACK.md provides exact TypeScript patterns.

### Phase 6: Frontend Tickets
**Rationale:** Core user workflow. Depends on Angular shell (Phase 5) and ticket backend (Phase 2).
**Delivers:** Ticket TypeScript models, TicketService, TicketNoteService, ticket list page (pagination + filters + URL persistence), create ticket page (reactive form, validation gated on `touched || submitted`), ticket detail page (three-zone layout), internal note composer.
**Addresses:** Ticket list, ticket detail, create ticket, internal notes, activity timeline display.
**Research flag:** Standard patterns — skip research phase.

### Phase 7: Frontend AI Chat
**Rationale:** AI panel is a child component inside ticket detail. Depends on Phase 6 (ticket detail page) and Phase 3 (AI chat backend).
**Delivers:** AiChatService (Angular), AiChatPanelComponent (collapsible drawer inside ticket detail, `ticketId` as `@Input()`), five quick-action buttons, message thread with role indicators + timestamps + model name, "Apply as Note" button, inline error state with retry.
**Addresses:** AI chat panel, five prompts, human-in-the-loop apply action, recoverable error UX.
**Research flag:** Collapsible drawer UX is straightforward. Token display and streaming indicator design decisions during planning.

### Phase 8: Frontend Dashboard and Admin
**Rationale:** Dashboard is the last frontend feature; depends on Phase 4. Admin user list is simple read-only.
**Delivers:** Dashboard page (all top-priority widgets, each card links to filtered ticket list), admin user list page (ADMIN only, read-only).
**Research flag:** Standard patterns — skip research phase.

### Phase 9: Polish, Tests, and Docs
**Rationale:** Tests need all features complete. Documentation needs the full system to describe.
**Delivers:** Unit tests (status transitions, prompt builder, AuthService, ticket list component), integration tests (ticket API, security tests) with Testcontainers singleton pattern, `@ActiveProfiles("test")` + `ai.provider=fake` in test properties, README (setup, architecture, AI integration notes, known limitations), `.env.example`, Makefile.
**Avoids:** Testcontainers container stopped between test classes (singleton base class), Spring Security filters not applied in MockMvc, `@WithMockUser` role prefix mismatch.
**Research flag:** Testcontainers singleton pattern is critical — use it from the start, not retrofitted.

### Phase Ordering Rationale

- Auth before everything: every subsequent endpoint requires a valid JWT to test.
- Fake AI provider before real AI: unblocks frontend and integration tests; real client should never be used in tests.
- Backend phases before matching frontend phases: Angular services call real backend endpoints.
- Migrations are immutable from Phase 1: establish the policy on day one.
- `open-in-view=false` from Phase 2: catches lazy-load bugs early before they become production 500s.

### Research Flags

Phases needing deeper research during planning:
- **Phase 3 (AI Chat):** Context window truncation budget — validate token estimates against the configured model's actual context limit; decide on truncation strategy (drop oldest vs summarize).

Phases with standard patterns (skip research-phase):
- Phase 1, Phase 2, Phase 4, Phase 5, Phase 6, Phase 8, Phase 9 — all well-documented with HIGH-confidence sources.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All technologies mandated; research confirmed exact API patterns using official docs. |
| Features | HIGH | Freshdesk/Zendesk anatomy confirms spec completeness; status transitions grounded in industry patterns. |
| Architecture | HIGH | Layered architecture and ConditionalOnProperty AI abstraction are established Spring Boot patterns verified by official docs. |
| Pitfalls | HIGH | Multiple independent sources confirmed each critical pitfall; these are documented production failures, not theoretical. |

**Overall confidence: HIGH**

### Gaps to Address

- **Token counting heuristic:** The "1 token per 4 chars" estimate is approximate. Validate against the configured model's context limit during Phase 3 planning. For `gpt-4o-mini` (128k context) overflow is unlikely; for Mistral 7B (8k) it is a real constraint.
- **Prompt injection delimiters:** PITFALLS.md recommends XML-like delimiters for untrusted ticket content. Decide on delimiter style and whether to add an explicit anti-injection instruction in the system prompt during Phase 3 planning.
- **JWT storage trade-off:** `localStorage` has XSS exposure risk; `sessionStorage` is the recommended default for this internal tool. Make the decision explicit in Phase 5 and document it in the README.
- **Flyway clean in dev profile:** Dev Docker Compose should allow `flyway clean` for reset convenience; all other profiles need `clean-disabled=true`. Establish this profile split in Phase 1.

---

## Sources

### Primary (HIGH confidence)
- Spring Boot 3.4 reference: https://docs.spring.io/spring-boot/3.4/reference/
- Spring Boot Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Angular HTTP interceptors: https://angular.dev/guide/http/interceptors
- Angular CanActivateFn: https://angular.dev/api/router/CanActivateFn
- MapStruct: https://mapstruct.org/
- jjwt: https://github.com/jwtk/jjwt
- OpenRouter API reference: https://openrouter.ai/docs/api/reference/overview
- springdoc-openapi: https://springdoc.org/
- Spring Security CORS: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html

### Secondary (MEDIUM confidence)
- Freshdesk ticket anatomy: https://support.freshdesk.com/support/solutions/articles/37588-anatomy-of-a-ticket
- Zendesk agent interface: https://support.zendesk.com/hc/en-us/articles/4408883355546
- JPA optimistic locking: https://www.baeldung.com/jpa-optimistic-locking
- JPA N+1 problem: https://www.baeldung.com/spring-hibernate-n1-problem
- Jackson + Lombok deserialization: https://www.baeldung.com/java-jackson-deserialization-lombok
- Spring Security integration tests: https://www.baeldung.com/spring-security-integration-tests
- Testcontainers lifecycle: https://testcontainers.com/guides/testcontainers-container-lifecycle/
- AI chat UX patterns: https://uxpatterns.dev/patterns/ai-intelligence/ai-chat
- Human-in-the-loop UX: https://alhena.ai/blog/designing-trust-hybrid-ai-human-support/
- Prompt injection: https://owasp.org/www-community/attacks/PromptInjection
- Docker Compose networking: https://medium.com/@python-javascript-php-html-css/fixing-jdbc-connection-problems-in-docker-compose-using-hibernate-and-postgresql-6ab38bf6c95c

---
*Research completed: 2026-05-14*
*Ready for roadmap: yes*
