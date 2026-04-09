package com.dms.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a user's public-safe profile.
 *
 * <p>
 * Contains all user fields except the password hash. Returned by the
 * user-management endpoints and embedded in {@link AuthResponse} after login.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    /**
     * Database surrogate ID.
     */
    private Long id;

    /**
     * Unique username chosen at registration.
     */
    private String username;

    /**
     * Unique e-mail address; used as the login identifier.
     */
    private String email;

    /**
     * Given (first) name.
     */
    private String firstName;

    /**
     * Family (last) name.
     */
    private String lastName;

    /**
     * Optional contact phone number.
     */
    private String phoneNumber;

    /**
     * URL of the user's avatar image; {@code null} if not set.
     */
    private String profilePicture;

    /**
     * {@code false} when the account has been deactivated by an admin.
     */
    private Boolean isActive;

    /**
     * {@code true} when the user has verified their e-mail address.
     */
    private Boolean isEmailVerified;

    /**
     * List of role name strings assigned to the user (e.g.
     * {@code ["ROLE_ADMIN", "ROLE_MANAGER"]}).
     */
    private List<String> roles;

    /**
     * Timestamp when the account was first created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last profile update.
     */
    private LocalDateTime updatedAt;

    /**
     * Timestamp of the user's most recent successful login; {@code null} if
     * never.
     */
    private LocalDateTime lastLogin;
}
