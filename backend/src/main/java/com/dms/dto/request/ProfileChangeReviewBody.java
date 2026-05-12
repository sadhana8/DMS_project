package com.dms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body for {@code PUT /api/profile-changes/{id}/review} — submitted by HR or
 * Admin. {@code approve=true} applies the change; {@code approve=false}
 * rejects.
 */
@Data
public class ProfileChangeReviewBody {

    @NotNull
    private Boolean approve;

    @Size(max = 500)
    private String note;
}
