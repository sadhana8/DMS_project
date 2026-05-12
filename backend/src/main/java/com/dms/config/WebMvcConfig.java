package com.dms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Local CDN — serves /cdn/** from classpath:/static/cdn/
        // Drop files in src/main/resources/static/cdn/ and reference as /cdn/...
        registry.addResourceHandler("/cdn/**")
                .addResourceLocations("classpath:/static/cdn/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/cdn/**")
                .allowedOrigins("*")
                .allowedMethods("GET")
                .maxAge(3600);
    }
}
