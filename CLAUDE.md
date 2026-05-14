# CLAUDE.md

## MANDATORY: GSD Workflow Enforcement

**You MUST use the GSD workflow for ALL implementation work. No exceptions.**

- NEVER write or edit source files directly with Write/Edit tools.
- ALWAYS go through the workflow:
  1. `/gsd-plan-phase <N>` — to plan a phase
  2. `/gsd-execute-phase <N>` — to execute it
  3. `/gsd-fast` — only for trivial one-line fixes explicitly requested by the user
- If you find yourself about to call Write or Edit on a source file without an active GSD phase, **STOP** and invoke the workflow first.
- STATE.md and ROADMAP.md must reflect actual progress after every phase.
- "YOLO mode" and "autonomous" mean: run the GSD workflow autonomously without asking for confirmation — they do NOT mean bypass the workflow.

A PreToolUse hook will **BLOCK** any direct Write/Edit to source files. If blocked, do not retry — invoke the GSD workflow.

---

## Project: AI Support Desk for Field Operations

You are working on a greenfield full-stack project named **OpsPilot AI Desk**.

The goal is to build a production-oriented web application for transport/logistics field support teams.
The application must include:
- a web interface for operators;
- a backend API;
- persistence;
- authentication and authorization;
- an AI chat service based on OpenRouter;
- an auditable ticket workflow;
- tests and developer documentation.

This file is intentionally detailed because it will be used to benchmark different Claude Code plugins, workflows, agents, and spec-driven approaches.

---

## Technology constraints

### Backend

Use:
- Java 21
- Spring Boot 3.3.x or newer stable 3.x
- Maven
- Lombok
- Spring Web
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Validation API
- OpenAPI/Swagger generation
- Testcontainers for integration tests
- JUnit 5

Prefer a layered architecture:
- controller
- DTO/request/response classes
- service
- repository
- entity
- mapper
- configuration
- security
- integration clients

### Frontend

Use:
- Angular 17+ or a recent stable Angular version
- TypeScript
- standalone components where appropriate
- reactive forms
- route guards
- a small design system or reusable UI components
- HTTP interceptors
- environment-based configuration

A minimal but real UI is required. Do not create only placeholder pages.

### AI provider

Use OpenRouter for AI chat.

The backend must call OpenRouter through a dedicated service/client abstraction.
Do not call OpenRouter directly from the frontend.

OpenRouter configuration must be externalized:
- `OPENROUTER_API_KEY`
- `OPENROUTER_BASE_URL`, default `https://openrouter.ai/api/v1`
- `OPENROUTER_MODEL`, default a cheap configurable model
- request timeout
- max tokens
- temperature

The OpenRouter client must support:
- system prompt
- conversation messages
- model selection from configuration
- basic error handling
- timeout handling
- structured logging without leaking API keys
- fake/mock implementation for tests and local development

---

## Functional scope

### 1. Authentication

Implement simple JWT-based authentication.

There must be two predefined roles:
- `ADMIN`
- `OPERATOR`

For the benchmark, it is acceptable to use local users stored in the database.

Required features:
- login endpoint
- JWT issuing
- JWT validation
- current user endpoint
- route protection in the frontend
- role-based authorization in the backend

Seed at least two users:
- admin@example.com / admin123 / ADMIN
- operator@example.com / operator123 / OPERATOR

Passwords must be hashed.

---

### 2. Tickets

A ticket represents an operational issue raised by a field operator.

Ticket fields:
- id
- external reference
- title
- description
- status
- priority
- category
- assigned operator
- created by
- created at
- updated at
- resolved at
- version for optimistic locking

Allowed statuses:
- `NEW`
- `IN_PROGRESS`
- `WAITING_FOR_CUSTOMER`
- `RESOLVED`
- `CLOSED`

Allowed priorities:
- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

Allowed categories:
- `DELIVERY`
- `PICKUP`
- `DOCUMENTATION`
- `CUSTOMER`
- `SYSTEM`
- `OTHER`

Required ticket features:
- create ticket
- list tickets with pagination and filters
- get ticket by id
- update ticket metadata
- change ticket status
- assign ticket to an operator
- add internal note
- close ticket
- optimistic locking on update
- audit trail for important changes

Validation rules:
- title is required and max 150 characters
- description is required and max 5000 characters
- external reference is optional but unique when present
- closed tickets cannot be edited except by ADMIN
- status transitions must be validated

