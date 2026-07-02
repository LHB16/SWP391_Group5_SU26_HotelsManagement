package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Refund;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Integer> {

    // Tìm yêu cầu hoàn tiền theo booking ID
    Optional<Refund> findByBookingId(int bookingId);

    // Lấy tất cả yêu cầu hoàn tiền theo trạng thái (dùng cho Admin)
    List<Refund> findByStatusOrderByRequestedAtDesc(String status);

    // Lấy tất cả yêu cầu hoàn tiền (dùng cho Admin dashboard)
    List<Refund> findAllByOrderByRequestedAtDesc();

    // Kiểm tra xem booking đã có yêu cầu hoàn tiền chưa
    boolean existsByBookingId(int bookingId);
}
