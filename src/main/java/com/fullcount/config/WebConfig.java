package com.fullcount.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프론트엔드에서 http://localhost:8080/uploads/... 로 접속하면
        // 실제 컴퓨터의 C:/fullcount/uploads/ 폴더의 파일을 보여주도록 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/fullcount/uploads/");
        // 맥/리눅스 환경이라면 "file:/Users/이름/fullcount/uploads/" 처럼 수정
    }
}