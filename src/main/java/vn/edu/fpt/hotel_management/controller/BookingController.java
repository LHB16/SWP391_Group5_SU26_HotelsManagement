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

    // Hiển thị trang lịch sử đặt phòng của người dùng đang đăng nhập
    @GetMapping("/booking/history")
    public String showBookingHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        // Kiểm tra đăng nhập, chưa đăng nhập thì redirect về trang login
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Lấy danh sách booking của user, sắp xếp mới nhất trước, phân trang 6 booking/trang
        int pageSize = 6;
        Page<Booking> bookingPage = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(
                loggedInUser.getId(),
                PageRequest.of(page, pageSize)
        );

        // Tạo map hotelId -> tên khách sạn để hiển thị trên card
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = new HashMap<>();
        for (Hotel hotel : hotels) {
            hotelMap.put(hotel.getId(), hotel.getName());
        }

        // Tạo map bookingId -> có thể yêu cầu hoàn tiền không
        // Eligible: booking CANCELLED + đã thanh toán + thời điểm hủy (updatedAt) trước deadline + chưa gửi refund
        Map<Integer, Boolean> refundEligibleMap = new HashMap<>();
        Map<Integer, Boolean> refundSubmittedMap = new HashMap<>();
        Map<Integer, Boolean> cancelableMap = new HashMap<>();
        Map<Integer, String> refundPolicyMap = new HashMap<>();
        
        java.time.LocalDateTime nowTime = java.time.LocalDateTime.now();
        
        for (Booking b : bookingPage.getContent()) {
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
                boolean hadPaid = (p != null &&
                        !"PENDING".equalsIgnoreCase(p.getStatus()) &&
                        !"FAILED".equalsIgnoreCase(p.getStatus()) &&
                        !"NO_REFUND".equalsIgnoreCase(p.getStatus()));
                // Kiểm tra thời điểm hủy so với deadline (dùng updatedAt — lúc booking bị cancel)
                boolean withinDeadline = false;
                if (b.getCheckInDate() != null && b.getUpdatedAt() != null) {
                    java.time.LocalDateTime refundDeadline = b.getCheckInDate().minusDays(3).atTime(12, 0);
                    withinDeadline = b.getUpdatedAt().isBefore(refundDeadline);
                }
                boolean eligible = hadPaid && withinDeadline && !alreadySubmitted;
                refundEligibleMap.put(b.getId(), eligible);
                refundSubmittedMap.put(b.getId(), alreadySubmitted);
            }
        }

        // Truyền dữ liệu vào model để render template
        model.addAttribute("user", loggedInUser);
        model.addAttribute("bookings", bookingPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingPage.getTotalPages());
        model.addAttribute("totalItems", bookingPage.getTotalElements());
        model.addAttribute("hotelMap", hotelMap);
        model.addAttribute("refundEligibleMap", refundEligibleMap);
        model.addAttribute("refundSubmittedMap", refundSubmittedMap);
        model.addAttribute("cancelableMap", cancelableMap);
        model.addAttribute("refundPolicyMap", refundPolicyMap);
        model.addAttribute("refundEligibleMap", refundEligibleMap);
        model.addAttribute("refundSubmittedMap", refundSubmittedMap);

        return "booking/history";
    }

    // Hiển thị trang chính sách hủy & điều khoản hoàn tiền
    @GetMapping("/booking/cancel-policy")
    public String showCancelPolicyPage(
            @RequestParam("bookingId") int bookingId,
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
        if (booking == null || booking.getCustomer().getId() != loggedInUser.getId()) {
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
            isFullRefundEligible = booking.getUpdatedAt() != null && booking.getUpdatedAt().isBefore(refundDeadline);
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

        return "booking/cancel-policy";
    }

    // Hủy booking: cho phép hủy khi CONFIRMED và chưa đến ngày check-in
    // Chính sách hoàn tiền: trước 12:00 trưa ngày (checkin - 3 ngày) → 100% | sau deadline → 0%
    @PostMapping("/booking/cancel/{id}")
    public String cancelBooking(
            @PathVariable("id") int bookingId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Tìm booking theo id
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getCustomer().getId() != loggedInUser.getId()) {
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

        // Hủy booking
        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(java.time.LocalDateTime.now());
        bookingRepository.save(booking);

        // Cập nhật payment
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null) {
            // Đánh dấu REFUNDED nếu còn trong thời hạn, NO_REFUND nếu quá hạn
            payment.setStatus(isFullRefundEligible ? "REFUNDED" : "NO_REFUND");
            paymentRepository.save(payment);
        }

        if (isFullRefundEligible) {
            // Chuyển sang trang yêu cầu hoàn tiền (100%)
            return "redirect:/booking/refund-request?bookingId=" + bookingId;
        } else {
            // Quá hạn hoàn tiền → báo không được hoàn
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
        if (booking == null || booking.getCustomer().getId() != loggedInUser.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        // Chỉ cho phép booking đã CANCELLED mới được yêu cầu hoàn tiền
        if (!"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only cancelled bookings are eligible for a refund.");
            return "redirect:/booking/history";
        }

        // Kiểm tra xem đã gửi yêu cầu hoàn tiền chưa
        if (refundRepository.existsByBookingId(bookingId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "A refund request for this booking has already been submitted.");
            return "redirect:/booking/history";
        }

        // Kiểm tra xem thời điểm hủy có trước deadline hoàn tiền không (dùng updatedAt)
        if (booking.getCheckInDate() == null || booking.getUpdatedAt() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to verify refund eligibility.");
            return "redirect:/booking/history";
        }
        java.time.LocalDateTime refundDeadline = booking.getCheckInDate().minusDays(3).atTime(12, 0);
        if (!booking.getUpdatedAt().isBefore(refundDeadline)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "This booking was cancelled within 3 days of check-in and is non-refundable per our policy.");
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
        if (booking == null || booking.getCustomer().getId() != loggedInUser.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        if (!"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only cancelled bookings are eligible for a refund.");
            return "redirect:/booking/history";
        }

        if (refundRepository.existsByBookingId(bookingId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "A refund request for this booking has already been submitted.");
            return "redirect:/booking/history";
        }

        // Validate input
        if (bankName == null || bankName.isBlank() ||
            accountNumber == null || accountNumber.isBlank() ||
            accountHolder == null || accountHolder.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fill in all bank account information.");
            return "redirect:/booking/refund-request?bookingId=" + bookingId;
        }

        // Kiểm tra lại deadline bằng updatedAt (chặn truy cập trực tiếp qua URL)
        if (booking.getCheckInDate() == null || booking.getUpdatedAt() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to verify refund eligibility.");
            return "redirect:/booking/history";
        }
        java.time.LocalDateTime refundDeadline2 = booking.getCheckInDate().minusDays(3).atTime(12, 0);
        if (!booking.getUpdatedAt().isBefore(refundDeadline2)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "This booking was cancelled within 3 days of check-in and is non-refundable per our policy.");
            return "redirect:/booking/history";
        }

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
        refund.setRequestedAt(java.time.LocalDateTime.now());
        refundRepository.save(refund);

        String formattedAmount = String.format("%,.0f", refundAmount.doubleValue());
        redirectAttributes.addFlashAttribute("successMessage",
                "Refund request submitted successfully! " + formattedAmount + " ₫ will be processed within 3-5 business days.");
        return "redirect:/booking/history";
    }

    // Xóa booking khỏi lịch sử của người dùng (xóa vĩnh viễn khỏi database)
    // Xóa payment trước (nếu có) để tránh lỗi foreign key, sau đó mới xóa booking
    @PostMapping("/booking/delete/{id}")
    public String deleteBooking(
            @PathVariable("id") int bookingId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Chỉ xóa nếu booking thuộc về người dùng hiện tại
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null && booking.getCustomer().getId() == loggedInUser.getId()) {
            // Giải phóng liên kết 2 chiều để tránh lỗi TransientPropertyValueException của Hibernate
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
            if (payment != null) {
                booking.setPayment(null);
                bookingRepository.saveAndFlush(booking); // Đồng bộ trạng thái Booking
                paymentRepository.delete(payment);
                paymentRepository.flush();
            }
            // Xóa booking
            bookingRepository.delete(booking);
            redirectAttributes.addFlashAttribute("successMessage", "Booking history record removed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking record not found or access denied.");
        }

        return "redirect:/booking/history";
    }

    @GetMapping({"/booking", "/booking/create"})
    public String showCreateBookingPage(
            @RequestParam(name = "hotelId", required = false) Integer hotelId,
            @RequestParam(name = "roomId", required = false) Integer roomId,
            @RequestParam(name = "roomIds", required = false) List<Integer> roomIds,
            @RequestParam(name = "checkin", required = false) String checkin,
            @RequestParam(name = "checkout", required = false) String checkout,
            HttpSession session, 
            Model model) {
        
        // Lấy thông tin user đăng nhập từ Session (nếu có)
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

        // Lấy danh sách các phòng (chỉ lấy phòng được chọn nếu roomId hoặc roomIds có giá trị)
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
            rooms = roomRepository.findAll();
        }
        model.addAttribute("rooms", rooms);

        // Lấy danh sách khách sạn để map ID sang tên khách sạn dễ hiển thị
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = new HashMap<>();
        for (Hotel hotel : hotels) {
            hotelMap.put(hotel.getId(), hotel.getName());
        }
        model.addAttribute("hotelMap", hotelMap);
        
        // Tính toán số đêm dựa trên ngày check-in và check-out
        long nights = 1;
        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                java.time.LocalDate d1 = java.time.LocalDate.parse(checkin);
                java.time.LocalDate d2 = java.time.LocalDate.parse(checkout);
                if (d2.isAfter(d1)) {
                    nights = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
                } else {
                    d2 = d1.plusDays(1);
                    checkout = d2.toString();
                    nights = 1;
                }
            } catch (Exception e) {
                // Parse error, defaults will be handled below
            }
        } else {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);
            checkin = today.toString();
            checkout = tomorrow.toString();
            nights = 1;
        }

        // Chuẩn bị danh sách phòng được chọn
        List<Integer> selectedRoomIds = new ArrayList<>();
        if (roomIds != null) {
            selectedRoomIds.addAll(roomIds);
        }
        if (roomId != null && !selectedRoomIds.contains(roomId)) {
            selectedRoomIds.add(roomId);
        }

        // Tính toán chi phí bằng Java có tăng 20% vào ngày lễ/cuối tuần
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Integer, BigDecimal> roomPricesMap = new java.util.HashMap<>();
        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                java.time.LocalDate d1 = java.time.LocalDate.parse(checkin.trim());
                java.time.LocalDate d2 = java.time.LocalDate.parse(checkout.trim());
                
                // Tính giá thực tế (tổng tiền) cho tất cả các phòng hiển thị
                for (Room r : rooms) {
                    BigDecimal actualSubtotal = calculateRoomSubtotal(r.getPrice(), d1, d2);
                    roomPricesMap.put(r.getId(), actualSubtotal);
                }

                // Cộng dồn subtotal cho các phòng được chọn
                for (Integer rId : selectedRoomIds) {
                    Room r = roomRepository.findById(rId).orElse(null);
                    if (r != null) {
                        subtotal = subtotal.add(calculateRoomSubtotal(r.getPrice(), d1, d2));
                    }
                }
            } catch (Exception e) {
                System.err.println("BookingController subtotal date parse error: " + e.getMessage());
                for (Room r : rooms) {
                    roomPricesMap.put(r.getId(), r.getPrice().multiply(BigDecimal.valueOf(nights)));
                }
                for (Integer rId : selectedRoomIds) {
                    Room r = roomRepository.findById(rId).orElse(null);
                    if (r != null) {
                        subtotal = subtotal.add(r.getPrice().multiply(BigDecimal.valueOf(nights)));
                    }
                }
            }
        } else {
            for (Room r : rooms) {
                roomPricesMap.put(r.getId(), r.getPrice().multiply(BigDecimal.valueOf(nights)));
            }
            for (Integer rId : selectedRoomIds) {
                Room r = roomRepository.findById(rId).orElse(null);
                if (r != null) {
                    subtotal = subtotal.add(r.getPrice().multiply(BigDecimal.valueOf(nights)));
                }
            }
        }
        model.addAttribute("roomPricesMap", roomPricesMap);

        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50000) : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(tax).add(serviceFee);

        // Truyền các giá trị tính toán vào model
        model.addAttribute("today", java.time.LocalDate.now().toString());
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("nights", nights);
        model.addAttribute("selectedRoomIds", selectedRoomIds);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("serviceFee", serviceFee);
        model.addAttribute("grandTotal", grandTotal);
        
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
        if (m == 1 && d == 1) return true;   // Tết Dương Lịch
        if (m == 4 && d == 30) return true;  // Giải phóng Miền Nam
        if (m == 5 && d == 1) return true;   // Quốc tế Lao động
        if (m == 9 && d == 2) return true;   // Quốc khánh

        // Các ngày lễ đặc biệt yêu cầu thêm
        if (m == 2 && d == 14) return true;  // Valentine
        if (m == 3 && d == 8) return true;   // Quốc tế Phụ nữ
        if (m == 6 && d == 1) return true;   // Quốc tế Thiếu nhi
        if (m == 10 && d == 20) return true; // Phụ nữ VN
        if (m == 11 && d == 20) return true; // Nhà giáo VN
        if (m == 12 && d == 25) return true; // Giáng sinh

        // Tết Âm Lịch năm 2025 (Từ 28/01 đến 03/02/2025)
        if (date.getYear() == 2025) {
            if (m == 1 && d >= 28) return true;
            if (m == 2 && d <= 3) return true;
        }
        // Tết Âm Lịch năm 2026 (Từ 16/02 đến 22/02/2026)
        if (date.getYear() == 2026) {
            if (m == 2 && d >= 16 && d <= 22) return true;
        }

        return false;
    }

    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, java.time.LocalDate checkin, java.time.LocalDate checkout) {
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
}
