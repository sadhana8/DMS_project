import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { useAuth } from '@/context/AuthContext'
import { useCompany } from '@/context/CompanyContext'
import { getErrorMessage } from '@/utils/helpers'
import { HiOutlineOfficeBuilding, HiOutlineEye, HiOutlineEyeOff } from 'react-icons/hi'
import Spinner from '@/components/common/Spinner'

const schema = yup.object({
  firstName: yup.string().required('First name is required'),
  lastName:  yup.string().required('Last name is required'),
  username:  yup.string().min(3, 'Min 3 characters').required('Username is required'),
  email:     yup.string().email('Invalid email').required('Email is required'),
  password:  yup.string().min(8, 'Min 8 characters').required('Password is required'),
  confirmPassword: yup.string().oneOf([yup.ref('password')], 'Passwords do not match').required('Confirm your password'),
})

export default function RegisterPage() {
  const { register: authRegister } = useAuth()
  const { company } = useCompany()
  const navigate  = useNavigate()
  const [showPw,  setShowPw]  = useState(false)
  const [apiErr,  setApiErr]  = useState('')

  const companyName = company?.company_name || 'DocVault'
  const logoUrl     = company?.company_logo_url || ''

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: yupResolver(schema) })

  const onSubmit = async (data) => {
    setApiErr('')
    try {
      const { confirmPassword, ...payload } = data
      const result = await authRegister(payload)
      if (result?.pending) {
        navigate('/login', { state: { pendingMessage: result.message } })
      } else {
        navigate('/dashboard')
      }
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
          <h1 className="text-xl font-semibold text-surface-900 dark:text-gray-100 mb-1">Create your account</h1>
          <p className="text-sm text-surface-500 dark:text-gray-400 mb-6">Join {companyName} to manage your documents securely</p>

          {apiErr && (
            <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-sm text-red-700 dark:text-red-400">{apiErr}</div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">First name</label>
                <input {...register('firstName')} placeholder="John" className="input" />
                {errors.firstName && <p className="mt-1 text-xs text-red-600">{errors.firstName.message}</p>}
              </div>
              <div>
                <label className="label">Last name</label>
                <input {...register('lastName')} placeholder="Doe" className="input" />
                {errors.lastName && <p className="mt-1 text-xs text-red-600">{errors.lastName.message}</p>}
              </div>
            </div>

            <div>
              <label className="label">Username</label>
              <input {...register('username')} placeholder="johndoe" className="input" />
              {errors.username && <p className="mt-1 text-xs text-red-600">{errors.username.message}</p>}
            </div>

            <div>
              <label className="label">Email address</label>
              <input {...register('email')} type="email" placeholder="you@company.com" className="input" />
              {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="Min 8 characters" className="input pr-10" />
                <button type="button" onClick={() => setShowPw(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600 dark:text-gray-500 dark:hover:text-gray-300">
                  {showPw ? <HiOutlineEyeOff className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
            </div>

            <div>
              <label className="label">Confirm password</label>
              <input {...register('confirmPassword')} type={showPw ? 'text' : 'password'} placeholder="Repeat password" className="input" />
              {errors.confirmPassword && <p className="mt-1 text-xs text-red-600">{errors.confirmPassword.message}</p>}
            </div>

            <div>
              <label className="label">Department</label>
              <select {...register('department')} className="input" defaultValue="OTHER">
                <option value="HR">HR</option>
                <option value="ACCOUNT">Account</option>
                <option value="ENGINEERING">Engineering</option>
                <option value="SALES">Sales</option>
                <option value="OPERATIONS">Operations</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full mt-2">
              {isSubmitting ? <><Spinner size="sm" /> Creating account…</> : 'Create account'}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-surface-500 dark:text-gray-400 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-primary-600 font-medium hover:text-primary-700">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
