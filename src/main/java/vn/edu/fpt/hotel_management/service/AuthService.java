package vn.edu.fpt.hotel_management.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;
import vn.edu.fpt.hotel_management.repository.AdminRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;
    private final HotelOwnerRepository hotelOwnerRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AdminRepository adminRepository,
                       CustomerRepository customerRepository,
                       HotelOwnerRepository hotelOwnerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
        this.customerRepository = customerRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Username or password incorrect!"));
        // Kiểm tra độ khớp của mật khẩu
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Username or password incorrect!");
        }
        if (!user.isEnabled()) {
            if (user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("UNVERIFIED:" + user.getEmail());
            } else {
                userRepository.delete(user);
                throw new RuntimeException("Account expired! Please register again.");
            }
        }
        
        // Nạp fullName cho User dựa theo role tương ứng
        populateUserFullName(user);
        
        return user;
    }

    private void populateUserFullName(User user) {
        if (user == null) return;
        String role = user.getRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            adminRepository.findByUserAccount(user).ifPresent(admin -> user.setFullName(admin.getFullName()));
        } else if ("HOTEL_OWNER".equalsIgnoreCase(role)) {
            hotelOwnerRepository.findByUserAccount(user).ifPresent(owner -> user.setFullName(owner.getFullName()));
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            customerRepository.findByUserAccount(user).ifPresent(cust -> user.setFullName(cust.getFullName()));
        }
    }
}