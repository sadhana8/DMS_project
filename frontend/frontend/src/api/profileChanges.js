import client from './client'

/**
 * API client for the profile-change-request workflow.
 *
 * Backend paths (mirrored exactly):
 *   POST /profile-changes              — employee submits
 *   GET  /profile-changes/mine         — employee's own requests
 *   GET  /profile-changes              — HR/Admin list (status= filter)
 *   GET  /profile-changes/count        — { pending: N }
 *   PUT  /profile-changes/{id}/review  — HR/Admin approve/reject
 */
export const profileChangesApi = {
  create: (data) =>
    client.post('/profile-changes', data).then(r => r.data),

  listMine: (params) =>
    client.get('/profile-changes/mine', { params }).then(r => r.data),

  listForReview: (params) =>
    client.get('/profile-changes', { params }).then(r => r.data),

  pendingCount: () =>
    client.get('/profile-changes/count').then(r => r.data),

  review: (id, data) =>
    client.put(`/profile-changes/${id}/review`, data).then(r => r.data),
}