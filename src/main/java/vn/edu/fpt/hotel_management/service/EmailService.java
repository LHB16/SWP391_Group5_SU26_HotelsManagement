package vn.edu.fpt.hotel_management.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String FROM = "hotelmanagementcantho@gmail.com";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM);
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Verify OTP");
        msg.setText("Your OTP code is: " + otp + "\n\nCode expires in 3 minutes.");
        mailSender.send(msg);
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM);
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Reset Password");
        msg.setText("Your password reset OTP is: " + otp + "\n\nCode expires in 3 minutes.");
        mailSender.send(msg);
    }

    public void sendWelcome(String toEmail, String fullName, String username) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM);
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Welcome!");
        msg.setText("Hi " + fullName + ",\n\n"
                + "Your account has been created successfully!\n"
                + "Username: " + username + "\n\n"
                + "Welcome to Hotels Management!");
        mailSender.send(msg);
    }
    public void sendPasswordResetSuccess(String toEmail, String fullName, String username) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM);
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Password Reset Successfully");
        msg.setText("Hi " + fullName + ",\n\n"
                + "Your password has been reset successfully!\n"
                + "Username: " + username + "\n\n"
                + "If you did not request this, please contact us immediately.");
        mailSender.send(msg);
    }

    public void sendProfileUpdateOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM);
        msg.setTo(toEmail);
        msg.setSubject("Hotels Management - Verify Profile Update OTP");
        msg.setText("Your OTP code to verify profile modification is: " + otp + "\n\n"
                + "This code is valid for 3 minutes.\n"
                + "If you did not request this change, please ignore this email.");
        mailSender.send(msg);
    }
}