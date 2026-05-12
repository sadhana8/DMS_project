import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import Spinner from '@/components/common/Spinner'

export default function ProtectedRoute({ requiredRole }) {
  const { isAuthenticated, loading, hasRole } = useAuth()
  const location = useLocation()

  if (loading) return <div className="h-screen flex items-center justify-center"><Spinner size="lg" /></div>

  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />

  if (requiredRole && !hasRole(requiredRole) && !hasRole('ROLE_ADMIN')) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
