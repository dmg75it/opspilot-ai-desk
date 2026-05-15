# OpsPilot AI Desk — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Spring Boot 3.3.x backend with JWT auth, ticket workflow, AI chat via OpenRouter (with FakeAiClient default), and PostgreSQL via Flyway.

**Architecture:** Layered: controller → service → repository → entity. AI behind AiClient interface. FakeAiClient is the Spring default bean unless `AI_PROVIDER=openrouter`. Flyway manages schema; DataInitializer seeds BCrypt-hashed users at startup.

**Tech Stack:** Java 21 (`JAVA_HOME=/opt/platform/jdk-21.0.7`), Spring Boot 3.3.5, Maven 3.8.x, JJWT 0.12.6, Springdoc OpenAPI 2.6.0, Flyway, PostgreSQL 16, Lombok, Testcontainers 1.20.4.

**Run all Maven commands as:** `JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml <goal>`

---

## File Map

```
backend/
  pom.xml
  src/main/resources/
    application.yml
    db/migration/V1__create_users.sql
    db/migration/V2__create_tickets.sql
    db/migration/V3__create_ticket_notes.sql
    db/migration/V4__create_audit_log.sql
    db/migration/V5__create_chat_sessions.sql
    db/migration/V6__create_chat_messages.sql
    db/migration/V7__seed_users.sql
  src/main/java/io/opspilot/desk/
    OpsPilotApplication.java
    config/SecurityConfig.java
    config/OpenApiConfig.java
    config/AiProperties.java
    security/JwtService.java
    security/JwtFilter.java
    security/UserDetailsServiceImpl.java
    entity/User.java
    entity/Ticket.java
    entity/TicketNote.java
    entity/AuditLog.java
    entity/ChatSession.java
    entity/ChatMessage.java
    repository/UserRepository.java
    repository/TicketRepository.java
    repository/TicketNoteRepository.java
    repository/AuditLogRepository.java
    repository/ChatSessionRepository.java
    repository/ChatMessageRepository.java
    dto/auth/LoginRequest.java
    dto/auth/LoginResponse.java
    dto/auth/UserResponse.java
    dto/ticket/CreateTicketRequest.java
    dto/ticket/UpdateTicketRequest.java
    dto/ticket/ChangeStatusRequest.java
    dto/ticket/AssignTicketRequest.java
    dto/ticket/TicketResponse.java
    dto/note/CreateNoteRequest.java
    dto/note/NoteResponse.java
    dto/ai/SendMessageRequest.java
    dto/ai/ChatMessageResponse.java
    dto/ai/ChatSessionResponse.java
    dto/ai/AiActionResponse.java
    dto/dashboard/DashboardResponse.java
    dto/user/UserListResponse.java
    service/AuthService.java
    service/TicketService.java
    service/NoteService.java
    service/AuditService.java
    service/AiService.java
    service/DashboardService.java
    service/DataInitializer.java
    controller/AuthController.java
    controller/TicketController.java
    controller/NoteController.java
    controller/AiController.java
    controller/DashboardController.java
    controller/UserController.java
    ai/AiClient.java
    ai/AiRequest.java
    ai/AiResponse.java
    ai/FakeAiClient.java
    ai/OpenRouterClient.java
    ai/PromptTemplates.java
    exception/GlobalExceptionHandler.java
    exception/TicketNotFoundException.java
    exception/InvalidStatusTransitionException.java
  src/test/java/io/opspilot/desk/
    service/TicketStatusTransitionTest.java
    security/JwtServiceTest.java
    ai/PromptBuilderTest.java
    integration/TicketControllerIT.java
    integration/AuthSecurityIT.java
```

---

### Task 1: Maven project + main class + application.yml

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/io/opspilot/desk/OpsPilotApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>
  <groupId>io.opspilot</groupId>
  <artifactId>desk</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>OpsPilot AI Desk</name>
  <properties>
    <java.version>21</java.version>
    <jjwt.version>0.12.6</jjwt.version>
    <springdoc.version>2.6.0</springdoc.version>
    <testcontainers.version>1.20.4</testcontainers.version>
  </properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>${springdoc.version}</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><version>${testcontainers.version}</version><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><version>${testcontainers.version}</version><scope>test</scope></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Create `backend/src/main/java/io/opspilot/desk/OpsPilotApplication.java`**

```java
package io.opspilot.desk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.opspilot.desk.config.AiProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class OpsPilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsPilotApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `backend/src/main/resources/application.yml`**

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/opspilot}
    username: ${DATABASE_USER:opspilot}
    password: ${DATABASE_PASSWORD:opspilot}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET:bXlTdXBlclNlY3JldEtleUZvck9wc1BpbG90QUlEZXNrMjAyNg==}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200}

ai:
  provider: ${AI_PROVIDER:fake}
  openrouter:
    api-key: ${OPENROUTER_API_KEY:}
    base-url: ${OPENROUTER_BASE_URL:https://openrouter.ai/api/v1}
    model: ${OPENROUTER_MODEL:openai/gpt-4o-mini}
    timeout-seconds: ${OPENROUTER_TIMEOUT_SECONDS:30}
    max-tokens: ${OPENROUTER_MAX_TOKENS:1024}
    temperature: ${OPENROUTER_TEMPERATURE:0.7}

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    io.opspilot.desk: INFO
```

- [ ] **Step 4: Verify compilation**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS (will fail on missing classes but pom.xml and main class should compile after creating AiProperties placeholder in next task)

- [ ] **Step 5: Commit**

```bash
git add backend/ && git commit -m "feat: add backend Maven project, main class, application.yml"
```

---

### Task 2: Infrastructure — Docker Compose, Makefile, .env.example

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `Makefile`
- Create: `.gitignore`

