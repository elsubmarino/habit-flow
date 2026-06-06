package io.streak.habitflow.domain.member.api;

import io.streak.habitflow.domain.member.dto.MemberResponse;
import io.streak.habitflow.domain.member.dto.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.MemberUpdateRequest;
import io.streak.habitflow.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> createMember(@RequestBody MemberSignUpRequest memberSignUpRequest){
        return ResponseEntity.ok(memberService.createMember(memberSignUpRequest));
    }

    @GetMapping
    public ResponseEntity<MemberResponse> getMember(@AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(memberService.getMember(userDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable Long id,
                                                       @RequestBody MemberUpdateRequest memberUpdateRequest,
                                                       @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(memberService.updateMember(id,memberUpdateRequest,userDetails));
    }
}
