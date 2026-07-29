package io.streak.habitflow.global.security;

import io.streak.habitflow.global.security.dto.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    private SecurityUtils(){}

    public static Long currentMemberId(){
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getMemberId();
    }
}