- [ ] **Step 1: Create `docker-compose.yml`**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: opspilot
      POSTGRES_USER: opspilot
      POSTGRES_PASSWORD: opspilot
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U opspilot"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    profiles: [fullstack]
    build:
      context: backend
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/opspilot
      DATABASE_USER: opspilot
      DATABASE_PASSWORD: opspilot
      JWT_SECRET: ${JWT_SECRET}
      AI_PROVIDER: ${AI_PROVIDER:-fake}
      OPENROUTER_API_KEY: ${OPENROUTER_API_KEY:-}
      OPENROUTER_MODEL: ${OPENROUTER_MODEL:-openai/gpt-4o-mini}
      CORS_ALLOWED_ORIGINS: http://localhost:4200
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

  frontend:
    profiles: [fullstack]
    build:
      context: frontend
      dockerfile: Dockerfile
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  pgdata:
```

- [ ] **Step 2: Create `.env.example`**

```
DATABASE_URL=jdbc:postgresql://localhost:5432/opspilot
DATABASE_USER=opspilot
DATABASE_PASSWORD=opspilot
JWT_SECRET=bXlTdXBlclNlY3JldEtleUZvck9wc1BpbG90QUlEZXNrMjAyNg==
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=http://localhost:4200
AI_PROVIDER=fake
OPENROUTER_API_KEY=
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_MODEL=openai/gpt-4o-mini
OPENROUTER_TIMEOUT_SECONDS=30
OPENROUTER_MAX_TOKENS=1024
OPENROUTER_TEMPERATURE=0.7
SERVER_PORT=8080
```

- [ ] **Step 3: Create `Makefile`**

```makefile
JAVA_HOME=/opt/platform/jdk-21.0.7
export JAVA_HOME

.PHONY: db-up db-down backend-run backend-test frontend-install frontend-run frontend-test frontend-build stack-up stack-down

db-up:
	docker compose up -d postgres

db-down:
	docker compose down

backend-run:
	mvn -f backend/pom.xml spring-boot:run

backend-test:
	mvn -f backend/pom.xml test

frontend-install:
	cd frontend && npm install

frontend-run:
	cd frontend && npm start

frontend-test:
	cd frontend && npm test -- --watch=false --browsers=ChromeHeadless

frontend-build:
	cd frontend && npm run build -- --configuration=production

stack-up:
	docker compose --profile fullstack up --build

stack-down:
	docker compose --profile fullstack down
```

- [ ] **Step 4: Create `.gitignore`**

```
# Java
backend/target/
*.class
*.jar

# Maven
.mvn/wrapper/maven-wrapper.jar

# Node
frontend/node_modules/
frontend/dist/
frontend/.angular/

# Environment
.env

# IDE
.idea/
*.iml
.vscode/
*.DS_Store
```

- [ ] **Step 5: Verify Docker**

```bash
docker compose up -d postgres
docker compose ps
```

Expected: postgres container running

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml .env.example Makefile .gitignore
git commit -m "feat: add docker-compose, Makefile, .env.example"
```

---

### Task 3: Flyway migrations

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_users.sql` through `V7__seed_users.sql`

- [ ] **Step 1: V1__create_users.sql**

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: V2__create_tickets.sql**

```sql
CREATE TYPE ticket_status AS ENUM ('NEW','IN_PROGRESS','WAITING_FOR_CUSTOMER','RESOLVED','CLOSED');
CREATE TYPE ticket_priority AS ENUM ('LOW','MEDIUM','HIGH','CRITICAL');
CREATE TYPE ticket_category AS ENUM ('DELIVERY','PICKUP','DOCUMENTATION','CUSTOMER','SYSTEM','OTHER');

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_ref VARCHAR(100),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status ticket_status NOT NULL DEFAULT 'NEW',
    priority ticket_priority NOT NULL,
    category ticket_category NOT NULL,
    assigned_to_id UUID REFERENCES users(id),
    created_by_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_tickets_external_ref
    ON tickets (external_ref)
    WHERE external_ref IS NOT NULL;
```

- [ ] **Step 3: V3__create_ticket_notes.sql**

```sql
CREATE TYPE note_visibility AS ENUM ('INTERNAL','AI_SUMMARY','SYSTEM');

CREATE TABLE ticket_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id),
    body TEXT NOT NULL,
    visibility note_visibility NOT NULL DEFAULT 'INTERNAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 4: V4__create_audit_log.sql**

```sql
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 5: V5__create_chat_sessions.sql**

```sql
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 6: V6__create_chat_messages.sql**

```sql
CREATE TYPE message_role AS ENUM ('SYSTEM','USER','ASSISTANT');

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role message_role NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(100),
    prompt_tokens INT,
    completion_tokens INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT
);
```

- [ ] **Step 7: V7__seed_users.sql**

```sql
INSERT INTO users (email, password, role) VALUES
('admin@example.com', '$PLACEHOLDER$', 'ADMIN'),
('operator@example.com', '$PLACEHOLDER$', 'OPERATOR');
```

- [ ] **Step 8: Verify migrations run**

Ensure PostgreSQL is running (`make db-up`), then:

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/opspilot \
  -Dflyway.user=opspilot \
  -Dflyway.password=opspilot
```

Expected: Successfully applied 7 migrations

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add Flyway migrations V1-V7"
```

---

### Task 4: JPA entities

**Files:**
- Create: all entity files in `backend/src/main/java/io/opspilot/desk/entity/`

- [ ] **Step 1: Create `entity/User.java`**

```java
package io.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    public enum Role { ADMIN, OPERATOR }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;
}
```

- [ ] **Step 2: Create `entity/Ticket.java`**

```java
package io.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor
public class Ticket {
    public enum Status { NEW, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED, CLOSED }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Category { DELIVERY, PICKUP, DOCUMENTATION, CUSTOMER, SYSTEM, OTHER }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ticket_status", nullable = false)
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ticket_priority", nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ticket_category", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    private Long version;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
```

- [ ] **Step 3: Create `entity/TicketNote.java`**

```java
package io.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_notes")
@Getter @Setter @NoArgsConstructor
public class TicketNote {
    public enum Visibility { INTERNAL, AI_SUMMARY, SYSTEM }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "note_visibility", nullable = false)
    private Visibility visibility = Visibility.INTERNAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 4: Create `entity/AuditLog.java`**

```java
package io.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 5: Create `entity/ChatSession.java`**

