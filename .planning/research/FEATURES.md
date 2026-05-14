# Feature Landscape: OpsPilot AI Desk

**Domain:** Transport/Logistics Field Operations Support Desk with AI Assistance
**Researched:** 2026-05-14
**Overall confidence:** HIGH (primary sources: Freshdesk anatomy docs, Zendesk dev docs, uxpatterns.dev, alhena.ai, eesel.ai, supportbench.com)

---

## Table Stakes

Features operators expect. Missing = product feels incomplete or unprofessional.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Ticket list with sortable columns | Operators scan by status, priority, age | Low | Status, priority, assignee, updated-at columns minimum |
| Ticket detail with full conversation thread | Must see complete history without context loss | Medium | Chronological order, newest-first toggle |
| Properties sidebar on ticket detail | Quick view/edit of status, priority, assignee without leaving page | Low | Right or left panel, always visible |
| Activity/audit timeline on ticket | "Who did what when" — non-negotiable for disputes | Medium | Every status change, note, assignment logged |
| Filter tickets by status/priority/assignee | Operators manage queues by slice | Low | Multi-select filters, persist in URL |
| Pagination or virtual scroll on ticket list | Performance with 100s of tickets | Low | Server-side pagination, page-size selector |
| Internal notes separate from AI content | Operators need private collaboration | Low | Visual distinction from AI content and system events |
| Optimistic locking with clear conflict error | Concurrent edits are common in ops | Medium | "Someone else updated this ticket" — not a silent overwrite |
| Keyboard-accessible forms | Speed-critical environment, mouse is slow | Low | Tab order, enter to submit, escape to cancel |
| Loading states on all async actions | Field ops have unreliable connectivity | Low | Spinner or skeleton on every HTTP call |
| Error messages that explain what to do | Operators are not tech-savvy | Low | "Title is required" not "validation error 422" |
| Role-based UI differences | ADMIN sees things OPERATOR does not | Medium | Hide/disable admin actions from operators |

---

## Ticket Detail Page: Canonical Layout

Research into Freshdesk and Zendesk anatomy reveals a three-zone layout that operators internalize quickly:

```
+-----------------------------------+------------------+
|  TICKET HEADER                    |                  |
|  Title | Status badge | Priority  |  PROPERTIES      |
|  #id | External ref | Category   |  SIDEBAR         |
+-----------------------------------+                  |
|                                   |  Status          |
|  CONVERSATION / NOTES THREAD      |  Priority        |
|  (chronological, unified)         |  Category        |
|                                   |  Assignee        |
|  [SYSTEM] Ticket created          |  Created by      |
|  [OPERATOR] Note text here        |  Created at      |
|  [AI_SUMMARY] AI generated...     |  Updated at      |
|                                   |  Resolved at     |
+-----------------------------------+                  |
|  REPLY / NOTE COMPOSER            |  ACTIVITY LOG    |
|  [Add Note]  [Request Info]       |  (scrollable)    |
+-----------------------------------+------------------+
|  AI ASSISTANT PANEL (collapsible, right-side drawer) |
+------------------------------------------------------+
```

The AI chat panel should be a **collapsible side drawer or bottom panel** inside the ticket detail, not a separate page. The operator never leaves the ticket context.

---

## AI Chat Panel: What Users Actually Need

Based on research into Zendesk/Freshdesk patterns and AI UX design literature:

### Panel Placement
- Right-side collapsible drawer or bottom split pane within the ticket detail view
- Toggled by a persistent "AI Assistant" button in the ticket toolbar
- Panel persists its open/closed state per session (not reset on every ticket navigation)

### Chat Message Display
Each message in the thread needs:
- Role indicator: USER vs ASSISTANT vs SYSTEM (distinct visual style per role)
- Timestamp
- Model name (for ASSISTANT messages — operators need to know what generated a response)
- Token count or "~N tokens" estimate (useful for audit and cost awareness)
- Error state with friendly message + retry button if the call failed
- Streaming indicator (typing dots or progressive text reveal) for in-flight responses

