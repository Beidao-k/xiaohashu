package com.quanxiaoha.xiaohashu.auth.component;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class MailSend {
    @Resource
    JavaMailSender mailSender;
    public  void sendCode(String targetEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("zsx8633@163.com");
        message.setTo(targetEmail);
        message.setSubject("登录验证码");
        message.setText("验证码：" + code + "，有效期1分钟，请勿泄露");
        mailSender.send(message);
    }

}
