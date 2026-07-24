package com.botter.rag.security;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final KbAuthInterceptor kbAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(kbAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/auth/**");
    }

    @Bean
    public SaServletFilter saServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/actuator/**", "/api/v1/auth/**")
                .setAuth(obj -> {
                    // 所有 /api/** 接口都需要登录
                    SaRouter.match("/api/**", () -> StpUtil.checkLogin());
                })
                .setError(e -> {
                    SaHolder.getResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
                    return "{\"code\":401,\"message\":\"请先登录\"}";
                });
    }
}
