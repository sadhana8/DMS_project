import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { useAuth } from '@/context/AuthContext'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineFolder, HiOutlineEye, HiOutlineEyeOff } from 'react-icons/hi'
import Spinner from '@/components/common/Spinner'

const schema = yup.object({
  email:    yup.string().email('Invalid email').required('Email is required'),
  password: yup.string().required('Password is required'),
})

export default function LoginPage() {
  const { login }    = useAuth()
  const navigate     = useNavigate()
  const location     = useLocation()
  const from         = location.state?.from?.pathname ?? '/dashboard'
  const [showPw, setShowPw] = useState(false)
  const [apiErr, setApiErr] = useState('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: yupResolver(schema) })

  const onSubmit = async (data) => {
    setApiErr('')
    try {
      await login(data)
      navigate(from, { replace: true })
    } catch (e) {
      setApiErr(getErrorMessage(e))
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-surface-50 to-primary-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-11 h-11 rounded-2xl bg-primary-600 flex items-center justify-center shadow-lg">
            <HiOutlineFolder className="w-6 h-6 text-white" />
          </div>
          <span className="text-2xl font-bold text-surface-900">DocVault</span>
        </div>

        <div className="card p-8">
          <h1 className="text-xl font-semibold text-surface-900 mb-1">Welcome back</h1>
          <p className="text-sm text-surface-500 mb-6">Sign in to your account to continue</p>

          {apiErr && (
            <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">{apiErr}</div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="label">Email address</label>
              <input {...register('email')} type="email" placeholder="you@company.com" className="input" autoFocus />
              {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="label !mb-0">Password</label>
                <Link to="/forgot-password" className="text-xs text-primary-600 hover:text-primary-700 font-medium">Forgot password?</Link>
              </div>
              <div className="relative">
                <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="••••••••" className="input pr-10" />
                <button type="button" onClick={() => setShowPw(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600">
                  {showPw ? <HiOutlineEyeOff className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full mt-2">
              {isSubmitting ? <><Spinner size="sm" /> Signing in…</> : 'Sign in'}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-surface-500 mt-6">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary-600 font-medium hover:text-primary-700">Create account</Link>
        </p>
      </div>
    </div>
  )
}
