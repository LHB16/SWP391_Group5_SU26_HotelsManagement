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
        email = normalizeEmail(email);
        java.util.Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            // Do not throw exception, just return to hide email existence.
            return;
        }

        User user = userOpt.get();
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        user.setOtpType("FORGOT_PASSWORD");
        user.setOtpAttempts(0); // Reset incorrect attempts on new OTP creation
        userRepository.save(user);

        final String targetEmail = email;
        final String targetOtp = otp;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emailService.sendPasswordResetOtp(targetEmail, targetOtp);
            } catch (Exception e) {
                System.err.println("[Email] Error sending reset password OTP in background: " + e.getMessage());
            }
        });
    }

    public void resetPassword(String email, String newPassword) {
        email = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Email not found!"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    public User findByEmail(String email) {
        email = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private String normalizeEmail(String email) {
        if (email != null) {
            return email.trim();
        }
        return null;
    }
}
