package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

    JavaMailSender mailSender;
    StringRedisTemplate redisTemplate;

    static String OTP_PREFIX = "OTP:";
    static long OTP_EXPIRE_MINUTES = 5;

    public String generateAndSaveOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);
        redisTemplate.opsForValue().set(
                OTP_PREFIX + email,
                otp,
                OTP_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );
        return otp;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[Hôm Nay Ăn Gì] Mã xác thực OTP");
        message.setText("Mã OTP của bạn là: " + otp + ". Mã này có hiệu lực trong " + OTP_EXPIRE_MINUTES + " phút.");
        mailSender.send(message);
    }

    public boolean validateOtp(String email, String otp) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        return storedOtp != null && storedOtp.equals(otp);
    }

    public void deleteOtp(String email) {
        redisTemplate.delete(OTP_PREFIX + email);
    }
}
