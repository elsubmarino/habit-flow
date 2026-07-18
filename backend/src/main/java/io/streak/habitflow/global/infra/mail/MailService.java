package io.streak.habitflow.global.infra.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VERIFIED_PREFIX ="VERIFIED:";
    private static final String MAIL_LIMIT_PREFIX ="MAIL_LIMIT:";

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Async("mailExecutor")
    public void sendProjectInvitationMail(String email, String projectName, String inviterName, String invitationToken){
        String acceptLink = frontendBaseUrl+"/projects/invite?token="+invitationToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[HabitFlow] "+inviterName+"님이 "+projectName+"' 프로젝트에 초대했습니다.");
        message.setText(inviterName+"님이 귀하를 '"+ projectName+"' 프로젝트에 초대했습니다.\n"+
                "아래 링크를 클릭하여 초대를 수락해주세요. (링크는 24시간 동안 유효합니다.)\n\n"+
                acceptLink);

        try {
            javaMailSender.send(message);
            log.info("프로젝트 초대 이메일 발송 완료: {}",email);
        } catch (MailException e) {
            log.error("프로젝트 초대 이메일 발송 실패: {}",email,e);
        }
    }

    @Async("mailExecutor")
    public void sendSignupVerificationMail(String email, String authCode){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[HabitFlow] 회원가입 인증번호 안내");
        message.setText("인증번호는 ["+authCode+"] 입니다. 3분 이내에 입력해주세요.");

        try {
            javaMailSender.send(message);
            log.info("인증 이메일 발송 완료 : {}",email);
        } catch (MailException e) {
            log.error("인증 이메일 발송 실패 : {}",email,e);
            redisTemplate.delete(MAIL_LIMIT_PREFIX+email);
        }
    }
}
