import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '@/context/AuthContext'
import ProtectedRoute  from '@/routes/ProtectedRoute'
import AppLayout       from '@/components/layout/AppLayout'

// Auth pages
import LoginPage          from '@/pages/auth/LoginPage'
import RegisterPage       from '@/pages/auth/RegisterPage'
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage'
import ResetPasswordPage  from '@/pages/auth/ResetPasswordPage'

// App pages
import DashboardPage  from '@/pages/dashboard/DashboardPage'
import DocumentsPage  from '@/pages/documents/DocumentsPage'
import DocumentDetail from '@/pages/documents/DocumentDetail'
import UsersPage      from '@/pages/users/UsersPage'
import ProfilePage    from '@/pages/dashboard/ProfilePage'
import AdminPage      from '@/pages/admin/AdminPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public */}
        <Route path="/login"           element={<LoginPage />} />
        <Route path="/register"        element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password"  element={<ResetPasswordPage />} />

        {/* Protected */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/"               element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard"      element={<DashboardPage />} />
            <Route path="/documents"      element={<DocumentsPage />} />
            <Route path="/documents/:id"  element={<DocumentDetail />} />
            <Route path="/profile"        element={<ProfilePage />} />

            {/* Manager+ */}
            <Route element={<ProtectedRoute requiredRole="ROLE_MANAGER" />}>
              <Route path="/users" element={<UsersPage />} />
            </Route>

            {/* Admin only */}
            <Route element={<ProtectedRoute requiredRole="ROLE_ADMIN" />}>
              <Route path="/admin" element={<AdminPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  )
}
