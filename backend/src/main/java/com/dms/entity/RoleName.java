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
    ROLE_EMPLOYEE,

    /**
     * Department / Team Manager. Can view team members, manage department
     * documents, and initiate approval requests. Cannot access system-level
     * admin pages.
     */
    ROLE_MANAGER,

    /**
     * Finance team member. Focused on financial documents: invoices, bills,
     * expense reports. Can upload and share within the finance domain.
     */
    ROLE_FINANCE,

    /**
     * Legal team member. Handles contracts, policies, and compliance
     * documents. Read-write on legal-category files.
     */
    ROLE_LEGAL,

    /**
     * Reviewer / Approver. Can review and approve documents assigned to them.
     * Cannot upload, delete, or manage users.
     */
    ROLE_REVIEWER
}
