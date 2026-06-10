import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { authApi } from '@/api/auth'
import { useCompany } from '@/context/CompanyContext'
import { passwordRules } from '@/utils/passwordSchema'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineOfficeBuilding, HiOutlineLockClosed, HiOutlineCheckCircle } from 'react-icons/hi'
import Spinner from '@/components/common/Spinner'
import PasswordField from '@/components/common/PasswordField'
import toast from 'react-hot-toast'

const schema = yup.object({
  password:        passwordRules,
  confirmPassword: yup.string().oneOf([yup.ref('password')], 'Passwords do not match').required('Confirm your password'),
})

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate       = useNavigate()
  const token          = searchParams.get('token')
  const { company }    = useCompany()
  const companyName    = company?.company_name || 'DocVault'
  const logoUrl        = company?.company_logo_url || ''
  const [done,    setDone]    = useState(false)
  const [apiErr,  setApiErr]  = useState('')

  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm({ resolver: yupResolver(schema) })
  const pwWatch = watch('password', '')

  if (!token) return (
    <div className="min-h-screen flex items-center justify-center dark:bg-gray-950">
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
    } catch (e) { setApiErr(getErrorMessage(e)) }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-surface-50 to-primary-50 dark:from-gray-950 dark:to-gray-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-12 h-12 rounded-2xl bg-primary-600 flex items-center justify-center shadow-lg overflow-hidden">
            {logoUrl ? <img src={logoUrl} alt={companyName} className="w-full h-full object-contain p-1" />
              : <HiOutlineOfficeBuilding className="w-6 h-6 text-white" />}
          </div>
          <div>
            <p className="text-xl font-bold text-surface-900 dark:text-gray-100">{companyName}</p>
            <p className="text-xs text-surface-500 dark:text-gray-400">Document Management System</p>
          </div>
        </div>

        <div className="card p-8">
          {done ? (
            <div className="text-center py-4">
              <div className="w-14 h-14 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mx-auto mb-4">
                <HiOutlineCheckCircle className="w-8 h-8 text-green-600 dark:text-green-400" />
              </div>
              <h2 className="text-lg font-semibold text-surface-900 dark:text-gray-100 mb-2">Password reset!</h2>
              <p className="text-sm text-surface-500 dark:text-gray-400 mb-6">Your {companyName} password has been updated.</p>
              <button onClick={() => navigate('/login')} className="btn-primary w-full">Go to sign in</button>
            </div>
          ) : (
            <>
              <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center mb-5">
                <HiOutlineLockClosed className="w-6 h-6 text-primary-600 dark:text-primary-400" />
              </div>
              <h1 className="text-xl font-semibold text-surface-900 dark:text-gray-100 mb-1">Set new password</h1>
              <p className="text-sm text-surface-500 dark:text-gray-400 mb-6">
                Choose a strong password for your {companyName} account.
              </p>

              {apiErr && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-sm text-red-700 dark:text-red-400">{apiErr}</div>}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <PasswordField
                  label="New password"
                  registration={register('password')}
                  error={errors.password?.message}
                  watch={pwWatch}
                  showStrength
                  placeholder="Min. 10 characters"
                />
                <PasswordField
                  label="Confirm new password"
                  registration={register('confirmPassword')}
                  error={errors.confirmPassword?.message}
                  placeholder="Repeat password"
                />
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
