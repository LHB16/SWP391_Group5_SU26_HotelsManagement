package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByHotelIdOrderByCreatedAtDesc(int hotelId);
    List<Review> findByHotelIdOrderByRatingDescCreatedAtDesc(int hotelId);
    List<Review> findByHotelIdAndStatusOrderByRatingDescCreatedAtDesc(int hotelId, String status);
    Page<Review> findByStatusIn(Collection<String> statuses, Pageable pageable);
    Page<Review> findByStatus(String status, Pageable pageable);
    boolean existsByHotelIdAndCustomerId(int hotelId, int customerId);
    List<Review> findByHotelIdAndCustomerId(int hotelId, int customerId);
    List<Review> findByCustomerId(int customerId);
    boolean existsByBookingId(int bookingId);
    Review findByBookingId(int bookingId);
}
