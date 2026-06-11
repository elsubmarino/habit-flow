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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        String token = memberService.login(memberLoginRequest);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}
