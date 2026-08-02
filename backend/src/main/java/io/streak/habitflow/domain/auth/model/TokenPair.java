package io.streak.habitflow.domain.auth.model;

public record TokenPair(String accessToken,
                        String refreshToken) {}
