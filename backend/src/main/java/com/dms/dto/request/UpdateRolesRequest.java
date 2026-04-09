package com.dms.dto.request;

import lombok.Data;
import java.util.List;

/**
 * Request DTO for {@code PUT /api/users/{id}/roles}.
 *
 * <p>
 * Replaces the complete set of roles assigned to a user. Sending an empty list
 * removes all roles (valid, but not recommended — the user will then have no
 * permissions to anything).
 *
 * <p>
 * Each string in the list must be a valid {@link com.dms.entity.RoleName}
 * constant (e.g. {@code "ROLE_ADMIN"}, {@code "ROLE_EDITOR"}).
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class UpdateRolesRequest {

    /**
     * The complete new set of role name strings to assign. Example:
     * {@code ["ROLE_EDITOR", "ROLE_VIEWER"]}.
     */
    private List<String> roles;
}
