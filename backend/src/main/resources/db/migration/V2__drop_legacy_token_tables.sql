-- ============================================================
-- Migration V2: Drop legacy token tables
--
-- These three tables (refresh_tokens, otp_tokens,
-- password_reset_tokens) have been superseded by the unified
-- app_tokens table (created by Hibernate DDL in V1 / first boot).
--
-- All active data should be migrated to app_tokens before running
-- this migration.  The TokenCleanupScheduler handles expiry.
--
-- Run this ONLY after confirming:
--   1. app_tokens table exists and has rows
--   2. No active application code references the legacy tables
-- ============================================================

-- Migrate any remaining un-expired refresh tokens to app_tokens
INSERT INTO app_tokens (token_value, token_type, user_id, expires_at, is_revoked, is_used, attempts, created_at)
SELECT
    rt.token,
    'REFRESH',
    rt.user_id,
    rt.expires_at,
    COALESCE(rt.is_revoked, false),
    false,
    0,
    COALESCE(rt.created_at, now())
FROM refresh_tokens rt
WHERE rt.expires_at > now()
  AND COALESCE(rt.is_revoked, false) = false
  AND NOT EXISTS (
      SELECT 1 FROM app_tokens at2
      WHERE at2.token_value = rt.token
        AND at2.token_type = 'REFRESH'
  );

-- Migrate any remaining un-used password reset tokens
INSERT INTO app_tokens (token_value, token_type, user_id, expires_at, is_revoked, is_used, attempts, created_at)
SELECT
    prt.token,
    'PASSWORD_RESET',
    prt.user_id,
    prt.expires_at,
    false,
    COALESCE(prt.is_used, false),
    0,
    COALESCE(prt.created_at, now())
FROM password_reset_tokens prt
WHERE prt.expires_at > now()
  AND COALESCE(prt.is_used, false) = false
  AND NOT EXISTS (
      SELECT 1 FROM app_tokens at2
      WHERE at2.token_value = prt.token
        AND at2.token_type = 'PASSWORD_RESET'
  );

-- Migrate any remaining valid OTP tokens
INSERT INTO app_tokens (token_value, token_type, user_id, expires_at, is_revoked, is_used, attempts, created_at)
SELECT
    ot.code,
    CASE ot.purpose
        WHEN 'LOGIN_2FA'        THEN 'OTP_LOGIN_2FA'
        WHEN 'ENABLE_2FA'       THEN 'OTP_ENABLE_2FA'
        WHEN 'SENSITIVE_ACTION' THEN 'OTP_SENSITIVE_ACTION'
        ELSE 'OTP_LOGIN_2FA'
    END,
    ot.user_id,
    ot.expires_at,
    false,
    COALESCE(ot.is_used, false),
    COALESCE(ot.attempts, 0),
    COALESCE(ot.created_at, now())
FROM otp_tokens ot
WHERE ot.expires_at > now()
  AND COALESCE(ot.is_used, false) = false
  AND NOT EXISTS (
      SELECT 1 FROM app_tokens at2
      WHERE at2.token_value = ot.code
        AND at2.user_id = ot.user_id
  );

-- Drop the legacy tables (after data is safely migrated above)
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS otp_tokens CASCADE;
