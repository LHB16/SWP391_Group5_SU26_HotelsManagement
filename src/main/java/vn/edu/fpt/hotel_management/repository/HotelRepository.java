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
           "AND (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) >= :minPrice " +
           "AND (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) <= :maxPrice " +
           "ORDER BY h.rating DESC, (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) ASC")
    List<Hotel> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Lọc khách sạn theo mức sao đơn lẻ (rating <= :rating) VÀ khoảng giá.
     * Sắp xếp theo rating giảm dần và giá tăng dần.
     */
    @Query("SELECT h FROM Hotel h WHERE h.active = true " +
           "AND h.rating <= :rating " +
           "AND (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) >= :minPrice " +
           "AND (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) <= :maxPrice " +
           "ORDER BY h.rating DESC, (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) ASC")
    List<Hotel> filterByRatingAndPrice(
            @Param("rating") double rating,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

}
