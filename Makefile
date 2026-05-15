JAVA_HOME=/opt/platform/jdk-21.0.7
export JAVA_HOME

.PHONY: db-up db-down backend-run backend-test frontend-install frontend-run frontend-test frontend-build stack-up stack-down

db-up:
	docker compose up -d postgres

db-down:
	docker compose down

backend-run:
	JAVA_HOME=$(JAVA_HOME) mvn -f backend/pom.xml spring-boot:run

backend-test:
	JAVA_HOME=$(JAVA_HOME) mvn -f backend/pom.xml test

frontend-install:
	cd frontend && npm install

frontend-run:
	cd frontend && npm start

frontend-test:
	cd frontend && CHROME_BIN=/usr/bin/chromium npm test -- --watch=false --browsers=ChromeHeadless

frontend-build:
	cd frontend && npm run build -- --configuration=production

stack-up:
	docker compose --profile fullstack up --build

stack-down:
	docker compose --profile fullstack down
