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
  HiOutlineShieldCheck, HiOutlineUserGroup, HiOutlineChartBar,
  HiOutlineCog, HiOutlineUpload, HiOutlineFolder, HiOutlineEye,
  HiOutlineBell, HiOutlineClipboardCheck, HiOutlineCash,
  HiOutlineScale, HiOutlineOfficeBuilding,
} from 'react-icons/hi'

const PIE_COLORS = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444']

function StatCard({ icon: Icon, label, value, sub, colour = 'blue', to }) {
  const cls = {
    blue:   'bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
    green:  'bg-green-50 text-green-700 dark:bg-green-900/30 dark:text-green-300',
    purple: 'bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
    amber:  'bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
    red:    'bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300',
    gray:   'bg-surface-100 text-surface-600 dark:bg-gray-800 dark:text-gray-400',
    indigo: 'bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300',
    teal:   'bg-teal-50 text-teal-700 dark:bg-teal-900/30 dark:text-teal-300',
  }
  const content = (
    <div className="stat-card">
      <div>
        <p className="text-xs font-medium text-surface-500 dark:text-gray-400 uppercase tracking-wider mb-2">{label}</p>
        <p className="text-2xl font-bold text-surface-900 dark:text-gray-100">{value ?? '—'}</p>
        {sub && <p className="text-xs text-surface-400 dark:text-gray-500 dark:text-gray-500 mt-1">{sub}</p>}
      </div>
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${cls[colour] ?? cls.blue}`}>
        <Icon className="w-5 h-5" />
      </div>
    </div>
  )
  return to ? <Link to={to} className="hover:shadow-md transition-shadow">{content}</Link> : content
}

function StorageBar({ used, limit, percent }) {
  const color = percent > 85 ? '#ef4444' : percent > 65 ? '#f59e0b' : '#3b82f6'
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200">Storage usage</p>
        <p className="text-sm text-surface-500 dark:text-gray-400">{formatFileSize(used ?? 0)} / {formatFileSize(limit ?? 0)}</p>
      </div>
      <div className="w-full bg-surface-200 dark:bg-gray-700 rounded-full h-2.5">
        <div className="h-2.5 rounded-full transition-all duration-500"
          style={{ width: `${percent}%`, backgroundColor: color }} />
      </div>
      <p className="text-xs text-surface-400 dark:text-gray-500 dark:text-gray-500 mt-1.5">{percent}% used</p>
    </div>
  )
}

function AreaChartCard({ title, data, color, gradId }) {
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200">{title}</p>
        <HiOutlineChartBar className="w-4 h-4 text-surface-400" />
      </div>
      {data ? (
        <ResponsiveContainer width="100%" height={200}>
          <AreaChart data={data}>
            <defs>
              <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor={color} stopOpacity={0.15} />
                <stop offset="95%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis dataKey="date" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} interval={4} />
            <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} width={25} />
            <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }} />
            <Area type="monotone" dataKey="uploads" stroke={color} strokeWidth={2} fill={`url(#${gradId})`} dot={false} />
          </AreaChart>
        </ResponsiveContainer>
      ) : <div className="flex justify-center py-12"><Spinner /></div>}
    </div>
  )
}

function StoragePieCard({ storage }) {
  return (
    <div className="card p-5">
      <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200 mb-4">Storage by type</p>
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
                  <span className="text-xs text-surface-600 dark:text-gray-300 dark:text-gray-400">{s.name}</span>
                </div>
                <span className="text-xs text-surface-500 dark:text-gray-400 dark:text-gray-400">{formatFileSize(s.size)}</span>
              </div>
            ))}
          </div>
        </>
      ) : <div className="text-xs text-surface-400 dark:text-gray-500 text-center py-8">No data yet</div>}
    </div>
  )
}

