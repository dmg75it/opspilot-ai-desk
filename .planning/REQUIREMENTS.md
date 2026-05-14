# Requirements: OpsPilot AI Desk

**Defined:** 2026-05-14
**Core Value:** Field operators can create tickets, get AI-powered assistance, and track status from creation to close through a secure, auditable interface.

## v1 Requirements

### Authentication

- [ ] **AUTH-01**: User can log in with email and password and receive a JWT token
- [ ] **AUTH-02**: User can access a "current user" endpoint to retrieve their profile and role
- [ ] **AUTH-03**: JWT token is validated on every protected request (filter chain)
- [ ] **AUTH-04**: ADMIN role can access all resources; OPERATOR role is restricted to their own operations
- [ ] **AUTH-05**: Passwords are hashed with BCrypt in the database
- [ ] **AUTH-06**: Seed users are created on first run (admin@example.com / ADMIN, operator@example.com / OPERATOR)
- [ ] **AUTH-07**: Frontend stores JWT and attaches it to all API requests via HTTP interceptor
- [ ] **AUTH-08**: Frontend route guard redirects unauthenticated users to login page
- [ ] **AUTH-09**: Frontend route guard enforces role-based access to admin pages

### Tickets

- [ ] **TICK-01**: Operator can create a ticket with title, description, priority, category, and optional external reference
- [ ] **TICK-02**: Operator can list tickets with pagination (page, size) and filters (status, priority, category, assignee)
- [ ] **TICK-03**: Operator can view a ticket by ID including all notes and status history
- [ ] **TICK-04**: Operator can update ticket metadata (title, description, priority, category, external reference)
- [ ] **TICK-05**: Status transitions are validated server-side: NEW→IN_PROGRESS, IN_PROGRESS→WAITING_FOR_CUSTOMER, IN_PROGRESS→RESOLVED, WAITING_FOR_CUSTOMER→IN_PROGRESS, RESOLVED→CLOSED
- [ ] **TICK-06**: ADMIN can force any status transition; OPERATOR follows allowed transition rules
- [ ] **TICK-07**: Operator can assign a ticket to another operator
- [ ] **TICK-08**: Closed tickets cannot be edited by OPERATORs (only ADMIN)
- [ ] **TICK-09**: Optimistic locking prevents concurrent update conflicts (HTTP 409 on conflict)
- [ ] **TICK-10**: External reference is unique when present (DB constraint)
- [ ] **TICK-11**: Ticket validation: title max 150 chars (required), description max 5000 chars (required)
- [ ] **TICK-12**: Audit trail events are recorded for: ticket created, status changed, assigned

### Ticket Notes

- [ ] **NOTE-01**: Operator can add an INTERNAL note to a ticket
- [ ] **NOTE-02**: System records SYSTEM notes automatically for audit events (status change, assignment)
- [ ] **NOTE-03**: AI-generated summaries are stored as AI_SUMMARY notes when explicitly applied by operator
- [ ] **NOTE-04**: Notes list shows author, visibility type, body, and creation timestamp

### AI Chat Assistant

- [ ] **AI-01**: Operator can open or retrieve an AI chat session scoped to a specific ticket
- [ ] **AI-02**: Operator can send a message to the AI assistant within a session
- [ ] **AI-03**: Operator can list all messages in a chat session
- [ ] **AI-04**: AI can generate a ticket summary using a versioned prompt template
- [ ] **AI-05**: AI can suggest the next action for a ticket using a versioned prompt template
- [ ] **AI-06**: AI can draft a customer-facing reply using a versioned prompt template
- [ ] **AI-07**: AI can identify missing information in the ticket using a versioned prompt template
- [ ] **AI-08**: AI can classify priority and category using a versioned prompt template
- [ ] **AI-09**: Operator can apply an AI-generated summary as an AI_SUMMARY note (explicit action required)
- [ ] **AI-10**: Fake/mock AI provider is available for local development and tests (AI_PROVIDER=fake)
- [ ] **AI-11**: AI calls are logged with model used, elapsed time, and token usage (no API key in logs)
- [ ] **AI-12**: OpenRouter failures surface a clear, recoverable error to the frontend
- [ ] **AI-13**: AI responses never automatically modify ticket status or data

### Dashboard

- [ ] **DASH-01**: Dashboard shows ticket counts grouped by status
- [ ] **DASH-02**: Dashboard shows ticket counts grouped by priority
- [ ] **DASH-03**: Dashboard shows operator's own assigned open tickets (list with drill-through)
- [ ] **DASH-04**: Dashboard shows recently updated tickets (last 10)
- [ ] **DASH-05**: Dashboard shows count of AI interactions today

### Frontend Pages