```java
package io.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
@Getter @Setter @NoArgsConstructor
public class ChatSession {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 6: Create `entity/ChatMessage.java`**

```java
package io.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor
public class ChatMessage {
    public enum Role { SYSTEM, USER, ASSISTANT }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "message_role", nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean error = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
```

- [ ] **Step 7: Compile check**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/io/opspilot/desk/entity/
git commit -m "feat: add JPA entities"
```

---

### Task 5: JPA repositories + exception classes + AiProperties

**Files:**
- Create: all repository interfaces
- Create: exception classes
- Create: `config/AiProperties.java`

- [ ] **Step 1: Create `repository/UserRepository.java`**

```java
package io.opspilot.desk.repository;

import io.opspilot.desk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 2: Create `repository/TicketRepository.java`**

```java
package io.opspilot.desk.repository;

import io.opspilot.desk.entity.Ticket;
import io.opspilot.desk.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {
    Page<Ticket> findAll(Pageable pageable);
    List<Ticket> findByAssignedToAndStatusNot(User user, Ticket.Status status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    long countByStatus(Ticket.Status status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.priority = :priority")
    long countByPriority(Ticket.Priority priority);

    List<Ticket> findTop10ByOrderByUpdatedAtDesc();
}
```

- [ ] **Step 3: Create `repository/TicketNoteRepository.java`**

```java
package io.opspilot.desk.repository;

import io.opspilot.desk.entity.Ticket;
import io.opspilot.desk.entity.TicketNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TicketNoteRepository extends JpaRepository<TicketNote, UUID> {
    List<TicketNote> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
```

- [ ] **Step 4: Create `repository/AuditLogRepository.java`**

```java
package io.opspilot.desk.repository;

import io.opspilot.desk.entity.AuditLog;
import io.opspilot.desk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTicketOrderByCreatedAtDesc(Ticket ticket);
}
```

- [ ] **Step 5: Create `repository/ChatSessionRepository.java`**

```java
package io.opspilot.desk.repository;

import io.opspilot.desk.entity.ChatSession;
import io.opspilot.desk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findByTicket(Ticket ticket);
}
```

- [ ] **Step 6: Create `repository/ChatMessageRepository.java`**

```java
package io.opspilot.desk.repository;

import io.opspilot.desk.entity.ChatMessage;
import io.opspilot.desk.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.role = io.opspilot.desk.entity.ChatMessage.Role.ASSISTANT AND m.createdAt >= :since")
    long countAssistantMessagesSince(Instant since);
}
```

- [ ] **Step 7: Create `exception/TicketNotFoundException.java`**

```java
package io.opspilot.desk.exception;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(UUID id) {
        super("Ticket not found: " + id);
    }
}
```

- [ ] **Step 8: Create `exception/InvalidStatusTransitionException.java`**

```java
package io.opspilot.desk.exception;

import io.opspilot.desk.entity.Ticket;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Ticket.Status from, Ticket.Status to) {
        super("Cannot transition ticket from " + from + " to " + to);
    }
}
```

- [ ] **Step 9: Create `config/AiProperties.java`**

```java
package io.opspilot.desk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String provider = "fake";
    private Openrouter openrouter = new Openrouter();

    @Data
    public static class Openrouter {
        private String apiKey = "";
        private String baseUrl = "https://openrouter.ai/api/v1";
        private String model = "openai/gpt-4o-mini";
        private int timeoutSeconds = 30;
        private int maxTokens = 1024;
        private double temperature = 0.7;
    }
}
```

- [ ] **Step 10: Compile check**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/io/opspilot/desk/
git commit -m "feat: add repositories, exceptions, AiProperties"
```

---

### Task 6: JWT unit tests + JwtService

- [ ] **Step 1: Write failing test `security/JwtServiceTest.java`**

```java
package io.opspilot.desk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.secret = "bXlTdXBlclNlY3JldEtleUZvck9wc1BpbG90QUlEZXNrMjAyNg==";
        jwtService.expirationMs = 86400000L;
    }

    @Test
    void generateAndValidateToken() {
        UserDetails user = User.withUsername("test@example.com")
            .password("x").authorities(List.of()).build();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void extractUsernameFromToken() {
        UserDetails user = User.withUsername("admin@example.com")
            .password("x").authorities(List.of()).build();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -Dtest=JwtServiceTest -q 2>&1 | tail -5
```

Expected: FAILURE (JwtService not yet created)

- [ ] **Step 3: Create `security/JwtService.java`**

```java
package io.opspilot.desk.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    String secret;

    @Value("${jwt.expiration-ms}")
    long expirationMs;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey())
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(signingKey()).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            Date expiration = Jwts.parser().verifyWith(signingKey()).build()
                .parseSignedClaims(token).getPayload().getExpiration();
            return username.equals(userDetails.getUsername()) && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -Dtest=JwtServiceTest -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS, Tests run: 2, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add backend/src/
git commit -m "feat: add JwtService with unit tests"
```

---

### Task 7: JwtFilter + UserDetailsServiceImpl + SecurityConfig + OpenApiConfig

**Files:**
- Create: `security/JwtFilter.java`
- Create: `security/UserDetailsServiceImpl.java`
- Create: `config/SecurityConfig.java`
- Create: `config/OpenApiConfig.java`

- [ ] **Step 1: Create `security/UserDetailsServiceImpl.java`**

```java
package io.opspilot.desk.security;

import io.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return User.withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
            .build();
    }
}
```

- [ ] **Step 2: Create `security/JwtFilter.java`**

```java
package io.opspilot.desk.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception ignored) {}
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 3: Create `config/SecurityConfig.java`**

```java
package io.opspilot.desk.config;

import io.opspilot.desk.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 4: Create `config/OpenApiConfig.java`**

```java
package io.opspilot.desk.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("OpsPilot AI Desk API").version("1.0"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
```

- [ ] **Step 5: Compile check**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add Spring Security config, JWT filter, UserDetailsService"
```

---

### Task 8: Auth DTOs + DataInitializer + AuthService + AuthController

**Files:**
- Create: `dto/auth/` package
- Create: `service/DataInitializer.java`
- Create: `service/AuthService.java`
- Create: `controller/AuthController.java`

- [ ] **Step 1: Create auth DTOs**

`dto/auth/LoginRequest.java`:
```java
package io.opspilot.desk.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
```

`dto/auth/LoginResponse.java`:
```java
package io.opspilot.desk.dto.auth;
public record LoginResponse(String token, String email, String role) {}
```

`dto/auth/UserResponse.java`:
```java
package io.opspilot.desk.dto.auth;
import java.util.UUID;
public record UserResponse(UUID id, String email, String role, boolean active) {}
```

- [ ] **Step 2: Create `service/DataInitializer.java`**

```java
package io.opspilot.desk.service;

