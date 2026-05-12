package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body for {@code POST /api/users/{id}/terminate}. Reason is required.
 * Termination is immediate — access is revoked the moment the request returns.
 */
@Data
public class TerminationRequest {

    @NotBlank(message = "A reason is required to terminate a user")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}
