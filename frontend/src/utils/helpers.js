import { formatDistanceToNow, format } from 'date-fns'

/* ── File helpers ─────────────────────────────────────────── */
export const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k    = 1024
  const sizes = ['B','KB','MB','GB','TB']
  const i    = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

export const getFileIcon = (mimeType = '', fileName = '') => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  if (mimeType.startsWith('image/'))         return '🖼️'
  if (mimeType === 'application/pdf')        return '📄'
  if (mimeType.includes('word') || ext === 'docx' || ext === 'doc') return '📝'
  if (mimeType.includes('excel') || ext === 'xlsx' || ext === 'xls') return '📊'
  if (mimeType.includes('powerpoint') || ext === 'pptx')            return '📋'
  if (mimeType.includes('zip') || mimeType.includes('rar'))         return '📦'
  if (mimeType.startsWith('video/'))         return '🎥'
  if (mimeType.startsWith('audio/'))         return '🎵'
  if (mimeType.startsWith('text/'))          return '📃'
  return '📁'
}

export const getFileColor = (mimeType = '') => {
  if (mimeType.startsWith('image/'))        return 'bg-purple-100 text-purple-700'
  if (mimeType === 'application/pdf')       return 'bg-red-100 text-red-700'
  if (mimeType.includes('word'))            return 'bg-blue-100 text-blue-700'
  if (mimeType.includes('excel'))           return 'bg-green-100 text-green-700'
  if (mimeType.includes('powerpoint'))      return 'bg-orange-100 text-orange-700'
  if (mimeType.startsWith('video/'))        return 'bg-pink-100 text-pink-700'
  return 'bg-surface-100 text-surface-600'
}

export const isPreviewable = (mimeType = '') =>
  mimeType === 'application/pdf' ||
  mimeType.startsWith('image/') ||
  mimeType.startsWith('text/')

/* ── Date helpers ─────────────────────────────────────────── */
export const timeAgo    = (date) => formatDistanceToNow(new Date(date), { addSuffix: true })
export const formatDate = (date) => format(new Date(date), 'MMM d, yyyy')
export const formatDateTime = (date) => format(new Date(date), 'MMM d, yyyy h:mm a')

/* ── Role helpers ─────────────────────────────────────────── */
export const ROLE_LABELS = {
  ROLE_ADMIN:    { label: 'Admin',    color: 'badge-red'    },
  ROLE_HR:       { label: 'HR',       color: 'badge-purple' },
  ROLE_ACCOUNT:  { label: 'Account',  color: 'badge-blue'   },
  ROLE_EMPLOYEE: { label: 'Employee', color: 'badge-gray'   },
}

export const getRoleBadge = (role) =>
  ROLE_LABELS[role] ?? { label: role, color: 'badge-gray' }

export const PERM_LABELS = {
  VIEW:     { label: 'View',     color: 'badge-gray'  },
  DOWNLOAD: { label: 'Download', color: 'badge-blue'  },
  EDIT:     { label: 'Edit',     color: 'badge-yellow'},
  ADMIN:    { label: 'Admin',    color: 'badge-red'   },
}

/* ── Misc ─────────────────────────────────────────────────── */
export const initials = (firstName = '', lastName = '') =>
  `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()

export const getErrorMessage = (error) =>
  error?.response?.data?.message ?? error?.message ?? 'Something went wrong'

export const truncate = (str, n = 40) =>
  str?.length > n ? str.slice(0, n) + '…' : str
