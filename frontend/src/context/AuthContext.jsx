import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { authApi } from '@/api/auth'
import { setAuthToken, clearAuthToken } from '@/api/client'
import toast from 'react-hot-toast'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null)
  const [loading, setLoading] = useState(true)

  /* Hydrate session from localStorage on mount */
  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    const saved  = localStorage.getItem('user')
    if (token && saved) {
      setAuthToken(token)
      try { setUser(JSON.parse(saved)) } catch { clearSession() }
    }
    setLoading(false)
  }, [])

  const saveSession = (data) => {
    localStorage.setItem('accessToken',  data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data.user))
    setAuthToken(data.accessToken)
    setUser(data.user)
  }

  const clearSession = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    clearAuthToken()
    setUser(null)
  }, [])

  const login = async (credentials) => {
    const data = await authApi.login(credentials)
    saveSession(data)
    toast.success(`Welcome back, ${data.user.firstName}!`)
    return data
  }

  const register = async (payload) => {
    const data = await authApi.register(payload)
    // Backend returns one of two shapes:
    //   • AuthResponse  → { accessToken, refreshToken, user, ... }
    //   • ApiResponse   → { success, message }   (when admin approval is required)
    if (data?.accessToken && data?.user) {
      saveSession(data)
      toast.success('Account created successfully!')
      return { ...data, pending: false }
    }
    // Pending approval — caller should redirect to /login with a banner
    toast.success(data?.message || 'Registration received. An admin will review your account.')
    return { pending: true, message: data?.message }
  }

  const logout = useCallback(async () => {
    try { await authApi.logout() } catch {}
    clearSession()
    toast.success('Logged out')
  }, [clearSession])

  const refreshUser = async () => {
    try {
      const data = await authApi.me()
      setUser(data)
      localStorage.setItem('user', JSON.stringify(data))
    } catch { clearSession() }
  }

  /* Role helpers — privilege ladder: ADMIN > HR > ACCOUNT > EMPLOYEE */
  const hasRole       = (role) => user?.roles?.includes(role) ?? false
  const isAdmin       = ()     => hasRole('ROLE_ADMIN')
  const isHr          = ()     => hasRole('ROLE_HR')      || isAdmin()
  const isAccount     = ()     => hasRole('ROLE_ACCOUNT') || isHr()
  const isEmployee    = ()     => hasRole('ROLE_EMPLOYEE') || isAccount()
  const canUpload     = ()     => isAccount()
  const canManageUsers = ()    => isAdmin()
  // Backward-compat aliases — older components may still call these.
  const isManager     = isHr
  const isEditor      = isAccount

  return (
    <AuthContext.Provider value={{
      user, loading,
      login, register, logout, refreshUser,
      hasRole, isAdmin, isHr, isAccount, isEmployee,
      isManager, isEditor,                   // legacy aliases
      canUpload, canManageUsers,
      isAuthenticated: !!user,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
