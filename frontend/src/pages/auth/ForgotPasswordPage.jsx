import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { authApi } from '@/api/auth'
import { useCompany } from '@/context/CompanyContext'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineOfficeBuilding, HiOutlineMail, HiOutlineCheckCircle } from 'react-icons/hi'
import Spinner from '@/components/common/Spinner'

const schema = yup.object({ email: yup.string().email('Invalid email').required('Email is required') })

export default function ForgotPasswordPage() {
  const [sent,   setSent]   = useState(false)
  const [apiErr, setApiErr] = useState('')
  const { company } = useCompany()
  const companyName = company?.company_name || 'DocVault'
  const logoUrl     = company?.company_logo_url || ''

  const { register, handleSubmit, getValues, formState: { errors, isSubmitting } } = useForm({ resolver: yupResolver(schema) })

  const onSubmit = async ({ email }) => {
    setApiErr('')
    try {
      await authApi.forgotPassword(email)
      setSent(true)
    } catch (e) {
      setApiErr(getErrorMessage(e))
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-surface-50 to-primary-50 dark:from-gray-950 dark:to-gray-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-12 h-12 rounded-2xl bg-primary-600 flex items-center justify-center shadow-lg overflow-hidden">
            {logoUrl
              ? <img src={logoUrl} alt={companyName} className="w-full h-full object-contain p-1" />
              : <HiOutlineOfficeBuilding className="w-6 h-6 text-white" />
            }
          </div>
          <div className="text-left">
            <p className="text-xl font-bold text-surface-900 dark:text-gray-100 leading-tight">{companyName}</p>
            <p className="text-xs text-surface-500 dark:text-gray-400">Document Management System</p>
          </div>
        </div>

        <div className="card p-8">
          {sent ? (
            <div className="text-center py-4">
              <div className="w-14 h-14 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mx-auto mb-4">
                <HiOutlineCheckCircle className="w-8 h-8 text-green-600 dark:text-green-400" />
              </div>
              <h2 className="text-lg font-semibold text-surface-900 dark:text-gray-100 mb-2">Check your email</h2>
              <p className="text-sm text-surface-500 dark:text-gray-400 mb-6">
                We've sent a password reset link to <strong>{getValues('email')}</strong>. Check your inbox and follow the instructions from <strong>{companyName}</strong>.
              </p>
              <Link to="/login" className="btn-primary w-full inline-flex items-center justify-center">Back to sign in</Link>
            </div>
          ) : (
            <>
              <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center mb-5">
                <HiOutlineMail className="w-6 h-6 text-primary-600 dark:text-primary-400" />
              </div>
              <h1 className="text-xl font-semibold text-surface-900 dark:text-gray-100 mb-1">Forgot password?</h1>
              <p className="text-sm text-surface-500 dark:text-gray-400 mb-6">
                Enter your email and {companyName} will send you a reset link.
              </p>

              {apiErr && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-sm text-red-700 dark:text-red-400">{apiErr}</div>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div>
                  <label className="label">Email address</label>
                  <input {...register('email')} type="email" placeholder="you@company.com" className="input" autoFocus />
                  {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
                </div>
                <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
                  {isSubmitting ? <><Spinner size="sm" /> Sending…</> : 'Send reset link'}
                </button>
              </form>
            </>
          )}
        </div>

        <p className="text-center text-sm text-surface-500 dark:text-gray-400 mt-6">
          Remember your password?{' '}
          <Link to="/login" className="text-primary-600 font-medium hover:text-primary-700">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
