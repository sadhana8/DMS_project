import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { twoFactorApi } from '@/api/twoFactor'
import { useAuth } from '@/context/AuthContext'
import Spinner from '@/components/common/Spinner'
import { formatDateTime, getErrorMessage } from '@/utils/helpers'
import toast from 'react-hot-toast'
import {
  HiOutlineShieldCheck, HiOutlineShieldExclamation,
  HiOutlineMail, HiOutlineKey, HiOutlineCheck,
} from 'react-icons/hi'
import clsx from 'clsx'

/**
 * Two-factor authentication settings. Drives the OTP flow exposed by
 * the backend `TwoFactorController`. Email-based 6-digit codes valid
 * for 10 minutes.
 *
 * Flow:
 *   Disabled → start enable → email OTP → confirm code → enabled
 *   Enabled  → request OTP → enter code → 2FA disabled
 */
export default function TwoFactorPage() {
  const { user } = useAuth()
  const qc = useQueryClient()

  const [stage,    setStage]    = useState('idle') // idle | awaitingEnable | awaitingDisable
  const [code,     setCode]     = useState('')
  const [busy,     setBusy]     = useState(false)

  const { data: status, isLoading } = useQuery({
    queryKey: ['2fa-status'],
    queryFn:  twoFactorApi.status,
  })

  const enabled = !!status?.enabled

  const startEnable = async () => {
    setBusy(true)
    try {
      await twoFactorApi.startEnable()
      setStage('awaitingEnable')
      toast.success('Verification code sent to your email')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setBusy(false) }
  }

  const confirmEnable = async () => {
    if (code.length < 4) { toast.error('Enter the 6-digit code'); return }
    setBusy(true)
    try {
      await twoFactorApi.confirmEnable(code)
      toast.success('Two-factor authentication enabled')
      setCode(''); setStage('idle')
      qc.invalidateQueries({ queryKey: ['2fa-status'] })
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setBusy(false) }
  }

  const startDisable = async () => {
    setBusy(true)
    try {
      await twoFactorApi.sendOtp('SENSITIVE_ACTION')
      setStage('awaitingDisable')
      toast.success('Verification code sent to your email')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setBusy(false) }
  }

  const confirmDisable = async () => {
    if (code.length < 4) { toast.error('Enter the 6-digit code'); return }
    setBusy(true)
    try {
      await twoFactorApi.disable(code)
      toast.success('Two-factor authentication disabled')
      setCode(''); setStage('idle')
      qc.invalidateQueries({ queryKey: ['2fa-status'] })
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setBusy(false) }
  }

  if (isLoading) {
    return <div className="flex justify-center py-16"><Spinner size="lg" /></div>
  }

  return (
    <div className="space-y-5">
      {/* Status banner */}
      <div className={clsx(
        'card p-5 flex items-start gap-4',
        enabled ? 'bg-green-50 border-green-200' : 'bg-amber-50 border-amber-200'
      )}>
        <div className={clsx(
          'w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0',
          enabled ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
        )}>
          {enabled
            ? <HiOutlineShieldCheck className="w-5 h-5" />
            : <HiOutlineShieldExclamation className="w-5 h-5" />}
        </div>
        <div className="flex-1">
          <p className={clsx(
            'font-semibold',
            enabled ? 'text-green-900' : 'text-amber-900'
          )}>
            Two-factor authentication is {enabled ? 'enabled' : 'disabled'}
          </p>
          <p className={clsx(
            'text-sm mt-0.5',
            enabled ? 'text-green-800' : 'text-amber-800'
          )}>
            {enabled
              ? `Sensitive actions require a 6-digit code emailed to ${user?.email}.`
              : 'Add an extra layer of security to your account by enabling email-based 2FA.'}
          </p>
          {enabled && status?.enabledAt && (
            <p className="text-xs text-green-700 mt-1">
              Enabled on {formatDateTime(status.enabledAt)}
            </p>
          )}
        </div>
      </div>

      {/* How it works */}
      <div className="card p-5">
        <h3 className="section-title">How it works</h3>
        <ul className="space-y-3 text-sm text-surface-700 dark:text-gray-300">
          <li className="flex gap-3">
            <div className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 text-xs font-bold flex items-center justify-center flex-shrink-0">1</div>
            <div><b>Sign in normally</b> with your email and password.</div>
          </li>
          <li className="flex gap-3">
            <div className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 text-xs font-bold flex items-center justify-center flex-shrink-0">2</div>
            <div><b>Receive a 6-digit code</b> by email when performing sensitive actions.</div>
          </li>
          <li className="flex gap-3">
            <div className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 text-xs font-bold flex items-center justify-center flex-shrink-0">3</div>
            <div><b>Enter the code</b> to confirm. Codes expire after 10 minutes.</div>
          </li>
        </ul>
      </div>

      {/* Enable flow */}
      {!enabled && stage === 'idle' && (
        <div className="card p-5">
          <h3 className="section-title">Enable two-factor authentication</h3>
          <p className="text-sm text-surface-600 dark:text-gray-400 mb-4">
            We'll send a 6-digit code to <b>{user?.email}</b>. Enter it on the next step to turn on 2FA.
          </p>
          <button onClick={startEnable} disabled={busy} className="btn-primary gap-2">
            {busy ? <Spinner size="sm" /> : <HiOutlineMail className="w-4 h-4" />}
            Send verification code
          </button>
        </div>
      )}

      {!enabled && stage === 'awaitingEnable' && (
        <CodeForm
          title="Enter verification code"
          subtitle={`We sent a 6-digit code to ${user?.email}. The code expires in 10 minutes.`}
          code={code} setCode={setCode} busy={busy}
          onConfirm={confirmEnable}
          onCancel={() => { setStage('idle'); setCode('') }}
          onResend={startEnable}
          confirmLabel="Enable 2FA"
          confirmIcon={HiOutlineCheck}
        />
      )}

      {/* Disable flow */}
      {enabled && stage === 'idle' && (
        <div className="card p-5">
          <h3 className="section-title text-red-700">Disable two-factor authentication</h3>
          <p className="text-sm text-surface-600 dark:text-gray-400 mb-4">
            Disabling 2FA reduces your account security. We'll email a verification code to confirm it's really you.
          </p>
          <button onClick={startDisable} disabled={busy} className="btn-danger gap-2">
            {busy ? <Spinner size="sm" /> : <HiOutlineKey className="w-4 h-4" />}
            Send code to disable
          </button>
        </div>
      )}

      {enabled && stage === 'awaitingDisable' && (
        <CodeForm
          title="Confirm to disable 2FA"
          subtitle={`Enter the 6-digit code sent to ${user?.email}`}
          code={code} setCode={setCode} busy={busy}
          onConfirm={confirmDisable}
          onCancel={() => { setStage('idle'); setCode('') }}
          onResend={startDisable}
          confirmLabel="Disable 2FA"
          danger
        />
      )}

      {/* Last verified */}
      {enabled && status?.lastVerifiedAt && (
        <p className="text-xs text-surface-400 dark:text-gray-500 text-center">
          Last verification: {formatDateTime(status.lastVerifiedAt)}
        </p>
      )}
    </div>
  )
}

function CodeForm({ title, subtitle, code, setCode, busy, onConfirm, onCancel, onResend, confirmLabel, confirmIcon: Icon, danger }) {
  return (
    <div className="card p-5">
      <h3 className="section-title">{title}</h3>
      <p className="text-sm text-surface-600 dark:text-gray-400 mb-4">{subtitle}</p>

      <input
        type="text" inputMode="numeric" autoFocus
        value={code}
        onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 8))}
        placeholder="000000"
        className="input text-center text-2xl tracking-[0.5em] font-mono mb-4"
        maxLength={8}
      />

      <div className="flex items-center gap-2">
        <button onClick={onConfirm} disabled={busy || code.length < 4}
          className={clsx(danger ? 'btn-danger' : 'btn-primary', 'gap-2')}>
          {busy ? <Spinner size="sm" /> : Icon ? <Icon className="w-4 h-4" /> : null}
          {confirmLabel}
        </button>
        <button onClick={onCancel} className="btn-secondary">Cancel</button>
        <button onClick={onResend} disabled={busy} className="btn-ghost ml-auto text-xs">
          Resend code
        </button>
      </div>
    </div>
  )
}
