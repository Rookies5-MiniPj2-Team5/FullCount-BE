package com.fullcount.config;

import org.springframework.beans.factory.annotation.Value; // 🌟 필수 추가
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 🌟 application.yml에서 파일 저장 경로를 읽어옵니다.
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file:/// 기반으로 실제 경로를 맵핑합니다. 윈도우의 \ 기호도 호환되도록 / 로 치환합니다.
        String location = "file:" + uploadDir;
        location = location.replace("\\", "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}