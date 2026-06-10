import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useCompany } from '@/context/CompanyContext'
import { useTheme } from '@/context/ThemeContext'
import { useQuery } from '@tanstack/react-query'
import { approvalsApi } from '@/api/approvals'
import { notificationsApi } from '@/api/notifications'
import clsx from 'clsx'
import {
  HiOutlineHome, HiOutlineDocumentText, HiOutlineUsers,
  HiOutlineShieldCheck, HiOutlineBell, HiOutlineCog,
  HiOutlineUserGroup, HiOutlineKey, HiOutlineSearch,
  HiOutlineChevronLeft, HiOutlineChevronRight,
  HiOutlineOfficeBuilding, HiOutlineLogout, HiOutlineUser,
  HiOutlineSun, HiOutlineMoon, HiOutlineX,
} from 'react-icons/hi'

const Divider = () => <div className="h-px bg-surface-100 dark:bg-gray-800 mx-3 my-1" />

const SideLabel = ({ label, collapsed }) =>
  collapsed ? null : (
    <p className="px-3 pt-3 pb-1 text-[10px] font-semibold text-surface-400 dark:text-gray-600 uppercase tracking-widest">{label}</p>
  )

export default function Sidebar({ open, onClose, collapsed, onToggleCollapse }) {
  const { user, isAdmin, isManager, logout } = useAuth()
  const { company }   = useCompany()
  const { dark, toggle: toggleTheme } = useTheme()
  const navigate      = useNavigate()
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  const { data: pendingApprovals = 0 } = useQuery({
    queryKey: ['approval-count'],
    queryFn:  approvalsApi.count,
    enabled:  isAdmin(),
    refetchInterval: 60_000,
  })

  const { data: changeReqsCount } = useQuery({
    queryKey: ['change-requests-count'],
    queryFn:  () => import('@/api/profileChanges').then(m => m.profileChangesApi.pendingCount()),
    enabled:  isManager() || isAdmin(),
    refetchInterval: 60_000,
  })
  const pendingChangeReqs = changeReqsCount?.pending ?? 0

  const { data: unreadNotif = 0 } = useQuery({
    queryKey: ['notif-count'],
    queryFn:  notificationsApi.unreadCount,
    refetchInterval: 30_000,
  })

  const navGroups = [
    {
      label: 'Main',
      items: [
        { to: '/dashboard',                label: 'Dashboard',       icon: HiOutlineHome,         show: true },
        { to: '/documents',                label: 'Documents',       icon: HiOutlineDocumentText, show: true },
        { to: '/documents/search/advanced',label: 'Advanced search', icon: HiOutlineSearch,       show: true },
      ],
    },
    {
      label: 'Team',
      items: [
        { to: '/users',               label: 'Users',            icon: HiOutlineUsers,       show: isManager() || isAdmin() },
        { to: '/hr/change-requests',  label: 'Change requests',  icon: HiOutlineDocumentText,show: isManager() || isAdmin(), badge: pendingChangeReqs },
        { to: '/approvals',           label: 'Approvals',        icon: HiOutlineUserGroup,   show: isAdmin(), badge: pendingApprovals },
      ],
    },
    {
      label: 'Admin',
      items: [
        { to: '/admin/roles', label: 'Roles',       icon: HiOutlineKey,         show: isAdmin() },
        { to: '/audit',       label: 'Audit trail',  icon: HiOutlineShieldCheck, show: isAdmin() },
        { to: '/settings',    label: 'Settings',     icon: HiOutlineCog,         show: isAdmin() },
      ],
    },
    {
      label: 'Account',
      items: [
        { to: '/notifications', label: 'Notifications', icon: HiOutlineBell,   show: true, badge: unreadNotif },
        { to: '/profile',       label: 'My profile',     icon: HiOutlineUser,   show: true },
      ],
    },
  ]

  const companyName = company?.company_name || 'DocVault'
  const logoUrl     = company?.company_logo_url || ''
  const initials    = user ? `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase() : '?'

  const handleLogout = async () => {
    await logout()
    navigate('/login')
    onClose?.()
  }

  return (
    <>
      {/* Mobile backdrop */}
      {open && <div className="fixed inset-0 bg-black/50 z-30 lg:hidden" onClick={onClose} />}

      <aside className={clsx(
        'fixed top-0 left-0 h-full z-40 flex flex-col transition-all duration-300',
        'bg-white dark:bg-gray-900 border-r border-surface-200 dark:border-gray-800',
        'lg:relative lg:translate-x-0 lg:z-auto',
        collapsed ? 'w-16' : 'w-64',
        open ? 'translate-x-0' : '-translate-x-full',
      )}>

        {/* Logo */}
        <div className={clsx(
          'flex items-center border-b border-surface-100 dark:border-gray-800 relative flex-shrink-0',
          collapsed ? 'justify-center px-2 py-4' : 'gap-3 px-4 py-4',
        )}>
          <div className="w-8 h-8 rounded-lg overflow-hidden flex items-center justify-center flex-shrink-0 bg-primary-100 dark:bg-primary-900/40">
            {logoUrl
              ? <img src={logoUrl} alt={companyName} className="w-full h-full object-contain" />
              : <HiOutlineOfficeBuilding className="w-5 h-5 text-primary-600 dark:text-primary-400" />
            }
          </div>
          {!collapsed && (
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-sm text-surface-900 dark:text-gray-100 truncate">{companyName}</p>
            </div>
          )}
          {/* Mobile close */}
          <button onClick={onClose} className="lg:hidden btn-ghost p-1 rounded-lg absolute right-2 top-1/2 -translate-y-1/2">
            <HiOutlineX className="w-4 h-4" />
          </button>
          {/* Desktop collapse toggle */}
          <button
            onClick={onToggleCollapse}
            className="hidden lg:flex items-center justify-center w-5 h-5 rounded-full bg-white dark:bg-gray-800 border border-surface-200 dark:border-gray-700 text-surface-400 dark:text-gray-500 hover:text-surface-700 dark:hover:text-gray-200 shadow-sm transition-colors absolute -right-2.5 top-1/2 -translate-y-1/2 z-10"
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <HiOutlineChevronRight className="w-3 h-3" /> : <HiOutlineChevronLeft className="w-3 h-3" />}
          </button>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-2">
          {navGroups.map(group => {
            const visible = group.items.filter(i => i.show)
            if (!visible.length) return null
            return (
              <div key={group.label}>
                <SideLabel label={group.label} collapsed={collapsed} />
                <div className="px-2 space-y-0.5">
                  {visible.map(({ to, label, icon: Icon, badge }) => (
                    <NavLink
                      key={to}
                      to={to}
                      onClick={onClose}
                      title={collapsed ? label : undefined}
                      className={({ isActive }) => clsx(
                        'sidebar-link relative',
                        isActive && 'active',
                        collapsed && 'justify-center px-2',
                      )}
                    >
                      <Icon className="w-4 h-4 flex-shrink-0" />
                      {!collapsed && <span className="flex-1 truncate">{label}</span>}
                      {!collapsed && badge > 0 && (
                        <span className="min-w-[18px] h-[18px] px-1 bg-red-500 text-white rounded-full text-[10px] font-bold flex items-center justify-center">
                          {badge > 9 ? '9+' : badge}
                        </span>
                      )}
                      {collapsed && badge > 0 && (
                        <span className="absolute top-0.5 right-0.5 w-2 h-2 bg-red-500 rounded-full" />
                      )}
                    </NavLink>
                  ))}
                </div>
              </div>
            )
          })}

          <Divider />

          {/* Dark mode toggle row */}
          <div className="px-2 mt-1">
            <button
              onClick={toggleTheme}
              title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
              className={clsx('sidebar-link w-full', collapsed && 'justify-center px-2')}
            >
              {dark
                ? <HiOutlineSun className="w-4 h-4 flex-shrink-0 text-amber-400" />
                : <HiOutlineMoon className="w-4 h-4 flex-shrink-0" />
              }
              {!collapsed && <span className="flex-1">{dark ? 'Light mode' : 'Dark mode'}</span>}
            </button>
          </div>
        </nav>

        {/* User card at bottom */}
        <div className="border-t border-surface-100 dark:border-gray-800 flex-shrink-0">
          {collapsed ? (
            <div className="p-2 space-y-1">
              <NavLink to="/profile" className="flex items-center justify-center p-2 rounded-lg hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors" title={`${user?.firstName} ${user?.lastName}`}>
                <div className="w-7 h-7 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center text-xs font-bold">{initials}</div>
              </NavLink>
              <button onClick={handleLogout} className="w-full flex items-center justify-center p-2 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-red-500 dark:text-red-400 transition-colors" title="Sign out">
                <HiOutlineLogout className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="p-3">
              <button
                onClick={() => setUserMenuOpen(v => !v)}
                className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors"
              >
                <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center text-xs font-bold flex-shrink-0">{initials}</div>
                <div className="flex-1 min-w-0 text-left">
                  <p className="text-sm font-medium text-surface-800 dark:text-gray-200 truncate">{user?.firstName} {user?.lastName}</p>
                  <p className="text-[11px] text-surface-400 dark:text-gray-500 truncate">{user?.email}</p>
                </div>
              </button>
              {userMenuOpen && (
                <div className="mt-1 space-y-0.5">
                  <NavLink to="/profile" onClick={() => { setUserMenuOpen(false); onClose?.() }}
                    className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-surface-700 dark:text-gray-300 hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors">
                    <HiOutlineUser className="w-4 h-4" /> My profile
                  </NavLink>
                  <NavLink to="/notifications/settings" onClick={() => { setUserMenuOpen(false); onClose?.() }}
                    className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-surface-700 dark:text-gray-300 hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors">
                    <HiOutlineBell className="w-4 h-4" /> Notification settings
                  </NavLink>
                  {isAdmin() && (
                    <NavLink to="/settings" onClick={() => { setUserMenuOpen(false); onClose?.() }}
                      className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-surface-700 dark:text-gray-300 hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors">
                      <HiOutlineCog className="w-4 h-4" /> System settings
                    </NavLink>
                  )}
                  <div className="h-px bg-surface-100 dark:bg-gray-800 my-1" />
                  <button onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors">
                    <HiOutlineLogout className="w-4 h-4" /> Sign out
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </aside>
    </>
  )
}
