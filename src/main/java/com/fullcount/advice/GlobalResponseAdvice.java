package com.fullcount.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.fullcount.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // StringHttpMessageConverter를 사용하는 응답(String 데이터)은 
        // Map으로 래핑 시 ClassCastException이 발생하므로 공통 래핑에서 제외합니다.
        return !org.springframework.http.converter.StringHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 1. 이미 공통 구조이거나 에러 응답(ErrorResponse)인 경우 그대로 반환
        if (body instanceof Map && ((Map<?, ?>) body).containsKey("success")) {
            return body;
        }
        if (body != null && body.getClass().getName().contains("ErrorResponse")) {
            return body;
        }

        // 2. 성공 응답을 공통 포맷으로 래핑
        Map<String, Object> wrappedBody = new LinkedHashMap<>();
        wrappedBody.put("success", true);
        
        if (body == null) {
            wrappedBody.put("message", "요청이 성공적으로 처리되었습니다");
        } else {
            // 이제 Page 객체도 그대로 data 필드에 담깁니다. (명시적 DTO 사용 권장)
            wrappedBody.put("data", body);
        }
        
        wrappedBody.put("timestamp", LocalDateTime.now());
        
        return wrappedBody;
    }
}
