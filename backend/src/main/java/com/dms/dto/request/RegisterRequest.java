package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for the {@code POST /api/auth/register} endpoint.
 *
 * <p>
 * All fields are validated before the account is created. Username and e-mail
 * uniqueness is checked at the service layer after passing these structural
 * constraints.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class RegisterRequest {

    /**
     * Unique login name chosen by the user. Must be between 3 and 50 characters
     * and must not be blank.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Unique e-mail address used for login and system notifications. Must be a
     * well-formed e-mail address.
     */
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    /**
     * Plain-text password chosen by the user. Must be at least 8 characters;
     * BCrypt-hashed before storage.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * User's given (first) name. Displayed in the UI and e-mail templates.
     */
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * User's family (last) name.
     */
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * Optional contact phone number. No format is enforced.
     */
    private String phoneNumber;
}
