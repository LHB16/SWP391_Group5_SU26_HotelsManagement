package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;

import java.util.Optional;

public interface HotelOwnerRepository extends JpaRepository<HotelOwner, Integer> {
    Optional<HotelOwner> findByUserAccount(User userAccount);
    Optional<HotelOwner> findByUserAccountId(int userAccountId);
    
    Page<HotelOwner> findByUserAccountUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<HotelOwner> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    Page<HotelOwner> findByUserAccountEmailContainingIgnoreCase(String email, Pageable pageable);
    Page<HotelOwner> findByPhoneContaining(String phone, Pageable pageable);
    Page<HotelOwner> findById(int id, Pageable pageable);
    
    long countByVerificationStatus(String verificationStatus);
    
    Page<HotelOwner> findAllByOrderByVerificationStatusDesc(Pageable pageable);
}
