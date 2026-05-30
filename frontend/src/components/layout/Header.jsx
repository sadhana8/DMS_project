import { useState, useRef, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/context/AuthContext'
import { useTheme } from '@/context/ThemeContext'
import { notificationsApi } from '@/api/notifications'
import {
  HiOutlineMenu, HiOutlineBell, HiOutlineLogout,
  HiOutlineUser, HiOutlineCog, HiOutlineSun, HiOutlineMoon,
} from 'react-icons/hi'

export default function Header({ onMenuClick }) {
  const { user, logout, isAdmin } = useAuth()
  const { dark, toggle: toggleTheme } = useTheme()
  const navigate      = useNavigate()
  const [drop, setDrop] = useState(false)
  const dropRef = useRef(null)

  const { data: unread = 0 } = useQuery({
    queryKey: ['notif-count'],
    queryFn:  notificationsApi.unreadCount,
    refetchInterval: 30_000,
  })

  useEffect(() => {
    const h = (e) => { if (dropRef.current && !dropRef.current.contains(e.target)) setDrop(false) }
    document.addEventListener('mousedown', h)
    return () => document.removeEventListener('mousedown', h)
  }, [])

  const initials = user
    ? `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase()
    : '?'

  return (
    <header className="flex items-center justify-between px-4 lg:px-6 h-16 bg-white dark:bg-gray-900 border-b border-surface-200 dark:border-gray-800 flex-shrink-0 z-20">
      <div className="flex items-center gap-3 flex-1">
        <button onClick={onMenuClick} className="btn-ghost p-2 rounded-lg lg:hidden">
          <HiOutlineMenu className="w-5 h-5" />
        </button>
      </div>

      <div className="flex items-center gap-1 ml-4">
        {/* Dark mode toggle */}
        <button
          onClick={toggleTheme}
          className="btn-ghost p-2 rounded-lg"
          title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {dark
            ? <HiOutlineSun  className="w-5 h-5 text-amber-400" />
            : <HiOutlineMoon className="w-5 h-5" />
          }
        </button>

        {/* Notification bell */}
        <Link to="/notifications" className="relative btn-ghost p-2 rounded-lg">
          <HiOutlineBell className="w-5 h-5" />
          {unread > 0 && (
            <span className="absolute top-1 right-1 min-w-[16px] h-4 px-0.5 rounded-full bg-red-500 text-white text-[9px] font-bold flex items-center justify-center leading-none">
              {unread > 99 ? '99+' : unread}
            </span>
          )}
        </Link>

        {/* Avatar dropdown */}
        <div className="relative" ref={dropRef}>
          <button
            onClick={() => setDrop(v => !v)}
            className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-surface-100 dark:hover:bg-gray-800 transition-colors"
          >
            <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center text-xs font-bold">
              {initials}
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-xs font-medium text-surface-800 dark:text-gray-200 leading-none">{user?.firstName} {user?.lastName}</p>
              <p className="text-[10px] text-surface-400 dark:text-gray-500 mt-0.5 truncate max-w-[120px]">{user?.email}</p>
            </div>
          </button>
          {drop && (
            <div className="dropdown w-56">
              <Link to="/profile"       onClick={() => setDrop(false)} className="dropdown-item"><HiOutlineUser className="w-4 h-4" /> My profile</Link>
              <Link to="/notifications/settings" onClick={() => setDrop(false)} className="dropdown-item"><HiOutlineBell className="w-4 h-4" /> Notification settings</Link>
              {isAdmin() && <Link to="/settings" onClick={() => setDrop(false)} className="dropdown-item"><HiOutlineCog className="w-4 h-4" /> System settings</Link>}
              <div className="border-t border-surface-100 dark:border-gray-700 my-1" />
              <button
                onClick={() => { toggleTheme(); setDrop(false) }}
                className="dropdown-item w-full"
              >
                {dark ? <HiOutlineSun className="w-4 h-4 text-amber-400" /> : <HiOutlineMoon className="w-4 h-4" />}
                {dark ? 'Light mode' : 'Dark mode'}
              </button>
              <div className="border-t border-surface-100 dark:border-gray-700 my-1" />
              <button onClick={logout} className="dropdown-item w-full text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20">
                <HiOutlineLogout className="w-4 h-4" /> Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
