package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Wishlist;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    List<Wishlist> findByCustomerIdOrderByAddedAtDesc(int customerId);

    Optional<Wishlist> findByCustomerIdAndRoomId(int customerId, int roomId);

    boolean existsByCustomerIdAndRoomId(int customerId, int roomId);
}
