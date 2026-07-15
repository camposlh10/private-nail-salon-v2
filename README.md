# Private Nail Salon

Single-tenant salon: public booking site + private owner CRM.

| Part | Tech | Port | Path |
|---|---|---|---|
| Backend API | Spring Boot 4.1 / Java 21 / PostgreSQL + Flyway | 8092 | `backend/` |
| Booking site (public) | Next.js 15 | 3000 | `frontend/booking/` |
| Owner CRM | Next.js 15 | 3001 | `frontend/crm/` |

## Run everything locally

```bash
# 1. Backend (pick one):
cd backend
docker compose up -d && ./mvnw spring-boot:run           # real PostgreSQL (port 5433)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,h2  # no Docker: embedded H2

# 2. Booking site
cd frontend/booking && npm install && npm run dev         # http://localhost:3000

# 3. Owner CRM
cd frontend/crm && npm install && npm run dev             # http://localhost:3001
```

The dev profile seeds a demo catalog and an owner login: `owner@nailsalon.local` /
`owner-dev-password`. Both Next.js apps proxy `/api/*` to the backend (override with
`BACKEND_URL`), so no CORS or cookie configuration is needed in the browser.

## Tests

```bash
cd backend && ./mvnw verify   # H2-backed suite + Testcontainers PostgreSQL (CI/Docker)
```

CI (`.github/workflows/backend-ci.yml`) runs the full suite on every push/PR touching `backend/`.

## API contract

`backend/src/main/resources/openapi/openapi.yaml` — the frontends' typed clients
(`frontend/*/lib/api.ts`) are hand-synced with it.
