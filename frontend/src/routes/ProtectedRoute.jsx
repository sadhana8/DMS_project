import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import Spinner from '@/components/common/Spinner'

/**
 * Guards a route behind authentication and an optional role check.
 *
 * `requiredRole` — a single role string OR an array of role strings.
 * The user must have AT LEAST ONE of the listed roles (or ROLE_ADMIN,
 * which always passes).
 */
export default function ProtectedRoute({ requiredRole }) {
  const { isAuthenticated, loading, hasRole } = useAuth()
  const location = useLocation()

  if (loading) return <div className="h-screen flex items-center justify-center"><Spinner size="lg" /></div>

  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />

  if (requiredRole) {
    const roles = Array.isArray(requiredRole) ? requiredRole : [requiredRole]
    const hasAccess = hasRole('ROLE_ADMIN') || roles.some(r => hasRole(r))
    if (!hasAccess) return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
