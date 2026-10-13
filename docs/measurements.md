# Measurements

## Environment

| | |
|---|---|
| Machine | 12-core x86-64 (AMD64), Ubuntu 24.04, Docker 29.1.3 |
| Toolchain | Go 1.27.2 / Node 24.19.0 / JDK 17 in `maven:3.9-eclipse-temurin-17` |
| Date | 2026-10-13 (UTC) |


## 27 tests, 71.2% of lines

```
$ docker run --rm -v "$PWD":/src -w /src maven:3.9-eclipse-temurin-17 ./mvnw -B test
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
lines 71.2% (173/243), branches 50.0%
```

**Result.** Exact: 173 of 243 lines. The suite runs with no Quarkus context, which is why it is fast
and why it covers the service layer and the token path rather than the HTTP layer.

## The stack runs, and reaches its database

```
$ make keys && docker compose up -d --build --wait
$ docker compose ps
quarkus-auth-api        Up ...  127.0.0.1:18120->8080/tcp
quarkus-auth-postgres   Up ... (healthy)  127.0.0.1:15440->5432/tcp
quarkus-auth-redis      Up ... (healthy)  127.0.0.1:16381->6379/tcp

$ curl -s -o /dev/null -w '%{http_code}\n' localhost:18120/q/openapi
200
$ docker exec quarkus-auth-postgres psql -U postgres -d auth -c '\dt'
 public | users | table | postgres
```

**Result.** The interesting line is the last one: Hibernate created the `users` table in a real
Postgres, which means the datasource and the cache were not merely configured, they were reachable.
`/q/health` answers 404 here and that is correct - this repository has no health extension.

**This is the file the Quarkus project template had git-ignored.** A clone used to contain neither
`application.properties` nor `docker-compose.yml`, so the container started, found no datasource, and
exited in a loop. Both are committed now, and this is the run that says so.
