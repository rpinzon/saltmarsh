# Saltmarsh — Marina Operations Platform

A full-stack Java web application for running a small marina / harbor: berth inventory, vessel registration, reservation lifecycle (including waitlist offers), maintenance work orders, and invoicing.

Built to be **runnable end-to-end** with a single command on a developer machine (embedded H2) or via Docker Compose with PostgreSQL.

## What it does

| Area | Capabilities |
|------|----------------|
| **Berths** | Inventory with capacity (length/draft), type, daily rate, availability search |
| **Vessels** | Owner registration with dimensions used for fit checks |
| **Reservations** | State machine: `PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT` (also `CANCELLED` / `NO_SHOW`) |
| **Waitlist** | FIFO queue; cancellation/no-show can offer a freed berth for 24 hours |
| **Work orders** | Priority queue with assign / start / block / complete; billable on complete |
| **Invoices** | Auto-generated on checkout, late cancel fees, and completed work; pay / void |
| **Security** | Form login, BCrypt, role-based access (`BOATER`, `STAFF`, `HARBORMASTER`, `ADMIN`), CSRF, security headers |
| **Audit** | Append-only action log for operational events |

### Business rules worth noting

- A vessel may only occupy a berth if **length and draft fit**.
- Overlapping active reservations on the same berth are **rejected** (berth row is locked for the booking transaction; active waitlist offers also hold inventory).
- Boaters create **PENDING** bookings; staff create **CONFIRMED** bookings.
- Cancel after confirmation within the free-cancel window (default **48h**) incurs a **25%** late fee invoice.
- Checkout and billable work-order completion **issue invoices** (8% tax). Checkout bills the **full reserved stay** (non-refundable unused nights on early departure) and promotes the waitlist for the freed dates.
- Waitlist promotion is **FIFO** among fitting vessels when a berth frees; an **OFFERED** berth is held for **24h**. Expired offers return to **WAITING**.
- Invoice numbers are allocated from a **database counter** (safe across restarts).
- Demo privileged accounts are seeded only under the **`local`** profile.

## Requirements

- **Java 21+**
- **Maven Wrapper** included (`./mvnw`); a system Maven 3.9+ install also works
- Optional: **Docker** / Docker Compose for PostgreSQL deployment

This repo was verified with Temurin/Zulu **Java 21** and Maven **3.9**.

## Quick start (local, zero external services)

```bash
# Use Java 21 if your default is older (example with sdkman):
# export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.11-zulu"
# export PATH="$JAVA_HOME/bin:$PATH"

./mvnw spring-boot:run
```

Open **http://localhost:8080**

Data is stored in a local file database under `./data/saltmarsh` (created on first run). Demo users are seeded automatically when the database is empty **on the `local` profile only** (the default).

### Demo logins (`local` profile only)

Password for **all** demo accounts: `password`

These accounts are **not** created under the `postgres` profile (Compose). Provision real users before using non-local deployments.

| Email | Role | Good for |
|-------|------|----------|
| `harbor@saltmarsh.harbor` | HARBORMASTER | Full ops board, confirm/check-in/out, berth admin |
| `staff@saltmarsh.harbor` | STAFF | Work orders, check-in/out, invoices |
| `alex@example.com` | BOATER | Own vessels, book berths, waitlist, request work |
| `sam@example.com` | BOATER | Waitlist demo data |
| `riley@example.com` | BOATER | Pending reservation demo |
| `admin@saltmarsh.harbor` | ADMIN | Same elevated access as harbormaster |

### Suggested walkthrough

1. Sign in as **harbor@saltmarsh.harbor**.
2. Open **Dashboard** — docked vessels, open work, audit trail.
3. **Berths → Find availability** — search dates + dimensions.
4. **Reservations** — confirm Riley’s pending stay, or check out the docked vessel (generates an invoice).
5. Cancel a confirmed reservation close to start date as a boater to see late fees (or adjust dates in seed data).
6. **Work Orders** — assign, start, complete with labor/parts → invoice.
7. **Waitlist** — cancel a stay that overlaps a waitlist entry and accept the offer as the boater.

Sign out and sign in as `alex@example.com` to see the boater-scoped views.

## API health check

```bash
curl -s http://localhost:8080/api/health
```

## Tests

```bash
./mvnw test
```

Includes domain unit tests (state machines, pricing, fit), service tests with Mockito, and Spring Boot integration tests (security, CSRF, reservation HTTP flow).

## PostgreSQL via Docker Compose

```bash
docker compose up --build
```

App: **http://localhost:8080**  
DB: `localhost:5432`, database/user/password `saltmarsh` / `saltmarsh` / `saltmarsh`

Profile `postgres` is activated automatically by the compose file. **No demo users are seeded** in this profile — create accounts via a controlled bootstrap process or SQL before signing in. Session cookies use the `Secure` flag under `postgres` (expect TLS termination).

### Run app locally against Compose DB only

```bash
docker compose up -d db
export SPRING_PROFILES_ACTIVE=postgres
export DB_HOST=localhost DB_PORT=5432 DB_NAME=saltmarsh DB_USER=saltmarsh DB_PASSWORD=saltmarsh
./mvnw spring-boot:run
```

## Configuration

| Property / env | Default (local) | Notes |
|----------------|-----------------|-------|
| `spring.profiles.active` | `local` | `local` = H2 file; `postgres` = PostgreSQL; `test` = H2 mem |
| `saltmarsh.cancellation.free-cancel-hours` | `48` | Hours before start for free cancel |
| `saltmarsh.cancellation.late-fee-percent` | `25` | Percent of stay total |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | — | Used by `postgres` profile |

Schema is managed by **Flyway** (`src/main/resources/db/migration`). Hibernate `ddl-auto` is `validate`.

## Project layout

```
src/main/java/com/saltmarsh/
  config/          # properties, clock, seed data
  domain/          # entities + enums (state machines)
  repository/      # Spring Data JPA
  service/         # business rules
  security/        # UserDetails, SecurityFilterChain
  web/             # Thymeleaf controllers
  dto/             # validated request records
  exception/       # domain exceptions
src/main/resources/
  templates/       # server-rendered UI
  static/css/      # app stylesheet
  db/migration/    # Flyway SQL
```

## Architecture notes

- **Modular monolith**: clear domain services, no anemic “pass-through” controllers.
- **Optimistic locking** on reservations, work orders, and invoices (`@Version`).
- **Open-in-view disabled**; services use explicit fetch joins where needed.
- **Role checks** both at HTTP layer (`hasAnyRole`) and in services (ownership / staff gates).
- **CSRF** enabled for all form posts; security headers for XSS/content-type/referrer/permissions policy.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` and packages the jar on push/PR.

## License

Demo / portfolio project — use freely.
