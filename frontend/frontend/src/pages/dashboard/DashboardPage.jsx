import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { dashboardApi } from '@/api/users'
import { approvalsApi } from '@/api/approvals'
import { formatFileSize, timeAgo } from '@/utils/helpers'
import FileIcon from '@/components/common/FileIcon'
import Spinner from '@/components/common/Spinner'
import {
  AreaChart, Area, PieChart, Pie, Cell,
  Tooltip, ResponsiveContainer, XAxis, YAxis,
} from 'recharts'
import {
  HiOutlineDocumentText, HiOutlineUsers, HiOutlineServer,
  HiOutlineDownload, HiOutlineShieldCheck, HiOutlineUserGroup,
  HiOutlineChartBar, HiOutlineCog, HiOutlineUpload,
  HiOutlineFolder, HiOutlineEye,
} from 'react-icons/hi'

const PIE_COLORS = ['#3b82f6','#8b5cf6','#10b981','#f59e0b','#ef4444']

function StatCard({ icon: Icon, label, value, sub, colour = 'blue', to }) {
  const cls = {
    blue:   'bg-blue-50 text-blue-700',
    green:  'bg-green-50 text-green-700',
    purple: 'bg-purple-50 text-purple-700',
    amber:  'bg-amber-50 text-amber-700',
    red:    'bg-red-50 text-red-700',
    gray:   'bg-surface-100 text-surface-600',
  }
  const content = (
    <div className="stat-card">
      <div>
        <p className="text-xs font-medium text-surface-500 uppercase tracking-wider mb-2">{label}</p>
        <p className="text-2xl font-bold text-surface-900">{value ?? '—'}</p>
        {sub && <p className="text-xs text-surface-400 mt-1">{sub}</p>}
      </div>
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${cls[colour]}`}>
        <Icon className="w-5 h-5" />
      </div>
    </div>
  )
  return to ? <Link to={to} className="hover:shadow-md transition-shadow">{content}</Link> : content
}

function RecentDocumentsList({ recent }) {
  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-surface-100">
        <p className="text-sm font-semibold text-surface-800">Recent documents</p>
        <Link to="/documents" className="text-xs text-primary-600 hover:text-primary-800 font-medium">View all →</Link>
      </div>
      {recent ? (
        <div className="divide-y divide-surface-100">
          {(recent ?? []).slice(0, 8).map(doc => (
            <Link key={doc.id} to={`/documents/${doc.id}`}
              className="flex items-center gap-3 px-5 py-3 hover:bg-surface-50 transition-colors">
              <FileIcon mimeType={doc.mimeType} fileName={doc.originalFileName} size="sm" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-surface-800 truncate">{doc.title}</p>
                <p className="text-xs text-surface-400">{doc.owner?.firstName} {doc.owner?.lastName} · {timeAgo(doc.createdAt)}</p>
              </div>
              <span className="text-xs text-surface-400 flex-shrink-0">{formatFileSize(doc.fileSize)}</span>
            </Link>
          ))}
          {(recent ?? []).length === 0 && (
            <div className="px-5 py-8 text-center text-sm text-surface-400">No documents yet</div>
          )}
        </div>
      ) : <div className="flex justify-center py-8"><Spinner /></div>}
    </div>
  )
}

/* ── ADMIN dashboard ──────────────────────────────────────────────────
   System-wide totals, approvals alert, both charts, admin actions.   */
function AdminDashboard({ stats, recent, trend, storage, pending, storagePercent }) {
  return (
    <>
      {/* Pending approvals alert */}
      {pending > 0 && (
        <Link to="/approvals" className="flex items-center gap-3 p-4 bg-amber-50 border border-amber-200 rounded-xl hover:bg-amber-100 transition-colors">
          <HiOutlineUserGroup className="w-5 h-5 text-amber-600 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-amber-800">{pending} user{pending > 1 ? 's' : ''} waiting for approval</p>
            <p className="text-xs text-amber-600">Click to review pending registrations</p>
          </div>
          <span className="text-amber-600 text-sm">Review →</span>
        </Link>
      )}

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineDocumentText} label="Total documents"   value={stats?.totalDocuments?.toLocaleString()}                   sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineUsers}        label="Active users"      value={stats?.activeUsers?.toLocaleString()}                       sub={`${stats?.totalUsers ?? 0} total`}                colour="green"  to="/users" />
        <StatCard icon={HiOutlineServer}       label="Storage used"      value={formatFileSize(stats?.storageUsed ?? 0)}                    sub={`${storagePercent}% of ${formatFileSize(stats?.storageLimit ?? 0)}`} colour="purple" />
        <StatCard icon={HiOutlineUserGroup}    label="Pending approvals" value={pending ?? 0}                                               colour={pending > 0 ? 'amber' : 'gray'} to="/approvals" />
      </div>

      {/* Storage bar */}
      <div className="card p-5">
        <div className="flex items-center justify-between mb-3">
          <p className="text-sm font-semibold text-surface-800">System storage</p>
          <p className="text-sm text-surface-500">{formatFileSize(stats?.storageUsed ?? 0)} / {formatFileSize(stats?.storageLimit ?? 0)}</p>
        </div>
        <div className="w-full bg-surface-200 rounded-full h-2.5">
          <div className="h-2.5 rounded-full transition-all duration-500"
            style={{ width: `${storagePercent}%`, backgroundColor: storagePercent > 85 ? '#ef4444' : storagePercent > 65 ? '#f59e0b' : '#3b82f6' }} />
        </div>
        <p className="text-xs text-surface-400 mt-1.5">{storagePercent}% used</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Upload trend */}
        <div className="card p-5 lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm font-semibold text-surface-800">Upload activity — last 30 days</p>
            <HiOutlineChartBar className="w-4 h-4 text-surface-400" />
          </div>
          {trend ? (
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={trend}>
                <defs>
                  <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="date" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} interval={4} />
                <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} width={25} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }} />
                <Area type="monotone" dataKey="uploads" stroke="#3b82f6" strokeWidth={2} fill="url(#grad)" dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          ) : <div className="flex justify-center py-12"><Spinner /></div>}
        </div>

        {/* Storage breakdown */}
        <div className="card p-5">
          <p className="text-sm font-semibold text-surface-800 mb-4">Storage by type</p>
          {storage && storage.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={140}>
                <PieChart>
                  <Pie data={storage} cx="50%" cy="50%" innerRadius={40} outerRadius={65} paddingAngle={3} dataKey="size">
                    {storage.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                  </Pie>
                  <Tooltip formatter={v => formatFileSize(v)} contentStyle={{ fontSize: 11, borderRadius: 8 }} />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-1.5 mt-2">
                {storage.map((s, i) => (
                  <div key={s.name} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: PIE_COLORS[i % PIE_COLORS.length] }} />
                      <span className="text-xs text-surface-600">{s.name}</span>
                    </div>
                    <span className="text-xs text-surface-500">{formatFileSize(s.size)}</span>
                  </div>
                ))}
              </div>
            </>
          ) : <div className="text-xs text-surface-400 text-center py-8">No data yet</div>}
        </div>
      </div>

      <RecentDocumentsList recent={recent} />

      <div className="card p-5">
        <p className="section-title">Admin quick actions</p>
        <div className="flex flex-wrap gap-3">
          <Link to="/users"     className="btn-secondary btn-sm gap-1.5"><HiOutlineUsers className="w-4 h-4" /> Manage users</Link>
          <Link to="/audit"     className="btn-secondary btn-sm gap-1.5"><HiOutlineShieldCheck className="w-4 h-4" /> Audit trail</Link>
          <Link to="/settings"  className="btn-secondary btn-sm gap-1.5"><HiOutlineCog className="w-4 h-4" /> System settings</Link>
          <Link to="/documents" state={{ upload: true }} className="btn-primary btn-sm gap-1.5"><HiOutlineUpload className="w-4 h-4" /> Upload</Link>
        </div>
      </div>
    </>
  )
}

/* ── HR dashboard ─────────────────────────────────────────────────────
   People-focused: user counts, employee directory link.              */
function HrDashboard({ stats, recent, trend }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard icon={HiOutlineUsers}        label="Active employees" value={stats?.activeUsers?.toLocaleString()} sub={`${stats?.totalUsers ?? 0} total accounts`} colour="green" to="/users" />
        <StatCard icon={HiOutlineDocumentText} label="Total documents"  value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue" to="/documents" />
        <StatCard icon={HiOutlineUpload}       label="Uploads (30d)"    value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="purple" />
      </div>

      <div className="card p-5">
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm font-semibold text-surface-800">Activity — last 30 days</p>
          <HiOutlineChartBar className="w-4 h-4 text-surface-400" />
        </div>
        {trend ? (
          <ResponsiveContainer width="100%" height={180}>
            <AreaChart data={trend}>
              <defs>
                <linearGradient id="hrgrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#10b981" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="date" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} interval={4} />
              <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} width={25} />
              <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }} />
              <Area type="monotone" dataKey="uploads" stroke="#10b981" strokeWidth={2} fill="url(#hrgrad)" dot={false} />
            </AreaChart>
          </ResponsiveContainer>
        ) : <div className="flex justify-center py-10"><Spinner /></div>}
      </div>

      <RecentDocumentsList recent={recent} />

      <div className="card p-5">
        <p className="section-title">HR quick actions</p>
        <div className="flex flex-wrap gap-3">
          <Link to="/users"     className="btn-primary  btn-sm gap-1.5"><HiOutlineUsers className="w-4 h-4" /> Employee directory</Link>
          <Link to="/documents" state={{ upload: true }} className="btn-secondary btn-sm gap-1.5"><HiOutlineUpload className="w-4 h-4" /> Upload</Link>
        </div>
      </div>
    </>
  )
}

/* ── ACCOUNT dashboard ────────────────────────────────────────────────
   "What I own and uploaded" view.                                    */
function AccountDashboard({ stats, recent, trend, storagePercent }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard icon={HiOutlineFolder}       label="My documents"     value={stats?.totalDocuments?.toLocaleString()} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineUpload}       label="New this month"   value={stats?.newThisMonth?.toLocaleString()}                       colour="green" />
        <StatCard icon={HiOutlineServer}       label="My storage"       value={formatFileSize(stats?.storageUsed ?? 0)} sub={stats?.storageLimit > 0 ? `${storagePercent}% of quota` : null} colour="purple" />
      </div>

      <div className="card p-5">
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm font-semibold text-surface-800">My uploads — last 30 days</p>
          <HiOutlineChartBar className="w-4 h-4 text-surface-400" />
        </div>
        {trend ? (
          <ResponsiveContainer width="100%" height={180}>
            <AreaChart data={trend}>
              <defs>
                <linearGradient id="acgrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#8b5cf6" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="date" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} interval={4} />
              <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} width={25} />
              <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }} />
              <Area type="monotone" dataKey="uploads" stroke="#8b5cf6" strokeWidth={2} fill="url(#acgrad)" dot={false} />
            </AreaChart>
          </ResponsiveContainer>
        ) : <div className="flex justify-center py-10"><Spinner /></div>}
      </div>

      <RecentDocumentsList recent={recent} />

      <div className="card p-5">
        <p className="section-title">Quick actions</p>
        <div className="flex flex-wrap gap-3">
          <Link to="/documents" state={{ upload: true }} className="btn-primary btn-sm gap-1.5"><HiOutlineUpload className="w-4 h-4" /> Upload document</Link>
          <Link to="/documents" className="btn-secondary btn-sm gap-1.5"><HiOutlineDocumentText className="w-4 h-4" /> My documents</Link>
        </div>
      </div>
    </>
  )
}

/* ── EMPLOYEE dashboard ───────────────────────────────────────────────
   Minimal "what I can read" view. No storage/upload widgets.         */
function EmployeeDashboard({ stats, recent }) {
  return (
    <>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <StatCard icon={HiOutlineEye}          label="Documents I can access" value={stats?.totalDocuments?.toLocaleString()} colour="blue"  to="/documents" />
        <StatCard icon={HiOutlineDocumentText} label="Recent activity"        value={(recent ?? []).length}                   colour="gray" />
      </div>

      <RecentDocumentsList recent={recent} />

      <div className="card p-5">
        <p className="section-title">Quick actions</p>
        <div className="flex flex-wrap gap-3">
          <Link to="/documents"     className="btn-primary btn-sm gap-1.5"><HiOutlineDocumentText className="w-4 h-4" /> Browse documents</Link>
          <Link to="/notifications" className="btn-secondary btn-sm gap-1.5">My notifications</Link>
          <Link to="/profile"       className="btn-secondary btn-sm gap-1.5">My profile</Link>
        </div>
      </div>

      <div className="card p-5 bg-blue-50 border-blue-100">
        <p className="text-sm font-semibold text-surface-800 mb-1">Welcome to DocVault</p>
        <p className="text-xs text-surface-600">As an employee, you can view and download documents that have been shared with you. To upload your own documents, ask your administrator to grant you the Account role.</p>
      </div>
    </>
  )
}

export default function DashboardPage() {
  const { user, isAdmin, isHr, isAccount } = useAuth()

  const { data: stats }   = useQuery({ queryKey: ['dashboard-stats'],   queryFn: dashboardApi.stats,            staleTime: 30_000 })
  const { data: recent }  = useQuery({ queryKey: ['dashboard-recent'],  queryFn: dashboardApi.recentDocs,        staleTime: 30_000 })
  const { data: trend }   = useQuery({ queryKey: ['dashboard-trend'],   queryFn: dashboardApi.uploadTrend,       staleTime: 60_000 })
  const { data: storage } = useQuery({ queryKey: ['dashboard-storage'], queryFn: dashboardApi.storageBreakdown,  staleTime: 60_000 })
  const { data: pending } = useQuery({ queryKey: ['approval-count'],    queryFn: approvalsApi.count, enabled: isAdmin(), staleTime: 60_000 })

  const storagePercent = stats && stats.storageLimit > 0
    ? Math.min(100, Math.round((stats.storageUsed / stats.storageLimit) * 100))
    : 0

  // Pick the right view. Note isHr() and isAccount() include higher roles
  // (because of the helper ladder) so we check most-privileged first.
  let body
  if (isAdmin())        body = <AdminDashboard    stats={stats} recent={recent} trend={trend} storage={storage} pending={pending} storagePercent={storagePercent} />
  else if (isHr())      body = <HrDashboard       stats={stats} recent={recent} trend={trend} />
  else if (isAccount()) body = <AccountDashboard  stats={stats} recent={recent} trend={trend} storagePercent={storagePercent} />
  else                  body = <EmployeeDashboard stats={stats} recent={recent} />

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <h1 className="page-title">Welcome back, {user?.firstName} 👋</h1>
        <p className="page-subtitle">
          {isAdmin() && "System overview and admin tools"}
          {!isAdmin() && isHr() && "People and document activity"}
          {!isAdmin() && !isHr() && isAccount() && "Your documents and uploads"}
          {!isAdmin() && !isHr() && !isAccount() && "Documents shared with you"}
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {user?.roles?.map(r => (
          <span key={r} className="badge badge-blue capitalize text-xs">
            {r.replace('ROLE_','').toLowerCase()}
          </span>
        ))}
      </div>

      {body}
    </div>
  )
}
