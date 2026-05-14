# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-14)

**Core value:** Field operators can create tickets, get AI-powered assistance, and track status from creation to close through a secure, auditable interface.
**Current focus:** Phase 1 — Infrastructure and Auth Foundation

## Current Position

Phase: 1 of 9 (Infrastructure and Auth Foundation)
Plan: 0 of ? in current phase
Status: Ready to plan
Last activity: 2026-05-14 — Roadmap created (9 phases, 57 requirements mapped)

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: -
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Init: JWT stored in sessionStorage (not localStorage) — internal tool, reduces XSS surface
- Init: `open-in-view=false` from Phase 2 day one — catch lazy-load bugs early
- Init: JwtAuthFilter instantiated inside SecurityFilterChain (no @Component) — prevent double-registration
- Init: AI_PROVIDER=fake default for all tests — no external dependencies in CI
- Init: Virtual threads enabled (`spring.threads.virtual.enabled=true`) — unblock AI I/O calls

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 3 planning: Validate context window truncation budget against the configured OpenRouter model's actual context limit before implementing the "last N messages" heuristic.
- Phase 3 planning: Decide on XML-like delimiter style for prompt injection protection in ticket content fields.

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| v2 | WebSocket real-time notifications | Deferred | Init |
| v2 | Email notifications | Deferred | Init |
| v2 | File attachments | Deferred | Init |
| v2 | Multi-tenant support | Deferred | Init |

## Session Continuity

Last session: 2026-05-14
Stopped at: Roadmap and STATE.md created; no phases planned or executed yet.
Resume file: None
