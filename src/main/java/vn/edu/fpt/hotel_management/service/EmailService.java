package vn.edu.fpt.hotel_management.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("hotelmanagementcantho@gmail.com");
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Verify OTP");
        msg.setText("Your OTP code is: " + otp + "\n\nCode expires in 3 minutes.");
        mailSender.send(msg);
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("hotelmanagementcantho@gmail.com");
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Reset Password");
        msg.setText("Your password reset OTP is: " + otp + "\n\nCode expires in 3 minutes.");
        mailSender.send(msg);
    }
}