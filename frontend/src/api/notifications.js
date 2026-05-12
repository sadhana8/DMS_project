import client from './client'

export const notificationsApi = {
  list:         (p) => client.get('/notifications', { params: p }).then(r => r.data),
  unreadCount:  ()  => client.get('/notifications/unread-count').then(r => r.data.count),
  markAllRead:  ()  => client.put('/notifications/read-all').then(r => r.data),
  markOneRead:  (id)=> client.put(`/notifications/${id}/read`).then(r => r.data),
  getSettings:  ()  => client.get('/notifications/settings').then(r => r.data),
  updateSetting:(d) => client.put('/notifications/settings', d).then(r => r.data),
  resetSettings:()  => client.post('/notifications/settings/reset').then(r => r.data),
}
