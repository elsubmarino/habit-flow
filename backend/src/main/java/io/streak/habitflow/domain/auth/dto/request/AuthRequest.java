package io.streak.habitflow.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequest {
    public record Login(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(max = 100) String password
    ) {}
    public record SendAuthCode(@NotBlank @Email String email) {}
    public record VerifyAuthCode(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String code
    ) {}
}