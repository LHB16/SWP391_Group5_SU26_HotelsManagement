package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.FeedbackVote;
import java.util.Optional;

@Repository
public interface FeedbackVoteRepository extends JpaRepository<FeedbackVote, Integer> {
    Optional<FeedbackVote> findByFeedbackIdAndCustomerId(int feedbackId, int customerId);
    long countByFeedbackIdAndVoteType(int feedbackId, String voteType);
    void deleteByFeedbackIdAndCustomerId(int feedbackId, int customerId);
}
