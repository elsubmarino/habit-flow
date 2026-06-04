package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.member.dto.MemberResponse;
import io.streak.habitflow.domain.member.dto.MemberSignUpRequest;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberResponse createMember(MemberSignUpRequest memberSignUpRequest){
        Member member = Member.builder()
                .email(memberSignUpRequest.getEmail())
                .userId(memberSignUpRequest.getUserId())
                .password(memberSignUpRequest.getPassword())
                .build();
        Member result = memberRepository.save(member);
        return MemberResponse.from(result);
    }
}
