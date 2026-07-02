package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.User;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUserAccount(User userAccount);
    Optional<Customer> findByUserAccountId(int userAccountId);
    
    Page<Customer> findByUserAccountUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<Customer> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    Page<Customer> findByUserAccountEmailContainingIgnoreCase(String email, Pageable pageable);
    Page<Customer> findById(int id, Pageable pageable);
}
