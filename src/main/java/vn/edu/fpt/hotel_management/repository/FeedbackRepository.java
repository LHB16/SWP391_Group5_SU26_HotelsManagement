package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findByHotelIdOrderByCreatedAtDesc(int hotelId);
    List<Feedback> findByHotelIdOrderByRatingDescCreatedAtDesc(int hotelId);
    List<Feedback> findByHotelIdAndStatusOrderByRatingDescCreatedAtDesc(int hotelId, String status);
    Page<Feedback> findByStatusIn(Collection<String> statuses, Pageable pageable);
    Page<Feedback> findByStatus(String status, Pageable pageable);
    boolean existsByHotelIdAndCustomerId(int hotelId, int customerId);
    List<Feedback> findByHotelIdAndCustomerId(int hotelId, int customerId);
    List<Feedback> findByCustomerId(int customerId);
    boolean existsByBookingId(int bookingId);
    Feedback findByBookingId(int bookingId);
}
