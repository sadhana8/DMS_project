import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '@/api/users'
import { formatFileSize, formatDateTime } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import {
  HiOutlineServer, HiOutlineShieldCheck, HiOutlineDatabase,
  HiOutlineChartBar, HiOutlineDocumentText, HiOutlineUsers,
} from 'react-icons/hi'

export default function AdminPage() {
  const { data: stats, isLoading } = useQuery({ queryKey: ['dashboard-stats'], queryFn: dashboardApi.stats })

  if (isLoading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>

  const systemCards = [
    {
      icon: HiOutlineDatabase,
      label: 'Total storage used',
      value: formatFileSize(stats?.storageUsed),
      sub:   `of ${formatFileSize(stats?.storageLimit)} limit`,
      color: 'bg-blue-50 text-blue-600',
    },
    {
      icon: HiOutlineDocumentText,
      label: 'Total documents',
      value: stats?.totalDocuments ?? '—',
      sub:   `${stats?.archivedDocuments ?? 0} archived`,
      color: 'bg-green-50 text-green-600',
    },
    {
      icon: HiOutlineUsers,
      label: 'Registered users',
      value: stats?.totalUsers ?? '—',
      sub:   `${stats?.activeUsers ?? 0} active`,
      color: 'bg-purple-50 text-purple-600',
    },
    {
      icon: HiOutlineChartBar,
      label: 'Uploads this month',
      value: stats?.newThisMonth ?? '—',
      sub:   'new documents',
      color: 'bg-yellow-50 text-yellow-600',
    },
  ]

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <h1 className="page-title">System Administration</h1>
        <p className="page-subtitle">Monitor system health and manage settings</p>
      </div>

      {/* System stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {systemCards.map(({ icon: Icon, label, value, sub, color }) => (
          <div key={label} className="card p-5">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center mb-3 ${color}`}>
              <Icon className="w-5 h-5" />
            </div>
            <p className="text-2xl font-bold text-surface-900">{value}</p>
            <p className="text-sm text-surface-600 mt-0.5">{label}</p>
            <p className="text-xs text-surface-400 mt-0.5">{sub}</p>
          </div>
        ))}
      </div>

      {/* Storage bar */}
      <div className="card p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-base font-semibold text-surface-800">Storage capacity</h2>
          <span className="text-sm text-surface-500">
            {formatFileSize(stats?.storageUsed)} / {formatFileSize(stats?.storageLimit)}
          </span>
        </div>
        <div className="h-3 rounded-full bg-surface-100 overflow-hidden">
          <div
            className="h-full rounded-full bg-primary-500 transition-all duration-700"
            style={{ width: `${Math.min(100, ((stats?.storageUsed ?? 0) / (stats?.storageLimit ?? 1)) * 100)}%` }}
          />
        </div>
        <p className="text-xs text-surface-400 mt-2">
          {Math.round(((stats?.storageUsed ?? 0) / (stats?.storageLimit ?? 1)) * 100)}% used
        </p>
      </div>

      {/* System info */}
      <div className="card">
        <div className="flex items-center gap-3 px-5 py-4 border-b border-surface-100">
          <HiOutlineServer className="w-5 h-5 text-surface-400" />
          <h2 className="text-base font-semibold text-surface-800">System information</h2>
        </div>
        <dl className="divide-y divide-surface-100">
          {[
            { label: 'Application',    value: 'DocVault v1.0.0' },
            { label: 'Backend',        value: 'Spring Boot 3.2 · Java 17' },
            { label: 'Database',       value: 'PostgreSQL' },
            { label: 'Storage driver', value: 'Local filesystem (configurable to S3)' },
            { label: 'Auth',           value: 'JWT (access + refresh tokens)' },
            { label: 'Email provider', value: 'SMTP via JavaMailSender' },
          ].map(({ label, value }) => (
            <div key={label} className="flex items-center px-5 py-3">
              <dt className="text-sm text-surface-500 w-44 flex-shrink-0">{label}</dt>
              <dd className="text-sm text-surface-800 font-medium">{value}</dd>
            </div>
          ))}
        </dl>
      </div>

      {/* Security panel */}
      <div className="card p-5">
        <div className="flex items-center gap-3 mb-4">
          <HiOutlineShieldCheck className="w-5 h-5 text-green-600" />
          <h2 className="text-base font-semibold text-surface-800">Security status</h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {[
            { label: 'JWT auth',      ok: true },
            { label: 'Password BCrypt', ok: true },
            { label: 'RBAC enabled',  ok: true },
            { label: 'CORS configured', ok: true },
            { label: 'HTTPS',         ok: false, note: 'Configure in production' },
            { label: 'Rate limiting', ok: false, note: 'Add API gateway' },
          ].map(({ label, ok, note }) => (
            <div key={label} className={`flex items-center gap-2.5 p-3 rounded-xl ${ok ? 'bg-green-50' : 'bg-yellow-50'}`}>
              <div className={`w-2 h-2 rounded-full flex-shrink-0 ${ok ? 'bg-green-500' : 'bg-yellow-500'}`} />
              <div>
                <p className={`text-sm font-medium ${ok ? 'text-green-800' : 'text-yellow-800'}`}>{label}</p>
                {note && <p className="text-xs text-yellow-600">{note}</p>}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
