package com.botter.rag.dto;

import lombok.Data;

@Data
public class DevLoginRequest {

    private Long userId;
    private String departmentId;
    private String role;
}
