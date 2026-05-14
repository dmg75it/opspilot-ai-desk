# Architecture Patterns

**Project:** OpsPilot AI Desk
**Researched:** 2026-05-14

---

## 1. Backend Package Layout

Root package: `com.opspilot`

Every sub-package below maps to exactly one responsibility. Do not flatten or merge.

```
com.opspilot/
  OpsPilotApplication.java          # @SpringBootApplication

  config/
    SecurityConfig.java             # SecurityFilterChain bean, CORS, BCrypt, AuthManager
    JpaConfig.java                  # @EnableJpaAuditing
    OpenApiConfig.java              # springdoc OpenAPI bean
    OpenRouterClientConfig.java     # RestClient bean for OpenRouter
    OpenRouterProperties.java       # @ConfigurationProperties(prefix="openrouter")

  security/
    JwtAuthFilter.java              # OncePerRequestFilter — extract + validate JWT
    JwtService.java                 # generate / parse / validate tokens (jjwt 0.12.x)
    UserDetailsServiceImpl.java     # loads User entity by email

  controller/
    AuthController.java             # POST /api/auth/login, GET /api/auth/me
    TicketController.java           # /api/tickets/**
    TicketNoteController.java       # /api/tickets/{id}/notes
    AiChatController.java           # /api/tickets/{id}/chat/**
    DashboardController.java        # GET /api/dashboard/stats

  dto/
    request/
      LoginRequest.java
      CreateTicketRequest.java
      UpdateTicketRequest.java
      ChangeStatusRequest.java
      AssignTicketRequest.java
      CreateNoteRequest.java
      SendMessageRequest.java
    response/
      AuthResponse.java             # token + user info
      UserResponse.java
      TicketResponse.java
      TicketNoteResponse.java
      ChatSessionResponse.java
      ChatMessageResponse.java
      DashboardStatsResponse.java
      PageResponse.java             # generic wrapper for Page<T>

  service/
    AuthService.java                # login logic, token issuance
    TicketService.java              # CRUD, status transitions, optimistic lock handling
    TicketNoteService.java          # note creation, visibility rules
    AiChatService.java              # interface — send message, generate summary/reply
    DashboardService.java           # aggregation queries

  integration/
    OpenRouterAiChatService.java    # @ConditionalOnProperty(name="ai.provider", havingValue="openrouter")
    FakeAiChatService.java          # @ConditionalOnProperty(name="ai.provider", havingValue="fake")
    model/
      OpenRouterRequest.java        # record: model, messages[], maxTokens, temperature
      OpenRouterResponse.java       # record: choices[], usage

  repository/
    UserRepository.java
    TicketRepository.java
    TicketNoteRepository.java
    ChatSessionRepository.java
    ChatMessageRepository.java
    AuditLogRepository.java

  entity/
    User.java
    Ticket.java
    TicketNote.java
    ChatSession.java
    ChatMessage.java
    AuditLog.java
    enums/
      Role.java                     # ADMIN, OPERATOR
      TicketStatus.java             # NEW, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED, CLOSED
      TicketPriority.java           # LOW, MEDIUM, HIGH, CRITICAL
      TicketCategory.java           # DELIVERY, PICKUP, DOCUMENTATION, CUSTOMER, SYSTEM, OTHER
      NoteVisibility.java           # INTERNAL, AI_SUMMARY, SYSTEM
      MessageRole.java              # SYSTEM, USER, ASSISTANT

  mapper/
    TicketMapper.java               # MapStruct: Ticket <-> TicketResponse
    TicketNoteMapper.java
    ChatSessionMapper.java
    ChatMessageMapper.java
    UserMapper.java

  exception/
    GlobalExceptionHandler.java     # @RestControllerAdvice
    TicketNotFoundException.java
    InvalidStatusTransitionException.java
    TicketClosedException.java
    AiServiceException.java
    ConflictException.java          # wraps OptimisticLockingFailureException -> 409
```

