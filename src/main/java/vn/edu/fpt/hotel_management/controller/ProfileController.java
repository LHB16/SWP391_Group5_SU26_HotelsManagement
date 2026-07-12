package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.edu.fpt.hotel_management.entity.Admin;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.AdminRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
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
    private final CustomerRepository customerRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final AdminRepository adminRepository;

    public ProfileController(UserRepository userRepository,
                             OtpService otpService,
                             EmailService emailService,
                             PasswordEncoder passwordEncoder,
                             CustomerRepository customerRepository,
                             HotelOwnerRepository hotelOwnerRepository,
                             AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.adminRepository = adminRepository;
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
        userInDb.setFullName(getFullNameByRole(userInDb));
        userInDb.setPhone(getPhoneByRole(userInDb));

        // Nếu là HOTEL_OWNER, truyền thông tin ngân hàng vào model
        if ("HOTEL_OWNER".equalsIgnoreCase(userInDb.getRole())) {
            hotelOwnerRepository.findByUserAccount(userInDb).ifPresent(owner -> {
                model.addAttribute("bankName", owner.getBankName() != null ? owner.getBankName() : "");
                model.addAttribute("bankAccountNumber", owner.getBankAccountNumber() != null ? owner.getBankAccountNumber() : "");
                model.addAttribute("bankAccountHolder", owner.getBankAccountHolder() != null ? owner.getBankAccountHolder() : "");
            });
        }

        model.addAttribute("user", userInDb);
        return "user/profile";
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

        // Chỉ cho phép chỉnh sửa "fullName", "email", "password" hoặc "phone"
        if (!"fullName".equals(field) && !"email".equals(field) && !"password".equals(field) && !"phone".equals(field)) {
            session.setAttribute("errorMessage", "Invalid action!");
            return "redirect:/profile";
        }

        if ("phone".equals(field)) {
            session.setAttribute("profileVerifiedForEdit", true);
            session.setAttribute("editField", field);
            return "redirect:/profile";
        }

        // Thiết lập cờ yêu cầu sửa đổi và lưu trường muốn sửa vào session
        session.setAttribute("pendingEditRequest", true);
        session.setAttribute("editField", field);

        return "redirect:/profile/verify-edit";
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

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            
            // Chỉ sinh và gửi OTP nếu chưa có mã OTP hiện tại hoạt động hoặc mã đã hết hạn
            if (userInDb.getOtp() == null || userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                String otp = otpService.generateOtp();
                userInDb.setOtp(otp);
                userInDb.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
                userInDb.setOtpType("UPDATE_PROFILE");
                userRepository.save(userInDb);

                final String userEmail = userInDb.getEmail();
                final String otpCode = otp;
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendProfileUpdateOtp(userEmail, otpCode);
                    } catch (Exception e) {
                        System.err.println("Error sending async profile update OTP: " + e.getMessage());
                    }
                });

                model.addAttribute("successMessage", "Verification OTP code is being sent to your email!");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
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
        return "user/verify-edit-profile-otp";
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
                return "user/verify-edit-profile-otp";
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

    // Lưu Họ tên mới vào Database (không cần xác thực thêm - AJAX)
    @PostMapping("/profile/save-fullname")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> saveFullName(
            @RequestParam("fullName") String fullName,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return org.springframework.http.ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            
            updateFullNameByRole(userInDb, fullName);
            userInDb.setFullName(fullName);

            // Cập nhật session và dọn cờ
            session.setAttribute("loggedInUser", userInDb);
            session.removeAttribute("profileVerifiedForEdit");
            session.removeAttribute("editField");

            return org.springframework.http.ResponseEntity.ok("success");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // Lưu số điện thoại mới vào Database (không cần xác thực OTP - AJAX)
    @PostMapping("/profile/save-phone")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> savePhone(
            @RequestParam("phone") String phone,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return org.springframework.http.ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            
            updatePhoneByRole(userInDb, phone);
            userInDb.setPhone(phone);

            // Cập nhật session và dọn cờ
            session.setAttribute("loggedInUser", userInDb);
            session.removeAttribute("profileVerifiedForEdit");
            session.removeAttribute("editField");

            return org.springframework.http.ResponseEntity.ok("success");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(e.getMessage());
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
            // Lưu email mới tạm thời vào session và thiết lập cờ xác nhận email mới
            session.setAttribute("pendingNewEmail", newEmail);
            session.setAttribute("pendingNewEmailOtpRequest", true);

            // Xóa OTP cũ trong DB để sẵn sàng cho việc sinh OTP mới ở trang GET
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            userInDb.setOtp(null);
            userInDb.setOtpExpiry(null);
            userInDb.setOtpType(null);
            userRepository.save(userInDb);

            return "redirect:/profile/verify-new-email";
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Error initializing email update: " + e.getMessage());
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

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            
            // Chỉ sinh và gửi OTP nếu chưa có mã OTP hiện tại hoạt động hoặc mã đã hết hạn
            if (userInDb.getOtp() == null || userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                String otp = otpService.generateOtp();
                userInDb.setOtp(otp);
                userInDb.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
                userInDb.setOtpType("UPDATE_PROFILE");
                userRepository.save(userInDb);

                final String targetEmail = pendingNewEmail;
                final String otpCode = otp;
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendProfileUpdateOtp(targetEmail, otpCode);
                    } catch (Exception e) {
                        System.err.println("Error sending async new email update OTP: " + e.getMessage());
                    }
                });

                model.addAttribute("successMessage", "Verification OTP code is being sent to your new email!");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
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
        return "user/verify-new-email-otp";
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
                return "user/verify-new-email-otp";
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

    // Lưu thông tin ngân hàng của Owner (AJAX - không cần OTP)
    @PostMapping("/profile/save-bank-info")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> saveBankInfo(
            @RequestParam("bankName") String bankName,
            @RequestParam("bankAccountNumber") String bankAccountNumber,
            @RequestParam("bankAccountHolder") String bankAccountHolder,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return org.springframework.http.ResponseEntity.status(401).body("Unauthorized");
        }

        if (!"HOTEL_OWNER".equalsIgnoreCase(loggedInUser.getRole())) {
            return org.springframework.http.ResponseEntity.status(403).body("Forbidden: Only Hotel Owner can update bank info");
        }

        if (bankName == null || bankName.isBlank() ||
            bankAccountNumber == null || bankAccountNumber.isBlank() ||
            bankAccountHolder == null || bankAccountHolder.isBlank()) {
            return org.springframework.http.ResponseEntity.badRequest().body("All bank info fields are required");
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));

            HotelOwner owner = hotelOwnerRepository.findByUserAccount(userInDb)
                    .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

            owner.setBankName(bankName.trim());
            owner.setBankAccountNumber(bankAccountNumber.trim());
            owner.setBankAccountHolder(bankAccountHolder.trim().toUpperCase());
            hotelOwnerRepository.save(owner);

            return org.springframework.http.ResponseEntity.ok("success");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private void clearEmailChangeSession(HttpSession session) {
        session.removeAttribute("profileVerifiedForEdit");
        session.removeAttribute("editField");
        session.removeAttribute("pendingNewEmail");
        session.removeAttribute("pendingNewEmailOtpRequest");
        session.removeAttribute("pendingEditRequest");
    }

    private void updateFullNameByRole(User user, String fullName) {
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Customer customer = customerRepository.findByUserAccount(user)
                    .orElseThrow(() -> new RuntimeException("Customer profile not found!"));
            customer.setFullName(fullName);
            customerRepository.save(customer);
        } else if ("HOTEL_OWNER".equalsIgnoreCase(user.getRole())) {
            HotelOwner owner = hotelOwnerRepository.findByUserAccount(user)
                    .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));
            owner.setFullName(fullName);
            hotelOwnerRepository.save(owner);
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            Admin admin = adminRepository.findByUserAccount(user)
                    .orElseThrow(() -> new RuntimeException("Admin profile not found!"));
            admin.setFullName(fullName);
            adminRepository.save(admin);
        } else {
            throw new RuntimeException("Unsupported account role!");
        }
    }

    private String getFullNameByRole(User user) {
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            return customerRepository.findByUserAccount(user).map(Customer::getFullName).orElse(user.getUsername());
        } else if ("HOTEL_OWNER".equalsIgnoreCase(user.getRole())) {
            return hotelOwnerRepository.findByUserAccount(user).map(HotelOwner::getFullName).orElse(user.getUsername());
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return adminRepository.findByUserAccount(user).map(Admin::getFullName).orElse(user.getUsername());
        }
        return user.getUsername();
    }

    private String getPhoneByRole(User user) {
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            return customerRepository.findByUserAccount(user).map(Customer::getPhone).orElse("");
        } else if ("HOTEL_OWNER".equalsIgnoreCase(user.getRole())) {
            return hotelOwnerRepository.findByUserAccount(user).map(HotelOwner::getPhone).orElse("");
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return adminRepository.findByUserAccount(user).map(Admin::getPhone).orElse("");
        }
        return "";
    }

    private void updatePhoneByRole(User user, String phone) {
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Customer customer = customerRepository.findByUserAccount(user)
                    .orElseThrow(() -> new RuntimeException("Customer profile not found!"));
            customer.setPhone(phone);
            customerRepository.save(customer);
        } else if ("HOTEL_OWNER".equalsIgnoreCase(user.getRole())) {
            HotelOwner owner = hotelOwnerRepository.findByUserAccount(user)
                    .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));
            owner.setPhone(phone);
            hotelOwnerRepository.save(owner);
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            Admin admin = adminRepository.findByUserAccount(user)
                    .orElseThrow(() -> new RuntimeException("Admin profile not found!"));
            admin.setPhone(phone);
            adminRepository.save(admin);
        } else {
            throw new RuntimeException("Unsupported account role!");
        }
    }
}
