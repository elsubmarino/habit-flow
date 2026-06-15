package io.streak.habitflow.domain.member.api;

import io.streak.habitflow.domain.member.dto.request.MemberLoginRequest;
import io.streak.habitflow.domain.member.dto.request.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.request.MemberUpdateRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.service.MemberService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "회원 가입")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "회원 가입 성공")})
    public ResponseEntity<MemberResponse> createMember(@RequestBody MemberSignUpRequest memberSignUpRequest){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.createMember(memberSignUpRequest));
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "회원 정보 업데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 정보 업데이트 성공")})
    public ResponseEntity<MemberResponse> updateMember(@PathVariable Long memberId,
                                                       @RequestBody MemberUpdateRequest memberUpdateRequest,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal){
        return ResponseEntity.ok(memberService.updateMember(memberId,memberUpdateRequest,userPrincipal.getMemberId()));
    }

    @PostMapping("/login")
    @Operation(summary = "회원 로그인")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 로그인 성공")})
    public ResponseEntity<Map<String, String>> loginMember(@RequestBody MemberLoginRequest memberLoginRequest){
        Map<String, String> result = memberService.login(memberLoginRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "회원 로그아웃")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "로그아웃 성공")})
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearerToken,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal){
        String accessToken = null;
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            accessToken = bearerToken.substring(7);
        }
        memberService.logout(accessToken, userPrincipal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급")
    public ResponseEntity<Map<String, String>> reissue(
            @RequestHeader("X-Refresh-Token") String refreshToken){
        Map<String, String> tokens = memberService.reissue(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.get("refreshToken"))
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 24 *14)//14일
                .build();

        return ResponseEntity.ok(Map.of("accessToken",tokens.get("accessToken")));
    }
}
