import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { auditApi } from '@/api/audit'
import { formatDateTime } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import { Pagination } from '@/components/common/index'
import clsx from 'clsx'
import {
  HiOutlineSearch, HiOutlineFilter, HiOutlineRefresh,
  HiOutlineShieldCheck, HiOutlineChartBar, HiOutlineDownload,
  HiOutlineX,
} from 'react-icons/hi'

const SEVERITY = {
  INFO:     'badge-blue',
  WARNING:  'badge-yellow',
  CRITICAL: 'badge-red',
}

const ACTION_GROUPS = {
  Auth:     ['LOGIN','LOGOUT','REGISTER','PASSWORD_CHANGE','PASSWORD_RESET'],
  Document: ['DOCUMENT_CREATE','DOCUMENT_UPDATE','DOCUMENT_DELETE','DOCUMENT_DOWNLOAD',
             'DOCUMENT_SHARE','DOCUMENT_DEPRECATE','DOCUMENT_RESTORE','VERSION_UPLOAD'],
  User:     ['USER_CREATE','USER_UPDATE','USER_DEPRECATE','USER_RESTORE',
             'USER_ACTIVATE','USER_DEACTIVATE','ROLE_CHANGE'],
  Admin:    ['SETTINGS_CHANGE','SYSTEM'],
}

const ENTITY_TYPES = ['USER','DOCUMENT','ROLE','SETTINGS','SYSTEM']

