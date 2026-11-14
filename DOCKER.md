# Local Docker demo

Install Docker with a current Compose plugin and start its Linux container engine.
Run these commands from the repository root. Java, Node.js and PostgreSQL are
installed inside the images; they are not required on the host.

```powershell
docker compose --env-file .env.example -f compose.demo.yml up --build -d --wait --wait-timeout 180
```

Open http://127.0.0.1:18080/feedback. The first build downloads dependencies.
Flyway creates the schema and synthetic demo data in the new demo database.
The web server serves the production Angular build and proxies `/api` to Spring Boot.
Direct links and refreshes work on `/feedback`.

The example credentials are for this local demo. To customize them or change a
busy/reserved web port, copy `.env.example` to `.env.demo`, edit it, and replace
`--env-file .env.example` with `--env-file .env.demo` in every command below.
Do not overwrite an existing env file. Compose passes the `POSTGRES_*` values
explicitly to both database and backend; `DEMO_WEB_PORT` controls only the host port.
Changing database credentials after initialization does not update existing database users.

```powershell
# Status and logs (Ctrl+C stops following logs, not the containers)
docker compose --env-file .env.example -f compose.demo.yml ps
docker compose --env-file .env.example -f compose.demo.yml logs --tail 100 -f

# Stop and remove demo containers; keep the database
docker compose --env-file .env.example -f compose.demo.yml down

# Rebuild after source changes and start again
docker compose --env-file .env.example -f compose.demo.yml up --build -d --wait --wait-timeout 180

# Refresh base images as well when updating
docker compose --env-file .env.example -f compose.demo.yml build --pull
docker compose --env-file .env.example -f compose.demo.yml pull postgres
docker compose --env-file .env.example -f compose.demo.yml up -d --wait --wait-timeout 180
```

The project is `coursepulse-demo`; its database volume is
`coursepulse-demo_demo-postgres-data`. Keep the same project name and env file
between runs. Do not add `-v` to `down`: it deletes database data. A volume is
not a backup. The database and backend have no published host ports; the web
port binds only to localhost. Healthchecks gate startup; `unless-stopped` restarts
exited services but does not automatically restart an unhealthy running process.

The existing `docker compose up -d postgres` development workflow is unchanged
and uses a separate database volume. The demo needs no host development servers.

Quick check: `/api/courses` returns JSON. In the UI, CS101 / 2025–2026 has 26
entries: try search, rating, sorting and paging, then select 2026–2027 (3 entries,
average 4.0). Refresh the direct offering URL and check the mobile layout.
API errors such as `/api/course-offerings/999999` remain JSON, not the SPA page.
