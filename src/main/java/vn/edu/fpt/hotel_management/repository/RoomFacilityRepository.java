package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.entity.RoomFacility;

import java.util.Optional;

@Repository
public interface RoomFacilityRepository extends JpaRepository<RoomFacility, Integer> {
    Optional<RoomFacility> findByRoom(Room room);
}
