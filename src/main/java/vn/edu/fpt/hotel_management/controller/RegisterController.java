package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.hotel_management.service.EmailService;
import vn.edu.fpt.hotel_management.service.OtpService;
import vn.edu.fpt.hotel_management.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
    public String showRegisterForm(HttpSession session) {
        vn.edu.fpt.hotel_management.entity.User loggedInUser = (vn.edu.fpt.hotel_management.entity.User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            if ("ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
                return "redirect:/admin/dashboard";
            } else if ("HOTEL_OWNER".equalsIgnoreCase(loggedInUser.getRole())) {
                return "redirect:/owner/dashboard";
            } else {
                return "redirect:/home";
            }
        }
        return "auth/register";
    }

    @GetMapping("/register-owner")
    public String showRegisterOwnerForm(HttpSession session) {
        vn.edu.fpt.hotel_management.entity.User loggedInUser = (vn.edu.fpt.hotel_management.entity.User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            if ("ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
                return "redirect:/admin/dashboard";
            } else if ("HOTEL_OWNER".equalsIgnoreCase(loggedInUser.getRole())) {
                return "redirect:/owner/dashboard";
            } else {
                return "redirect:/home";
            }
        }
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
            userService.savePendingUser(fullName, username, password, email, otp, role, null, null, null, null, null);

            final String targetEmail = email;
            final String targetOtp = otp;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendOtp(targetEmail, targetOtp);
                } catch (Exception e) {
                    System.err.println("[Email] Error sending registration OTP in background: " + e.getMessage());
                }
            });

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
                                @RequestParam(value = "idCardDocument", required = false) MultipartFile idCardDocument,
                                HttpSession session,
                                Model model) {
        try {
            // Validate thông tin bắt buộc và định dạng số điện thoại (E.164)
            if (phone == null || phone.trim().isEmpty()) {
                throw new RuntimeException("Phone number is required!");
            }
            if (!phone.trim().matches("^\\+?[0-9]{8,19}$")) {
                throw new RuntimeException("Invalid phone number format! Must contain only numbers and be between 8 and 19 digits (e.g. +84912345678).");
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

            // ===== XỬ LÝ UPLOAD ẢNH CCCD =====
            String idCardDocumentPath = null;
            if (idCardDocument != null && !idCardDocument.isEmpty()) {
                try {
                    idCardDocumentPath = saveUploadedFile(idCardDocument, "owner_docs");
                } catch (IOException e) {
                    model.addAttribute("error", "Failed to upload ID card document: " + e.getMessage());
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

            String otp = otpService.generateOtp();

            // Owner có đầy đủ thông tin kèm đường dẫn ảnh CCCD
            userService.savePendingUser(fullName, username, password, email, otp, "HOTEL_OWNER",
                    phone, address, idCard, taxId, idCardDocumentPath);

            final String targetEmail = email;
            final String targetOtp = otp;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendOtp(targetEmail, targetOtp);
                } catch (Exception e) {
                    System.err.println("[Email] Error sending registration OTP in background: " + e.getMessage());
                }
            });

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


    private String saveUploadedFile(MultipartFile file, String subDir) throws IOException {
        Path uploadPath = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", "assets", "docs", subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFilename = file.getOriginalFilename();
        String safeFilename = System.currentTimeMillis() + "_" +
                (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "document.pdf");
        Path target = uploadPath.resolve(safeFilename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/assets/docs/" + subDir + "/" + safeFilename;
    }
}