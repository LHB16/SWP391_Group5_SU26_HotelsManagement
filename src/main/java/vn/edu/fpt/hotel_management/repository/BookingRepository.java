package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Booking;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    
    List<Booking> findAllByOrderByCreatedAtDesc();

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC")
    Page<Booking> findByCustomerId(@Param("customerId") int customerId, Pageable pageable);
    @Query("""
            SELECT b FROM Booking b
            WHERE b.customer.id = :customerId
              AND b.room.id     = :roomId
              AND b.checkInDate  = :checkIn
              AND b.checkOutDate = :checkOut
              AND b.status = 'PENDING'
            ORDER BY b.createdAt DESC
            """)
    java.util.List<Booking> findPendingBookings(
            @Param("customerId") int customerId,
            @Param("roomId")     int roomId,
            @Param("checkIn")    java.time.LocalDate checkIn,
            @Param("checkOut")   java.time.LocalDate checkOut);
}