### Action Buttons on AI Responses
Each AI response should have inline action buttons:
- "Copy" — paste into the note composer or external system
- "Apply as Note" — adds the AI output as an AI_SUMMARY note on the ticket (explicit user action, never automatic)
- "Regenerate" — sends the same prompt again

These buttons implement the human-in-the-loop requirement: AI suggestions require explicit user action before they affect the ticket.

### Prompt Quick-Actions (above chat input)
Pre-built action buttons reduce friction and produce consistent outputs:
- "Summarize ticket" — one click, runs summarization prompt
- "Suggest next action" — one click, runs next-action prompt
- "Draft customer reply" — one click, runs reply draft prompt
- "Identify missing info" — one click
- "Classify priority/category" — one click

These replace free-text prompting for 90% of use cases. Free-text input remains for edge cases.

---

## Prompt Templates (Versioned in Code)

These templates are designed for field operations context. They should live as versioned constants in the backend, never hardcoded in the frontend.

### SUMMARIZE_TICKET (v1)
```
You are a support analyst for a transport and logistics operations team.

Given the following ticket, produce a structured summary for an agent picking up this case.

Ticket:
Title: {title}
Description: {description}
Status: {status}
Priority: {priority}
Category: {category}
External Reference: {externalRef}
Notes:
{notes}

Respond with:
1. Core issue (one sentence)
2. What has already been done (bullet list, or "Nothing yet" if empty)
3. Customer/operator sentiment (Frustrated / Neutral / Satisfied)
4. Missing information that would help resolve this (bullet list, or "None identified")
5. Recommended next step (one sentence)
```

### SUGGEST_NEXT_ACTION (v1)
```
You are a tier-2 support analyst for a transport and logistics operations team.

Review this ticket and suggest the single most important next action the assigned operator should take.

Ticket:
Title: {title}
Status: {status}
Priority: {priority}
Category: {category}
Description: {description}
Recent notes:
{recentNotes}

Respond with:
- Next action: (one concrete, actionable step)
- Rationale: (one sentence explaining why)
- Escalate: YES or NO with reason if YES
```

### DRAFT_CUSTOMER_REPLY (v1)
```
You are a customer-facing support agent for a transport and logistics company.

Draft a professional, empathetic reply for the following operational issue.

Ticket:
Title: {title}
Description: {description}
Category: {category}
Status: {status}

Guidelines:
- Acknowledge the issue in the first sentence
- Explain what is being done or has been done
- Set clear expectations for next steps or timeline
- Close with one reassuring sentence
- Tone: professional and direct, not overly formal
- Length: 3-5 sentences maximum
```

### IDENTIFY_MISSING_INFO (v1)
```
You are a support quality analyst for a transport and logistics operations team.

Review this ticket and identify information that is missing but would be necessary to resolve it.

Ticket:
Title: {title}
Description: {description}
Category: {category}
Notes:
{notes}

Respond with a bullet list of specific pieces of missing information.
If all necessary information is present, respond with "No missing information identified."
For each missing item, suggest a specific question the operator could ask.
```

### CLASSIFY_PRIORITY_CATEGORY (v1)
```
You are a ticket triage specialist for a transport and logistics operations team.

Analyze this ticket and classify it.

Ticket:
Title: {title}
Description: {description}
Current priority: {currentPriority}
Current category: {currentCategory}

Valid priorities: LOW, MEDIUM, HIGH, CRITICAL
Valid categories: DELIVERY, PICKUP, DOCUMENTATION, CUSTOMER, SYSTEM, OTHER

Respond with a JSON object only, no explanation:
{
  "suggestedPriority": "...",
  "suggestedCategory": "...",
  "priorityRationale": "one sentence",
  "categoryRationale": "one sentence",
  "confidenceLevel": "HIGH | MEDIUM | LOW"
}
```

All templates must include:
- A version identifier (`v1`, `v2`) so prompt changes are traceable in the audit log
- The prompt version stored alongside each AI message record in the database

---

## Ticket Status Workflow for Field Operations

The spec defines five statuses. The allowed transitions matter as much as the statuses themselves.

