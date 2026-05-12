# DocVault Frontend

A React 18 + Vite + Tailwind UI for the DocVault backend.

## Prerequisites
- Node.js 18 or newer
- The DocVault backend running on `http://localhost:8080`

## Quick start
```bash
npm install
npm run dev
```
The dev server runs on http://localhost:3000 and proxies `/api/**` to the backend at `http://localhost:8080`. Open http://localhost:3000 and sign in with the seeded admin (`admin@dms.com` / `Admin@123`) — change that password from the profile page immediately.

## Production build
```bash
npm run build      # outputs to dist/
npm run preview    # serve the built files locally for sanity check
```

## Backend URL
Dev: handled by Vite's proxy in `vite.config.js`.
Production: serve `dist/` from the same origin as the API, or change `baseURL` in `src/api/client.js` to your absolute API URL.

## Roles & what each can see
- **VIEWER** — read documents shared with them, manage their own profile.
- **EDITOR** — viewer + upload documents and new versions.
- **MANAGER** — editor + see the user list.
- **ADMIN** — everything: approvals queue, audit trail, system settings, deprecate/restore users and documents, role changes, hard-delete (purge).

The dashboard automatically scopes its widgets to the caller's top role — admins/managers see system-wide totals, editors see their own data, viewers see only what they can access.

## Known gaps in this revision
- **No folder UI yet.** The backend supports folders fully (`/api/folders/*`), but the React side does not have a folder browser, picker, or move-to-folder UI. Add it as `src/api/folders.js` and a `FoldersPage.jsx` when ready.
- **No deprecated-users admin page yet.** `usersApi.listDeprecated()` and `usersApi.restore()` work; just no page consumes them.
- **No verify-email flow.** The placeholder endpoint was removed because the backend doesn't expose it.
