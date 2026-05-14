# Technology Stack

**Project:** OpsPilot AI Desk
**Researched:** 2026-05-14
**Stack status:** Mandated — no alternative evaluation needed

---

## Core Versions (pinned)

| Technology | Version | Notes |
|------------|---------|-------|
| Java | 21 | LTS; virtual threads available |
| Spring Boot | 3.3.x (use latest 3.3.y patch) | 3.4.x is current stable but 3.3.x is in scope |
| Spring Security | 6.3.x (bundled with Boot 3.3) | WebSecurityConfigurerAdapter removed |
| Angular | 18 (or 17 LTS) | 17+ standalone by default |
| Node / npm | 20 LTS | Match Angular 18 toolchain |
| PostgreSQL | 16 | Use `postgres:16-alpine` in Docker |
| Flyway | 10.x (bundled with Boot 3.3) | V__ naming convention |
| springdoc-openapi | 2.8.x | Use 2.8.17 or latest 2.8.y |
| jjwt | 0.12.x (latest: 0.13.0) | Breaking API change from 0.11.x |
| Testcontainers | 1.20.x | Bundled BOM in Boot 3.3 |

---

## 1. Spring Boot 3.3.x — Key Features and Patterns

### Maven parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.11</version>
</parent>
```

### Core starters (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.8.17</version>
    </dependency>
</dependencies>
```

### Virtual threads (Java 21 + Spring Boot 3.2+)

Enable in `application.properties`. This routes all Tomcat request threads through Java 21 virtual threads — beneficial for the I/O-heavy AI call path:

```properties
spring.threads.virtual.enabled=true
```

No other configuration is needed. Tomcat, Jetty, and `@Async` executors are all automatically reconfigured when this flag is set.

### RestClient (use instead of RestTemplate)

Spring Boot 3.2+ auto-configures a `RestClient.Builder` bean. Use `RestClient` (not `RestTemplate`, not `WebClient`) for the synchronous OpenRouter HTTP client:

```java
@Configuration
public class OpenRouterClientConfig {
    @Bean
    public RestClient openRouterRestClient(RestClient.Builder builder) {
        return builder
            .baseUrl("${openrouter.base-url}")
            .defaultHeader("Authorization", "Bearer ${openrouter.api-key}")
            .build();
    }
}
```

Global HTTP client timeouts via properties (Spring Boot 3.4+; for 3.3.x configure on the builder):

```properties
spring.http.client.connect-timeout=5s
spring.http.client.read-timeout=60s
```

---

## 2. Spring Security 6.x — JWT Authentication

### Pattern: SecurityFilterChain bean (class-based config removed)

`WebSecurityConfigurerAdapter` was removed in Spring Security 6.0. Declare a `@Bean SecurityFilterChain` directly:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### JWT filter pattern

Extend `OncePerRequestFilter` to run validation once per request:

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String token = authHeader.substring(7);
        final String username = jwtService.extractUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### JJWT 0.12.x / 0.13.x — API (breaking change from 0.11)

The 0.12 line changed `parseClaimsJws()` to `parseSignedClaims()`. Use the current API:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")          // base64-encoded, min 32 bytes for HS256
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey())
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
            && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration()
            .before(new Date());
    }
}
```

Generate a secret for local dev:

```bash
openssl rand -base64 64
```

### application.properties security config

```properties
jwt.secret=<base64-encoded-secret>
jwt.expiration-ms=3600000
```

---

## 3. Spring Data JPA — Best Practices

### Repository pattern

Extend `JpaRepository` (not `CrudRepository`) for pagination support:

```java
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.assignedOperator.id = :operatorId AND t.status != 'CLOSED'")
    List<Ticket> findOpenByOperator(@Param("operatorId") Long operatorId);
}
```

### Optimistic locking

Add `@Version` on the entity field. Spring Data / Hibernate will check the version in the `WHERE` clause on `UPDATE`. Handle `OptimisticLockingFailureException` at the service layer and return HTTP 409:

```java
@Entity
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Version
    private Integer version;   // must be in request/response DTOs for updates

    // ... other fields
}
```

### Pagination

Use `Pageable` parameters and return `Page<T>`. Map to a response DTO containing `content`, `totalElements`, `totalPages`, `number`.

### Enum mapping

Map Java enums to PostgreSQL using `@Enumerated(EnumType.STRING)` (never `ORDINAL`):

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private TicketStatus status;
```