```
NEW --> IN_PROGRESS
NEW --> CLOSED (admin only, e.g. duplicate)

IN_PROGRESS --> WAITING_FOR_CUSTOMER
IN_PROGRESS --> RESOLVED
IN_PROGRESS --> CLOSED

WAITING_FOR_CUSTOMER --> IN_PROGRESS
WAITING_FOR_CUSTOMER --> RESOLVED
WAITING_FOR_CUSTOMER --> CLOSED

RESOLVED --> CLOSED
RESOLVED --> IN_PROGRESS (reopening — field ops issues often resurface)

CLOSED --> (no transitions, immutable except by ADMIN)
```

The WAITING_FOR_CUSTOMER status is critical for field ops: drivers or operators in the field often go dark and tickets pile up waiting for callback/confirmation. Without this status, everything gets stuck in IN_PROGRESS and the queue loses meaning.

Transition enforcement must be server-side. The frontend can grey out disallowed transitions but must not rely on it for security.

**Closed ticket rule:** OPERATOR cannot edit a closed ticket's fields. ADMIN can reopen or correct. This must be enforced at the API layer, not just the UI.

---

## Dashboard: What a Field Ops Manager Actually Needs

Based on research into service desk KPIs and logistics operations patterns. The spec requires simple cards and tables — charts are optional. The data model below prioritizes what managers act on daily.

### Top Priority Widgets (always visible, above the fold)

| Widget | Data | Why Managers Need It |
|--------|------|----------------------|
| Open tickets by status | Count per status (NEW, IN_PROGRESS, WAITING) | Immediate queue health |
| My assigned open tickets | Filtered to current user, sorted by priority desc | Personal workload |
| Critical/High tickets older than 24h | Count + link to list | Escalation trigger |
| Tickets created today vs yesterday | Simple delta | Volume trend |

### Secondary Widgets (scrollable or tabbed)

| Widget | Data | Why Managers Need It |
|--------|------|----------------------|
| Tickets by priority (all open) | Count per priority | Resource allocation |
| Recently updated tickets | Last 10, with timestamp and who updated | Activity pulse |
| Tickets by category | Count per category (open) | Pattern detection |
| AI interactions today | Count of AI sessions and messages | Cost/adoption awareness |
| Unassigned tickets | Count + list | Queue gaps |

### What to Defer (not MVP)
- Resolution time trends (requires time-series data)
- First response time (requires SLA config)
- Per-operator performance metrics (political sensitivity, not v1)
- Ticket volume charts over time (nice, but not actionable in v1)

The dashboard must link through to filtered ticket lists. A number on a card is only useful if clicking it shows the underlying tickets.

---

## AI Chat Message Storage Model

Each message record should store:

| Field | Why |
|-------|-----|
| id | Primary key |
| session_id | Groups messages in a conversation per ticket |
| role | SYSTEM / USER / ASSISTANT |
| content | The message text |
| model | The OpenRouter model used (for ASSISTANT messages) |
| prompt_template_id | Which template triggered this (for SYSTEM/prompted messages) |
| prompt_template_version | Version of the template at time of generation |
| input_tokens | From provider response headers if available |
| output_tokens | From provider response headers if available |
| created_at | Timestamp |
| error_flag | Boolean |
| error_message | Provider error detail (never the API key) |

One session per ticket (not one session per operator visit). Sessions accumulate messages. The frontend reconstructs the conversation by fetching all messages for the session.

Conversation history sent to the LLM should include:
- System prompt (first message, role SYSTEM)
- All prior USER and ASSISTANT messages in the session
- The new USER message

Token budget management: for sessions with many messages, truncate oldest USER/ASSISTANT pairs before hitting the context limit. Never truncate the system prompt.

---

## Differentiators

