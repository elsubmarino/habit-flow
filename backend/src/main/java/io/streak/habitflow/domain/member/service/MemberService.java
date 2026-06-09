package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.member.dto.request.MemberLoginRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.dto.request.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.request.MemberUpdateRequest;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.type.Role;
import io.streak.habitflow.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public MemberResponse createMember(MemberSignUpRequest memberSignUpRequest){
        Member member = Member.builder()
                .email(memberSignUpRequest.getEmail())
                .name(memberSignUpRequest.getName())
                .password(passwordEncoder.encode(memberSignUpRequest.getPassword()))
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
    public MemberResponse updateMember(Long memberId,MemberUpdateRequest  memberUpdateRequest,UserDetails userDetails){
        Member member = memberRepository.findById(memberId)
                .orElseThrow((()->new IllegalArgumentException("멤버가 존재하지 않습니다.")));

        if(!member.getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        member.updateMember(memberUpdateRequest.getPassword());

        return MemberResponse.from(member);
    }

    public String login(MemberLoginRequest memberLoginRequest){
        Member member = memberRepository.findByEmail(memberLoginRequest.getEmail())
                .orElseThrow(()->new IllegalArgumentException("가입되지 않은 이메일입니다."));
        if(!passwordEncoder.matches(memberLoginRequest.getPassword(),member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return jwtTokenProvider.createToken(member.getEmail());
    }
}
