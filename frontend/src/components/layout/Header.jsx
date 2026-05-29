import { useState, useRef, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/context/AuthContext'
import { notificationsApi } from '@/api/notifications'
import {
  HiOutlineMenu, HiOutlineBell, HiOutlineSearch,
  HiOutlineLogout, HiOutlineUser, HiOutlineCog,
} from 'react-icons/hi'

export default function Header({ onMenuClick }) {
  const { user, logout, isAdmin } = useAuth()
  const navigate      = useNavigate()
  const [drop, setDrop] = useState(false)
  const [search, setSearch] = useState('')
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

  const handleSearch = (e) => {
    if (e.key === 'Enter' && search.trim()) {
      navigate(`/documents?q=${encodeURIComponent(search.trim())}`)
      setSearch('')
    }
  }

  const initials = user
    ? `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase()
    : '?'

  return (
    <header className="flex items-center justify-between px-4 lg:px-6 h-16 bg-white border-b border-surface-200 flex-shrink-0 z-20">
      <div className="flex items-center gap-3 flex-1">
        <button onClick={onMenuClick} className="btn-ghost p-2 rounded-lg lg:hidden">
          <HiOutlineMenu className="w-5 h-5" />
        </button>
        {/* <div className="relative w-full max-w-md hidden sm:block">
          <HiOutlineSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
          <input
            type="text" placeholder="Search documents…"
            value={search} onChange={e => setSearch(e.target.value)} onKeyDown={handleSearch}
            className="input pl-9 py-1.5 text-sm"
          />
        </div> */}
      </div>

      <div className="flex items-center gap-1 ml-4">
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
            className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-surface-100 transition-colors"
          >
            <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-bold">
              {initials}
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-xs font-medium text-surface-800 leading-none">{user?.firstName} {user?.lastName}</p>
              <p className="text-[10px] text-surface-400 mt-0.5 truncate max-w-[120px]">{user?.email}</p>
            </div>
          </button>
          {drop && (
            <div className="dropdown w-56">
              <Link to="/profile"       onClick={() => setDrop(false)} className="dropdown-item"><HiOutlineUser className="w-4 h-4" /> My profile</Link>
              <Link to="/notifications/settings" onClick={() => setDrop(false)} className="dropdown-item"><HiOutlineBell className="w-4 h-4" /> Notification settings</Link>
              {isAdmin() && <Link to="/settings" onClick={() => setDrop(false)} className="dropdown-item"><HiOutlineCog className="w-4 h-4" /> System settings</Link>}
              <div className="border-t border-surface-100 my-1" />
              <button onClick={logout} className="dropdown-item w-full text-red-600 hover:bg-red-50">
                <HiOutlineLogout className="w-4 h-4" /> Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
