package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.hotel_management.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByEnabledFalseAndOtpExpiryBefore(LocalDateTime time);
    List<User> findByRole(String role);
    Page<User> findByRole(String role, Pageable pageable);
    
    Page<User> findByRoleAndId(String role, int id, Pageable pageable);
    
    Page<User> findByRoleAndUsernameContainingIgnoreCase(String role, String username, Pageable pageable);
    
    Page<User> findByRoleAndEmailContainingIgnoreCase(String role, String email, Pageable pageable);
}
