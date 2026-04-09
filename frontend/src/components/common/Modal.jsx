import { useEffect } from 'react'
import { HiOutlineX } from 'react-icons/hi'
import clsx from 'clsx'

const SIZES = { sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl', full: 'max-w-6xl' }

export default function Modal({ open, onClose, title, children, size = 'md', footer }) {
  useEffect(() => {
    if (open) document.body.style.overflow = 'hidden'
    else       document.body.style.overflow = ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose?.() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 animate-fade-in" onClick={onClose} />
      <div className={clsx('relative w-full bg-white rounded-2xl shadow-modal animate-slide-up flex flex-col max-h-[90vh]', SIZES[size])}>
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-surface-100 flex-shrink-0">
          <h2 className="text-lg font-semibold text-surface-900">{title}</h2>
          <button onClick={onClose} className="btn-ghost p-1.5 rounded-lg">
            <HiOutlineX className="w-5 h-5" />
          </button>
        </div>
        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
        {/* Footer */}
        {footer && (
          <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-surface-100 flex-shrink-0">
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}
