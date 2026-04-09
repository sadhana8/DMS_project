import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import {
  HiOutlineMenu, HiOutlineBell, HiOutlineSearch,
  HiOutlineLogout, HiOutlineUser, HiOutlineCog,
} from 'react-icons/hi'

export default function Header({ onMenuClick }) {
  const { user, logout } = useAuth()
  const navigate         = useNavigate()
  const [dropOpen, setDropOpen]  = useState(false)
  const [search,   setSearch]    = useState('')
  const dropRef = useRef(null)

  useEffect(() => {
    const handler = (e) => { if (dropRef.current && !dropRef.current.contains(e.target)) setDropOpen(false) }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleSearch = (e) => {
    if (e.key === 'Enter' && search.trim()) {
      navigate(`/documents?q=${encodeURIComponent(search.trim())}`)
      setSearch('')
    }
  }

  return (
    <header className="flex items-center justify-between px-4 lg:px-6 h-16 bg-white border-b border-surface-200 flex-shrink-0">
      {/* Left */}
      <div className="flex items-center gap-3 flex-1">
        <button onClick={onMenuClick} className="btn-ghost p-2 rounded-lg lg:hidden">
          <HiOutlineMenu className="w-5 h-5" />
        </button>
        <div className="relative w-full max-w-md hidden sm:block">
          <HiOutlineSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
          <input
            type="text"
            placeholder="Search documents…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={handleSearch}
            className="input pl-9 py-1.5 text-sm"
          />
        </div>
      </div>

      {/* Right */}
      <div className="flex items-center gap-2 ml-4">
        <button className="btn-ghost p-2 rounded-lg relative">
          <HiOutlineBell className="w-5 h-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full" />
        </button>

        {/* Avatar dropdown */}
        <div className="relative" ref={dropRef}>
          <button
            onClick={() => setDropOpen(v => !v)}
            className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-surface-100 transition-colors"
          >
            <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-semibold">
              {user?.firstName?.[0]}{user?.lastName?.[0]}
            </div>
            <span className="hidden md:block text-sm font-medium text-surface-700">
              {user?.firstName}
            </span>
          </button>

          {dropOpen && (
            <div className="dropdown w-56">
              <div className="px-4 py-3 border-b border-surface-100">
                <p className="text-sm font-semibold text-surface-900">{user?.firstName} {user?.lastName}</p>
                <p className="text-xs text-surface-500 truncate">{user?.email}</p>
              </div>
              <button className="dropdown-item w-full" onClick={() => { navigate('/profile'); setDropOpen(false) }}>
                <HiOutlineUser className="w-4 h-4" /> My Profile
              </button>
              <button className="dropdown-item w-full" onClick={() => { navigate('/profile?tab=security'); setDropOpen(false) }}>
                <HiOutlineCog className="w-4 h-4" /> Settings
              </button>
              <div className="border-t border-surface-100 my-1" />
              <button className="dropdown-item w-full text-red-600 hover:bg-red-50" onClick={logout}>
                <HiOutlineLogout className="w-4 h-4" /> Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
