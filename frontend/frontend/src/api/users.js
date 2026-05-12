import client from './client'

/**
 * User API client. Paths/verbs mirror the backend `UserController`.
 *
 * Notes:
 *  - The backend uses POST for /deprecate and /restore lifecycle calls.
 *  - "deactivate"/"activate" are kept as separate idempotent PUTs that map
 *    to the older /activate, /deactivate endpoints.
 */
export const usersApi = {
  list:           (params)     => client.get('/users',                      { params }).then(r => r.data),
  listDeprecated: (params)     => client.get('/users/deprecated',           { params }).then(r => r.data),
  directory:      ()           => client.get('/users/directory').then(r => r.data),
  get:            (id)         => client.get(`/users/${id}`).then(r => r.data),
  updateRoles:    (id, roles)  => client.put(`/users/${id}/roles`,    { roles }).then(r => r.data),
  activate:       (id)         => client.put(`/users/${id}/activate`).then(r => r.data),
  deactivate:     (id)         => client.put(`/users/${id}/deactivate`).then(r => r.data),
  deprecate:      (id, reason) => client.post(`/users/${id}/deprecate`, { reason }).then(r => r.data),
  restore:        (id)         => client.post(`/users/${id}/restore`).then(r => r.data),
  /** Permanent delete — admin only. */
  delete:         (id)         => client.delete(`/users/${id}`).then(r => r.data),
  updateProfile:  (data)       => client.put('/users/profile', data).then(r => r.data),
  changePassword: (data)       => client.put('/auth/change-password', data).then(r => r.data),
  me:             ()           => client.get('/users/me').then(r => r.data),

  // ── New admin lifecycle endpoints ─────────────────────────────────
  /** Admin creates a user. Backend generates random password, emails it. */
  adminCreate:    (data)       => client.post('/users/admin-create',  data).then(r => r.data),
  /** Immediate access revocation. Reason required (5-500 chars). */
  terminate:      (id, reason) => client.post(`/users/${id}/terminate`, { reason }).then(r => r.data),
  /** Self resignation. Optional reason and effectiveDate (yyyy-mm-dd). */
  resignSelf:     (data)       => client.post('/users/me/resign', data ?? {}).then(r => r.data),
  /** Admin records resignation on someone's behalf. */
  resignFor:      (id, data)   => client.post(`/users/${id}/resign`, data ?? {}).then(r => r.data),
}

export const dashboardApi = {
  stats:            () => client.get('/dashboard/stats').then(r => r.data),
  recentDocs:       () => client.get('/dashboard/recent-documents').then(r => r.data),
  storageBreakdown: () => client.get('/dashboard/storage').then(r => r.data),
  uploadTrend:      () => client.get('/dashboard/upload-trend').then(r => r.data),
}
