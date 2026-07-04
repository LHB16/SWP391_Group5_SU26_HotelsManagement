package vn.edu.fpt.hotel_management.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       HotelOwnerRepository hotelOwnerRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validateRegister(String username, String email) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new RuntimeException("Username already exists!");
            } else {
                userRepository.delete(existing);
            }
        });
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new RuntimeException("Email already in use!");
            } else {
                userRepository.delete(existing);
            }
        });
    }

    @Transactional
    public void savePendingUser(String fullName, String username,
                                String password, String email, String otp, String role,
                                String phone, String address, String idCard, String taxId) {
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
            owner.setVerificationStatus("PENDING");
            hotelOwnerRepository.save(owner);
        }
    }
}