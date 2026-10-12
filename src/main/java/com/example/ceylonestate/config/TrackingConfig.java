package com.example.ceylonestate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the visitor-counting and admin-audit interceptors. */
@Configuration
public class TrackingConfig implements WebMvcConfigurer {

    private final VisitorTrackingInterceptor visitorTrackingInterceptor;
    private final AdminAuditInterceptor adminAuditInterceptor;

    public TrackingConfig(VisitorTrackingInterceptor visitorTrackingInterceptor,
                          AdminAuditInterceptor adminAuditInterceptor) {
        this.visitorTrackingInterceptor = visitorTrackingInterceptor;
        this.adminAuditInterceptor = adminAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitorTrackingInterceptor);
        registry.addInterceptor(adminAuditInterceptor).addPathPatterns("/admin/**");
    }
}
