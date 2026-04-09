import client from './client'

export const authApi = {
  login:         (data)  => client.post('/auth/login',          data).then(r => r.data),
  register:      (data)  => client.post('/auth/register',       data).then(r => r.data),
  logout:        ()      => client.post('/auth/logout'),
  me:            ()      => client.get('/auth/me').then(r => r.data),
  refreshToken:  (token) => client.post('/auth/refresh-token',  { refreshToken: token }).then(r => r.data),
  forgotPassword:(email) => client.post('/auth/forgot-password',{ email }).then(r => r.data),
  resetPassword: (data)  => client.post('/auth/reset-password', data).then(r => r.data),
  verifyEmail:   (token) => client.post('/auth/verify-email',   { token }).then(r => r.data),
  changePassword:(data)  => client.put('/auth/change-password', data).then(r => r.data),
}
