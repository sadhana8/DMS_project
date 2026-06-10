import { useState, useRef } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { dashboardApi } from '@/api/users'
import { approvalsApi } from '@/api/approvals'
import { auditApi } from '@/api/audit'
import { settingsApi } from '@/api/settings'
import { useCompany } from '@/context/CompanyContext'
import { formatFileSize, formatDateTime } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import {
  HiOutlineShieldCheck, HiOutlineCog, HiOutlineUserGroup,
  HiOutlineDocumentText, HiOutlineCheck,
  HiOutlineX, HiOutlineKey, HiOutlinePencil, HiOutlineOfficeBuilding,
  HiOutlineTag, HiOutlinePhotograph, HiOutlineExclamationCircle,
  HiOutlineTrash,
} from 'react-icons/hi'

const SECURITY_CHECKS = [
  { label: 'JWT authentication',    ok: true,  note: 'HMAC-SHA256 access + refresh tokens' },
  { label: 'BCrypt password hashing', ok: true, note: '12 rounds' },
  { label: 'Role-based access control', ok: true, note: '8 roles — ADMIN, HR, ACCOUNT, EMPLOYEE, MANAGER, FINANCE, LEGAL, REVIEWER' },
  { label: 'CORS configured',        ok: true,  note: 'Localhost origins in dev' },
  { label: 'Audit trail active',     ok: true,  note: 'All write actions logged' },
  { label: 'Two-factor authentication', ok: true, note: 'Email-based OTP, opt-in per user' },
  { label: 'HTTPS',                  ok: false, note: 'Configure Nginx + SSL for production' },
  { label: 'Rate limiting',          ok: false, note: 'Add API gateway for production' },
]

