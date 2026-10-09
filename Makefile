SHELL := /bin/bash
COMPOSE ?= docker compose
MVN ?= ./mvnw -B

.PHONY: help keys up down logs build test package clean diagram

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

# Sources are HTML and Mermaid; a PNG is a build artifact.
diagram:
	@if command -v chromium >/dev/null 2>&1; then B=chromium; elif command -v google-chrome >/dev/null 2>&1; then B=google-chrome; else echo "no chromium on PATH: open docs/diagrams/*.html in a browser"; exit 0; fi; \
	for f in docs/diagrams/*.html; do $$B --headless --screenshot="$${f%.html}.png" --window-size=1200,1000 "$$f" && echo "wrote $${f%.html}.png"; done
