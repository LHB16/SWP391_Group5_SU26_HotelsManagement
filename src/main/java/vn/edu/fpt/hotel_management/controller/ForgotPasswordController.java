package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.service.EmailService;
import vn.edu.fpt.hotel_management.service.ForgotPasswordService;

@Controller
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;
    private final EmailService emailService;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService,
                                    EmailService emailService) {
        this.forgotPasswordService = forgotPasswordService;
        this.emailService = emailService;
    }

    @GetMapping("/forgot-password")
    public String showForgotForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetOtp(@RequestParam String email, HttpSession session, Model model) {
        try {
            forgotPasswordService.sendResetOtp(email);
            session.setAttribute("pendingEmail", email);
            session.setAttribute("resetFlow", true);
            return "redirect:/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPassword(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot-password";
        model.addAttribute("email", email);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String newPassword,
                                HttpSession session,
                                Model model) {
        try {
            forgotPasswordService.resetPassword(email, newPassword);

            User user = forgotPasswordService.findByEmail(email);
            if (user != null) {
                emailService.sendPasswordResetSuccess(email, user.getFullName(), user.getUsername());
            }

            session.removeAttribute("resetEmail");
            session.removeAttribute("resetOtp");
            return "redirect:/login?passwordReset";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/reset-password";
        }
    }
}