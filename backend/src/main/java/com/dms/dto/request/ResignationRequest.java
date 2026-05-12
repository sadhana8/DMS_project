package com.dms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * Body for {@code POST /api/users/me/resign} (employee initiates) and
 * {@code POST /api/users/{id}/resign} (admin records on behalf).
 *
 * <p>By default the effective date is the last day of the resignation month —
 * e.g. resigning on the 15th of June means access ends on 30 June at 23:59.
 * Admins may override the effective date.
 */
@Data
public class ResignationRequest {

    @Size(max = 500)
    private String reason;

    /**
     * Optional admin override. If null, the system computes the last day of the
     * current month at 23:59:59 as the effective date.
     */
    private LocalDate effectiveDate;
}