### Auditing

Use `@EnableJpaAuditing` on a config class, and `@EntityListeners(AuditingEntityListener.class)` on entities with `@CreatedDate` / `@LastModifiedDate`:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {}

// On the entity:
@EntityListeners(AuditingEntityListener.class)
public class Ticket {
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

---

## 4. Flyway — Migration Conventions

### Dependency (Spring Boot 3.3 with PostgreSQL)

`flyway-database-postgresql` must be declared explicitly in 3.x (the old `flyway-core` alone does not include PostgreSQL support):

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### File naming

```
src/main/resources/db/migration/
  V1__create_users.sql
  V2__create_tickets.sql
  V3__create_ticket_notes.sql
  V4__create_ai_chat.sql
  V5__seed_users.sql
```

Rules:
- Prefix `V` for versioned (applied once, immutable)
- Prefix `R` for repeatable (re-applied when checksum changes — use for views/functions)
- Separator is **two underscores** `__` (one underscore causes silent failures)
- Description uses underscores for spaces
- Never modify a migration once it has run; create a new version instead

### application.properties

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=false
spring.flyway.validate-on-migrate=true
```

### PostgreSQL-specific patterns

Use `BIGSERIAL` or `CREATE SEQUENCE` with `OWNED BY`:

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- V5__seed_users.sql  (passwords are bcrypt of admin123/operator123)
INSERT INTO users (email, password, role) VALUES
    ('admin@example.com',    '$2a$12$...', 'ADMIN'),
    ('operator@example.com', '$2a$12$...', 'OPERATOR');
```

Generate bcrypt hashes for the seed script at build time or hard-code known hashes (cost factor 12 recommended).

---

## 5. OpenRouter API Integration

### API facts

- Endpoint: `POST https://openrouter.ai/api/v1/chat/completions`
- Authentication: `Authorization: Bearer <key>`
- Request is OpenAI-compatible: `{ model, messages[], max_tokens, temperature }`
- System prompt: add a message with `"role": "system"` at index 0
- Response shape: `{ choices[0].message.content, usage.prompt_tokens, usage.completion_tokens }`

### Configuration properties

```properties
openrouter.api-key=${OPENROUTER_API_KEY}
openrouter.base-url=https://openrouter.ai/api/v1
openrouter.model=openai/gpt-4o-mini
openrouter.max-tokens=2048
openrouter.temperature=0.3
openrouter.timeout-seconds=60
```

Bind with `@ConfigurationProperties`:

```java
@ConfigurationProperties(prefix = "openrouter")
@Validated
public record OpenRouterProperties(
    @NotBlank String apiKey,
    @NotBlank String baseUrl,
    @NotBlank String model,
    @Min(1) int maxTokens,
    double temperature,
    @Min(1) int timeoutSeconds
) {}
```

Enable with `@EnableConfigurationProperties(OpenRouterProperties.class)` on a config class.

### Service abstraction

Define an interface so the fake implementation can be swapped:

```java
public interface AiChatService {
    AiResponse sendMessage(String sessionId, List<ChatMessage> history, String systemPrompt);
    String generateSummary(Ticket ticket);
    String generateSuggestedReply(Ticket ticket);
}
```

### OpenRouter REST client (request/response POJOs)

```java
public record OpenRouterRequest(
    String model,
    List<Message> messages,
    @JsonProperty("max_tokens") int maxTokens,
    double temperature
) {
    public record Message(String role, String content) {}
}

public record OpenRouterResponse(
    List<Choice> choices,
    Usage usage
) {
    public record Choice(Message message) {
        public record Message(String role, String content) {}
    }
    public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens
    ) {}
}
```

### Implementation with RestClient

```java
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OpenRouterAiChatService implements AiChatService {

    private final RestClient restClient;
    private final OpenRouterProperties props;

    @Override
    public AiResponse sendMessage(String sessionId, List<ChatMessage> history, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        var request = new OpenRouterRequest(props.model(), messages, props.maxTokens(), props.temperature());

        log.info("AI request: sessionId={} model={}", sessionId, props.model());
        long start = System.currentTimeMillis();
        try {
            var response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(OpenRouterResponse.class);
            log.info("AI response: sessionId={} elapsed={}ms tokens={}", sessionId,
                System.currentTimeMillis() - start,
                response.usage().completionTokens());
            return mapToAiResponse(response);
        } catch (Exception ex) {
            log.error("AI call failed: sessionId={} error={}", sessionId, ex.getMessage());
            throw new AiServiceException("OpenRouter call failed", ex);
        }
    }
}
```

### Fake implementation for tests/local dev

```java
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "fake")
public class FakeAiChatService implements AiChatService {

    @Override
    public AiResponse sendMessage(String sessionId, List<ChatMessage> history, String systemPrompt) {
        return new AiResponse("[FAKE] AI response for session " + sessionId, 0, 0);
    }

    @Override
    public String generateSummary(Ticket ticket) {
        return "[FAKE] Summary for ticket #" + ticket.getId();
    }

    @Override
    public String generateSuggestedReply(Ticket ticket) {
        return "[FAKE] Suggested reply for ticket #" + ticket.getId();
    }
}
```

Activate via env var: `AI_PROVIDER=fake` → `ai.provider=fake` in properties.

---

## 6. Testcontainers + JUnit 5

### Test dependencies (pom.xml)

The `spring-boot-testcontainers` artifact is a first-class Boot 3.1+ artifact — no version needed when using the Boot BOM:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### Integration test pattern with @ServiceConnection

`@ServiceConnection` (available since Spring Boot 3.1) automatically reads the container's exposed port and sets `spring.datasource.*` properties — no `@DynamicPropertySource` required:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TicketApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void createTicket_returnsCreated() {
        // ...
    }
}
```

### Shared container across test classes

Extract container setup into a reusable `@TestConfiguration` class to avoid spinning up a new container per test class:

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
```

