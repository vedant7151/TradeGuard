# TradeGuard

**An automated testing suite for a financial Trade Management REST API — unit, functional, system, integration, and performance testing, with a BDD layer and a Selenium UI suite, all orchestrated through a GitHub Actions CI/CD pipeline.**

TradeGuard demonstrates every testing discipline a QA/automation engineer at a financial firm is expected to own: functional, system, integration, and performance testing, written in Java, with Cucumber + Gherkin BDD scenarios readable by non-technical stakeholders and a Selenium-driven UI suite.

> This repo ships as **two things in one place**:
> 1. **`sut-api`** — a realistic Trade Management REST API (the "system under test").
> 2. **`test-suite` / `performance`** — the actual product: a layered, automated test suite that exercises it at every level, wired into CI on every push.

---



## Why This Project Exists

Financial software processes trades worth real money — a bug in validation, settlement, or a status transition is not a cosmetic issue, it's a financial loss. At the same time, teams need to ship daily, which manual QA alone cannot support.

TradeGuard shows how a layered automated test suite gives a team the confidence to ship quickly, at every layer of the stack:

| Testing type | Tool | What it proves |
|---|---|---|
| Unit | JUnit 5 + Mockito | Business rules (validation, settlement math) are correct in isolation |
| Integration | Spring Boot Test + Testcontainers | The full stack works against a **real PostgreSQL** database |
| BDD / Acceptance | Cucumber + Gherkin + REST Assured | Scenarios a business analyst can read and approve actually pass |
| UI | Selenium WebDriver (headless Chrome) | The dashboard a trader would use behaves correctly end-to-end |
| Performance | Gatling | The API holds up under concurrent load with acceptable latency |

All tools used are free and open-source. No SaaS testing platforms, no paid grid.

---

## Architecture

```
GitHub Actions CI Pipeline
│
├── Unit Tests (JUnit + Mockito)
│     └── TradeValidationService, SettlementService (mocked repositories)
│
├── Integration Tests (Spring Boot Test + Testcontainers)
│     └── Real PostgreSQL container ← TradeController → TradeService → TradeRepository
│
├── BDD Tests (Cucumber + REST Assured)
│     └── Feature files (Gherkin) → Step Definitions → HTTP calls to a live SUT instance
│
├── Selenium UI Tests
│     └── Headless Chrome → React Dashboard → calls SUT API
│
└── Performance Tests (Gatling)
      └── 50 virtual users → SUT API → assert p95 < 500ms, error rate < 1%
```

---

## Repository Layout

```
TradeGuard/
├── sut-api/                  Spring Boot REST API under test
│   ├── src/main/java/com/tradeguard/sutapi/
│   │   ├── entity/            Trade, Portfolio, Position (JPA entities)
│   │   ├── enums/              TradeType, TradeStatus
│   │   ├── repository/         Spring Data JPA repositories
│   │   ├── service/             TradeValidationService, SettlementService, TradeService
│   │   ├── controller/          TradeController, PortfolioController
│   │   ├── dto/                  Request/response DTOs
│   │   └── exception/            Domain exceptions + @RestControllerAdvice handler
│   ├── src/main/resources/       application.yml, application-test.yml, data.sql
│   └── Dockerfile
│
├── test-suite/                Java test modules (unit, integration, BDD, Selenium)
│   └── src/test/java/com/tradeguard/testsuite/
│       ├── unit/                 JUnit 5 + Mockito tests
│       ├── integration/           Testcontainers-backed Spring Boot tests
│       ├── bdd/                    Cucumber step definitions + runner
│       └── selenium/                Headless-Chrome UI tests
│   └── src/test/resources/features/  Gherkin .feature files
│
├── performance/                Gatling load test module (Scala DSL)
│   └── src/test/scala/simulations/TradeSubmissionSimulation.scala
│
├── frontend-dashboard/         React + Vite trading dashboard (target for Selenium)
│   └── src/                      App.jsx, TradeTable.jsx, Toast.jsx, api.js
│
├── .github/workflows/ci.yml    GitHub Actions pipeline (build → all test types → reports)
├── docker-compose.yml          Local orchestration: Postgres + API + dashboard
└── pom.xml                     Maven aggregator for sut-api + test-suite
```

