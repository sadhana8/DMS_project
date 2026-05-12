package com.dms.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    /**
     * Department name as String — never null in response (defaults to "OTHER").
     */
    private String department;
    private String profilePicture;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    /**
     * True for admin-created accounts that haven't yet changed their password.
     */
    private Boolean mustChangePassword;

    /**
     * Resignation timestamp; null if user hasn't resigned.
     */
    private LocalDateTime resignationDate;
    /**
     * When the resignation takes effect — access ends at this point.
     */
    private LocalDateTime resignationEffectiveDate;

    /**
     * Termination timestamp; null if user is not terminated.
     */
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private String terminatedBy;
}
