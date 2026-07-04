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

    @GetMapping("/register-owner")
    public String showRegisterOwnerForm() {
        return "auth/register-owner";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String email,
                           @RequestParam(defaultValue = "CUSTOMER") String role,
                           HttpSession session,
                           Model model) {
        try {
            userService.validateRegister(username, email);

            String otp = otpService.generateOtp();
            emailService.sendOtp(email, otp);

            // Customer không có thông tin bổ sung
            userService.savePendingUser(fullName, username, password, email, otp, role, null, null, null, null);

            session.setAttribute("pendingEmail", email);
            session.setAttribute("pendingFullName", fullName);
            session.setAttribute("pendingUsername", username);

            return "redirect:/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/register-owner")
    public String registerOwner(@RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam String password,
                                @RequestParam String email,
                                @RequestParam String phone,
                                @RequestParam String address,
                                @RequestParam String idCard,
                                @RequestParam String taxId,
                                HttpSession session,
                                Model model) {
        try {
            // Validate thông tin bắt buộc
            if (phone == null || phone.trim().isEmpty()) {
                model.addAttribute("error", "Phone number is required!");
                return "auth/register-owner";
            }
            if (address == null || address.trim().isEmpty()) {
                model.addAttribute("error", "Address is required!");
                return "auth/register-owner";
            }
            if (idCard == null || idCard.trim().isEmpty()) {
                model.addAttribute("error", "ID Card number is required!");
                return "auth/register-owner";
            }
            if (taxId == null || taxId.trim().isEmpty()) {
                model.addAttribute("error", "Tax ID is required!");
                return "auth/register-owner";
            }

            userService.validateRegister(username, email);

            String otp = otpService.generateOtp();
            emailService.sendOtp(email, otp);

            // Owner có đầy đủ thông tin
            userService.savePendingUser(fullName, username, password, email, otp, "HOTEL_OWNER",
                    phone, address, idCard, taxId);

            session.setAttribute("pendingEmail", email);
            session.setAttribute("pendingFullName", fullName);
            session.setAttribute("pendingUsername", username);

            return "redirect:/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            // Giữ lại giá trị đã nhập
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("address", address);
            model.addAttribute("idCard", idCard);
            model.addAttribute("taxId", taxId);
            return "auth/register-owner";
        }
    }
}