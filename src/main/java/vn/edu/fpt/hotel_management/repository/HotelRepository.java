package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Hotel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

    List<Hotel> findByActiveTrue();

    @Query("SELECT h FROM Hotel h WHERE h.active = true " +
            "AND (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) >= :minPrice " +
            "AND (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) <= :maxPrice " +
            "ORDER BY h.rating DESC, (SELECT COALESCE(MIN(r.price), 0) FROM Room r WHERE r.hotelId = h.id) ASC")
    List<Hotel> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

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

    List<Hotel> findByOwnerId(int ownerId);

    Optional<Hotel> findByIdAndOwnerId(int id, int ownerId);

    Page<Hotel> findByApprovalStatus(String approvalStatus, Pageable pageable);

    Page<Hotel> findByApprovalStatusAndNameContainingIgnoreCase(String approvalStatus, String name, Pageable pageable);

    Page<Hotel> findByNameContainingIgnoreCase(String name, Pageable pageable);
}