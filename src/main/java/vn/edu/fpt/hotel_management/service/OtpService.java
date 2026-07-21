package vn.edu.fpt.hotel_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public OtpService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    @Transactional
    public User verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid OTP code."));

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP code has expired! Please request a new OTP.");
        }

        if (!user.getOtp().equals(otp)) {
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);
            if (attempts >= 5) {
                user.setOtp(null);
                user.setOtpExpiry(null);
                user.setOtpAttempts(0);
                userRepository.save(user);
                throw new RuntimeException("OTP code has been disabled due to exceeding 5 attempts. Please request a new OTP.");
            } else {
                userRepository.save(user);
                throw new RuntimeException("Incorrect OTP code! You have " + (5 - attempts) + " attempts remaining.");
            }
        }

        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        
        User savedUser = userRepository.save(user);
        
        return savedUser;
    }
}