import io.opspilot.desk.entity.User;
import io.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        updatePasswordIfPlaceholder("admin@example.com", "admin123");
        updatePasswordIfPlaceholder("operator@example.com", "operator123");
    }

    private void updatePasswordIfPlaceholder(String email, String rawPassword) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if ("$PLACEHOLDER$".equals(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.info("Password initialized for {}", email);
            }
        });
    }
}
```

- [ ] **Step 3: Create `service/AuthService.java`**

```java
package io.opspilot.desk.service;

import io.opspilot.desk.dto.auth.*;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.repository.UserRepository;
import io.opspilot.desk.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        log.info("Login successful for {}", request.email());
        var userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        var user = userRepository.findByEmail(request.email()).orElseThrow();
        return new LoginResponse(token, user.getEmail(), user.getRole().name());
    }

    public UserResponse currentUser(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isActive());
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
            .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.isActive()))
            .toList();
    }
}
```

- [ ] **Step 4: Create `controller/AuthController.java`**

```java
package io.opspilot.desk.controller;

import io.opspilot.desk.dto.auth.*;
import io.opspilot.desk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(authService.currentUser(user.getUsername()));
    }
}
```

- [ ] **Step 5: Start backend, verify login**

Ensure `make db-up` is running, then:
```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml spring-boot:run &
sleep 15
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' | python3 -m json.tool
```

Expected: JSON with `token`, `email`, `role`

Stop the background server after verification: `kill %1`

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add auth endpoints, DataInitializer, JWT login"
```

---

### Task 9: Ticket status transition tests + TicketService

- [ ] **Step 1: Write failing test `service/TicketStatusTransitionTest.java`**

```java
package io.opspilot.desk.service;

import io.opspilot.desk.entity.Ticket.Status;
import io.opspilot.desk.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TicketStatusTransitionTest {
    private final TicketService service = new TicketService(null, null, null, null);

    @Test
    void newToInProgress_allowed() {
        assertThatCode(() -> service.validateTransition(Status.NEW, Status.IN_PROGRESS))
            .doesNotThrowAnyException();
    }

    @Test
    void newToResolved_notAllowed() {
        assertThatThrownBy(() -> service.validateTransition(Status.NEW, Status.RESOLVED))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void closedToAny_notAllowed() {
        assertThatThrownBy(() -> service.validateTransition(Status.CLOSED, Status.IN_PROGRESS))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void resolvedToClosed_allowed() {
        assertThatCode(() -> service.validateTransition(Status.RESOLVED, Status.CLOSED))
            .doesNotThrowAnyException();
    }

    @Test
    void inProgressToWaiting_allowed() {
        assertThatCode(() -> service.validateTransition(Status.IN_PROGRESS, Status.WAITING_FOR_CUSTOMER))
            .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -Dtest=TicketStatusTransitionTest -q 2>&1 | tail -5
```

Expected: FAILURE

- [ ] **Step 3: Create `service/TicketService.java`**

```java
package io.opspilot.desk.service;

import io.opspilot.desk.dto.ticket.*;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.entity.Ticket.*;
import io.opspilot.desk.exception.*;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        Status.NEW,                  Set.of(Status.IN_PROGRESS, Status.CLOSED),
        Status.IN_PROGRESS,          Set.of(Status.WAITING_FOR_CUSTOMER, Status.RESOLVED, Status.CLOSED),
        Status.WAITING_FOR_CUSTOMER, Set.of(Status.IN_PROGRESS, Status.RESOLVED, Status.CLOSED),
        Status.RESOLVED,             Set.of(Status.CLOSED, Status.IN_PROGRESS),
        Status.CLOSED,               Set.of()
    );

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NoteService noteService;

    public void validateTransition(Status from, Status to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest req, String creatorEmail) {
        var creator = userRepository.findByEmail(creatorEmail).orElseThrow();
        var ticket = new Ticket();
        ticket.setTitle(req.title());
        ticket.setDescription(req.description());
        ticket.setPriority(Priority.valueOf(req.priority()));
        ticket.setCategory(Category.valueOf(req.category()));
        ticket.setExternalRef(req.externalRef());
        ticket.setCreatedBy(creator);
        var saved = ticketRepository.save(ticket);
        log.info("Ticket created id={} by={}", saved.getId(), creatorEmail);
        auditService.log(saved, creator, "CREATED", null, saved.getStatus().name());
        return toResponse(saved);
    }

    public Page<TicketResponse> list(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(this::toResponse);
    }

    public TicketResponse getById(UUID id) {
        return toResponse(findTicket(id));
    }

    @Transactional
    public TicketResponse update(UUID id, UpdateTicketRequest req, String userEmail) {
        var ticket = findTicket(id);
        var user = userRepository.findByEmail(userEmail).orElseThrow();
        if (ticket.getStatus() == Status.CLOSED && !isAdmin(user)) {
            throw new AccessDeniedException("Only ADMIN can edit closed tickets");
        }
        if (req.title() != null) ticket.setTitle(req.title());
        if (req.description() != null) ticket.setDescription(req.description());
        if (req.priority() != null) ticket.setPriority(Priority.valueOf(req.priority()));
        if (req.category() != null) ticket.setCategory(Category.valueOf(req.category()));
        if (req.externalRef() != null) ticket.setExternalRef(req.externalRef());
        log.info("Ticket updated id={} by={}", id, userEmail);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(UUID id, ChangeStatusRequest req, String userEmail) {
        var ticket = findTicket(id);
        var user = userRepository.findByEmail(userEmail).orElseThrow();
        var newStatus = Status.valueOf(req.status());
        validateTransition(ticket.getStatus(), newStatus);
        var old = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (newStatus == Status.RESOLVED) ticket.setResolvedAt(Instant.now());
        ticketRepository.save(ticket);
        log.info("Ticket status changed id={} {}→{} by={}", id, old, newStatus, userEmail);
        auditService.log(ticket, user, "STATUS_CHANGED", old.name(), newStatus.name());
        noteService.addSystemNote(ticket, "Status changed: " + old + " → " + newStatus);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse assign(UUID id, AssignTicketRequest req, String userEmail) {
        var ticket = findTicket(id);
        var actor = userRepository.findByEmail(userEmail).orElseThrow();
        var assignee = req.assigneeId() != null
            ? userRepository.findById(req.assigneeId()).orElseThrow()
            : null;
        var old = ticket.getAssignedTo();
        ticket.setAssignedTo(assignee);
        ticketRepository.save(ticket);
        auditService.log(ticket, actor, "ASSIGNED",
            old != null ? old.getEmail() : null,
            assignee != null ? assignee.getEmail() : null);
        return toResponse(ticket);
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }

    public TicketResponse toResponse(Ticket t) {
        return new TicketResponse(
            t.getId(), t.getExternalRef(), t.getTitle(), t.getDescription(),
            t.getStatus().name(), t.getPriority().name(), t.getCategory().name(),
            t.getAssignedTo() != null ? t.getAssignedTo().getEmail() : null,
            t.getCreatedBy().getEmail(), t.getCreatedAt(), t.getUpdatedAt(),
            t.getResolvedAt(), t.getVersion()
        );
    }
}
```

