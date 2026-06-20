package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Review;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByHotelIdOrderByCreatedAtDesc(int hotelId);
}