export default function AuditTrailPage() {
  const [page,       setPage]       = useState(1)
  const [showStats,  setShowStats]  = useState(false)
  const [filters,    setFilters]    = useState({ user:'', action:'', entityType:'', from:'', to:'' })
  const [applied,    setApplied]    = useState({})

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['audit', page, applied],
    queryFn:  () => auditApi.search({
      user:       applied.user       || undefined,
      action:     applied.action     || undefined,
      entityType: applied.entityType || undefined,
      // Send local timestamp without timezone — backend uses LocalDateTime
      // which rejects ISO strings ending in 'Z' (UTC marker).
      from:       applied.from || undefined,
      to:         applied.to   || undefined,
      page: page - 1, size: 20,
    }),
  })

  const { data: stats } = useQuery({
    queryKey: ['audit-stats'],
    queryFn:  () => auditApi.stats(),
    enabled:  showStats,
  })

  const logs       = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const total      = data?.totalElements ?? 0
  const hasFilters = Object.values(applied).some(Boolean)

  const applyFilters = () => { setApplied({ ...filters }); setPage(1) }
  const resetFilters = () => {
    const empty = { user:'', action:'', entityType:'', from:'', to:'' }
    setFilters(empty); setApplied({}); setPage(1)
  }

  const exportCsv = () => {
    const rows = [
      ['Timestamp','User','Action','Entity','Description','IP','Severity'],
      ...logs.map(l => [l.createdAt, l.performedBy, l.action,
        `${l.entityType ?? ''}${l.entityId ? ' #'+l.entityId : ''}`,
        `"${(l.description ?? '').replace(/"/g,'""')}"`,
        l.ipAddress ?? '', l.severity]),
    ]
    const csv  = rows.map(r => r.join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv' })
    const a    = document.createElement('a')
    a.href     = URL.createObjectURL(blob)
    a.download = `audit-${new Date().toISOString().slice(0,10)}.csv`
    a.click()
  }

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="page-header mb-5">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-xl">
            <HiOutlineShieldCheck className="w-5 h-5 text-purple-700 dark:text-purple-400" />
          </div>
          <div>
            <h1 className="page-title">Audit Trail</h1>
            <p className="page-subtitle">
              {total.toLocaleString()} entries — complete immutable system history
              {hasFilters && <span className="ml-2 badge badge-blue">Filtered</span>}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={exportCsv} disabled={logs.length === 0}
            className="btn-secondary gap-2 text-sm disabled:opacity-40">
            <HiOutlineDownload className="w-4 h-4" /> Export CSV
          </button>
          <button onClick={() => setShowStats(v => !v)}
            className={clsx('btn-secondary gap-2 text-sm', showStats && 'bg-primary-50 text-primary-700 border-primary-200')}>
            <HiOutlineChartBar className="w-4 h-4" /> Analytics
          </button>
          <button onClick={() => refetch()} className="btn-secondary p-2 rounded-lg">
            <HiOutlineRefresh className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Analytics panel */}
      {showStats && stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-5">
          <div className="card p-4">
            <p className="text-xs font-semibold text-surface-500 dark:text-gray-400 dark:text-gray-400 uppercase tracking-wider mb-3">
              Top Active Users — Last 30 Days
            </p>
            <div className="space-y-2">
              {(stats.topUsers ?? []).slice(0, 5).map((u, i) => (
                <div key={i} className="flex items-center gap-3">
                  <span className="text-xs text-surface-400 dark:text-gray-500 dark:text-gray-500 w-4">{i + 1}</span>
                  <div className="flex-1 bg-surface-100 dark:bg-gray-700 rounded-full h-2 overflow-hidden">
                    <div className="bg-primary-500 h-2 rounded-full"
                      style={{ width: `${Math.min(100, (u.count / ((stats.topUsers[0]?.count ?? 1))) * 100)}%` }} />
                  </div>
                  <span className="text-sm font-medium text-surface-700 dark:text-gray-200 dark:text-gray-300 w-32 truncate">{u.user}</span>
                  <span className="badge badge-blue text-xs">{u.count}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="card p-4">
            <p className="text-xs font-semibold text-surface-500 dark:text-gray-400 dark:text-gray-400 uppercase tracking-wider mb-3">
              Actions Breakdown — Last 30 Days
            </p>
            <div className="space-y-1.5">
              {(stats.actionCounts ?? []).slice(0, 8).map((a, i) => (
                <div key={i} className="flex items-center justify-between">
                  <span className="text-xs font-mono text-surface-600 dark:text-gray-300">
                    {a.action.replace(/_/g, ' ')}
                  </span>
                  <span className="badge badge-gray text-xs">{a.count}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="card p-4 mb-5">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <HiOutlineFilter className="w-4 h-4 text-surface-400 dark:text-gray-500" />
            <span className="text-sm font-medium text-surface-700 dark:text-gray-200">Filter</span>
          </div>
          {hasFilters && (
            <button onClick={resetFilters} className="flex items-center gap-1 text-xs text-surface-500 dark:text-gray-400 dark:text-gray-400 hover:text-red-600">
              <HiOutlineX className="w-3.5 h-3.5" /> Clear all
            </button>
          )}
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-3 mb-3">
          <div>
            <label className="label text-xs">User</label>
            <input value={filters.user}
              onChange={e => setFilters(f => ({ ...f, user: e.target.value }))}
              onKeyDown={e => e.key === 'Enter' && applyFilters()}
              placeholder="username or email" className="input text-sm" />
          </div>
          <div>
            <label className="label text-xs">Action</label>
            <select value={filters.action}
              onChange={e => setFilters(f => ({ ...f, action: e.target.value }))}
              className="input text-sm">
              <option value="">All actions</option>
              {Object.entries(ACTION_GROUPS).map(([group, actions]) => (
                <optgroup key={group} label={group}>
                  {actions.map(a => (
                    <option key={a} value={a}>{a.replace(/_/g, ' ')}</option>
                  ))}
                </optgroup>
              ))}
            </select>
          </div>
          <div>
            <label className="label text-xs">Entity type</label>
            <select value={filters.entityType}
              onChange={e => setFilters(f => ({ ...f, entityType: e.target.value }))}
              className="input text-sm">
              <option value="">All entities</option>
              {ENTITY_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label className="label text-xs">From date</label>
            <input type="datetime-local" value={filters.from}
              onChange={e => setFilters(f => ({ ...f, from: e.target.value }))}
              className="input text-sm" />
          </div>
          <div>
            <label className="label text-xs">To date</label>
            <input type="datetime-local" value={filters.to}
              onChange={e => setFilters(f => ({ ...f, to: e.target.value }))}
              className="input text-sm" />
          </div>
        </div>
        <button onClick={applyFilters} className="btn-primary btn-sm gap-1.5">
          <HiOutlineSearch className="w-3.5 h-3.5" /> Apply filters
        </button>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : logs.length === 0 ? (
        <div className="text-center py-20">
          <HiOutlineShieldCheck className="w-10 h-10 mx-auto mb-3 text-surface-300 dark:text-gray-600" />
          <p className="text-surface-500 dark:text-gray-400">No audit logs match your filters</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="table text-xs">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User</th>
                <th>Action</th>
                <th>Entity</th>
                <th>Description</th>
                <th>IP Address</th>
                <th>Severity</th>
              </tr>
            </thead>
            <tbody>
              {logs.map(log => (
                <tr key={log.id}>
                  <td className="font-mono text-surface-400 dark:text-gray-500 dark:text-gray-500 whitespace-nowrap">
                    {formatDateTime(log.createdAt)}
                  </td>
                  <td className="font-medium text-surface-800 dark:text-gray-200">{log.performedBy}</td>
                  <td>
                    <span className="font-mono bg-surface-100 dark:bg-gray-800 text-surface-700 dark:text-gray-300 px-1.5 py-0.5 rounded text-xs">
                      {log.action}
                    </span>
                  </td>
                  <td className="text-surface-500 dark:text-gray-400">
                    {log.entityType}
                    {log.entityId && <span className="text-surface-400 dark:text-gray-500"> #{log.entityId}</span>}
                  </td>
                  <td className="max-w-xs">
                    <span className="truncate block text-surface-700 dark:text-gray-300" title={log.description}>
                      {log.description}
                    </span>
                  </td>
                  <td className="font-mono text-surface-400 dark:text-gray-500">{log.ipAddress ?? '—'}</td>
                  <td>
                    <span className={clsx('badge', SEVERITY[log.severity] ?? 'badge-gray')}>
                      {log.severity}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
