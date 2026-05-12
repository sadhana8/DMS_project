import client from './client'

/**
 * Document API client. Verbs and paths mirror the backend
 * `DocumentController` exactly.
 */
export const documentsApi = {
  // ── List / Search ─────────────────────────────────────────────────
  list:   (params)        => client.get('/documents',           { params }).then(r => r.data),
  get:    (id)            => client.get(`/documents/${id}`).then(r => r.data),
  search: (query, params) => client.get('/documents/search',    { params: { query, ...params } }).then(r => r.data),

  // ── Upload ────────────────────────────────────────────────────────
  upload: (formData, onProgress) =>
    client.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => onProgress?.(Math.round((e.loaded * 100) / e.total)),
    }).then(r => r.data),

  // ── Update / Move ─────────────────────────────────────────────────
  update: (id, data)      => client.put(`/documents/${id}`, data).then(r => r.data),
  move:   (id, folderId)  =>
    client.put(`/documents/${id}/move`, null, { params: folderId == null ? {} : { folderId } })
          .then(r => r.data),

  // ── Lifecycle (POST in the backend) ───────────────────────────────
  deprecate: (id, reason) => client.post(`/documents/${id}/deprecate`, { reason }).then(r => r.data),
  restore:   (id)         => client.post(`/documents/${id}/restore`).then(r => r.data),

  /** Soft-delete; backend turns this into a deprecate. */
  delete:    (id)         => client.delete(`/documents/${id}`).then(r => r.data),
  /** Admin-only hard delete. */
  purge:     (id)         => client.delete(`/documents/${id}/purge`).then(r => r.data),

  // ── Download / Preview ────────────────────────────────────────────
  download: async (id, fileName) => {
    const res = await client.get(`/documents/${id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data]))
    const a   = document.createElement('a')
    a.href = url; a.download = fileName || 'download'; a.click()
    URL.revokeObjectURL(url)
  },
  preview: (id) => client.get(`/documents/${id}/preview`, { responseType: 'blob' }).then(r => r.data),

  // ── Versions ──────────────────────────────────────────────────────
  getVersions:    (id) => client.get(`/documents/${id}/versions`).then(r => r.data),
  uploadVersion:  (id, formData, summary, onProgress) =>
    client.post(`/documents/${id}/versions`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params:  { changeSummary: summary },
      onUploadProgress: (e) => onProgress?.(Math.round((e.loaded * 100) / e.total)),
    }).then(r => r.data),
  restoreVersion: (id, versionId)            => client.post(`/documents/${id}/versions/${versionId}/restore`).then(r => r.data),
  downloadVersion: async (id, vId, fileName) => {
    const res = await client.get(`/documents/${id}/versions/${vId}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data]))
    const a   = document.createElement('a')
    a.href = url; a.download = fileName || 'download'; a.click()
    URL.revokeObjectURL(url)
  },

  // ── Permissions / Sharing ─────────────────────────────────────────
  getPermissions:   (id)             => client.get(`/documents/${id}/permissions`).then(r => r.data),
  shareDocument:    (id, data)       => client.post(`/documents/${id}/permissions`, data).then(r => r.data),
  updatePermission: (id, uid, perm)  => client.put(`/documents/${id}/permissions/${uid}`, { permission: perm }).then(r => r.data),
  removePermission: (id, uid)        => client.delete(`/documents/${id}/permissions/${uid}`).then(r => r.data),
}
