package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.hotel_management.entity.Admin;
import vn.edu.fpt.hotel_management.entity.User;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByUserAccount(User userAccount);
    Optional<Admin> findByUserAccountId(int userAccountId);
}
