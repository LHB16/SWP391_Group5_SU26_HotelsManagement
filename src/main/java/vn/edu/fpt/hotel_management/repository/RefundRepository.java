package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<Refund> findByStatusOrderByRequestedAtAsc(String status);

    // Lấy tất cả yêu cầu hoàn tiền (dùng cho Admin dashboard)
    List<Refund> findAllByOrderByRequestedAtAsc();

    // Kiểm tra xem booking đã có yêu cầu hoàn tiền chưa
    boolean existsByBookingId(int bookingId);

    // --- CÁC PHƯƠNG THỨC PHÂN TRANG (MAGIC METHODS) ---
    
    // Phân trang theo trạng thái (không lọc từ khóa)
    Page<Refund> findByStatus(String status, Pageable pageable);

    // Phân trang và tìm kiếm khi trạng thái lọc là ALL
    Page<Refund> findByBookingCustomerFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    Page<Refund> findByBookingCustomerUserAccountEmailContainingIgnoreCase(String email, Pageable pageable);

    // Phân trang và tìm kiếm khi trạng thái lọc cụ thể
    Page<Refund> findByStatusAndBookingCustomerFullNameContainingIgnoreCase(String status, String fullName, Pageable pageable);
    Page<Refund> findByStatusAndBookingCustomerUserAccountEmailContainingIgnoreCase(String status, String email, Pageable pageable);
}