- [ ] **Step 4: Create `service/AuditService.java`** (required by TicketService)

```java
package io.opspilot.desk.service;

import io.opspilot.desk.entity.*;
import io.opspilot.desk.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(Ticket ticket, User actor, String action, String oldValue, String newValue) {
        var entry = new AuditLog();
        entry.setTicket(ticket);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        auditLogRepository.save(entry);
    }
}
```

- [ ] **Step 5: Create `service/NoteService.java`** (required by TicketService)

```java
package io.opspilot.desk.service;

import io.opspilot.desk.dto.note.*;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final TicketNoteRepository noteRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoteResponse addNote(UUID ticketId, CreateNoteRequest req, String authorEmail) {
        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
        var author = userRepository.findByEmail(authorEmail).orElseThrow();
        var note = new TicketNote();
        note.setTicket(ticket);
        note.setAuthor(author);
        note.setBody(req.body());
        note.setVisibility(TicketNote.Visibility.INTERNAL);
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void addSystemNote(Ticket ticket, String body) {
        var note = new TicketNote();
        note.setTicket(ticket);
        note.setBody(body);
        note.setVisibility(TicketNote.Visibility.SYSTEM);
        noteRepository.save(note);
    }

    @Transactional
    public NoteResponse addAiSummaryNote(Ticket ticket, String body) {
        var note = new TicketNote();
        note.setTicket(ticket);
        note.setBody(body);
        note.setVisibility(TicketNote.Visibility.AI_SUMMARY);
        return toResponse(noteRepository.save(note));
    }

    public List<NoteResponse> listNotes(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return noteRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
            .map(this::toResponse).toList();
    }

    private NoteResponse toResponse(TicketNote n) {
        return new NoteResponse(n.getId(),
            n.getTicket().getId(),
            n.getAuthor() != null ? n.getAuthor().getEmail() : null,
            n.getBody(), n.getVisibility().name(), n.getCreatedAt());
    }
}
```

- [ ] **Step 6: Create ticket DTOs**

`dto/ticket/CreateTicketRequest.java`:
```java
package io.opspilot.desk.dto.ticket;
import jakarta.validation.constraints.*;
public record CreateTicketRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotBlank String priority,
    @NotBlank String category,
    String externalRef) {}
```

`dto/ticket/UpdateTicketRequest.java`:
```java
package io.opspilot.desk.dto.ticket;
import jakarta.validation.constraints.Size;
public record UpdateTicketRequest(
    @Size(max = 150) String title,
    @Size(max = 5000) String description,
    String priority, String category, String externalRef) {}
```

`dto/ticket/ChangeStatusRequest.java`:
```java
package io.opspilot.desk.dto.ticket;
import jakarta.validation.constraints.NotBlank;
public record ChangeStatusRequest(@NotBlank String status) {}
```

`dto/ticket/AssignTicketRequest.java`:
```java
package io.opspilot.desk.dto.ticket;
import java.util.UUID;
public record AssignTicketRequest(UUID assigneeId) {}
```

`dto/ticket/TicketResponse.java`:
```java
package io.opspilot.desk.dto.ticket;
import java.time.Instant;
import java.util.UUID;
public record TicketResponse(
    UUID id, String externalRef, String title, String description,
    String status, String priority, String category,
    String assignedToEmail, String createdByEmail,
    Instant createdAt, Instant updatedAt, Instant resolvedAt, Long version) {}
```

`dto/note/CreateNoteRequest.java`:
```java
package io.opspilot.desk.dto.note;
import jakarta.validation.constraints.NotBlank;
public record CreateNoteRequest(@NotBlank String body) {}
```

`dto/note/NoteResponse.java`:
```java
package io.opspilot.desk.dto.note;
import java.time.Instant;
import java.util.UUID;
public record NoteResponse(UUID id, UUID ticketId, String authorEmail, String body, String visibility, Instant createdAt) {}
```

- [ ] **Step 7: Run transition tests — expect PASS**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -Dtest=TicketStatusTransitionTest -q 2>&1 | tail -5
```

Expected: Tests run: 5, Failures: 0

- [ ] **Step 8: Commit**

```bash
git add backend/src/
git commit -m "feat: add TicketService with status transition validation, NoteService, AuditService"
```

---

### Task 10: TicketController + NoteController + UserController

- [ ] **Step 1: Create `controller/TicketController.java`**

```java
package io.opspilot.desk.controller;

import io.opspilot.desk.dto.ticket.*;
import io.opspilot.desk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ticketService.create(req, user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Sort.Direction direction = Sort.Direction.fromString(dir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return ResponseEntity.ok(ticketService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.update(id, req, user.getUsername()));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<TicketResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.changeStatus(id, req, user.getUsername()));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable UUID id,
            @RequestBody AssignTicketRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.assign(id, req, user.getUsername()));
    }
}
```

- [ ] **Step 2: Create `controller/NoteController.java`**

```java
package io.opspilot.desk.controller;

