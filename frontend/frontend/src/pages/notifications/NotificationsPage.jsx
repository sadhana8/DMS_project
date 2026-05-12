import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { notificationsApi } from '@/api/notifications'
import { timeAgo } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import { Pagination } from '@/components/common/index'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineBell, HiOutlineCheckCircle, HiOutlineCog, HiOutlineFilter } from 'react-icons/hi'

const COLOUR = { blue:'badge-blue', amber:'badge-amber', green:'badge-green', red:'badge-red', purple:'badge-purple', pink:'badge-pink', gray:'badge-gray' }
const DOT    = { blue:'bg-blue-500', amber:'bg-amber-500', green:'bg-green-500', red:'bg-red-500', purple:'bg-purple-500', pink:'bg-pink-500', gray:'bg-surface-400' }

export default function NotificationsPage() {
  const qc = useQueryClient()
  const [page, setPage]   = useState(1)
  const [type, setType]   = useState('')
  const [read, setRead]   = useState('')
  const [from, setFrom]   = useState('')
  const [to,   setTo]     = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['notifications-full', page, type, read, from, to],
    queryFn: () => notificationsApi.list({
      page: page - 1, size: 20,
      type:   type   || undefined,
      isRead: read === '' ? undefined : read === 'unread' ? false : true,
      // Convert YYYY-MM-DD → full LocalDateTime so the Spring parser accepts it.
      // "to" gets end-of-day so the user's selected day is included.
      from:   from ? `${from}T00:00:00` : undefined,
      to:     to   ? `${to}T23:59:59`   : undefined,
    }),
  })

  const notifications = data?.content ?? []
  const totalPages    = data?.totalPages ?? 1
  const total         = data?.totalElements ?? 0

  const markAll = async () => {
    try { await notificationsApi.markAllRead(); qc.invalidateQueries({ queryKey: ['notifications-full'] }); qc.invalidateQueries({ queryKey: ['notif-count'] }); toast.success('All marked as read') }
    catch { toast.error('Failed') }
  }
  const markOne = async (id) => {
    try { await notificationsApi.markOneRead(id); qc.invalidateQueries({ queryKey: ['notifications-full'] }); qc.invalidateQueries({ queryKey: ['notif-count'] }) }
    catch {}
  }
  const reset = () => { setType(''); setRead(''); setFrom(''); setTo(''); setPage(1) }

  return (
    <div className="animate-fade-in max-w-3xl mx-auto">
      <div className="page-header mb-5">
        <div>
          <h1 className="page-title">Notifications</h1>
          <p className="page-subtitle">{total} total</p>
        </div>
        <div className="flex gap-2">
          <button onClick={markAll} className="btn-secondary btn-sm gap-1.5"><HiOutlineCheckCircle className="w-4 h-4" /> Mark all read</button>
          <Link to="/notifications/settings" className="btn-secondary btn-sm gap-1.5"><HiOutlineCog className="w-4 h-4" /> Settings</Link>
        </div>
      </div>

      {/* Filters */}
      <div className="card p-4 mb-5">
        <div className="flex items-center gap-2 mb-3"><HiOutlineFilter className="w-4 h-4 text-surface-400" /><span className="text-sm font-medium text-surface-700">Filter notifications</span></div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-3">
          <div>
            <label className="label text-xs">Type</label>
            <select value={type} onChange={e=>{ setType(e.target.value); setPage(1) }} className="input text-sm py-1.5">
              <option value="">All types</option>
              {['DOCUMENT_SHARED','DOCUMENT_DEPRECATED','DOCUMENT_RESTORED','VERSION_UPLOADED','ROLE_CHANGED','USER_APPROVED','USER_REJECTED','ACCOUNT_DEPRECATED','ACCOUNT_RESTORED','PENDING_APPROVAL','MENTION','SYSTEM'].map(t => (
                <option key={t} value={t}>{t.replace(/_/g,' ')}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label text-xs">Status</label>
            <select value={read} onChange={e=>{ setRead(e.target.value); setPage(1) }} className="input text-sm py-1.5">
              <option value="">All</option>
              <option value="unread">Unread</option>
              <option value="read">Read</option>
            </select>
          </div>
          <div>
            <label className="label text-xs">From date</label>
            <input type="date" value={from} onChange={e=>{ setFrom(e.target.value); setPage(1) }} className="input text-sm py-1.5" />
          </div>
          <div>
            <label className="label text-xs">To date</label>
            <input type="date" value={to} onChange={e=>{ setTo(e.target.value); setPage(1) }} className="input text-sm py-1.5" />
          </div>
        </div>
        <button onClick={reset} className="btn-secondary btn-sm">Reset filters</button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : notifications.length === 0 ? (
        <div className="text-center py-16"><HiOutlineBell className="w-12 h-12 mx-auto mb-3 text-surface-300" /><p className="text-surface-400">No notifications match your filters</p></div>
      ) : (
        <div className="card overflow-hidden">
          {notifications.map((n, i) => (
            <div key={n.id} onClick={() => markOne(n.id)}
              className={clsx('flex items-start gap-4 px-5 py-4 cursor-pointer transition-colors', i > 0 && 'border-t border-surface-100', !n.isRead ? 'bg-blue-50/40 hover:bg-blue-50' : 'hover:bg-surface-50')}>
              <div className={clsx('w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5', COLOUR[n.colour] ? `badge ${COLOUR[n.colour]}` : 'badge badge-gray')}>
                {n.typeLabel?.charAt(0) ?? 'N'}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className={clsx('text-sm', !n.isRead ? 'font-semibold text-surface-900' : 'font-medium text-surface-700')}>{n.title}</p>
                    <p className="text-sm text-surface-500 mt-0.5">{n.message}</p>
                  </div>
                  <div className="flex flex-col items-end gap-1 flex-shrink-0">
                    <span className="text-xs text-surface-400 whitespace-nowrap">{timeAgo(n.createdAt)}</span>
                    {!n.isRead && <span className={clsx('w-2 h-2 rounded-full', DOT[n.colour] ?? 'bg-blue-500')} />}
                  </div>
                </div>
                <div className="flex items-center gap-2 mt-1.5">
                  <span className={clsx('badge', COLOUR[n.colour] ?? 'badge-gray')}>{n.typeLabel}</span>
                  {n.link && <Link to={n.link} onClick={e => e.stopPropagation()} className="text-xs text-primary-600 hover:text-primary-800 font-medium">View →</Link>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
