package vn.edu.fpt.hotel_management.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    public ForgotPasswordService(UserRepository userRepository,
                                 EmailService emailService,
                                 OtpService otpService,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found!"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified!");
        }

        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        userRepository.save(user);

        emailService.sendPasswordResetOtp(email, otp);
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found!"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}