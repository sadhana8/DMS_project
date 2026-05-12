package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;
    @Email @NotBlank
    private String email;
    @NotBlank @Size(min = 8)
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String phoneNumber;
}
