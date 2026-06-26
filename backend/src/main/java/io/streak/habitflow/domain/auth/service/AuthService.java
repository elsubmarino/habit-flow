package io.streak.habitflow.domain.auth.service;

import io.streak.habitflow.domain.member.dto.request.MemberRequest;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.security.dto.TokenDto;
import io.streak.habitflow.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public TokenDto login(MemberRequest.Login request){
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(()->new BadCredentialsException("가입되지 않은 이메일입니다."));
        if(!passwordEncoder.matches(request.password(),member.getPassword())){
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getId());

        redisTemplate.opsForValue().set(
                "REFRESH_TOKEN:"+member.getEmail(),
                refreshToken,
                14,
                TimeUnit.DAYS
        );

        return new TokenDto(accessToken, refreshToken);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken, String email){
        String redisRefreshKey = "REFRESH_TOKEN:"+email;
        redisTemplate.delete(redisRefreshKey);
        if(accessToken != null){
            long remainingExpiration = jwtTokenProvider.getRemainingExpiration(accessToken);
            if(remainingExpiration>0){
                redisTemplate.opsForValue().set(
                        "BLACKLIST:"+accessToken,
                        "logout",
                        remainingExpiration,
                        TimeUnit.MILLISECONDS
                );
            }

        }

    }

    @Transactional
    public TokenDto refreshTokens(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("만료되거나 올바르지 않은 Refresh Token 입니다.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        String redisKey = "REFRESH_TOKEN:"+email;
        String redisRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
            redisTemplate.delete(redisKey);
            throw new BadCredentialsException("토큰 오염이 감지되었습니다. 보안을 위해 모든 세션을 만료합니다.");
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
        return new TokenDto(newAccessToken,newRefreshToken);
    }
}
