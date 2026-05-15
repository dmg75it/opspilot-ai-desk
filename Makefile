.PHONY: help up down db backend frontend test clean

help:
	@echo "OpsPilot AI Desk"
	@echo ""
	@echo "  make up         Start PostgreSQL"
	@echo "  make down       Stop all containers"
	@echo "  make db         Start only PostgreSQL"
	@echo "  make backend    Run backend (requires DB)"
	@echo "  make frontend   Run frontend dev server"
	@echo "  make test-be    Run backend tests"
	@echo "  make test-fe    Run frontend tests"
	@echo "  make stack      Start full stack with Docker"
	@echo "  make clean      Remove volumes and containers"

up: db

db:
	docker compose up postgres -d

down:
	docker compose down

stack:
	docker compose --profile fullstack up --build -d

backend:
	cd backend && ./mvnw spring-boot:run

frontend:
	cd frontend && npm start

test-be:
	cd backend && ./mvnw test

test-fe:
	cd frontend && npm test -- --watch=false --browsers=ChromeHeadless

clean:
	docker compose down -v
	cd frontend && rm -rf node_modules dist .angular
