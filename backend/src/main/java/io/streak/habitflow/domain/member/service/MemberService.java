package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.member.dto.MemberResponse;
import io.streak.habitflow.domain.member.dto.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.MemberUpdateRequest;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;

    /**
     * 멤버 생성
     * @param memberSignUpRequest
     * @return
     */
    @Transactional
    public MemberResponse createMember(MemberSignUpRequest memberSignUpRequest){
        Member member = Member.builder()
                .email(memberSignUpRequest.getEmail())
                .name(memberSignUpRequest.getName())
                .password(memberSignUpRequest.getPassword())
                .build();
        Member result = memberRepository.save(member);
        return MemberResponse.from(result);
    }

    /**
     * 멤버 단건 조회
     * @param userDetails
     * @return
     */
    public MemberResponse getMember(UserDetails userDetails){
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        return MemberResponse.from(member);
    }

    /**
     * 멤버 업데이트
     * @param id
     * @param memberUpdateRequest
     * @return
     */
    @Transactional
    public MemberResponse updateMember(Long id,MemberUpdateRequest  memberUpdateRequest){
        Member member = Member.builder()
                .id(id)
                .password(memberUpdateRequest.getPassword())
                .build();
        return MemberResponse.from(memberRepository.save(member));
    }
}