function RecentDocumentsList({ recent }) {
  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-surface-100">
        <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200">Recent documents</p>
        <Link to="/documents" className="text-xs text-primary-600 hover:text-primary-800 font-medium">View all →</Link>
      </div>
      {recent ? (
        <div className="divide-y divide-surface-100 dark:divide-gray-800">
          {(recent ?? []).slice(0, 8).map(doc => (
            <Link key={doc.id} to={`/documents/${doc.id}`}
              className="flex items-center gap-3 px-5 py-3 hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors">
              <FileIcon mimeType={doc.mimeType} fileName={doc.originalFileName} size="sm" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-surface-800 dark:text-gray-200 truncate">{doc.title}</p>
                <p className="text-xs text-surface-400 dark:text-gray-500">{doc.owner?.firstName} {doc.owner?.lastName} · {timeAgo(doc.createdAt)}</p>
              </div>
              <span className="text-xs text-surface-400 dark:text-gray-500 flex-shrink-0">{formatFileSize(doc.fileSize)}</span>
            </Link>
          ))}
          {(recent ?? []).length === 0 && (
            <div className="px-5 py-8 text-center text-sm text-surface-400 dark:text-gray-500">No documents yet</div>
          )}
        </div>
      ) : <div className="flex justify-center py-8"><Spinner /></div>}
    </div>
  )
}

function QuickActions({ title = 'Quick actions', actions = [] }) {
  return (
    <div className="card p-5">
      <p className="section-title">{title}</p>
      <div className="flex flex-wrap gap-3">
        {actions.map(({ to, label, icon: Icon, primary, state }, idx) =>
          primary
            ? <Link key={idx} to={to} state={state} className="btn-primary btn-sm gap-1.5">{Icon && <Icon className="w-4 h-4" />}{label}</Link>
            : <Link key={idx} to={to} state={state} className="btn-secondary btn-sm gap-1.5">{Icon && <Icon className="w-4 h-4" />}{label}</Link>
        )}
      </div>
    </div>
  )
}

function CategoryCard({ to, state, borderColor, bgColor, iconColor, Icon, title, desc }) {
  return (
    <Link to={to} state={state} className={`card p-5 hover:shadow-md transition-shadow border-l-4 ${borderColor}`}>
      <div className="flex items-center gap-3 mb-2">
        <div className={`w-8 h-8 ${bgColor} rounded-lg flex items-center justify-center`}>
          <Icon className={`w-4 h-4 ${iconColor}`} />
        </div>
        <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200">{title}</p>
      </div>
      <p className="text-xs text-surface-500 dark:text-gray-400 dark:text-gray-400">{desc}</p>
    </Link>
  )
}

function AccessRow({ label, allowed }) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-surface-100 dark:border-gray-800 last:border-0">
      <span className="text-xs text-surface-600 dark:text-gray-300 dark:text-gray-400">{label}</span>
      <span className={`badge text-xs ${allowed ? 'badge-green' : 'badge-gray'}`}>{allowed ? 'Allowed' : 'Restricted'}</span>
    </div>
  )
}

/* ── ADMIN ───────────────────────────────────────────────────────────── */
function AdminDashboard({ stats, recent, trend, storage, pending, storagePercent }) {
  return (
    <>
      {pending > 0 && (
        <Link to="/approvals" className="flex items-center gap-3 p-4 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-xl hover:bg-amber-100 dark:hover:bg-amber-900/30 dark:bg-amber-900/30 transition-colors">
          <HiOutlineUserGroup className="w-5 h-5 text-amber-600 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-amber-800 dark:text-amber-300">{pending} user{pending > 1 ? 's' : ''} waiting for approval</p>
            <p className="text-xs text-amber-600 dark:text-amber-400">Click to review pending registrations</p>
          </div>
          <span className="text-amber-600 text-sm">Review →</span>
        </Link>
      )}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineDocumentText} label="Total documents"   value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineUsers}        label="Active users"      value={stats?.activeUsers?.toLocaleString()}     sub={`${stats?.totalUsers ?? 0} total`}            colour="green"  to="/users" />
        <StatCard icon={HiOutlineServer}       label="Storage used"      value={formatFileSize(stats?.storageUsed ?? 0)}  sub={`${storagePercent}% of ${formatFileSize(stats?.storageLimit ?? 0)}`} colour="purple" />
        <StatCard icon={HiOutlineUserGroup}    label="Pending approvals" value={pending ?? 0}                             colour={pending > 0 ? 'amber' : 'gray'} to="/approvals" />
      </div>
      {/* <StorageBar used={stats?.storageUsed} limit={stats?.storageLimit} percent={storagePercent} /> */}
      {/* <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Upload activity — last 30 days" data={trend} color="#3b82f6" gradId="grad-admin" /></div>
        <StoragePieCard storage={storage} />
      </div> */}
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Admin quick actions" actions={[
        { to: '/documents', state: { upload: true }, label: 'Upload',              icon: HiOutlineUpload,       primary: true },
        { to: '/users',                              label: 'Manage users',         icon: HiOutlineUsers },
        { to: '/audit',                              label: 'Audit trail',          icon: HiOutlineShieldCheck },
        { to: '/settings',                           label: 'System settings',      icon: HiOutlineCog },
        { to: '/admin/roles',                        label: 'Roles & permissions',  icon: HiOutlineShieldCheck },
      ]} />
    </>
  )
}

