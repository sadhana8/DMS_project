package com.dms.entity;

/**
 * Enumeration of all role names recognised by the DocVault permission system.
 *
 * <p>
 * The {@code ROLE_} prefix is mandatory for Spring Security's
 * {@code hasRole()} / {@code hasAnyRole()} expression shortcuts, which strip
 * the prefix when comparing against {@code @PreAuthorize("hasRole('ADMIN')")}
 * annotations.
 *
 * <h2>Role hierarchy (loosely enforced)</h2>
 * <pre>
 * ROLE_ADMIN
 *   └─ ROLE_MANAGER
 *        └─ ROLE_EDITOR
 *             └─ ROLE_VIEWER
 * </pre> The hierarchy is enforced in application code (service / controller
 * layer) rather than in the database, so a user only needs one role assigned;
 * they are not expected to carry every role in the hierarchy.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum RoleName {

    /**
     * Full system access: manage users, all documents, roles, and settings.
     * Users with this role bypass most access-control checks.
     */
    ROLE_ADMIN,
    /**
     * Can upload, share, approve documents, and view the user list. Cannot
     * change user roles or delete other users' accounts.
     */
    ROLE_MANAGER,
    /**
     * Can upload, edit, version, and share documents they own or have been
     * granted EDIT permission on.
     */
    ROLE_EDITOR,
    /**
     * Read-only access: can view and download documents they have been
     * explicitly granted access to, or that are marked public.
     */
    ROLE_VIEWER
}
