import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { dashboardApi } from '@/api/users'
import { useAuth } from '@/context/AuthContext'
import { formatFileSize, timeAgo, getFileIcon } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import FileIcon from '@/components/common/FileIcon'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts'
import {
  HiOutlineDocumentText, HiOutlineUsers, HiOutlineDatabase,
  HiOutlineDownload, HiOutlineUpload, HiOutlineClock,
} from 'react-icons/hi'

const COLORS = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#ec4899']

function StatCard({ icon: Icon, label, value, sub, color = 'blue' }) {
  const colors = {
    blue:   'bg-blue-50 text-blue-600',
    green:  'bg-green-50 text-green-600',
    yellow: 'bg-yellow-50 text-yellow-600',
    purple: 'bg-purple-50 text-purple-600',
  }
  return (
    <div className="stat-card">
      <div>
        <p className="text-sm text-surface-500 mb-1">{label}</p>
        <p className="text-2xl font-bold text-surface-900">{value ?? '—'}</p>
        {sub && <p className="text-xs text-surface-400 mt-1">{sub}</p>}
      </div>
      <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${colors[color]}`}>
        <Icon className="w-5 h-5" />
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { user, isAdmin } = useAuth()

  const { data: stats,    isLoading: ls } = useQuery({ queryKey: ['dashboard-stats'],   queryFn: dashboardApi.stats })
  const { data: recent,   isLoading: lr } = useQuery({ queryKey: ['recent-docs'],        queryFn: dashboardApi.recentDocs })
  const { data: trend,    isLoading: lt } = useQuery({ queryKey: ['upload-trend'],       queryFn: dashboardApi.uploadTrend })
  const { data: storage,  isLoading: lk } = useQuery({ queryKey: ['storage-breakdown'], queryFn: dashboardApi.storageBreakdown })

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Greeting */}
      <div>
        <h1 className="page-title">Good {getGreeting()}, {user?.firstName} 👋</h1>
        <p className="page-subtitle">Here's what's happening in your workspace today.</p>
      </div>

      {/* Stats */}
      {ls ? (
        <div className="flex justify-center py-8"><Spinner size="lg" /></div>
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard icon={HiOutlineDocumentText} label="Total Documents" value={stats?.totalDocuments}   sub={`${stats?.newThisMonth ?? 0} this month`} color="blue" />
          <StatCard icon={HiOutlineDatabase}     label="Storage Used"    value={formatFileSize(stats?.storageUsed)} sub={`of ${formatFileSize(stats?.storageLimit)} limit`} color="green" />
          <StatCard icon={HiOutlineUsers}        label="Team Members"    value={stats?.totalUsers}      sub={`${stats?.activeUsers ?? 0} active`}      color="purple" />
          <StatCard icon={HiOutlineDownload}     label="Downloads Today" value={stats?.downloadsToday}  sub="across all documents"                      color="yellow" />
        </div>
      )}

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Upload trend */}
        <div className="card p-5 lg:col-span-2">
          <h2 className="text-base font-semibold text-surface-800 mb-4">Upload Activity</h2>
          {lt ? <div className="flex justify-center h-40"><Spinner /></div> : (
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={trend ?? []} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#94a3b8' }} tickLine={false} axisLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ borderRadius: '10px', border: '1px solid #e2e8f0', fontSize: 12 }} />
                <Area type="monotone" dataKey="uploads" stroke="#3b82f6" fill="url(#grad)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Storage breakdown */}
        <div className="card p-5">
          <h2 className="text-base font-semibold text-surface-800 mb-4">Storage by Type</h2>
          {lk ? <div className="flex justify-center h-40"><Spinner /></div> : (
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={storage ?? []} cx="50%" cy="50%" innerRadius={55} outerRadius={80} paddingAngle={3} dataKey="size">
                  {(storage ?? []).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={(v) => formatFileSize(v)} contentStyle={{ borderRadius: '10px', border: '1px solid #e2e8f0', fontSize: 12 }} />
                <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Recent documents */}
      <div className="card">
        <div className="flex items-center justify-between px-5 py-4 border-b border-surface-100">
          <h2 className="text-base font-semibold text-surface-800">Recent Documents</h2>
          <Link to="/documents" className="text-sm text-primary-600 font-medium hover:text-primary-700">View all →</Link>
        </div>
        {lr ? (
          <div className="flex justify-center py-8"><Spinner /></div>
        ) : !recent?.length ? (
          <div className="text-center py-10 text-surface-400 text-sm">No documents yet</div>
        ) : (
          <div className="divide-y divide-surface-100">
            {recent.slice(0, 8).map((doc) => (
              <Link key={doc.id} to={`/documents/${doc.id}`}
                className="flex items-center gap-4 px-5 py-3.5 hover:bg-surface-50 transition-colors">
                <FileIcon mimeType={doc.mimeType} fileName={doc.originalFileName} size="sm" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-surface-800 truncate">{doc.title}</p>
                  <p className="text-xs text-surface-400">{formatFileSize(doc.fileSize)} · {doc.owner?.firstName} {doc.owner?.lastName}</p>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-surface-400 flex-shrink-0">
                  <HiOutlineClock className="w-3.5 h-3.5" />
                  {timeAgo(doc.createdAt)}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 18) return 'afternoon'
  return 'evening'
}
