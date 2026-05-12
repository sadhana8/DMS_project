package com.dms.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class SystemSettingRequest {
    private Map<String, String> settings;
}
