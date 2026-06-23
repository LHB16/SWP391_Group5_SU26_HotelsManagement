package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Review;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.ReviewRepository;

@Controller
public class ReviewController {

    private final ReviewRepository reviewRepository;

    public ReviewController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // ======================== POST /hotels/{id}/reviews ========================
    @PostMapping("/hotels/{id}/reviews")
    public String createReview(
            @PathVariable("id") int hotelId,
            @RequestParam("rating") int rating,
            @RequestParam("comment") String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to submit a review.");
            return "redirect:/login";
        }

        // Giới hạn chỉ cho khách hàng (CUSTOMER) gửi đánh giá
        if (!"CUSTOMER".equalsIgnoreCase(loggedInUser.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only customers can submit reviews.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        // Giới hạn mỗi tài khoản chỉ được đánh giá 1 lần cho 1 khách sạn
        if (reviewRepository.existsByHotelIdAndUserId(hotelId, loggedInUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You have already reviewed this hotel. You can only review once.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rating must be between 1 and 5 stars.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        Review review = new Review();
        review.setHotelId(hotelId);
        review.setUserId(loggedInUser.getId());
        review.setUserFullName(loggedInUser.getFullName());
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : "");

        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successMessage", "Review submitted successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }

    // ======================== POST /hotels/{id}/reviews/{reviewId}/delete ========================
    @PostMapping("/hotels/{id}/reviews/{reviewId}/delete")
    public String deleteReview(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to perform this action.");
            return "redirect:/login";
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (review.getUserId() != loggedInUser.getId() && !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to delete this review.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        reviewRepository.delete(review);

        redirectAttributes.addFlashAttribute("successMessage", "Review deleted successfully.");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }
}
