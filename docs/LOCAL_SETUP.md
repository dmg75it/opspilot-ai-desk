# Local Setup Guide

## Prerequisites

| Tool           | Minimum version | Install                                        |
|----------------|-----------------|------------------------------------------------|
| Java           | 21              | [sdkman.io](https://sdkman.io) or package manager |
| Maven          | 3.9+            | `sdk install maven` or package manager        |
| Node.js        | 20 LTS          | [nvm](https://github.com/nvm-sh/nvm)          |
| npm            | 10+             | Bundled with Node                             |
| Docker         | 24+             | [docs.docker.com](https://docs.docker.com/get-docker/) |
| Docker Compose | 2.x (plugin)    | Bundled with Docker Desktop                   |

---

## Step 1 — Clone and configure environment

```bash
git clone https://github.com/your-org/opspilot-ai-desk.git
cd opspilot-ai-desk
cp .env.example .env
```

Edit `.env` to set your values. Minimum required change for local dev:
- `JWT_SECRET` — set any long random string
- `OPENROUTER_API_KEY` — only needed if `AI_PROVIDER=openrouter`
- Leave `AI_PROVIDER=fake` to run without an API key

---

## Step 2 — Start PostgreSQL

```bash
make up
```

This starts a PostgreSQL 16 container on port 5432.
Verify it is ready:

```bash
docker compose ps
# postgres should show "healthy"
```

---

## Step 3 — Start the backend

The backend reads environment variables from `.env` automatically when using Spring Boot DevTools,
or you can export them manually:

```bash
export $(grep -v '^#' .env | xargs)
make backend
```

The backend will:
1. Run Flyway migrations (creates schema and seeds two users).
2. Start on `http://localhost:8080`.

Verify:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Step 4 — Start the frontend

```bash
make frontend
```

The Angular dev server starts on `http://localhost:4200`.
It proxies `/api` requests to `http://localhost:8080/api`.

---

## Step 5 — Log in

Open `http://localhost:4200` in your browser.

Seed credentials:

| Email                   | Password    | Role     |
|-------------------------|-------------|----------|
| admin@example.com       | admin123    | ADMIN    |
| operator@example.com    | operator123 | OPERATOR |

---

## Step 6 — Happy path walkthrough

1. Log in as `operator@example.com`.
2. Go to **Tickets** and click **New Ticket**.
3. Fill in title, description, priority, and category. Submit.
4. Open the ticket detail page.
5. Click **AI Assistant** to open the chat panel.
6. Type "Summarize this ticket". The fake AI responds with a canned summary.
7. Click **Apply as Note** to save the summary as a ticket note.
8. Change the ticket status to `IN_PROGRESS`.
9. Log out and log in as `admin@example.com`.
10. Assign the ticket to the operator. Close it.

---

## Running with real OpenRouter

1. Set `AI_PROVIDER=openrouter` in `.env`.
2. Set `OPENROUTER_API_KEY=<your key>`.
3. Optionally change `OPENROUTER_MODEL` to a model you have access to.
4. Restart the backend.

The AI chat panel will now call OpenRouter for real responses.

---

## Running the full stack with Docker Compose

To run backend, frontend, and database all in containers:

```bash
make full-stack-up
```

This builds Docker images for backend and frontend and starts all services.
Open `http://localhost:4200`.

Requires Dockerfiles in `backend/` and `frontend/`.

---

## Useful commands

```bash
make logs           # tail all docker-compose logs
make down           # stop all containers
make test-backend   # run backend tests (requires Docker for Testcontainers)
make test-frontend  # run frontend tests (headless Chrome)
make clean          # remove build artefacts
```

---

## Troubleshooting

**Port 5432 already in use**
Another PostgreSQL instance is running locally. Either stop it or change `DB_PORT` in `.env`.

**Backend fails to start: "Unable to acquire JDBC Connection"**
PostgreSQL container is not ready yet. Wait a few seconds and retry, or run `make down && make up`.

**Frontend shows "401 Unauthorized" on all API calls**
Your JWT has expired (default 24 h). Log out and log in again.

**AI chat returns "AI service unavailable"**
If `AI_PROVIDER=openrouter`, check that `OPENROUTER_API_KEY` is set and valid.
Switch to `AI_PROVIDER=fake` for offline development.
