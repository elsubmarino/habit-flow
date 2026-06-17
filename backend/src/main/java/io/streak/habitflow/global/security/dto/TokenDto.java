package io.streak.habitflow.global.security.dto;

public record TokenDto(String accessToken,
                       String refreshToken) {}
