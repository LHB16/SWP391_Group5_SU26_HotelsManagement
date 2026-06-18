package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Room;

import java.util.List;

/**
 * Repository cung cấp các phương thức truy vấn dữ liệu cho bảng room.
 * Dùng field name mới: roomType (tránh JPQL reserved word "type"), numWindow
 * (tránh "window")
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

       /** Lấy tất cả phòng của một khách sạn theo hotel_id. */
       List<Room> findByHotelId(int hotelId);

       /**
        * Lọc phòng theo hotel_id và khoảng giá.
        */
       @Query("SELECT r FROM Room r " +
                     "WHERE r.hotelId = :hotelId " +
                     "AND r.price >= :minPrice " +
                     "AND r.price <= :maxPrice " +
                     "ORDER BY r.price ASC")
       List<Room> findByHotelIdAndPriceRange(
                     @Param("hotelId") int hotelId,
                     @Param("minPrice") double minPrice,
                     @Param("maxPrice") double maxPrice);

       /**
        * Lọc phòng theo hotel_id, loại phòng và khoảng giá.
        * Dùng r.roomType thay vì r.type để tránh JPQL reserved word.
        */
       @Query("SELECT r FROM Room r " +
                     "WHERE r.hotelId = :hotelId " +
                     "AND r.roomType IN :types " +
                     "AND r.price >= :minPrice " +
                     "AND r.price <= :maxPrice " +
                     "ORDER BY r.price ASC")
       List<Room> filterByHotelAndTypesAndPrice(
                     @Param("hotelId") int hotelId,
                     @Param("types") List<String> types,
                     @Param("minPrice") double minPrice,
                     @Param("maxPrice") double maxPrice);

       /**
        * Lấy danh sách loại phòng duy nhất của một khách sạn.
        * Dùng r.roomType thay vì r.type.
        */
       @Query("SELECT DISTINCT r.roomType FROM Room r " +
                     "WHERE r.hotelId = :hotelId " +
                     "ORDER BY r.roomType ASC")
       List<String> findDistinctTypesByHotelId(@Param("hotelId") int hotelId);
}
