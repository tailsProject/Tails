package com.tails.common.mail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// 인증/재설정 메일 발송 전담. 어떤 상황에 보낼지는 AuthService가 판단
@Slf4j
@Component
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String emailVerifyUrl;
    private final String passwordResetUrl;

    public MailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress,
                        @Value("${app.email-verify-url}") String emailVerifyUrl,
                        @Value("${app.password-reset-url}") String passwordResetUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.emailVerifyUrl = emailVerifyUrl;
        this.passwordResetUrl = passwordResetUrl;
    }

    public void sendVerificationMail(String to, String token) {
        send(to, "[Tails] 이메일 인증을 완료해주세요",
                "아래 링크를 눌러 이메일 인증을 완료해주세요. (30분간 유효)\n\n"
                        + emailVerifyUrl + "?token=" + token);
    }

    public void sendSignupCodeMail(String to, String code) {
        send(to, "[Tails] 회원가입 인증번호",
                "아래 인증번호를 입력해주세요. (5분간 유효)\n\n" + code);
    }

    public void sendPasswordResetMail(String to, String token) {
        send(to, "[Tails] 비밀번호 재설정 안내",
                "아래 링크에서 새 비밀번호를 설정해주세요. (30분간 유효)\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하세요.\n\n"
                        + passwordResetUrl + "?token=" + token);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}", to, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
