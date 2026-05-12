package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank
    @Email(message = "Must be a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
             message = "Email must be in a valid format like name@example.com")
    @Size(max = 254)
    private String email;
}
