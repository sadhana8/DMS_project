package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email(message = "Must be a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email must be in a valid format like name@example.com")
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String phoneNumber;

    /**
     * Optional. If omitted, defaults to OTHER.
     */
    private String department;
}
