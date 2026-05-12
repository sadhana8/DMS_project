import client from './client'

export const approvalsApi = {
  list:   (p) => client.get('/approvals', { params: p }).then(r => r.data),
  count:  ()  => client.get('/approvals/count').then(r => r.data.pending),
  review: (id, d) => client.put(`/approvals/${id}`, d).then(r => r.data),
}
