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

    @Transactional
    public void validateRegister(String username, String email) {
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
                                String idCardDocument) {  // <-- THÊM THAM SỐ
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
            owner.setIdCardDocument(idCardDocument);  // <-- THÊM
            owner.setVerificationStatus("PENDING");
            hotelOwnerRepository.save(owner);
        }
    }
}