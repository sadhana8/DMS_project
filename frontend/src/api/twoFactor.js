import client from './client'

/**
 * Two-factor / OTP API. Mirrors backend `TwoFactorController` at /2fa/*.
 *
 * Purpose values: LOGIN_2FA | ENABLE_2FA | SENSITIVE_ACTION
 */
export const twoFactorApi = {
  status:        () => client.get('/2fa/status').then(r => r.data),

  sendOtp:       (purpose) =>
    client.post('/2fa/send-otp', purpose ? { purpose } : {}).then(r => r.data),

  verify:        (code, purpose) =>
    client.post('/2fa/verify', { code, purpose }).then(r => r.data),

  startEnable:   () => client.post('/2fa/enable/start').then(r => r.data),

  confirmEnable: (code) =>
    client.post('/2fa/enable/confirm', { code, purpose: 'ENABLE_2FA' }).then(r => r.data),

  disable:       (code) =>
    client.post('/2fa/disable', { code, purpose: 'SENSITIVE_ACTION' }).then(r => r.data),
}
