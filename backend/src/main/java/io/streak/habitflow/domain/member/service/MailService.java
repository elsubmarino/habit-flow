package io.streak.habitflow.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String AUTH_CODE_PREFIX = "AUTH_CODE:";
    private static final String VERIFIED_PREFIX ="VERIFIED:";
    private static final long AUTH_CODE_EXPIRATION_MINUTES = 3L; //인증 코드 유효시간 3분
    private static final long VERIFIED_EXPIRATION_MINUTES = 30L; //가입 유효 ㅅ기간 30분

    public void sendAuthCode(String email){
        String authCode = generateRandomCode();

        redisTemplate.opsForValue().set(
                AUTH_CODE_PREFIX+email,
                authCode,
                AUTH_CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[HabitFlow] 회원가입 인증번호 안내");
        message.setText("인증번호는 ["+authCode+"] 입니다. 3분 이내에 입력해주세요.");
        javaMailSender.send(message);

        log.info("인증 이메일 발송 완료 : {}",email);
    }

    public void verifyAuthCode(String email, String inputCode){
        String redisKey = AUTH_CODE_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(redisKey);

        if (savedCode == null) {
            throw new IllegalArgumentException("인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.");
        }

        if(!savedCode.equals(inputCode)){
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
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

    public void removeVerifiedStatus(String email){
        redisTemplate.delete(VERIFIED_PREFIX+email);
    }


    public String generateRandomCode(){
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
