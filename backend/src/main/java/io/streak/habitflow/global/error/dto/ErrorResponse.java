package io.streak.habitflow.global.error.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse (
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
){
    public static ErrorResponse from(int status, String error, String message, String path){
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}