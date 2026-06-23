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

    // Lọc phòng theo khách sạn và khoảng giá
    @Query("SELECT r FROM Room r WHERE r.hotelId = :hotelId AND r.price BETWEEN :minPrice AND :maxPrice")
    List<Room> findByHotelIdAndPriceRange(
            @Param("hotelId") int hotelId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    // Lọc phòng theo khách sạn, loại phòng và khoảng giá
    @Query("SELECT r FROM Room r WHERE r.hotelId = :hotelId AND r.type IN :types AND r.price BETWEEN :minPrice AND :maxPrice")
    List<Room> filterByHotelAndTypesAndPrice(
            @Param("hotelId") int hotelId,
            @Param("types") List<String> types,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
