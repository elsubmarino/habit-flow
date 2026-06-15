package io.streak.habitflow.domain.member.service;

import io.streak.habitflow.domain.member.dto.request.MemberLoginRequest;
import io.streak.habitflow.domain.member.dto.request.MemberSignUpRequest;
import io.streak.habitflow.domain.member.dto.request.MemberUpdateRequest;
import io.streak.habitflow.domain.member.dto.response.MemberResponse;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.type.Role;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.pool.TypePool;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

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

    @Transactional
    @CheckOwnership(type = "MEMBER")
    @SuppressWarnings("unused")
    public MemberResponse updateMember(Long memberId,MemberUpdateRequest  memberUpdateRequest,Long loginMemberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow();

        member.updateMember(memberUpdateRequest.getPassword());

        return MemberResponse.from(member);
    }

    public MemberResponse getMember(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));
        return MemberResponse.from(member);
    }

    @Transactional
    public Map<String, String> login(MemberLoginRequest memberLoginRequest){
        Member member = memberRepository.findByEmail(memberLoginRequest.getEmail())
                .orElseThrow(()->new IllegalArgumentException("가입되지 않은 이메일입니다."));
        if(!passwordEncoder.matches(memberLoginRequest.getPassword(),member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getId());

        redisTemplate.opsForValue().set(
                "REFRESH_TOKEN:"+member.getEmail(),
                refreshToken,
                14,
                TimeUnit.DAYS
        );

        return Map.of("accessToken",accessToken,"refreshToken",refreshToken);
    }

    @Transactional
    public void logout(String accessToken, String email){
        redisTemplate.delete("REFRESH_TOKEN:"+email);
        Long expiration = jwtTokenProvider.getRemainingExpiration(accessToken);
        redisTemplate.opsForValue().set(
                "BLACKLIST:"+accessToken,
                "logout",
                expiration,
                TimeUnit.MILLISECONDS
        );
    }

    @Transactional
    public Map<String, String> reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("만료되거나 올바르지 않은 Refresh Token 입니다.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        String redisKey = "REFRESH_TOKEN:"+email;
        String redisRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
            redisTemplate.delete(redisKey);
            throw new IllegalArgumentException("토큰 오염이 감지되었습니다. 보안을 위해 모든 세션을 만료합니다.");
        }
        Member member = memberRepository.findByEmail(email).orElseThrow();
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getId());

        //RTR(Refresh Token Rotation
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getId());
        redisTemplate.opsForValue().set(
                redisKey,
                newRefreshToken,
                14,
                TimeUnit.DAYS
        );
        return Map.of("accessToken",newAccessToken,"refreshToken",newRefreshToken);
    }
}
