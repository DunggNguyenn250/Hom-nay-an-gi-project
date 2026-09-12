package org.example.esport.Configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest; // Import đúng HttpServletRequest
import jakarta.servlet.http.HttpServletResponse;
import org.example.esport.Dto.response.ApiResponse;
import org.example.esport.Exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request, // Sửa ở đây: HttpServletRequest thay vì HttpServletResponse
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
