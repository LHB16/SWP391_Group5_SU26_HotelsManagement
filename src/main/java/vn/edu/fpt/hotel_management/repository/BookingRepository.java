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

    // Magic Method lấy danh sách booking của khách hàng và sắp xếp giảm dần theo thời gian tạo
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(int customerId, Pageable pageable);

    // Magic Method lấy toàn bộ booking của khách hàng (không phân trang) để filter trong controller
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(int customerId);

    // Magic Method tìm booking đang chờ xử lý (PENDING) dựa trên khách hàng, phòng, thời gian thuê và trạng thái
    List<Booking> findByCustomerIdAndRoomIdAndCheckInDateAndCheckOutDateAndStatusOrderByCreatedAtDesc(
            int customerId,
            int roomId,
            java.time.LocalDate checkInDate,
            java.time.LocalDate checkOutDate,
            String status
    );

    @Query("SELECT DISTINCT b.room FROM Booking b WHERE b.customer.id = :customerId AND b.hotel.id = :hotelId AND b.status IN :statuses")
    List<vn.edu.fpt.hotel_management.entity.Room> findDistinctRoomsBookedByCustomer(
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
            java.time.LocalDate checkout,
            java.time.LocalDate checkin
    );
}
