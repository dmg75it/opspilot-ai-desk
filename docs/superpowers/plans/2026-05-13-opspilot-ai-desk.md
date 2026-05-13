# OpsPilot AI Desk — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-oriented full-stack web application for transport/logistics field support with JWT auth, ticket workflow, and OpenRouter AI chat.

**Architecture:** Spring Boot 3.3 backend (Java 21 features on JDK 25) + Angular 17 frontend in a monorepo. PostgreSQL via Docker; Flyway migrations for schema; JWT for stateless auth; OpenRouter for AI with a fake provider profile for tests/dev.

**Tech Stack:** Java 21, Spring Boot 3.3, Maven, Lombok, Spring Security, Spring Data JPA, Flyway, PostgreSQL, JJWT, Testcontainers, JUnit 5 / Angular 17, TypeScript, RxJS, Angular Material

---

## Assumptions

- Backend port: 8080, Frontend port: 4200
- Java package root: `com.opspilot.desk`
- DB name: `opspilot_desk`, user: `opspilot`, password: `opspilot`
- JWT secret from env `JWT_SECRET`, expiry 24h
- Default OpenRouter model: `openai/gpt-3.5-turbo`
- Fake AI profile activated by `FAKE_AI=true` env or Spring profile `fake-ai`
- Seed data inserted via Flyway migration V2

---

## Phase 1: Project Foundation

### Task 1: Repo structure, .gitignore, docker-compose, .env.example, Makefile

**Files:**
- Create: `.gitignore`
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `Makefile`

- [ ] Create `.gitignore` covering Java/Maven, Node/Angular, IDE, env files
- [ ] Create `docker-compose.yml` with postgres service + backend + frontend profiles
- [ ] Create `.env.example` with all env variables documented
- [ ] Create `Makefile` with targets: `up`, `down`, `backend-run`, `frontend-run`, `test`
- [ ] Commit: `chore: project foundation - docker, env, makefile`

---

## Phase 2: Backend Foundation

### Task 2: Maven pom.xml and application skeleton

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/opspilot/desk/OpsPilotDeskApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-fake-ai.yml`

- [ ] Write `pom.xml` with Spring Boot 3.3.x parent, Java 21, all required deps
- [ ] Write main application class
- [ ] Write `application.yml` with DB, JPA, Flyway, JWT, OpenRouter, CORS config
- [ ] Write `application-fake-ai.yml` overriding AI provider bean
- [ ] Verify project compiles: `mvn -f backend/pom.xml compile -q`
- [ ] Commit: `feat: backend maven setup`

### Task 3: Flyway migrations

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed_users.sql`

- [ ] Write V1 migration: users, tickets, ticket_notes, chat_sessions, chat_messages, ticket_audit tables
- [ ] Write V2 migration: seed admin + operator users with bcrypt passwords
- [ ] Verify migrations valid SQL (syntax check)
- [ ] Commit: `feat: flyway migrations - schema and seed data`

