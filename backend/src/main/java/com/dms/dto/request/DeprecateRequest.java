package com.dms.dto.request;

import lombok.Data;

/**
 * Request DTO used when deprecating a user or document.
 *
 * <p>
 * Provides an optional reason that is stored in the record's
 * {@code deprecation_reason} column for audit purposes.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class DeprecateRequest {

    /**
     * Human-readable reason for the deprecation. Examples: "Left the
     * organisation", "Duplicate account", "Document superseded by v2",
     * "Confidential content". {@code null} or blank is accepted but discouraged
     * — always provide a reason.
     */
    private String reason;
}
