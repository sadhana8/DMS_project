package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * Body for {@code POST /api/profile-changes} — submitted by an employee.
 *
 * <p>
 * {@code fieldName} must be one of {@link #ALLOWED_FIELDS}. Anything else is
 * rejected at the service layer.
 */
@Data
public class ProfileChangeRequestBody {

    /**
     * Whitelist of {@code User} entity field names that an employee is allowed
     * to request changes to. Adding a new field requires updating this set AND
     * adding a case in {@code ProfileChangeRequestService.applyApproved()}.
     */
    public static final Set<String> ALLOWED_FIELDS
            = Set.of("phoneNumber", "address");

    @NotBlank
    private String fieldName;

    @NotBlank
    @Size(max = 500)
    private String newValue;

    @Size(max = 500)
    private String reason;
}
