package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.service.OtpService;

@Controller
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(HttpSession session, Model model) {
        String email = (String) session.getAttribute("pendingEmail");
        if (email == null) return "redirect:/register";
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp,
                            HttpSession session,
                            Model model) {
        try {
            otpService.verifyOtp(email, otp);
            session.removeAttribute("pendingEmail");

            Boolean resetFlow = (Boolean) session.getAttribute("resetFlow");
            if (Boolean.TRUE.equals(resetFlow)) {
                session.removeAttribute("resetFlow");
                session.setAttribute("resetEmail", email);
                session.setAttribute("resetOtp", otp);
                return "redirect:/reset-password";
            }

            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/verify-otp";
        }
    }
}