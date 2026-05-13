.PHONY: up down backend-run frontend-run test backend-test frontend-test build

up:
	docker compose up postgres -d

down:
	docker compose down

full-up:
	docker compose --profile full up -d

full-down:
	docker compose --profile full down

backend-run:
	cd backend && mvn spring-boot:run

backend-run-fake:
	cd backend && FAKE_AI=true mvn spring-boot:run -Dspring-boot.run.profiles=fake-ai

backend-test:
	cd backend && mvn test

backend-build:
	cd backend && mvn package -DskipTests

frontend-install:
	cd frontend && npm install

frontend-run:
	cd frontend && npm start

frontend-test:
	cd frontend && npm test -- --watch=false

frontend-build:
	cd frontend && npm run build

test: backend-test frontend-test

build: backend-build frontend-build
