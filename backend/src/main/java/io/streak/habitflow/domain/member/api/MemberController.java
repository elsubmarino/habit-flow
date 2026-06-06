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

    /**
     * 멤버 생성
     * @param memberSignUpRequest 멤버 생성 요청 정보 DTO
     * @return 멤버 생성 응답 정보 DTO
     */
    @PostMapping
    public ResponseEntity<MemberResponse> createMember(@RequestBody MemberSignUpRequest memberSignUpRequest){
        return ResponseEntity.ok(memberService.createMember(memberSignUpRequest));
    }

    /**
     * 멤버 단건 조회
     * @param userDetails 인증된 사용자 정보
     * @return 멤버 응답 정보 DTO
     */
    @GetMapping
    public ResponseEntity<MemberResponse> getMember(@AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(memberService.getMember(userDetails));
    }

    /**
     * 멤버 업데이트
     * @param id 멤버 ID
     * @param memberUpdateRequest 멤버 요청 정보 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 멤버 응답 정보 DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable Long id,
                                                       @RequestBody MemberUpdateRequest memberUpdateRequest,
                                                       @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(memberService.updateMember(id,memberUpdateRequest,userDetails));
    }
}
