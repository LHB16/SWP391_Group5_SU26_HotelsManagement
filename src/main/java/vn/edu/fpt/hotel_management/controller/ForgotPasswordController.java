package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.Admin;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.AdminRepository;
import vn.edu.fpt.hotel_management.service.EmailService;
import vn.edu.fpt.hotel_management.service.ForgotPasswordService;

@Controller
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final AdminRepository adminRepository;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService,
                                    EmailService emailService,
                                    CustomerRepository customerRepository,
                                    HotelOwnerRepository hotelOwnerRepository,
                                    AdminRepository adminRepository) {
        this.forgotPasswordService = forgotPasswordService;
        this.emailService = emailService;
        this.customerRepository = customerRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.adminRepository = adminRepository;
    }

    @GetMapping("/forgot-password")
    public String showForgotForm(HttpSession session) {
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
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetOtp(@RequestParam String email, HttpSession session, Model model) {
        try {
            if (email != null) {
                email = email.trim();
            }
            forgotPasswordService.sendResetOtp(email);
            session.setAttribute("pendingEmail", email);
            session.setAttribute("resetFlow", true);
            return "redirect:/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPassword(HttpSession session, Model model) {
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
            if (email != null) {
                email = email.trim();
            }
            forgotPasswordService.resetPassword(email, newPassword);

            User user = forgotPasswordService.findByEmail(email);
            if (user != null) {
                String fullName = getFullName(user);
                emailService.sendPasswordResetSuccess(email, fullName, user.getUsername());
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

    private String getFullName(User user) {
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            return customerRepository.findByUserAccount(user).map(Customer::getFullName).orElse(user.getUsername());
        } else if ("HOTEL_OWNER".equalsIgnoreCase(user.getRole())) {
            return hotelOwnerRepository.findByUserAccount(user).map(HotelOwner::getFullName).orElse(user.getUsername());
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return adminRepository.findByUserAccount(user).map(Admin::getFullName).orElse(user.getUsername());
        }
        return user.getUsername();
    }
}
