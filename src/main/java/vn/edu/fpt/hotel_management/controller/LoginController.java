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

    @GetMapping("/home")
    public String home() {
        return "HomePage/home";
    }

    @GetMapping("/login")
    public String showLoginForm() {
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

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/admin/dashboard";
            } else {
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