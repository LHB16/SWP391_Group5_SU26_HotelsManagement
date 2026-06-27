package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.service.EmailService;
import vn.edu.fpt.hotel_management.service.OtpService;
import vn.edu.fpt.hotel_management.service.UserService;

@Controller
public class RegisterController {

    private final UserService userService;
    private final EmailService emailService;
    private final OtpService otpService;

    public RegisterController(UserService userService, EmailService emailService, OtpService otpService) {
        this.userService = userService;
        this.emailService = emailService;
        this.otpService = otpService;
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "auth/register";
    }

    // Thêm mapping cho Owner
    @GetMapping("/register-owner")
    public String showRegisterOwnerForm() {
        return "auth/register-owner";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String email,
                           @RequestParam(defaultValue = "CUSTOMER") String role,  // Thêm role
                           HttpSession session,
                           Model model) {
        try {
            userService.validateRegister(username, email);

            String otp = otpService.generateOtp();
            emailService.sendOtp(email, otp);
            userService.savePendingUser(fullName, username, password, email, otp, role);  // Gửi role

            session.setAttribute("pendingEmail", email);
            session.setAttribute("pendingFullName", fullName);
            session.setAttribute("pendingUsername", username);

            return "redirect:/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}