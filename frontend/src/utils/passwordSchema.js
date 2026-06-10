import * as yup from 'yup'

// Must stay in sync with backend PasswordValidator.java
const COMMON_PASSWORDS = new Set([
  'password','password1','password123','123456789','1234567890',
  'qwerty','qwerty123','abc123','admin','admin123','letmein',
  'welcome','iloveyou','sunshine','princess','dragon','monkey',
  'pass@123','pass@1234','p@ssword','p@ssw0rd','passw0rd',
  'test1234','test@123','root','root123','1q2w3e4r',
  'india@123','nepal@123','user@123','changeme','hello123',
])

export const passwordRules = yup.string()
  .required('Password is required')
  .min(10, 'Must be at least 10 characters')
  .max(128, 'Must not exceed 128 characters')
  .matches(/[A-Z]/, 'Must contain at least one uppercase letter')
  .matches(/[a-z]/, 'Must contain at least one lowercase letter')
  .matches(/[0-9]/, 'Must contain at least one digit')
  .matches(/[!@#$%^&*()\-_+=\[\]{};':"\\|,.<>\/?`~]/, 'Must contain at least one special character')
  .test('no-repeat', 'Must not contain 4+ repeated characters', v => v && !/(.)\\1{3,}/.test(v))
  .test('not-common', 'This password is too common — choose a unique passphrase', v => !COMMON_PASSWORDS.has((v || '').toLowerCase()))
