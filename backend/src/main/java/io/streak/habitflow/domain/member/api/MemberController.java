package io.streak.habitflow.domain.member.api;

import io.streak.habitflow.domain.member.dto.request.MemberRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.service.MemberService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<MemberResponse.Detail> createMember(@RequestBody MemberRequest.SignUp request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.createMember(request));
    }

    @GetMapping
    @Operation(summary="회원 정보 조회")
    @ApiResponses({@ApiResponse(responseCode = "200",description = "회원 정보 조회 성공")})
    public ResponseEntity<MemberResponse.Detail> getMember(@AuthenticationPrincipal UserPrincipal userPrincipal){
        MemberResponse.Detail memberResponse = memberService.getMember(userPrincipal.getMemberId());
        return ResponseEntity.ok(memberResponse);
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "회원 정보 업데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 정보 업데이트 성공")})
    public ResponseEntity<Void> updateMember(@PathVariable Long memberId,
                                                       @RequestBody MemberRequest.Update request,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal){
        memberService.updateMember(memberId,request,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @Operation(summary = "회원 로그인")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 로그인 성공")})
    public ResponseEntity<Map<String, String>> loginMember(@RequestBody MemberRequest.Login request){
        Map<String, String> result = memberService.login(request);
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
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletResponse response){
        Map<String, String> tokens = memberService.reissue(refreshToken);

        //TODO 추후 HTTPS 적용시에 손댐
//        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.get("refreshToken"))
//                .httpOnly(true)
//                .secure(true)
//                .sameSite("Strict")
//                .path("/")
//                .maxAge(60 * 60 * 24 *14)//14일
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("accessToken",tokens.get("accessToken"),
                "refreshToken",tokens.get("refreshToken")));
    }
}
