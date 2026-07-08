package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.FeedbackReply;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Integer> {

    List<FeedbackReply> findByHotelId(int hotelId);
    Optional<FeedbackReply> findByFeedbackId(int feedbackId);

    @Query(value = "SELECT fr FROM FeedbackReply fr " +
            "JOIN FETCH fr.feedback " +
            "JOIN FETCH fr.owner " +
            "JOIN FETCH fr.hotel",
            countQuery = "SELECT COUNT(fr) FROM FeedbackReply fr")
    Page<FeedbackReply> findAllWithAssociations(Pageable pageable);

    @Query(value = "SELECT fr FROM FeedbackReply fr " +
            "JOIN FETCH fr.feedback " +
            "JOIN FETCH fr.owner " +
            "JOIN FETCH fr.hotel " +
            "WHERE LOWER(fr.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(fr.hotel.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(fr.owner.fullName) LIKE LOWER(CONCAT('%', :query, '%'))",
            countQuery = "SELECT COUNT(fr) FROM FeedbackReply fr " +
            "WHERE LOWER(fr.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(fr.hotel.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(fr.owner.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<FeedbackReply> searchFeedbackReplies(@Param("query") String query, Pageable pageable);
}