### Task 4: Entity layer

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/entity/User.java`
- Create: `backend/src/main/java/com/opspilot/desk/entity/Ticket.java`
- Create: `backend/src/main/java/com/opspilot/desk/entity/TicketNote.java`
- Create: `backend/src/main/java/com/opspilot/desk/entity/ChatSession.java`
- Create: `backend/src/main/java/com/opspilot/desk/entity/ChatMessage.java`
- Create: `backend/src/main/java/com/opspilot/desk/entity/TicketAudit.java`
- Create: `backend/src/main/java/com/opspilot/desk/entity/enums/*.java` (all enums)

- [ ] Write all enums: `Role`, `TicketStatus`, `TicketPriority`, `TicketCategory`, `NoteVisibility`, `MessageRole`
- [ ] Write `User` entity with `UserDetails` implementation
- [ ] Write `Ticket` entity with optimistic locking (`@Version`)
- [ ] Write `TicketNote` entity
- [ ] Write `ChatSession` + `ChatMessage` entities
- [ ] Write `TicketAudit` entity
- [ ] Verify compilation: `mvn -f backend/pom.xml compile -q`
- [ ] Commit: `feat: entity layer`

### Task 5: Repository layer

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/repository/*.java`

- [ ] Write `UserRepository` with `findByEmail`
- [ ] Write `TicketRepository` with filter+pagination query
- [ ] Write `TicketNoteRepository`
- [ ] Write `ChatSessionRepository` with `findByTicketId`
- [ ] Write `ChatMessageRepository`
- [ ] Write `TicketAuditRepository`
- [ ] Commit: `feat: repository layer`

---

## Phase 3: Security / JWT

### Task 6: JWT utility and Spring Security config

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/security/JwtUtil.java`
- Create: `backend/src/main/java/com/opspilot/desk/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/opspilot/desk/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/opspilot/desk/security/UserDetailsServiceImpl.java`

- [ ] Write `JwtUtil`: generate, validate, extract claims — never log the secret
- [ ] Write `JwtAuthenticationFilter`: read `Authorization: Bearer` header
- [ ] Write `SecurityConfig`: permit login endpoint, require auth for rest, CORS config
- [ ] Write `UserDetailsServiceImpl`: load by email
- [ ] Write unit test for `JwtUtil`
- [ ] Commit: `feat: JWT security`

### Task 7: Auth endpoints

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/dto/auth/*.java`
- Create: `backend/src/main/java/com/opspilot/desk/controller/AuthController.java`
- Create: `backend/src/main/java/com/opspilot/desk/service/AuthService.java`

- [ ] Write `LoginRequest`, `AuthResponse`, `UserProfileResponse` DTOs
- [ ] Write `AuthService`: authenticate, build JWT response
- [ ] Write `AuthController`: `POST /api/auth/login`, `GET /api/auth/me`
- [ ] Write integration test for login (Testcontainers)
- [ ] Commit: `feat: auth endpoints`

---

## Phase 4: Ticket API

### Task 8: Ticket DTOs and status transition validator

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/dto/ticket/*.java`
- Create: `backend/src/main/java/com/opspilot/desk/service/TicketStatusTransitionValidator.java`

- [ ] Write `CreateTicketRequest`, `UpdateTicketRequest`, `ChangeStatusRequest`, `AssignTicketRequest`, `TicketResponse`, `TicketListResponse`, `TicketFilterRequest` DTOs
- [ ] Write `TicketStatusTransitionValidator` with allowed transitions map
- [ ] Write unit tests for all valid and invalid transitions
- [ ] Commit: `feat: ticket DTOs and status validator`

### Task 9: Ticket service and controller

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/service/TicketService.java`
- Create: `backend/src/main/java/com/opspilot/desk/controller/TicketController.java`
- Create: `backend/src/main/java/com/opspilot/desk/mapper/TicketMapper.java`

- [ ] Write `TicketService`: create, list (paginated+filtered), get, update, changeStatus, assign, close, addNote
- [ ] Write `TicketMapper` (MapStruct or manual)
- [ ] Write `TicketController` with all endpoints under `/api/tickets`
- [ ] Write integration tests for ticket CRUD and status transitions
- [ ] Commit: `feat: ticket service and controller`

### Task 10: Ticket audit and notes

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/service/TicketAuditService.java`
- Create: `backend/src/main/java/com/opspilot/desk/controller/TicketNoteController.java`

- [ ] Write `TicketAuditService`: record audit events for status changes, assignments
- [ ] Wire audit service into ticket service
- [ ] Write `TicketNoteController`: `POST /api/tickets/{id}/notes`, `GET /api/tickets/{id}/notes`
- [ ] Commit: `feat: ticket audit and notes`

---

## Phase 5: AI Integration

### Task 11: OpenRouter client abstraction

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/ai/AiChatProvider.java` (interface)
- Create: `backend/src/main/java/com/opspilot/desk/ai/OpenRouterChatProvider.java`
- Create: `backend/src/main/java/com/opspilot/desk/ai/FakeAiChatProvider.java`
- Create: `backend/src/main/java/com/opspilot/desk/ai/AiConfig.java`
- Create: `backend/src/main/java/com/opspilot/desk/ai/PromptTemplates.java`

- [ ] Write `AiChatProvider` interface with `chat(systemPrompt, messages)` method
- [ ] Write `OpenRouterChatProvider`: HTTP call to OpenRouter, structured logging, no key leak
- [ ] Write `FakeAiChatProvider`: deterministic responses for tests
- [ ] Write `AiConfig`: conditional bean registration based on `fake-ai` profile
- [ ] Write `PromptTemplates`: versioned prompt strings for summarize, suggest, reply, classify
- [ ] Write unit test for `PromptTemplates`
- [ ] Commit: `feat: OpenRouter AI provider abstraction`

### Task 12: Chat session service and endpoints

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/service/ChatService.java`
- Create: `backend/src/main/java/com/opspilot/desk/controller/ChatController.java`
- Create: `backend/src/main/java/com/opspilot/desk/dto/chat/*.java`

- [ ] Write chat DTOs: `ChatSessionResponse`, `SendMessageRequest`, `ChatMessageResponse`, `AiActionRequest`
- [ ] Write `ChatService`: startOrGetSession, sendMessage, listMessages, generateSummary, generateSuggestedReply, applyAiSummaryAsNote
- [ ] Write `ChatController` under `/api/tickets/{id}/chat`
- [ ] Commit: `feat: chat service and endpoints`

---

## Phase 6: Dashboard and Admin endpoints

### Task 13: Dashboard and user management

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/controller/DashboardController.java`
- Create: `backend/src/main/java/com/opspilot/desk/controller/UserController.java`
- Create: `backend/src/main/java/com/opspilot/desk/dto/dashboard/DashboardResponse.java`

- [ ] Write `DashboardController`: `GET /api/dashboard` returning tickets by status/priority, my open tickets, recent, AI count
- [ ] Write `UserController`: `GET /api/users` (ADMIN only) for operator list
- [ ] Commit: `feat: dashboard and user endpoints`

---

## Phase 7: OpenAPI and exception handling

### Task 14: Global error handling and OpenAPI

**Files:**
- Create: `backend/src/main/java/com/opspilot/desk/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/opspilot/desk/exception/*.java`
- Create: `backend/src/main/java/com/opspilot/desk/config/OpenApiConfig.java`

- [ ] Write domain exceptions: `TicketNotFoundException`, `InvalidStatusTransitionException`, `OptimisticLockException`, `AiProviderException`
- [ ] Write `GlobalExceptionHandler` with `@RestControllerAdvice`
- [ ] Write `OpenApiConfig` for Swagger UI
- [ ] Commit: `feat: error handling and OpenAPI`

---

## Phase 8: Frontend Foundation

### Task 15: Angular project setup

**Files:**
- Create: `frontend/` (Angular workspace)
- Create: `frontend/src/environments/environment.ts`
- Create: `frontend/src/environments/environment.prod.ts`

- [ ] Scaffold Angular 17 app: `ng new frontend --standalone --routing --style=scss --skip-git`
- [ ] Add Angular Material: `ng add @angular/material`
- [ ] Add `@auth0/angular-jwt` or implement JWT interceptor manually
- [ ] Configure environment files with `apiUrl: http://localhost:8080`
- [ ] Commit: `feat: Angular frontend scaffold`

### Task 16: Core services and auth

**Files:**
- Create: `frontend/src/app/core/services/auth.service.ts`
- Create: `frontend/src/app/core/services/ticket.service.ts`
- Create: `frontend/src/app/core/services/chat.service.ts`
- Create: `frontend/src/app/core/services/dashboard.service.ts`
- Create: `frontend/src/app/core/interceptors/auth.interceptor.ts`
- Create: `frontend/src/app/core/interceptors/error.interceptor.ts`
- Create: `frontend/src/app/core/guards/auth.guard.ts`
- Create: `frontend/src/app/core/guards/role.guard.ts`
- Create: `frontend/src/app/core/models/*.ts`

- [ ] Write TypeScript models matching backend DTOs
- [ ] Write `AuthService`: login, logout, getCurrentUser, isLoggedIn, hasRole
- [ ] Write `AuthInterceptor`: attach Bearer token to requests
- [ ] Write `ErrorInterceptor`: handle 401 redirect, surface errors
- [ ] Write `AuthGuard` and `RoleGuard`
- [ ] Write unit tests for `AuthService`
- [ ] Commit: `feat: Angular core services`

---

## Phase 9: Frontend Pages

### Task 17: App shell and routing

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Create: `frontend/src/app/layout/shell/shell.component.ts`
- Create: `frontend/src/app/pages/login/login.component.ts`

- [ ] Define routes: `/login`, `/dashboard`, `/tickets`, `/tickets/:id`, `/tickets/new`, `/admin`
- [ ] Write `ShellComponent` with nav sidebar and `<router-outlet>`
- [ ] Write `LoginComponent` with reactive form
- [ ] Commit: `feat: app shell and login page`

### Task 18: Dashboard page

**Files:**
- Create: `frontend/src/app/pages/dashboard/dashboard.component.ts`

- [ ] Write `DashboardComponent`: fetch and display status/priority cards, my tickets table, recent tickets
- [ ] Commit: `feat: dashboard page`

### Task 19: Ticket list and create pages

**Files:**
- Create: `frontend/src/app/pages/tickets/ticket-list/ticket-list.component.ts`
- Create: `frontend/src/app/pages/tickets/ticket-create/ticket-create.component.ts`
- Create: `frontend/src/app/shared/components/ticket-form/ticket-form.component.ts`

- [ ] Write `TicketListComponent`: paginated table with filters (status, priority, category)
- [ ] Write `TicketCreateComponent` using `TicketFormComponent` with reactive form + validation
- [ ] Commit: `feat: ticket list and create pages`

### Task 20: Ticket detail and status management

**Files:**
- Create: `frontend/src/app/pages/tickets/ticket-detail/ticket-detail.component.ts`
- Create: `frontend/src/app/pages/tickets/ticket-detail/ticket-notes/ticket-notes.component.ts`
- Create: `frontend/src/app/pages/tickets/ticket-detail/status-dialog/status-dialog.component.ts`

- [ ] Write `TicketDetailComponent`: show all ticket fields, notes, action buttons
- [ ] Write `TicketNotesComponent`: list notes, add internal note
- [ ] Write `StatusDialogComponent`: change status with validation
- [ ] Commit: `feat: ticket detail page`

### Task 21: AI chat panel

**Files:**
- Create: `frontend/src/app/pages/tickets/ticket-detail/ai-chat/ai-chat.component.ts`
- Create: `frontend/src/app/pages/tickets/ticket-detail/ai-chat/ai-actions/ai-actions.component.ts`

- [ ] Write `AiChatComponent`: message thread, send input, loading state, error display
- [ ] Write `AiActionsComponent`: buttons for summarize, suggest reply, classify — explicit apply action
- [ ] Commit: `feat: AI chat panel`

### Task 22: Admin page and user list

**Files:**
- Create: `frontend/src/app/pages/admin/admin.component.ts`

- [ ] Write `AdminComponent`: user list table (ADMIN only)
- [ ] Commit: `feat: admin page`

---

## Phase 10: Tests

### Task 23: Backend integration tests

**Files:**
- Create: `backend/src/test/java/com/opspilot/desk/integration/TicketApiIntegrationTest.java`
- Create: `backend/src/test/java/com/opspilot/desk/integration/AuthApiIntegrationTest.java`
- Create: `backend/src/test/java/com/opspilot/desk/integration/SecurityIntegrationTest.java`

- [ ] Write Testcontainers-based integration test for ticket CRUD
- [ ] Write auth integration tests (login, me endpoint)
- [ ] Write security tests: 401 without token, 403 wrong role
- [ ] Run tests: `mvn -f backend/pom.xml test`
- [ ] Commit: `test: backend integration tests`

### Task 24: Frontend unit tests

**Files:**
- Create: `frontend/src/app/core/services/auth.service.spec.ts`
- Create: `frontend/src/app/pages/login/login.component.spec.ts`

- [ ] Write `AuthService` unit tests
- [ ] Write `LoginComponent` unit tests
- [ ] Run: `cd frontend && npm test -- --watch=false`
- [ ] Commit: `test: frontend unit tests`

---

## Phase 11: Documentation

### Task 25: README and BENCHMARK_REPORT

**Files:**
- Modify: `README.md`
- Modify: `BENCHMARK_REPORT.md`

- [ ] Write full README: architecture, setup guide, env vars, run commands, API overview, AI integration notes, test strategy, known limitations
- [ ] Write `BENCHMARK_REPORT.md` with all required sections
- [ ] Commit: `docs: README and benchmark report`
