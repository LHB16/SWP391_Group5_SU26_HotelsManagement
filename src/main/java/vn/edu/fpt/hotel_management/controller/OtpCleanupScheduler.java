package vn.edu.fpt.hotel_management.controller;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;
import vn.edu.fpt.hotel_management.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OtpCleanupScheduler {

    private final UserRepository userRepository;
    private final UserService userService;

    public OtpCleanupScheduler(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredUnverifiedUsers() {
        // Yêu cầu: Hết hạn thì OTP và thông tin người dùng vẫn còn hiện trong database (chỉ không nhập được).
        // Không tự động xoá bản ghi hết hạn OTP.
    }
}