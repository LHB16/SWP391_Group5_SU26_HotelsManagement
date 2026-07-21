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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Mã OTP không chính xác."));

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            if (!user.isEnabled()) {
                userRepository.delete(user);
            }
            throw new RuntimeException("Mã OTP đã hết hạn! Vui lòng yêu cầu mã OTP mới.");
        }

        if (!user.getOtp().equals(otp)) {
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);
            if (attempts >= 5) {
                user.setOtp(null);
                user.setOtpExpiry(null);
                user.setOtpAttempts(0);
                userRepository.save(user);
                throw new RuntimeException("Mã OTP đã bị vô hiệu hóa do nhập sai quá 5 lần. Vui lòng yêu cầu mã OTP mới.");
            } else {
                userRepository.save(user);
                throw new RuntimeException("Mã OTP không chính xác! Bạn còn " + (5 - attempts) + " lần thử.");
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