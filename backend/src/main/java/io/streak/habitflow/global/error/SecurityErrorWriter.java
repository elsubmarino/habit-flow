package io.streak.habitflow.global.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.streak.habitflow.global.error.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        String errorCode = switch (status) {
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN  -> "ACCESS_DENIED";
            default         -> status.name();
        };
        ErrorResponse body = ErrorResponse.of(
                status.value(), errorCode, message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
