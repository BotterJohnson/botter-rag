package com.botter.rag.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.botter.rag.dto.ApiResponse;
import com.botter.rag.dto.DevLoginRequest;
import com.botter.rag.dto.LoginResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@Profile("dev")
public class DevAuthController {

    private static final Set<String> ROLES = Set.of("ADMIN", "MEMBER");

    @PostMapping("/dev-login")
    public ApiResponse<LoginResponse> login(@RequestBody DevLoginRequest request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("userId 必须是正整数");
        }

        String departmentId = StringUtils.hasText(request.getDepartmentId())
                ? request.getDepartmentId().trim()
                : "default";
        String role = StringUtils.hasText(request.getRole())
                ? request.getRole().trim().toUpperCase(Locale.ROOT)
                : "MEMBER";
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException("role 仅支持 ADMIN 或 MEMBER");
        }

        StpUtil.login(request.getUserId());
        StpUtil.getSession().set("departmentId", departmentId);
        StpUtil.getSession().set("role", role);

        return ApiResponse.ok(new LoginResponse(
                StpUtil.getTokenName(),
                StpUtil.getTokenValue(),
                request.getUserId(),
                departmentId,
                role));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.ok(null);
    }
}
