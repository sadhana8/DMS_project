import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { authApi } from '@/api/auth'
import { useAuth } from '@/context/AuthContext'
import { passwordRules } from '@/utils/passwordSchema'
import toast from 'react-hot-toast'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineLockClosed } from 'react-icons/hi'
import PasswordField from '@/components/common/PasswordField'
import Spinner from '@/components/common/Spinner'

const schema = yup.object({
  newPassword: passwordRules,
  confirm:     yup.string().oneOf([yup.ref('newPassword')], 'Passwords do not match').required('Confirm your password'),
})

export default function FirstLoginPasswordChange() {
  const { logout } = useAuth()
  const navigate   = useNavigate()
  const [apiErr, setApiErr] = useState('')

  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm({ resolver: yupResolver(schema) })
  const pwWatch = watch('newPassword', '')

  const onSubmit = async (data) => {
    setApiErr('')
    try {
      await authApi.firstLoginPasswordChange(data.newPassword)
      toast.success('Password updated. Please sign in with your new password.')
      await logout()
      navigate('/login', { replace: true })
    } catch (e) { setApiErr(getErrorMessage(e)) }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-surface-50 to-primary-50 dark:from-gray-950 dark:to-gray-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="card p-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-11 h-11 rounded-xl bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center flex-shrink-0">
              <HiOutlineLockClosed className="w-5 h-5 text-amber-700 dark:text-amber-400" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-surface-900 dark:text-gray-100">Set your password</h1>
              <p className="text-xs text-surface-500 dark:text-gray-400">One-time setup required</p>
            </div>
          </div>

          <p className="text-sm text-surface-500 dark:text-gray-400 mb-6">
            For your security, you must change the temporary password that was emailed to you before you can continue.
          </p>

          {apiErr && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-sm text-red-700 dark:text-red-400">{apiErr}</div>}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <PasswordField
              label="New password"
              registration={register('newPassword')}
              error={errors.newPassword?.message}
              watch={pwWatch}
              showStrength
              placeholder="Min. 10 characters"
            />
            <PasswordField
              label="Confirm new password"
              registration={register('confirm')}
              error={errors.confirm?.message}
              placeholder="Repeat password"
            />
            <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
              {isSubmitting ? <><Spinner size="sm" /> Setting password…</> : 'Set password & sign in'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
