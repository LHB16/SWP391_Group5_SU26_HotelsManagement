package vn.edu.fpt.hotel_management.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.AdminRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       HotelOwnerRepository hotelOwnerRepository,
                       AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteUserCascaded(User user) {
        customerRepository.findByUserAccount(user).ifPresent(customerRepository::delete);
        hotelOwnerRepository.findByUserAccount(user).ifPresent(hotelOwnerRepository::delete);
        adminRepository.findByUserAccount(user).ifPresent(adminRepository::delete);
        userRepository.delete(user);
    }

    /**
     * Validate username and password constraints
     * - Username: 8-30 characters
     * - Password: at least 8 characters
     */
    public void validateUsernameAndPassword(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username is required!");
        }
        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < 8) {
            throw new RuntimeException("Username must be at least 8 characters long!");
        }
        if (trimmedUsername.length() > 30) {
            throw new RuntimeException("Username must not exceed 30 characters!");
        }
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is required!");
        }
        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long!");
        }
    }

    @Transactional
    public void validateRegister(String username, String email, String password) {
        // Validate username and password constraints
        validateUsernameAndPassword(username, password);

        userRepository.findByUsername(username).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new RuntimeException("Username already exists!");
            } else {
                deleteUserCascaded(existing);
            }
        });
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new RuntimeException("Email already in use!");
            } else {
                deleteUserCascaded(existing);
            }
        });
    }

    @Transactional
    public void savePendingUser(String fullName, String username,
                                String password, String email, String otp, String role,
                                String phone, String address, String idCard, String taxId,
                                String idCardDocument) {
        if (!"CUSTOMER".equalsIgnoreCase(role) && !"HOTEL_OWNER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Invalid role selected!");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role.toUpperCase());
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        user.setEnabled(false);

        User savedUser = userRepository.save(user);

        if ("CUSTOMER".equalsIgnoreCase(role)) {
            Customer customer = new Customer();
            customer.setUserAccount(savedUser);
            customer.setFullName(fullName);
            customerRepository.save(customer);
        } else if ("HOTEL_OWNER".equalsIgnoreCase(role)) {
            HotelOwner owner = new HotelOwner();
            owner.setUserAccount(savedUser);
            owner.setFullName(fullName);
            owner.setPhone(phone);
            owner.setAddress(address);
            owner.setIdCard(idCard);
            owner.setTaxId(taxId);
            owner.setIdCardDocument(idCardDocument);
            owner.setVerificationStatus("PENDING");
            hotelOwnerRepository.save(owner);
        }
    }
}