/* ── HR ──────────────────────────────────────────────────────────────── */
function HrDashboard({ stats, recent, trend, storage, storagePercent }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineUsers}        label="Active employees" value={stats?.activeUsers?.toLocaleString()}     sub={`${stats?.totalUsers ?? 0} total accounts`}  colour="green"  to="/users" />
        <StatCard icon={HiOutlineDocumentText} label="Total documents"  value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineServer}       label="Storage used"     value={formatFileSize(stats?.storageUsed ?? 0)} sub={`${storagePercent}% of limit`}               colour="purple" />
        <StatCard icon={HiOutlineUpload}       label="Uploads (30d)"    value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="teal" />
      </div>
      {/* <StorageBar used={stats?.storageUsed} limit={stats?.storageLimit} percent={storagePercent} />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Document activity — last 30 days" data={trend} color="#10b981" gradId="grad-hr" /></div>
        <StoragePieCard storage={storage} />
      </div> */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/users"              borderColor="border-l-green-400"  bgColor="bg-green-100 dark:bg-green-900/30"  iconColor="text-green-600"  Icon={HiOutlineUsers}           title="Employee Directory"  desc="View and manage all employee accounts" />
        <CategoryCard to="/hr/change-requests" borderColor="border-l-blue-400"   bgColor="bg-blue-100 dark:bg-blue-900/30"   iconColor="text-blue-600"   Icon={HiOutlineClipboardCheck}  title="Change Requests"     desc="Pending profile and role change requests" />
        <CategoryCard to="/documents"          borderColor="border-l-purple-400" bgColor="bg-purple-100 dark:bg-purple-900/30" iconColor="text-purple-600" Icon={HiOutlineDocumentText}    title="HR Documents"        desc="Policies, contracts and HR files" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="HR quick actions" actions={[
        { to: '/users',                              label: 'Employee directory', icon: HiOutlineUsers,          primary: true },
        { to: '/documents', state: { upload: true }, label: 'Upload document',   icon: HiOutlineUpload },
        { to: '/hr/change-requests',                 label: 'Change requests',   icon: HiOutlineClipboardCheck },
        { to: '/documents/search/advanced',          label: 'Advanced search',   icon: HiOutlineFolder },
      ]} />
    </>
  )
}

