import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider }  from '@/context/AuthContext'
import ProtectedRoute    from '@/routes/ProtectedRoute'
import AppLayout         from '@/components/layout/AppLayout'

// Auth pages
import LoginPage          from '@/pages/auth/LoginPage'
import RegisterPage       from '@/pages/auth/RegisterPage'
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage'
import ResetPasswordPage  from '@/pages/auth/ResetPasswordPage'
import FirstLoginPasswordChange from '@/pages/auth/FirstLoginPasswordChange'

// App pages
import DashboardPage  from '@/pages/dashboard/DashboardPage'
import ProfilePage    from '@/pages/dashboard/ProfilePage'
import DocumentsPage  from '@/pages/documents/DocumentsPage'
import DocumentDetail from '@/pages/documents/DocumentDetail'
import UsersPage      from '@/pages/users/UsersPage'

// New pages
import AuditTrailPage            from '@/pages/audit/AuditTrailPage'
import ApprovalsPage             from '@/pages/approvals/ApprovalsPage'
import ChangeRequestsPage        from '@/pages/admin/ChangeRequestsPage'
import NotificationsPage         from '@/pages/notifications/NotificationsPage'
import NotificationSettingsPage  from '@/pages/notifications/NotificationSettingsPage'
import SettingsPage              from '@/pages/settings/SettingsPage'
import AdminPage                 from '@/pages/admin/AdminPage'
import RolesPage                 from '@/pages/admin/RolesPage'
import AdvancedSearchPage        from '@/pages/documents/AdvancedSearchPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* ── Public ────────────────────────────────────────── */}
        <Route path="/login"           element={<LoginPage />} />
        <Route path="/register"        element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password"  element={<ResetPasswordPage />} />
        <Route path="/first-login-password-change" element={<FirstLoginPasswordChange />} />

        {/* ── Protected (any authenticated user) ────────────── */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/"          element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/profile"   element={<ProfilePage />} />

            <Route path="/documents"     element={<DocumentsPage />} />
            <Route path="/documents/search/advanced" element={<AdvancedSearchPage />} />
            <Route path="/documents/:id" element={<DocumentDetail />} />

            {/* Notifications — any user */}
            <Route path="/notifications"          element={<NotificationsPage />} />
            <Route path="/notifications/settings" element={<NotificationSettingsPage />} />

            {/* HR + Admin + Manager */}
            <Route element={<ProtectedRoute requiredRole={['ROLE_HR', 'ROLE_MANAGER']} />}>
              <Route path="/users"           element={<UsersPage />} />
              <Route path="/hr/change-requests" element={<ChangeRequestsPage />} />
            </Route>

            {/* Admin only */}
            <Route element={<ProtectedRoute requiredRole="ROLE_ADMIN" />}>
              <Route path="/admin"     element={<AdminPage />} />
              <Route path="/admin/roles" element={<RolesPage />} />
              <Route path="/audit"     element={<AuditTrailPage />} />
              <Route path="/approvals" element={<ApprovalsPage />} />
              <Route path="/settings"  element={<SettingsPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  )
}