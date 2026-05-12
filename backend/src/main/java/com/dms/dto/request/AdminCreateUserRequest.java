package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * Body for {@code POST /api/users/admin-create}. Admin-created accounts get a
 * randomly generated password, emailed to the user, and
 * {@code mustChangePassword} is set so they're forced to change it on first
 * login.
 */
@Data
public class AdminCreateUserRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phoneNumber;

    /**
     * Department code (HR, ACCOUNT, ENGINEERING, SALES, OPERATIONS, OTHER).
     * Defaults to OTHER.
     */
    private String department;

    /**
     * Roles to assign. Defaults to ROLE_EMPLOYEE if empty.
     */
    private List<String> roles;
}
