package com.botter.rag.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
public class KnowledgeBaseCreateRequest {
    private String name;
    private String description;
    private String departmentId;
    private Boolean isPublic = false;
}