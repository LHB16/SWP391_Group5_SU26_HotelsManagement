// src/main/java/vn/edu/fpt/hotel_management/controller/LoginController.java
package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.service.AuthService;

@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
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
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = authService.login(username, password);
            session.setAttribute("loggedInUser", user);

            // KIỂM TRA ROLE VÀ CHUYỂN HƯỚNG ĐÚNG
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/admin/dashboard";
            } else if ("HOTEL_OWNER".equalsIgnoreCase(user.getRole())) {
                return "redirect:/owner/dashboard";
            } else {
                // CUSTOMER hoặc role khác quay về home
                return "redirect:/home";
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("UNVERIFIED:")) {
                String email = msg.replace("UNVERIFIED:", "");
                session.setAttribute("pendingEmail", email);
                return "redirect:/verify-otp";
            }
            model.addAttribute("error", msg);
            return "auth/login";
        }
    }
}