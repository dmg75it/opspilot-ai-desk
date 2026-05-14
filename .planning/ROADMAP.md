# Roadmap: OpsPilot AI Desk

**Milestone:** v1.0 — Complete Support Desk with AI Assistant
**Phases:** 9
**Requirements:** 57 v1 requirements

## Overview

Build a production-oriented field operations support desk from the ground up: infrastructure and auth first, then ticket core, then AI chat backend, then dashboard backend, then the Angular frontend shell and each feature area in order, finishing with polish, tests, and documentation. Every phase delivers a coherent, independently verifiable capability that unblocks the next.

## Phases

- [ ] **Phase 1: Infrastructure and Auth Foundation** - Maven project, Docker Compose, Flyway migrations, Spring Security JWT, seeded users, CORS, Makefile
- [ ] **Phase 2: Ticket Core Backend** - Ticket entity, status state machine, CRUD endpoints, notes, optimistic locking, pagination, audit trail, unit tests for transitions
- [ ] **Phase 3: AI Chat Backend** - AiChatService interface, FakeAiChatService, OpenRouterChatService, chat entities, 5 versioned prompt templates, apply-as-note endpoint, unit tests for prompt builder
- [ ] **Phase 4: Dashboard Backend** - Aggregation endpoints for status, priority, my tickets, recent tickets, AI interaction count
- [ ] **Phase 5: Frontend Shell and Auth** - Angular scaffold, standalone components, login page, JWT interceptor, route guards, responsive layout, frontend auth unit tests
- [ ] **Phase 6: Frontend Ticket Management** - Ticket list (paginated, filtered), ticket detail, create ticket, status change UI, notes display
- [ ] **Phase 7: Frontend AI Chat Panel** - Collapsible AI panel in ticket detail, 5 quick-action buttons, apply-as-note, recoverable error UX
- [ ] **Phase 8: Frontend Dashboard and Admin** - Dashboard page with cards and recent table, admin user list page
- [ ] **Phase 9: Polish, Tests, and Documentation** - Remaining integration tests (Testcontainers), security tests, README documentation, .env.example, BENCHMARK_REPORT.md

## Phase Details

### Phase 1: Infrastructure and Auth Foundation
**Goal**: Stand up a runnable backend with correct JWT authentication, role-based authorization, seeded users, and a reproducible local development environment.
**Depends on**: Nothing (first phase)
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, INFRA-01, INFRA-02, INFRA-03, INFRA-06, INFRA-07
**Success Criteria** (what must be TRUE):
  1. `make up` starts PostgreSQL and `mvn spring-boot:run` starts the backend without errors
  2. POST /api/auth/login with admin@example.com/admin123 returns a JWT token
  3. GET /api/auth/me with a valid JWT returns the current user's email and role
  4. A request to any protected endpoint without a JWT receives HTTP 401
  5. CORS pre-flight requests from localhost:4200 receive correct headers
**Plans**: 4 plans
Plans:
- [ ] 01-01-PLAN.md — Maven project scaffold, Docker Compose, Makefile, .env.example, .gitignore
- [x] 01-02-PLAN.md — Flyway migrations V1 (users table) and V2 (seed users)
- [ ] 01-03-PLAN.md — Spring Security JWT auth: entity, JwtService, JwtAuthFilter, SecurityConfig, AuthController
- [ ] 01-04-PLAN.md — Integration and verification: Testcontainers, JwtServiceTest, AuthControllerIntegrationTest

### Phase 2: Ticket Core Backend
**Goal**: Deliver a fully functional ticket management backend with validated status transitions, optimistic locking, notes, an audit trail, and unit tests for transition rules.
**Depends on**: Phase 1
**Requirements**: TICK-01, TICK-02, TICK-03, TICK-04, TICK-05, TICK-06, TICK-07, TICK-08, TICK-09, TICK-10, TICK-11, TICK-12, NOTE-01, NOTE-02, NOTE-03, NOTE-04, TEST-01
**Success Criteria** (what must be TRUE):
  1. Operator can create a ticket and retrieve it by ID with all fields present
  2. Attempting an invalid status transition (e.g. NEW to RESOLVED) returns HTTP 422 with a descriptive error
  3. A concurrent update with a stale version field returns HTTP 409
  4. Closed tickets cannot be edited by an OPERATOR (HTTP 403)
  5. Every status change and assignment is recorded as a SYSTEM note in the ticket's note list
**Plans**: TBD

### Phase 3: AI Chat Backend
**Goal**: Deliver a complete AI chat backend with a fake provider for tests, five versioned prompt templates, unit tests for the prompt builder, and an explicit apply-as-note endpoint.
**Depends on**: Phase 2
**Requirements**: AI-01, AI-02, AI-03, AI-04, AI-05, AI-06, AI-07, AI-08, AI-09, AI-10, AI-11, AI-12, AI-13, TEST-02
**Success Criteria** (what must be TRUE):
  1. With AI_PROVIDER=fake, all five quick-action endpoints return deterministic responses without calling OpenRouter
  2. With AI_PROVIDER=openrouter and a valid key, a chat message returns a non-empty AI response and logs model and elapsed time (no API key in logs)
  3. POST /api/ai/sessions/{id}/apply-note creates an AI_SUMMARY note on the ticket and requires explicit operator action
  4. AI responses never modify ticket status or data directly
  5. An OpenRouter timeout or error returns a structured error response (not a 500 stack trace)