function LogoPicker({ value, onChange }) {
  const fileRef = useRef(null)
  const [tab, setTab]       = useState(value?.startsWith('http') ? 'url' : value ? 'file' : 'file')
  const [urlInput, setUrlInput] = useState(value?.startsWith('http') ? value : '')
  const [dragOver, setDragOver] = useState(false)
  const [sizeError, setSizeError] = useState('')

  const MAX_BYTES = 512 * 1024 // 512 KB

  const processFile = (file) => {
    setSizeError('')
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setSizeError('Please choose an image file (PNG, JPG, SVG, WebP, GIF).')
      return
    }
    if (file.size > MAX_BYTES) {
      setSizeError(`File is ${(file.size/1024).toFixed(0)} KB — max allowed is 512 KB. Resize or compress the image first.`)
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => onChange(e.target.result)
    reader.readAsDataURL(file)
  }

  const handleFileInput = (e) => processFile(e.target.files?.[0])

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    processFile(e.dataTransfer.files?.[0])
  }

  const handleUrlBlur = () => {
    if (urlInput.trim()) onChange(urlInput.trim())
    else onChange('')
  }

  const clearLogo = () => {
    onChange('')
    setUrlInput('')
    setSizeError('')
    if (fileRef.current) fileRef.current.value = ''
  }

  const isBase64 = value?.startsWith('data:')
  const isUrl    = value?.startsWith('http')

  return (
    <div className="space-y-3">
      <label className="label">Company Logo</label>

      {/* Tab switcher */}
      <div className="flex gap-1 p-1 bg-surface-100 dark:bg-gray-800 rounded-lg w-fit">
        {[
          { key: 'file', label: '📁 Upload file' },
          { key: 'url',  label: '🔗 Paste URL'  },
        ].map(t => (
          <button key={t.key} type="button"
            onClick={() => setTab(t.key)}
            className={clsx(
              'px-3 py-1.5 text-xs font-medium rounded-md transition-all',
              tab === t.key
                ? 'bg-white dark:bg-gray-700 text-surface-900 dark:text-gray-100 shadow-sm'
                : 'text-surface-500 dark:text-gray-400 hover:text-surface-700 dark:hover:text-gray-200'
            )}>
            {t.label}
          </button>
        ))}
      </div>

      {/* FILE UPLOAD TAB */}
      {tab === 'file' && (
        <div>
          {/* Drop zone */}
          <div
            onClick={() => fileRef.current?.click()}
            onDragOver={e => { e.preventDefault(); setDragOver(true) }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            className={clsx(
              'relative flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed cursor-pointer transition-all duration-150 py-6',
              dragOver
                ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                : 'border-surface-200 dark:border-gray-700 hover:border-primary-400 hover:bg-surface-50 dark:hover:bg-gray-800/60'
            )}
          >
            <input
              ref={fileRef}
              type="file"
              accept="image/png,image/jpeg,image/svg+xml,image/webp,image/gif"
              className="hidden"
              onChange={handleFileInput}
            />
            {isBase64 ? (
              <>
                <img
                  src={value}
                  alt="Logo preview"
                  className="h-14 w-auto object-contain rounded-lg"
                />
                <p className="text-xs text-surface-500 dark:text-gray-400">Click to replace</p>
              </>
            ) : (
              <>
                <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <HiOutlinePhotograph className="w-6 h-6 text-primary-600 dark:text-primary-400" />
                </div>
                <div className="text-center">
                  <p className="text-sm font-medium text-surface-700 dark:text-gray-300">
                    {dragOver ? 'Drop image here' : 'Click to choose or drag & drop'}
                  </p>
                  <p className="text-xs text-surface-400 dark:text-gray-500 mt-0.5">
                    PNG, JPG, SVG, WebP · Max 512 KB
                  </p>
                </div>
              </>
            )}
          </div>

          {sizeError && (
            <p className="mt-1.5 text-xs text-red-600 dark:text-red-400 flex items-center gap-1">
              <HiOutlineExclamationCircle className="w-3.5 h-3.5 flex-shrink-0" /> {sizeError}
            </p>
          )}
        </div>
      )}

      {/* URL TAB */}
      {tab === 'url' && (
        <div>
          <input
            value={urlInput}
            onChange={e => setUrlInput(e.target.value)}
            onBlur={handleUrlBlur}
            onKeyDown={e => e.key === 'Enter' && handleUrlBlur()}
            className="input"
            placeholder="https://yoursite.com/logo.png"
          />
          <p className="text-xs text-surface-400 dark:text-gray-500 mt-1">
            Must be a publicly accessible image URL. Press Enter or click away to preview.
          </p>
          {isUrl && (
            <div className="mt-2 flex items-center gap-3 p-2 bg-surface-50 dark:bg-gray-800 rounded-lg border border-surface-200 dark:border-gray-700">
              <img
                src={value}
                alt="Logo preview"
                className="h-10 w-auto object-contain rounded"
                onError={e => { e.target.style.display='none' }}
              />
              <p className="text-xs text-surface-500 dark:text-gray-400">Preview</p>
            </div>
          )}
        </div>
      )}

      {/* Clear button */}
      {value && (
        <button type="button" onClick={clearLogo}
          className="flex items-center gap-1.5 text-xs text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 transition-colors">
          <HiOutlineTrash className="w-3.5 h-3.5" /> Remove logo
        </button>
      )}
    </div>
  )
}

function CompanyProfileModal({ open, onClose, settings, onSaved }) {
  const [form, setForm] = useState(() => ({
    company_name:     settings?.company_name?.value     ?? '',
    company_logo_url: settings?.company_logo_url?.value ?? '',
    company_address:  settings?.company_address?.value  ?? '',
    company_email:    settings?.company_email?.value    ?? '',
    company_phone:    settings?.company_phone?.value    ?? '',
    company_website:  settings?.company_website?.value  ?? '',
    app_version:      settings?.app_version?.value      ?? '1.0.0',
  }))
  const [saving, setSaving] = useState(false)

  const handleSave = async () => {
    setSaving(true)
    try {
      await settingsApi.update(form)
      toast.success('Company profile updated')
      onSaved?.()
      onClose()
    } catch {
      toast.error('Failed to save company profile')
    } finally { setSaving(false) }
  }

  return (
    <Modal open={open} onClose={onClose} title="Edit Company Profile" size="lg"
      footer={<>
        <button className="btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
        <button className="btn-primary" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </>}
    >
      <div className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="label">Company Name *</label>
            <input
              value={form.company_name}
              onChange={e => setForm(p => ({...p, company_name: e.target.value}))}
              className="input"
              placeholder="Your Company Name"
            />
          </div>
          <div>
            <label className="label">App Version</label>
            <input
              value={form.app_version}
              onChange={e => setForm(p => ({...p, app_version: e.target.value}))}
              className="input"
              placeholder="e.g. 1.2.0"
            />
          </div>
        </div>

        {/* Logo picker */}
        <LogoPicker
          value={form.company_logo_url}
          onChange={val => setForm(p => ({...p, company_logo_url: val}))}
        />
        <div>
          <label className="label">Company Address</label>
          <input
            value={form.company_address}
            onChange={e => setForm(p => ({...p, company_address: e.target.value}))}
            className="input"
            placeholder="123 Main St, City, Country"
          />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="label">Contact Email</label>
            <input
              value={form.company_email}
              onChange={e => setForm(p => ({...p, company_email: e.target.value}))}
              className="input"
              type="email"
              placeholder="info@company.com"
            />
          </div>
          <div>
            <label className="label">Phone Number</label>
            <input
              value={form.company_phone}
              onChange={e => setForm(p => ({...p, company_phone: e.target.value}))}
              className="input"
              placeholder="+1 (555) 000-0000"
            />
          </div>
        </div>
        <div>
          <label className="label">Website</label>
          <input
            value={form.company_website}
            onChange={e => setForm(p => ({...p, company_website: e.target.value}))}
            className="input"
            placeholder="https://www.company.com"
          />
        </div>
      </div>
    </Modal>
  )
}

