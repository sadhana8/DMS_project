import clsx from 'clsx'
import { HiOutlineExclamationCircle } from 'react-icons/hi'
import Modal from './Modal'

/* ── EmptyState ─────────────────────────────────────────────────── */
export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      {Icon && <div className="w-14 h-14 rounded-2xl bg-surface-100 dark:bg-gray-800 flex items-center justify-center mb-4">
        <Icon className="w-7 h-7 text-surface-400 dark:text-gray-500" />
      </div>}
      <h3 className="text-base font-semibold text-surface-700 dark:text-gray-300 mb-1">{title}</h3>
      {description && <p className="text-sm text-surface-400 dark:text-gray-500 max-w-xs mb-5">{description}</p>}
      {action}
    </div>
  )
}

/* ── Pagination ─────────────────────────────────────────────────── */
export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  const pages = Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
    if (totalPages <= 7) return i + 1
    if (i === 0) return 1
    if (i === 6) return totalPages
    if (page <= 4) return i + 1
    if (page >= totalPages - 3) return totalPages - 6 + i
    return page - 3 + i
  })
  return (
    <div className="flex items-center justify-center gap-1 mt-6">
      <button disabled={page === 1} onClick={() => onChange(page - 1)}
        className="btn-secondary btn-sm disabled:opacity-40">← Prev</button>
      {pages.map((p, i) => (
        <button key={i} onClick={() => typeof p === 'number' && onChange(p)}
          className={clsx('w-9 h-9 rounded-lg text-sm font-medium transition-colors',
            p === page ? 'bg-primary-600 text-white' : 'hover:bg-surface-100 dark:hover:bg-gray-800 text-surface-600 dark:text-gray-400')}>
          {p}
        </button>
      ))}
      <button disabled={page === totalPages} onClick={() => onChange(page + 1)}
        className="btn-secondary btn-sm disabled:opacity-40">Next →</button>
    </div>
  )
}

/* ── ConfirmDialog ──────────────────────────────────────────────── */
export function ConfirmDialog({ open, onClose, onConfirm, title, message, confirmLabel = 'Delete', variant = 'danger', loading }) {
  return (
    <Modal open={open} onClose={onClose} title={title} size="sm"
      footer={<>
        <button className="btn-secondary" onClick={onClose}>Cancel</button>
        <button className={clsx('btn', variant === 'danger' ? 'btn-danger' : 'btn-primary')}
          onClick={onConfirm} disabled={loading}>
          {loading ? 'Processing…' : confirmLabel}
        </button>
      </>}>
      <div className="flex gap-4">
        <HiOutlineExclamationCircle className="w-10 h-10 text-red-400 flex-shrink-0 mt-0.5" />
        <p className="text-sm text-surface-600 leading-relaxed">{message}</p>
      </div>
    </Modal>
  )
}

/* ── Avatar ─────────────────────────────────────────────────────── */
export function Avatar({ user, size = 'md' }) {
  const sizes = { sm: 'w-7 h-7 text-xs', md: 'w-9 h-9 text-sm', lg: 'w-12 h-12 text-base', xl: 'w-16 h-16 text-xl' }
  if (user?.profilePicture) {
    return <img src={user.profilePicture} alt="" className={clsx('rounded-full object-cover', sizes[size])} />
  }
  return (
    <div className={clsx('rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 font-semibold flex items-center justify-center flex-shrink-0', sizes[size])}>
      {user?.firstName?.[0]}{user?.lastName?.[0]}
    </div>
  )
}

/* ── StatusBadge ────────────────────────────────────────────────── */
export function StatusBadge({ status }) {
  const map = {
    ACTIVE:         'badge-green',
    ARCHIVED:       'badge-yellow',
    DELETED:        'badge-red',
    PENDING_REVIEW: 'badge-blue',
  }
  return <span className={clsx(map[status] ?? 'badge-gray')}>{status?.replace('_', ' ')}</span>
}
