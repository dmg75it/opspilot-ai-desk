# API Overview

Base URL: `http://localhost:8080/api`

Interactive docs (Swagger UI): `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

All authenticated endpoints require the header:
```
Authorization: Bearer <jwt-token>
```

---

## Authentication

| Method | Path               | Auth | Description                        |
|--------|--------------------|------|------------------------------------|
| POST   | /auth/login        | No   | Authenticate and receive JWT       |
| GET    | /auth/me           | Yes  | Return current user info           |

### POST /auth/login

Request:
```json
{ "email": "operator@example.com", "password": "operator123" }
```

Response `200`:
```json
{
  "token": "<jwt>",
  "expiresIn": 86400000,
  "user": { "id": 2, "email": "operator@example.com", "role": "OPERATOR" }
}
```

---

## Tickets

| Method | Path                          | Role       | Description                      |
|--------|-------------------------------|------------|----------------------------------|
| GET    | /tickets                      | Any        | List tickets (paginated, filtered)|
| POST   | /tickets                      | Any        | Create ticket                    |
| GET    | /tickets/{id}                 | Any        | Get ticket by id                 |
| PUT    | /tickets/{id}                 | Any/ADMIN  | Update ticket metadata           |
| PATCH  | /tickets/{id}/status          | Any        | Change ticket status             |
| PATCH  | /tickets/{id}/assign          | ADMIN      | Assign ticket to operator        |
| POST   | /tickets/{id}/close           | ADMIN      | Close ticket                     |

### GET /tickets query parameters

| Parameter  | Type    | Description                           |
|------------|---------|---------------------------------------|
| page       | int     | Zero-based page number (default 0)    |
| size       | int     | Page size (default 20, max 100)       |
| status     | string  | Filter by status                      |
| priority   | string  | Filter by priority                    |
| category   | string  | Filter by category                    |
| assignedTo | long    | Filter by assigned operator id        |
| q          | string  | Full-text search on title/description |

### PATCH /tickets/{id}/status

Request:
```json
{ "status": "IN_PROGRESS", "reason": "Taking ownership" }
```

Valid transitions:
```
NEW -> IN_PROGRESS, CLOSED
IN_PROGRESS -> WAITING_FOR_CUSTOMER, RESOLVED, CLOSED
WAITING_FOR_CUSTOMER -> IN_PROGRESS, RESOLVED, CLOSED
RESOLVED -> CLOSED, IN_PROGRESS
CLOSED -> (none, ADMIN only can reopen to IN_PROGRESS)
```

---

## Ticket Notes

| Method | Path                          | Role | Description              |
|--------|-------------------------------|------|--------------------------|
| GET    | /tickets/{id}/notes           | Any  | List notes for a ticket  |
| POST   | /tickets/{id}/notes           | Any  | Add internal note        |

### POST /tickets/{id}/notes

Request:
```json
{ "body": "Customer confirmed pickup address." }
```

Notes created via this endpoint always have visibility `INTERNAL`.
`AI_SUMMARY` and `SYSTEM` notes are created programmatically.

---

## AI Chat

| Method | Path                                      | Role | Description                        |
|--------|-------------------------------------------|------|------------------------------------|
| POST   | /tickets/{id}/ai/session                  | Any  | Start or retrieve chat session     |
| POST   | /tickets/{id}/ai/session/messages         | Any  | Send user message                  |
| GET    | /tickets/{id}/ai/session/messages         | Any  | List messages in session           |
| POST   | /tickets/{id}/ai/summary                  | Any  | Generate AI ticket summary         |
| POST   | /tickets/{id}/ai/suggested-reply          | Any  | Generate suggested customer reply  |
| POST   | /tickets/{id}/ai/apply-summary            | Any  | Save AI summary as ticket note     |

### POST /tickets/{id}/ai/session/messages

Request:
```json
{ "content": "What is the best next action for this ticket?" }
```

Response `200`:
```json
{
  "id": 42,
  "role": "ASSISTANT",
  "content": "Based on the ticket details...",
  "model": "openai/gpt-3.5-turbo",
  "promptTokens": 312,
  "completionTokens": 87,
  "createdAt": "2026-05-14T10:23:00Z"
}
```

---

## Dashboard

| Method | Path            | Role | Description              |
|--------|-----------------|------|--------------------------|
| GET    | /dashboard      | Any  | Aggregate stats           |

Response includes:
- `ticketsByStatus`: map of status -> count
- `ticketsByPriority`: map of priority -> count
- `myOpenTickets`: list of tickets assigned to current user (max 10)
- `recentlyUpdated`: list of recently updated tickets (max 10)
- `aiInteractionsToday`: count of AI messages created today

---

## Admin

| Method | Path            | Role  | Description       |
|--------|-----------------|-------|-------------------|
| GET    | /admin/users    | ADMIN | List all users    |

---

## Error responses

All errors follow the RFC 7807 problem detail format:

```json
{
  "type": "https://opspilot.io/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "title: must not be blank",
  "instance": "/api/tickets"
}
```

Common status codes:
- `400` Bad request / validation error
- `401` Missing or invalid JWT
- `403` Insufficient role
- `404` Resource not found
- `409` Optimistic lock conflict
- `422` Invalid status transition
- `503` AI provider unavailable
