# DocVault — Setup & Run

A Spring Boot 3.2 document management system with JWT auth, role-based access (ADMIN / HR / ACCOUNT / EMPLOYEE), document versioning, sharing, approvals, audit logs, notifications, folders, and a role-scoped dashboard.

## Prerequisites

- **Java 17** or newer
- **PostgreSQL 14** or newer, installed and running locally
- **Gradle** is bundled via the included wrapper — no system install needed

## Step 1 — Create the database

Open a `psql` shell as the `postgres` superuser (the exact command depends on your install — `psql -U postgres` on most setups, `sudo -u postgres psql` on Ubuntu) and run:

```sql
CREATE DATABASE dms_db;
```

That's it. The application will auto-create all tables on first boot through Hibernate.

If your `postgres` user has a non-default password, note it — you'll override `DB_PASSWORD` in step 2.

## Step 2 — Run the backend

```bash
./gradlew bootRun
```

On Windows: `gradlew.bat bootRun`. First run takes a few minutes to download dependencies.

You'll know it worked when you see something like `Started DmsApplication in 8.4 seconds`. The API is now at **http://localhost:8080/api**.

On first boot the app seeds the four roles (`ROLE_ADMIN`, `ROLE_HR`, `ROLE_ACCOUNT`, `ROLE_EMPLOYEE`) and creates a default admin:

```
email:    admin@dms.com
password: Admin@123
```

**Change this password immediately** through `PUT /api/auth/change-password` or via the profile page in the frontend.

## Configuring credentials

All connection settings read from environment variables, with sensible defaults for local dev. Override anything that doesn't match your machine.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/dms_db` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | dev-only default | Base64 string of at least 32 bytes. Generate with `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Access-token lifetime |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7d) | Refresh-token lifetime |
| `UPLOAD_DIR` | `./uploads` | Where uploaded files are stored on disk |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | *(empty)* | SMTP user — leave empty to silently drop emails |
| `MAIL_PASSWORD` | *(empty)* | SMTP password / app-password |
| `MAIL_FROM` | `noreply@dms.local` | From-address for outgoing mail |
| `FRONTEND_URL` | `http://localhost:3000` | Used in email links |
| `LOG_LEVEL` | `INFO` | `com.dms` package log level |
| `SERVER_PORT` | `8080` | HTTP port |

Setting them on Linux/macOS:

```bash
export DB_PASSWORD=mysecret
export JWT_SECRET=$(openssl rand -base64 48)
./gradlew bootRun
```

Setting them on Windows PowerShell:

```powershell
$env:DB_PASSWORD = "mysecret"
$env:JWT_SECRET  = "paste-a-long-random-base64-string"
./gradlew.bat bootRun
```

## Roles

| Role | Display | Can do |
| --- | --- | --- |
| `ROLE_EMPLOYEE` | Employee | Read accessible docs, edit own profile |
| `ROLE_ACCOUNT`  | Account  | Employee + upload documents, upload new versions |
| `ROLE_HR`       | HR       | Account + view user list and employee directory |
| `ROLE_ADMIN`    | Admin    | Everything: approvals, settings, audit, role changes, deprecate/restore, hard delete |

New self-registrations default to `ROLE_EMPLOYEE`. Admins grant additional roles via `PUT /api/users/{id}/roles`.

## Key endpoints

**Auth**
- `POST /api/auth/login`, `/register`, `/refresh-token`, `/logout`, `/forgot-password`, `/reset-password`
- `GET  /api/auth/me`
- `PUT  /api/auth/change-password`

**Documents** (lifecycle uses deprecation, not hard delete)
- `GET  /api/documents`, `/search`, `/by-folder/{id}`, `/deprecated`
- `POST /api/documents/upload` (ADMIN/HR/ACCOUNT)
- `PUT  /api/documents/{id}`, `/{id}/move?folderId=`
- `POST /api/documents/{id}/deprecate`, `/{id}/restore`
- `DELETE /api/documents/{id}` (soft = deprecate)
- `DELETE /api/documents/{id}/purge` (admin hard delete)
- `GET  /api/documents/{id}/download`, `/preview`
- `GET/POST /api/documents/{id}/versions`, `/{id}/versions/{v}/restore`
- `GET/POST/PUT/DELETE /api/documents/{id}/permissions[/{userId}]`

**Folders**
- `GET  /api/folders`, `/tree`, `/{id}`, `/{id}/children`
- `POST /api/folders` (ACCOUNT+)
- `PUT  /api/folders/{id}` — rename/move/toggle public
- `DELETE /api/folders/{id}[?recursive=true]`

**Users** (deprecation-aware)
- `GET  /api/users/me`, `/{id}` (HR+)
- `PUT  /api/users/profile`, `/{id}/roles` (admin)
- `GET  /api/users`, `/directory` (HR+)
- `GET  /api/users/deprecated` (admin)
- `POST /api/users/{id}/deprecate`, `/{id}/restore`, `DELETE /api/users/{id}` (admin)

**Admin**
- `GET/PUT /api/approvals/{id}` — pending registrations
- `GET/PUT /api/settings` — system settings
- `GET  /api/audit?user=&action=&from=&to=`, `/audit/actions`

**Dashboard** (role-scoped automatically)
- `GET /api/dashboard/stats`, `/recent-documents`, `/upload-trend`, `/storage`

**Notifications**
- `GET/PUT /api/notifications`, `/notifications/settings`

**Local CDN** — files in `src/main/resources/static/cdn/` served at `GET /cdn/**`. Drop assets in `cdn/css/`, `cdn/js/`, `cdn/fonts/`, `cdn/images/` and reference as `/cdn/<filename>` from any HTML.

See `API_DOCUMENTATION.md` for the full request/response details.

## Stopping

`Ctrl+C` in the terminal stops the backend. PostgreSQL keeps running in the background as a regular service — stop it through your OS service manager if needed.

## Known rough edges

- The bundled `gradlew` was minimally patched to fix a shell-quoting bug. If you want the canonical Gradle 8.5 wrapper, run `gradle wrapper --gradle-version 8.5` once on a machine with Gradle installed.
- Tests beyond `contextLoads` are starter-only — add your own before shipping.
- The default `JWT_SECRET` is checked into source. **Never run production with the default value.**
