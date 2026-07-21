package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.service.EmailService;
import vn.edu.fpt.hotel_management.service.OtpService;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class OtpController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public OtpController(OtpService otpService, EmailService emailService, UserRepository userRepository) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            if ("ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
                return "redirect:/admin/dashboard";
            } else if ("HOTEL_OWNER".equalsIgnoreCase(loggedInUser.getRole())) {
                return "redirect:/owner/dashboard";
            } else {
                return "redirect:/home";
            }
        }
        String email = (String) session.getAttribute("pendingEmail");
        if (email == null) return "redirect:/register";
        model.addAttribute("email", email);

        // Tính cooldown còn lại cho resend OTP
        Boolean resetFlow = (Boolean) session.getAttribute("resetFlow");
        model.addAttribute("resetFlow", Boolean.TRUE.equals(resetFlow));
        String sessionKey = Boolean.TRUE.equals(resetFlow) ? "FORGOT_PASSWORD" : "REGISTER";
        Integer resendCount = (Integer) session.getAttribute("otp_resend_count_" + sessionKey);
        LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otp_last_sent_" + sessionKey);
        long cooldown = 0;
        if (resendCount != null && lastSent != null) {
            long cooldownSeconds = 60L * (1L << resendCount);
            long secondsElapsed = java.time.Duration.between(lastSent, LocalDateTime.now()).getSeconds();
            cooldown = Math.max(0, cooldownSeconds - secondsElapsed);
        } else {
            cooldown = 60;
            session.setAttribute("otp_resend_count_" + sessionKey, 0);
            session.setAttribute("otp_last_sent_" + sessionKey, LocalDateTime.now());
        }
        model.addAttribute("cooldown", cooldown);

        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp,
                            HttpSession session,
                            Model model) {
        try {
            User user = otpService.verifyOtp(email, otp);
            session.removeAttribute("pendingEmail");

            Boolean resetFlow = (Boolean) session.getAttribute("resetFlow");
            if (Boolean.TRUE.equals(resetFlow)) {
                session.removeAttribute("resetFlow");
                session.setAttribute("resetEmail", email);
                session.setAttribute("resetOtp", otp);
                return "redirect:/reset-password";
            }

            String fullName = (String) session.getAttribute("pendingFullName");
            if (fullName == null) {
                fullName = user.getUsername();
            }
            emailService.sendWelcome(email, fullName, user.getUsername());

            session.removeAttribute("pendingOtp");
            session.removeAttribute("pendingFullName");
            session.removeAttribute("pendingUsername");
            session.removeAttribute("pendingPassword");

            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);

            // Tính cooldown còn lại khi xảy ra lỗi nhập để giữ timer hoạt động chính xác
            Boolean resetFlow = (Boolean) session.getAttribute("resetFlow");
            model.addAttribute("resetFlow", Boolean.TRUE.equals(resetFlow));
            String sessionKey = Boolean.TRUE.equals(resetFlow) ? "FORGOT_PASSWORD" : "REGISTER";
            Integer resendCount = (Integer) session.getAttribute("otp_resend_count_" + sessionKey);
            LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otp_last_sent_" + sessionKey);
            long cooldown = 0;
            if (resendCount != null && lastSent != null) {
                long cooldownSeconds = 60L * (1L << resendCount);
                long secondsElapsed = java.time.Duration.between(lastSent, LocalDateTime.now()).getSeconds();
                cooldown = Math.max(0, cooldownSeconds - secondsElapsed);
            } else {
                cooldown = 60;
            }
            model.addAttribute("cooldown", cooldown);

            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    @ResponseBody
    public ResponseEntity<?> resendOtp(@RequestParam String type, HttpSession session) {
        String sessionKey = "";
        String email = "";
        String otpType = "";
        boolean isRegistration = false;

        if ("auth".equals(type)) {
            Boolean resetFlow = (Boolean) session.getAttribute("resetFlow");
            email = (String) session.getAttribute("pendingEmail");
            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Session expired. Please register again."));
            }
            if (Boolean.TRUE.equals(resetFlow)) {
                sessionKey = "FORGOT_PASSWORD";
                otpType = "FORGOT_PASSWORD";
            } else {
                sessionKey = "REGISTER";
                otpType = null;
                isRegistration = true;
            }
        } else if ("profile_edit".equals(type)) {
            User loggedInUser = (User) session.getAttribute("loggedInUser");
            if (loggedInUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "You are not logged in."));
            }
            email = loggedInUser.getEmail();
            sessionKey = "UPDATE_PROFILE";
            otpType = "UPDATE_PROFILE";
        } else if ("profile_new_email".equals(type)) {
            User loggedInUser = (User) session.getAttribute("loggedInUser");
            if (loggedInUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "You are not logged in."));
            }
            email = (String) session.getAttribute("pendingNewEmail");
            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "New email request not found."));
            }
            sessionKey = "UPDATE_EMAIL";
            otpType = "UPDATE_PROFILE";
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid request type."));
        }

        String countAttr = "otp_resend_count_" + sessionKey;
        String lastSentAttr = "otp_last_sent_" + sessionKey;

        Integer resendCount = (Integer) session.getAttribute(countAttr);
        LocalDateTime lastSent = (LocalDateTime) session.getAttribute(lastSentAttr);

        if (resendCount == null) resendCount = 0;
        if (lastSent == null) lastSent = LocalDateTime.now().minusMinutes(10);

        long cooldownSeconds = 60L * (1L << resendCount);
        LocalDateTime nextAllowedTime = lastSent.plusSeconds(cooldownSeconds);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(nextAllowedTime)) {
            long secondsToWait = java.time.Duration.between(now, nextAllowedTime).getSeconds();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Please wait " + secondsToWait + " seconds before resending OTP.",
                "remainingSeconds", secondsToWait
            ));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        boolean isForgotFlow = "FORGOT_PASSWORD".equals(sessionKey);
        if (user == null) {
            if (isForgotFlow) {
                session.setAttribute(countAttr, resendCount + 1);
                session.setAttribute(lastSentAttr, now);
                long nextCooldown = 60L * (1L << (resendCount + 1));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "A new OTP code has been sent to your email.",
                    "nextCooldown", nextCooldown
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Account not found to send OTP."));
            }
        }

        String newOtp = otpService.generateOtp();
        user.setOtp(newOtp);
        user.setOtpExpiry(now.plusMinutes(3));
        user.setOtpType(otpType);
        user.setOtpAttempts(0);
        userRepository.save(user);

        final String targetEmail = email;
        final String targetOtp = newOtp;
        final String finalSessionKey = sessionKey;

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if ("FORGOT_PASSWORD".equals(finalSessionKey)) {
                    emailService.sendPasswordResetOtp(targetEmail, targetOtp);
                } else if ("REGISTER".equals(finalSessionKey)) {
                    emailService.sendOtp(targetEmail, targetOtp);
                } else {
                    emailService.sendProfileUpdateOtp(targetEmail, targetOtp);
                }
            } catch (Exception e) {
                System.err.println("Error sending async resend OTP: " + e.getMessage());
            }
        });

        session.setAttribute(countAttr, resendCount + 1);
        session.setAttribute(lastSentAttr, now);
        long nextCooldown = 60L * (1L << (resendCount + 1));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "A new OTP code has been sent to your email.",
            "nextCooldown", nextCooldown
        ));
    }
}