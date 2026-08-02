package io.streak.habitflow.global.security.principal;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UserPrincipal extends User {
    private final Long memberId;

    public UserPrincipal(Long memberId, String email, String role) {
        super(email, "", List.of(new SimpleGrantedAuthority(role)));
        this.memberId=memberId;
    }
}
