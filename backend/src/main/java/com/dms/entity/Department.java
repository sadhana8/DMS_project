package com.dms.entity;

/**
 * Departments employees can belong to.
 *
 * <p>
 * Used for HR reporting (headcount by department), filtering the user
 * directory, and routing department-specific notifications.
 *
 * <p>
 * Adding a new department here is the only change needed — the enum is stored
 * as a string in the database, so there's no schema migration. Existing users
 * with a NULL department row are treated as {@link #OTHER} by the
 * {@code UserResponse} mapper.
 */
public enum Department {
    HR,
    ACCOUNT,
    ENGINEERING,
    SALES,
    OPERATIONS,
    OTHER
}
