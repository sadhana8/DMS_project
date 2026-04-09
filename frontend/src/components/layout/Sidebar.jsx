import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import {
  HiOutlineViewGrid, HiOutlineDocumentText, HiOutlineUsers,
  HiOutlineCog, HiOutlineX, HiOutlineFolder,
  HiOutlineShieldCheck,
} from 'react-icons/hi'
import clsx from 'clsx'

const NAV = [
  { to: '/dashboard', icon: HiOutlineViewGrid,     label: 'Dashboard',  role: null },
  { to: '/documents', icon: HiOutlineDocumentText, label: 'Documents',  role: null },
  { to: '/users',     icon: HiOutlineUsers,        label: 'Users',      role: 'ROLE_MANAGER' },
  { to: '/admin',     icon: HiOutlineShieldCheck,  label: 'Admin',      role: 'ROLE_ADMIN' },
  { to: '/profile',   icon: HiOutlineCog,          label: 'Settings',   role: null },
]

export default function Sidebar({ open, onClose }) {
  const { user, hasRole, isAdmin } = useAuth()

  const canSee = (role) => !role || hasRole(role) || isAdmin()

  const content = (
    <div className="flex flex-col h-full">
      {/* Logo */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-surface-100">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center">
            <HiOutlineFolder className="w-5 h-5 text-white" />
          </div>
          <span className="text-base font-semibold text-surface-900">DMS</span>
        </div>
        <button onClick={onClose} className="lg:hidden btn-ghost p-1.5 rounded-lg">
          <HiOutlineX className="w-5 h-5" />
        </button>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5">
        {NAV.filter(n => canSee(n.role)).map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onClose}
            className={({ isActive }) =>
              clsx('sidebar-link', isActive && 'active')
            }
          >
            <Icon className="w-5 h-5 flex-shrink-0" />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* User card */}
      <div className="p-3 border-t border-surface-100">
        <NavLink to="/profile" onClick={onClose} className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-surface-100 transition-colors">
          <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-semibold flex-shrink-0">
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-surface-900 truncate">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-xs text-surface-500 truncate">{user?.email}</p>
          </div>
        </NavLink>
      </div>
    </div>
  )

  return (
    <>
      {/* Desktop */}
      <aside className="hidden lg:flex flex-col w-64 bg-white border-r border-surface-200 flex-shrink-0">
        {content}
      </aside>

      {/* Mobile overlay */}
      {open && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div className="absolute inset-0 bg-black/40 animate-fade-in" onClick={onClose} />
          <aside className="relative w-64 bg-white flex flex-col animate-slide-in">
            {content}
          </aside>
        </div>
      )}
    </>
  )
}