**Rule:** Controllers receive requests, call one service, return DTOs. No entity types cross the controller boundary. No business logic in controllers or repositories.

---

## 2. Database Schema

Five core tables plus one audit table. All timestamps are `TIMESTAMPTZ`. All enum columns are `VARCHAR`.

### users

```sql
CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,           -- bcrypt
    role        VARCHAR(50)     NOT NULL,           -- ADMIN | OPERATOR
    full_name   VARCHAR(255),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
```

### tickets

```sql
CREATE TABLE tickets (
    id                  BIGSERIAL       PRIMARY KEY,
    external_ref        VARCHAR(100)    UNIQUE,     -- nullable, unique when present
    title               VARCHAR(150)    NOT NULL,
    description         TEXT            NOT NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'NEW',
    priority            VARCHAR(50)     NOT NULL DEFAULT 'MEDIUM',
    category            VARCHAR(50)     NOT NULL DEFAULT 'OTHER',
    assigned_to         BIGINT          REFERENCES users(id),
    created_by          BIGINT          NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ,
    version             INTEGER         NOT NULL DEFAULT 0  -- optimistic locking
);

CREATE INDEX idx_tickets_status   ON tickets(status);
CREATE INDEX idx_tickets_assigned ON tickets(assigned_to);
CREATE INDEX idx_tickets_created  ON tickets(created_at DESC);
```

### ticket_notes

```sql
CREATE TABLE ticket_notes (
    id          BIGSERIAL       PRIMARY KEY,
    ticket_id   BIGINT          NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id   BIGINT          NOT NULL REFERENCES users(id),
    body        TEXT            NOT NULL,
    visibility  VARCHAR(50)     NOT NULL DEFAULT 'INTERNAL',  -- INTERNAL | AI_SUMMARY | SYSTEM
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notes_ticket ON ticket_notes(ticket_id);
```

### chat_sessions

```sql
CREATE TABLE chat_sessions (
    id          BIGSERIAL       PRIMARY KEY,
    ticket_id   BIGINT          NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    created_by  BIGINT          NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (ticket_id, created_by)   -- one active session per user per ticket
);
```

### chat_messages

```sql
CREATE TABLE chat_messages (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20)     NOT NULL,   -- SYSTEM | USER | ASSISTANT
    content         TEXT            NOT NULL,
    model           VARCHAR(100),               -- null for USER/SYSTEM messages
    prompt_tokens   INTEGER,
    completion_tokens INTEGER,
    error           BOOLEAN         NOT NULL DEFAULT FALSE,
    error_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at ASC);
```

### audit_log

```sql
CREATE TABLE audit_log (
    id          BIGSERIAL       PRIMARY KEY,
    ticket_id   BIGINT          REFERENCES tickets(id),
    actor_id    BIGINT          REFERENCES users(id),
    action      VARCHAR(100)    NOT NULL,   -- e.g. STATUS_CHANGED, ASSIGNED, NOTE_ADDED
    old_value   TEXT,
    new_value   TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_ticket ON audit_log(ticket_id, created_at DESC);
```

### Entity relationships

```
users           1──n  tickets         (created_by, assigned_to)
users           1──n  ticket_notes    (author_id)
users           1──n  chat_sessions   (created_by)
tickets         1──n  ticket_notes
tickets         1──n  chat_sessions
chat_sessions   1──n  chat_messages
tickets         1──n  audit_log
```

---

## 3. REST API Endpoint Design

### Auth

```
POST   /api/auth/login          — body: {email, password} → {token, user}
GET    /api/auth/me             — returns current authenticated user
```

### Tickets

