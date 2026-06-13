package vn.edu.fpt.hotel_management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("hotelmanagementcantho@gmail.com");
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Verify OTP");
        msg.setText("Your OTP code is: " + otp + "\n\nCode expires in 5 minutes.");
        mailSender.send(msg);
    }
}
