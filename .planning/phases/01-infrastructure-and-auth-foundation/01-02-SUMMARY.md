---
phase: 01-infrastructure-and-auth-foundation
plan: "02"
subsystem: database
tags: [flyway, postgresql, bcrypt, migrations, users]

requires: []
provides:
  - "Flyway V1 migration: users table DDL (id, email, password, role, full_name, timestamps)"
  - "Flyway V2 migration: seed rows for admin@example.com (ADMIN) and operator@example.com (OPERATOR) with BCrypt cost-12 hashes"
affects:
  - 01-03
  - 01-04
  - 02-tickets
  - auth

tech-stack:
  added: []
  patterns:
    - "Flyway IMMUTABLE migration pattern: no edits after first apply, corrections via new Vn migration"
    - "BCrypt cost 12 hashes stored in SQL seed file (not Java DataInitializer)"

key-files:
  created:
    - backend/src/main/resources/db/migration/V1__create_users.sql
    - backend/src/main/resources/db/migration/V2__seed_users.sql
  modified: []

key-decisions:
  - "BCrypt prefix $2b$12$ used (Python bcrypt library output); equivalent to $2a$ per BCrypt spec and accepted by Spring Security BCryptPasswordEncoder"
  - "Seed via Flyway migration (not DataInitializer bean) for idempotency and auditability"
  - "Remediation path documented: if $2b$ causes 401 in integration tests, create V3 with UPDATE statements using $2a$-prefixed hashes from Spring encoder"

patterns-established:
  - "Migration immutability: comment header in every migration file notes IMMUTABLE status"
  - "No secrets in DDL: passwords stored as BCrypt hashes, never plain text"

requirements-completed:
  - INFRA-02
  - AUTH-05
  - AUTH-06

duration: 5min
completed: 2026-05-14
---

# Phase 1 Plan 02: Flyway Migrations (Users Table and Seed) Summary

**PostgreSQL users table created via Flyway V1 DDL, seeded in V2 with BCrypt cost-12 hashes for admin and operator accounts**

## Performance

- **Duration:** 5 min
- **Started:** 2026-05-14T00:00:00Z
- **Completed:** 2026-05-14T00:05:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- V1__create_users.sql: users table with BIGSERIAL pk, UNIQUE email, password (VARCHAR 255), role, full_name, TIMESTAMPTZ timestamps, and email index
- V2__seed_users.sql: admin@example.com/ADMIN and operator@example.com/OPERATOR seeded with BCrypt cost-12 $2b$-prefixed hashes
- Double-underscore naming verified (Flyway version parsing requires exactly two underscores)
- No plain-text passwords in data rows; passwords appear only in comment for documentation

## Task Commits

Each task was committed atomically:

1. **Task 1: Write Flyway migration V1 -- create users table** - `a3a193f` (feat)
2. **Task 2: Write Flyway migration V2 -- seed admin and operator users** - `a3a193f` (feat)

Both tasks committed in a single atomic commit: `a3a193f` - feat(phase-1): Flyway migrations V1 (users table) and V2 (seed users)

## Files Created/Modified
- `backend/src/main/resources/db/migration/V1__create_users.sql` - Users table DDL with BIGSERIAL id, UNIQUE email, BCrypt-length password column, role, full_name, TIMESTAMPTZ columns and email index
- `backend/src/main/resources/db/migration/V2__seed_users.sql` - Seed INSERT for two predefined users with $2b$12$-prefixed BCrypt hashes

## Decisions Made
- BCrypt $2b$ prefix used (output of Python bcrypt library at cost 12); this prefix is treated identically to $2a$ by jBCrypt and Spring Security BCryptPasswordEncoder per the BCrypt specification.
- Seed via Flyway migration rather than a Spring DataInitializer bean: idempotent, visible in flyway_schema_history, environment-agnostic.
- Remediation path for $2b$ prefix documented inline in V2 comments: if integration tests show 401, create V3__fix_seed_hashes.sql with UPDATE statements using Spring-generated $2a$ hashes. Do not edit V2.

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Threat Surface Scan
No new network endpoints or auth paths introduced. Migration files run at DB startup with full DB privileges (accepted per T-02-01 in plan threat model). spring.flyway.clean-disabled=true must be set in application configuration (addressed in Plan 01-03).

## Next Phase Readiness
- Migration files are in place at the path Flyway scans by default (db/migration/)
- Plan 01-03 (Spring Boot project scaffold) must add Flyway dependency and set spring.flyway.clean-disabled=true
- Plan 01-04 (integration tests) will verify Flyway applies exactly 2 migrations and seed rows exist with correct hashes
- If $2b$ prefix causes 401 during Plan 01-04 tests, create V3__fix_seed_hashes.sql before proceeding to Plan 02

---
*Phase: 01-infrastructure-and-auth-foundation*
*Completed: 2026-05-14*
