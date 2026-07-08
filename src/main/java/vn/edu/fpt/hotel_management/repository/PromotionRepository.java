package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Promotion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    List<Promotion> findByHotelIdOrderByCreatedAtDesc(int hotelId);

    List<Promotion> findByHotelIdAndStatusOrderByCreatedAtDesc(int hotelId, String status);

    @Query("SELECT p FROM Promotion p WHERE p.hotel.id = :hotelId ORDER BY p.createdAt DESC")
    List<Promotion> findByHotelIdSimple(@Param("hotelId") int hotelId);

    @Query("SELECT p FROM Promotion p WHERE p.hotel.owner.id = :ownerId ORDER BY p.createdAt DESC")
    List<Promotion> findByOwnerId(@Param("ownerId") int ownerId);

    @Query("SELECT p FROM Promotion p WHERE p.hotel.id = :hotelId " +
            "AND p.status = 'ACTIVE' " +
            "AND p.startDate <= :today " +
            "AND p.endDate >= :today")
    List<Promotion> findActivePromotionsByHotelId(@Param("hotelId") int hotelId, @Param("today") LocalDate today);

    Optional<Promotion> findByIdAndHotelId(int id, int hotelId);

    long countByHotelId(int hotelId);
}