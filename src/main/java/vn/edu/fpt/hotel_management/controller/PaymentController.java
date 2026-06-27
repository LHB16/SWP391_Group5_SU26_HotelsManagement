package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    private final String bankCode = "MB";
    private final String bankAccount = "0326781606";
    private final String accountName = "CHAU%20QUOC%20INH";

    public PaymentController(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository,
            HotelRepository hotelRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    @GetMapping("/booking/qr-payment")
    public String showQrPaymentPage(
            @RequestParam("roomId") int roomId,
            @RequestParam(value = "checkIn",  required = false) String checkIn,
            @RequestParam(value = "checkOut", required = false) String checkOut,
            HttpSession session,
            Model model
    ) {
        // 1. Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // 2. Lấy thông tin phòng và khách sạn từ database bằng JPA
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return "redirect:/hotels";
        }

        Hotel hotel = hotelRepository.findById(room.getHotelId()).orElse(null);
        if (hotel == null) {
            return "redirect:/hotels";
        }

        // 3. Gán ngày mặc định nếu không truyền (để test nhanh)
        if (checkIn == null || checkIn.isBlank())  checkIn  = java.time.LocalDate.now().toString();
        if (checkOut == null || checkOut.isBlank()) checkOut = java.time.LocalDate.now().plusDays(1).toString();

        // 4. Chuyển đổi định dạng ngày tháng
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);
        
        // Tính số đêm đơn giản
        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0) {
            nights = 1;
        }

        BigDecimal totalPrice = BigDecimal.valueOf(2000); // Giá phòng cố định 2,000 VND để test chuyển khoản thực tế

        // 4. Tạo và lưu đơn đặt phòng (Booking) mới vào database
        Booking booking = new Booking();
        booking.setCustomerId(loggedInUser.getId());
        booking.setRoomId(roomId);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("PENDING"); // Đặt phòng ở trạng thái chờ
        bookingRepository.save(booking);

        // 5. Tạo và lưu bản ghi thanh toán (Payment) tương ứng vào database
        LocalDateTime qrExpiresAt = LocalDateTime.now().plusMinutes(15); // QR hết hạn sau 15 phút
        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(totalPrice);
        payment.setMethod("QR_CODE");
        payment.setStatus("PENDING"); // Thanh toán ở trạng thái chờ
        payment.setPaidAt(LocalDateTime.now());
        payment.setQrExpiresAt(qrExpiresAt); // Lưu thời điểm hết hạn vào DB
        paymentRepository.save(payment);

        // Tính số giây còn lại để Thymeleaf hiển thị (không cần JS)
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds();
        long remainingMin = remainingSeconds / 60;
        long remainingSec = remainingSeconds % 60;
        String remainingLabel = remainingMin + ":" + (remainingSec < 10 ? "0" : "") + remainingSec;

        // 6. Tạo nội dung chuyển khoản duy nhất (Ví dụ: PAYBOOKING15)
        String transferInfo = "PAYBOOKING" + booking.getId();

        // Tạo đường dẫn ảnh QR VietQR
        String qrUrl = "https://img.vietqr.io/image/" + bankCode + "-" + bankAccount + "-compact.png"
                + "?amount=" + totalPrice.longValue()
                + "&addInfo=" + transferInfo
                + "&accountName=" + accountName;

        // 7. Đẩy dữ liệu ra giao diện Thymeleaf
        model.addAttribute("user", loggedInUser);
        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("checkIn", checkInDate);
        model.addAttribute("checkOut", checkOutDate);
        model.addAttribute("nights", nights);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("bankCode", bankCode + " Bank");
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("accountName", "CHAU QUOC INH");
        model.addAttribute("transferInfo", transferInfo);
        model.addAttribute("bookingId", booking.getId());
        model.addAttribute("qrExpiresAt", qrExpiresAt);     // thời điểm hết hạn (LocalDateTime)
        model.addAttribute("remainingLabel", remainingLabel); // chuỗi "14:59" để hiển thị tĩnh

        return "booking/qr-payment";
    }

    @PostMapping("/booking/confirm")
    public String confirmPayment(
            @RequestParam("bookingId") int bookingId,
            @RequestParam("transactionCode") String transactionCode,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // 1. Lấy thông tin đặt phòng
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt phòng!");
            return "redirect:/hotels";
        }

        // 2. Lấy bản ghi thanh toán
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // 3. Kiểm tra QR có hết hạn chưa – xử lý hoàn toàn bằng Java, không cần JS
        if (payment != null && payment.isQrExpired()) {
            // QR đã hết hạn: huỷ booking và payment, chuyển hướng về trang phòng
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Mã QR đã hết hạn. Vui lòng thực hiện đặt phòng lại.");
            // Tìm hotel để redirect đúng trang phòng
            Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
            int hotelId = (room != null) ? room.getHotelId() : 0;
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        // 4. Kiểm tra mã giao dịch
        if (transactionCode == null || transactionCode.trim().isEmpty()) {
            // Lấy hotelId từ room để redirect đúng trang
            Room roomForRedirect = roomRepository.findById(booking.getRoomId()).orElse(null);
            int hotelId = (roomForRedirect != null) ? roomForRedirect.getHotelId() : 0;
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mã giao dịch từ hóa đơn!");
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        // 5. Cập nhật trạng thái – đã xác nhận bởi khách hàng, chờ admin duyệt
        if ("PENDING".equals(booking.getStatus())) {
            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);
        }
        if (payment != null) {
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Xác nhận thanh toán thành công! Booking của bạn đang chờ admin phê duyệt.");
        return "redirect:/hotels";
    }
}
