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

    // =====================================================
    // CHỨC NĂNG: XEM THÔNG TIN CÁ NHÂN (VIEW PROFILE)
    // Mô tả: Lấy thông tin tài khoản hiện tại từ database và hiển thị lên giao diện profile.
    // Đối với Hotel Owner, hệ thống sẽ lấy thêm thông tin tài khoản ngân hàng của họ.
    // =====================================================
    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        // Kiểm tra xem người dùng đã đăng nhập hay chưa
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login"; // Chưa đăng nhập thì chuyển hướng về trang đăng nhập
        }

        // Đọc các thông báo thành công hoặc thất bại từ session và đưa vào Model để hiển thị ngoài UI
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

        // Lấy thông tin tài khoản mới nhất từ Database theo ID để tránh dữ liệu bị cũ
        User userInDb = userRepository.findById(loggedInUser.getId())
                .orElse(loggedInUser);
        
        // Thiết lập Họ tên và Số điện thoại tương ứng với vai trò (Role) của người dùng
        userInDb.setFullName(getFullNameByRole(userInDb));
        userInDb.setPhone(getPhoneByRole(userInDb));

        // Kiểm tra nếu là vai trò HOTEL_OWNER (Chủ khách sạn), lấy thêm thông tin tài khoản ngân hàng
        if ("HOTEL_OWNER".equalsIgnoreCase(userInDb.getRole())) {
            hotelOwnerRepository.findByUserAccount(userInDb).ifPresent(owner -> {
                model.addAttribute("bankName", owner.getBankName() != null ? owner.getBankName() : "");
                model.addAttribute("bankAccountNumber", owner.getBankAccountNumber() != null ? owner.getBankAccountNumber() : "");
                model.addAttribute("bankAccountHolder", owner.getBankAccountHolder() != null ? owner.getBankAccountHolder() : "");
            });
        }

        // Truyền thông tin tài khoản hoàn chỉnh sang Model
        model.addAttribute("user", userInDb);
        return "user/profile";
    }

    // =====================================================
    // CHỨC NĂNG: GỬI YÊU CẦU CHỈNH SỬA THÔNG TIN (EDIT REQUEST)
    // Mô tả: Tiếp nhận trường cần sửa (fullName, email, password, phone).
    // - Đối với số điện thoại (phone): Hệ thống cho phép sửa trực tiếp (đặt cờ và redirect).
    // - Đối với các trường còn lại: Yêu cầu xác thực bảo mật trước (sinh OTP và gửi email).
    // =====================================================
    @GetMapping("/profile/edit-request")
    public String handleEditRequest(
            @RequestParam("field") String field,
            HttpSession session) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Kiểm tra tính hợp lệ của trường yêu cầu sửa đổi
        if (!"fullName".equals(field) && !"email".equals(field) && !"password".equals(field) && !"phone".equals(field)) {
            session.setAttribute("errorMessage", "Invalid action!");
            return "redirect:/profile";
        }

        // Nếu chỉ sửa số điện thoại, không bắt buộc xác thực OTP email hiện tại
        if ("phone".equals(field)) {
            session.setAttribute("profileVerifiedForEdit", true);
            session.setAttribute("editField", field);
            return "redirect:/profile";
        }

        // Lưu thông tin trường cần chỉnh sửa và bật cờ yêu cầu xác thực OTP trước khi sửa
        session.setAttribute("pendingEditRequest", true);
        session.setAttribute("editField", field);

        return "redirect:/profile/verify-edit";
    }

    // =====================================================
    // CHỨC NĂNG: GIAO DIỆN XÁC THỰC OTP EMAIL HIỆN TẠI (GET /verify-edit)
    // Mô tả: Sinh mã OTP ngẫu nhiên, lưu vào DB với hạn 3 phút và gửi bất đồng bộ về email hiện tại của người dùng.
    // =====================================================
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
            
            // Chỉ tạo mới OTP nếu chưa có OTP nào hoặc OTP cũ đã hết hạn
            if (userInDb.getOtp() == null || userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                String otp = otpService.generateOtp();
                userInDb.setOtp(otp);
                userInDb.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
                userInDb.setOtpType("UPDATE_PROFILE");
                userInDb.setOtpAttempts(0); // Reset
                userRepository.save(userInDb);

                final String userEmail = userInDb.getEmail();
                final String otpCode = otp;
                // Gửi email chứa OTP bất đồng bộ để tránh làm nghẽn luồng xử lý UI của người dùng
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendProfileUpdateOtp(userEmail, otpCode);
                    } catch (Exception e) {
                        System.err.println("Error sending async profile update OTP: " + e.getMessage());
                    }
                });

                // Initialize resend tracking for Update Profile
                session.setAttribute("otp_resend_count_UPDATE_PROFILE", 0);
                session.setAttribute("otp_last_sent_UPDATE_PROFILE", LocalDateTime.now());

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

        // Tính cooldown
        String sessionKey = "UPDATE_PROFILE";
        Integer resendCount = (Integer) session.getAttribute("otp_resend_count_" + sessionKey);
        LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otp_last_sent_" + sessionKey);
        long cooldown = 0;
        if (resendCount != null && lastSent != null) {
            long cooldownSeconds = 60L * (1L << resendCount);
            long secondsElapsed = java.time.Duration.between(lastSent, LocalDateTime.now()).getSeconds();
            cooldown = Math.max(0, cooldownSeconds - secondsElapsed);
        } else {
            cooldown = 60;
            session.setAttribute("otp_resend_count_" + sessionKey, 0);
            session.setAttribute("otp_last_sent_" + sessionKey, LocalDateTime.now());
        }
        model.addAttribute("cooldown", cooldown);

        model.addAttribute("pendingEmail", loggedInUser.getEmail());
        model.addAttribute("pageTitle", "Verify OTP");
        model.addAttribute("subtitle", "Enter the verification code to edit profile");
        model.addAttribute("actionUrl", "/profile/verify-edit");
        model.addAttribute("submitText", "Verify & Edit");
        model.addAttribute("backUrl", "/profile");
        model.addAttribute("backText", "Back");
        model.addAttribute("resendType", "profile_edit");
        return "auth/verify-otp";
    }

    // =====================================================
    // CHỨC NĂNG: XÁC THỰC MÃ OTP EMAIL HIỆN TẠI (POST /verify-edit)
    // Mô tả: Kiểm tra mã OTP do người dùng nhập vào.
    // Nếu chính xác và còn hạn, lưu cờ "profileVerifiedForEdit = true" cho phép sửa thông tin cá nhân.
    // =====================================================
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

            // Kiểm tra xem mã OTP đã hết hạn chưa
            if (userInDb.getOtpExpiry() == null || userInDb.getOtpExpiry().isBefore(LocalDateTime.now())) {
                session.setAttribute("errorMessage", "OTP code has expired! Please request a new one.");
                session.removeAttribute("pendingEditRequest");
                session.removeAttribute("editField");
                return "redirect:/profile";
            }

            // Kiểm tra kiểu OTP và tính khớp của mã OTP nhập vào
            if (!"UPDATE_PROFILE".equals(userInDb.getOtpType()) || !otp.equals(userInDb.getOtp())) {
                int attempts = userInDb.getOtpAttempts() + 1;
                userInDb.setOtpAttempts(attempts);
                if (attempts >= 5) {
                    userInDb.setOtp(null);
                    userInDb.setOtpExpiry(null);
                    userInDb.setOtpType(null);
                    userInDb.setOtpAttempts(0);
                    userRepository.save(userInDb);
                    session.setAttribute("errorMessage", "OTP code has been invalidated due to 5 incorrect attempts. Please request a new OTP.");
                    session.removeAttribute("pendingEditRequest");
                    session.removeAttribute("editField");
                    return "redirect:/profile";
                } else {
                    userRepository.save(userInDb);
                    model.addAttribute("errorMessage", "Incorrect OTP code! You have " + (5 - attempts) + " attempt(s) remaining.");
                }
                model.addAttribute("pendingEmail", loggedInUser.getEmail());
                model.addAttribute("pageTitle", "Verify OTP");
                model.addAttribute("subtitle", "Enter the verification code to edit profile");
                model.addAttribute("actionUrl", "/profile/verify-edit");
                model.addAttribute("submitText", "Verify & Edit");
                model.addAttribute("backUrl", "/profile");
                model.addAttribute("backText", "Back");
                model.addAttribute("resendType", "profile_edit");

                // Tính cooldown
                String sessionKey = "UPDATE_PROFILE";
                Integer resendCount = (Integer) session.getAttribute("otp_resend_count_" + sessionKey);
                LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otp_last_sent_" + sessionKey);
                long cooldown = 0;
                if (resendCount != null && lastSent != null) {
                    long cooldownSeconds = 60L * (1L << resendCount);
                    long secondsElapsed = java.time.Duration.between(lastSent, LocalDateTime.now()).getSeconds();
                    cooldown = Math.max(0, cooldownSeconds - secondsElapsed);
                } else {
                    cooldown = 60;
                }
                model.addAttribute("cooldown", cooldown);

                return "auth/verify-otp";
            }

            // Xác thực thành công: Xóa OTP và các thông tin liên quan trong Database
            userInDb.setOtp(null);
            userInDb.setOtpExpiry(null);
            userInDb.setOtpType(null);
            userInDb.setOtpAttempts(0);
            userRepository.save(userInDb);

            // Lưu cờ cho phép thực hiện chỉnh sửa vào session
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

        // Validate số điện thoại (E.164: 8-19 chữ số, tùy chọn + ở đầu)
        if (phone == null || phone.trim().isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body("Phone number is required!");
        }
        if (!phone.trim().matches("^\\+?[0-9]{8,19}$")) {
            return org.springframework.http.ResponseEntity.badRequest().body("Invalid phone number format! Must contain only numbers and be between 8 and 19 digits (e.g. +84912345678).");
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));
            
            updatePhoneByRole(userInDb, phone);
            userInDb.setPhone(phone);
            
            // Bảo toàn fullName trong session
            userInDb.setFullName(getFullNameByRole(userInDb));

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
                userInDb.setOtpAttempts(0); // Reset
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

                // Initialize resend tracking for Update Email
                session.setAttribute("otp_resend_count_UPDATE_EMAIL", 0);
                session.setAttribute("otp_last_sent_UPDATE_EMAIL", LocalDateTime.now());

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

        // Tính cooldown
        String sessionKey = "UPDATE_EMAIL";
        Integer resendCount = (Integer) session.getAttribute("otp_resend_count_" + sessionKey);
        LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otp_last_sent_" + sessionKey);
        long cooldown = 0;
        if (resendCount != null && lastSent != null) {
            long cooldownSeconds = 60L * (1L << resendCount);
            long secondsElapsed = java.time.Duration.between(lastSent, LocalDateTime.now()).getSeconds();
            cooldown = Math.max(0, cooldownSeconds - secondsElapsed);
        } else {
            cooldown = 60;
            session.setAttribute("otp_resend_count_" + sessionKey, 0);
            session.setAttribute("otp_last_sent_" + sessionKey, LocalDateTime.now());
        }
        model.addAttribute("cooldown", cooldown);

        model.addAttribute("pendingEmail", pendingNewEmail);
        model.addAttribute("pageTitle", "Verify New Email");
        model.addAttribute("subtitle", "Enter the verification code sent to your new email address");
        model.addAttribute("actionUrl", "/profile/verify-new-email");
        model.addAttribute("submitText", "Confirm Change");
        model.addAttribute("backUrl", "/profile/cancel-edit");
        model.addAttribute("backText", "Cancel");
        model.addAttribute("resendType", "profile_new_email");
        return "auth/verify-otp";
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
                int attempts = userInDb.getOtpAttempts() + 1;
                userInDb.setOtpAttempts(attempts);
                if (attempts >= 5) {
                    userInDb.setOtp(null);
                    userInDb.setOtpExpiry(null);
                    userInDb.setOtpType(null);
                    userInDb.setOtpAttempts(0);
                    userRepository.save(userInDb);
                    session.setAttribute("errorMessage", "OTP code has been invalidated due to 5 incorrect attempts. Please try again.");
                    clearEmailChangeSession(session);
                    return "redirect:/profile";
                } else {
                    userRepository.save(userInDb);
                    model.addAttribute("errorMessage", "Incorrect OTP code! You have " + (5 - attempts) + " attempt(s) remaining.");
                }
                model.addAttribute("pendingEmail", pendingNewEmail);
                model.addAttribute("pageTitle", "Verify New Email");
                model.addAttribute("subtitle", "Enter the verification code sent to your new email address");
                model.addAttribute("actionUrl", "/profile/verify-new-email");
                model.addAttribute("submitText", "Confirm Change");
                model.addAttribute("backUrl", "/profile/cancel-edit");
                model.addAttribute("backText", "Cancel");
                model.addAttribute("resendType", "profile_new_email");

                // Tính cooldown
                String sessionKey = "UPDATE_EMAIL";
                Integer resendCount = (Integer) session.getAttribute("otp_resend_count_" + sessionKey);
                LocalDateTime lastSent = (LocalDateTime) session.getAttribute("otp_last_sent_" + sessionKey);
                long cooldown = 0;
                if (resendCount != null && lastSent != null) {
                    long cooldownSeconds = 60L * (1L << resendCount);
                    long secondsElapsed = java.time.Duration.between(lastSent, LocalDateTime.now()).getSeconds();
                    cooldown = Math.max(0, cooldownSeconds - secondsElapsed);
                } else {
                    cooldown = 60;
                }
                model.addAttribute("cooldown", cooldown);

                return "auth/verify-otp";
            }

            // Lưu email mới vào DB
            userInDb.setEmail(pendingNewEmail);
            userInDb.setOtp(null);
            userInDb.setOtpExpiry(null);
            userInDb.setOtpType(null);
            userInDb.setOtpAttempts(0);
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

    // =====================================================
    // CHỨC NĂNG: THAY ĐỔI MẬT KHẨU MỚI (CHANGE PASSWORD)
    // Mô tả: Kiểm tra tính hợp lệ của mật khẩu mới (trùng khớp, độ dài >= 8, khác mật khẩu cũ),
    // mã hóa mật khẩu mới trước khi lưu xuống Database.
    // =====================================================
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

        // Đảm bảo người dùng đã xác thực danh tính qua OTP trước đó
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

        // Kiểm tra độ dài mật khẩu mới tối thiểu 8 ký tự
        if (newPassword == null || newPassword.length() < 8) {
            session.setAttribute("errorMessage", "New password must be at least 8 characters long!");
            return "redirect:/profile";
        }

        try {
            User userInDb = userRepository.findById(loggedInUser.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found!"));

            // Sử dụng PasswordEncoder để đối chiếu mật khẩu hiện tại có đúng không
            if (!passwordEncoder.matches(currentPassword, userInDb.getPassword())) {
                session.setAttribute("errorMessage", "Incorrect current password!");
                return "redirect:/profile";
            }

            // Đảm bảo mật khẩu mới phải khác mật khẩu cũ
            if (passwordEncoder.matches(newPassword, userInDb.getPassword())) {
                session.setAttribute("errorMessage", "New password must be different from current password!");
                return "redirect:/profile";
            }

            // Mã hóa mật khẩu mới bằng BCrypt/Argon2 và cập nhật vào Database
            userInDb.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(userInDb);

            // Dọn dẹp các cờ xác nhận lưu trong session
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

    // =====================================================
    // CHỨC NĂNG: CẬP NHẬT THÔNG TIN NGÂN HÀNG CỦA OWNER (SAVE BANK INFO)
    // Mô tả: Chỉ dành cho Hotel Owner, lưu thông tin tài khoản ngân hàng phục vụ đối soát payout.
    // Không yêu cầu mã xác nhận OTP.
    // =====================================================
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

        // Chỉ cho phép người dùng có Role là HOTEL_OWNER thực hiện
        if (!"HOTEL_OWNER".equalsIgnoreCase(loggedInUser.getRole())) {
            return org.springframework.http.ResponseEntity.status(403).body("Forbidden: Only Hotel Owner can update bank info");
        }

        // Kiểm tra các trường dữ liệu ngân hàng bắt buộc không được rỗng
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

            // Lưu thông tin ngân hàng đã chuẩn hóa (chữ in hoa cho tên chủ tài khoản)
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
