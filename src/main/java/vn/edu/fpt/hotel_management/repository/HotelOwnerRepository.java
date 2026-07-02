package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;

import java.util.Optional;

public interface HotelOwnerRepository extends JpaRepository<HotelOwner, Integer> {
    Optional<HotelOwner> findByUserAccount(User userAccount);
    Optional<HotelOwner> findByUserAccountId(int userAccountId);
}
