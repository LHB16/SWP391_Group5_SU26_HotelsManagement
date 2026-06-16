package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    public ProfileController(UserRepository userRepository, OtpService otpService, EmailService emailService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    // Hiển thị form chỉnh sửa thông tin
    @GetMapping("/profile/edit")
    public String showEditProfilePage(HttpSession session, Model model) {
        // Lấy thông tin user đăng nhập từ Session
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", loggedInUser);
        return "User/edit-profile";
    }

    // Xử lý yêu cầu chỉnh sửa thông tin, sinh OTP và gửi email
    @PostMapping("/profile/edit")
    public String handleEditProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin user đăng nhập từ Session
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Kiểm tra xem email mới đã được sử dụng bởi người dùng khác hay chưa
        if (!loggedInUser.getEmail().equalsIgnoreCase(email)) {
            boolean emailExists = userRepository.findByEmail(email)
                    .map(User::isEnabled)
                    .orElse(false);
            if (emailExists) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email is already in use by another account!");
                return "redirect:/profile/edit";
            }
        }

        // Nếu thông tin không thay đổi gì so với hiện tại
        if (loggedInUser.getFullName().equals(fullName) && loggedInUser.getEmail().equalsIgnoreCase(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "No changes were made.");
            return "redirect:/profile/edit";
        }

        try {
            // Sinh mã OTP mới cho phiên cập nhật này
            String otp = otpService.generateOtp();
            
            // Lưu mã OTP và thời hạn vào user trong DB để làm căn cứ đối chiếu
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            userInDb.setOtp(otp);
            userInDb.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
            userRepository.save(userInDb);

            // Gửi OTP qua email mới (hoặc email hiện tại nếu không sửa email)
            emailService.sendProfileUpdateOtp(email, otp);

            // Lưu các thông tin chỉnh sửa tạm thời vào session để chờ kích hoạt khi OTP chính xác
            session.setAttribute("pendingFullName", fullName);
            session.setAttribute("pendingEmail", email);

            redirectAttributes.addFlashAttribute("successMessage", "Verification OTP code has been sent!");
            return "redirect:/profile/verify";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error sending verification email: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }

    // Hiển thị giao diện nhập mã OTP để xác nhận cập nhật thông tin
    @GetMapping("/profile/verify")
    public String showVerifyOtpPage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        String pendingEmail = (String) session.getAttribute("pendingEmail");
        if (pendingEmail == null) {
            return "redirect:/profile/edit";
        }

        model.addAttribute("pendingEmail", pendingEmail);
        return "User/verify-profile-otp";
    }

    // Xử lý xác thực OTP và lưu cập nhật vào Database
    @PostMapping("/profile/verify")
    public String verifyProfileUpdate(
            @RequestParam("otp") String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        String pendingFullName = (String) session.getAttribute("pendingFullName");
        String pendingEmail = (String) session.getAttribute("pendingEmail");

        if (pendingFullName == null || pendingEmail == null) {
            return "redirect:/profile/edit";
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account does not exist!"));

            // Kiểm tra hạn OTP
            if (userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("errorMessage", "OTP code has expired! Please try again.");
                return "redirect:/profile/edit";
            }

            // Kiểm tra tính chính xác của OTP
            if (!otp.equals(userInDb.getOtp())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Incorrect OTP code!");
                return "redirect:/profile/verify";
            }

            // Cập nhật thông tin chính thức
            userInDb.setFullName(pendingFullName);
            userInDb.setEmail(pendingEmail);
            userInDb.setOtp(null);
            userInDb.setOtpExpiry(null);
            userRepository.save(userInDb);

            // Cập nhật lại User trong Session
            session.setAttribute("loggedInUser", userInDb);

            // Xóa thông tin tạm trong Session
            session.removeAttribute("pendingFullName");
            session.removeAttribute("pendingEmail");

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "System error: " + e.getMessage());
            return "redirect:/profile/verify";
        }
    }
}
