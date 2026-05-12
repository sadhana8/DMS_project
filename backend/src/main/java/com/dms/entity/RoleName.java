package com.dms.entity;

/**
 * The four organizational roles. Privilege ordering (highest to lowest):
 * {@code ADMIN > HR > ACCOUNT > EMPLOYEE}.
 *
 * <ul>
 *   <li>{@code ROLE_ADMIN}    — system administrator. Full control: approvals,
 *       settings, audit, role changes, deprecate/restore, hard delete.</li>
 *   <li>{@code ROLE_HR}       — human resources. Sees the user directory and
 *       can manage people-related data; can upload and share documents.</li>
 *   <li>{@code ROLE_ACCOUNT}  — accounts/finance team. Can upload, edit, and
 *       version documents. No admin-level access.</li>
 *   <li>{@code ROLE_EMPLOYEE} — default for self-registered users. Read-only
 *       on documents shared with them; manages own profile.</li>
 * </ul>
 */
public enum RoleName {
    ROLE_ADMIN,
    ROLE_HR,
    ROLE_ACCOUNT,
    ROLE_EMPLOYEE
}