---

## The Trade Lifecycle

```
DRAFT → VALIDATED → MATCHED → SETTLED
              ↓
           REJECTED (validation failure)

DRAFT → CANCELLED (only a DRAFT trade may be cancelled)
```

**Validation rules** (`TradeValidationService`):
- Quantity must be > 0.
- Price must be > 0.
- **BUY**: portfolio cash balance must cover `quantity × price`.
- **SELL**: portfolio must already hold enough units of the ticker.
- A trade cannot be settled unless it is in `MATCHED` status.
- A trade cannot be cancelled once it is `MATCHED` or `SETTLED`.

**Settlement effects** (`SettlementService`), applied only to a `MATCHED` trade:
- **BUY**: cash balance decreases by notional, position quantity increases.
- **SELL**: cash balance increases by notional, position quantity decreases.

State-machine guards enforced in `TradeService`:
- Only a `DRAFT` trade can be validated.
- Only a `VALIDATED` trade can be matched.
- Only a `MATCHED` trade can be settled.
- Only a `DRAFT` trade can be cancelled.

---

## API Reference

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/trades` | Create a new trade (starts in `DRAFT`) |
| `GET` | `/api/trades/{id}` | Get a trade by id |
| `GET` | `/api/trades?status=` | List trades, optionally filtered by status |
| `POST` | `/api/trades/{id}/validate` | Run validation rules → `VALIDATED` or `REJECTED` |
| `POST` | `/api/trades/{id}/match` | Mark a `VALIDATED` trade as `MATCHED` |
| `POST` | `/api/trades/{id}/settle` | Settle a `MATCHED` trade, updating position + cash |
| `DELETE` | `/api/trades/{id}` | Cancel a `DRAFT` trade |
| `GET` | `/api/portfolios/{id}/positions` | View resulting positions for a portfolio |

Example request:

```bash
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
        "portfolioId": 1,
        "ticker": "AAPL",
        "tradeType": "BUY",
        "quantity": 10,
        "price": 150.00
      }'
```

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| API framework | Spring Boot 3 (Web, Data JPA, Validation) |
| Database | PostgreSQL 16 (H2 for fast local/CI test profiles) |
| Unit testing | JUnit 5 + Mockito |
| Coverage | JaCoCo (≥ 80% line coverage on the service layer, enforced in CI) |
| Integration testing | Spring Boot Test + MockMvc + Testcontainers |
| BDD | Cucumber 7 + Gherkin + REST Assured |
| UI testing | Selenium WebDriver 4 + headless Chrome |
| Performance | Gatling 3 (Scala DSL) |
| Frontend (SUT UI) | React 18 + Vite |
| Build tool | Maven (multi-module + profiles per test type) |
| CI/CD | GitHub Actions |
| Containerization | Docker + docker-compose |



---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+ (for the dashboard)
- Docker (for PostgreSQL locally and for Testcontainers-based integration tests)

### 1. Run everything with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL on `5432`, the SUT API on `8080`, and the dashboard on `5173`.

### 2. Or run components individually

```bash
# Start PostgreSQL only
docker compose up postgres -d

# Run the API (uses the default profile, connects to Postgres)
cd sut-api
mvn spring-boot:run

# In another terminal, run the dashboard
cd frontend-dashboard
npm install
npm run dev
```

The dashboard expects the API at `http://localhost:8080/api` (configurable via `VITE_API_BASE_URL`).

### 3. Environment variables / secrets

This project is fully self-contained (Postgres + Spring Boot + React) and needs no external API keys to run locally or in CI. If you extend it with a real market-data feed, deployment target, or notification integration, add the corresponding keys as GitHub Actions secrets and document them here.

---

## Running Each Test Suite

All commands are run from the repository root unless noted.

