package com.example.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class SendMailUtil {

    @Value("${email.sender:}")
    private String defaultSender;

    @Value("${email.password:}")
    private String defaultPassword;

    @Value("${email.host:smtp.qq.com}")
    private String smtpHost;

    @Value("${email.port:587}")
    private int smtpPort;

    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱地址
     * @return 是否为有效邮箱格式
     */
    public boolean isEmail(String email) {
        if (email == null || email.length() < 1 || email.length() > 256) {
            return false;
        }
        Pattern pattern = Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");
        return pattern.matcher(email).matches();
    }

    /**
     * 发送验证码（使用配置文件中的邮箱信息）
     * 
     * @param receiver 收件人
     * @param code     验证码
     * @return 发送结果信息
     */
    public String sendEmail(String receiver, String code) {
        return sendEmail(defaultSender, defaultPassword, receiver, code);
    }

    /**
     * 发送验证码
     * 
     * @param sender   发送人的邮箱
     * @param pwd      邮箱授权码
     * @param receiver 收件人
     * @param code     验证码
     * @return 发送结果信息
     */
    public String sendEmail(String sender, String pwd, String receiver, String code) {

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.auth", "true");

        // 根据端口设置SSL/TLS配置
        if (smtpPort == 465) {
            // SSL配置
            props.setProperty("mail.smtp.ssl.enable", "true");
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
        } else {
            // TLS配置
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
        }

        props.setProperty("mail.smtp.port", String.valueOf(smtpPort));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, pwd);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender, "智考云", "UTF-8"));
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receiver));
            message.setSubject("邮箱验证码", "UTF-8");
            message.setSentDate(new Date());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String str = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>" +
                    "<p style='font-size: 20px;font-weight:bold;'>尊敬的用户：" + receiver + "</p>" +
                    "<p style='text-indent:2em; font-size: 20px;'>您本次的验证码是 " +
                    "<span style='font-size:30px;font-weight:bold;color:red'>" + code + "</span>，" +
                    "有效期60秒，请尽快使用！如非本人操作，请忽略此邮件！</p>" +
                    "<p style='text-align:right; padding-right: 20px;'>" +
                    "<a href='#' style='font-size: 18px'>智考云</a></p>" +
                    "<span style='font-size: 18px; float:right; margin-right: 60px;'>" +
                    sdf.format(new Date()) + "</span></body></html>";

            message.setContent(str, "text/html;charset=utf-8");
            message.saveChanges();

            Transport.send(message);

            return "验证码发送成功";
        } catch (MessagingException | UnsupportedEncodingException e) {
            return "验证码发送失败: " + e.getMessage();
        }
    }

    /**
     * 生成随机的六位验证码
     * 
     * @return 六位数字验证码
     */
    public String createCode() {
        String chars = "0123456789";
        StringBuilder code = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            int index = r.nextInt(chars.length());
            char c = chars.charAt(index);
            code.append(c);
        }
        return code.toString();
    }
}
