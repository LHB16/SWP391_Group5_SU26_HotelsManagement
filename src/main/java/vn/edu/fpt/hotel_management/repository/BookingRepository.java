package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.entity.Room;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(int customerId, Pageable pageable);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(int customerId);

    List<Booking> findByCustomerIdAndRoomIdAndCheckInDateAndCheckOutDateAndStatusOrderByCreatedAtDesc(
            int customerId,
            int roomId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String status
    );

    @Query("SELECT DISTINCT b.room FROM Booking b WHERE b.customer.id = :customerId AND b.hotel.id = :hotelId AND b.status IN :statuses")
    List<Room> findDistinctRoomsBookedByCustomer(
            @Param("customerId") int customerId,
            @Param("hotelId") int hotelId,
            @Param("statuses") List<String> statuses
    );

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.hotel.id = :hotelId AND b.status IN :statuses ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByCustomerAndHotel(
            @Param("customerId") int customerId,
            @Param("hotelId") int hotelId,
            @Param("statuses") List<String> statuses
    );

    long countByRoomIdAndStatusAndCheckInDateBeforeAndCheckOutDateAfter(
            int roomId,
            String status,
            LocalDate checkout,
            LocalDate checkin
    );

    // ===== FILTER METHODS FOR BOOKINGS =====

    @Query("SELECT b FROM Booking b WHERE b.hotel.id IN :hotelIds ORDER BY b.createdAt DESC")
    List<Booking> findByHotelIds(@Param("hotelIds") List<Integer> hotelIds);

    @Query("SELECT b FROM Booking b WHERE b.hotel.id IN :hotelIds AND (LOWER(b.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')) OR (b.fullName IS NULL AND LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')))) ORDER BY b.createdAt DESC")
    List<Booking> findByHotelIdsAndCustomerName(
            @Param("hotelIds") List<Integer> hotelIds,
            @Param("customerName") String customerName
    );

    @Query("SELECT b FROM Booking b WHERE b.hotel.id IN :hotelIds AND b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByHotelIdsAndStatus(
            @Param("hotelIds") List<Integer> hotelIds,
            @Param("status") String status
    );

    @Query("SELECT b FROM Booking b WHERE b.hotel.id IN :hotelIds AND b.checkInDate = :checkInDate ORDER BY b.createdAt DESC")
    List<Booking> findByHotelIdsAndCheckInDate(
            @Param("hotelIds") List<Integer> hotelIds,
            @Param("checkInDate") LocalDate checkInDate
    );

    @Query("SELECT b FROM Booking b WHERE b.hotel.id IN :hotelIds AND b.checkOutDate = :checkOutDate ORDER BY b.createdAt DESC")
    List<Booking> findByHotelIdsAndCheckOutDate(
            @Param("hotelIds") List<Integer> hotelIds,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    List<Booking> findByCustomerIdAndStatusContainingIgnoreCaseAndRoomTypeContainingIgnoreCaseAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqualOrderByCreatedAtDesc(
            int customerId,
            String status,
            String roomType,
            java.time.LocalDate checkOutLimit,
            java.time.LocalDate checkInLimit
    );
}

