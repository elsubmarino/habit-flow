package io.streak.habitflow.domain.auth.service;

import io.streak.habitflow.domain.auth.dto.request.AuthRequest;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.infra.mail.MailService;
import io.streak.habitflow.global.security.dto.TokenDto;
import io.streak.habitflow.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailService mailService;

    private static final String MAIL_LIMIT_PREFIX ="MAIL_LIMIT:";
    private static final long MAIL_LIMIT_SECONDS = 60L;
    private static final long AUTH_CODE_EXPIRATION_MINUTES = 3L; //인증 코드 유효시간 3분
    private static final String AUTH_CODE_PREFIX = "AUTH_CODE:";
    private static final String VERIFIED_PREFIX ="VERIFIED:";
    private static final long VERIFIED_EXPIRATION_MINUTES = 30L;

    @Transactional
    public TokenDto login(AuthRequest.Login request){
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if(!passwordEncoder.matches(request.password(),member.getPassword())){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
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
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        String redisKey = "REFRESH_TOKEN:"+email;
        String redisRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
            redisTemplate.delete(redisKey);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다."));
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

    public void sendAuthCode(String email){
        String limitKey = MAIL_LIMIT_PREFIX + email;

        String isLimited = redisTemplate.opsForValue().get(limitKey);
        if("LOCK".equals(isLimited)){
            log.warn("[Rate Limit 차단] 단 시간 내 이메일 중복 요청 발생 -> Email: {}",email);
            throw new BusinessException(ErrorCode.MAIL_RATE_LIMIT);
        }
        redisTemplate.opsForValue().set(
                limitKey,
                "LOCK",
                MAIL_LIMIT_SECONDS,
                TimeUnit.SECONDS
        );

        String authCode = generateRandomCode();

        redisTemplate.opsForValue().set(
                AUTH_CODE_PREFIX+email,
                authCode,
                AUTH_CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        mailService.sendSignupVerificationMail(email, authCode);

    }

    private String generateRandomCode(){
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    public void verifyAuthCode(String email, String inputCode){
        String redisKey = AUTH_CODE_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(redisKey);

        if (savedCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if(!savedCode.equals(inputCode)){
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        redisTemplate.delete(redisKey);
        redisTemplate.opsForValue().set(
                VERIFIED_PREFIX+email,
                "TRUE",
                VERIFIED_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public boolean isVerifiedEmail(String email){
        String isVerified = redisTemplate.opsForValue().get(VERIFIED_PREFIX+email);
        return "TRUE".equals(isVerified);
    }

    public void clearEmailVerification(String email){
        redisTemplate.delete(VERIFIED_PREFIX+email);
    }
}