Import in each test: `@Import(TestcontainersConfiguration.class)`.

### Security integration tests

Test protected endpoints by sending a valid JWT in the `Authorization` header, or use `@WithMockUser` for unit-level slice tests:

```java
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getTicket_whenAuthenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/tickets/1"))
            .andExpect(status().isOk());
    }

    @Test
    void getTicket_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets/1"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## 7. OpenAPI / Swagger (springdoc-openapi 2.8.x)

### Configuration

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("OpsPilot AI Desk API")
                .version("1.0.0")
                .description("Field operations support desk API"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

### application.properties

```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.persist-authorization=true
springdoc.swagger-ui.operations-sorter=alpha
```

### Expose Swagger UI without authentication

Include Swagger paths in the `permitAll()` matcher in `SecurityConfig`:

```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
```

---

## 8. Angular 17/18 — Standalone Component Patterns

### Bootstrap (main.ts)

```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { authInterceptor } from './app/core/auth.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
});
```

### Standalone component structure

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'app-create-ticket',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-ticket.component.html',
})
export class CreateTicketComponent {
  form = new FormGroup({
    title: new FormControl('', [Validators.required, Validators.maxLength(150)]),
    description: new FormControl('', [Validators.required, Validators.maxLength(5000)]),
    priority: new FormControl('MEDIUM', Validators.required),
    category: new FormControl('', Validators.required),
  });
}
```

### Functional HTTP interceptor (auth)

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  if (token) {
    const cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    return next(cloned);
  }
  return next(req);
};
```

### Functional route guard (auth)

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.isAuthenticated()
    ? true
    : router.parseUrl('/login');
};

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.hasRole('ADMIN')
    ? true
    : router.parseUrl('/dashboard');
};
```