import io.opspilot.desk.dto.note.*;
import io.opspilot.desk.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateNoteRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(noteService.addNote(ticketId, req, user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> listNotes(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(noteService.listNotes(ticketId));
    }
}
```

- [ ] **Step 3: Create user DTOs and `controller/UserController.java`**

`dto/user/UserListResponse.java`:
```java
package io.opspilot.desk.dto.user;
import java.util.UUID;
public record UserListResponse(UUID id, String email, String role, boolean active) {}
```

`controller/UserController.java`:
```java
package io.opspilot.desk.controller;

import io.opspilot.desk.dto.auth.UserResponse;
import io.opspilot.desk.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }
}
```

- [ ] **Step 4: Compile check**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/
git commit -m "feat: add TicketController, NoteController, UserController"
```

---

### Task 11: AI client — PromptTemplates tests + FakeAiClient + OpenRouterClient

- [ ] **Step 1: Create `ai/AiRequest.java` and `ai/AiResponse.java`**

`ai/AiRequest.java`:
```java
package io.opspilot.desk.ai;
import java.util.List;
public record AiRequest(String systemPrompt, List<Message> messages) {
    public record Message(String role, String content) {}
}
```

`ai/AiResponse.java`:
```java
package io.opspilot.desk.ai;
public record AiResponse(String content, int promptTokens, int completionTokens, String model, String errorMessage) {
    public boolean isError() { return errorMessage != null; }
}
```

- [ ] **Step 2: Create `ai/PromptTemplates.java`**

```java
package io.opspilot.desk.ai;

public final class PromptTemplates {
    public static final String VERSION = "v1";

    public static final String SYSTEM_BASE =
        "You are an AI assistant for OpsPilot AI Desk, helping field operations support teams. " +
        "Be concise, professional, and focus on actionable advice.";

    public static final String SUMMARIZE_TICKET =
        "Summarize this support ticket in 2-3 sentences. Focus on the issue, current status, and any blockers.";

    public static final String SUGGEST_NEXT_ACTION =
        "Based on this ticket, suggest the most appropriate next action for the operator. " +
        "Be specific and actionable.";

    public static final String DRAFT_CUSTOMER_REPLY =
        "Draft a professional customer-facing reply for this ticket. " +
        "Be empathetic, clear, and include next steps if applicable.";

    public static final String IDENTIFY_MISSING_INFO =
        "Identify any missing information in this ticket that would help resolve it faster. " +
        "List each missing piece as a bullet point.";

    public static final String CLASSIFY_PRIORITY_CATEGORY =
        "Based on the ticket description, suggest the most appropriate priority (LOW/MEDIUM/HIGH/CRITICAL) " +
        "and category (DELIVERY/PICKUP/DOCUMENTATION/CUSTOMER/SYSTEM/OTHER). " +
        "Respond with: Priority: X, Category: Y, Reason: ...";

    private PromptTemplates() {}
}
```

- [ ] **Step 3: Write failing test `ai/PromptBuilderTest.java`**

```java
package io.opspilot.desk.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PromptBuilderTest {
    @Test
    void allTemplatesNonBlank() {
        assertThat(PromptTemplates.SYSTEM_BASE).isNotBlank();
        assertThat(PromptTemplates.SUMMARIZE_TICKET).isNotBlank();
        assertThat(PromptTemplates.SUGGEST_NEXT_ACTION).isNotBlank();
        assertThat(PromptTemplates.DRAFT_CUSTOMER_REPLY).isNotBlank();
        assertThat(PromptTemplates.IDENTIFY_MISSING_INFO).isNotBlank();
        assertThat(PromptTemplates.CLASSIFY_PRIORITY_CATEGORY).isNotBlank();
    }

    @Test
    void versionTagPresent() {
        assertThat(PromptTemplates.VERSION).isNotBlank();
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -Dtest=PromptBuilderTest -q 2>&1 | tail -5
```

Expected: Tests run: 2, Failures: 0

- [ ] **Step 5: Create `ai/AiClient.java`**

```java
package io.opspilot.desk.ai;

public interface AiClient {
    AiResponse chat(AiRequest request);
}
```

- [ ] **Step 6: Create `ai/FakeAiClient.java`**

```java
package io.opspilot.desk.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "fake", matchIfMissing = true)
@Slf4j
public class FakeAiClient implements AiClient {
    @Override
    public AiResponse chat(AiRequest request) {
        log.info("FakeAiClient processing request with {} messages", request.messages().size());
        String lastUserMessage = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .reduce((a, b) -> b)
            .map(AiRequest.Message::content)
            .orElse("");
        String response = generateFakeResponse(lastUserMessage);
        return new AiResponse(response, 100, 50, "fake/model", null);
    }

    private String generateFakeResponse(String input) {
        if (input.toLowerCase().contains("summar")) {
            return "[FAKE AI] Ticket summary: This is a simulated summary of the support ticket. The issue appears to be operational in nature and requires follow-up with the field team.";
        }
        if (input.toLowerCase().contains("next action") || input.toLowerCase().contains("suggest")) {
            return "[FAKE AI] Suggested next action: Contact the assigned operator to gather additional details and update the ticket status accordingly.";
        }
        if (input.toLowerCase().contains("reply") || input.toLowerCase().contains("draft")) {
            return "[FAKE AI] Draft reply: Dear customer, thank you for contacting us. We have reviewed your request and our team is actively working on a resolution. We will update you shortly.";
        }
        if (input.toLowerCase().contains("missing") || input.toLowerCase().contains("information")) {
            return "[FAKE AI] Missing information:\n- Customer contact details\n- Exact time of incident\n- Affected location or route";
        }
        if (input.toLowerCase().contains("classify") || input.toLowerCase().contains("priority")) {
            return "[FAKE AI] Priority: MEDIUM, Category: OTHER, Reason: Based on the description, this appears to be a standard operational issue.";
        }
        return "[FAKE AI] I understand your request. This is a simulated AI response for development and testing purposes.";
    }
}
```

- [ ] **Step 7: Create `ai/OpenRouterClient.java`**

