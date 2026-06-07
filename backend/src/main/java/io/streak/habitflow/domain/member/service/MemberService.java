package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.dto.request.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.request.MemberUpdateRequest;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public MemberResponse createMember(MemberSignUpRequest memberSignUpRequest){
        Member member = Member.builder()
                .email(memberSignUpRequest.getEmail())
                .name(memberSignUpRequest.getName())
                .password(memberSignUpRequest.getPassword())
                .role(Role.USER)
                .build();
        Member result = memberRepository.save(member);
        return MemberResponse.from(result);
    }

    public MemberResponse getMember(UserDetails userDetails){
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateMember(Long id,MemberUpdateRequest  memberUpdateRequest,UserDetails userDetails){
        Member member = memberRepository.findById(id)
                .orElseThrow((()->new IllegalArgumentException("멤버가 존재하지 않습니다.")));

        if(!member.getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        member.updateMember(memberUpdateRequest.getPassword());

        return MemberResponse.from(member);
    }
}