### Route configuration

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'tickets', loadComponent: () => import('./pages/ticket-list/ticket-list.component').then(m => m.TicketListComponent) },
      { path: 'tickets/new', loadComponent: () => import('./pages/create-ticket/create-ticket.component').then(m => m.CreateTicketComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./pages/ticket-detail/ticket-detail.component').then(m => m.TicketDetailComponent) },
      { path: 'admin', canActivate: [adminGuard], loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ]
  },
  { path: '**', redirectTo: '' }
];
```

### API service pattern

```typescript
@Injectable({ providedIn: 'root' })
export class TicketService {
  private http = inject(HttpClient);
  private base = '/api/tickets';

  list(params: TicketFilter): Observable<Page<Ticket>> {
    return this.http.get<Page<Ticket>>(this.base, { params: toHttpParams(params) });
  }

  get(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/${id}`);
  }

  create(dto: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.base, dto);
  }

  update(id: number, dto: UpdateTicketRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.base}/${id}`, dto);
  }

  changeStatus(id: number, status: TicketStatus): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.base}/${id}/status`, { status });
  }
}
```

### Environment configuration

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
};

// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: '/api',
};
```

Use `proxy.conf.json` in development to avoid CORS:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

---

## 9. Layered Architecture — Package Structure

### Backend

```
com.opspilot
  config/           # SecurityConfig, OpenApiConfig, JpaConfig, etc.
  security/         # JwtAuthFilter, JwtService, UserDetailsServiceImpl
  controller/       # TicketController, AuthController, AiChatController, DashboardController
  dto/              # request/, response/ subdirectories
  service/          # TicketService, AuthService, AiChatService (interface), DashboardService
  integration/      # OpenRouterAiChatService, FakeAiChatService
  repository/       # TicketRepository, UserRepository, TicketNoteRepository, ChatSessionRepository
  entity/           # Ticket, User, TicketNote, ChatSession, ChatMessage (enum: TicketStatus, etc.)
  mapper/           # TicketMapper, UserMapper (MapStruct or manual)
  exception/        # GlobalExceptionHandler (@RestControllerAdvice), domain exceptions
```

### Frontend

```
src/app/
  core/
    guards/         # auth.guard.ts
    interceptors/   # auth.interceptor.ts
    services/       # auth.service.ts
  services/         # ticket.service.ts, ai-chat.service.ts, dashboard.service.ts
  pages/
    login/
    dashboard/
    ticket-list/
    ticket-detail/
    create-ticket/
    admin/
  shared/
    components/     # reusable UI components
    models/         # TypeScript interfaces matching API DTOs
```

---

## 10. Supporting Library Choices

| Library | Purpose | Why |
|---------|---------|-----|
| MapStruct 1.5.x | DTO ↔ Entity mapping | Compile-time, no reflection, works with Lombok |
| Jackson (bundled) | JSON serialization | Default, no change needed |
| Spring Boot Actuator | Health checks | Add `/actuator/health` for Docker healthcheck |
| Lombok | Boilerplate reduction | `@Data`, `@Builder`, `@RequiredArgsConstructor` |

### MapStruct + Lombok — pom.xml order matters

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Lombok must appear before MapStruct in `annotationProcessorPaths` or MapStruct cannot see Lombok-generated constructors.

---

## Sources

- Spring Boot 3.4 reference docs: https://docs.spring.io/spring-boot/3.4/reference/
- Spring Boot Testcontainers docs: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Angular official docs (interceptors): https://angular.dev/guide/http/interceptors
- Angular route guards: https://angular.dev/guide/routing/route-guards
- springdoc-openapi: https://springdoc.org/
- jjwt GitHub: https://github.com/jwtk/jjwt
- OpenRouter API reference: https://openrouter.ai/docs/api/reference/overview
- Spring virtual threads: https://spring.io/blog/2022/10/11/embracing-virtual-threads/
- Flyway naming conventions: https://blog.jetbrains.com/idea/2024/11/how-to-use-flyway-for-database-migrations-in-spring-boot-applications/
