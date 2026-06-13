package vn.edu.fpt.hotel_management.service;

import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateRegister(String username, String email) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new RuntimeException("Username already exists!");
            } else {
                userRepository.delete(existing);
            }
        });
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new RuntimeException("Email already in use!");
            } else {
                userRepository.delete(existing);
            }
        });
    }

    public void savePendingUser(String fullName, String username,
                                String password, String email, String otp) {
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRole("CUSTOMER");
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        user.setEnabled(false);
        userRepository.save(user);
    }
}