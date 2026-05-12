import client from './client'

export const settingsApi = {
  getAll:      ()     => client.get('/settings').then(r => r.data),
  getCategory: (cat)  => client.get(`/settings/${cat}`).then(r => r.data),
  update:      (data) => client.put('/settings', { settings: data }).then(r => r.data),
}
