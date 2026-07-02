package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.entity.Review;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.repository.ReviewRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

@Controller
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final HotelRepository hotelRepository;

    public ReviewController(ReviewRepository reviewRepository,
                            CustomerRepository customerRepository,
                            HotelRepository hotelRepository) {
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.hotelRepository = hotelRepository;
    }

    @PostMapping("/hotels/{id}/reviews")
    public String createReview(
            @PathVariable("id") int hotelId,
            @RequestParam("rating") int rating,
            @RequestParam("comment") String comment,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            session.setAttribute("errorMessage", "Please log in to submit a review.");
            return "redirect:/login";
        }

        if (!"CUSTOMER".equalsIgnoreCase(loggedInUser.getRole())) {
            session.setAttribute("errorMessage", "Only customers can submit reviews.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        Customer customer = customerRepository.findByUserAccount(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Customer profile not found!"));

        if (reviewRepository.existsByHotelIdAndCustomerId(hotelId, customer.getId())) {
            session.setAttribute("errorMessage", "You have already reviewed this hotel. You can only review once.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (rating < 1 || rating > 5) {
            session.setAttribute("errorMessage", "Rating must be between 1 and 5 stars.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found!"));

        Review review = new Review();
        review.setHotel(hotel);
        review.setCustomer(customer);
        review.setUserFullName(customer.getFullName());
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : "");

        reviewRepository.save(review);

        session.setAttribute("successMessage", "Review submitted successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/edit")
    public String updateReview(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam("rating") int rating,
            @RequestParam("comment") String comment,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            session.setAttribute("errorMessage", "Please log in to edit a review.");
            return "redirect:/login";
        }

        Customer customer = customerRepository.findByUserAccount(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Customer profile not found!"));

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            session.setAttribute("errorMessage", "Review not found.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (review.getCustomer().getId() != customer.getId()) {
            session.setAttribute("errorMessage", "You are not authorized to edit this review.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (rating < 1 || rating > 5) {
            session.setAttribute("errorMessage", "Rating must be between 1 and 5 stars.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : "");
        reviewRepository.save(review);

        session.setAttribute("successMessage", "Review updated successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/delete")
    public String deleteReview(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            session.setAttribute("errorMessage", "Please log in to perform this action.");
            return "redirect:/login";
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            session.setAttribute("errorMessage", "Review not found.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(loggedInUser.getRole());
        boolean isOwner = customerRepository.findByUserAccount(loggedInUser)
                .map(customer -> review.getCustomer() != null && review.getCustomer().getId() == customer.getId())
                .orElse(false);

        if (!isOwner && !isAdmin) {
            session.setAttribute("errorMessage", "You are not authorized to delete this review.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        reviewRepository.delete(review);

        session.setAttribute("successMessage", "Review deleted successfully.");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }
}