/* ── ACCOUNT ─────────────────────────────────────────────────────────── */
function AccountDashboard({ stats, recent, trend, storage, storagePercent }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineFolder}       label="My documents"   value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineUpload}       label="Uploads (30d)"  value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="green" />
        <StatCard icon={HiOutlineServer}       label="My storage"     value={formatFileSize(stats?.storageUsed ?? 0)} sub={stats?.storageLimit > 0 ? `${storagePercent}% of quota` : null} colour="purple" />
        <StatCard icon={HiOutlineDocumentText} label="New this month" value={stats?.newThisMonth?.toLocaleString()}   colour="amber" />
      </div>
      {/* <StorageBar used={stats?.storageUsed} limit={stats?.storageLimit} percent={storagePercent} />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="My uploads — last 30 days" data={trend} color="#8b5cf6" gradId="grad-account" /></div>
        <StoragePieCard storage={storage} />
      </div> */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/documents" state={{ upload: true }} borderColor="border-l-blue-400"   bgColor="bg-blue-100 dark:bg-blue-900/30"   iconColor="text-blue-600"   Icon={HiOutlineUpload}       title="Upload Document"  desc="Add a new document to your library" />
        <CategoryCard to="/documents"                          borderColor="border-l-purple-400" bgColor="bg-purple-100 dark:bg-purple-900/30" iconColor="text-purple-600" Icon={HiOutlineFolder}       title="My Documents"     desc="Browse all documents you own" />
        <CategoryCard to="/documents/search/advanced"          borderColor="border-l-amber-400"  bgColor="bg-amber-100 dark:bg-amber-900/30"  iconColor="text-amber-600"  Icon={HiOutlineChartBar}     title="Advanced Search"  desc="Filter by type, date, size and more" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Quick actions" actions={[
        { to: '/documents', state: { upload: true }, label: 'Upload document', icon: HiOutlineUpload,       primary: true },
        { to: '/documents',                          label: 'My documents',    icon: HiOutlineDocumentText },
        { to: '/documents/search/advanced',          label: 'Advanced search', icon: HiOutlineFolder },
        { to: '/notifications',                      label: 'Notifications',   icon: HiOutlineBell },
      ]} />
    </>
  )
}

/* ── EMPLOYEE ────────────────────────────────────────────────────────── */
function EmployeeDashboard({ stats, recent, trend }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineEye}          label="Docs I can access" value={stats?.totalDocuments?.toLocaleString()} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineDocumentText} label="Recent activity"   value={(recent ?? []).length}                   colour="gray" />
        <StatCard icon={HiOutlineUpload}       label="Uploads (30d)"     value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="purple" />
        <StatCard icon={HiOutlineBell}         label="New this month"    value={stats?.newThisMonth?.toLocaleString()}   colour="amber" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Document activity — last 30 days" data={trend} color="#6366f1" gradId="grad-emp" /></div>
        <div className="card p-5">
          <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200 mb-4">Your access</p>
          <div className="space-y-1">
            <AccessRow label="View documents"  allowed={true} />
            <AccessRow label="Download files"  allowed={true} />
            <AccessRow label="Upload documents" allowed={false} />
            <AccessRow label="Manage users"    allowed={false} />
          </div>
          <p className="text-xs text-surface-400 dark:text-gray-500 mt-4">Contact your admin to request additional access.</p>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/documents"                 borderColor="border-l-blue-400"   bgColor="bg-blue-100 dark:bg-blue-900/30"   iconColor="text-blue-600"   Icon={HiOutlineDocumentText} title="Browse Documents"  desc="View all documents shared with you" />
        <CategoryCard to="/documents/search/advanced" borderColor="border-l-purple-400" bgColor="bg-purple-100 dark:bg-purple-900/30" iconColor="text-purple-600" Icon={HiOutlineChartBar}     title="Advanced Search"   desc="Filter and find documents quickly" />
        <CategoryCard to="/notifications"             borderColor="border-l-amber-400"  bgColor="bg-amber-100 dark:bg-amber-900/30"  iconColor="text-amber-600"  Icon={HiOutlineBell}         title="Notifications"     desc="Stay updated on document activity" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Quick actions" actions={[
        { to: '/documents',                 label: 'Browse documents', icon: HiOutlineDocumentText, primary: true },
        { to: '/documents/search/advanced', label: 'Advanced search',  icon: HiOutlineFolder },
        { to: '/notifications',             label: 'Notifications',    icon: HiOutlineBell },
        { to: '/profile',                   label: 'My profile',       icon: HiOutlineUsers },
      ]} />
    </>
  )
}

