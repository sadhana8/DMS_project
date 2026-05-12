import client from './client'

export const auditApi = {
  search:       (p)    => client.get('/audit', { params: p }).then(r => r.data),
  entityHistory:(t, id)=> client.get(`/audit/entity/${t}/${id}`).then(r => r.data),
  stats:        (from) => client.get('/audit/stats', { params: { since: from } }).then(r => r.data),
  actions:      ()     => client.get('/audit/actions').then(r => r.data),
}
