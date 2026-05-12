import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { dashboardApi } from '@/api/users'
import { approvalsApi } from '@/api/approvals'
import { auditApi } from '@/api/audit'
import { formatFileSize, formatDateTime } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import clsx from 'clsx'
import {
  HiOutlineShieldCheck, HiOutlineCog, HiOutlineUserGroup,
  HiOutlineChartBar, HiOutlineDocumentText, HiOutlineCheck,
  HiOutlineX, HiOutlineExclamation, HiOutlineKey,
} from 'react-icons/hi'

const SECURITY_CHECKS = [
  { label: 'JWT authentication',    ok: true,  note: 'HMAC-SHA256 access + refresh tokens' },
  { label: 'BCrypt password hashing', ok: true, note: '12 rounds' },
  { label: 'Role-based access control', ok: true, note: '8 roles — ADMIN, HR, ACCOUNT, EMPLOYEE, MANAGER, FINANCE, LEGAL, REVIEWER' },
  { label: 'CORS configured',        ok: true,  note: 'Localhost origins in dev' },
  { label: 'Audit trail active',     ok: true,  note: 'All write actions logged' },
  { label: 'Two-factor authentication', ok: true, note: 'Email-based OTP, opt-in per user' },
  { label: 'HTTPS',                  ok: false, note: 'Configure Nginx + SSL for production' },
  { label: 'Rate limiting',          ok: false, note: 'Add API gateway for production' },
]

export default function AdminPage() {
  const { data: stats }   = useQuery({ queryKey: ['dashboard-stats'],   queryFn: dashboardApi.stats })
  const { data: pending } = useQuery({ queryKey: ['approval-count'],    queryFn: approvalsApi.count })
  const { data: audit }   = useQuery({ queryKey: ['audit-admin-stats'], queryFn: () => auditApi.stats() })

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <div className="flex items-center gap-2 mb-1"><HiOutlineShieldCheck className="w-5 h-5 text-primary-600" /><h1 className="page-title">Admin Dashboard</h1></div>
        <p className="page-subtitle">System overview, health status and quick access to admin tools</p>
      </div>

      {/* Key metrics */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total documents', value: stats?.totalDocuments?.toLocaleString() ?? '—', colour: 'bg-blue-50 text-blue-700', to: '/documents' },
          { label: 'Total users',     value: stats?.totalUsers?.toLocaleString() ?? '—',     colour: 'bg-green-50 text-green-700', to: '/users' },
          { label: 'Pending approvals', value: pending ?? 0,                                colour: pending > 0 ? 'bg-amber-50 text-amber-700' : 'bg-surface-100 text-surface-600', to: '/approvals' },
          { label: 'Storage used',    value: formatFileSize(stats?.storageUsed ?? 0),        colour: 'bg-purple-50 text-purple-700' },
        ].map(s => (
          <div key={s.label} className={s.to ? '' : ''}>
            {s.to ? (
              <Link to={s.to} className="card p-4 flex items-start justify-between hover:shadow-md transition-shadow block">
                <div><p className="text-xs text-surface-500 mb-1">{s.label}</p><p className="text-2xl font-bold text-surface-900">{s.value}</p></div>
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${s.colour}`}><HiOutlineDocumentText className="w-4 h-4" /></div>
              </Link>
            ) : (
              <div className="card p-4 flex items-start justify-between">
                <div><p className="text-xs text-surface-500 mb-1">{s.label}</p><p className="text-2xl font-bold text-surface-900">{s.value}</p></div>
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${s.colour}`}><HiOutlineDocumentText className="w-4 h-4" /></div>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Top active users */}
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm font-semibold text-surface-800">Top users — last 30 days</p>
            <Link to="/audit" className="text-xs text-primary-600 hover:text-primary-800">Full audit →</Link>
          </div>
          {audit ? (
            <div className="space-y-2">
              {(audit.topUsers ?? []).slice(0,5).map((u, i) => (
                <div key={i} className="flex items-center gap-3">
                  <span className="w-5 text-xs text-surface-400 text-right">{i+1}</span>
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-sm text-surface-700">{u.user}</span>
                      <span className="text-xs text-surface-500">{u.count} actions</span>
                    </div>
                    <div className="w-full bg-surface-100 rounded-full h-1.5">
                      <div className="h-1.5 rounded-full bg-primary-500" style={{ width: `${Math.min(100, (u.count / ((audit.topUsers[0]?.count ?? 1))) * 100)}%` }} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : <div className="flex justify-center py-8"><Spinner /></div>}
        </div>

        {/* Security checklist */}
        <div className="card p-5">
          <p className="text-sm font-semibold text-surface-800 mb-4">Security checklist</p>
          <div className="space-y-3">
            {SECURITY_CHECKS.map(s => (
              <div key={s.label} className="flex items-center gap-3">
                <div className={clsx('w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0',
                  s.ok ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600')}>
                  {s.ok ? <HiOutlineCheck className="w-3 h-3" /> : <HiOutlineX className="w-3 h-3" />}
                </div>
                <div>
                  <p className="text-sm text-surface-700">{s.label}</p>
                  <p className="text-xs text-surface-400">{s.note}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Quick admin links */}
      <div className="card p-5">
        <p className="section-title mb-4">Admin tools</p>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { to:'/audit',     icon: HiOutlineShieldCheck, label: 'Audit Trail',     desc: 'All system events' },
            { to:'/approvals', icon: HiOutlineUserGroup,   label: 'Approvals',       desc: `${pending ?? 0} pending` },
            { to:'/users',     icon: HiOutlineUserGroup,   label: 'Manage Users',    desc: `${stats?.totalUsers ?? 0} users` },
            { to:'/admin/roles', icon: HiOutlineKey,       label: 'Roles',           desc: 'Permissions matrix' },
            { to:'/settings',  icon: HiOutlineCog,         label: 'System Settings', desc: 'Configure DocVault' },
          ].map(l => (
            <Link key={l.to} to={l.to} className="flex flex-col items-center gap-2 p-4 rounded-xl border border-surface-200 hover:border-primary-300 hover:bg-primary-50 transition-all text-center">
              <l.icon className="w-6 h-6 text-primary-600" />
              <div><p className="text-sm font-medium text-surface-800">{l.label}</p><p className="text-xs text-surface-400">{l.desc}</p></div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
