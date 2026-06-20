package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Hotel;

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
            @Param("minPrice") long minPrice,
            @Param("maxPrice") long maxPrice
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
            @Param("minPrice") long minPrice,
            @Param("maxPrice") long maxPrice
    );
}
