package com.dms.dto.request;

import lombok.Data;

/**
 * Request DTO for {@code PUT /api/users/profile}.
 *
 * <p>
 * All fields are optional; only non-null values are applied to the existing
 * user record (partial update semantics). E-mail and username cannot be changed
 * through this endpoint.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class UpdateProfileRequest {

    /**
     * New given (first) name. {@code null} leaves the existing value unchanged.
     */
    private String firstName;

    /**
     * New family (last) name. {@code null} leaves the existing value unchanged.
     */
    private String lastName;

    /**
     * New contact phone number. {@code null} leaves the existing value
     * unchanged.
     */
    private String phoneNumber;
}