**Plans**: TBD

### Phase 4: Dashboard Backend
**Goal**: Deliver read-only aggregation endpoints that supply all data needed by the dashboard page.
**Depends on**: Phase 2
**Requirements**: DASH-01, DASH-02, DASH-03, DASH-04, DASH-05
**Success Criteria** (what must be TRUE):
  1. GET /api/dashboard returns ticket counts grouped by status and by priority
  2. GET /api/dashboard returns the calling operator's own assigned open tickets
  3. GET /api/dashboard returns the 10 most recently updated tickets
  4. GET /api/dashboard returns the count of AI chat messages created today
**Plans**: TBD

### Phase 5: Frontend Shell and Auth
**Goal**: Deliver a working Angular application shell with JWT-based auth, functional interceptors, route guards, a responsive layout, and passing unit tests for auth service and login component.
**Depends on**: Phase 1
**Requirements**: AUTH-07, AUTH-08, AUTH-09, UI-01, UI-08, UI-09, INFRA-04, TEST-05, TEST-06
**Success Criteria** (what must be TRUE):
  1. Unauthenticated user visiting any protected route is redirected to /login
  2. After login, JWT is persisted and attached to subsequent API requests automatically
  3. OPERATOR visiting an admin-only route receives a "not authorized" redirect
  4. Login page displays form validation errors on submit with empty fields
  5. Navigation sidebar or header is visible on all authenticated pages with a working logout action
**Plans**: TBD
**UI hint**: yes

### Phase 6: Frontend Ticket Management
**Goal**: Deliver complete ticket list, ticket detail, and create-ticket pages with pagination, filters, status change UI, and notes display.
**Depends on**: Phase 5, Phase 2
**Requirements**: UI-03, UI-04, UI-05
**Success Criteria** (what must be TRUE):
  1. Operator can browse paginated ticket list and filter by status, priority, and category
  2. Operator can open a create-ticket form, submit it, and see the new ticket in the list
  3. Ticket detail page shows all metadata, notes with visibility labels, and status history
  4. Operator can trigger a status change from the ticket detail page and see the updated status immediately
**Plans**: TBD
**UI hint**: yes

### Phase 7: Frontend AI Chat Panel
**Goal**: Deliver a collapsible AI chat panel embedded in ticket detail with five quick-action buttons, message thread, apply-as-note action, and recoverable error states.
**Depends on**: Phase 6, Phase 3
**Requirements**: UI-06
**Success Criteria** (what must be TRUE):
  1. AI chat panel opens and closes from the ticket detail page without leaving the page
  2. Clicking any of the five quick-action buttons (Summarize, Suggest Next Action, Draft Reply, Missing Info, Classify) sends a request and displays the AI response in the thread
  3. Clicking "Apply as Note" on an AI response creates an AI_SUMMARY note on the ticket (visible in notes list)
  4. When OpenRouter fails, the panel shows an inline error message with a retry option (no unhandled exception, no blank state)
**Plans**: TBD
**UI hint**: yes

### Phase 8: Frontend Dashboard and Admin
**Goal**: Deliver the dashboard page with status/priority cards and recent tickets, plus the admin user list page accessible only to ADMIN role.
**Depends on**: Phase 5, Phase 4
**Requirements**: UI-02, UI-07
**Success Criteria** (what must be TRUE):
  1. Dashboard page loads and displays ticket count cards for each status and priority
  2. Dashboard page shows the operator's own assigned open tickets and the 10 most recently updated tickets
  3. Dashboard count cards link through to the ticket list pre-filtered by the relevant status or priority
  4. Admin user list page is accessible to ADMIN role and lists all users; OPERATOR receives a redirect
**Plans**: TBD
**UI hint**: yes

### Phase 9: Polish, Tests, and Documentation
**Goal**: Complete all remaining integration tests, security tests, documentation, and environment files so the project fully meets the definition of done.
**Depends on**: Phase 8
**Requirements**: TEST-03, TEST-04, DOC-01, DOC-02, DOC-03, DOC-04, DOC-05, INFRA-05
**Success Criteria** (what must be TRUE):
  1. `make test` runs all backend tests (unit + integration via Testcontainers) with a green result
  2. Security tests confirm that unauthenticated and under-authorized requests to all protected endpoints are rejected
  3. README contains setup instructions, architecture overview, AI integration notes, test strategy, and known limitations
  4. `.env.example` documents all required environment variables with descriptions
  5. BENCHMARK_REPORT.md is updated with final observations on the GSD workflow execution
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Infrastructure and Auth Foundation | 1/4 | In Progress|  |
| 2. Ticket Core Backend | 0/? | Not started | - |
| 3. AI Chat Backend | 0/? | Not started | - |
| 4. Dashboard Backend | 0/? | Not started | - |
| 5. Frontend Shell and Auth | 0/? | Not started | - |
| 6. Frontend Ticket Management | 0/? | Not started | - |
| 7. Frontend AI Chat Panel | 0/? | Not started | - |
| 8. Frontend Dashboard and Admin | 0/? | Not started | - |
| 9. Polish, Tests, and Documentation | 0/? | Not started | - |
