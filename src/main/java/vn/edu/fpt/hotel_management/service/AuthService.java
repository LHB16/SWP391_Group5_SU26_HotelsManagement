package vn.edu.fpt.hotel_management.service;

import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Username or password incorrect!"));
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Username or password incorrect!");
        }
        if (!user.isEnabled()) {
            if (user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("UNVERIFIED:" + user.getEmail());
            } else {
                userRepository.delete(user);
                throw new RuntimeException("Account expired! Please register again.");
            }
        }
        return user;
    }
}