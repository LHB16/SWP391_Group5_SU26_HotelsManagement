package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.BookingRepository;
import vn.edu.fpt.hotel_management.repository.PaymentRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.RefundRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.FeedbackRepository;
import vn.edu.fpt.hotel_management.repository.PromotionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BookingController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FeedbackRepository FeedbackRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @org.springframework.beans.factory.annotation.Value("${payment.payos.client-id:}")
    private String payosClientId;

    @org.springframework.beans.factory.annotation.Value("${payment.payos.api-key:}")
    private String payosApiKey;

    // Hiển thị trang lịch sử đặt phòng của người dùng đang đăng nhập
    @GetMapping("/booking/history")
    public String showBookingHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "status", required = false, defaultValue = "") String status,
            @RequestParam(name = "roomType", required = false, defaultValue = "") String roomType,
            @RequestParam(name = "checkIn", required = false, defaultValue = "") String checkIn,
            @RequestParam(name = "checkOut", required = false, defaultValue = "") String checkOut,
            HttpSession session,
            Model model) {

        // Kiểm tra đăng nhập, chưa đăng nhập thì redirect về trang login
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        int customerId = (customer != null) ? customer.getId() : 0;

        // Dọn dẹp booking hết hạn
        cleanExpiredBookings(customerId);

        // Lấy tất cả booking của user để lọc trong bộ nhớ
        List<Booking> allBookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        // Tự động kiểm tra trạng thái thanh toán đối với các đơn hàng còn PENDING khi
        // truy cập Lịch sử đặt phòng
        for (Booking b : allBookings) {
            if ("PENDING".equals(b.getStatus())) {
                Payment payment = paymentRepository.findByBookingId(b.getId()).orElse(null);

                // Chỉ tiến hành kiểm tra trên PayOS đối với các booking chưa hết hạn (chỉ gọi API đối với phòng cha)
                boolean isChild = b.getSpecialNotes() != null
                        && b.getSpecialNotes().startsWith("GROUP_BOOKING_parent:");
                if (!isChild) {
                    boolean verified = checkPayOSPaymentStatus(b.getId(), b.getTotalPrice());
                    if (verified) {
                        b.setStatus("CONFIRMED");
                        b.setUpdatedAt(java.time.LocalDateTime.now());
                        bookingRepository.save(b);

                        if (payment != null) {
                            payment.setStatus("PAID");
                            payment.setPaidAt(java.time.LocalDateTime.now());
                            paymentRepository.save(payment);
                        }

                        // Đồng bộ xác nhận tất cả phòng con đi kèm
                        try {
                            final int parentId = b.getId();
                            List<Booking> childBookings = bookingRepository
                                    .findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                                    .filter(cb -> cb.getSpecialNotes() != null
                                            && cb.getSpecialNotes().equals("GROUP_BOOKING_parent:" + parentId))
                                    .collect(java.util.stream.Collectors.toList());
                            for (Booking cb : childBookings) {
                                cb.setStatus("CONFIRMED");
                                cb.setUpdatedAt(java.time.LocalDateTime.now());
                                bookingRepository.save(cb);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }

            // Tự động chuyển sang COMPLETED nếu đã qua 12h trưa ngày checkout
            if ("CONFIRMED".equals(b.getStatus())) {
                if (b.getCheckOutDate() != null
                        && b.getCheckOutDate().atTime(12, 0).isBefore(java.time.LocalDateTime.now())) {
                    b.setStatus("COMPLETED");
                    b.setUpdatedAt(java.time.LocalDateTime.now());
                    bookingRepository.save(b);
                }
            }
        }

        // Chuẩn bị tham số lọc và gọi Magic Method từ Repository
        String statusParam = (status != null) ? status.trim() : "";
        String roomTypeParam = (roomType != null) ? roomType.trim() : "";

        java.time.LocalDate filterCheckInDate = null;
        if (checkIn != null && !checkIn.trim().isEmpty()) {
            try {
                filterCheckInDate = java.time.LocalDate.parse(checkIn.trim());
            } catch (Exception e) {
            }
        }

        java.time.LocalDate filterCheckOutDate = null;
        if (checkOut != null && !checkOut.trim().isEmpty()) {
            try {
                filterCheckOutDate = java.time.LocalDate.parse(checkOut.trim());
            } catch (Exception e) {
            }
        }

        java.time.LocalDate checkInLimit = (filterCheckInDate != null) ? filterCheckInDate
                : java.time.LocalDate.of(1970, 1, 1);
        java.time.LocalDate checkOutLimit = (filterCheckOutDate != null) ? filterCheckOutDate
                : java.time.LocalDate.of(2099, 12, 31);

        List<Booking> filteredBookings = bookingRepository
                .findByCustomerIdAndStatusContainingIgnoreCaseAndRoomTypeContainingIgnoreCaseAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqualOrderByCreatedAtDesc(
                        customerId,
                        statusParam,
                        roomTypeParam,
                        checkOutLimit,
                        checkInLimit);

        // Lấy danh sách allRoomTypes của toàn bộ hệ thống để hiển thị trên Filter Panel
        List<String> allRoomTypes = new java.util.ArrayList<>();
        for (Room r : roomRepository.findAll()) {
            if (r.getRoomType() != null && !r.getRoomType().trim().isEmpty()
                    && !allRoomTypes.contains(r.getRoomType())) {
                allRoomTypes.add(r.getRoomType());
            }
        }

        // Phân trang danh sách đã lọc
        int pageSize = 6;
        int totalItems = filteredBookings.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) {
            totalPages = 1;
        }

        // Đảm bảo trang yêu cầu nằm trong khoảng hợp lệ
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIdx = page * pageSize;
        int endIdx = Math.min(startIdx + pageSize, totalItems);
        List<Booking> pagedBookings = new java.util.ArrayList<>();
        if (startIdx < totalItems) {
            pagedBookings = filteredBookings.subList(startIdx, endIdx);
        }

        // Tạo map hotelId -> tên khách sạn để hiển thị trên card
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = new java.util.HashMap<>();
        for (Hotel hotel : hotels) {
            hotelMap.put(hotel.getId(), hotel.getName());
        }

        Map<Integer, Boolean> refundEligibleMap = new java.util.HashMap<>();
        Map<Integer, Boolean> refundSubmittedMap = new java.util.HashMap<>();
        Map<Integer, Boolean> cancelableMap = new java.util.HashMap<>();
        Map<Integer, String> refundPolicyMap = new java.util.HashMap<>();

        java.time.LocalDateTime nowTime = java.time.LocalDateTime.now();

        for (Booking b : pagedBookings) {
            // Tính cancelable và refund policy trước khi hủy
            cancelableMap.put(b.getId(), b.isCancelable());
            if (b.getCheckInDate() != null) {
                java.time.LocalDateTime refundDeadline = b.getCheckInDate().minusDays(3).atTime(12, 0);
                refundPolicyMap.put(b.getId(), nowTime.isBefore(refundDeadline) ? "full" : "none");
            } else {
                refundPolicyMap.put(b.getId(), "none");
            }

            if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                Payment p = paymentRepository.findByBookingId(b.getId()).orElse(null);
                boolean alreadySubmitted = refundRepository.existsByBookingId(b.getId());
                // Kiểm tra điều kiện hoàn tiền
                boolean eligible = (p != null && "PAID".equalsIgnoreCase(p.getStatus())) && !alreadySubmitted;
                refundEligibleMap.put(b.getId(), eligible);
                refundSubmittedMap.put(b.getId(), alreadySubmitted);
            }
        }
        // Truyền dữ liệu vào model để render template
        model.addAttribute("user", loggedInUser);
        java.util.Set<Integer> reviewedBookingIds = new java.util.HashSet<>();
        if (customerId > 0) {
            List<Feedback> customerReviews = FeedbackRepository.findByCustomerId(customerId);
            for (Feedback r : customerReviews) {
                if (r.getBooking() != null) {
                    reviewedBookingIds.add(r.getBooking().getId());
                }
            }
        }
        model.addAttribute("reviewedBookingIds", reviewedBookingIds);
        model.addAttribute("bookings", pagedBookings);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hotelMap", hotelMap);
        model.addAttribute("refundEligibleMap", refundEligibleMap);
        model.addAttribute("refundSubmittedMap", refundSubmittedMap);
        model.addAttribute("cancelableMap", cancelableMap);
        model.addAttribute("refundPolicyMap", refundPolicyMap);
        model.addAttribute("now", java.time.LocalDateTime.now());

        // Truyền các bộ lọc hiện tại để giữ lại trên giao diện
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedRoomType", roomType);
        model.addAttribute("selectedCheckIn", checkIn);
        model.addAttribute("selectedCheckOut", checkOut);
        model.addAttribute("allRoomTypes", allRoomTypes);

        return "booking/history";
    }

    // Hiển thị trang chính sách hủy & điều khoản hoàn tiền
    @GetMapping("/booking/cancel-policy")
    public String showCancelPolicyPage(
            @RequestParam("bookingId") int bookingId,
            @RequestParam(name = "action", defaultValue = "cancel") String action,
            HttpSession session,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Tìm booking
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (booking == null || customer == null || booking.getCustomer().getId() != customer.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) && !"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid booking status.");
            return "redirect:/booking/history";
        }

        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            if (booking.getCheckInDate() == null || !java.time.LocalDate.now().isBefore(booking.getCheckInDate())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot cancel a booking after check-in date.");
                return "redirect:/booking/history";
            }
        }

        // Tính deadline
        java.time.LocalDateTime refundDeadline = booking.getCheckInDate().minusDays(3).atTime(12, 0);
        boolean isFullRefundEligible;
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            Payment p = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            isFullRefundEligible = p != null && "REFUNDED".equalsIgnoreCase(p.getStatus());
        } else {
            isFullRefundEligible = java.time.LocalDateTime.now().isBefore(refundDeadline);
        }

        // Định dạng deadline để hiển thị đẹp mắt
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDeadline = refundDeadline.format(formatter);

        // Lấy tên khách sạn
        Hotel hotel = hotelRepository.findById(booking.getRoom().getHotelId()).orElse(null);
        String hotelName = (hotel != null) ? hotel.getName() : "Hotel";

        model.addAttribute("booking", booking);
        model.addAttribute("hotelName", hotelName);
        model.addAttribute("policy", isFullRefundEligible ? "full" : "none");
        model.addAttribute("formattedDeadline", formattedDeadline);
        model.addAttribute("isFullRefundEligible", isFullRefundEligible);
        model.addAttribute("action", action);

        return "booking/cancel-policy";
    }

    // Hiển thị trang chọn lý do hủy đặt phòng
    @GetMapping("/booking/cancel/{id}")
    public String showCancelBookingPage(
            @PathVariable("id") int bookingId,
            HttpSession session,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Tìm booking theo id
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (booking == null || customer == null || booking.getCustomer().getId() != customer.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only confirmed bookings can be cancelled.");
            return "redirect:/booking/history";
        }

        if (booking.getCheckInDate() == null || !java.time.LocalDate.now().isBefore(booking.getCheckInDate())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot cancel a booking after check-in date.");
            return "redirect:/booking/history";
        }

        // Tính toán chi phí hủy
        java.time.LocalDateTime refundDeadline = booking.getCheckInDate().minusDays(3).atTime(12, 0);
        boolean isFullRefundEligible = java.time.LocalDateTime.now().isBefore(refundDeadline);

        java.math.BigDecimal cancellationFee = isFullRefundEligible ? java.math.BigDecimal.ZERO
                : booking.getTotalPrice();

        model.addAttribute("booking", booking);
        model.addAttribute("hotel", booking.getHotel());
        model.addAttribute("room", booking.getRoom());
        model.addAttribute("cancellationFee", cancellationFee);
        model.addAttribute("isFullRefundEligible", isFullRefundEligible);
        model.addAttribute("user", loggedInUser);

        return "booking/cancel";
    }

    // Hủy booking: cho phép hủy khi CONFIRMED và chưa đến ngày check-in
    // Chính sách hoàn tiền: trước 12:00 trưa ngày (checkin - 3 ngày) → 100% | sau
    // deadline → 0%
    @PostMapping("/booking/cancel/{id}")
    public String cancelBooking(
            @PathVariable("id") int bookingId,
            @RequestParam(name = "reason", required = false) String reason,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Tìm booking theo id
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (booking == null || customer == null || booking.getCustomer().getId() != customer.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only confirmed bookings can be cancelled.");
            return "redirect:/booking/history";
        }

        if (booking.getCheckInDate() == null || !java.time.LocalDate.now().isBefore(booking.getCheckInDate())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot cancel a booking after check-in date.");
            return "redirect:/booking/history";
        }

        // Tính deadline hoàn tiền: 12:00 trưa của ngày (check-in - 3 ngày)
        java.time.LocalDateTime refundDeadline = booking.getCheckInDate()
                .minusDays(3)
                .atTime(12, 0);
        boolean isFullRefundEligible = java.time.LocalDateTime.now().isBefore(refundDeadline);

        if (isFullRefundEligible) {
            // Đủ điều kiện hoàn tiền: KHÔNG hủy ngay tại đây
            // Lưu lý do hủy vào session
            session.setAttribute("cancelReason_" + bookingId, reason);
            return "redirect:/booking/refund-request?bookingId=" + bookingId;
        } else {
            // Quá hạn hoàn tiền: HỦY NGAY
            booking.setStatus("CANCELLED");
            if (reason != null && !reason.isBlank()) {
                booking.setSpecialNotes((booking.getSpecialNotes() != null ? booking.getSpecialNotes() + "\n" : "")
                        + "Reason for canceling: " + reason);
            }
            bookingRepository.save(booking);

            // Cập nhật payment
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
            if (payment != null) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
            }

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Booking cancelled. Unfortunately, cancellations within 3 days of check-in are non-refundable per our policy.");
            return "redirect:/booking/history";
        }
    }

    // Hiển thị form yêu cầu hoàn tiền
    @GetMapping("/booking/refund-request")
    public String showRefundRequestPage(
            @RequestParam("bookingId") int bookingId,
            HttpSession session,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (booking == null || customer == null || booking.getCustomer().getId() != customer.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        // Chấp nhận booking CONFIRMED (đang trong luồng yêu cầu hoàn tiền để hủy)
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only confirmed bookings can initiate a refund.");
            return "redirect:/booking/history";
        }

        // Kiểm tra xem đã gửi yêu cầu hoàn tiền chưa
        if (refundRepository.existsByBookingId(bookingId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "A refund request for this booking has already been submitted.");
            return "redirect:/booking/history";
        }

        // Đảm bảo đơn đã thanh toán (Payment status là PAID hoặc SUCCESSFUL)
        Payment p = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (p == null || (!"PAID".equalsIgnoreCase(p.getStatus()) && !"SUCCESSFUL".equalsIgnoreCase(p.getStatus()))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This booking has not been paid or is not eligible for a refund.");
            return "redirect:/booking/history";
        }

        // Kiểm tra lại hạn hoàn tiền
        java.time.LocalDateTime refundDeadline = booking.getCheckInDate().minusDays(3).atTime(12, 0);
        if (!java.time.LocalDateTime.now().isBefore(refundDeadline)) {
            redirectAttributes.addFlashAttribute("errorMessage", "The refund deadline has passed.");
            return "redirect:/booking/history";
        }

        // Hoàn tiền 100%
        java.math.BigDecimal refundAmount = booking.getTotalPrice()
                .setScale(0, java.math.RoundingMode.HALF_UP);

        // Lấy tên khách sạn
        Room room = booking.getRoom();
        Hotel hotel = (room != null) ? hotelRepository.findById(room.getHotelId()).orElse(null) : null;

        model.addAttribute("user", loggedInUser);
        model.addAttribute("booking", booking);
        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("refundAmount", refundAmount);
        model.addAttribute("refundDeadline", refundDeadline);

        return "booking/refund-request";
    }

    // Xử lý submit form yêu cầu hoàn tiền
    @PostMapping("/booking/refund-request")
    public String submitRefundRequest(
            @RequestParam("bookingId") int bookingId,
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountHolder") String accountHolder,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (booking == null || customer == null || booking.getCustomer().getId() != customer.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This booking is not eligible for cancellation/refund.");
            return "redirect:/booking/history";
        }

        if (refundRepository.existsByBookingId(bookingId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "A refund request for this booking has already been submitted.");
            return "redirect:/booking/history";
        }

        // Validate input
        if (bankName == null || bankName.isBlank() ||
                accountNumber == null || accountNumber.isBlank() ||
                accountHolder == null || accountHolder.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fill in all bank account information.");
            return "redirect:/booking/refund-request?bookingId=" + bookingId;
        }

        // Kiểm tra lại hạn hoàn tiền
        java.time.LocalDateTime refundDeadline = booking.getCheckInDate().minusDays(3).atTime(12, 0);
        if (!java.time.LocalDateTime.now().isBefore(refundDeadline)) {
            redirectAttributes.addFlashAttribute("errorMessage", "The refund deadline has passed.");
            return "redirect:/booking/history";
        }

        Payment p = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (p == null || (!"PAID".equalsIgnoreCase(p.getStatus()) && !"SUCCESSFUL".equalsIgnoreCase(p.getStatus()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "This booking is not eligible for a refund.");
            return "redirect:/booking/history";
        }

        // ĐỔI TRẠNG THÁI BOOKING THÀNH CANCELLED CHỈ KHI SUBMIT REFUND REQUEST THÀNH
        // CÔNG
        booking.setStatus("CANCELLED");

        // Lấy lý do hủy từ session và ghi vào specialNotes
        String reason = (String) session.getAttribute("cancelReason_" + bookingId);
        if (reason != null && !reason.isBlank()) {
            booking.setSpecialNotes((booking.getSpecialNotes() != null ? booking.getSpecialNotes() + "\n" : "")
                    + "Reason for canceling: " + reason);
        }
        bookingRepository.save(booking);
        session.removeAttribute("cancelReason_" + bookingId); // dọn dẹp session

        // Trạng thái thanh toán vẫn giữ là PAID, chỉ chuyển sang REFUNDED khi Admin
        // Approve hoàn tiền thành công

        // Hoàn tiền 100%
        java.math.BigDecimal refundAmount = booking.getTotalPrice()
                .setScale(0, java.math.RoundingMode.HALF_UP);

        // Tạo bản ghi Refund
        Refund refund = new Refund();
        refund.setBooking(booking);
        refund.setBankName(bankName.trim());
        refund.setAccountNumber(accountNumber.trim());
        refund.setAccountHolder(accountHolder.trim().toUpperCase());
        refund.setRefundAmount(refundAmount);
        refund.setStatus("PENDING");
        refund.setCancellationReason(reason);
        refund.setRequestedAt(java.time.LocalDateTime.now());
        refundRepository.save(refund);

        String formattedAmount = String.format("%,.0f", refundAmount.doubleValue());
        redirectAttributes.addFlashAttribute("successMessage",
                "Booking cancelled and refund request submitted successfully! " + formattedAmount
                        + " VND will be processed within 3-5 business days.");
        return "redirect:/booking/history";
    }

    @GetMapping({ "/booking", "/booking/create" })
    public String showCreateBookingPage(
            @RequestParam(name = "hotelId", required = false) Integer hotelId,
            @RequestParam(name = "roomId", required = false) Integer roomId,
            @RequestParam(name = "roomIds", required = false) List<Integer> roomIds,
            @RequestParam(name = "checkin", required = false) String checkin,
            @RequestParam(name = "checkout", required = false) String checkout,
            @RequestParam(name = "checkins", required = false) List<String> checkins,
            @RequestParam(name = "checkouts", required = false) List<String> checkouts,
            @RequestParam(name = "quantities", required = false) List<Integer> quantities,
            HttpSession session,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập và vai trò (chỉ CUSTOMER đã đăng nhập mới được vào)
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"CUSTOMER".equalsIgnoreCase(loggedInUser.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only customers are allowed to access booking page.");
            return "redirect:/home";
        }

        // Kiểm tra khách sạn bị vô hiệu hóa trước khi xử lý
        if (hotelId != null) {
            Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
            if (hotel == null || !hotel.isActive()) {
                redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
                return "redirect:/hotels";
            }
        }
        if (roomId != null) {
            Room r = roomRepository.findById(roomId).orElse(null);
            if (r != null) {
                Hotel hotel = hotelRepository.findById(r.getHotelId()).orElse(null);
                if (hotel == null || !hotel.isActive()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
                    return "redirect:/hotels";
                }
            }
        }
        if (roomIds != null && !roomIds.isEmpty()) {
            for (Integer rId : roomIds) {
                Room r = roomRepository.findById(rId).orElse(null);
                if (r != null) {
                    Hotel hotel = hotelRepository.findById(r.getHotelId()).orElse(null);
                    if (hotel == null || !hotel.isActive()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
                        return "redirect:/hotels";
                    }
                }
            }
        }

        // Lấy thông tin user đăng nhập từ Session
        model.addAttribute("user", loggedInUser);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        model.addAttribute("customer", customer);

        // Lấy danh sách các phòng (chỉ lấy phòng được chọn nếu roomId hoặc roomIds có
        // giá trị)
        List<Room> rooms = new ArrayList<>();
        if (roomId != null) {
            Room r = roomRepository.findById(roomId).orElse(null);
            if (r != null) {
                rooms.add(r);
            }
            if (hotelId == null && r != null) {
                hotelId = r.getHotelId();
            }
            model.addAttribute("selectedHotelId", hotelId);
        } else if (roomIds != null && !roomIds.isEmpty()) {
            for (Integer rId : roomIds) {
                Room r = roomRepository.findById(rId).orElse(null);
                if (r != null) {
                    rooms.add(r);
                }
            }
            if (hotelId == null && !rooms.isEmpty()) {
                hotelId = rooms.get(0).getHotelId();
            }
            model.addAttribute("selectedHotelId", hotelId);
        } else if (hotelId != null) {
            rooms = roomRepository.findByHotelId(hotelId);
            model.addAttribute("selectedHotelId", hotelId);
        } else {
            // Lọc chỉ lấy các phòng của khách sạn đang active
            List<Room> allRooms = roomRepository.findAll();
            for (Room r : allRooms) {
                Hotel hotel = hotelRepository.findById(r.getHotelId()).orElse(null);
                if (hotel != null && hotel.isActive()) {
                    rooms.add(r);
                }
            }
        }
        model.addAttribute("rooms", rooms);

        // Lấy danh sách khách sạn để map ID sang tên khách sạn dễ hiển thị và địa chỉ
        // đầy đủ
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = new HashMap<>();
        Map<Integer, String> hotelAddressMap = new HashMap<>();
        for (Hotel hotel : hotels) {
            hotelMap.put(hotel.getId(), hotel.getName());
            String fullAddr = hotel.getAddress();
            if (hotel.getDistrict() != null && !hotel.getDistrict().trim().isEmpty()) {
                fullAddr += ", " + hotel.getDistrict().trim();
            }
            if (hotel.getCity() != null && !hotel.getCity().trim().isEmpty()) {
                fullAddr += ", " + hotel.getCity().trim();
            }
            hotelAddressMap.put(hotel.getId(), fullAddr);
        }
        model.addAttribute("hotelMap", hotelMap);
        model.addAttribute("hotelAddressMap", hotelAddressMap);

        // Chuẩn bị danh sách phòng được chọn
        List<Integer> selectedRoomIds = new ArrayList<>();
        if (roomIds != null) {
            selectedRoomIds.addAll(roomIds);
        }
        if (roomId != null && !selectedRoomIds.contains(roomId)) {
            selectedRoomIds.add(roomId);
        }

        // Đồng bộ checkins, checkouts, quantities theo selectedRoomIds
        List<String> finalCheckins = new ArrayList<>();
        List<String> finalCheckouts = new ArrayList<>();
        List<Integer> finalQuantities = new ArrayList<>();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate tomorrow = today.plusDays(1);

        for (int i = 0; i < selectedRoomIds.size(); i++) {
            String ci = null;
            if (checkins != null && checkins.size() > i) {
                ci = checkins.get(i);
            }
            if (ci == null || ci.trim().isEmpty()) {
                ci = (checkin != null && !checkin.trim().isEmpty()) ? checkin : today.toString();
            }
            finalCheckins.add(ci);

            String co = null;
            if (checkouts != null && checkouts.size() > i) {
                co = checkouts.get(i);
            }
            if (co == null || co.trim().isEmpty()) {
                co = (checkout != null && !checkout.trim().isEmpty()) ? checkout : tomorrow.toString();
            }
            finalCheckouts.add(co);

            Integer qty = 1;
            if (quantities != null && quantities.size() > i) {
                qty = quantities.get(i);
            }
            if (qty == null || qty < 1) {
                qty = 1;
            }
            finalQuantities.add(qty);
        }

        // Tính toán chi phí bằng Java có tăng 20% vào ngày lễ/cuối tuần
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Integer, BigDecimal> roomPricesMap = new java.util.HashMap<>();
        Map<Integer, BigDecimal> roomSinglePricesMap = new java.util.HashMap<>();
        Map<Integer, Long> roomNightsMap = new java.util.HashMap<>();

        for (int i = 0; i < selectedRoomIds.size(); i++) {
            Integer rId = selectedRoomIds.get(i);
            Room r = roomRepository.findById(rId).orElse(null);
            if (r != null) {
                String ci = finalCheckins.get(i);
                String co = finalCheckouts.get(i);
                Integer qty = finalQuantities.get(i);

                long nights = 1;
                try {
                    java.time.LocalDate d1 = java.time.LocalDate.parse(ci.trim());
                    java.time.LocalDate d2 = java.time.LocalDate.parse(co.trim());
                    if (d2.isAfter(d1)) {
                        nights = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
                    }
                } catch (Exception e) {
                }

                BigDecimal singleRoomSubtotal = BigDecimal.ZERO;
                try {
                    singleRoomSubtotal = calculateRoomSubtotal(r.getPrice(), java.time.LocalDate.parse(ci.trim()),
                            java.time.LocalDate.parse(co.trim()));
                } catch (Exception e) {
                    singleRoomSubtotal = r.getPrice().multiply(BigDecimal.valueOf(nights));
                }

                BigDecimal totalRoomSubtotal = singleRoomSubtotal.multiply(BigDecimal.valueOf(qty));

                roomPricesMap.put(rId, totalRoomSubtotal);
                roomSinglePricesMap.put(rId, singleRoomSubtotal);
                roomNightsMap.put(rId, nights);
                subtotal = subtotal.add(totalRoomSubtotal);
            }
        }

        model.addAttribute("roomPricesMap", roomPricesMap);
        model.addAttribute("roomSinglePricesMap", roomSinglePricesMap);
        model.addAttribute("roomNightsMap", roomNightsMap);

        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50000) : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(tax).add(serviceFee);

        // Truyền các giá trị tính toán vào model
        model.addAttribute("today", java.time.LocalDate.now().toString());
        model.addAttribute("checkin", checkin != null ? checkin : today.toString());
        model.addAttribute("checkout", checkout != null ? checkout : tomorrow.toString());
        model.addAttribute("checkins", finalCheckins);
        model.addAttribute("checkouts", finalCheckouts);
        model.addAttribute("quantities", finalQuantities);
        model.addAttribute("selectedRoomIds", selectedRoomIds);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("serviceFee", serviceFee);
        model.addAttribute("grandTotal", grandTotal);

        // Thu thập các hotelId từ danh sách phòng đã chọn
        java.util.Set<Integer> hotelIds = new java.util.HashSet<>();
        for (Integer rId : selectedRoomIds) {
            Room r = roomRepository.findById(rId).orElse(null);
            if (r != null) {
                hotelIds.add(r.getHotelId());
            }
        }

        // Lấy danh sách khuyến mãi còn hiệu lực của tất cả các khách sạn có phòng được
        // chọn
        int customerId = (customer != null) ? customer.getId() : 0;
        
        // Dọn dẹp booking hết hạn
        cleanExpiredBookings(customerId);

        List<Promotion> rawPromotions = new java.util.ArrayList<>();
        for (Integer hId : hotelIds) {
            List<Promotion> hotelPromos = promotionRepository.findActivePromotionsByHotelId(hId,
                    java.time.LocalDate.now());
            if (hotelPromos != null) {
                for (Promotion promo : hotelPromos) {
                    if (customerId == 0
                            || !bookingRepository.existsByCustomerIdAndIdPromotion(customerId, promo.getId())) {
                        rawPromotions.add(promo);
                    }
                }
            }
        }
        List<Map<String, Object>> promotionsList = new java.util.ArrayList<>();
        for (Promotion p : rawPromotions) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("description", p.getDescription());
            map.put("discountPercent", p.getDiscountPercent());
            map.put("status", p.getStatus());
            map.put("hotelId", p.getHotel() != null ? p.getHotel().getId() : 0);
            map.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
            map.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
            promotionsList.add(map);
        }
        model.addAttribute("promotions", promotionsList);

        // Trả về file giao diện templates/booking/create.html
        return "booking/create";
    }

    private boolean isHolidayOrWeekend(java.time.LocalDate date) {
        // 1. Kiểm tra cuối tuần (Thứ 7 & Chủ Nhật)
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        // 2. Kiểm tra ngày lễ
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        // Lễ dương lịch VN cố định
        if (m == 1 && d == 1)
            return true; // Tết Dương Lịch
        if (m == 4 && d == 30)
            return true; // Giải phóng Miền Nam
        if (m == 5 && d == 1)
            return true; // Quốc tế Lao động
        if (m == 9 && d == 2)
            return true; // Quốc khánh

        // Các ngày lễ đặc biệt yêu cầu thêm
        if (m == 2 && d == 14)
            return true; // Valentine
        if (m == 3 && d == 8)
            return true; // Quốc tế Phụ nữ
        if (m == 6 && d == 1)
            return true; // Quốc tế Thiếu nhi
        if (m == 10 && d == 20)
            return true; // Phụ nữ VN
        if (m == 11 && d == 20)
            return true; // Nhà giáo VN
        if (m == 12 && d == 25)
            return true; // Giáng sinh

        // Tết Âm Lịch năm 2025 (Từ 28/01 đến 03/02/2025)
        if (date.getYear() == 2025) {
            if (m == 1 && d >= 28)
                return true;
            if (m == 2 && d <= 3)
                return true;
        }
        // Tết Âm Lịch năm 2026 (Từ 16/02 đến 22/02/2026)
        if (date.getYear() == 2026) {
            if (m == 2 && d >= 16 && d <= 22)
                return true;
        }

        return false;
    }

    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, java.time.LocalDate checkin,
            java.time.LocalDate checkout) {
        BigDecimal total = BigDecimal.ZERO;
        java.time.LocalDate temp = checkin;
        while (temp.isBefore(checkout)) {
            BigDecimal dailyPrice = basePrice;
            if (isHolidayOrWeekend(temp)) {
                dailyPrice = dailyPrice.multiply(BigDecimal.valueOf(1.20));
            }
            total = total.add(dailyPrice);
            temp = temp.plusDays(1);
        }
        return total;
    }

    // Hiển thị chi tiết một booking
    @GetMapping("/booking/detail/{id}")
    public String showBookingDetail(
            @PathVariable("id") int id,
            HttpSession session,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Tìm booking
        Booking booking = bookingRepository.findById(id).orElse(null);
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (customer != null) {
            cleanExpiredBookings(customer.getId());
        }

        // Tải lại booking sau khi dọn dẹp
        booking = bookingRepository.findById(id).orElse(null);

        if (booking == null || customer == null || booking.getCustomer().getId() != customer.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        // Tự động kiểm tra trạng thái thanh toán đối với các đơn hàng còn PENDING chưa hết hạn
        if ("PENDING".equals(booking.getStatus())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);

            boolean isChild = booking.getSpecialNotes() != null
                    && booking.getSpecialNotes().startsWith("GROUP_BOOKING_parent:");
            if (!isChild) {
                boolean verified = checkPayOSPaymentStatus(booking.getId(), booking.getTotalPrice());
                if (verified) {
                    booking.setStatus("CONFIRMED");
                    booking.setUpdatedAt(java.time.LocalDateTime.now());
                    bookingRepository.save(booking);

                    if (payment != null) {
                        payment.setStatus("PAID");
                        payment.setPaidAt(java.time.LocalDateTime.now());
                        paymentRepository.save(payment);
                    }

                    // Đồng bộ xác nhận tất cả phòng con đi kèm
                    try {
                        final int parentId = booking.getId();
                        List<Booking> childBookings = bookingRepository
                                .findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                                .filter(cb -> cb.getSpecialNotes() != null
                                        && cb.getSpecialNotes().equals("GROUP_BOOKING_parent:" + parentId))
                                .collect(java.util.stream.Collectors.toList());
                        for (Booking cb : childBookings) {
                            cb.setStatus("CONFIRMED");
                            cb.setUpdatedAt(java.time.LocalDateTime.now());
                            bookingRepository.save(cb);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

        // Kiểm tra điều kiện hủy và hoàn tiền
        boolean cancelable = booking.isCancelable();
        String refundPolicy = "none";
        if (booking.getCheckInDate() != null) {
            java.time.LocalDateTime refundDeadline = booking.getCheckInDate().minusDays(3).atTime(12, 0);
            refundPolicy = java.time.LocalDateTime.now().isBefore(refundDeadline) ? "full" : "none";
        }

        boolean refundEligible = false;
        boolean refundSubmitted = false;
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            Payment p = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            refundSubmitted = refundRepository.existsByBookingId(booking.getId());
            refundEligible = (p != null && "REFUNDED".equalsIgnoreCase(p.getStatus())) && !refundSubmitted;
        }

        boolean hasFeedback = FeedbackRepository.existsByBookingId(booking.getId());
        if (hasFeedback) {
            Feedback feedback = FeedbackRepository.findByBookingId(booking.getId());
            model.addAttribute("feedback", feedback);
        }

        model.addAttribute("booking", booking);
        model.addAttribute("hotel", booking.getHotel());
        model.addAttribute("room", booking.getRoom());
        model.addAttribute("cancelable", cancelable);
        model.addAttribute("refundPolicy", refundPolicy);
        model.addAttribute("refundEligible", refundEligible);
        model.addAttribute("refundSubmitted", refundSubmitted);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("hasFeedback", hasFeedback);
        model.addAttribute("now", java.time.LocalDateTime.now());

        return "booking/detail";
    }

    // Tự động đối soát giao dịch với PayOS khi người dùng xem trang Lịch sử
    private boolean checkPayOSPaymentStatus(int bookingId, java.math.BigDecimal expectedAmount) {
        try {
            if (payosClientId == null || payosClientId.isBlank() || payosApiKey == null || payosApiKey.isBlank()) {
                return false;
            }

            String orderCodeStr = String.valueOf(bookingId);
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
            if (payment != null && payment.getTransactionId() != null && !payment.getTransactionId().isBlank()) {
                orderCodeStr = payment.getTransactionId();
            }

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api-merchant.payos.vn/v2/payment-requests/" + orderCodeStr))
                    .header("x-client-id", payosClientId)
                    .header("x-api-key", payosApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Kiểm tra trạng thái PAID và đối chiếu số tiền để tránh trùng ID của đơn hàng
            // test cũ trên sandbox
            if (body != null && body.contains("\"status\":\"PAID\"")) {
                long expectedVal = expectedAmount.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
                if (body.contains("\"amount\":" + expectedVal) || body.contains("\"amount\": " + expectedVal)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[PayOS] Loi kiem tra trang thai tu dong: " + e.getMessage());
        }
        return false;
    }

    // Helper dọn dẹp các booking hết hạn thanh toán
    private void cleanExpiredBookings(int customerId) {
        if (customerId <= 0) return;

        // 1. Quét các booking PENDING của customer này
        List<Booking> pendingBookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (Booking b : pendingBookings) {
            // Bỏ qua child booking để xử lý theo parent booking
            boolean isChild = b.getSpecialNotes() != null && b.getSpecialNotes().startsWith("GROUP_BOOKING_parent:");
            if (isChild) {
                continue;
            }

            Payment payment = paymentRepository.findByBookingId(b.getId()).orElse(null);
            if (payment != null && payment.getQrExpiresAt() != null && payment.getQrExpiresAt().isBefore(now)) {
                // Xóa booking hết hạn
                final int parentId = b.getId();
                List<Booking> childBookings = pendingBookings.stream()
                        .filter(cb -> cb.getSpecialNotes() != null
                                && cb.getSpecialNotes().equals("GROUP_BOOKING_parent:" + parentId))
                        .collect(java.util.stream.Collectors.toList());

                // Gỡ bỏ liên kết hai chiều trước khi xóa để tránh lỗi TransientPropertyValueException của Hibernate
                b.setPayment(null);
                payment.setBooking(null);
                
                // Xóa payment trước
                paymentRepository.delete(payment);
                paymentRepository.flush();

                // Xóa child bookings
                for (Booking cb : childBookings) {
                    bookingRepository.delete(cb);
                }

                // Xóa parent booking
                bookingRepository.delete(b);
                bookingRepository.flush();
            }
        }

        // 2. Đồng thời xóa các booking EXPIRED cũ (nếu lỡ còn tồn tại trong DB)
        List<Booking> expiredBookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .filter(b -> "EXPIRED".equals(b.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        for (Booking b : expiredBookings) {
            boolean isChild = b.getSpecialNotes() != null && b.getSpecialNotes().startsWith("GROUP_BOOKING_parent:");
            if (isChild) {
                bookingRepository.delete(b);
                bookingRepository.flush();
                continue;
            }
            Payment payment = paymentRepository.findByBookingId(b.getId()).orElse(null);
            if (payment != null) {
                b.setPayment(null);
                payment.setBooking(null);
                paymentRepository.delete(payment);
                paymentRepository.flush();
            }
            bookingRepository.delete(b);
            bookingRepository.flush();
        }
    }

    // API dọn dẹp các booking hết hạn và lấy danh sách promotion khả dụng mới nhất cho trang booking
    @GetMapping("/booking/api/clean-and-get-promotions")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<Map<String, Object>> cleanAndGetPromotions(
            @RequestParam("hotelId") int hotelId,
            HttpSession session) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return new java.util.ArrayList<>();
        }
        
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        int customerId = (customer != null) ? customer.getId() : 0;
        
        // 1. Dọn dẹp booking hết hạn
        cleanExpiredBookings(customerId);
        
        // 2. Lấy danh sách khuyến mãi khả dụng mới nhất
        List<Promotion> rawPromotions = new java.util.ArrayList<>();
        List<Promotion> hotelPromos = promotionRepository.findActivePromotionsByHotelId(hotelId, java.time.LocalDate.now());
        if (hotelPromos != null) {
            for (Promotion promo : hotelPromos) {
                if (customerId == 0
                        || !bookingRepository.existsByCustomerIdAndIdPromotion(customerId, promo.getId())) {
                    rawPromotions.add(promo);
                }
            }
        }
        
        List<Map<String, Object>> promotionsList = new java.util.ArrayList<>();
        for (Promotion p : rawPromotions) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("description", p.getDescription());
            map.put("discountPercent", p.getDiscountPercent());
            map.put("status", p.getStatus());
            map.put("hotelId", p.getHotel() != null ? p.getHotel().getId() : 0);
            map.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
            map.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
            promotionsList.add(map);
        }
        
        return promotionsList;
    }
}
