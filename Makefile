.PHONY: up down backend frontend test-backend test-frontend build full-stack-up logs clean

BACKEND_DIR := backend
FRONTEND_DIR := frontend
JAVA_HOME ?= /opt/platform/jdk-21.0.7
export JAVA_HOME

## up: start PostgreSQL with docker-compose
up:
	docker compose up -d postgres

## down: stop all docker-compose services
down:
	docker compose down

## backend: compile and run the Spring Boot backend
backend:
	cd $(BACKEND_DIR) && mvn spring-boot:run

## frontend: install deps if needed and start the Angular dev server
frontend:
	cd $(FRONTEND_DIR) && npm install && npm start

## test-backend: run backend tests
test-backend:
	cd $(BACKEND_DIR) && mvn test

## test-frontend: run frontend unit tests (headless)
test-frontend:
	cd $(FRONTEND_DIR) && npm test -- --watch=false --browsers=ChromeHeadless

## build: full build (backend + frontend)
build:
	cd $(BACKEND_DIR) && mvn clean package -DskipTests
	cd $(FRONTEND_DIR) && npm install && npm run build

## full-stack-up: start all services with the full-stack docker-compose profile
full-stack-up:
	docker compose --profile full-stack up -d --build

## logs: tail docker-compose logs
logs:
	docker compose logs -f

## clean: remove build artefacts
clean:
	cd $(BACKEND_DIR) && mvn clean
	rm -rf $(FRONTEND_DIR)/dist $(FRONTEND_DIR)/node_modules