/* ── MANAGER ─────────────────────────────────────────────────────────── */
function ManagerDashboard({ stats, recent, trend, storage, storagePercent }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineUsers}        label="Team members"    value={stats?.activeUsers?.toLocaleString()}     sub={`${stats?.totalUsers ?? 0} total accounts`}  colour="green"  to="/users" />
        <StatCard icon={HiOutlineDocumentText} label="Total documents" value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineServer}       label="Storage used"    value={formatFileSize(stats?.storageUsed ?? 0)} sub={`${storagePercent}% of limit`}               colour="purple" />
        <StatCard icon={HiOutlineUpload}       label="Uploads (30d)"   value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="indigo" />
      </div>
      {/* <StorageBar used={stats?.storageUsed} limit={stats?.storageLimit} percent={storagePercent} />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Team activity — last 30 days" data={trend} color="#6366f1" gradId="grad-mgr" /></div>
        <StoragePieCard storage={storage} />
      </div> */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/users"              borderColor="border-l-green-400"  bgColor="bg-green-100 dark:bg-green-900/30"  iconColor="text-green-600"  Icon={HiOutlineUsers}          title="Team Directory"    desc="View and manage your team members" />
        <CategoryCard to="/hr/change-requests" borderColor="border-l-indigo-400" bgColor="bg-indigo-100 dark:bg-indigo-900/30" iconColor="text-indigo-600" Icon={HiOutlineClipboardCheck} title="Change Requests"   desc="Review pending team change requests" />
        <CategoryCard to="/documents"          borderColor="border-l-blue-400"   bgColor="bg-blue-100 dark:bg-blue-900/30"   iconColor="text-blue-600"   Icon={HiOutlineOfficeBuilding} title="Department Files"  desc="Documents belonging to your department" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Manager quick actions" actions={[
        { to: '/users',                              label: 'Team directory',  icon: HiOutlineUsers,          primary: true },
        { to: '/documents', state: { upload: true }, label: 'Upload document', icon: HiOutlineUpload },
        { to: '/hr/change-requests',                 label: 'Change requests', icon: HiOutlineClipboardCheck },
        { to: '/documents/search/advanced',          label: 'Advanced search', icon: HiOutlineFolder },
      ]} />
    </>
  )
}

/* ── FINANCE ─────────────────────────────────────────────────────────── */
function FinanceDashboard({ stats, recent, trend, storage, storagePercent }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineDocumentText} label="Total documents" value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"  to="/documents" />
        <StatCard icon={HiOutlineUpload}       label="Uploads (30d)"   value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="green" />
        <StatCard icon={HiOutlineServer}       label="Storage used"    value={formatFileSize(stats?.storageUsed ?? 0)} sub={`${storagePercent}% of quota`} colour="amber" />
        <StatCard icon={HiOutlineCash}         label="New this month"  value={stats?.newThisMonth?.toLocaleString()}   colour="teal" />
      </div>
      {/* <StorageBar used={stats?.storageUsed} limit={stats?.storageLimit} percent={storagePercent} />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Financial document activity — last 30 days" data={trend} color="#10b981" gradId="grad-fin" /></div>
        <StoragePieCard storage={storage} />
      </div> */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/documents" state={{ category: 'invoice' }} borderColor="border-l-green-400" bgColor="bg-green-100 dark:bg-green-900/30" iconColor="text-green-600" Icon={HiOutlineDocumentText} title="Invoices"        desc="View and manage invoice documents" />
        <CategoryCard to="/documents" state={{ category: 'bill'    }} borderColor="border-l-amber-400" bgColor="bg-amber-100 dark:bg-amber-900/30" iconColor="text-amber-600" Icon={HiOutlineCash}         title="Bills & Payments" desc="Bills pending approval and payment records" />
        <CategoryCard to="/documents" state={{ category: 'expense' }} borderColor="border-l-blue-400"  bgColor="bg-blue-100 dark:bg-blue-900/30"  iconColor="text-blue-600"  Icon={HiOutlineChartBar}     title="Expense Reports"  desc="Team expense and reimbursement documents" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Finance quick actions" actions={[
        { to: '/documents', state: { upload: true }, label: 'Upload document', icon: HiOutlineUpload,       primary: true },
        { to: '/documents',                          label: 'All documents',    icon: HiOutlineDocumentText },
        { to: '/documents/search/advanced',          label: 'Advanced search',  icon: HiOutlineFolder },
        { to: '/notifications',                      label: 'Notifications',    icon: HiOutlineBell },
      ]} />
    </>
  )
}

