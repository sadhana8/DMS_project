package com.dms.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateRolesRequest {
    private List<String> roles;
}
