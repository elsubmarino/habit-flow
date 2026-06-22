package io.streak.habitflow.domain.member.api;

import io.streak.habitflow.domain.member.dto.request.MemberRequest;
import io.streak.habitflow.domain.member.service.AuthService;
import io.streak.habitflow.domain.member.service.MailService;
import io.streak.habitflow.domain.member.service.MemberService;
import io.streak.habitflow.global.security.dto.TokenDto;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.streak.habitflow.global.security.jwt.TokenCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MemberService memberService;
    private final AuthService authService;
    private final TokenCookieManager tokenCookieManager;
    private final MailService mailService;

    @PostMapping("/email/send-code")
    @Operation(summary = "이메일 인증번호 발송")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "이메일 인증번호 발송 성공")})
    public ResponseEntity<Void> sendAuthCode(@RequestBody @Valid MemberRequest.SendAuthCode request){
        mailService.sendAuthCode(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify-code")
    @Operation(summary = "이메일 인증번호 확인")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "이메일 인증번호 확인 성공")})
    public ResponseEntity<Void> verifyAuthCode(@RequestBody @Valid MemberRequest.VerifyAuthCode request){
        mailService.verifyAuthCode(request.email(), request.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "회원 로그인")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 로그인 성공")})
    public ResponseEntity<Map<String, String>> loginMember(@RequestBody MemberRequest.Login request,
                                                           HttpServletResponse httpServletResponse){
        TokenDto tokenDto = authService.login(request);
        tokenCookieManager.addRefreshTokenCookie(httpServletResponse,tokenDto.refreshToken());
        return ResponseEntity.ok(Map.of("accessToken", tokenDto.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "회원 로그아웃")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "로그아웃 성공")})
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearerToken,
                                       @CookieValue(value="refreshToken",required = false)String refreshToken,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal,
                                       HttpServletResponse httpServletResponse){
        String accessToken = null;
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            accessToken = bearerToken.substring(7);
        }
        authService.logout(accessToken, refreshToken,userPrincipal.getUsername());
        tokenCookieManager.deleteRefreshTokenCookie(httpServletResponse);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급")
    public ResponseEntity<Map<String, String>> reissue(
            @CookieValue(value="refreshToken") String refreshToken,
            HttpServletResponse response){
        TokenDto tokenDto = authService.reissue(refreshToken);
        tokenCookieManager.addRefreshTokenCookie(response,tokenDto.refreshToken());

        return ResponseEntity.ok(Map.of("accessToken",tokenDto.accessToken()));
    }
}
