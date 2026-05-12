import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/context/AuthContext'
import { usersApi } from '@/api/users'
import { notificationsApi } from '@/api/notifications'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineUser, HiOutlineShieldCheck, HiOutlineBell, HiOutlineEye, HiOutlineEyeOff, HiOutlineLogout, HiOutlineLockClosed } from 'react-icons/hi'
import TwoFactorPage from '@/pages/auth/TwoFactorPage'

function Toggle({ checked, onChange }) {
  return (
    <button type="button" onClick={() => onChange(!checked)}
      className={`toggle ${checked ? 'toggle-on' : 'toggle-off'}`}>
      <span className={`toggle-thumb ${checked ? 'toggle-thumb-on' : 'toggle-thumb-off'}`} />
    </button>
  )
}

export default function ProfilePage() {
  const { user, refreshUser } = useAuth()
  const qc = useQueryClient()
  const [tab,     setTab]     = useState('profile')
  const [showOld, setShowOld] = useState(false)
  const [showNew, setShowNew] = useState(false)

  // Resignation
  const [resignReason,   setResignReason]   = useState('')
  const [resignBusy,     setResignBusy]     = useState(false)

  const onResign = async () => {
    if (!confirm('Submit resignation? Your access will end at the end of this month.')) return
    setResignBusy(true)
    try {
      const r = await usersApi.resignSelf({ reason: resignReason || null })
      toast.success(r?.message || 'Resignation recorded')
      await refreshUser()
      setResignReason('')
    } catch (e) { toast.error(e?.response?.data?.message || 'Failed to record resignation') }
    finally { setResignBusy(false) }
  }

  const profileForm = useForm({ defaultValues: { firstName: user?.firstName ?? '', lastName: user?.lastName ?? '', phoneNumber: user?.phoneNumber ?? '' } })
  const pwForm = useForm()

  const { data: notifSettings = [] } = useQuery({
    queryKey: ['notif-settings'],
    queryFn:  notificationsApi.getSettings,
    enabled:  tab === 'notifications',
  })

  const onProfile = async (data) => {
    try { await usersApi.updateProfile(data); await refreshUser(); toast.success('Profile updated') }
    catch { toast.error('Failed to update profile') }
  }

  const onPassword = async (data) => {
    if (data.newPassword !== data.confirmPassword) { toast.error('Passwords do not match'); return }
    try { await usersApi.changePassword?.(data) ?? toast.error('Not implemented'); pwForm.reset(); toast.success('Password changed') }
    catch (e) { toast.error(e?.response?.data?.message ?? 'Failed') }
  }

  const toggleNotif = async (type, channel, value) => {
    try { await notificationsApi.updateSetting({ type, [channel]: value }); qc.invalidateQueries({ queryKey: ['notif-settings'] }) }
    catch { toast.error('Failed to save') }
  }

  const TABS = [
    { key: 'profile',       label: 'Profile',       icon: HiOutlineUser },
    { key: 'security',      label: 'Security',       icon: HiOutlineShieldCheck },
    { key: 'twofactor',     label: 'Two-factor',     icon: HiOutlineLockClosed },
    { key: 'notifications', label: 'Notifications',  icon: HiOutlineBell },
    { key: 'resign',        label: 'Resignation',    icon: HiOutlineLogout },
  ]

  const initials = user ? `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase() : '?'

  return (
    <div className="animate-fade-in max-w-2xl mx-auto">
      {/* User header */}
      <div className="card p-6 mb-5 flex items-center gap-4">
        <div className="w-16 h-16 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xl font-bold flex-shrink-0">{initials}</div>
        <div>
          <h1 className="text-xl font-semibold text-surface-900">{user?.firstName} {user?.lastName}</h1>
          <p className="text-sm text-surface-500">{user?.email}</p>
          <div className="flex gap-1.5 mt-1.5">
            {user?.roles?.map(r => <span key={r} className="badge badge-blue capitalize">{r.replace('ROLE_','').toLowerCase()}</span>)}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-surface-200 mb-5">
        {TABS.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={clsx('flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
              tab === t.key ? 'border-primary-600 text-primary-700' : 'border-transparent text-surface-500 hover:text-surface-800')}>
            <t.icon className="w-4 h-4" />{t.label}
          </button>
        ))}
      </div>

      {/* Profile tab */}
      {tab === 'profile' && (
        <form onSubmit={profileForm.handleSubmit(onProfile)} className="card p-6 space-y-4">
          <h2 className="section-title">Personal information</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">First name</label>
              <input {...profileForm.register('firstName', { required: true })} className="input" />
            </div>
            <div>
              <label className="label">Last name</label>
              <input {...profileForm.register('lastName', { required: true })} className="input" />
            </div>
          </div>
          <div>
            <label className="label">Email address</label>
            <input value={user?.email ?? ''} disabled className="input" />
            <p className="text-xs text-surface-400 mt-1">Email cannot be changed</p>
          </div>
          <div>
            <label className="label">Phone number <span className="text-surface-400 font-normal">(optional)</span></label>
            <input {...profileForm.register('phoneNumber')} placeholder="+977-9841234567" className="input" />
          </div>
          <div className="pt-2">
            <button type="submit" disabled={profileForm.formState.isSubmitting} className="btn-primary gap-2">
              {profileForm.formState.isSubmitting ? <><Spinner size="sm" /> Saving…</> : 'Save changes'}
            </button>
          </div>
        </form>
      )}

      {/* Security tab */}
      {tab === 'security' && (
        <div className="space-y-5">
          <form onSubmit={pwForm.handleSubmit(onPassword)} className="card p-6 space-y-4">
            <h2 className="section-title">Change password</h2>
            <div>
              <label className="label">Current password</label>
              <div className="relative">
                <input {...pwForm.register('currentPassword', { required: true })} type={showOld ? 'text' : 'password'} className="input pr-10" />
                <button type="button" tabIndex={-1} onClick={() => setShowOld(v=>!v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400">
                  {showOld ? <HiOutlineEyeOff className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div>
              <label className="label">New password</label>
              <div className="relative">
                <input {...pwForm.register('newPassword', { required: true, minLength: 8 })} type={showNew ? 'text' : 'password'} placeholder="Min. 8 characters" className="input pr-10" />
                <button type="button" tabIndex={-1} onClick={() => setShowNew(v=>!v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400">
                  {showNew ? <HiOutlineEyeOff className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div>
              <label className="label">Confirm new password</label>
              <input {...pwForm.register('confirmPassword', { required: true })} type="password" className="input" />
            </div>
            <button type="submit" disabled={pwForm.formState.isSubmitting} className="btn-primary gap-2">
              {pwForm.formState.isSubmitting ? <><Spinner size="sm" /> Changing…</> : 'Change password'}
            </button>
          </form>
          <div className="card p-6">
            <h2 className="section-title mb-4">Active sessions</h2>
            <div className="flex items-center justify-between p-3 bg-green-50 rounded-xl">
              <div>
                <p className="text-sm font-medium text-surface-800">Current session</p>
                <p className="text-xs text-surface-400 mt-0.5">This browser · Active now</p>
              </div>
              <span className="badge badge-green">Active</span>
            </div>
          </div>
        </div>
      )}

      {/* Two-factor tab */}
      {tab === 'twofactor' && (
        <TwoFactorPage />
      )}

      {/* Notifications tab */}
      {tab === 'notifications' && (
        <div className="card overflow-hidden">
          <div className="grid grid-cols-[1fr_auto_auto] items-center gap-4 px-5 py-3 bg-surface-50 border-b border-surface-200 text-xs font-semibold text-surface-500 uppercase tracking-wider">
            <span>Event</span><span className="w-14 text-center">In-app</span><span className="w-14 text-center">Email</span>
          </div>
          {notifSettings.map((s, i) => (
            <div key={s.type} className={`grid grid-cols-[1fr_auto_auto] items-center gap-4 px-5 py-3.5 ${i > 0 ? 'border-t border-surface-100' : ''}`}>
              <div>
                <p className="text-sm font-medium text-surface-800">{s.typeLabel}</p>
                <p className="text-xs text-surface-400 mt-0.5">{s.description}</p>
              </div>
              <div className="w-14 flex justify-center"><Toggle checked={s.inApp ?? true} onChange={v => toggleNotif(s.type, 'inApp', v)} /></div>
              <div className="w-14 flex justify-center"><Toggle checked={s.email ?? true} onChange={v => toggleNotif(s.type, 'email', v)} /></div>
            </div>
          ))}
        </div>
      )}
      {/* Resignation tab */}
      {tab === 'resign' && (
        <div className="card p-6">
          {user?.resignationEffectiveDate ? (
            <div>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-9 h-9 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center">
                  <HiOutlineLogout className="w-5 h-5" />
                </div>
                <h3 className="text-base font-semibold">Resignation already submitted</h3>
              </div>
              <p className="text-sm text-surface-600 mb-2">
                Your resignation was recorded on{' '}
                <strong>{new Date(user.resignationDate).toLocaleString()}</strong>.
              </p>
              <p className="text-sm text-surface-600">
                Your DocVault access will end at end of day on{' '}
                <strong>{new Date(user.resignationEffectiveDate).toLocaleDateString()}</strong>.
              </p>
              <p className="text-xs text-surface-400 mt-3">
                Need to revoke this? Contact an administrator.
              </p>
            </div>
          ) : (
            <div>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-9 h-9 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center">
                  <HiOutlineLogout className="w-5 h-5" />
                </div>
                <h3 className="text-base font-semibold">Submit resignation</h3>
              </div>
              <p className="text-sm text-surface-500 mb-4">
                Once submitted, your DocVault access will be revoked at end of day on
                the last day of the current month. Make sure you've downloaded any
                personal documents before then.
              </p>
              <label className="label">Reason <span className="text-surface-400 font-normal">(optional)</span></label>
              <textarea value={resignReason} onChange={e => setResignReason(e.target.value)}
                placeholder="Anything you'd like HR to know"
                rows={3} className="input mb-4" />
              <button onClick={onResign} disabled={resignBusy}
                className="btn bg-amber-600 text-white hover:bg-amber-700 shadow-sm">
                {resignBusy ? 'Submitting…' : 'Submit resignation'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
