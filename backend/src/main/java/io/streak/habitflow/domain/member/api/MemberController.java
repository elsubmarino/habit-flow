package io.streak.habitflow.domain.member.api;

import io.streak.habitflow.domain.member.dto.request.MemberRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.service.MemberService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @GetMapping
    @Operation(summary="회원 정보 조회")
    @ApiResponses({@ApiResponse(responseCode = "200",description = "회원 정보 조회 성공")})
    public ResponseEntity<MemberResponse.Detail> getMember(@LoginMemberId Long loginMemberId){
        MemberResponse.Detail memberResponse = memberService.getMember(loginMemberId);
        return ResponseEntity.ok(memberResponse);
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "회원 정보 업데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 정보 업데이트 성공")})
    public ResponseEntity<Void> updateMember(@PathVariable Long memberId,
                                                       @RequestBody MemberRequest.Update request,
                                                       @LoginMemberId Long loginMemberId){
        memberService.updateMember(memberId,request,loginMemberId);
        return ResponseEntity.noContent().build();
    }
}
