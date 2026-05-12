import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

export const setAuthToken   = (token) => { client.defaults.headers.common['Authorization'] = `Bearer ${token}` }
export const clearAuthToken = ()      => { delete client.defaults.headers.common['Authorization'] }

/* Request interceptor — always pick up latest token */
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  return config
})

/* Response interceptor — auto-refresh on 401 */
let isRefreshing  = false
let failedQueue   = []

const processQueue = (error, token = null) => {
  failedQueue.forEach((p) => error ? p.reject(error) : p.resolve(token))
  failedQueue = []
}

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => failedQueue.push({ resolve, reject }))
          .then((token) => { original.headers['Authorization'] = `Bearer ${token}`; return client(original) })
          .catch((err)  => Promise.reject(err))
      }
      original._retry = true
      isRefreshing     = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        isRefreshing = false
        window.location.href = '/login'
        return Promise.reject(error)
      }
      try {
        const { data } = await axios.post('/api/auth/refresh-token', { refreshToken })
        localStorage.setItem('accessToken', data.accessToken)
        setAuthToken(data.accessToken)
        processQueue(null, data.accessToken)
        original.headers['Authorization'] = `Bearer ${data.accessToken}`
        return client(original)
      } catch (err) {
        processQueue(err, null)
        localStorage.clear()
        window.location.href = '/login'
        return Promise.reject(err)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default client
