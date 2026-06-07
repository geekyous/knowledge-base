package com.company.kb.config;

import com.company.kb.dto.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应自动包裹 — Controller 返回业务数据，框架层自动包装为 ApiResponse
 * @author Geekyous
 */
@RestControllerAdvice(basePackages = "com.company.kb.controller")
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseWrapperAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 已经是 ApiResponse 类型的不重复包裹（如 GlobalExceptionHandler 的返回值）
        return !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            return body;
        }
        // String 返回值由 StringHttpMessageConverter 处理，需要手动序列化为 JSON
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(ApiResponse.success(body));
            } catch (JsonProcessingException e) {
                return ApiResponse.error(500, "响应序列化失败");
            }
        }
        return ApiResponse.success(body);
    }
}
