# What's new — Lifecycle & Email release

## New backend features

### Stricter email validation
Both `/auth/register` and `/auth/forgot-password` now reject malformed email
addresses with a 400 before any database lookup. Pattern allows the standard
`name@domain.tld` shape with at least a 2-character TLD.

### Forgot password — confirmed working with real SMTP
No code change required from before — but the email flow is documented as the
default path. Configure SMTP via the env vars below and password resets work
end-to-end.

### Admin creates user
- `POST /api/users/admin-create` (admin only)
- Body: `{ username, email, firstName, lastName, phoneNumber, roles[] }`
- Server generates a 12-char strong random password
- Emails the user their temp credentials
- Sets `mustChangePassword=true`
- On first login the response carries `mustChangePassword: true`
- The new user must call `PUT /api/auth/first-login-password-change` (no current password required, only the new one) before they can use the system normally

### Termination — immediate access revocation
- `POST /api/users/{id}/terminate` (admin only)
- Body: `{ reason }` (required, 5-500 chars)
- Sets `isActive=false`, records `terminatedAt`, `terminationReason`, `terminatedBy`
- Revokes all refresh tokens immediately
- Writes `USER_TERMINATE` audit entry
- Emails the user
- Termination prevents login forever (until admin restores)
- An admin can't terminate themselves

### Resignation — end-of-month effective date
- `POST /api/users/me/resign` (any user) or `POST /api/users/{id}/resign` (admin)
- Body: `{ reason, effectiveDate (YYYY-MM-DD, optional admin override) }`
- Default effective date: last day of the current month at 23:59:59
- Records `resignationDate` and `resignationEffectiveDate` on the user
- Writes `USER_RESIGN` audit entry
- Emails the user

### Scheduled access revocation
- New `ResignationScheduler` runs every minute
- Finds users where `resignationEffectiveDate <= now() AND isActive=true`
- Sets them inactive, revokes refresh tokens
- Writes `USER_ACCESS_REVOKED` audit entry
- "Within minutes" SLA from the originally-requested feature

### New audit actions
`USER_TERMINATE`, `USER_RESIGN`, `USER_ACCESS_REVOKED`

### New User entity fields
- `mustChangePassword` — boolean
- `resignationDate`, `resignationEffectiveDate` — datetimes
- `terminatedAt`, `terminationReason`, `terminatedBy` — termination metadata

These are all exposed in `UserResponse` so the frontend can display them.

### New notification type
`PENDING_APPROVAL` — fanned out to all admins when someone registers.
Already wired up; no extra setup needed.

## SMTP setup (for emails to actually deliver)

Set these env vars before running `gradlew bootRun`:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-real-email@gmail.com
MAIL_PASSWORD=your-16-char-google-app-password
MAIL_FROM=your-real-email@gmail.com
FRONTEND_URL=http://localhost:3000
```

For Gmail you must generate an "App password" (not your regular password):
1. Go to https://myaccount.google.com/apppasswords
2. Generate one for "Mail" → "Other (DocVault)"
3. Use that 16-char string as `MAIL_PASSWORD`

If `MAIL_USERNAME` is empty, every email-sending call silently does nothing
(safe for local development).

## Testing the flows

### Forgot password
1. POST `/auth/forgot-password` with `{ email: "admin@dms.com" }`
2. Check the email inbox of the user, click the reset link

### Admin creates user
1. Log in as admin
2. POST `/users/admin-create` with `{ username, email, firstName, lastName, roles: ["ROLE_EMPLOYEE"] }`
3. The new user receives an email with their temp password
4. They log in — response has `mustChangePassword: true`
5. They PUT `/auth/first-login-password-change` with `{ newPassword }`
6. They can then log in normally

### Termination
1. Log in as admin
2. POST `/users/{id}/terminate` with `{ reason: "Policy violation" }`
3. The user is logged out within seconds (refresh tokens revoked)
4. They cannot log in again

### Resignation
1. Employee POSTs `/users/me/resign` with `{ reason: "Found new role" }`
2. Effective date defaults to last day of current month
3. They keep access until that date
4. At/after effective date, the scheduled job revokes their access within 1 minute

## Database changes

`ddl-auto=update` will add the new columns automatically:
- users: `must_change_password`, `resignation_date`, `resignation_effective_date`,
  `terminated_at`, `termination_reason`, `terminated_by`
- audit_logs: no schema change (new enum values stored as strings)

If you previously ran the app with the old enum CHECK constraint on `audit_logs.action`,
drop it once so Hibernate can rebuild it with the new values:

```sql
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
```

Restart the backend after that — Hibernate recreates the constraint with the
full enum.