- [ ] **UI-01**: Login page with email/password form
- [ ] **UI-02**: Dashboard page with status/priority cards and recent tickets table
- [ ] **UI-03**: Ticket list page with pagination, filters, and create button
- [ ] **UI-04**: Ticket detail page showing metadata, notes, and status history
- [ ] **UI-05**: Create ticket page with reactive form and validation
- [ ] **UI-06**: AI chat panel embedded in ticket detail page (collapsible)
- [ ] **UI-07**: Admin user list page (ADMIN role only)
- [ ] **UI-08**: Loading states and error handling on all API calls
- [ ] **UI-09**: Responsive layout with basic navigation sidebar or header

### Infrastructure and Developer Experience

- [ ] **INFRA-01**: Docker Compose file with PostgreSQL service
- [ ] **INFRA-02**: Flyway migrations create all tables with correct constraints and indexes
- [ ] **INFRA-03**: Backend runs via `mvn spring-boot:run` with .env variable support
- [ ] **INFRA-04**: Frontend runs via `npm start` with proxy to backend
- [ ] **INFRA-05**: `.env.example` documents all required environment variables
- [ ] **INFRA-06**: Makefile with `make up`, `make backend`, `make frontend`, `make test` targets
- [ ] **INFRA-07**: Docker Compose profile for full stack (backend + frontend + postgres)

### Testing

- [ ] **TEST-01**: Unit tests for ticket status transition rules
- [ ] **TEST-02**: Unit tests for AI prompt builder / template versioning
- [ ] **TEST-03**: Integration tests for ticket API (create, list, update, status change) using Testcontainers
- [ ] **TEST-04**: Security tests verifying protected endpoints reject unauthenticated/unauthorized requests
- [ ] **TEST-05**: Frontend unit test for at least one Angular service (e.g., AuthService or TicketService)
- [ ] **TEST-06**: Frontend unit test for at least one Angular component (e.g., LoginComponent)

### Documentation

- [ ] **DOC-01**: README with local setup instructions (prerequisites, env vars, run commands)
- [ ] **DOC-02**: README with architecture overview (layers, components, data flow)
- [ ] **DOC-03**: README with AI integration notes (OpenRouter, fake provider, prompt versioning)
- [ ] **DOC-04**: README with test strategy and how to run tests
- [ ] **DOC-05**: README with known limitations and assumptions

## v2 Requirements

### Notifications
- Real-time websocket notifications for ticket updates
- Email notifications on assignment or status change

### Advanced Features
- File attachments on tickets
- Ticket templates for common issue types
- SLA tracking and breach alerts
- Multi-tenant organization support
- OAuth2 / SSO login

### AI Enhancements
- Streaming AI responses
- AI-suggested ticket routing (auto-assign based on category)
- AI-powered ticket deduplication

## Out of Scope

| Feature | Reason |
|---------|--------|
| WebSocket / real-time push | High complexity, not required for v1 support desk |
| Email notifications | Requires SMTP integration, deferred to v2 |
| File attachments | Storage complexity, out of scope for benchmark |
| Multi-tenant / organizations | Out of scope — single-tenant benchmark project |
| Mobile app | Web-first; mobile is a separate product decision |
| AI auto-modifying tickets | Explicit safety requirement: AI suggestions require human confirmation |
| OAuth2 / SSO | Local users sufficient for benchmark scope |
| Audit log querying UI | Audit records stored; UI for searching deferred |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 to AUTH-06 | Phase 1 | Pending |
| AUTH-07 to AUTH-09 | Phase 5 | Pending |
| TICK-01 to TICK-12 | Phase 2 | Pending |
| NOTE-01 to NOTE-04 | Phase 2 | Pending |
| AI-01 to AI-13 | Phase 3 | Pending |
| DASH-01 to DASH-05 | Phase 4 | Pending |
| UI-01 | Phase 5 | Pending |
| UI-02 | Phase 8 | Pending |
| UI-03 to UI-05 | Phase 6 | Pending |
| UI-06 | Phase 7 | Pending |
| UI-07 | Phase 8 | Pending |
| UI-08 to UI-09 | Phase 5 | Pending |
| INFRA-01 to INFRA-03 | Phase 1 | Pending |
| INFRA-04 | Phase 5 | Pending |
| INFRA-05 | Phase 9 | Pending |
| INFRA-06 to INFRA-07 | Phase 1 | Pending |
| TEST-01 | Phase 2 | Pending |
| TEST-02 | Phase 3 | Pending |
| TEST-03 to TEST-04 | Phase 9 | Pending |
| TEST-05 to TEST-06 | Phase 5 | Pending |
| DOC-01 to DOC-05 | Phase 9 | Pending |

**Coverage:**
- v1 requirements: 70 total
- Mapped to phases: 70
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-14*
*Last updated: 2026-05-14 — traceability updated after roadmap creation*
