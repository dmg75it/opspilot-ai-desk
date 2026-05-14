# Phase 1: Infrastructure and Auth Foundation - Context

**Gathered:** 2026-05-14
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

Stand up a runnable backend with correct JWT authentication, role-based authorization, seeded users, and a reproducible local development environment.

Requirements: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, INFRA-01, INFRA-02, INFRA-03, INFRA-06, INFRA-07

</domain>

<decisions>
## Implementation Decisions

- Maven multi-module: NO — single backend module under /backend for simplicity
- Spring Boot version: 3.3.x (latest stable 3.x)
- JWT library: JJWT 0.12.6 (use new 0.12.x API: parseSignedClaims, verifyWith)
- JWT filter: extends OncePerRequestFilter, NOT annotated with @Component (to avoid double-registration)
- Security: SecurityFilterChain bean (not WebSecurityConfigurerAdapter — removed in Boot 3.x)
- CORS: configured inside SecurityFilterChain via CorsConfigurationSource bean (NOT WebMvcConfigurer)
- Virtual threads: spring.threads.virtual.enabled=true (Java 21 + Boot 3.2+)
- springdoc-openapi: 2.8.x
- Password hashing: BCrypt
- Flyway: flyway-core + flyway-database-postgresql (required separately in Boot 3.x)
- Migration naming: V1__init.sql (double underscore)
- open-in-view: false (prevent JPA lazy loading issues)
- ddl-auto: validate (Flyway manages schema)
- .env support: via dotenv-java or spring-dotenv at dev; in prod use system env
- clean-disabled: true (safety)

</decisions>

<code_context>
## Existing Code Insights

Greenfield project. No existing code to analyze.
</code_context>

<specifics>
## Specific Requirements

- POST /api/auth/login → returns JWT token
- GET /api/auth/me → returns current user profile (email, role)
- Seed users via Flyway migration: admin@example.com/admin123/ADMIN, operator@example.com/operator123/OPERATOR
- Docker Compose: postgres service with healthcheck, .env file support
- Makefile: make up, make backend, make frontend, make test targets
- CORS: allow http://localhost:4200 (from CORS_ALLOWED_ORIGINS env)
- JWT expiration: configurable via JWT_EXPIRATION_MS env
- Structured logging for auth events (login success/failure, token validation)

</specifics>

<deferred>
## Deferred Ideas

- Refresh token endpoint — deferred to v2
- Password reset — deferred to v2
- OAuth2 — out of scope

</deferred>
