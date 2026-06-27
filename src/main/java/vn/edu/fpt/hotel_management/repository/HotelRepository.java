package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Hotel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository cung cấp các phương thức truy vấn dữ liệu cho bảng hotels.
 * Kế thừa JpaRepository để có sẵn các CRUD cơ bản.
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

    /**
     * Lấy tất cả khách sạn đang hoạt động (active = true).
     */
    List<Hotel> findByActiveTrue();

    /**
     * Lọc khách sạn theo khoảng giá (không lọc số sao – dùng khi chọn "All").
     *
     * @param minPrice Giá tối thiểu mỗi đêm (VND)
     * @param maxPrice Giá tối đa mỗi đêm (VND)
     */
    @Query("SELECT h FROM Hotel h WHERE h.active = true " +
           "AND h.price >= :minPrice " +
           "AND h.price <= :maxPrice " +
           "ORDER BY h.price ASC")
    List<Hotel> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Lọc khách sạn theo nhiều mức sao (multi-select) VÀ khoảng giá.
     * Sử dụng IN để lọc theo danh sách số sao được chọn.
     *
     * @param ratings  Danh sách số sao được chọn (ví dụ: [3, 4, 5])
     * @param minPrice Giá tối thiểu mỗi đêm (VND)
     * @param maxPrice Giá tối đa mỗi đêm (VND)
     */
    @Query("SELECT h FROM Hotel h WHERE h.active = true " +
           "AND h.rating IN :ratings " +
           "AND h.price >= :minPrice " +
           "AND h.price <= :maxPrice " +
           "ORDER BY h.price ASC")
    List<Hotel> filterByRatingsAndPrice(
            @Param("ratings") List<Integer> ratings,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Thống kê doanh thu của từng khách sạn theo năm, quý, tháng.
     */
    @Query(value = "SELECT " +
            "    h.id AS hotelId, " +
            "    h.name AS hotelName, " +
            "    YEAR(b.created_at) AS year, " +
            "    DATEPART(QUARTER, b.created_at) AS quarter, " +
            "    MONTH(b.created_at) AS month, " +
            "    SUM(b.total_price) AS revenue, " +
            "    COUNT(b.id) AS bookingCount " +
            "FROM bookings b " +
            "JOIN room r ON b.room_id = r.id " +
            "JOIN hotel h ON r.hotel_id = h.id " +
            "WHERE b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
            "GROUP BY h.id, h.name, YEAR(b.created_at), DATEPART(QUARTER, b.created_at), MONTH(b.created_at) " +
            "ORDER BY year DESC, month DESC, h.name ASC", nativeQuery = true)
    List<java.util.Map<String, Object>> getHotelRevenueStatistics();

    /**
     * Lấy toàn bộ danh sách hóa đơn đặt phòng của toàn bộ khách hàng kèm trạng thái thanh toán.
     */
    @Query(value = "SELECT " +
            "    b.id AS bookingId, " +
            "    b.customer_id AS customerId, " +
            "    r.room_type AS roomType, " +
            "    h.name AS hotelName, " +
            "    b.check_in_date AS checkInDate, " +
            "    b.check_out_date AS checkOutDate, " +
            "    b.total_price AS totalPrice, " +
            "    b.status AS bookingStatus, " +
            "    COALESCE(p.status, 'PENDING') AS paymentStatus, " +
            "    b.created_at AS createdAt " +
            "FROM bookings b " +
            "JOIN room r ON b.room_id = r.id " +
            "JOIN hotel h ON r.hotel_id = h.id " +
            "LEFT JOIN payments p ON p.booking_id = b.id " +
            "ORDER BY b.created_at DESC", nativeQuery = true)
    List<java.util.Map<String, Object>> getAllCustomerBookings();

    /**
     * Lấy danh sách bookings của riêng một khách hàng dựa trên ID.
     */
    @Query(value = "SELECT " +
            "    b.id AS bookingId, " +
            "    r.room_type AS roomType, " +
            "    h.name AS hotelName, " +
            "    b.check_in_date AS checkInDate, " +
            "    b.check_out_date AS checkOutDate, " +
            "    b.total_price AS totalPrice, " +
            "    b.status AS bookingStatus, " +
            "    COALESCE(p.status, 'PENDING') AS paymentStatus " +
            "FROM bookings b " +
            "JOIN room r ON b.room_id = r.id " +
            "JOIN hotel h ON r.hotel_id = h.id " +
            "LEFT JOIN payments p ON p.booking_id = b.id " +
            "WHERE b.customer_id = :customerId " +
            "ORDER BY b.created_at DESC", nativeQuery = true)
    List<java.util.Map<String, Object>> getBookingsByCustomerId(@Param("customerId") int customerId);
}