---

### 3. Ticket notes

Ticket notes are internal comments.

Fields:
- id
- ticket id
- author
- body
- created at
- visibility

Visibility:
- `INTERNAL`
- `AI_SUMMARY`
- `SYSTEM`

Operators may create `INTERNAL` notes.
AI-generated summaries must be stored as `AI_SUMMARY`.
System events must be stored as `SYSTEM`.

---

### 4. AI chat assistant

Operators can open an AI chat panel linked to a ticket.

The AI assistant must help the operator:
- summarize the ticket;
- suggest the next action;
- draft a customer-facing answer;
- identify missing information;
- classify priority and category.

Required backend endpoints:
- start or retrieve chat session for a ticket
- send message
- list messages
- generate ticket summary
- generate suggested reply
- apply AI summary as ticket note

Chat message fields:
- id
- session id
- role: `SYSTEM`, `USER`, `ASSISTANT`
- content
- model
- token estimate or provider token usage when available
- created at
- error flag
- error message

Important requirements:
- AI calls must be auditable.
- Prompt templates must be versioned in code.
- AI responses must never automatically modify ticket status.
- Applying an AI suggestion must require an explicit user action.
- If OpenRouter fails, the UI must show a clear recoverable error.
- Secrets must never be logged or returned to the frontend.

---

### 5. Dashboard

Create a dashboard page showing:
- tickets by status
- tickets by priority
- my assigned open tickets
- recently updated tickets
- count of AI interactions today

The dashboard may use simple cards and tables.
Charts are optional.

---

### 6. Frontend pages

Implement at least these pages:
- login
- dashboard
- ticket list
- ticket detail
- create ticket
- AI chat panel inside ticket detail
- admin user list or minimal admin page

The frontend must include:
- API service layer
- auth service
- route guards
- token persistence
- error handling
- loading states
- basic responsive layout

---

## Non-functional requirements

### Architecture

The solution must be understandable and maintainable.

Required documentation:
- architecture overview
- local setup guide
- API overview
- AI integration notes
- test strategy
- known limitations

### Security

- Hash passwords.
- Do not expose OpenRouter API key.
- Do not log JWT tokens.
- Do not log OpenRouter API key.
- Validate all incoming requests.
- Enforce authorization both in backend and frontend.
- CORS must be configurable.

### Observability

Add structured logs for:
- authentication events
- ticket creation/update
- status changes
- AI request start/end/error
- OpenRouter model used
- elapsed time for AI calls

Do not log sensitive data.

### Testing

Minimum expected tests:
- unit tests for status transition rules
- unit tests for AI prompt builder
- integration tests for ticket API
- security tests for protected endpoints
- frontend unit tests for at least one service and one component

### Docker/local dev

Provide:
- docker-compose with PostgreSQL
- backend run instructions
- frontend run instructions
- environment variable examples
- Flyway migrations
- docker-compose profile for full stack
- Makefile

---

## Suggested repository structure

Work must be done in the starting branch for the project, pushng or opening PR vs main if forbidden.
Agents can create new branches from the starting branch, and merge toward it as needed.

Update the .gitignore as required.

Preferred monorepo layout:

```text
opspilot-ai-desk/
  CLAUDE.md
  README.md
  docs/
  backend/
    pom.xml
    src/main/java/...
    src/main/resources/
    src/test/java/...
  frontend/
    package.json
    src/
  docker-compose.yml
  .env.example
```

---

## Implementation expectations

The output should not be a toy skeleton.

A good implementation must:
- compile;
- run locally;
- include database migrations;
- expose usable REST APIs;
- provide a usable web interface;
- include meaningful tests;
- document design decisions;
- isolate OpenRouter behind a service abstraction;
- support a fake AI provider for tests;
- include at least one end-to-end happy path documented in the README.

---

## Definition of done

The project is complete when:

1. Backend starts successfully.
2. Frontend starts successfully.
3. PostgreSQL schema is created by Flyway.
4. Seed users can log in.
5. Operator can create and update tickets.
6. Ticket status transitions are enforced.
7. AI chat works through OpenRouter when configured.
8. Fake AI mode works without external API calls.
9. Tests run successfully or failures are documented with reasons.
10. README explains setup, architecture, and limitations.