```java
package io.opspilot.desk.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opspilot.desk.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter")
@Slf4j
public class OpenRouterClient implements AiClient {
    private final RestClient restClient;
    private final AiProperties props;

    public OpenRouterClient(AiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
            .baseUrl(props.getOpenrouter().getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + props.getOpenrouter().getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public AiResponse chat(AiRequest request) {
        var messages = buildMessages(request);
        var body = Map.of(
            "model", props.getOpenrouter().getModel(),
            "messages", messages,
            "max_tokens", props.getOpenrouter().getMaxTokens(),
            "temperature", props.getOpenrouter().getTemperature()
        );

        log.info("AI request model={} messages={}", props.getOpenrouter().getModel(), messages.size());
        long start = System.currentTimeMillis();
        try {
            var response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(OpenRouterResponse.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("AI response elapsed={}ms model={}", elapsed, props.getOpenrouter().getModel());

            var choice = response.choices().get(0);
            var usage = response.usage();
            return new AiResponse(
                choice.message().content(),
                usage != null ? usage.promptTokens() : 0,
                usage != null ? usage.completionTokens() : 0,
                props.getOpenrouter().getModel(),
                null
            );
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI request failed elapsed={}ms error={}", elapsed, e.getMessage());
            return new AiResponse(null, 0, 0, props.getOpenrouter().getModel(), e.getMessage());
        }
    }

    private List<Map<String, String>> buildMessages(AiRequest request) {
        var msgs = new java.util.ArrayList<Map<String, String>>();
        if (request.systemPrompt() != null) {
            msgs.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        request.messages().forEach(m -> msgs.add(Map.of("role", m.role(), "content", m.content())));
        return msgs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterResponse(List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                 @JsonProperty("completion_tokens") int completionTokens) {}
}
```

- [ ] **Step 8: Compile check**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add backend/src/
git commit -m "feat: add AiClient interface, FakeAiClient, OpenRouterClient, PromptTemplates"
```

---

### Task 12: AiService + AiController + DTOs

- [ ] **Step 1: Create AI DTOs**

`dto/ai/SendMessageRequest.java`:
```java
package io.opspilot.desk.dto.ai;
import jakarta.validation.constraints.NotBlank;
public record SendMessageRequest(@NotBlank String content) {}
```

`dto/ai/ChatMessageResponse.java`:
```java
package io.opspilot.desk.dto.ai;
import java.time.Instant;
import java.util.UUID;
public record ChatMessageResponse(UUID id, String role, String content, String model,
    Integer promptTokens, Integer completionTokens, Instant createdAt, boolean error, String errorMessage) {}
```

`dto/ai/ChatSessionResponse.java`:
```java
package io.opspilot.desk.dto.ai;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ChatSessionResponse(UUID id, UUID ticketId, Instant createdAt, List<ChatMessageResponse> messages) {}
```

`dto/ai/AiActionResponse.java`:
```java
package io.opspilot.desk.dto.ai;
public record AiActionResponse(String content, boolean success, String error) {}
```

- [ ] **Step 2: Create `service/AiService.java`**

```java
package io.opspilot.desk.service;

import io.opspilot.desk.ai.*;
import io.opspilot.desk.dto.ai.*;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final AiClient aiClient;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final NoteService noteService;

    @Transactional
    public ChatSessionResponse getOrCreateSession(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var session = sessionRepository.findByTicket(ticket)
            .orElseGet(() -> {
                var s = new ChatSession();
                s.setTicket(ticket);
                return sessionRepository.save(s);
            });
        var messages = messageRepository.findBySessionOrderByCreatedAtAsc(session)
            .stream().map(this::toMessageResponse).toList();
        return new ChatSessionResponse(session.getId(), ticketId, session.getCreatedAt(), messages);
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID ticketId, String userContent) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var session = sessionRepository.findByTicket(ticket).orElseGet(() -> {
            var s = new ChatSession(); s.setTicket(ticket); return sessionRepository.save(s);
        });

        var userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(ChatMessage.Role.USER);
        userMsg.setContent(userContent);
        messageRepository.save(userMsg);

        var history = messageRepository.findBySessionOrderByCreatedAtAsc(session);
        var requestMessages = history.stream()
            .map(m -> new AiRequest.Message(m.getRole().name().toLowerCase(), m.getContent()))
            .toList();

        var ticketContext = buildTicketContext(ticket);
        var aiRequest = new AiRequest(PromptTemplates.SYSTEM_BASE + "\n\nTicket context:\n" + ticketContext, requestMessages);
        var aiResponse = aiClient.chat(aiRequest);

        var assistantMsg = new ChatMessage();
        assistantMsg.setSession(session);
        assistantMsg.setRole(ChatMessage.Role.ASSISTANT);
        assistantMsg.setContent(aiResponse.isError() ? "" : aiResponse.content());
        assistantMsg.setModel(aiResponse.model());
        assistantMsg.setPromptTokens(aiResponse.promptTokens());
        assistantMsg.setCompletionTokens(aiResponse.completionTokens());
        assistantMsg.setError(aiResponse.isError());
        assistantMsg.setErrorMessage(aiResponse.errorMessage());
        return toMessageResponse(messageRepository.save(assistantMsg));
    }

    @Transactional
    public AiActionResponse generateSummary(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var context = buildTicketContext(ticket);
        var request = new AiRequest(PromptTemplates.SYSTEM_BASE,
            List.of(new AiRequest.Message("user", PromptTemplates.SUMMARIZE_TICKET + "\n\n" + context)));
        var response = aiClient.chat(request);
        if (response.isError()) return new AiActionResponse(null, false, response.errorMessage());
        return new AiActionResponse(response.content(), true, null);
    }

    @Transactional
    public AiActionResponse generateSuggestedReply(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        var context = buildTicketContext(ticket);
        var request = new AiRequest(PromptTemplates.SYSTEM_BASE,
            List.of(new AiRequest.Message("user", PromptTemplates.DRAFT_CUSTOMER_REPLY + "\n\n" + context)));
        var response = aiClient.chat(request);
        if (response.isError()) return new AiActionResponse(null, false, response.errorMessage());
        return new AiActionResponse(response.content(), true, null);
    }

    @Transactional
    public NoteResponse applyAiSummaryAsNote(UUID ticketId, String content) {
        var ticket = ticketRepository.findById(ticketId).orElseThrow();
        return noteService.addAiSummaryNote(ticket, content);
    }

    public long countAiInteractionsToday() {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return messageRepository.countAssistantMessagesSince(startOfDay);
    }

    private String buildTicketContext(Ticket t) {
        return "Title: " + t.getTitle() + "\n" +
            "Description: " + t.getDescription() + "\n" +
            "Status: " + t.getStatus() + "\n" +
            "Priority: " + t.getPriority() + "\n" +
            "Category: " + t.getCategory();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getRole().name(), m.getContent(),
            m.getModel(), m.getPromptTokens(), m.getCompletionTokens(),
            m.getCreatedAt(), m.isError(), m.getErrorMessage());
    }
}
```

- [ ] **Step 3: Import NoteResponse in AiService** — add to imports: `import io.opspilot.desk.dto.note.NoteResponse;`

- [ ] **Step 4: Create `controller/AiController.java`**

```java
package io.opspilot.desk.controller;

