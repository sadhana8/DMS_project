import { useMemo } from 'react'
import { HiOutlineCheck, HiOutlineX } from 'react-icons/hi'

// Same rules as backend PasswordValidator.java — must stay in sync
const RULES = [
  { key: 'len',     label: 'At least 10 characters',           test: p => p.length >= 10 },
  { key: 'upper',   label: 'One uppercase letter (A-Z)',        test: p => /[A-Z]/.test(p) },
  { key: 'lower',   label: 'One lowercase letter (a-z)',        test: p => /[a-z]/.test(p) },
  { key: 'digit',   label: 'One digit (0-9)',                   test: p => /[0-9]/.test(p) },
  { key: 'special', label: 'One special character (!@#$%^&*…)', test: p => /[!@#$%^&*()\-_+=\[\]{};':"\\|,.<>\/?`~]/.test(p) },
  { key: 'repeat',  label: 'No 4+ repeated characters (aaaa)', test: p => !/(.)\\1{3,}/.test(p) },
  { key: 'maxlen',  label: 'Under 128 characters',             test: p => p.length <= 128 },
]

const COMMON = new Set([
  'password','password1','password123','123456789','1234567890',
  'qwerty','qwerty123','abc123','admin','admin123','letmein',
  'welcome','iloveyou','sunshine','princess','dragon','monkey',
  'pass@123','pass@1234','p@ssword','p@ssw0rd','passw0rd',
  'test1234','test@123','root','root123','1q2w3e4r','qazwsx',
  'india@123','nepal@123','user@123','changeme','hello123',
])

function getScore(password) {
  if (!password) return 0
  const passed = RULES.filter(r => r.test(password)).length
  const common = COMMON.has(password.toLowerCase())
  if (common) return 1
  if (passed <= 2) return 1
  if (passed <= 4) return 2
  if (passed <= 6) return 3
  return 4
}

const SCORE_CONFIG = [
  { label: '',          bars: 0, color: 'bg-gray-200 dark:bg-gray-700' },
  { label: 'Weak',      bars: 1, color: 'bg-red-500' },
  { label: 'Fair',      bars: 2, color: 'bg-amber-500' },
  { label: 'Good',      bars: 3, color: 'bg-yellow-400' },
  { label: 'Strong',    bars: 4, color: 'bg-green-500' },
]

export default function PasswordStrengthMeter({ password, showRules = true }) {
  const results = useMemo(() => RULES.map(r => ({ ...r, passed: r.test(password || '') })), [password])
  const isCommon = COMMON.has((password || '').toLowerCase())
  const score    = getScore(password || '')
  const cfg      = SCORE_CONFIG[score]

  if (!password) return null

  return (
    <div className="mt-2 space-y-2">
      {/* Strength bars */}
      <div className="flex items-center gap-2">
        <div className="flex gap-1 flex-1">
          {[1, 2, 3, 4].map(n => (
            <div key={n} className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${n <= cfg.bars ? cfg.color : 'bg-gray-200 dark:bg-gray-700'}`} />
          ))}
        </div>
        {cfg.label && (
          <span className={`text-xs font-semibold ${score >= 4 ? 'text-green-600 dark:text-green-400' : score === 3 ? 'text-yellow-600 dark:text-yellow-400' : score === 2 ? 'text-amber-600 dark:text-amber-400' : 'text-red-600 dark:text-red-400'}`}>
            {cfg.label}
          </span>
        )}
      </div>

      {/* Common password warning */}
      {isCommon && (
        <p className="text-xs text-red-600 dark:text-red-400 font-medium flex items-center gap-1">
          <HiOutlineX className="w-3.5 h-3.5" />
          This password has been found in data breaches — choose a unique passphrase.
        </p>
      )}

      {/* Rule checklist */}
      {showRules && (
        <div className="grid grid-cols-1 gap-0.5">
          {results.map(r => (
            <div key={r.key} className="flex items-center gap-1.5">
              <div className={`w-3.5 h-3.5 rounded-full flex items-center justify-center flex-shrink-0 ${r.passed ? 'bg-green-100 dark:bg-green-900/40 text-green-600 dark:text-green-400' : 'bg-red-100 dark:bg-red-900/30 text-red-500 dark:text-red-400'}`}>
                {r.passed
                  ? <HiOutlineCheck className="w-2.5 h-2.5" />
                  : <HiOutlineX className="w-2.5 h-2.5" />
                }
              </div>
              <span className={`text-xs ${r.passed ? 'text-surface-500 dark:text-gray-400' : 'text-surface-600 dark:text-gray-300'}`}>{r.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
