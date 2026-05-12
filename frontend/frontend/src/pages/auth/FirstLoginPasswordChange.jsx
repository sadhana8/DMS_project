import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { authApi } from '@/api/auth'
import { useAuth } from '@/context/AuthContext'
import toast from 'react-hot-toast'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineLockClosed, HiOutlineEye, HiOutlineEyeOff } from 'react-icons/hi'

/**
 * Shown immediately after first login of an admin-created account.
 * The user has just authenticated with a temp password; they must set a
 * new one before they can use the rest of the app.
 *
 * No "current password" field — the temp one was emailed and is single-use.
 */
export default function FirstLoginPasswordChange() {
  const { logout } = useAuth()
  const navigate   = useNavigate()
  const [show, setShow]       = useState(false)
  const [busy, setBusy]       = useState(false)
  const [apiErr, setApiErr]   = useState('')
  const { register, handleSubmit, watch, formState: { errors } } = useForm()

  const onSubmit = async (data) => {
    setApiErr('')
    if (data.newPassword !== data.confirm) {
      setApiErr('Passwords do not match')
      return
    }
    setBusy(true)
    try {
      await authApi.firstLoginPasswordChange(data.newPassword)
      toast.success('Password updated. Please log in with your new password.')
      // Backend revokes refresh tokens — clear our session and force re-login
      await logout()
      navigate('/login', { replace: true })
    } catch (e) {
      setApiErr(getErrorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-surface-50 to-primary-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="card p-8">
          <div className="flex items-center gap-2 mb-2">
            <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
              <HiOutlineLockClosed className="w-5 h-5" />
            </div>
            <h1 className="text-xl font-bold">Set your password</h1>
          </div>
          <p className="text-sm text-surface-500 mb-6">
            For security, you must change the temporary password that was emailed to you.
            Pick something only you know.
          </p>

          {apiErr && (
            <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
              {apiErr}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="label text-xs">New password</label>
              <div className="relative">
                <input
                  type={show ? 'text' : 'password'}
                  className="input pr-10"
                  placeholder="At least 8 characters"
                  {...register('newPassword', { required: 'Required', minLength: { value: 8, message: 'At least 8 characters' } })}
                />
                <button type="button" onClick={() => setShow(s => !s)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600">
                  {show ? <HiOutlineEyeOff className="w-5 h-5" /> : <HiOutlineEye className="w-5 h-5" />}
                </button>
              </div>
              {errors.newPassword && <p className="text-xs text-red-600 mt-1">{errors.newPassword.message}</p>}
            </div>

            <div>
              <label className="label text-xs">Confirm new password</label>
              <input
                type={show ? 'text' : 'password'}
                className="input"
                placeholder="Type it again"
                {...register('confirm', { required: 'Required' })}
              />
              {errors.confirm && <p className="text-xs text-red-600 mt-1">{errors.confirm.message}</p>}
            </div>

            <button type="submit" disabled={busy} className="btn-primary w-full">
              {busy ? 'Updating…' : 'Set password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
