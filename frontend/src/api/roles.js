import client from './client'

/**
 * Role management API. Mirrors backend `RoleController` at /roles/*.
 *
 * Roles are a fixed enum: ROLE_ADMIN, ROLE_HR, ROLE_ACCOUNT, ROLE_EMPLOYEE.
 * This client exposes the read-only catalog and the permission matrix.
 * Role assignment to a user is performed via `usersApi.updateRoles`.
 */
export const rolesApi = {
  list:               ()       => client.get('/roles').then(r => r.data),
  get:                (id)     => client.get(`/roles/${id}`).then(r => r.data),
  /** Full role/permission matrix — admin only. */
  permissionsMatrix:  ()       => client.get('/roles/permissions').then(r => r.data),
  /** Permissions for one role; accepts ROLE_ADMIN or ADMIN. */
  permissionsForRole: (name)   => client.get(`/roles/${name}/permissions`).then(r => r.data),
}
