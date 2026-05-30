import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { settingsApi } from '@/api/settings'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineCog, HiOutlineSave, HiOutlineRefresh } from 'react-icons/hi'

const CATEGORIES = [
  { key: 'company',       label: 'Company',          icon: '🏢' },
  { key: 'auth',          label: 'Authentication',   icon: '🔐' },
  { key: 'storage',       label: 'Storage',          icon: '💾' },
  { key: 'security',      label: 'Security',         icon: '🛡️' },
  { key: 'documents',     label: 'Documents',        icon: '📄' },
  { key: 'notifications', label: 'Notifications',    icon: '🔔' },
  { key: 'email',         label: 'Email',            icon: '✉️' },
  { key: 'system',        label: 'System',           icon: '⚙️' },
]

function Toggle({ checked, onChange }) {
  return (
    <button type="button" role="switch" aria-checked={checked} onClick={() => onChange(!checked)}
      className={`toggle ${checked ? 'toggle-on' : 'toggle-off'}`}>
      <span className={`toggle-thumb ${checked ? 'toggle-thumb-on' : 'toggle-thumb-off'}`} />
    </button>
  )
}

export default function SettingsPage() {
  const qc = useQueryClient()
  const [activeTab, setActiveTab] = useState('auth')
  const [dirty,     setDirty]     = useState({})
  const [saving,    setSaving]    = useState(false)

  const { data: allSettings = {}, isLoading } = useQuery({
    queryKey: ['system-settings'],
    queryFn:  settingsApi.getAll,
  })

  const getVal = (key) => {
    if (key in dirty) return dirty[key]
    return allSettings[key]?.value ?? ''
  }

  const setVal = (key, value) => {
    setDirty(d => ({ ...d, [key]: value }))
  }

  const handleSave = async () => {
    if (Object.keys(dirty).length === 0) { toast('No changes to save'); return }
    setSaving(true)
    try {
      await settingsApi.update(dirty)
      setDirty({})
      qc.invalidateQueries({ queryKey: ['system-settings'] })
      toast.success(`${Object.keys(dirty).length} setting${Object.keys(dirty).length > 1 ? 's' : ''} saved`)
    } catch { toast.error('Failed to save settings') }
    finally { setSaving(false) }
  }

  const handleReset = () => {
    setDirty({})
    toast('Changes discarded')
  }

  // Get settings for active category
  const catSettings = Object.entries(allSettings)
    .filter(([_, v]) => v.category === activeTab)

  const isBool  = (v) => v === 'true' || v === 'false'
  const isNum   = (key) => ['max_upload_size_mb','document_retention_days','max_versions_per_document','session_timeout_minutes'].includes(key)
  const isDirtyCount = Object.keys(dirty).length

  return (
    <div className="animate-fade-in">
      <div className="page-header mb-5">
        <div>
          <div className="flex items-center gap-2 mb-1"><HiOutlineCog className="w-5 h-5 text-primary-600" /><h1 className="page-title">System Settings</h1></div>
          <p className="page-subtitle">Configure all Document Management system behaviour</p>
        </div>
        <div className="flex items-center gap-2">
          {isDirtyCount > 0 && (
            <>
              <span className="text-sm text-amber-600 font-medium">{isDirtyCount} unsaved change{isDirtyCount > 1 ? 's' : ''}</span>
              <button onClick={handleReset} className="btn-secondary btn-sm gap-1.5"><HiOutlineRefresh className="w-3.5 h-3.5" /> Discard</button>
            </>
          )}
          <button onClick={handleSave} disabled={saving || isDirtyCount === 0} className="btn-primary btn-sm gap-1.5">
            <HiOutlineSave className="w-3.5 h-3.5" /> {saving ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : (
        <div className="flex gap-5">
          {/* Category sidebar */}
          <div className="w-48 flex-shrink-0">
            <div className="card overflow-hidden">
              {CATEGORIES.map(cat => (
                <button key={cat.key} onClick={() => setActiveTab(cat.key)}
                  className={clsx('w-full flex items-center gap-2.5 px-4 py-3 text-sm font-medium text-left border-b border-surface-100 last:border-0 transition-colors',
                    activeTab === cat.key ? 'bg-primary-50 text-primary-700' : 'text-surface-700 hover:bg-surface-50')}>
                  <span className="text-base">{cat.icon}</span>
                  <span>{cat.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Settings panel */}
          <div className="flex-1 min-w-0">
            <div className="card overflow-hidden">
              <div className="px-5 py-4 border-b border-surface-100 bg-surface-50">
                <h2 className="text-sm font-semibold text-surface-800">
                  {CATEGORIES.find(c => c.key === activeTab)?.label} Settings
                </h2>
              </div>

              {catSettings.length === 0 ? (
                <div className="text-center py-12 text-surface-400">No settings in this category</div>
              ) : (
                <div className="divide-y divide-surface-100">
                  {catSettings.map(([key, meta]) => {
                    const val        = getVal(key)
                    const isChanged  = key in dirty
                    const boolSetting = isBool(meta.value)
                    return (
                      <div key={key} className={clsx('flex items-center gap-4 px-5 py-4', isChanged && 'bg-amber-50/50')}>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-medium text-surface-800">
                              {key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                            </p>
                            {isChanged && <span className="badge badge-amber text-[10px]">modified</span>}
                          </div>
                          <p className="text-xs text-surface-400 mt-0.5">{meta.description}</p>
                        </div>
                        <div className="flex-shrink-0 w-48">
                          {boolSetting ? (
                            <Toggle checked={val === 'true'} onChange={v => setVal(key, String(v))} />
                          ) : isNum(key) ? (
                            <input type="number" value={val} onChange={e => setVal(key, e.target.value)}
                              className="input text-sm py-1.5 w-full" min={0} />
                          ) : (
                            <input type="text" value={val} onChange={e => setVal(key, e.target.value)}
                              className="input text-sm py-1.5 w-full" />
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
