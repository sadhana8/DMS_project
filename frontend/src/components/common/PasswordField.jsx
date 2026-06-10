import { useState } from 'react'
import { HiOutlineEye, HiOutlineEyeOff } from 'react-icons/hi'
import PasswordStrengthMeter from './PasswordStrengthMeter'
import clsx from 'clsx'

/**
 * Reusable password input field with show/hide toggle.
 * Optionally shows the PasswordStrengthMeter below.
 *
 * Props:
 *  - label         string
 *  - registration  spread result of react-hook-form register()
 *  - error         string | undefined
 *  - watch         string — the watched value for strength meter
 *  - showStrength  boolean — whether to show the strength meter
 *  - placeholder   string
 */
export default function PasswordField({
  label,
  registration = {},
  error,
  watch = '',
  showStrength = false,
  placeholder = '••••••••',
  className = '',
}) {
  const [show, setShow] = useState(false)

  return (
    <div className={className}>
      {label && <label className="label">{label}</label>}
      <div className="relative">
        <input
          {...registration}
          type={show ? 'text' : 'password'}
          placeholder={placeholder}
          className={clsx('input pr-10', error && 'border-red-400 focus:ring-red-400')}
          autoComplete="new-password"
        />
        <button
          type="button"
          onClick={() => setShow(v => !v)}
          tabIndex={-1}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600 dark:text-gray-500 dark:hover:text-gray-300"
        >
          {show ? <HiOutlineEyeOff className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
        </button>
      </div>
      {error && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{error}</p>}
      {showStrength && <PasswordStrengthMeter password={watch} />}
    </div>
  )
}
