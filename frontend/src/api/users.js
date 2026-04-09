import client from './client'

export const usersApi = {
  list:            (params) => client.get('/users', { params }).then(r => r.data),
  listDeprecated:  (params) => client.get('/admin/deprecated/users', { params }).then(r => r.data),
  get:             (id)     => client.get(`/users/${id}`).then(r => r.data),
  update:          (id, d)  => client.put(`/users/${id}`, d).then(r => r.data),
  updateRoles:     (id, roles) => client.put(`/users/${id}/roles`, { roles }).then(r => r.data),
  activate:        (id)     => client.put(`/users/${id}/activate`).then(r => r.data),
  deactivate:      (id)     => client.put(`/users/${id}/deactivate`).then(r => r.data),
  deprecate:       (id, reason) => client.put(`/users/${id}/deprecate`, { reason }).then(r => r.data),
  restore:         (id)     => client.put(`/users/${id}/restore`).then(r => r.data),
  updateProfile:   (data)   => client.put('/users/profile', data).then(r => r.data),
  uploadAvatar:    (form)   => client.post('/users/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data),
}

export const dashboardApi = {
  stats:            () => client.get('/dashboard/stats').then(r => r.data),
  recentDocs:       () => client.get('/dashboard/recent-documents').then(r => r.data),
  activityLog:      () => client.get('/dashboard/activity').then(r => r.data),
  storageBreakdown: () => client.get('/dashboard/storage').then(r => r.data),
  uploadTrend:      () => client.get('/dashboard/upload-trend').then(r => r.data),
}
