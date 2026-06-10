import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header  from './Header'

export default function AppLayout() {
  const [sidebarOpen,      setSidebarOpen]      = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    try { return localStorage.getItem('sidebarCollapsed') === 'true' } catch { return false }
  })

  const handleToggleCollapse = () => {
    setSidebarCollapsed(v => {
      const next = !v
      try { localStorage.setItem('sidebarCollapsed', String(next)) } catch {}
      return next
    })
  }

  return (
    <div className="flex h-screen overflow-hidden bg-surface-50 dark:bg-gray-950">
      <Sidebar
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        collapsed={sidebarCollapsed}
        onToggleCollapse={handleToggleCollapse}
      />
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <Header onMenuClick={() => setSidebarOpen(true)} />
        <main className="flex-1 overflow-y-auto">
          <div className="p-6 max-w-7xl mx-auto w-full">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
