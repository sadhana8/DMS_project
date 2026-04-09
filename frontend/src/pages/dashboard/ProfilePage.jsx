import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/context/AuthContext'
import { usersApi } from '@/api/users'
import { authApi } from '@/api/auth'
import { getRoleBadge, formatDateTime, getErrorMessage } from '@/utils/helpers'
import { Avatar } from '@/components/common/index'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineUser, HiOutlineLockClosed, HiOutlineShieldCheck } from 'react-icons/hi'

const profileSchema = yup.object({
  firstName:   yup.string().required('Required'),
  lastName:    yup.string().required('Required'),
  phoneNumber: yup.string().nullable(),
})

const pwSchema = yup.object({
  currentPassword:    yup.string().required('Required'),
  newPassword:        yup.string().min(8, 'Min 8 characters').required('Required'),
  confirmNewPassword: yup.string().oneOf([yup.ref('newPassword')], 'Passwords do not match').required('Required'),
})

const TABS = [
  { id: 'profile',  label: 'Profile',   icon: HiOutlineUser },
  { id: 'security', label: 'Security',  icon: HiOutlineLockClosed },
  { id: 'roles',    label: 'Roles',     icon: HiOutlineShieldCheck },
]

export default function ProfilePage() {
  const { user, refreshUser } = useAuth()
  const qc   = useQueryClient()
  const [tab, setTab] = useState('profile')

  // Profile form
  const { register: regProf, handleSubmit: handleProf, formState: { errors: errProf, isSubmitting: subProf } } =
    useForm({ resolver: yupResolver(profileSchema), defaultValues: { firstName: user?.firstName, lastName: user?.lastName, phoneNumber: user?.phoneNumber } })

  // Password form
  const { register: regPw, handleSubmit: handlePw, reset: resetPw, formState: { errors: errPw, isSubmitting: subPw } } =
    useForm({ resolver: yupResolver(pwSchema) })

  const updateProfile = async (data) => {
    try {
      await usersApi.updateProfile(data)
      await refreshUser()
      toast.success('Profile updated!')
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  const changePassword = async ({ currentPassword, newPassword }) => {
    try {
      await authApi.changePassword({ currentPassword, newPassword })
      resetPw()
      toast.success('Password changed!')
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  return (
    <div className="max-w-2xl mx-auto animate-fade-in">
      <div className="mb-6">
        <h1 className="page-title">Account Settings</h1>
        <p className="page-subtitle">Manage your profile and security preferences</p>
      </div>

      {/* User card */}
      <div className="card p-6 mb-6 flex items-center gap-4">
        <Avatar user={user} size="xl" />
        <div>
          <p className="text-lg font-semibold text-surface-900">{user?.firstName} {user?.lastName}</p>
          <p className="text-sm text-surface-500">{user?.email}</p>
          <div className="flex gap-1.5 mt-2 flex-wrap">
            {user?.roles?.map(r => {
              const { label, color } = getRoleBadge(r)
              return <span key={r} className={color}>{label}</span>
            })}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-surface-200 mb-6">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={clsx('flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
              tab === id ? 'border-primary-600 text-primary-700' : 'border-transparent text-surface-500 hover:text-surface-800')}>
            <Icon className="w-4 h-4" /> {label}
          </button>
        ))}
      </div>

      {/* Profile tab */}
      {tab === 'profile' && (
        <div className="card p-6">
          <h2 className="text-base font-semibold mb-5">Personal information</h2>
          <form onSubmit={handleProf(updateProfile)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">First name</label>
                <input {...regProf('firstName')} className="input" />
                {errProf.firstName && <p className="mt-1 text-xs text-red-600">{errProf.firstName.message}</p>}
              </div>
              <div>
                <label className="label">Last name</label>
                <input {...regProf('lastName')} className="input" />
                {errProf.lastName && <p className="mt-1 text-xs text-red-600">{errProf.lastName.message}</p>}
              </div>
            </div>
            <div>
              <label className="label">Email address</label>
              <input value={user?.email} disabled className="input" />
              <p className="mt-1 text-xs text-surface-400">Email cannot be changed. Contact admin.</p>
            </div>
            <div>
              <label className="label">Phone number</label>
              <input {...regProf('phoneNumber')} placeholder="+1 (555) 000-0000" className="input" />
            </div>
            <div className="pt-2">
              <button type="submit" disabled={subProf} className="btn-primary">
                {subProf ? <><Spinner size="sm" /> Saving…</> : 'Save changes'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Security tab */}
      {tab === 'security' && (
        <div className="card p-6">
          <h2 className="text-base font-semibold mb-5">Change password</h2>
          <form onSubmit={handlePw(changePassword)} className="space-y-4">
            <div>
              <label className="label">Current password</label>
              <input {...regPw('currentPassword')} type="password" className="input" />
              {errPw.currentPassword && <p className="mt-1 text-xs text-red-600">{errPw.currentPassword.message}</p>}
            </div>
            <div>
              <label className="label">New password</label>
              <input {...regPw('newPassword')} type="password" placeholder="Min 8 characters" className="input" />
              {errPw.newPassword && <p className="mt-1 text-xs text-red-600">{errPw.newPassword.message}</p>}
            </div>
            <div>
              <label className="label">Confirm new password</label>
              <input {...regPw('confirmNewPassword')} type="password" className="input" />
              {errPw.confirmNewPassword && <p className="mt-1 text-xs text-red-600">{errPw.confirmNewPassword.message}</p>}
            </div>
            <div className="pt-2">
              <button type="submit" disabled={subPw} className="btn-primary">
                {subPw ? <><Spinner size="sm" /> Updating…</> : 'Update password'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Roles tab */}
      {tab === 'roles' && (
        <div className="card p-6">
          <h2 className="text-base font-semibold mb-5">My roles & permissions</h2>
          <div className="space-y-3">
            {user?.roles?.map(r => {
              const { label, color } = getRoleBadge(r)
              const descriptions = {
                ROLE_ADMIN:   'Full system access — manage users, all documents, and system settings.',
                ROLE_MANAGER: 'Can upload, share, approve documents, and manage team members.',
                ROLE_EDITOR:  'Can upload, edit, and comment on documents.',
                ROLE_VIEWER:  'Read-only access — can view and download documents.',
              }
              return (
                <div key={r} className="flex items-start gap-3 p-4 rounded-xl bg-surface-50 border border-surface-100">
                  <span className={clsx(color, 'mt-0.5')}>{label}</span>
                  <p className="text-sm text-surface-600">{descriptions[r] ?? r}</p>
                </div>
              )
            })}
          </div>
          <p className="text-xs text-surface-400 mt-4">To change your roles, contact an administrator.</p>
        </div>
      )}
    </div>
  )
}