import io.opspilot.desk.dto.ai.*;
import io.opspilot.desk.dto.note.NoteResponse;
import io.opspilot.desk.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @GetMapping("/session")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(aiService.getOrCreateSession(ticketId));
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(aiService.sendMessage(ticketId, req.content()));
    }

    @PostMapping("/summary")
    public ResponseEntity<AiActionResponse> generateSummary(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(aiService.generateSummary(ticketId));
    }

    @PostMapping("/suggested-reply")
    public ResponseEntity<AiActionResponse> suggestedReply(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(aiService.generateSuggestedReply(ticketId));
    }

    @PostMapping("/apply-summary")
    public ResponseEntity<NoteResponse> applySummary(
            @PathVariable UUID ticketId,
            @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(aiService.applyAiSummaryAsNote(ticketId, req.content()));
    }
}
```

- [ ] **Step 5: Compile check**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add AiService and AiController"
```

---

### Task 13: Dashboard + GlobalExceptionHandler

- [ ] **Step 1: Create `dto/dashboard/DashboardResponse.java`**

```java
package io.opspilot.desk.dto.dashboard;

import io.opspilot.desk.dto.ticket.TicketResponse;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
    Map<String, Long> ticketsByStatus,
    Map<String, Long> ticketsByPriority,
    List<TicketResponse> myOpenTickets,
    List<TicketResponse> recentlyUpdated,
    long aiInteractionsToday) {}
```

- [ ] **Step 2: Create `service/DashboardService.java`**

```java
package io.opspilot.desk.service;

import io.opspilot.desk.dto.dashboard.DashboardResponse;
import io.opspilot.desk.entity.Ticket;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final TicketService ticketService;

    public DashboardResponse getDashboard(String userEmail) {
        var byStatus = Arrays.stream(Ticket.Status.values())
            .collect(Collectors.toMap(Enum::name, ticketRepository::countByStatus));

        var byPriority = Arrays.stream(Ticket.Priority.values())
            .collect(Collectors.toMap(Enum::name, ticketRepository::countByPriority));

        var myTickets = userRepository.findByEmail(userEmail)
            .map(u -> ticketRepository.findByAssignedToAndStatusNot(u, Ticket.Status.CLOSED)
                .stream().map(ticketService::toResponse).toList())
            .orElse(List.of());

        var recent = ticketRepository.findTop10ByOrderByUpdatedAtDesc()
            .stream().map(ticketService::toResponse).toList();

        long aiToday = aiService.countAiInteractionsToday();

        return new DashboardResponse(byStatus, byPriority, myTickets, recent, aiToday);
    }
}
```

- [ ] **Step 3: Create `controller/DashboardController.java`**

```java
package io.opspilot.desk.controller;

import io.opspilot.desk.dto.dashboard.DashboardResponse;
import io.opspilot.desk.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(dashboardService.getDashboard(user.getUsername()));
    }
}
```

- [ ] **Step 4: Create `exception/GlobalExceptionHandler.java`**

```java
package io.opspilot.desk.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(TicketNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidStatusTransitionException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
        return ResponseEntity.badRequest().body(Map.of(
            "timestamp", Instant.now(),
            "status", 400,
            "error", "Validation failed",
            "details", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", Instant.now(),
            "status", status.value(),
            "error", message));
    }
}
```

- [ ] **Step 5: Compile and full test run**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -q 2>&1 | tail -10
```

Expected: Tests run: 9+, Failures: 0 (unit tests pass; integration tests skipped without Docker)

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add Dashboard, GlobalExceptionHandler"
```

---

### Task 14: Integration tests

**Files:**
- Create: `src/test/java/io/opspilot/desk/integration/TicketControllerIT.java`
- Create: `src/test/java/io/opspilot/desk/integration/AuthSecurityIT.java`

- [ ] **Step 1: Create `integration/AuthSecurityIT.java`**

```java
package io.opspilot.desk.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthSecurityIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("opspilot")
        .withUsername("opspilot")
        .withPassword("opspilot");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/tickets"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Create `integration/TicketControllerIT.java`**

```java
package io.opspilot.desk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("opspilot")
        .withUsername("opspilot")
        .withPassword("opspilot");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String operatorToken;
    static String createdTicketId;

    @Test @Order(1)
    void loginAsOperator() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"operator@example.com\",\"password\":\"operator123\"}"))
            .andExpect(status().isOk()).andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        operatorToken = body.get("token").asText();
    }

    @Test @Order(2)
    void createTicket() throws Exception {
        var result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test ticket\",\"description\":\"Test description\",\"priority\":\"HIGH\",\"category\":\"DELIVERY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test ticket"))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdTicketId = body.get("id").asText();
    }

    @Test @Order(3)
    void listTickets() throws Exception {
        mockMvc.perform(get("/api/tickets")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test @Order(4)
    void changeTicketStatus() throws Exception {
        mockMvc.perform(post("/api/tickets/" + createdTicketId + "/status")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @Order(5)
    void invalidStatusTransitionReturns422() throws Exception {
        mockMvc.perform(post("/api/tickets/" + createdTicketId + "/status")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"NEW\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
}
```

- [ ] **Step 3: Run integration tests (requires Docker)**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test -Dtest="AuthSecurityIT,TicketControllerIT" 2>&1 | tail -15
```

Expected: Tests run: 8, Failures: 0 (may take 30-60s for Testcontainers to start)

- [ ] **Step 4: Run all backend tests**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test 2>&1 | tail -10
```

Expected: All tests pass (unit + integration)

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/
git commit -m "test: add integration tests for auth security and ticket CRUD"
```

---

Backend implementation complete. Proceed to `docs/superpowers/plans/2026-05-15-opspilot-frontend.md`.
