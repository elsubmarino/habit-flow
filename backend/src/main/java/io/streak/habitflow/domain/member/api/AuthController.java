package io.streak.habitflow.domain.member.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/success")
    public ResponseEntity<String> loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        return ResponseEntity.ok(email+"님, 소셜 가입/로그인에 성공했습니다.");
    }

    @GetMapping("/fail")
    public ResponseEntity<String> loginFail(){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("소셜 로그인에 실패했습니다.");
    }

}
