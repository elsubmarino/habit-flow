package io.streak.habitflow.domain.member.api;

import io.streak.habitflow.domain.member.dto.request.MemberLoginRequest;
import io.streak.habitflow.domain.member.dto.request.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.request.MemberUpdateRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.service.MemberService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<MemberResponse> createMember(@RequestBody MemberSignUpRequest memberSignUpRequest){
        return ResponseEntity.ok(memberService.createMember(memberSignUpRequest));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable Long memberId,
                                                       @RequestBody MemberUpdateRequest memberUpdateRequest,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal){
        return ResponseEntity.ok(memberService.updateMember(memberId,memberUpdateRequest,userPrincipal.getMemberId()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginMember(@RequestBody MemberLoginRequest memberLoginRequest){
        String token = memberService.login(memberLoginRequest);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}