/* ── LEGAL ───────────────────────────────────────────────────────────── */
function LegalDashboard({ stats, recent, trend, storage, storagePercent }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineDocumentText} label="Total documents" value={stats?.totalDocuments?.toLocaleString()} sub={`${stats?.newThisMonth ?? 0} new this month`} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineScale}        label="Uploads (30d)"   value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="purple" />
        <StatCard icon={HiOutlineServer}       label="Storage used"    value={formatFileSize(stats?.storageUsed ?? 0)} sub={`${storagePercent}% of quota`} colour="indigo" />
        <StatCard icon={HiOutlineShieldCheck}  label="New this month"  value={stats?.newThisMonth?.toLocaleString()}   colour="green" />
      </div>
      {/* <StorageBar used={stats?.storageUsed} limit={stats?.storageLimit} percent={storagePercent} />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Legal document activity — last 30 days" data={trend} color="#8b5cf6" gradId="grad-legal" /></div>
        <StoragePieCard storage={storage} />
      </div> */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/documents" state={{ category: 'contract'   }} borderColor="border-l-purple-400" bgColor="bg-purple-100 dark:bg-purple-900/30" iconColor="text-purple-600" Icon={HiOutlineDocumentText} title="Contracts"       desc="Active and archived contracts" />
        <CategoryCard to="/documents" state={{ category: 'policy'     }} borderColor="border-l-indigo-400" bgColor="bg-indigo-100 dark:bg-indigo-900/30" iconColor="text-indigo-600" Icon={HiOutlineShieldCheck}  title="Policy Files"    desc="Organizational policies and procedures" />
        <CategoryCard to="/documents" state={{ category: 'compliance' }} borderColor="border-l-red-400"    bgColor="bg-red-100 dark:bg-red-900/20"    iconColor="text-red-600"    Icon={HiOutlineScale}         title="Compliance Docs" desc="Regulatory and compliance documentation" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Legal quick actions" actions={[
        { to: '/documents', state: { upload: true }, label: 'Upload document', icon: HiOutlineUpload,       primary: true },
        { to: '/documents',                          label: 'All documents',    icon: HiOutlineDocumentText },
        { to: '/documents/search/advanced',          label: 'Advanced search',  icon: HiOutlineFolder },
        { to: '/notifications',                      label: 'Notifications',    icon: HiOutlineBell },
      ]} />
    </>
  )
}

/* ── REVIEWER ────────────────────────────────────────────────────────── */
function ReviewerDashboard({ stats, recent, trend }) {
  return (
    <>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={HiOutlineDocumentText}  label="Docs to review"  value={stats?.totalDocuments?.toLocaleString()} colour="blue"   to="/documents" />
        <StatCard icon={HiOutlineEye}           label="Recent activity"  value={(recent ?? []).length}                   colour="purple" />
        <StatCard icon={HiOutlineClipboardCheck} label="Uploads (30d)"  value={trend ? trend.reduce((a, b) => a + (b.uploads ?? 0), 0) : '—'} colour="green" />
        <StatCard icon={HiOutlineBell}          label="New this month"   value={stats?.newThisMonth?.toLocaleString()}   colour="amber" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2"><AreaChartCard title="Review activity — last 30 days" data={trend} color="#f59e0b" gradId="grad-rev" /></div>
        <div className="card p-5">
          <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 dark:text-gray-200 dark:text-gray-200 mb-4">Reviewer access</p>
          <div className="space-y-1">
            <AccessRow label="View documents"    allowed={true} />
            <AccessRow label="Download files"    allowed={true} />
            <AccessRow label="Approve documents" allowed={true} />
            <AccessRow label="Upload documents"  allowed={false} />
          </div>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <CategoryCard to="/documents"                 borderColor="border-l-blue-400"   bgColor="bg-blue-100 dark:bg-blue-900/30"   iconColor="text-blue-600"   Icon={HiOutlineDocumentText}  title="Shared With Me"   desc="Documents assigned for your review" />
        <CategoryCard to="/documents/search/advanced" borderColor="border-l-purple-400" bgColor="bg-purple-100 dark:bg-purple-900/30" iconColor="text-purple-600" Icon={HiOutlineChartBar}      title="Advanced Search"  desc="Filter by type, date and status" />
        <CategoryCard to="/notifications"             borderColor="border-l-amber-400"  bgColor="bg-amber-100 dark:bg-amber-900/30"  iconColor="text-amber-600"  Icon={HiOutlineBell}          title="Notifications"    desc="Review requests and updates" />
      </div>
      <RecentDocumentsList recent={recent} />
      <QuickActions title="Reviewer quick actions" actions={[
        { to: '/documents',                 label: 'Browse documents', icon: HiOutlineDocumentText, primary: true },
        { to: '/documents/search/advanced', label: 'Advanced search',  icon: HiOutlineFolder },
        { to: '/notifications',             label: 'Notifications',    icon: HiOutlineBell },
        { to: '/profile',                   label: 'My profile',       icon: HiOutlineUsers },
      ]} />
    </>
  )
}