Features that add value beyond the minimum, in rough priority order.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| One-click "Apply as Note" on AI response | Human-in-the-loop with minimal friction | Low | Eliminates copy-paste step |
| Prompt quick-action buttons | Reduces AI learning curve to zero for operators | Low | Run standard prompts in one click |
| Classification suggestion with diff | Show current vs AI-suggested priority/category, accept with one click | Medium | Use JSON response from classify prompt |
| Conversation context continuity | AI has context of whole session, not just last message | Medium | Include prior messages in each API call |
| Ticket audit trail with actor + timestamp | Every change attributable | Medium | Required for logistics compliance |
| External reference (tracking number etc.) | Links ticket to real-world logistics IDs | Low | Already in spec, but searchable by this field |
| Unassigned ticket alert on dashboard | Highlights queue gaps before they become SLA failures | Low | Simple count widget |

---

## Anti-Features

Features to explicitly NOT build in v1.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| AI auto-updating ticket status | Violates the project's own safety constraint; erodes operator trust | Always require explicit user action |
| Sending AI output directly to customers | No review step, legal/ops risk | Operators copy-paste or adapt the draft |
| Ticket merge/split | Complex UI, data model complexity, low v1 value | Defer; add via linked tickets later |
| SLA timers and breach alerts | Requires SLA config model, time zone logic | Defer; mention in docs as known gap |
| File attachments | Complex storage, out of scope in PROJECT.md | Document the limitation |
| Real-time websocket updates | Out of scope per PROJECT.md | Polling on ticket detail page is acceptable for v1 |
| Knowledge base / FAQ | Separate product surface | Defer entirely |
| Customer portal (non-operator facing) | Separate auth model, out of scope | Document |
| Bulk ticket operations | Complex selection UI, low priority | Defer |
| AI auto-assignment of tickets | Automation rules require config model not in spec | Defer |

---

## Feature Dependencies

```
Login + JWT --> All other features (auth required everywhere)
Ticket Create --> Ticket Detail --> Notes --> AI Chat
Ticket List --> Dashboard (reuses same filter/query logic)
AI Chat Session --> AI Prompt Templates (templates define what the chat can do)
Apply AI as Note --> Notes (AI_SUMMARY visibility type)
Activity Log --> All ticket mutations (mutations emit audit events)
Dashboard widgets --> Ticket query layer (aggregate queries)
Admin user list --> Role-based auth (ADMIN role required)
```

---

## MVP Recommendation

### Must ship (product is broken without these)
1. Authentication (login, JWT, route guards, role enforcement)
2. Ticket CRUD with status transitions and validation
3. Internal notes (OPERATOR-created INTERNAL notes)
4. AI chat panel with the five quick-action prompts
5. Apply AI as Note (AI_SUMMARY visibility)
6. Dashboard with top-priority widgets
7. Activity audit trail on tickets

### Ship but can be rough
8. Admin user list page (read-only is enough)
9. Ticket filter by status/priority/assignee
10. Classification suggestion with diff display

### Defer to post-MVP
- SLA timers
- Ticket search by external reference (API filter can do it; no dedicated UI needed)
- Per-operator performance metrics on dashboard
- Token usage totals on dashboard (add once cost data is available)

---

## Sources

- Freshdesk ticket anatomy: https://support.freshdesk.com/support/solutions/articles/37588-anatomy-of-a-ticket
- Zendesk agent interface: https://support.zendesk.com/hc/en-us/articles/4408883355546
- AI chat UX patterns: https://uxpatterns.dev/patterns/ai-intelligence/ai-chat
- Hybrid AI support UX: https://alhena.ai/blog/designing-trust-hybrid-ai-human-support/
- AI prompt templates for support: https://www.eesel.ai/blog/prompt-templates-for-support-triage-and-summaries
- AI prompts copy-paste: https://www.supportbench.com/ai-prompts-customer-support-copy-paste-faster-replies/
- Ticket workflow best practices: https://unito.io/blog/build-support-ticket-workflow/
- Service desk KPIs: https://www.smcconsulting.be/service-desk-kpis-complete-guide/
- Ticketing system mistakes: https://blog.happyfox.com/ticketing-system-mistakes/
- Support ticket UX best practices: https://www.coveo.com/blog/support-ticket-ui-best-practices/
- Human-in-the-loop UX: https://www.sethserver.com/ai/human-in-the-loop-not-human-as-rubber-stamp.html
- Help desk workflow: https://front.com/blog/help-desk-workflows
