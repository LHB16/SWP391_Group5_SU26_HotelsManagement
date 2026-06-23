package vn.edu.fpt.hotel_management.service;

import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private final UserRepository userRepository;

    public OtpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    public User verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Account not found!"));

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            if (!user.isEnabled()) {
                userRepository.delete(user);
            }
            throw new RuntimeException("OTP has expired! Please register again.");
        }

        if (!user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP!");
        }

        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        return userRepository.save(user);
    }
}