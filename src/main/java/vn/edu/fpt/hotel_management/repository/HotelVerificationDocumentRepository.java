package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.hotel_management.entity.HotelVerificationDocument;
import java.util.List;

public interface HotelVerificationDocumentRepository extends JpaRepository<HotelVerificationDocument, Integer> {
    List<HotelVerificationDocument> findByHotelId(int hotelId);
}
