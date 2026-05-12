package com.dms.dto.request;

import lombok.Data;

@Data
public class UpdateDocumentRequest {
    private String title;
    private String description;
    private String tags;
    private Boolean isPublic;
    private String status;
}
