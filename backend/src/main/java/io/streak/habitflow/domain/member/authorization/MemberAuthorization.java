package io.streak.habitflow.domain.member.authorization;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("memberAuthorization")
@RequiredArgsConstructor
public class MemberAuthorization {
    private final MemberRepository memberRepository;

    public boolean canAccess(UUID publicMemberId){
        Member member = memberRepository.getOrThrowByPublicId(publicMemberId);
        return member.getId().equals(SecurityUtils.currentMemberId());
    }
}
