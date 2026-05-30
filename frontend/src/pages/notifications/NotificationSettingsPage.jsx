import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { notificationsApi } from '@/api/notifications'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'
import { HiOutlineArrowLeft, HiOutlineRefresh } from 'react-icons/hi'

function Toggle({ checked, onChange }) {
  return (
    <button type="button" role="switch" aria-checked={checked} onClick={() => onChange(!checked)}
      className={`toggle ${checked ? 'toggle-on' : 'toggle-off'}`}>
      <span className={`toggle-thumb ${checked ? 'toggle-thumb-on' : 'toggle-thumb-off'}`} />
    </button>
  )
}

export default function NotificationSettingsPage() {
  const qc = useQueryClient()
  const { data: settings = [], isLoading } = useQuery({ queryKey: ['notif-settings'], queryFn: notificationsApi.getSettings })

  const toggle = async (type, channel, value) => {
    try { await notificationsApi.updateSetting({ type, [channel]: value }); qc.invalidateQueries({ queryKey: ['notif-settings'] }); toast.success('Saved') }
    catch { toast.error('Failed to save') }
  }
  const reset = async () => {
    if (!confirm('Reset all settings to defaults?')) return
    try { await notificationsApi.resetSettings(); qc.invalidateQueries({ queryKey: ['notif-settings'] }); toast.success('Reset to defaults') }
    catch { toast.error('Failed') }
  }

  return (
    <div className="animate-fade-in max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <Link to="/notifications" className="btn-ghost p-2 rounded-lg"><HiOutlineArrowLeft className="w-5 h-5" /></Link>
        <div className="flex-1"><h1 className="page-title">Notification Settings</h1><p className="page-subtitle">Control what you get notified about</p></div>
        <button onClick={reset} className="btn-secondary btn-sm gap-1.5"><HiOutlineRefresh className="w-4 h-4" /> Reset defaults</button>
      </div>

      {isLoading ? <div className="flex justify-center py-16"><Spinner size="lg" /></div> : (
        <div className="card overflow-hidden">
          <div className="grid grid-cols-[1fr_auto_auto] items-center gap-4 px-5 py-3 bg-surface-50 dark:bg-gray-800 border-b border-surface-200 dark:border-gray-700 text-xs font-semibold text-surface-500 dark:text-gray-400 uppercase tracking-wider">
            <span>Event</span><span className="w-16 text-center">In-app</span><span className="w-16 text-center">Email</span>
          </div>
          {settings.map((s, i) => (
            <div key={s.type} className={`grid grid-cols-[1fr_auto_auto] items-center gap-4 px-5 py-4 ${i > 0 ? 'border-t border-surface-100 dark:border-gray-800' : ''}`}>
              <div>
                <p className="text-sm font-medium text-surface-800 dark:text-gray-200">{s.typeLabel}</p>
                <p className="text-xs text-surface-400 dark:text-gray-500 mt-0.5">{s.description}</p>
              </div>
              <div className="w-16 flex justify-center"><Toggle checked={s.inApp ?? true} onChange={v => toggle(s.type, 'inApp', v)} /></div>
              <div className="w-16 flex justify-center"><Toggle checked={s.email ?? true} onChange={v => toggle(s.type, 'email', v)} /></div>
            </div>
          ))}
        </div>
      )}
      <p className="text-xs text-surface-400 dark:text-gray-500 text-center mt-4">Changes take effect immediately for new events.</p>
    </div>
  )
}
