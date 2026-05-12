import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineFolder, HiOutlineLockClosed, HiOutlineEye, HiOutlineEyeOff, HiOutlineCheckCircle } from 'react-icons/hi'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'

const schema = yup.object({
  password:        yup.string().min(8, 'Min 8 characters').required('Password is required'),
  confirmPassword: yup.string().oneOf([yup.ref('password')], 'Passwords do not match').required('Confirm your password'),
})

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate       = useNavigate()
  const token          = searchParams.get('token')
  const [showPw,  setShowPw]  = useState(false)
  const [done,    setDone]    = useState(false)
  const [apiErr,  setApiErr]  = useState('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: yupResolver(schema) })

  if (!token) return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <p className="text-red-600 mb-4">Invalid or missing reset token.</p>
        <Link to="/forgot-password" className="btn-primary">Request new link</Link>
      </div>
    </div>
  )

  const onSubmit = async ({ password }) => {
    setApiErr('')
    try {
      await authApi.resetPassword({ token, newPassword: password })
      setDone(true)
      toast.success('Password reset successfully!')
    } catch (e) {
      setApiErr(getErrorMessage(e))
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-surface-50 to-primary-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-11 h-11 rounded-2xl bg-primary-600 flex items-center justify-center shadow-lg">
            <HiOutlineFolder className="w-6 h-6 text-white" />
          </div>
          <span className="text-2xl font-bold text-surface-900">DocVault</span>
        </div>

        <div className="card p-8">
          {done ? (
            <div className="text-center py-4">
              <div className="w-14 h-14 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
                <HiOutlineCheckCircle className="w-8 h-8 text-green-600" />
              </div>
              <h2 className="text-lg font-semibold text-surface-900 mb-2">Password reset!</h2>
              <p className="text-sm text-surface-500 mb-6">Your password has been updated. You can now sign in with your new password.</p>
              <button onClick={() => navigate('/login')} className="btn-primary w-full">Go to sign in</button>
            </div>
          ) : (
            <>
              <div className="w-12 h-12 rounded-xl bg-primary-100 flex items-center justify-center mb-5">
                <HiOutlineLockClosed className="w-6 h-6 text-primary-600" />
              </div>
              <h1 className="text-xl font-semibold text-surface-900 mb-1">Set new password</h1>
              <p className="text-sm text-surface-500 mb-6">Must be at least 8 characters long.</p>

              {apiErr && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">{apiErr}</div>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div>
                  <label className="label">New password</label>
                  <div className="relative">
                    <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="Min 8 characters" className="input pr-10" autoFocus />
                    <button type="button" onClick={() => setShowPw(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600">
                      {showPw ? <HiOutlineEyeOff className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
                    </button>
                  </div>
                  {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
                </div>

                <div>
                  <label className="label">Confirm new password</label>
                  <input {...register('confirmPassword')} type={showPw ? 'text' : 'password'} placeholder="Repeat password" className="input" />
                  {errors.confirmPassword && <p className="mt-1 text-xs text-red-600">{errors.confirmPassword.message}</p>}
                </div>

                <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
                  {isSubmitting ? <><Spinner size="sm" /> Resetting…</> : 'Reset password'}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
