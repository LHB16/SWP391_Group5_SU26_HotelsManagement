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

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Booking b WHERE b.room.id = :roomId AND b.status = :status AND b.checkInDate < :checkout AND b.checkOutDate > :checkin")
    long sumQuantityByRoomIdAndStatusAndCheckInDateBeforeAndCheckOutDateAfter(
            @Param("roomId") int roomId,
            @Param("status") String status,
            @Param("checkout") LocalDate checkout,
            @Param("checkin") LocalDate checkin
    );

    // ===== FILTER METHODS FOR BOOKINGS =====

    List<Booking> findByHotelIdInOrderByCreatedAtDesc(List<Integer> hotelIds);

    @Query("SELECT b FROM Booking b WHERE b.hotel.id IN :hotelIds AND (LOWER(b.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')) OR (b.fullName IS NULL AND LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')))) ORDER BY b.createdAt DESC")
    List<Booking> findByHotelIdsAndCustomerName(
            @Param("hotelIds") List<Integer> hotelIds,
            @Param("customerName") String customerName
    );

    List<Booking> findByHotelIdInAndStatusOrderByCreatedAtDesc(List<Integer> hotelIds, String status);

    List<Booking> findByHotelIdInAndCheckInDateOrderByCreatedAtDesc(List<Integer> hotelIds, LocalDate checkInDate);

    List<Booking> findByHotelIdInAndCheckOutDateOrderByCreatedAtDesc(List<Integer> hotelIds, LocalDate checkOutDate);

    List<Booking> findByCustomerIdAndStatusContainingIgnoreCaseAndRoomTypeContainingIgnoreCaseAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqualOrderByCreatedAtDesc(
            int customerId,
            String status,
            String roomType,
            java.time.LocalDate checkOutLimit,
            java.time.LocalDate checkInLimit
    );

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.customer.id = :customerId AND b.idPromotion = :idPromotion")
    boolean existsByCustomerIdAndIdPromotion(@Param("customerId") int customerId, @Param("idPromotion") int idPromotion);

    // =====================================================
    // PAYOUT: Magic Methods cho Admin truy vấn đối soát
    // =====================================================

    /**
     * Lấy danh sách booking cần đối soát (không tìm kiếm theo từ khoá).
     * Điều kiện: booking đã COMPLETED, thanh toán đã PAID, theo trạng thái payout.
     */
    Page<Booking> findByStatusAndPayment_StatusAndPayoutStatusOrderByCheckOutDateDesc(
            String status,
            String paymentStatus,
            String payoutStatus,
            Pageable pageable
    );

    /**
     * Lấy danh sách booking cần đối soát CÓ tìm kiếm theo tên khách sạn HOẶC tên Owner.
     * Điều kiện giống phương thức trên nhưng thêm bộ lọc OR cho hotel.name và hotel.owner.fullName.
     */
    Page<Booking> findByStatusAndPayment_StatusAndPayoutStatusAndHotel_NameContainingIgnoreCaseOrStatusAndPayment_StatusAndPayoutStatusAndHotel_Owner_FullNameContainingIgnoreCaseOrderByCheckOutDateDesc(
            String status1, String paymentStatus1, String payoutStatus1, String hotelName,
            String status2, String paymentStatus2, String payoutStatus2, String ownerName,
            Pageable pageable
    );

    long countByStatusAndPayment_StatusAndPayoutStatus(String status, String paymentStatus, String payoutStatus);
}
