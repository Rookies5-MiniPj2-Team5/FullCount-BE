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
        // String 응답은 별도의 처리가 필요할 수 있으나, 보통 DTO나 Void이므로 true 반환
        return true;
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
            // 반환 값이 없는 경우 (Void)
            wrappedBody.put("message", "요청이 성공적으로 처리되었습니다");
        } else {
            // 반환 데이터가 있는 경우
            wrappedBody.put("data", body);
        }
        
        wrappedBody.put("timestamp", LocalDateTime.now());
        
        return wrappedBody;
    }
}
