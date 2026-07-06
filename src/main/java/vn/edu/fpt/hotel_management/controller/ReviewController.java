package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.entity.Review;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.repository.ReviewRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.BookingRepository;

@Controller
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public ReviewController(ReviewRepository reviewRepository,
                            CustomerRepository customerRepository,
                            HotelRepository hotelRepository,
                            RoomRepository roomRepository,
                            BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping("/hotels/{id}/reviews")
    public String createReview(
            @PathVariable("id") int hotelId,
            @RequestParam("rating") int rating,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam("roomId") int roomId,
            @RequestParam(value = "bookingId", required = false) Integer bookingId,
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

        if (rating < 1 || rating > 5) {
            session.setAttribute("errorMessage", "Rating must be between 1 and 5 stars.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found!"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found!"));

        // Find completed bookings for this specific room
        java.util.List<Booking> completedBookingsForRoom = bookingRepository.findBookingsByCustomerAndHotel(
                customer.getId(),
                hotelId,
                java.util.List.of("COMPLETED")
        ).stream().filter(b -> b.getRoom().getId() == roomId).collect(java.util.stream.Collectors.toList());

        // Find reviews for this specific room
        java.util.List<Review> roomReviews = reviewRepository.findByHotelIdAndCustomerId(hotelId, customer.getId())
                .stream().filter(r -> r.getRoom() != null && r.getRoom().getId() == roomId).collect(java.util.stream.Collectors.toList());

        if (completedBookingsForRoom.isEmpty()) {
            session.setAttribute("errorMessage", "You must have a completed booking for this room type to submit a review.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (roomReviews.size() >= completedBookingsForRoom.size()) {
            session.setAttribute("errorMessage", "You have already reviewed all your bookings for this room type.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        String combinedComment = (title != null ? title.trim() : "") + "|||"
                + (comment != null ? comment.trim() : "");

        Review review = new Review();
        review.setHotel(hotel);
        review.setCustomer(customer);
        review.setUserFullName(customer.getFullName());
        review.setRating(rating);
        review.setComment(combinedComment);
        review.setRoom(room);
        review.setRoomType(room.getType());

        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null && booking.getCustomer().getId() == customer.getId()) {
                review.setBooking(booking);
            }
        } else {
            Booking fallbackBooking = completedBookingsForRoom.stream()
                    .filter(b -> roomReviews.stream().noneMatch(r -> r.getBooking() != null && r.getBooking().getId() == b.getId()))
                    .findFirst()
                    .orElse(null);
            if (fallbackBooking != null) {
                review.setBooking(fallbackBooking);
            }
        }

        reviewRepository.save(review);

        session.setAttribute("successMessage", "Review submitted successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/edit")
    public String updateReview(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam("rating") int rating,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "comment", required = false) String comment,
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

        String combinedComment = (title != null ? title.trim() : "") + "|||"
                + (comment != null ? comment.trim() : "");

        review.setRating(rating);
        review.setComment(combinedComment);
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