```
GET    /api/tickets             — paginated list; query params: status, priority, category,
                                  assignedTo, page, size, sort
POST   /api/tickets             — create; body: CreateTicketRequest
GET    /api/tickets/{id}        — get by id
PUT    /api/tickets/{id}        — update metadata; requires version in body for optimistic lock
PATCH  /api/tickets/{id}/status — change status; body: {status} — explicit sub-resource
PATCH  /api/tickets/{id}/assign — assign operator; body: {operatorId}
DELETE /api/tickets/{id}        — ADMIN only; soft-close or hard-delete depending on policy

GET    /api/tickets/{id}/notes           — list notes
POST   /api/tickets/{id}/notes           — create INTERNAL note
```

**Why PATCH /status instead of PUT /tickets/{id}?**
Status transitions are state-machine operations with validation logic independent of metadata updates. Separating them makes the intent explicit, prevents accidental status resets through bulk updates, and maps cleanly to service-layer methods. PATCH is correct because only one field changes.

**Why include version in PUT body?**
The client must send the `version` field it received in the GET response. The service compares it before writing. On mismatch, the repository throws `OptimisticLockingFailureException`, caught and returned as HTTP 409.

### AI Chat

```
GET    /api/tickets/{id}/chat/session          — get or create session for this user+ticket
POST   /api/tickets/{id}/chat/messages         — send user message → triggers AI call
GET    /api/tickets/{id}/chat/messages         — list message history (asc by created_at)
POST   /api/tickets/{id}/chat/summary          — generate AI ticket summary
POST   /api/tickets/{id}/chat/suggested-reply  — generate AI reply draft
POST   /api/tickets/{id}/chat/apply-summary    — saves last AI_SUMMARY as TicketNote (explicit action)
```

**Why nest under /tickets/{id}?**
Chat sessions are always scoped to a ticket. Nesting eliminates a redundant ticketId parameter and allows the backend to authorize based on ticket ownership in a single filter.

### Dashboard

```
GET    /api/dashboard/stats     — returns counts by status, by priority, my open tickets,
                                  recent tickets, AI interaction count today
```

### Admin

```
GET    /api/admin/users         — list users (ADMIN only)
POST   /api/admin/users         — create user (ADMIN only)
```

### Pagination response envelope

All paginated endpoints return:

```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

Map `Page<T>` from Spring Data to `PageResponse<T>` in the mapper layer. Never return the Spring `Page` object directly (it has framework-internal fields).

---

## 4. Ticket Status State Machine

Valid transitions — enforce in `TicketService.changeStatus()`:

```
NEW               → IN_PROGRESS, WAITING_FOR_CUSTOMER, CLOSED (ADMIN only)
IN_PROGRESS       → WAITING_FOR_CUSTOMER, RESOLVED, CLOSED (ADMIN only)
WAITING_FOR_CUSTOMER → IN_PROGRESS, RESOLVED, CLOSED (ADMIN only)
RESOLVED          → CLOSED, IN_PROGRESS (reopen)
CLOSED            → (terminal — no transitions; ADMIN may reopen to IN_PROGRESS)
```

Implementation approach — a static map in the service or a dedicated `TicketStatusTransition` enum:

```java
// In TicketService
private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
    NEW,                    Set.of(IN_PROGRESS, WAITING_FOR_CUSTOMER),
    IN_PROGRESS,            Set.of(WAITING_FOR_CUSTOMER, RESOLVED),
    WAITING_FOR_CUSTOMER,   Set.of(IN_PROGRESS, RESOLVED),
    RESOLVED,               Set.of(CLOSED, IN_PROGRESS),
    CLOSED,                 Set.of()
);

void validateTransition(TicketStatus from, TicketStatus to, boolean isAdmin) {
    var allowed = ALLOWED.getOrDefault(from, Set.of());
    if (!allowed.contains(to)) {
        throw new InvalidStatusTransitionException(from, to);
    }
    // CLOSED transitions always require ADMIN
    if (from == CLOSED && !isAdmin) {
        throw new AccessDeniedException("Only ADMIN can reopen closed tickets");
    }
}
```

Set `resolvedAt` on the entity when transitioning to RESOLVED. Write an audit log entry for every status change.

---

## 5. AI Integration Architecture

### Interface contract

```java
// service/AiChatService.java
public interface AiChatService {
    AiResponse sendMessage(String sessionId, List<ChatMessageDto> history, String userMessage);
    String generateSummary(Ticket ticket, List<TicketNote> notes);
    String generateSuggestedReply(Ticket ticket);
}