/* ── Page ────────────────────────────────────────────────────────────── */
export default function DashboardPage() {
  const { user, isAdmin, isHr, isAccount, hasManagerRole, hasFinanceRole, hasLegalRole, hasReviewerRole } = useAuth()

  const { data: stats }   = useQuery({ queryKey: ['dashboard-stats'],   queryFn: dashboardApi.stats,           staleTime: 30_000 })
  const { data: recent }  = useQuery({ queryKey: ['dashboard-recent'],  queryFn: dashboardApi.recentDocs,       staleTime: 30_000 })
  const { data: trend }   = useQuery({ queryKey: ['dashboard-trend'],   queryFn: dashboardApi.uploadTrend,      staleTime: 60_000 })
  const { data: storage } = useQuery({ queryKey: ['dashboard-storage'], queryFn: dashboardApi.storageBreakdown, staleTime: 60_000 })
  const { data: pending } = useQuery({ queryKey: ['approval-count'],    queryFn: approvalsApi.count, enabled: isAdmin(), staleTime: 60_000 })

  const storagePercent = stats && stats.storageLimit > 0
    ? Math.min(100, Math.round((stats.storageUsed / stats.storageLimit) * 100))
    : 0

  const subtitle =
    isAdmin()           ? 'System overview and admin tools'
    : isHr()            ? 'People operations and document activity'
    : isAccount()       ? 'Your documents, uploads, and storage'
    : hasManagerRole()  ? 'Team documents and department management'
    : hasFinanceRole()  ? 'Financial documents and expense reports'
    : hasLegalRole()    ? 'Contracts, policies, and compliance'
    : hasReviewerRole() ? 'Documents assigned for your review'
    : 'Documents shared with you'

  let body
  if      (isAdmin())          body = <AdminDashboard    stats={stats} recent={recent} trend={trend} storage={storage} pending={pending} storagePercent={storagePercent} />
  else if (isHr())             body = <HrDashboard       stats={stats} recent={recent} trend={trend} storage={storage} storagePercent={storagePercent} />
  else if (isAccount())        body = <AccountDashboard  stats={stats} recent={recent} trend={trend} storage={storage} storagePercent={storagePercent} />
  else if (hasManagerRole())   body = <ManagerDashboard  stats={stats} recent={recent} trend={trend} storage={storage} storagePercent={storagePercent} />
  else if (hasFinanceRole())   body = <FinanceDashboard  stats={stats} recent={recent} trend={trend} storage={storage} storagePercent={storagePercent} />
  else if (hasLegalRole())     body = <LegalDashboard    stats={stats} recent={recent} trend={trend} storage={storage} storagePercent={storagePercent} />
  else if (hasReviewerRole())  body = <ReviewerDashboard stats={stats} recent={recent} trend={trend} />
  else                         body = <EmployeeDashboard stats={stats} recent={recent} trend={trend} />

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <h1 className="page-title">Welcome back, {user?.firstName} 👋</h1>
        <p className="page-subtitle">{subtitle}</p>
      </div>
      <div className="flex flex-wrap gap-2">
        {user?.roles?.map(r => (
          <span key={r} className="badge badge-blue capitalize text-xs">
            {r.replace('ROLE_', '').toLowerCase()}
          </span>
        ))}
      </div>
      {body}
    </div>
  )
}