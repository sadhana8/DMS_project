# Role rename: MIGRATION NOTES

The roles have been renamed to organizational terms:

| Old           | New             | Display label |
| ------------- | --------------- | ------------- |
| ROLE_ADMIN    | ROLE_ADMIN      | Admin         |
| ROLE_MANAGER  | **ROLE_HR**     | HR            |
| ROLE_EDITOR   | **ROLE_ACCOUNT**| Account       |
| ROLE_VIEWER   | **ROLE_EMPLOYEE**| Employee     |

## If you have NEVER run the old version

Nothing to do. Just run the new build — `DataInitializer` will seed the four
new roles automatically and create the default admin.

## If you HAVE previously run the old version against PostgreSQL

The `roles` table already contains rows with the old names, and existing users
are linked to those rows. You have two options:

### Option A — Drop and recreate the database (simplest, dev only)

```sql
DROP DATABASE dms_db;
CREATE DATABASE dms_db;
```

Then restart the backend — `DataInitializer` will reseed everything.

### Option B — Run a SQL migration (preserves users)

Connect with `psql -U postgres -d dms_db` and run:

```sql
UPDATE roles SET name = 'ROLE_HR'       WHERE name = 'ROLE_MANAGER';
UPDATE roles SET name = 'ROLE_ACCOUNT'  WHERE name = 'ROLE_EDITOR';
UPDATE roles SET name = 'ROLE_EMPLOYEE' WHERE name = 'ROLE_VIEWER';
```

Or as a one-liner from a regular shell:

```bash
psql -U postgres -d dms_db -c "\
  UPDATE roles SET name='ROLE_HR'       WHERE name='ROLE_MANAGER'; \
  UPDATE roles SET name='ROLE_ACCOUNT'  WHERE name='ROLE_EDITOR'; \
  UPDATE roles SET name='ROLE_EMPLOYEE' WHERE name='ROLE_VIEWER';"
```

After the SQL runs, restart the backend (`./gradlew bootRun`). The four roles
are now renamed and existing user-role links are preserved.

## Permission mapping (unchanged)

The privilege ladder kept the same structure — only the labels changed.

| Capability                          | EMPLOYEE | ACCOUNT | HR  | ADMIN |
| ----------------------------------- | :------: | :-----: | :-: | :---: |
| Read accessible documents           |    ✓     |    ✓    |  ✓  |   ✓   |
| Edit own profile                    |    ✓     |    ✓    |  ✓  |   ✓   |
| Upload documents and new versions   |          |    ✓    |  ✓  |   ✓   |
| See user list / employee directory  |          |         |  ✓  |   ✓   |
| Approvals queue                     |          |         |     |   ✓   |
| Audit trail                         |          |         |     |   ✓   |
| System settings                     |          |         |     |   ✓   |
| Change roles, deprecate/restore     |          |         |     |   ✓   |
| Hard-delete (purge)                 |          |         |     |   ✓   |
