package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final FeedbackVoteRepository feedbackVoteRepository;

    public ReviewController(ReviewRepository reviewRepository,
                            CustomerRepository customerRepository,
                            HotelRepository hotelRepository,
                            RoomRepository roomRepository,
                            BookingRepository bookingRepository,
                            FeedbackReplyRepository feedbackReplyRepository,
                            HotelOwnerRepository hotelOwnerRepository,
                            FeedbackVoteRepository feedbackVoteRepository) {
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.feedbackReplyRepository = feedbackReplyRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.feedbackVoteRepository = feedbackVoteRepository;
    }

    @PostMapping("/hotels/{id}/reviews")
    public String createReview(
            @PathVariable("id") int hotelId,
            @RequestParam("rating") int rating,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "roomId", required = false) Integer roomId,
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

        // Tự động phân giải roomId và bookingId từ các completed booking của khách hàng nếu bị thiếu
        if (roomId == null) {
            List<Booking> customerBookings = bookingRepository.findBookingsByCustomerAndHotel(
                    customer.getId(),
                    hotelId,
                    List.of("COMPLETED")
            );
            if (!customerBookings.isEmpty()) {
                Booking targetBooking = null;
                if (bookingId != null) {
                    final Integer finalBookingId = bookingId;
                    targetBooking = customerBookings.stream()
                            .filter(b -> b.getId() == finalBookingId)
                            .findFirst()
                            .orElse(null);
                }
                if (targetBooking == null) {
                    targetBooking = customerBookings.get(0);
                }
                roomId = targetBooking.getRoom().getId();
                if (bookingId == null) {
                    bookingId = targetBooking.getId();
                }
            }
        }

        if (roomId == null) {
            session.setAttribute("errorMessage", "You must have a completed booking at this hotel to submit a review.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (rating < 1 || rating > 5) {
            session.setAttribute("errorMessage", "Rating must be between 1 and 5 stars.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found!"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found!"));

        final int finalRoomId = roomId;

        // Find completed bookings for this specific room
        List<Booking> completedBookingsForRoom = bookingRepository.findBookingsByCustomerAndHotel(
                customer.getId(),
                hotelId,
                List.of("COMPLETED")
        ).stream().filter(b -> b.getRoom().getId() == finalRoomId).collect(Collectors.toList());

        // Find reviews for this specific room
        List<Review> roomReviews = reviewRepository.findByHotelIdAndCustomerId(hotelId, customer.getId())
                .stream().filter(r -> r.getRoom() != null && r.getRoom().getId() == finalRoomId).collect(Collectors.toList());

        if (completedBookingsForRoom.isEmpty()) {
            session.setAttribute("errorMessage", "You must have a completed booking for this room type to submit a review.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        if (roomReviews.size() >= completedBookingsForRoom.size()) {
            session.setAttribute("errorMessage", "You have already reviewed all your bookings for this room type.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        String combinedComment = (comment != null ? comment.trim() : "");

        Review review = new Review();
        review.setHotel(hotel);
        review.setCustomer(customer);
        review.setUserFullName(customer.getFullName());
        review.setRating(rating);
        review.setComment(combinedComment);
        review.setRoom(room);
        review.setRoomType(room.getType());
        review.setStatus("VISIBLE");

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

        String combinedComment = (comment != null ? comment.trim() : "");

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

        // Xóa phản hồi của khách sạn liên kết với đánh giá này trước (nếu có) để tránh lỗi khóa ngoại do ON DELETE NO ACTION
        feedbackReplyRepository.findByFeedbackId(reviewId).ifPresent(reply -> {
            feedbackReplyRepository.delete(reply);
        });

        reviewRepository.delete(review);

        session.setAttribute("successMessage", "Review deleted successfully.");
        return "redirect:/hotels/" + hotelId + "/rooms";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/reply")
    public String replyFeedback(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam("content") String content,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            session.setAttribute("errorMessage", "Only hotel owners can reply to feedback.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }
        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (owner == null || hotel == null || hotel.getOwner() == null || hotel.getOwner().getId() != owner.getId()) {
            session.setAttribute("errorMessage", "You don't have permission to reply to this hotel's feedback.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getHotel().getId() != hotelId) {
            session.setAttribute("errorMessage", "Feedback not found.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        // Check if already replied
        if (feedbackReplyRepository.findByFeedbackId(reviewId).isPresent()) {
            session.setAttribute("errorMessage", "You have already replied to this feedback.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        FeedbackReply reply = new FeedbackReply();
        reply.setFeedback(review);
        reply.setOwner(owner);
        reply.setHotel(hotel);
        reply.setContent(content);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        feedbackReplyRepository.save(reply);

        session.setAttribute("successMessage", "Reply submitted successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms#reviews";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/reply/edit")
    public String editReplyFeedback(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam("content") String content,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            session.setAttribute("errorMessage", "Only hotel owners can edit replies.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }
        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (owner == null) {
            session.setAttribute("errorMessage", "Owner profile not found.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        FeedbackReply reply = feedbackReplyRepository.findByFeedbackId(reviewId).orElse(null);
        if (reply == null || reply.getOwner().getId() != owner.getId()) {
            session.setAttribute("errorMessage", "Reply not found or you don't have permission to edit it.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        reply.setContent(content);
        reply.setUpdatedAt(LocalDateTime.now());
        feedbackReplyRepository.save(reply);

        session.setAttribute("successMessage", "Reply updated successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms#reviews";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/reply/delete")
    public String deleteReplyFeedback(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            session.setAttribute("errorMessage", "Only hotel owners can delete replies.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }
        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (owner == null) {
            session.setAttribute("errorMessage", "Owner profile not found.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        FeedbackReply reply = feedbackReplyRepository.findByFeedbackId(reviewId).orElse(null);
        if (reply == null || reply.getOwner().getId() != owner.getId()) {
            session.setAttribute("errorMessage", "Reply not found or you don't have permission to delete it.");
            return "redirect:/hotels/" + hotelId + "/rooms#reviews";
        }

        feedbackReplyRepository.delete(reply);

        session.setAttribute("successMessage", "Reply deleted successfully!");
        return "redirect:/hotels/" + hotelId + "/rooms#reviews";
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/vote")
    @ResponseBody
    public java.util.Map<String, Object> voteReview(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam("type") String voteType,
            HttpSession session) {
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            response.put("success", false);
            response.put("message", "Please log in to vote.");
            return response;
        }

        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (customer == null) {
            response.put("success", false);
            response.put("message", "Only customers can vote.");
            return response;
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getHotel().getId() != hotelId) {
            response.put("success", false);
            response.put("message", "Review not found.");
            return response;
        }

        if (!"UPVOTE".equalsIgnoreCase(voteType) && !"DOWNVOTE".equalsIgnoreCase(voteType)) {
            response.put("success", false);
            response.put("message", "Invalid vote type.");
            return response;
        }

        Optional<FeedbackVote> existingVoteOpt = feedbackVoteRepository.findByFeedbackIdAndCustomerId(reviewId, customer.getId());
        String finalVoteType = voteType.toUpperCase();
        String userVoteResult = null;

        if (existingVoteOpt.isPresent()) {
            FeedbackVote existingVote = existingVoteOpt.get();
            if (existingVote.getVoteType().equalsIgnoreCase(finalVoteType)) {
                // Cancel vote
                feedbackVoteRepository.delete(existingVote);
                if ("UPVOTE".equals(finalVoteType)) {
                    review.setUpvote(Math.max(0, review.getUpvote() - 1));
                } else {
                    review.setDownvote(Math.max(0, review.getDownvote() - 1));
                }
                userVoteResult = "NONE";
            } else {
                // Change vote type
                existingVote.setVoteType(finalVoteType);
                feedbackVoteRepository.save(existingVote);
                if ("UPVOTE".equals(finalVoteType)) {
                    review.setUpvote(review.getUpvote() + 1);
                    review.setDownvote(Math.max(0, review.getDownvote() - 1));
                } else {
                    review.setDownvote(review.getDownvote() + 1);
                    review.setUpvote(Math.max(0, review.getUpvote() - 1));
                }
                userVoteResult = finalVoteType;
            }
        } else {
            // New vote
            FeedbackVote newVote = new FeedbackVote(review, customer, finalVoteType);
            feedbackVoteRepository.save(newVote);
            if ("UPVOTE".equals(finalVoteType)) {
                review.setUpvote(review.getUpvote() + 1);
            } else {
                review.setDownvote(review.getDownvote() + 1);
            }
            userVoteResult = finalVoteType;
        }

        reviewRepository.save(review);

        response.put("success", true);
        response.put("upvotes", review.getUpvote());
        response.put("downvotes", review.getDownvote());
        response.put("userVote", userVoteResult);
        return response;
    }

    @PostMapping("/admin/reviews/{reviewId}/status")
    public String updateReviewStatus(
            @PathVariable("reviewId") int reviewId,
            @RequestParam("status") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/admin/dashboard?tab=customerReviewPanel";
        }

        if (!"VISIBLE".equalsIgnoreCase(status) && !"HIDDEN".equalsIgnoreCase(status) && !"PENDING".equalsIgnoreCase(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status.");
            return "redirect:/admin/dashboard?tab=customerReviewPanel";
        }

        review.setStatus(status.toUpperCase());
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successMessage", "Review status updated to " + status.toUpperCase() + " successfully.");
        return "redirect:/admin/dashboard?tab=customerReviewPanel&page=" + page;
    }

    @PostMapping("/hotels/{id}/reviews/{reviewId}/status")
    public String updateReviewStatusFromDetail(
            @PathVariable("id") int hotelId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam("status") String status,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getHotel().getId() != hotelId) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        review.setStatus(status.toUpperCase());
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successMessage", "Review status updated successfully.");
        return "redirect:/hotels/" + hotelId + "/rooms#reviews";
    }
}
