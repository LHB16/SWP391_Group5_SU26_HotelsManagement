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

    List<Hotel> findTop6ByActiveTrueAndApprovalStatusOrderByRatingDesc(String approvalStatus);

    @Query("SELECT h FROM Hotel h WHERE h.active = true AND h.approvalStatus = 'APPROVED' " +
            "AND EXISTS (SELECT p FROM Promotion p WHERE p.hotel = h AND p.status = 'ACTIVE' AND p.startDate <= :today AND p.endDate >= :today) " +
            "ORDER BY (SELECT COALESCE(MAX(p2.discountPercent), 0) FROM Promotion p2 WHERE p2.hotel = h AND p2.status = 'ACTIVE' AND p2.startDate <= :today AND p2.endDate >= :today) DESC")
    List<Hotel> findHotelsWithActivePromotions(@Param("today") java.time.LocalDate today);

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
    
    long countByApprovalStatus(String approvalStatus);
    
    long countByApprovalStatusAndOwnerId(String approvalStatus, int ownerId);

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.city IS NOT NULL AND TRIM(h.city) <> '' ORDER BY h.city ASC")
    List<String> findDistinctCitiesAlphabetically();
}