package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.auth.service.AuthService;
import io.streak.habitflow.domain.member.dto.request.MemberRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.type.MemberRole;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final HashidsProvider hashidsProvider;

    @Transactional
    public MemberResponse.Detail createMember(MemberRequest.SignUp request) {
        if (!authService.isVerifiedEmail(request.email())) {
            throw new BusinessException(ErrorCode.UNVERIFIED_EMAIL);
        }

        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .name(request.name())
                .password(passwordEncoder.encode(request.password()))
                .memberRole(MemberRole.USER)
                .build();

        Member result;

        try {
            result = memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException ex) {
            if (hasConstraint(ex, "uk_members_email")) {
                throw new BusinessException(
                        ErrorCode.DUPLICATE_EMAIL,
                        "회원 이메일 고유 제약 위반",
                        ex
                );
            }

            // 이메일 중복이 아닌 예상하지 못한 DB 오류는 500으로 처리
            throw ex;
        }

        authService.clearEmailVerification(request.email());

        return MemberResponse.Detail.to(
                result,
                result.getPublicId().toString()
        );
    }


    @Transactional
    @PreAuthorize("@memberAuth(#publicMemberId)")
    public MemberResponse.Detail updateMember(UUID publicMemberId, MemberRequest.Update  request, Long loginMemberId){
        Member member = memberRepository.getOrThrowByPublicId(publicMemberId);
        if (!request.name().equals(member.getName())) {
            member.updateName(request.name());
        }
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            member.updatePassword(passwordEncoder.encode(request.password()));
        }
        return MemberResponse.Detail.to(member,member.getPublicId().toString());
    }

    public MemberResponse.Detail getMember(Long memberId){
        Member member = memberRepository.getOrThrow(memberId);
        return MemberResponse.Detail.to(member,member.getPublicId().toString());
    }
}
