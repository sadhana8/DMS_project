package com.dms.entity;

/**
 * Represents the deprecation lifecycle state of any depreciable entity
 * (currently {@link User} and {@link Document}).
 *
 * <p>
 * Deprecation is a <b>soft, reversible</b> alternative to hard deletion. No
 * data is ever physically removed; deprecated records are simply hidden from
 * normal queries and can be restored by an administrator at any time.
 *
 * <h2>Lifecycle transitions</h2>
 * <pre>
 * ACTIVE ──► DEPRECATED ──► ACTIVE   (restored by admin)
 *                │
 *                └──► PERMANENTLY_DELETED  (admin only, irreversible)
 * </pre>
 *
 * <h2>Visibility rules</h2>
 * <ul>
 * <li>{@link #ACTIVE} – visible in all standard queries.</li>
 * <li>{@link #DEPRECATED} – hidden from standard queries; visible only to
 * admins via the {@code /admin/deprecated} endpoints.</li>
 * <li>{@link #PERMANENTLY_DELETED} – excluded from all queries including admin;
 * retained only for audit-log references.</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum DeprecationStatus {

    /**
     * Record is live and visible to authorized users. This is the default state
     * for all newly created records.
     */
    ACTIVE,
    /**
     * Record has been soft-deprecated: hidden from normal listings but fully
     * intact in the database. Can be restored to {@link #ACTIVE} by an
     * administrator via {@code PUT /admin/deprecated/{type}/{id}/restore}.
     *
     * <p>
     * When a user is deprecated, their login is blocked. When a document is
     * deprecated, it is excluded from search results and download/preview
     * endpoints return {@code 410 Gone}.
     */
    DEPRECATED,
    /**
     * Record has been permanently flagged for deletion after a retention period
     * (default 90 days). The data is still physically present for audit
     * purposes but is irrecoverable through normal API calls. Only an admin
     * with direct database access can recover such records.
     */
    PERMANENTLY_DELETED
}
