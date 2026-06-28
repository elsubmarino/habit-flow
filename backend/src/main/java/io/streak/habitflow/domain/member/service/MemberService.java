package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.member.dto.request.MemberRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.type.MemberRole;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final HashidsProvider hashidsProvider;

    @Transactional
    public MemberResponse.Detail createMember(MemberRequest.SignUp request){
        if(!mailService.isVerifiedEmail(request.email())){
            throw new BusinessException(ErrorCode.UNVERIFIED_EMAIL);
        }

        if(memberRepository.findByEmail(request.email()).isPresent()){
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .name(request.name())
                .password(passwordEncoder.encode(request.password()))
                .memberRole(MemberRole.USER)
                .build();
        Member result = memberRepository.save(member);

        mailService.clearEmailVerification(request.email());
        String encodedId = hashidsProvider.encode(result.getId());
        return MemberResponse.Detail.to(result,encodedId);
    }

    @Transactional
    @CheckOwnership(type = "MEMBER")
    @SuppressWarnings("unused")
    public MemberResponse.Detail updateMember(Long memberId,MemberRequest.Update  request,Long loginMemberId){
        Member member = memberRepository.getOrThrow(memberId);
        String encodedId = hashidsProvider.encode(member.getId());
        if (!request.name().equals(member.getName())) {
            member.updateName(request.name());
        }
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            member.updatePassword(passwordEncoder.encode(request.password()));
        }
        return MemberResponse.Detail.to(member,encodedId);
    }

    public MemberResponse.Detail getMember(Long memberId){
        Member member = memberRepository.getOrThrow(memberId);
        String encodedId = hashidsProvider.encode(member.getId());
        return MemberResponse.Detail.to(member,encodedId);
    }
}
