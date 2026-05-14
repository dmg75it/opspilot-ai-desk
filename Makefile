.PHONY: up down backend frontend test

up:
	docker compose up -d

down:
	docker compose down

backend:
	cd backend && ./mvnw spring-boot:run

frontend:
	cd frontend && npm start

test:
	cd backend && ./mvnw test
