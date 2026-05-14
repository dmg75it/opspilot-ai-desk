---
phase: 01-infrastructure-and-auth-foundation
plan: "03"
subsystem: backend-auth
tags: [spring-security, jwt, jjwt, bcrypt, cors, lombok]
dependency_graph:
  requires:
    - "01-01 (Maven scaffold, pom.xml)"
    - "01-02 (Flyway V1 users table, V2 seed users)"
  provides:
    - "POST /api/auth/login - JWT token issuance"
    - "GET /api/auth/me - authenticated user profile"
    - "JwtAuthFilter - token validation on every request"
    - "SecurityFilterChain - CORS, CSRF disable, stateless sessions, role-based authorization"
    - "GlobalExceptionHandler - structured error responses"
  affects:
    - "all subsequent plans that require authentication"
    - "01-04 (integration tests will exercise these endpoints)"
tech_stack:
  added:
    - "JJWT 0.12.6 (jjwt-api, jjwt-impl, jjwt-jackson) - JWT generation and validation"
    - "Spring Security 6 SecurityFilterChain pattern"
    - "BCryptPasswordEncoder - password verification"
    - "Lombok 1.18.46 - bumped from 1.18.34 for Java 25 compatibility"
  patterns:
    - "OncePerRequestFilter without @Component - prevents double-registration"
    - "SecurityContextHolder.getContextHolderStrategy() - Spring Security 6 strategy-aware auth"
    - "CorsConfigurationSource inside SecurityFilterChain - not WebMvcConfigurer"
    - "setAllowedOriginPatterns() with allowCredentials=true"
    - "Java records for DTOs (LoginRequest, AuthResponse, UserResponse)"
    - "Layered architecture: controller -> DTO -> service -> repository -> entity"
key_files:
  created:
    - backend/src/main/java/com/opspilot/entity/enums/Role.java
    - backend/src/main/java/com/opspilot/entity/User.java
    - backend/src/main/java/com/opspilot/repository/UserRepository.java
    - backend/src/main/java/com/opspilot/security/UserDetailsServiceImpl.java
    - backend/src/main/java/com/opspilot/security/JwtService.java
    - backend/src/main/java/com/opspilot/security/JwtAuthFilter.java
    - backend/src/main/java/com/opspilot/config/SecurityConfig.java
    - backend/src/main/java/com/opspilot/dto/request/LoginRequest.java
    - backend/src/main/java/com/opspilot/dto/response/AuthResponse.java
    - backend/src/main/java/com/opspilot/dto/response/UserResponse.java
    - backend/src/main/java/com/opspilot/service/AuthService.java
    - backend/src/main/java/com/opspilot/controller/AuthController.java
    - backend/src/main/java/com/opspilot/exception/GlobalExceptionHandler.java
  modified:
    - backend/pom.xml (Lombok annotation processor bumped to 1.18.46, lombok.version property added)
decisions:
  - "JwtAuthFilter has no @Component - only instantiated via @Bean factory in SecurityConfig"
  - "JJWT 0.12.6 parseSignedClaims() API used - not deprecated parseClaimsJws()"
  - "SecurityContextHolder.getContextHolderStrategy().getContext().setAuthentication() used in filter"
  - "Lombok 1.18.46 required for Java 25 - 1.18.34/1.18.36 fail with TypeTag.UNKNOWN NoSuchFieldException"
  - "GlobalExceptionHandler returns generic 'Invalid credentials' for both BadCredentialsException and UsernameNotFoundException - prevents email enumeration"
metrics:
  duration: 224s
  completed: "2026-05-14"
  tasks_completed: 3
  files_created: 13
  files_modified: 1
---

# Phase 01 Plan 03: Spring Security JWT Auth Stack Summary

**Complete Spring Security JWT authentication stack: entity, repository, JWT service, filter, security config, DTOs, auth service, auth controller, and global exception handler**

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Entity layer: Role, User, UserRepository, UserDetailsServiceImpl | b8aeef7 | 4 files + pom.xml |
| 2 | Security layer: JwtService, JwtAuthFilter, SecurityConfig | 2778834 | 3 files |
| 3 | Application layer: DTOs, AuthService, AuthController, GlobalExceptionHandler | 9e07b32 | 6 files |

