package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Room;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    // Tìm các phòng thuộc về một khách sạn cụ thể
    List<Room> findByHotelId(int hotelId);

    // Lọc các loại phòng phân biệt theo khách sạn
    @Query("SELECT DISTINCT r.type FROM Room r WHERE r.hotelId = :hotelId")
    List<String> findDistinctTypesByHotelId(@Param("hotelId") int hotelId);

    // Lọc phòng theo khách sạn và khoảng giá (magic method)
    List<Room> findByHotelIdAndPriceBetween(int hotelId, BigDecimal minPrice, BigDecimal maxPrice);

    // Lọc phòng theo khách sạn, loại phòng và khoảng giá (magic method)
    List<Room> findByHotelIdAndTypeInAndPriceBetween(int hotelId, List<String> types, BigDecimal minPrice,
            BigDecimal maxPrice);

    // Tìm phòng có giá thấp nhất của khách sạn (magic method thay thế cho SELECT MIN)
    java.util.Optional<Room> findFirstByHotelIdOrderByPriceAsc(int hotelId);
}
