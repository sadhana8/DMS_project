# DocVault — Document Management System

A production-ready full-stack Document Management System built with **React 18**, **Spring Boot 3.2**, **PostgreSQL**, and **JWT** authentication.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Features](#features)
4. [Deprecation Policy — No Hard Deletes](#deprecation-policy--no-hard-deletes)
5. [Prerequisites](#prerequisites)
6. [Setup Guide](#setup-guide)
   - [Step 1 — Clone / download the project](#step-1--clone--download-the-project)
   - [Step 2 — Create the PostgreSQL database](#step-2--create-the-postgresql-database)
   - [Step 3 — Configure application.properties](#step-3--configure-applicationproperties)
   - [Step 4 — Configure Gmail (optional)](#step-4--configure-gmail-optional)
   - [Step 5 — Run the backend](#step-5--run-the-backend)
   - [Step 6 — Run the frontend](#step-6--run-the-frontend)
   - [Step 7 — Login and verify](#step-7--login-and-verify)
7. [Default Credentials](#default-credentials)
8. [API Reference](#api-reference)
9. [Role Permissions](#role-permissions)
10. [Deprecation API](#deprecation-api)
11. [Environment Variables](#environment-variables)
12. [Common Errors & Fixes](#common-errors--fixes)
13. [Daily Workflow](#daily-workflow)
14. [Building for Production](#building-for-production)
15. [Generate JavaDoc](#generate-javadoc)
16. [Production Checklist](#production-checklist)

---

## Tech Stack

| Layer     | Technology                                        |
|-----------|---------------------------------------------------|
| Frontend  | React 18, Vite, TailwindCSS, TanStack Query, Axios |
| Backend   | Spring Boot 3.2, Spring Security, Spring Data JPA  |
| Database  | PostgreSQL 14+                                     |
| Auth      | JWT (access + refresh tokens), BCrypt              |
| Email     | Spring JavaMailSender (SMTP)                       |
| File detection | Apache Tika                                   |
| Build     | Gradle 8.x (wrapper included — no install needed) |

---

## Project Structure

```
docvault/
├── backend/                          ← Spring Boot application
│   ├── build.gradle                  ← Dependencies and build config
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── gradlew / gradlew.bat         ← Gradle wrapper (no install needed)
│   ├── gradle/wrapper/
│   └── src/main/java/com/dms/
│       ├── config/                   ← SecurityConfig, AppConfig, DataInitializer
│       ├── controller/               ← REST endpoints (Auth, Document, User, Dashboard)
│       ├── dto/
│       │   ├── request/              ← LoginRequest, RegisterRequest, DeprecateRequest, ...
│       │   └── response/             ← AuthResponse, DocumentResponse, UserResponse, ...
│       ├── entity/                   ← JPA entities (User, Document, Role, ...)
│       │   └── DeprecationStatus.java ← ACTIVE | DEPRECATED | PERMANENTLY_DELETED
│       ├── exception/                ← GlobalExceptionHandler + custom exceptions
│       ├── repository/               ← JPA repositories with deprecation-aware queries
│       ├── security/                 ← JwtAuthenticationFilter, CustomUserDetailsService
│       ├── service/
│       │   ├── EmailService.java
│       │   ├── FileStorageService.java
│       │   └── impl/                 ← AuthServiceImpl, DocumentServiceImpl, UserServiceImpl
│       └── util/JwtUtil.java
│
└── frontend/                         ← React application
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    └── src/
        ├── api/                      ← client.js (axios + auto-refresh), auth.js, documents.js, users.js
        ├── components/
        │   ├── common/               ← Modal, Spinner, FileIcon, EmptyState, Pagination
        │   ├── documents/            ← UploadModal, ShareModal, VersionHistory, DocumentPreview
        │   └── layout/               ← AppLayout, Sidebar, Header
        ├── context/AuthContext.jsx   ← User state + role helpers
        ├── pages/
        │   ├── auth/                 ← Login, Register, ForgotPassword, ResetPassword
        │   ├── dashboard/            ← DashboardPage, ProfilePage
        │   ├── documents/            ← DocumentsPage, DocumentDetail
        │   ├── users/                ← UsersPage
        │   └── admin/                ← AdminPage
        └── routes/ProtectedRoute.jsx ← JWT + role guard
```

---

## Features

| Feature               | Details                                                              |
|-----------------------|----------------------------------------------------------------------|
| JWT Authentication    | Access tokens (24 h) + refresh tokens (7 days) with rotation        |
| Role-Based Access     | ADMIN · MANAGER · EDITOR · VIEWER — enforced at route & method level |
| Document Upload       | Any file type, up to 50 MB. MIME type detected by Apache Tika       |
| Document Preview      | Inline preview for PDF, images, and plain text                       |
| Version Control       | Full version history, upload new versions, restore any past version  |
| Document Sharing      | Per-user permissions: VIEW / DOWNLOAD / EDIT / ADMIN                 |
| **Soft Deprecation**  | No hard deletes — users and documents are deprecated, not removed    |
| Restore               | Admin can restore any deprecated user or document at any time        |
| Email Notifications   | Welcome email, password reset link, share notifications              |
| Dashboard             | Stats cards, upload trend chart, storage pie chart, recent documents |
| Search                | Full-text search across title, description, and tags                 |
| Folder organisation   | Hierarchical folder tree for document organisation                   |

---

## Deprecation Policy — No Hard Deletes

> **DocVault never permanently deletes users or documents through the API.**

Instead of deleting, every record goes through a **deprecation lifecycle**:

```
ACTIVE  ──►  DEPRECATED  ──►  ACTIVE        (restored by admin)
                  │
                  └──►  PERMANENTLY_DELETED  (admin-only, irreversible)
```

### Why deprecation instead of deletion?

- **Audit trail** — you always know who created what, even if they left.
- **Document ownership** — deprecated users' documents remain accessible.
- **Accidental actions** — a mistaken "delete" can be undone in seconds.
- **Compliance** — many regulations require data retention for 5–7 years.
- **Referential integrity** — no dangling foreign keys or broken references.

### What happens when a user is deprecated?

- Their account is hidden from all standard user-list queries.
- Login is immediately blocked (`isActive = false`).
- All refresh tokens are revoked (existing sessions terminated).
- Their documents, permissions, and history are fully preserved.
- `deprecatedAt`, `deprecationReason`, and `deprecatedBy` are recorded.

### What happens when a document is deprecated?

- It is excluded from all search results and document-list endpoints.
- Download and preview endpoints return `404 Not Found`.
- The physical file on disk is **not** deleted.
- `deprecatedAt`, `deprecationReason`, and `deprecatedBy` are recorded.

### Restore at any time

```
PUT /api/users/{id}/restore          → restore a deprecated user
PUT /api/documents/{id}/restore      → restore a deprecated document
GET /api/admin/deprecated/users      → list all deprecated users
GET /api/admin/deprecated/documents  → list all deprecated documents
```

---

## Prerequisites

| Tool          | Minimum version | Check command       | Download                        |
|---------------|-----------------|---------------------|---------------------------------|
| Java JDK      | 17              | `java -version`     | adoptium.net (Temurin 17 LTS)   |
| Node.js       | 18              | `node -v`           | nodejs.org                      |
| npm           | 9               | `npm -v`            | bundled with Node.js            |
| PostgreSQL    | 14              | `psql --version`    | postgresql.org/download         |
| Gradle        | 8.x             | `./gradlew --version`| **Not needed** — wrapper auto-downloads |

> **Gradle wrapper**: You do **not** need to install Gradle. The `gradlew` / `gradlew.bat` scripts download the correct version automatically on first run.

---

## Setup Guide

### Step 1 — Clone / download the project

Place the project in a path with **no spaces**:

```bash
# Good
C:\Projects\docvault\        (Windows)
~/Projects/docvault/         (Mac / Linux)

# Avoid
C:\My Documents\docvault\    (spaces cause issues)
```

### Step 2 — Create the PostgreSQL database

First, make sure PostgreSQL is running:

```bash
# Windows: Services → find "postgresql-x64-16" → Start
# Mac:
brew services start postgresql@16
# Linux:
sudo systemctl start postgresql
```

Then create the database:

```bash
# Open psql
psql -U postgres          # Mac / Linux
# Windows: search "SQL Shell (psql)" in Start menu
```

Run these SQL commands inside psql:

```sql
CREATE DATABASE dms_db;
CREATE USER dms_user WITH PASSWORD 'dms_pass123';
GRANT ALL PRIVILEGES ON DATABASE dms_db TO dms_user;
\q
```

> Spring Boot automatically creates all tables on first run — no SQL schema file needed.

### Step 3 — Configure application.properties

Open `backend/src/main/resources/application.properties` and update:

```properties
# Database — match what you set in Step 2
spring.datasource.url=jdbc:postgresql://localhost:5432/dms_db
spring.datasource.username=dms_user
spring.datasource.password=dms_pass123

# Email — fill in if you want password reset emails (see Step 4)
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_16_char_app_password

# Frontend URL — leave as-is for local development
app.frontend.url=http://localhost:3000

# File storage — where uploaded files are saved on disk
app.storage.upload-dir=./uploads
```

### Step 4 — Configure Gmail (optional)

Required only for password-reset and welcome emails. The app works fully without it.

1. Go to **myaccount.google.com → Security**
2. Enable **2-Step Verification**
3. Search **"App passwords"** → create one for Mail
4. Copy the 16-character password into `spring.mail.password`

### Step 5 — Run the backend

**Option A — IntelliJ IDEA (recommended)**

1. File → Open → select the `backend/` folder → "Open as Project"
2. Wait for Gradle sync (watch the progress bar at the bottom, ~2 min first time)
3. Settings → Build → Compiler → Annotation Processors → tick **"Enable annotation processing"**
4. Find `DocumentManagementSystemApplication.java` → click the green ▶ Run button

**Option B — Terminal**

```bash
# Mac / Linux
cd backend
chmod +x gradlew      # first time only
./gradlew bootRun

# Windows (Command Prompt)
cd backend
gradlew.bat bootRun

# Windows (PowerShell)
cd backend
.\gradlew.bat bootRun
```

Wait for this message in the console:
```
Started DocumentManagementSystemApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

> **First run**: all database tables are auto-created and the default admin account is seeded.

### Step 6 — Run the frontend

Open a **second terminal** (keep the backend terminal running):

```bash
# First time only — install dependencies (~200 MB, takes 1-2 min)
cd frontend
npm install

# Every time — start the dev server
npm run dev
```

### Step 7 — Login and verify

Open **http://localhost:3000** in your browser and login with:

| Field    | Value           |
|----------|-----------------|
| Email    | `admin@dms.com` |
| Password | `Admin@123`     |

> ⚠️ Change this password immediately: Profile → Security → Change Password.

Verify everything works:
- ✅ Dashboard loads with charts
- ✅ Upload a document in the Documents page
- ✅ Preview a PDF or image inline
- ✅ Register a second user and share a document with them
- ✅ Deprecate a document → it disappears from the list
- ✅ Restore it from the Admin panel → it reappears

---

## Default Credentials

| Role    | Email              | Password    |
|---------|--------------------|-------------|
| Admin   | `admin@dms.com`    | `Admin@123` |

The admin has both `ROLE_ADMIN` and `ROLE_MANAGER` roles. Change the password before deploying.

---

## API Reference

### Authentication (`/api/auth`)

| Method | Endpoint              | Access   | Description                          |
|--------|-----------------------|----------|--------------------------------------|
| POST   | `/login`              | Public   | Login, returns JWT tokens            |
| POST   | `/register`           | Public   | Create account, returns JWT tokens   |
| POST   | `/refresh-token`      | Public   | Rotate refresh token                 |
| POST   | `/logout`             | Auth     | Revoke all refresh tokens            |
| POST   | `/forgot-password`    | Public   | Send password-reset email            |
| POST   | `/reset-password`     | Public   | Reset password using token           |
| PUT    | `/change-password`    | Auth     | Change own password                  |

### Documents (`/api/documents`)

| Method | Endpoint                              | Access   | Description                    |
|--------|---------------------------------------|----------|--------------------------------|
| GET    | `/`                                   | Auth     | List accessible documents      |
| GET    | `/search?query=`                      | Auth     | Full-text search               |
| GET    | `/{id}`                               | Auth     | Get document details           |
| POST   | `/upload`                             | Editor+  | Upload new document            |
| PUT    | `/{id}`                               | Auth     | Update metadata                |
| PUT    | `/{id}/deprecate`                     | Owner/Admin | **Soft-deprecate document** |
| PUT    | `/{id}/restore`                       | Admin    | Restore deprecated document    |
| GET    | `/{id}/download`                      | Auth     | Download file                  |
| GET    | `/{id}/preview`                       | Auth     | Inline preview                 |
| GET    | `/{id}/versions`                      | Auth     | Version history                |
| POST   | `/{id}/versions`                      | Editor+  | Upload new version             |
| POST   | `/{id}/versions/{vId}/restore`        | Editor+  | Restore a past version         |
| GET    | `/{id}/versions/{vId}/download`       | Auth     | Download specific version      |
| GET    | `/{id}/permissions`                   | Auth     | List ACL                       |
| POST   | `/{id}/permissions`                   | Auth     | Share with user                |
| PUT    | `/{id}/permissions/{userId}`          | Auth     | Change permission level        |
| DELETE | `/{id}/permissions/{userId}`          | Auth     | Revoke access                  |

### Users (`/api/users`)

| Method | Endpoint              | Access       | Description                    |
|--------|-----------------------|--------------|--------------------------------|
| GET    | `/me`                 | Auth         | Own profile                    |
| PUT    | `/profile`            | Auth         | Update own profile             |
| GET    | `/`                   | Manager+     | List all active users          |
| GET    | `/{id}`               | Manager+     | Single user profile            |
| PUT    | `/{id}/roles`         | Admin        | Replace user roles             |
| PUT    | `/{id}/activate`      | Admin        | Re-enable account              |
| PUT    | `/{id}/deactivate`    | Admin        | Disable account (not deprecated) |
| PUT    | `/{id}/deprecate`     | Admin        | **Soft-deprecate user**        |
| PUT    | `/{id}/restore`       | Admin        | Restore deprecated user        |

### Dashboard (`/api/dashboard`)

| Method | Endpoint               | Description                    |
|--------|------------------------|--------------------------------|
| GET    | `/stats`               | Aggregate counts + storage     |
| GET    | `/recent-documents`    | Last 10 uploaded documents     |
| GET    | `/upload-trend`        | 30-day upload chart data       |
| GET    | `/storage`             | Storage breakdown by type      |

---

## Role Permissions

| Action                  | VIEWER | EDITOR | MANAGER | ADMIN |
|-------------------------|:------:|:------:|:-------:|:-----:|
| View / download docs    | ✅ | ✅ | ✅ | ✅ |
| Upload documents        | ❌ | ✅ | ✅ | ✅ |
| Edit / version docs     | ❌ | ✅ | ✅ | ✅ |
| Share documents         | ❌ | ✅ | ✅ | ✅ |
| Deprecate own docs      | ❌ | ✅ | ✅ | ✅ |
| View user list          | ❌ | ❌ | ✅ | ✅ |
| Manage user roles       | ❌ | ❌ | ❌ | ✅ |
| Deprecate users/docs    | ❌ | ❌ | ❌ | ✅ |
| Restore deprecated      | ❌ | ❌ | ❌ | ✅ |

---

## Deprecation API

### Deprecate a document

```http
PUT /api/documents/{id}/deprecate
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "Superseded by the 2024 revised version"
}
```

### Restore a document

```http
PUT /api/documents/{id}/restore
Authorization: Bearer <admin-token>
```

### Deprecate a user

```http
PUT /api/users/{id}/deprecate
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "reason": "Employee left the organisation on 2024-12-01"
}
```

### Restore a user

```http
PUT /api/users/{id}/restore
Authorization: Bearer <admin-token>
```

### List all deprecated documents (admin)

```http
GET /api/admin/deprecated/documents?page=0&size=20
Authorization: Bearer <admin-token>
```

### List all deprecated users (admin)

```http
GET /api/admin/deprecated/users?page=0&size=20
Authorization: Bearer <admin-token>
```

---

## Environment Variables

All configuration lives in `backend/src/main/resources/application.properties`:

| Property                              | Default              | Description                        |
|---------------------------------------|----------------------|------------------------------------|
| `spring.datasource.url`               | PostgreSQL localhost | JDBC connection string             |
| `spring.datasource.username`          | `postgres`           | DB username                        |
| `spring.datasource.password`          | —                    | DB password                        |
| `app.jwt.secret`                      | —                    | 64-char hex key for JWT signing    |
| `app.jwt.expiration`                  | `86400000` (24 h)    | Access token lifetime (ms)         |
| `app.jwt.refresh-expiration`          | `604800000` (7 days) | Refresh token lifetime (ms)        |
| `app.storage.upload-dir`              | `./uploads`          | Directory for uploaded files       |
| `app.storage.max-file-size`           | `52428800` (50 MB)   | Maximum upload size (bytes)        |
| `spring.mail.host`                    | `smtp.gmail.com`     | SMTP server                        |
| `spring.mail.username`                | —                    | SMTP username                      |
| `spring.mail.password`                | —                    | SMTP app password                  |
| `app.mail.from`                       | `noreply@dms.com`    | Sender address                     |
| `app.frontend.url`                    | `http://localhost:3000` | Frontend origin for CORS & links|
| `app.password-reset.expiry-minutes`   | `60`                 | Reset link lifetime                |

---

## Common Errors & Fixes

| Error                                    | Fix                                                              |
|------------------------------------------|------------------------------------------------------------------|
| `Port 8080 already in use`               | Add `server.port=8081` to properties; update `vite.config.js` proxy |
| `Failed to configure DataSource`          | PostgreSQL is not running — start it first                       |
| `Permission denied: ./gradlew`            | Run `chmod +x gradlew`                                           |
| `JAVA_HOME not set`                       | Windows: add `JAVA_HOME=C:\Program Files\Java\jdk-17` to System Environment Variables |
| `npm install` fails                       | Delete `node_modules/` and `package-lock.json`, then retry       |
| Lombok getters missing in IntelliJ        | Settings → Build → Compiler → Annotation Processors → Enable     |
| White screen at `localhost:3000`          | Check backend is running; open DevTools → Console for errors     |
| `PKIX path building failed` (SSL)         | Add `spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com`  |
| `User already deprecated`                 | The user is already in DEPRECATED state — restore first if needed |
| `Document is not deprecated`              | Cannot restore an ACTIVE document — it was never deprecated      |

---

## Daily Workflow

Every time you want to use the application:

```bash
# 1. Start PostgreSQL
#    Windows: Services → PostgreSQL → Start
#    Mac: brew services start postgresql@16
#    Linux: sudo systemctl start postgresql

# 2. Terminal 1 — Start backend
cd backend
./gradlew bootRun          # Mac/Linux
gradlew.bat bootRun        # Windows

# 3. Terminal 2 — Start frontend (keep Terminal 1 open)
cd frontend
npm run dev

# 4. Open browser
# http://localhost:3000
```

To stop, press `Ctrl+C` in each terminal.

---

## Building for Production

### Backend — create a runnable JAR

```bash
cd backend
./gradlew build -x test

# JAR is at:
build/libs/document-management-system-1.0.0.jar

# Run it:
java -jar build/libs/document-management-system-1.0.0.jar
```

### Frontend — create a static build

```bash
cd frontend
npm run build

# Output is in: dist/
# Deploy dist/ to any static host (Nginx, Apache, S3, Netlify, Vercel)
```

---

## Generate JavaDoc

```bash
cd backend
./gradlew javadoc

# Open in browser:
build/docs/javadoc/index.html
```

The JavaDoc covers all 56 Java files with 426 documented blocks including `@param`, `@return`, `@throws`, and `@see` cross-references.

---

## Production Checklist

- [ ] Change default admin password (`Admin@123`)
- [ ] Generate a strong JWT secret: `openssl rand -hex 32`
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Configure HTTPS (Nginx reverse proxy or cloud load balancer)
- [ ] Set `app.storage.upload-dir` to a persistent disk location (or swap to S3)
- [ ] Set `app.frontend.url` to your production domain
- [ ] Configure proper CORS origins in `SecurityConfig`
- [ ] Add rate limiting at the API gateway level
- [ ] Set up database backups
- [ ] Configure log rotation

---

## License

This project is licensed under the MIT License.