export default function AdminPage() {
  const qc = useQueryClient()
  const { company, refresh: refreshCompany } = useCompany()
  const [showCompanyModal, setShowCompanyModal] = useState(false)

  const { data: stats }    = useQuery({ queryKey: ['dashboard-stats'],   queryFn: dashboardApi.stats })
  const { data: pending }  = useQuery({ queryKey: ['approval-count'],    queryFn: approvalsApi.count })
  const { data: audit }    = useQuery({ queryKey: ['audit-admin-stats'], queryFn: () => auditApi.stats() })
  const { data: allSettings } = useQuery({ queryKey: ['system-settings'], queryFn: settingsApi.getAll })

  const appVersion = allSettings?.app_version?.value ?? company?.app_version ?? '1.0.0'

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <div className="flex items-center justify-between mb-1">
          <div className="flex items-center gap-2">
            <HiOutlineShieldCheck className="w-5 h-5 text-primary-600" />
            <h1 className="page-title">Admin Dashboard</h1>
          </div>
          <div className="flex items-center gap-2">
            <span className="badge badge-blue flex items-center gap-1">
              <HiOutlineTag className="w-3 h-3" /> v{appVersion}
            </span>
            <button
              onClick={() => setShowCompanyModal(true)}
              className="btn-secondary btn-sm gap-1.5"
            >
              <HiOutlinePencil className="w-3.5 h-3.5" /> Edit company profile
            </button>
          </div>
        </div>
        <p className="page-subtitle">System overview, health status and quick access to admin tools</p>
      </div>

      {/* Company info banner */}
      {company?.company_name && (
        <div className="card p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-lg bg-primary-50 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0 overflow-hidden">
            {company.company_logo_url
              ? <img src={company.company_logo_url} alt={company.company_name} className="w-full h-full object-contain p-1" />
              : <HiOutlineOfficeBuilding className="w-5 h-5 text-primary-600" />
            }
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-surface-900 dark:text-gray-100 text-sm">{company.company_name}</p>
            <div className="flex flex-wrap gap-3 mt-0.5">
              {company.company_email   && <p className="text-xs text-surface-400 dark:text-gray-500">{company.company_email}</p>}
              {company.company_phone   && <p className="text-xs text-surface-400 dark:text-gray-500">{company.company_phone}</p>}
              {company.company_website && <a href={company.company_website} target="_blank" rel="noreferrer" className="text-xs text-primary-500 hover:underline">{company.company_website}</a>}
            </div>
          </div>
          <span className="badge badge-gray text-[10px]">v{appVersion}</span>
        </div>
      )}

      {/* Key metrics */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total documents', value: stats?.totalDocuments?.toLocaleString() ?? '—', colour: 'bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300', to: '/documents' },
          { label: 'Total users',     value: stats?.totalUsers?.toLocaleString() ?? '—',     colour: 'bg-green-50 text-green-700 dark:bg-green-900/30 dark:text-green-300', to: '/users' },
          { label: 'Pending approvals', value: pending ?? 0,                                colour: pending > 0 ? 'bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' : 'bg-surface-100 text-surface-600 dark:bg-gray-800 dark:text-gray-400', to: '/approvals' },
          { label: 'Storage used',    value: formatFileSize(stats?.storageUsed ?? 0),        colour: 'bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300' },
        ].map(s => (
          <div key={s.label}>
            {s.to ? (
              <Link to={s.to} className="card p-4 flex items-start justify-between hover:shadow-md transition-shadow block">
                <div><p className="text-xs text-surface-500 dark:text-gray-400 mb-1">{s.label}</p><p className="text-2xl font-bold text-surface-900 dark:text-gray-100">{s.value}</p></div>
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${s.colour}`}><HiOutlineDocumentText className="w-4 h-4" /></div>
              </Link>
            ) : (
              <div className="card p-4 flex items-start justify-between">
                <div><p className="text-xs text-surface-500 dark:text-gray-400 mb-1">{s.label}</p><p className="text-2xl font-bold text-surface-900 dark:text-gray-100">{s.value}</p></div>
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${s.colour}`}><HiOutlineDocumentText className="w-4 h-4" /></div>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Top active users */}
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm font-semibold text-surface-800 dark:text-gray-200">Top users — last 30 days</p>
            <Link to="/audit" className="text-xs text-primary-600 hover:text-primary-800">Full audit →</Link>
          </div>
          {audit ? (
            <div className="space-y-2">
              {(audit.topUsers ?? []).slice(0,5).map((u, i) => (
                <div key={i} className="flex items-center gap-3">
                  <span className="w-5 text-xs text-surface-400 dark:text-gray-500 text-right">{i+1}</span>
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-sm text-surface-700 dark:text-gray-300">{u.user}</span>
                      <span className="text-xs text-surface-500 dark:text-gray-400">{u.count} actions</span>
                    </div>
                    <div className="w-full bg-surface-100 dark:bg-gray-800 rounded-full h-1.5">
                      <div className="h-1.5 rounded-full bg-primary-500" style={{ width: `${Math.min(100, (u.count / ((audit.topUsers[0]?.count ?? 1))) * 100)}%` }} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : <div className="flex justify-center py-8"><Spinner /></div>}
        </div>

        {/* Security checklist */}
        <div className="card p-5">
          <p className="text-sm font-semibold text-surface-800 dark:text-gray-200 mb-4">Security checklist</p>
          <div className="space-y-3">
            {SECURITY_CHECKS.map(s => (
              <div key={s.label} className="flex items-center gap-3">
                <div className={clsx('w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0',
                  s.ok ? 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-400' : 'bg-red-100 dark:bg-red-900/40 text-red-600 dark:text-red-400')}>
                  {s.ok ? <HiOutlineCheck className="w-3 h-3" /> : <HiOutlineX className="w-3 h-3" />}
                </div>
                <div>
                  <p className="text-sm text-surface-700 dark:text-gray-300">{s.label}</p>
                  <p className="text-xs text-surface-400 dark:text-gray-500">{s.note}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Quick admin links */}
      <div className="card p-5">
        <p className="section-title mb-4">Admin tools</p>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { to:'/audit',     icon: HiOutlineShieldCheck, label: 'Audit Trail',     desc: 'All system events' },
            { to:'/approvals', icon: HiOutlineUserGroup,   label: 'Approvals',       desc: `${pending ?? 0} pending` },
            { to:'/users',     icon: HiOutlineUserGroup,   label: 'Manage Users',    desc: `${stats?.totalUsers ?? 0} users` },
            { to:'/admin/roles', icon: HiOutlineKey,       label: 'Roles',           desc: 'Permissions matrix' },
            { to:'/settings',  icon: HiOutlineCog,         label: 'System Settings', desc: `Configure ${company?.company_name || 'DMS'}` },
          ].map(l => (
            <Link key={l.to} to={l.to} className="flex flex-col items-center gap-2 p-4 rounded-xl border border-surface-200 dark:border-gray-700 hover:border-primary-300 hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-all text-center">
              <l.icon className="w-6 h-6 text-primary-600" />
              <div><p className="text-sm font-medium text-surface-800 dark:text-gray-200">{l.label}</p><p className="text-xs text-surface-400 dark:text-gray-500">{l.desc}</p></div>
            </Link>
          ))}
        </div>
      </div>

      {/* Company Profile Edit Modal */}
      {showCompanyModal && (
        <CompanyProfileModal
          open={showCompanyModal}
          onClose={() => setShowCompanyModal(false)}
          settings={allSettings}
          onSaved={() => {
            qc.invalidateQueries({ queryKey: ['system-settings'] })
            refreshCompany()
          }}
        />
      )}
    </div>
  )
}