## Verification Results

| Check | Result |
|-------|--------|
| `./mvnw compile` (after Task 1) | PASS - no errors |
| `./mvnw compile` (after Task 2) | PASS - no errors |
| `./mvnw compile` (after Task 3) | PASS - no errors |
| JwtAuthFilter has no @Component | PASS - grep on `^@Component` returns nothing |
| JwtService uses `parseSignedClaims` and `verifyWith` | PASS - both present at lines 55, 57 |
| JwtAuthFilter uses `getContextHolderStrategy()` | PASS - line 52 |
| AuthService logs "AUTH login success" at INFO | PASS - line 39 |
| AuthService logs "AUTH login failure" at WARN | PASS - line 33 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Lombok annotation processor Java 25 incompatibility**
- **Found during:** Task 1 first compilation attempt
- **Issue:** Lombok 1.18.34 (pinned in pom.xml annotation processor path) threw `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` on Java 25. The `TypeTag.UNKNOWN` field was removed in Java 25. Lombok 1.18.36 (Spring Boot BOM-managed) has the same issue. Lombok 1.18.46 (latest available on Maven Central as of 2026-05-14) fixes this.
- **Fix:** Updated `<lombok.version>` property in `pom.xml` to `1.18.46` and updated the `maven-compiler-plugin` annotation processor path from `1.18.34` to `1.18.46`.
- **Files modified:** `backend/pom.xml`
- **Commit:** b8aeef7

## Known Stubs

None. All 13 source files implement real functionality. No placeholder or hardcoded values in business logic.

## Threat Surface Scan

No threat surface beyond the plan's threat model. Threat register T-03-01 through T-03-07 fully addressed:

- **T-03-01 (Tampering/JWT):** JJWT `verifyWith()` + `parseSignedClaims()` rejects alg=none and unsigned tokens.
- **T-03-02 (JWT secret disclosure):** `jwt.secret=${JWT_SECRET}` with no default — missing env var fails fast at startup.
- **T-03-03 (Password logging):** `AuthService` logs only email, never password. Password never in any response.
- **T-03-04 (Empty credentials):** `@NotBlank` on `LoginRequest` fields; `@Valid` on controller parameter.
- **T-03-05 (Privilege escalation):** `/api/admin/**` requires `hasRole("ADMIN")`; all other endpoints require `authenticated()`.
- **T-03-06 (Brute-force):** Accepted for Phase 1 — documented known limitation.
- **T-03-07 (Error info disclosure):** `GlobalExceptionHandler` returns generic "Invalid credentials" for both `BadCredentialsException` and `UsernameNotFoundException` — prevents email enumeration.

## Self-Check: PASSED

- backend/src/main/java/com/opspilot/entity/enums/Role.java: FOUND
- backend/src/main/java/com/opspilot/entity/User.java: FOUND
- backend/src/main/java/com/opspilot/repository/UserRepository.java: FOUND
- backend/src/main/java/com/opspilot/security/UserDetailsServiceImpl.java: FOUND
- backend/src/main/java/com/opspilot/security/JwtService.java: FOUND
- backend/src/main/java/com/opspilot/security/JwtAuthFilter.java: FOUND
- backend/src/main/java/com/opspilot/config/SecurityConfig.java: FOUND
- backend/src/main/java/com/opspilot/dto/request/LoginRequest.java: FOUND
- backend/src/main/java/com/opspilot/dto/response/AuthResponse.java: FOUND
- backend/src/main/java/com/opspilot/dto/response/UserResponse.java: FOUND
- backend/src/main/java/com/opspilot/service/AuthService.java: FOUND
- backend/src/main/java/com/opspilot/controller/AuthController.java: FOUND
- backend/src/main/java/com/opspilot/exception/GlobalExceptionHandler.java: FOUND
- Commit b8aeef7: FOUND
- Commit 2778834: FOUND
- Commit 9e07b32: FOUND
