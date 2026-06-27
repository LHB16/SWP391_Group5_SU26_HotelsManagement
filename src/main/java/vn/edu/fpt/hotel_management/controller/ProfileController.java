package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;
import vn.edu.fpt.hotel_management.service.EmailService;
import vn.edu.fpt.hotel_management.service.OtpService;

import java.time.LocalDateTime;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, OtpService otpService, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Hiển thị thông tin cá nhân (View Profile)
    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        String successMsg = (String) session.getAttribute("successMessage");
        if (successMsg != null) {
            model.addAttribute("successMessage", successMsg);
            session.removeAttribute("successMessage");
        }
        String errorMsg = (String) session.getAttribute("errorMessage");
        if (errorMsg != null) {
            model.addAttribute("errorMessage", errorMsg);
            session.removeAttribute("errorMessage");
        }

        // Lấy thông tin mới nhất từ DB
        User userInDb = userRepository.findById(loggedInUser.getId())
                .orElse(loggedInUser);

        model.addAttribute("user", userInDb);
        return "User/profile";
    }

    // Xử lý yêu cầu chỉnh sửa riêng lẻ từng trường, sinh OTP gửi về email hiện tại
    @GetMapping("/profile/edit-request")
    public String handleEditRequest(
            @RequestParam("field") String field,
            HttpSession session) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Chỉ cho phép chỉnh sửa "fullName", "email" hoặc "password"
        if (!"fullName".equals(field) && !"email".equals(field) && !"password".equals(field)) {
            session.setAttribute("errorMessage", "Invalid action!");
            return "redirect:/profile";
        }

        try {
            // Sinh mã OTP mới
            String otp = otpService.generateOtp();

            // Lưu mã OTP, thời hạn và kiểu OTP vào DB của user hiện tại
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            userInDb.setOtp(otp);
            userInDb.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
            userInDb.setOtpType("UPDATE_PROFILE");
            userRepository.save(userInDb);

            // Gửi OTP qua email hiện tại để xác minh danh tính
            emailService.sendProfileUpdateOtp(userInDb.getEmail(), otp);

            // Thiết lập cờ yêu cầu sửa đổi và lưu trường muốn sửa vào session
            session.setAttribute("pendingEditRequest", true);
            session.setAttribute("editField", field);

            session.setAttribute("successMessage", "Verification OTP code has been sent to your email to verify identity!");
            return "redirect:/profile/verify-edit";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Error sending verification email: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    // Giao diện nhập mã OTP để xác nhận chỉnh sửa
    @GetMapping("/profile/verify-edit")
    public String showVerifyEditOtpPage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Boolean pendingEditRequest = (Boolean) session.getAttribute("pendingEditRequest");
        if (pendingEditRequest == null || !pendingEditRequest) {
            return "redirect:/profile";
        }

        String successMsg = (String) session.getAttribute("successMessage");
        if (successMsg != null) {
            model.addAttribute("successMessage", successMsg);
            session.removeAttribute("successMessage");
        }
        String errorMsg = (String) session.getAttribute("errorMessage");
        if (errorMsg != null) {
            model.addAttribute("errorMessage", errorMsg);
            session.removeAttribute("errorMessage");
        }

        model.addAttribute("pendingEmail", loggedInUser.getEmail());
        return "User/verify-edit-profile-otp";
    }

    // Xử lý xác thực OTP email hiện tại
    @PostMapping("/profile/verify-edit")
    public String verifyEditOtp(
            @RequestParam("otp") String otp,
            HttpSession session,
            Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account does not exist!"));

            // Kiểm tra hạn OTP
            if (userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                session.setAttribute("errorMessage", "OTP code has expired! Please request a new one.");
                session.removeAttribute("pendingEditRequest");
                session.removeAttribute("editField");
                return "redirect:/profile";
            }

            // Kiểm tra kiểu OTP và tính chính xác
            if (!"UPDATE_PROFILE".equals(userInDb.getOtpType()) || !otp.equals(userInDb.getOtp())) {
                model.addAttribute("errorMessage", "Incorrect OTP code!");
                model.addAttribute("pendingEmail", loggedInUser.getEmail());
                return "User/verify-edit-profile-otp";
            }

            // Xác thực thành công: Xóa OTP trong DB
            userInDb.setOtp(null);
            userInDb.setOtpExpiry(null);
            userInDb.setOtpType(null);
            userRepository.save(userInDb);

            // Ghi nhận đã xác thực danh tính để cho phép sửa đổi
            session.setAttribute("profileVerifiedForEdit", true);
            session.removeAttribute("pendingEditRequest");

            session.setAttribute("successMessage", "Identity verified! You can now make changes.");
            return "redirect:/profile";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "System error: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    // Lưu Họ tên mới vào Database (không cần xác thực thêm)
    @PostMapping("/profile/save-fullname")
    public String saveFullName(
            @RequestParam("fullName") String fullName,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Boolean verified = (Boolean) session.getAttribute("profileVerifiedForEdit");
        String editField = (String) session.getAttribute("editField");

        if (verified == null || !verified || !"fullName".equals(editField)) {
            session.setAttribute("errorMessage", "Unauthorized action!");
            return "redirect:/profile";
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            
            userInDb.setFullName(fullName);
            userRepository.save(userInDb);

            // Cập nhật session và dọn cờ
            session.setAttribute("loggedInUser", userInDb);
            session.removeAttribute("profileVerifiedForEdit");
            session.removeAttribute("editField");

            session.setAttribute("successMessage", "Full Name updated successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "System error: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    // Gửi OTP xác thực đến Email mới (Xác thực bước 2)
    @PostMapping("/profile/request-new-email")
    public String requestNewEmail(
            @RequestParam("email") String newEmail,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Boolean verified = (Boolean) session.getAttribute("profileVerifiedForEdit");
        String editField = (String) session.getAttribute("editField");

        if (verified == null || !verified || !"email".equals(editField)) {
            session.setAttribute("errorMessage", "Unauthorized action!");
            return "redirect:/profile";
        }

        // Kiểm tra email mới trùng với tài khoản khác
        if (loggedInUser.getEmail().equalsIgnoreCase(newEmail)) {
            session.setAttribute("errorMessage", "New email must be different from current email!");
            return "redirect:/profile";
        }

        User existingUser = userRepository.findByEmail(newEmail).orElse(null);
        boolean emailExists = existingUser != null && existingUser.isEnabled();
        if (emailExists) {
            session.setAttribute("errorMessage", "Email is already in use by another account!");
            return "redirect:/profile";
        }

        try {
            // Sinh OTP mới gửi đến email mới
            String otp = otpService.generateOtp();

            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            userInDb.setOtp(otp);
            userInDb.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
            userInDb.setOtpType("UPDATE_PROFILE");
            userRepository.save(userInDb);

            // Gửi mã xác nhận đến Email mới
            emailService.sendProfileUpdateOtp(newEmail, otp);

            // Lưu email mới tạm thời vào session và thiết lập cờ xác nhận email mới
            session.setAttribute("pendingNewEmail", newEmail);
            session.setAttribute("pendingNewEmailOtpRequest", true);

            session.setAttribute("successMessage", "Verification OTP code has been sent to your new email!");
            return "redirect:/profile/verify-new-email";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Error sending OTP to new email: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    // Giao diện nhập mã OTP của Email mới
    @GetMapping("/profile/verify-new-email")
    public String showVerifyNewEmailOtpPage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Boolean pendingNewEmailOtpRequest = (Boolean) session.getAttribute("pendingNewEmailOtpRequest");
        String pendingNewEmail = (String) session.getAttribute("pendingNewEmail");

        if (pendingNewEmailOtpRequest == null || !pendingNewEmailOtpRequest || pendingNewEmail == null) {
            return "redirect:/profile";
        }

        String successMsg = (String) session.getAttribute("successMessage");
        if (successMsg != null) {
            model.addAttribute("successMessage", successMsg);
            session.removeAttribute("successMessage");
        }
        String errorMsg = (String) session.getAttribute("errorMessage");
        if (errorMsg != null) {
            model.addAttribute("errorMessage", errorMsg);
            session.removeAttribute("errorMessage");
        }

        model.addAttribute("pendingEmail", pendingNewEmail);
        return "User/verify-new-email-otp";
    }

    // Xử lý xác thực OTP email mới để lưu thay đổi
    @PostMapping("/profile/verify-new-email")
    public String verifyNewEmailOtp(
            @RequestParam("otp") String otp,
            HttpSession session,
            Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        String pendingNewEmail = (String) session.getAttribute("pendingNewEmail");
        if (pendingNewEmail == null) {
            return "redirect:/profile";
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account does not exist!"));

            // Kiểm tra hạn OTP
            if (userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                session.setAttribute("errorMessage", "OTP code has expired! Please try again.");
                clearEmailChangeSession(session);
                return "redirect:/profile";
            }

            // Kiểm tra tính chính xác của OTP
            if (!"UPDATE_PROFILE".equals(userInDb.getOtpType()) || !otp.equals(userInDb.getOtp())) {
                model.addAttribute("errorMessage", "Incorrect OTP code!");
                model.addAttribute("pendingEmail", pendingNewEmail);
                return "User/verify-new-email-otp";
            }

            // Lưu email mới vào DB
            userInDb.setEmail(pendingNewEmail);
            userInDb.setOtp(null);
            userInDb.setOtpExpiry(null);
            userInDb.setOtpType(null);
            userRepository.save(userInDb);

            // Cập nhật lại session
            session.setAttribute("loggedInUser", userInDb);
            clearEmailChangeSession(session);

            session.setAttribute("successMessage", "Email updated successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "System error: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    // Xử lý đổi mật khẩu mới vào Database
    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Boolean verified = (Boolean) session.getAttribute("profileVerifiedForEdit");
        String editField = (String) session.getAttribute("editField");

        if (verified == null || !verified || !"password".equals(editField)) {
            session.setAttribute("errorMessage", "Unauthorized action!");
            return "redirect:/profile";
        }

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu mới có trùng khớp không
        if (!newPassword.equals(confirmPassword)) {
            session.setAttribute("errorMessage", "Confirm password does not match new password!");
            return "redirect:/profile";
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));

            // Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(currentPassword, userInDb.getPassword())) {
                session.setAttribute("errorMessage", "Incorrect current password!");
                return "redirect:/profile";
            }

            // Mã hóa và lưu mật khẩu mới
            userInDb.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(userInDb);

            // Dọn dẹp cờ
            clearEmailChangeSession(session);

            session.setAttribute("successMessage", "Password updated successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "System error: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    // Hủy bỏ chế độ chỉnh sửa thông tin cá nhân
    @GetMapping("/profile/cancel-edit")
    public String cancelEdit(HttpSession session) {
        clearEmailChangeSession(session);
        return "redirect:/profile";
    }

    private void clearEmailChangeSession(HttpSession session) {
        session.removeAttribute("profileVerifiedForEdit");
        session.removeAttribute("editField");
        session.removeAttribute("pendingNewEmail");
        session.removeAttribute("pendingNewEmailOtpRequest");
        session.removeAttribute("pendingEditRequest");
    }
}