```bash
# Build the API first so the test-suite module can depend on it
mvn -f sut-api/pom.xml install -DskipTests

# 1. Unit tests (JUnit + Mockito), + JaCoCo coverage report
mvn -f test-suite/pom.xml test
# report: test-suite/target/site/jacoco/index.html

# 2. Integration tests (Testcontainers spins up real PostgreSQL — Docker must be running)
mvn -f test-suite/pom.xml verify -P integration

# 3. BDD tests (Cucumber + REST Assured against a live SUT instance)
mvn -f test-suite/pom.xml verify -P bdd
# report: test-suite/target/cucumber-reports/cucumber.html

# 4. Selenium UI tests (requires the SUT API AND the dashboard running - see below)
java -jar sut-api/target/sut-api.jar --spring.profiles.active=test &
(cd frontend-dashboard && npm install && npm run dev -- --host) &
mvn -f test-suite/pom.xml verify -P selenium

# 5. Performance tests (Gatling; requires the SUT API running)
java -jar sut-api/target/sut-api.jar --spring.profiles.active=test &
mvn -f performance/pom.xml gatling:test -Dgatling.baseUrl=http://localhost:8080
# report: performance/target/gatling/<simulation-run>/index.html
```


---

## CI/CD Pipeline

Every push and every PR to `main` triggers `.github/workflows/ci.yml`, which runs these jobs in parallel after a compile step:

1. **build** — compiles `sut-api` and `test-suite`.
2. **unit-tests** — `mvn test`, publishes the JaCoCo HTML report as an artifact.
3. **integration-tests** — `mvn verify -P integration`, using Testcontainers-managed PostgreSQL.
4. **bdd-tests** — `mvn verify -P bdd`, publishes the Cucumber HTML report.
5. **selenium-tests** — boots the API + dashboard, runs headless Chrome UI tests.
6. **performance-tests** — boots the API, runs the Gatling simulation, publishes the HTML report and enforces the SLA assertions (fails the build if they're violated).
7. **publish-reports** — aggregates and publishes every suite's report as a downloadable artifact on the run.

---

## Test Reports

| Report | Produced by | Location |
|---|---|---|
| JaCoCo coverage | `mvn test` | `test-suite/target/site/jacoco/index.html` |
| Cucumber BDD report | `mvn verify -P bdd` | `test-suite/target/cucumber-reports/cucumber.html` |
| Surefire/Failsafe | any `mvn test`/`verify` | `test-suite/target/surefire-reports/`, `.../failsafe-reports/` |
| Gatling performance | `mvn gatling:test` | `performance/target/gatling/<run>/index.html` |

Every CI run uploads all of these as downloadable GitHub Actions artifacts.

---

## Feature Highlights

- **Trade Management API** — full CRUD + lifecycle transitions (create, validate, match, settle, cancel, list, view positions), backed by PostgreSQL.
- **Unit test suite** — JUnit 5 + Mockito coverage of `TradeValidationService` and `SettlementService`, including edge cases: zero quantity, negative price, insufficient cash, insufficient position, and invalid status transitions, with JaCoCo enforcing ≥ 80% line coverage on the service layer.
- **Integration test suite** — Spring Boot Test + MockMvc driving requests through the full controller → service → repository → database stack, against a real PostgreSQL instance spun up per run via Testcontainers, with `@Transactional` rollback keeping tests independent.
- **BDD acceptance suite** — Gherkin feature files covering validation and the full trade lifecycle, written in plain English for a business analyst to read, backed by REST Assured step definitions and a generated Cucumber HTML report.
- **Selenium UI suite** — a React trading dashboard with a live trade table and per-row action buttons, exercised end-to-end with headless Chrome: page load, status updates after actions, and error-toast behavior on disallowed actions.
- **Performance suite** — a Gatling simulation modeling 50 concurrent users each submitting 10 trades, asserting p95 response time under 500ms and error rate under 1%, with the HTML report published as a CI artifact.
- **CI/CD pipeline** — every test type runs as its own GitHub Actions job on every push and PR, each publishing its own report artifact, all on the free tier.

