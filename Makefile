SHELL := /bin/bash
COMPOSE ?= docker compose
MVN ?= ./mvnw -B

.PHONY: help keys up down logs build test package clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-8s\033[0m %s\n", $$1, $$2}'

keys: ## Generate the development RSA key pair the JWT layer needs
	./scripts/gen-dev-keys.sh certs

up: ## Start postgres + redis + the api (generates dev keys first)
	@$(MAKE) --no-print-directory keys
	$(COMPOSE) up -d --build
	@echo "api     : http://localhost:$${AUTH_PORT:-8081}/auth"
	@echo "swagger : http://localhost:$${AUTH_PORT:-8081}/swagger-ui.html"

down: ## Stop the stack
	$(COMPOSE) down

logs: ## Tail the api log
	$(COMPOSE) logs -f auth-api

build: ## Build the jar
	$(MVN) -DskipTests package

test: ## Tests
	$(MVN) test

clean: ## Remove build output and volumes
	$(MVN) clean
	$(COMPOSE) down -v
