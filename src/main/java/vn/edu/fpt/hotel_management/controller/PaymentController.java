package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Controller
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final CustomerRepository customerRepository;

    // Thông tin tài khoản ngân hàng (đọc từ application.properties)
    @Value("${payment.bank-code:KLB}")
    private String bankCode;

    @Value("${payment.bank-account:9614072005}")
    private String bankAccount;

    @Value("${payment.account-name:CHAU QUOC INH}")
    private String accountName;

    // API key của Casso để xác minh giao dịch thật
    @Value("${payment.casso.api-key:}")
    private String cassoApiKey;

    // Thông tin kết nối PayOS
    @Value("${payment.payos.client-id:}")
    private String payosClientId;

    @Value("${payment.payos.api-key:}")
    private String payosApiKey;

    @Value("${payment.payos.checksum-key:}")
    private String payosChecksumKey;

    public PaymentController(BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            CustomerRepository customerRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.customerRepository = customerRepository;
    }

    // ===================== HIỂN THỊ TRANG QR THANH TOÁN =====================

    @GetMapping("/booking/qr-payment")
    public String showQrPaymentPage(
            @RequestParam(value = "roomId", required = false) Integer roomId,
            @RequestParam(value = "roomIds", required = false) java.util.List<Integer> roomIds,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "from", required = false) String from,
            HttpSession session, Model model) {

        // Kiểm tra đã đăng nhập chưa
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null)
            return "redirect:/login";

        // Lấy roomId từ danh sách nếu không có roomId đơn
        if (roomId == null) {
            if (roomIds != null && !roomIds.isEmpty())
                roomId = roomIds.get(0);
            else
                return "redirect:/hotels";
        }

        // Tải thông tin phòng và khách sạn
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null)
            return "redirect:/hotels";

        Hotel hotel = hotelRepository.findById(room.getHotelId()).orElse(null);
        if (hotel == null)
            return "redirect:/hotels";

        // Đặt ngày mặc định nếu không có
        if (checkin == null || checkin.isBlank())
            checkin = LocalDate.now().toString();
        if (checkout == null || checkout.isBlank())
            checkout = LocalDate.now().plusDays(1).toString();

        // Parse ngày check-in và check-out
        LocalDate checkInDate, checkOutDate;
        try {
            checkInDate = LocalDate.parse(checkin);
            checkOutDate = LocalDate.parse(checkout);
        } catch (Exception e) {
            return "redirect:/hotels/" + hotel.getId() + "/rooms";
        }

        // Tính số đêm
        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0)
            nights = 1;

        // Tính giá tiền (có tăng giá ngày lễ/cuối tuần)
        BigDecimal subtotal = calculateRoomSubtotal(room.getPrice(), checkInDate, checkOutDate);
        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50_000) : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPrice = subtotal.add(tax).add(serviceFee);

        // Lấy thông tin Customer từ User đang đăng nhập
        Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (customer == null)
            return "redirect:/hotels/" + hotel.getId() + "/rooms";

        // Kiểm tra nếu đã có booking PENDING còn hạn QR thì dùng lại
        java.util.Optional<Booking> existingOpt = bookingRepository
                .findByCustomerIdAndRoomIdAndCheckInDateAndCheckOutDateAndStatusOrderByCreatedAtDesc(
                        customer.getId(), roomId, checkInDate, checkOutDate, "PENDING")
                .stream()
                .filter(b -> {
                    Payment p = paymentRepository.findByBookingId(b.getId()).orElse(null);
                    return p != null && !p.isQrExpired(); // QR còn hạn
                })
                .findFirst();

        if (existingOpt.isPresent()) {
            // Chuyển sang trang QR của booking cũ
            int existingBookingId = existingOpt.get().getId();
            return "redirect:/booking/qr-payment-status?bookingId=" + existingBookingId
                    + "&hotelId=" + hotel.getId() + "&roomId=" + roomId
                    + "&checkin=" + checkInDate + "&checkout=" + checkOutDate;
        }

        // Tạo booking mới
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setHotel(hotel);
        booking.setNumNights((int) nights);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("PENDING");
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Tạo bản ghi thanh toán với trạng thái PENDING
        LocalDateTime qrExpiresAt = LocalDateTime.now().plusMinutes(15); // QR hết hạn sau 15 phút
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(totalPrice);
        payment.setMethod("QR_CODE");
        payment.setStatus("PENDING");
        payment.setPaidAt(null);
        payment.setQrExpiresAt(qrExpiresAt);
        paymentRepository.save(payment);

        // Tạo mã QR (ưu tiên PayOS, nếu không có thì dùng VietQR tĩnh)
        String transferInfo = buildTransferInfo(booking.getId(), hotel.getName(), room.getRoomType(), nights);
        String qrUrl = getQrUrl(booking.getId(), totalPrice, transferInfo, payment);

        // Tính thời gian còn lại cho đồng hồ đếm ngược
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds();

        // Đưa dữ liệu vào model để hiển thị trên giao diện
        pushToModel(model, loggedInUser, hotel, room, checkInDate, checkOutDate,
                nights, subtotal, tax, serviceFee, totalPrice, qrUrl,
                transferInfo, booking.getId(), qrExpiresAt, remainingSeconds, from);

        return "booking/qr-payment";
    }

    // ===================== XÁC NHẬN THANH TOÁN =====================

    @PostMapping("/booking/confirm-casso")
    public String confirmPayment(@RequestParam("bookingId") int bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null)
            return "redirect:/login";

        // Tải booking
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/hotels";
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // Nếu QR đã hết hạn → hủy booking
        if (payment != null && payment.isQrExpired()) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            redirectAttributes.addFlashAttribute("errorMessage", "QR code has expired. Please start a new booking.");
            int hotelId = booking.getRoom() != null ? booking.getRoom().getHotelId() : 0;
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        // Nếu đã thanh toán rồi thì không cần làm gì
        if (payment != null && "PAID".equals(payment.getStatus())) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/hotels";
        }

        // Lấy thông tin để kiểm tra giao dịch
        long nights = booking.getCheckOutDate().toEpochDay() - booking.getCheckInDate().toEpochDay();
        if (nights <= 0)
            nights = 1;
        Room room = booking.getRoom();
        Hotel hotel = room != null ? hotelRepository.findById(room.getHotelId()).orElse(null) : null;
        String transferInfo = buildTransferInfo(bookingId,
                hotel != null ? hotel.getName() : "",
                room != null ? room.getRoomType() : "", nights);
        BigDecimal expectedAmount = payment != null ? payment.getAmount() : booking.getTotalPrice();

        // Kiểm tra qua PayOS (ưu tiên) hoặc Casso
        boolean verified = false;
        if (payosApiKey != null && !payosApiKey.isBlank()) {
            verified = checkPayOSPaymentStatus(bookingId);
        } else if (cassoApiKey != null && !cassoApiKey.isBlank()) {
            verified = checkCassoTransaction(transferInfo, expectedAmount);
        }

        if (verified) {
            // Xác nhận booking và payment thành công
            confirmBooking(booking, payment);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/hotels";
        } else {
            // Chưa phát hiện giao dịch → yêu cầu thử lại
            redirectAttributes.addFlashAttribute("errorMessage",
                    "We haven't detected your payment yet. Please complete the transfer then click Verify again.");
            int hotelId = room != null ? room.getHotelId() : 0;
            return "redirect:/booking/qr-payment-status?bookingId=" + bookingId
                    + "&hotelId=" + hotelId + "&roomId=" + (room != null ? room.getId() : 0)
                    + "&checkin=" + booking.getCheckInDate() + "&checkout=" + booking.getCheckOutDate();
        }
    }

    // ===================== HIỂN THỊ LẠI TRANG QR CŨ =====================

    @GetMapping("/booking/qr-payment-status")
    public String showExistingQrPage(
            @RequestParam("bookingId") int bookingId,
            @RequestParam(value = "from", required = false) String from,
            HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null)
            return "redirect:/login";

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null)
            return "redirect:/hotels";

        // Nếu đã xác nhận thành công rồi thì chuyển sang lịch sử đặt phòng
        if ("CONFIRMED".equals(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/booking/history";
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // Tự động kiểm tra PayOS khi vào trang
        if ("PENDING".equals(booking.getStatus()) && payosApiKey != null && !payosApiKey.isBlank()) {
            if (checkPayOSPaymentStatus(bookingId)) {
                confirmBooking(booking, payment);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Payment verified successfully! Your booking is confirmed.");
                return "redirect:/booking/history";
            }
        }

        // QR hết hạn → hủy và chuyển về lịch sử
        if (payment != null && payment.isQrExpired()) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This payment session has expired. Please make a new reservation.");
            return "redirect:/booking/history";
        }

        Room room = booking.getRoom();
        Hotel hotel = room != null ? hotelRepository.findById(room.getHotelId()).orElse(null) : null;
        if (room == null || hotel == null)
            return "redirect:/hotels";

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate checkOutDate = booking.getCheckOutDate();
        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0)
            nights = 1;

        // Tính lại giá để hiển thị
        BigDecimal subtotal = calculateRoomSubtotal(room.getPrice(), checkInDate, checkOutDate);
        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50_000) : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);

        // Lấy hoặc tạo lại URL mã QR
        String transferInfo = buildTransferInfo(bookingId, hotel.getName(), room.getRoomType(), nights);
        String qrUrl = getQrUrl(bookingId, booking.getTotalPrice(), transferInfo, payment);

        LocalDateTime qrExpiresAt = payment != null ? payment.getQrExpiresAt() : LocalDateTime.now().plusMinutes(15);
        long remainingSeconds = Math.max(0, java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds());

        pushToModel(model, loggedInUser, hotel, room, checkInDate, checkOutDate,
                nights, subtotal, tax, serviceFee, booking.getTotalPrice(), qrUrl,
                transferInfo, bookingId, qrExpiresAt, remainingSeconds, from);

        return "booking/qr-payment";
    }

    // ===================== AJAX KIỂM TRA TRẠNG THÁI =====================

    @GetMapping("/booking/check-status")
    @ResponseBody
    public String checkPaymentStatus(@RequestParam("bookingId") int bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null)
            return "NOT_FOUND";
        if ("CONFIRMED".equals(booking.getStatus()))
            return "CONFIRMED";

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // Kiểm tra PayOS và cập nhật nếu đã thanh toán
        if (payosApiKey != null && !payosApiKey.isBlank() && checkPayOSPaymentStatus(bookingId)) {
            confirmBooking(booking, payment);
            return "CONFIRMED";
        }

        return booking.getStatus(); // Trả về PENDING nếu chưa thanh toán
    }

    // ===================== CÁC HÀM DÙNG CHUNG =====================

    // Xác nhận booking và payment thành công
    private void confirmBooking(Booking booking, Payment payment) {
        booking.setStatus("CONFIRMED");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        if (payment != null) {
            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
    }

    // Lấy URL mã QR: ưu tiên dùng QR đã lưu → PayOS → VietQR tĩnh
    private String getQrUrl(int bookingId, BigDecimal totalPrice, String transferInfo, Payment payment) {
        // Dùng QR đã lưu trong database
        if (payment != null && payment.getQrCodeUrl() != null && !payment.getQrCodeUrl().isBlank()) {
            String saved = payment.getQrCodeUrl();
            // Nếu là dạng text thì chuyển qua qrserver.com
            if (!saved.startsWith("http")) {
                saved = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                        + URLEncoder.encode(saved, StandardCharsets.UTF_8);
            }
            return saved;
        }

        // Tạo QR qua PayOS
        if (payosApiKey != null && !payosApiKey.isBlank()) {
            String[] res = createPayOSPaymentRequest(bookingId, totalPrice.longValue(), transferInfo);
            if (res != null) {
                if (payment != null) {
                    payment.setQrCodeUrl(res[0]);
                    paymentRepository.save(payment);
                }
                return res[0];
            }
        }

        // Fallback: tự tạo VietQR tĩnh
        return "https://img.vietqr.io/image/" + bankCode + "-" + bankAccount + "-compact.png"
                + "?amount=" + totalPrice.longValue()
                + "&addInfo=" + URLEncoder.encode(transferInfo, StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8);
    }

    // Đưa tất cả dữ liệu vào model để hiển thị trên HTML
    private void pushToModel(Model model, User user, Hotel hotel, Room room,
            LocalDate checkIn, LocalDate checkOut, long nights,
            BigDecimal subtotal, BigDecimal tax, BigDecimal serviceFee,
            BigDecimal totalPrice, String qrUrl, String transferInfo,
            int bookingId, LocalDateTime qrExpiresAt, long remainingSeconds, String from) {
        model.addAttribute("user", user);
        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("nights", nights);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("serviceFee", serviceFee);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("bankCode", bankCode + " Bank");
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("accountName", accountName);
        model.addAttribute("transferInfo", transferInfo);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("qrExpiresAt", qrExpiresAt);
        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("from", from);
    }

    // Tạo nội dung chuyển khoản ngắn gọn (tối đa 25 ký tự)
    private String buildTransferInfo(int bookingId, String hotelName, String roomType, long nights) {
        String hotel = sanitizeForTransfer(hotelName, 6);
        String room = sanitizeForTransfer(roomType, 5);
        String dayText = nights == 1 ? "1 Day" : nights + " Days";
        String info = "BK" + bookingId + " " + hotel + " " + room + " " + dayText;
        if (info.length() > 25)
            info = info.substring(0, 25);
        return info.trim();
    }

    // Xóa ký tự đặc biệt và giới hạn độ dài
    private String sanitizeForTransfer(String raw, int maxLen) {
        if (raw == null || raw.isBlank())
            return "";
        String clean = raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    // Tính tổng tiền phòng (tăng 20% vào ngày lễ/cuối tuần)
    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, LocalDate checkin, LocalDate checkout) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate d = checkin; d.isBefore(checkout); d = d.plusDays(1)) {
            BigDecimal price = isHolidayOrWeekend(d) ? basePrice.multiply(BigDecimal.valueOf(1.20)) : basePrice;
            total = total.add(price);
        }
        return total;
    }

    // Kiểm tra ngày lễ hoặc cuối tuần
    private boolean isHolidayOrWeekend(LocalDate d) {
        var dow = d.getDayOfWeek();
        if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY)
            return true;
        int m = d.getMonthValue(), day = d.getDayOfMonth(), y = d.getYear();
        if (m == 1 && day == 1)
            return true; // Tết Dương lịch
        if (m == 4 && day == 30)
            return true; // Ngày Giải phóng
        if (m == 5 && day == 1)
            return true; // Ngày Quốc tế Lao động
        if (m == 9 && day == 2)
            return true; // Ngày Quốc khánh
        if (m == 2 && day == 14)
            return true; // Valentine
        if (m == 3 && day == 8)
            return true; // Ngày Quốc tế Phụ nữ
        if (m == 6 && day == 1)
            return true; // Ngày Quốc tế Thiếu nhi
        if (m == 10 && day == 20)
            return true; // Ngày Phụ nữ Việt Nam
        if (m == 11 && day == 20)
            return true; // Ngày Nhà giáo Việt Nam
        if (m == 12 && day == 25)
            return true; // Giáng sinh
        if (y == 2025 && m == 1 && day >= 28)
            return true; // Tết Âm lịch 2025
        if (y == 2025 && m == 2 && day <= 3)
            return true;
        if (y == 2026 && m == 2 && day >= 16 && day <= 22)
            return true; // Tết Âm lịch 2026
        return false;
    }

    // Kiểm tra giao dịch qua API Casso
    private boolean checkCassoTransaction(String transferInfo, BigDecimal expectedAmount) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.casso.vn/v2/transactions?sort=DESC&limit=20"))
                    .header("Authorization", "Apikey " + cassoApiKey)
                    .GET().build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            // Tìm nội dung chuyển khoản trong danh sách giao dịch
            if (body != null && body.contains(transferInfo)) {
                int idx = body.indexOf(transferInfo);
                String window = body.substring(Math.max(0, idx - 500), Math.min(body.length(), idx + 500));
                long expected = expectedAmount.longValue();
                if (window.contains(String.valueOf(expected)))
                    return true;

                // Kiểm tra thêm nếu số tiền >= mong đợi
                Matcher matcher = Pattern.compile("\"amount\"\\s*:\\s*(\\d+)").matcher(window);
                while (matcher.find()) {
                    if (Long.parseLong(matcher.group(1)) >= expected)
                        return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[Casso] Lỗi gọi API: " + e.getMessage());
        }
        return false;
    }

    // Kiểm tra trạng thái thanh toán qua PayOS
    private boolean checkPayOSPaymentStatus(int bookingId) {
        try {
            if (payosClientId == null || payosClientId.isBlank())
                return false;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-merchant.payos.vn/v2/payment-requests/" + bookingId))
                    .header("x-client-id", payosClientId)
                    .header("x-api-key", payosApiKey)
                    .GET().build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return body != null && java.util.regex.Pattern.compile("\"status\"\\s*:\\s*\"PAID\"").matcher(body).find(); // Kiểm tra đã thanh toán chưa
        } catch (Exception e) {
            System.err.println("[PayOS] Lỗi kiểm tra trạng thái: " + e.getMessage());
        }
        return false;
    }

    // Tạo link thanh toán PayOS và lấy URL mã QR
    private String[] createPayOSPaymentRequest(int bookingId, long amount, String description) {
        try {
            // Làm sạch mô tả (PayOS chỉ chấp nhận tối đa 25 ký tự không dấu)
            description = description.replaceAll("[^a-zA-Z0-9 ]", "").trim();
            if (description.length() > 25)
                description = description.substring(0, 25);

            String cancelUrl = "http://localhost:8082/booking/qr-payment-status?bookingId=" + bookingId;
            String returnUrl = cancelUrl;

            // Tạo chữ ký bảo mật theo chuẩn PayOS (sắp xếp A-Z)
            String signatureData = "amount=" + amount + "&cancelUrl=" + cancelUrl
                    + "&description=" + description + "&orderCode=" + bookingId + "&returnUrl=" + returnUrl;
            String signature = computeHmacSha256(signatureData, payosChecksumKey);

            // Tạo JSON gửi lên PayOS
            String jsonBody = "{\"orderCode\":" + bookingId + ",\"amount\":" + amount
                    + ",\"description\":\"" + description + "\",\"cancelUrl\":\"" + cancelUrl
                    + "\",\"returnUrl\":\"" + returnUrl + "\",\"signature\":\"" + signature + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-merchant.payos.vn/v2/payment-requests"))
                    .header("x-client-id", payosClientId)
                    .header("x-api-key", payosApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            // Nếu tạo thành công thì dựng URL VietQR
            if (body != null && body.contains("\"code\":\"00\"")) {
                String bin = extractJsonField(body, "bin");
                String accNumber = extractJsonField(body, "accountNumber");
                String accName = extractJsonField(body, "accountName");
                String amt = extractJsonField(body, "amount");
                String desc = extractJsonField(body, "description");
                String qrUrl = "https://img.vietqr.io/image/" + bin + "-" + accNumber + "-compact.png"
                        + "?amount=" + amt
                        + "&addInfo=" + URLEncoder.encode(desc, StandardCharsets.UTF_8)
                        + "&accountName=" + URLEncoder.encode(accName, StandardCharsets.UTF_8);
                return new String[] { qrUrl, extractJsonField(body, "checkoutUrl") };
            }
        } catch (Exception e) {
            System.err.println("[PayOS] Lỗi tạo link thanh toán: " + e.getMessage());
        }
        return null;
    }

    // Tính chữ ký HMAC-SHA256 cho PayOS
    private String computeHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // Tách giá trị từ chuỗi JSON bằng Regex
    private String extractJsonField(String json, String fieldName) {
        if (json == null || fieldName == null)
            return "";
        // Tìm giá trị dạng chuỗi
        Matcher m = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (m.find())
            return m.group(1);
        // Tìm giá trị dạng số
        m = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*([0-9.-]+)").matcher(json);
        if (m.find())
            return m.group(1);
        return "";
    }
}