# Ticket Assignment UI — Design Spec

**Date:** 2026-05-15  
**Scope:** Frontend only — backend endpoint already exists.

---

## Goal

Add assign/unassign controls to the ticket detail page so operators can self-assign and admins can assign to any user.

---

## Rules

| Actor    | Can do |
|----------|--------|
| OPERATOR | Assign ticket to themselves; unassign themselves |
| ADMIN    | Assign to any user; unassign anyone |

---

## UI

Controls appear inside the existing info `mat-card` in `TicketDetailComponent`, below the "Created by / Assigned" line.

### Operator view (non-admin)

- If `assignedToEmail !== currentUser.email`: show **"Assign to me"** button.
- If `assignedToEmail === currentUser.email`: show **"Unassign"** button.

### Admin view

- Dropdown (`mat-select`) pre-populated with all users from `UserService.listUsers()`.  
  Pre-selected to current `assignedToEmail` if set.
- **"Assign"** button — enabled when dropdown has a selection different from current.
- **"Unassign"** button — always visible when ticket has an assignee.

---

## Data flow

1. On `ngOnInit`, if `auth.isAdmin()`, call `userService.listUsers()` and store in `users` signal.
2. `assign(userId: string | null)` calls `ticketService.assign(ticketId, userId)`.  
   On success: `ticket.set(updatedTicket)`. On error: set error message.
3. `assignToMe()` calls `assign(currentUser.id)`.
4. `unassign()` calls `assign(null)`.

---

## Changes required

| File | Change |
|------|--------|
| `ticket-detail.component.ts` | Add `users` signal, `assigneeControl`, `assignToMe()`, `unassign()`, `assign()` method; load users if admin; extend template |
| No other files | `UserService`, `TicketService.assign()`, `User` model all already exist |

---

## Out of scope

- Notifications on assignment
- Filtering the user dropdown by role (OPERATOR only)
- Assignment history in audit trail (backend already logs it)
