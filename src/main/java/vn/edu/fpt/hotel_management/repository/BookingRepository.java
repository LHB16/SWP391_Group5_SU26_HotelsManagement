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

    // Magic Method tìm booking đang chờ xử lý (PENDING) dựa trên khách hàng, phòng, thời gian thuê và trạng thái
    List<Booking> findByCustomerIdAndRoomIdAndCheckInDateAndCheckOutDateAndStatusOrderByCreatedAtDesc(
            int customerId,
            int roomId,
            java.time.LocalDate checkInDate,
            java.time.LocalDate checkOutDate,
            String status
    );
}
