import client from './client'

export const documentsApi = {
  list: (params) =>
    client.get('/documents', { params }).then(r => r.data),

  get: (id) =>
    client.get(`/documents/${id}`).then(r => r.data),

  upload: (formData, onProgress) =>
    client.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => onProgress?.(Math.round((e.loaded * 100) / e.total)),
    }).then(r => r.data),

  update: (id, data) =>
    client.put(`/documents/${id}`, data).then(r => r.data),

  deprecate: (id, reason) =>
    client.put(`/documents/${id}/deprecate`, { reason }).then(r => r.data),

  restore: (id) =>
    client.put(`/documents/${id}/restore`).then(r => r.data),

  download: async (id, fileName) => {
    const res = await client.get(`/documents/${id}/download`, { responseType: 'blob' })
    const url  = URL.createObjectURL(new Blob([res.data]))
    const a    = document.createElement('a')
    a.href     = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
  },

  preview: (id) =>
    client.get(`/documents/${id}/preview`, { responseType: 'blob' }).then(r => r.data),

  search: (query, params) =>
    client.get('/documents/search', { params: { query, ...params } }).then(r => r.data),

  // Versions
  getVersions: (id) =>
    client.get(`/documents/${id}/versions`).then(r => r.data),

  uploadVersion: (id, formData, changeSummary, onProgress) =>
    client.post(`/documents/${id}/versions`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: { changeSummary },
      onUploadProgress: (e) => onProgress?.(Math.round((e.loaded * 100) / e.total)),
    }).then(r => r.data),

  restoreVersion: (id, versionId) =>
    client.post(`/documents/${id}/versions/${versionId}/restore`).then(r => r.data),

  downloadVersion: async (id, versionId, fileName) => {
    const res = await client.get(`/documents/${id}/versions/${versionId}/download`, { responseType: 'blob' })
    const url  = URL.createObjectURL(new Blob([res.data]))
    const a    = document.createElement('a')
    a.href     = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
  },

  // Permissions
  getPermissions: (id) =>
    client.get(`/documents/${id}/permissions`).then(r => r.data),

  shareDocument: (id, data) =>
    client.post(`/documents/${id}/permissions`, data).then(r => r.data),

  updatePermission: (id, userId, permission) =>
    client.put(`/documents/${id}/permissions/${userId}`, { permission }).then(r => r.data),

  removePermission: (id, userId) =>
    client.delete(`/documents/${id}/permissions/${userId}`).then(r => r.data),
}
