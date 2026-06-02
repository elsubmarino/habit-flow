package io.streak.habitflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String userId;
    private String userName;
    private String email;
    private String role;
}