// Record returned from every AI call
public record AiResponse(
    String content,
    int promptTokens,
    int completionTokens,
    String modelUsed,
    boolean error,
    String errorMessage
) {}
```

### Prompt builder

Prompts are not inline strings. They live in a dedicated class versioned in code:

```java
// integration/PromptTemplates.java
public final class PromptTemplates {

    public static final String TICKET_SYSTEM_PROMPT = """
        You are a support assistant for a transport and logistics operations team.
        Help operators resolve field tickets efficiently.
        Be concise. Never change ticket status yourself.
        """;

    public static String summaryPrompt(Ticket ticket, List<TicketNote> notes) {
        // build string from ticket fields + note bodies
    }

    public static String suggestedReplyPrompt(Ticket ticket) {
        // build string from ticket fields
    }
}
```

Having prompts in one class means they are unit-testable and versioned through git history.

### Message persistence flow

On each `POST /api/tickets/{id}/chat/messages`:

1. Save the USER message to `chat_messages` with `role=USER`.
2. Load full message history for the session (ordered ASC by `created_at`).
3. Call `AiChatService.sendMessage(sessionId, history, systemPrompt)`.
4. Save the ASSISTANT response to `chat_messages` with `role=ASSISTANT`, model, and token counts.
5. Return both messages to the client (or just the assistant response with updated message list).

If the AI call fails: save a `chat_messages` row with `error=true`, `error_message=...`, and return HTTP 200 with error flag in response body (not HTTP 500) so the frontend can display a recoverable error inline.

### Fake provider

`FakeAiChatService` activated by `ai.provider=fake`. Returns deterministic strings useful in tests. Zero external calls. Unit tests inject the fake directly. Integration tests use `ai.provider=fake` in test properties.

---

## 6. Security Architecture

### Filter chain wiring

```
Request → JwtAuthFilter (OncePerRequestFilter)
              ↓ extract token from Authorization header
              ↓ validate with JwtService
              ↓ set SecurityContextHolder
          → Spring Security authorization check
              ↓ permit: /api/auth/**, /v3/api-docs/**, /swagger-ui/**
              ↓ require ADMIN: /api/admin/**
              ↓ authenticated: everything else
          → Controller
```

`JwtAuthFilter` is a `@Component`. It is injected into `SecurityConfig.securityFilterChain()` and added with `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.

Do NOT annotate `SecurityConfig` with `@EnableMethodSecurity` unless you need method-level `@PreAuthorize`. Endpoint-level authorization in `authorizeHttpRequests()` is sufficient and easier to audit.

### CORS

Configure CORS in `SecurityConfig` only (not in `WebMvcConfigurer`). With Spring Security, the security filter layer runs before the MVC layer, so a `WebMvcConfigurer` CORS config is never reached for secured endpoints.

```java
@Bean
CorsConfigurationSource corsConfigurationSource(
        @Value("${cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Pass this bean to `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` in the filter chain.

Externalise the allowed origins via `CORS_ALLOWED_ORIGINS` env var. Development default is `http://localhost:4200`.

### Closed-ticket edit guard

In `TicketService.updateTicket()`:

```java
if (ticket.getStatus() == TicketStatus.CLOSED && !currentUser.isAdmin()) {
    throw new TicketClosedException(ticket.getId());
}
```

Return HTTP 403.

---

## 7. Frontend Architecture

### Module boundaries

```
src/app/
  core/                 # singleton services, guards, interceptors — loaded once
    guards/
      auth.guard.ts     # canActivateFn: check isAuthenticated()
      admin.guard.ts    # canActivateFn: check hasRole('ADMIN')
    interceptors/
      auth.interceptor.ts     # attach Bearer token
      error.interceptor.ts    # catch 401 → redirect to login; catch 5xx → show toast
    services/
      auth.service.ts         # login(), logout(), getToken(), isAuthenticated(), hasRole()
                              # stores token in localStorage under key 'opspilot_token'
  services/             # feature services, not singletons in the strict sense but provided root
    ticket.service.ts
    ticket-note.service.ts
    ai-chat.service.ts
    dashboard.service.ts
    user.service.ts
  pages/                # routed components; each lazy-loaded
    login/
    dashboard/
    ticket-list/
    ticket-detail/      # contains the AI chat panel as a child component
    create-ticket/
    admin/
  shared/
    components/         # StatusBadge, PriorityBadge, LoadingSpinner, ErrorAlert, ConfirmDialog
    models/             # TypeScript interfaces mirroring backend DTOs
      ticket.model.ts
      ticket-note.model.ts
      chat.model.ts
      user.model.ts
      page.model.ts
    pipes/              # TicketStatusLabelPipe, DateAgoPipe
```

### AuthService contract

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'opspilot_token';
  private currentUser = signal<UserResponse | null>(null);

  login(email: string, password: string): Observable<void>
  logout(): void                         // clear token, navigate to /login
  getToken(): string | null              // read from localStorage
  isAuthenticated(): boolean             // token present and not expired
  hasRole(role: string): boolean         // decode JWT payload, check role claim
  getCurrentUser(): UserResponse | null  // from signal
}
```

Decode the JWT payload client-side (atob on the base64 part) to read `role` and `exp` without a round-trip. Do NOT call `/api/auth/me` on every route guard check — call it once after login and cache in the signal.

### Error interceptor

```typescript
// Handle 401 globally: clear token and redirect to /login
// Handle 4xx/5xx: emit to a global error subject (toast service)
// Never swallow errors silently
```

The error interceptor should NOT handle AI call errors — those return HTTP 200 with an `error` flag in the body and are handled by the component.

### AI chat panel

Lives inside `TicketDetailComponent` as a child component (`AiChatPanelComponent`). Conditionally displayed with a toggle button. Not routed separately. Receives `ticketId` as `@Input()`.

On first open: calls `GET /api/tickets/{id}/chat/session` to get or create the session, then loads message history.

"Apply to Ticket" button: calls `POST /api/tickets/{id}/chat/apply-summary`. This is the explicit user action required before any AI content modifies a ticket.

---

## 8. Build Order and Dependencies

Build in this order. Each step has hard dependencies on prior steps.

```
Step 1 — Infrastructure
  - docker-compose.yml with PostgreSQL
  - .env.example
  - Makefile targets: up, down, backend, frontend

Step 2 — Database schema
  - Flyway migrations V1–V5
  - Schema verified by starting backend and checking Flyway output

Step 3 — Backend: Auth
  - User entity + UserRepository
  - JwtService, JwtAuthFilter, SecurityConfig
  - AuthController (login, me)
  - Seed migration V5
  - Test: login endpoint returns token; /api/auth/me with token returns user

Step 4 — Backend: Tickets
  - Ticket entity with @Version
  - TicketRepository, TicketService (CRUD + state machine)
  - TicketController, DTOs, TicketMapper
  - TicketNote entity + flow
  - AuditLog writes on status change
  - Integration tests with Testcontainers

Step 5 — Backend: AI Chat
  - ChatSession + ChatMessage entities and repositories
  - AiChatService interface
  - FakeAiChatService (always first — makes tests runnable without keys)
  - PromptTemplates class
  - AiChatController
  - OpenRouterAiChatService (after fake works)
  - Test: full chat flow with fake provider

Step 6 — Backend: Dashboard
  - DashboardService (aggregation queries)
  - DashboardController
  - Verify with Swagger UI

Step 7 — Frontend: Shell
  - Angular project scaffold
  - AppComponent with router-outlet
  - Environment files, proxy.conf.json
  - Core module: AuthService, auth interceptor, error interceptor
  - Login page + reactive form
  - Route config with lazy loading
  - Auth guard wired to all non-login routes

Step 8 — Frontend: Tickets
  - Ticket model interfaces
  - TicketService
  - Ticket list page with pagination and filters
  - Create ticket page
  - Ticket detail page

Step 9 — Frontend: AI Chat
  - AI chat service
  - AiChatPanelComponent inside ticket detail
  - Apply-to-ticket button

Step 10 — Frontend: Dashboard + Admin
  - Dashboard page with stats cards
  - Admin user list page (ADMIN only)

Step 11 — Polish
  - Unit tests (status transitions, prompt builder, auth service, ticket list component)
  - README with setup + architecture + known limitations
  - Verify full happy path end-to-end
```

### Key dependency facts

- Step 3 (Auth) must complete before any other backend feature — every endpoint requires a valid JWT.
- Step 5 (AI) must use the fake provider as the first working implementation so Steps 6–11 are not blocked on an OpenRouter key.
- Step 7 (Angular shell + login) can start in parallel with Step 4 (Tickets) once Step 3 is done — the frontend only needs the auth endpoints initially.
- Do not implement the OpenRouter client until the fake client is fully tested — this prevents leaking API keys into test logs.

---

## 9. Entity Design Notes

### Ticket entity

```java
@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String externalRef;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedOperator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private Integer version;
}
```

`FetchType.LAZY` on all `@ManyToOne` associations. Eager loading on `Ticket` would include User on every query. Use explicit JPQL JOIN FETCH in the repository when the full graph is needed (e.g., a detail endpoint).

### ChatSession + ChatMessage

```java
@Entity @Table(name = "chat_sessions")
public class ChatSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private Instant createdAt;
}

@Entity @Table(name = "chat_messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String model;
    private Integer promptTokens;
    private Integer completionTokens;

    @Column(nullable = false)
    private boolean error;

    private String errorMessage;

    private Instant createdAt;
}
```

The `ChatMessageRepository` must load messages ordered ascending by `createdAt`:

```java
List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
```

This ascending order is required — LLMs need history oldest-first.

---

## 10. MapStruct Wiring

Mappers are Spring-managed beans (`@Mapper(componentModel = "spring")`). They are injected into services, not controllers.

```java
@Mapper(componentModel = "spring")
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);
    Ticket toEntity(CreateTicketRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateTicketRequest request, @MappingTarget Ticket ticket);
}
```

The `updateEntityFromRequest` pattern with `IGNORE` strategy means null fields in the update request are not applied — prevents accidental nulling of unset fields in partial updates. This is cleaner than a manual null check loop.

---

## Sources

- [Spring Boot CORS with Security](https://reflectoring.io/spring-cors/) — MEDIUM confidence, verified pattern
- [Spring Security 6 JWT filter](https://medium.com/@truongbui95/jwt-authentication-and-authorization-with-spring-boot-3-and-spring-security-6-2f90f9337421) — MEDIUM confidence
- [MapStruct partial update pattern](https://mapstruct.org/) — HIGH confidence (official docs)
- [Angular HTTP interceptors](https://angular.dev/guide/http/interceptors) — HIGH confidence (official docs)
- [Angular CanActivateFn](https://angular.dev/api/router/CanActivateFn) — HIGH confidence (official docs)
- [REST state machine design](https://nordicapis.com/designing-a-true-rest-state-machine/) — MEDIUM confidence
- [Optimistic locking @Version](https://www.baeldung.com/jpa-optimistic-locking) — HIGH confidence (Baeldung + official JPA)
- [OpenRouter API reference](https://openrouter.ai/docs/quickstart) — HIGH confidence (official docs)
- [Spring Boot project structure](https://docs.bswen.com/blog/2026-02-28-springboot-project-structure/) — MEDIUM confidence
