// src/main/java/vn/edu/fpt/hotel_management/repository/UserRepository.java
package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.hotel_management.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}