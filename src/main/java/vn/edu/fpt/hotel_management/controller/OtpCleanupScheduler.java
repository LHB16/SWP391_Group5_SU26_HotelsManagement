package vn.edu.fpt.hotel_management.controller;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OtpCleanupScheduler {

    private final UserRepository userRepository;

    public OtpCleanupScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredUnverifiedUsers() {
        List<User> expired = userRepository
                .findByEnabledFalseAndOtpExpiryBefore(LocalDateTime.now());
        userRepository.deleteAll(expired);
    }
}