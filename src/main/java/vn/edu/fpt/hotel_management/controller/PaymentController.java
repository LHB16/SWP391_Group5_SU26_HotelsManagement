package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import vn.edu.fpt.hotel_management.service.ExchangeRateService;

// PayPal Server SDK
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.models.*;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.Environment;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
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
    private final ExchangeRateService exchangeRateService;

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

    // Cấu hình PayPal
    @Value("${payment.paypal.client-id:}")
    private String paypalClientId;

    @Value("${payment.paypal.client-secret:}")
    private String paypalClientSecret;

    @Value("${payment.paypal.mode:sandbox}")
    private String paypalMode;

    @Value("${payment.paypal.vnd-to-usd-rate:25400}")
    private double vndToUsdRate;

    @Value("${payment.paypal.return-base-url:http://localhost:8082}")
    private String returnBaseUrl;

    private final PromotionRepository promotionRepository;

    public PaymentController(BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            CustomerRepository customerRepository,
            ExchangeRateService exchangeRateService,
            PromotionRepository promotionRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.customerRepository = customerRepository;
        this.exchangeRateService = exchangeRateService;
        this.promotionRepository = promotionRepository;
    }

    // ===================== HIỂN THỊ TRANG QR THANH TOÁN =====================

    @GetMapping("/booking/qr-payment")
    public String showQrPaymentPage(
            @RequestParam(value = "roomId", required = false) Integer roomId,
            @RequestParam(value = "roomIds", required = false) java.util.List<Integer> roomIds,
            @RequestParam(value = "quantities", required = false) java.util.List<Integer> quantities,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "checkins", required = false) java.util.List<String> checkins,
            @RequestParam(value = "checkouts", required = false) java.util.List<String> checkouts,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "specialRequests", required = false) String specialRequests,
            @RequestParam(value = "promotionIds", required = false) java.util.List<Integer> promotionIds,
            HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra đã đăng nhập chưa
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null)
            return "redirect:/login";

        // Đồng bộ checkin/checkout từ danh sách checkins/checkouts nếu có
        if (checkin == null || checkin.isBlank()) {
            if (checkins != null && !checkins.isEmpty()) {
                checkin = checkins.get(0);
            }
        }
        if (checkout == null || checkout.isBlank()) {
            if (checkouts != null && !checkouts.isEmpty()) {
                checkout = checkouts.get(0);
            }
        }

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
            return "redirect:/hotels";
        }

        // Lấy danh sách các phòng hợp lệ
        java.util.List<Integer> validRoomIds = new java.util.ArrayList<>();
        java.util.List<Integer> validQuantities = new java.util.ArrayList<>();
        if (roomIds != null && !roomIds.isEmpty()) {
            for (int i = 0; i < roomIds.size(); i++) {
                Integer rId = roomIds.get(i);
                Room r = roomRepository.findById(rId).orElse(null);
                if (r != null) {
                    validRoomIds.add(rId);
                    int qtyVal = 1;
                    if (quantities != null && quantities.size() > i) {
                        qtyVal = quantities.get(i);
                    }
                    validQuantities.add(qtyVal);
                }
            }
        } else if (roomId != null) {
            Room r = roomRepository.findById(roomId).orElse(null);
            if (r != null) {
                validRoomIds.add(roomId);
                int qtyVal = 1;
                if (quantities != null && !quantities.isEmpty()) {
                    qtyVal = quantities.get(0);
                }
                validQuantities.add(qtyVal);
            }
        }

        if (validRoomIds.isEmpty()) {
            return "redirect:/hotels";
        }

        // Tải thông tin phòng chính (phòng đầu tiên) và khách sạn
        int primaryRoomId = validRoomIds.get(0);
        Room room = roomRepository.findById(primaryRoomId).orElse(null);
        Hotel hotel = hotelRepository.findById(room.getHotelId()).orElse(null);
        if (hotel == null || !hotel.isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
            if ("history".equals(from)) {
                return "redirect:/booking/history";
            }
            return "redirect:/hotels";
        }

        // Tính số đêm
        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0)
            nights = 1;

        // Lấy thông tin Customer từ User đang đăng nhập
        vn.edu.fpt.hotel_management.entity.Customer customer = customerRepository.findByUserAccount(loggedInUser)
                .orElse(null);
        if (customer == null)
            return "redirect:/hotels/" + hotel.getId() + "/rooms";
        int customerId = customer.getId();

        // Tính toán các giá trị tổng cộng cho cả nhóm phòng
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        java.util.List<BigDecimal> roomSubtotals = new java.util.ArrayList<>();

        for (int i = 0; i < validRoomIds.size(); i++) {
            Room r = roomRepository.findById(validRoomIds.get(i)).orElse(null);
            int qtyVal = validQuantities.get(i);
            BigDecimal baseSub = calculateRoomSubtotal(r.getPrice(), checkInDate, checkOutDate);
            BigDecimal rSub = baseSub.multiply(BigDecimal.valueOf(qtyVal));

            // Áp dụng khuyến mãi nếu có
            Integer promoId = promotionIds != null && promotionIds.size() > i ? promotionIds.get(i) : null;
            if (promoId != null && promoId > 0) {
                Promotion promo = promotionRepository.findById(promoId).orElse(null);
                if (promo != null && "ACTIVE".equalsIgnoreCase(promo.getStatus()) && promo.getHotel() != null
                        && promo.getHotel().getId() == r.getHotelId()) {
                    // Kiểm tra xem promotion đã được khách hàng này sử dụng chưa
                    if (!bookingRepository.existsByCustomerIdAndIdPromotion(customerId, promoId)) {
                        BigDecimal discountRate = promo.getDiscountPercent().divide(BigDecimal.valueOf(100));
                        BigDecimal discountAmount = rSub.multiply(discountRate);
                        rSub = rSub.subtract(discountAmount);
                    }
                }
            }

            totalSubtotal = totalSubtotal.add(rSub);
            roomSubtotals.add(rSub);
        }

        BigDecimal serviceFee = totalSubtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50_000)
                : BigDecimal.ZERO;
        BigDecimal totalTax = totalSubtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0,
                java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotalPrice = totalSubtotal.add(totalTax).add(serviceFee);

        Booking parentBooking;
        java.util.List<Booking> bookingsList = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> bookingQuantitiesMap = new java.util.HashMap<>();

        // Luôn tạo mới cả group booking để đảm bảo chính xác các phòng và số lượng đã
        // chọn
        parentBooking = new Booking();
        parentBooking.setCustomer(customer);
        parentBooking.setRoom(room);
        parentBooking.setHotel(hotel);
        parentBooking.setNumNights((int) nights);
        parentBooking.setCheckInDate(checkInDate);
        parentBooking.setCheckOutDate(checkOutDate);
        parentBooking.setPhone(phone);
        parentBooking.setFullName(fullName);
        parentBooking.setEmail(email);
        parentBooking.setQuantity(validQuantities.get(0)); // Lưu số lượng phòng đặt cho phòng chính
        if (specialRequests != null && !specialRequests.trim().isEmpty()) {
            parentBooking.setSpecialNotes(specialRequests.trim());
        } else {
            parentBooking.setSpecialNotes(null);
        }
        Integer parentPromoId = promotionIds != null && promotionIds.size() > 0 ? promotionIds.get(0) : null;
        if (parentPromoId != null && parentPromoId > 0) {
            Promotion promo = promotionRepository.findById(parentPromoId).orElse(null);
            if (promo != null && "ACTIVE".equalsIgnoreCase(promo.getStatus()) && promo.getHotel() != null
                    && promo.getHotel().getId() == room.getHotelId()) {
                if (!bookingRepository.existsByCustomerIdAndIdPromotion(customerId, parentPromoId)) {
                    parentBooking.setIdPromotion(parentPromoId);
                }
            }
        }

        BigDecimal rSub = roomSubtotals.get(0);
        BigDecimal rTax = rSub.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal rTotalPrice = rSub.add(rTax).add(serviceFee);

        parentBooking.setTotalPrice(rTotalPrice);
        parentBooking.setStatus("PENDING");
        parentBooking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(parentBooking);

        bookingsList.add(parentBooking);
        bookingQuantitiesMap.put(parentBooking.getId(), validQuantities.get(0));

        for (int i = 1; i < validRoomIds.size(); i++) {
            Room r = roomRepository.findById(validRoomIds.get(i)).orElse(null);
            BigDecimal childSub = roomSubtotals.get(i);
            BigDecimal childTax = childSub.multiply(BigDecimal.valueOf(0.1)).setScale(0,
                    java.math.RoundingMode.HALF_UP);
            BigDecimal childTotalPrice = childSub.add(childTax);

            Booking childBooking = new Booking();
            childBooking.setCustomer(customer);
            childBooking.setRoom(r);
            childBooking.setHotel(hotel);
            childBooking.setNumNights((int) nights);
            childBooking.setCheckInDate(checkInDate);
            childBooking.setCheckOutDate(checkOutDate);
            childBooking.setTotalPrice(childTotalPrice);
            childBooking.setStatus("PENDING");
            childBooking.setCreatedAt(LocalDateTime.now());
            childBooking.setSpecialNotes("GROUP_BOOKING_parent:" + parentBooking.getId());
            childBooking.setPhone(phone);
            childBooking.setFullName(fullName);
            childBooking.setEmail(email);
            childBooking.setQuantity(validQuantities.get(i)); // Lưu số lượng phòng đặt cho phòng con
            Integer childPromoId = promotionIds != null && promotionIds.size() > i ? promotionIds.get(i) : null;
            if (childPromoId != null && childPromoId > 0) {
                Promotion promo = promotionRepository.findById(childPromoId).orElse(null);
                if (promo != null && "ACTIVE".equalsIgnoreCase(promo.getStatus()) && promo.getHotel() != null
                        && promo.getHotel().getId() == r.getHotelId()) {
                    if (!bookingRepository.existsByCustomerIdAndIdPromotion(customerId, childPromoId)) {
                        childBooking.setIdPromotion(childPromoId);
                    }
                }
            }
            bookingRepository.save(childBooking);

            bookingsList.add(childBooking);
            bookingQuantitiesMap.put(childBooking.getId(), validQuantities.get(i));
        }

        int bookingId = parentBooking.getId();

        // Tạo hoặc lấy bản ghi thanh toán với trạng thái PENDING
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        LocalDateTime qrExpiresAt;
        if (payment == null) {
            qrExpiresAt = LocalDateTime.now().plusMinutes(15); // QR hết hạn sau 15 phút
            payment = new Payment();
            payment.setBooking(parentBooking);
            payment.setAmount(grandTotalPrice); // Sử dụng giá trị tổng cộng cho Payment
            payment.setMethod("QR_CODE");
            payment.setStatus("PENDING");
            payment.setPaidAt(null);
            payment.setQrExpiresAt(qrExpiresAt);
            paymentRepository.save(payment);
        } else {
            qrExpiresAt = payment.getQrExpiresAt();
        }

        // Tạo mã QR (ưu tiên PayOS, nếu không có thì dùng VietQR tĩnh)
        String transferInfo = buildTransferInfo(bookingId, hotel.getName(), room.getRoomType(), nights);
        String qrUrl = getQrUrl(bookingId, grandTotalPrice, transferInfo, payment);

        // Tính thời gian còn lại cho đồng hồ đếm ngược
        long remainingSeconds = Math.max(0, java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds());

        int totalQty = 0;
        if (validQuantities != null) {
            for (int q : validQuantities) {
                totalQty += q;
            }
        }

        // Đưa dữ liệu vào model để hiển thị trên giao diện
        pushToModel(model, loggedInUser, hotel, room, checkInDate, checkOutDate,
                nights, totalSubtotal, totalTax, serviceFee, grandTotalPrice, qrUrl,
                transferInfo, bookingId, qrExpiresAt, remainingSeconds, from, totalQty,
                fullName, phone);

        model.addAttribute("bookingsList", bookingsList);
        model.addAttribute("bookingQuantitiesMap", bookingQuantitiesMap);

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
            verified = checkPayOSPaymentStatus(bookingId, payment);
        } else if (cassoApiKey != null && !cassoApiKey.isBlank()) {
            verified = checkCassoTransaction(transferInfo, expectedAmount, payment);
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

        // Nếu click từ child booking, tự động đổi hướng sang parent booking tương ứng
        // để xử lý
        if (booking.getSpecialNotes() != null && booking.getSpecialNotes().startsWith("GROUP_BOOKING_parent:")) {
            try {
                String parentIdStr = booking.getSpecialNotes().replace("GROUP_BOOKING_parent:", "").trim();
                int parentId = Integer.parseInt(parentIdStr);
                Booking parent = bookingRepository.findById(parentId).orElse(null);
                if (parent != null) {
                    booking = parent;
                    bookingId = parentId;
                }
            } catch (Exception e) {
            }
        }

        Room room = booking.getRoom();
        Hotel hotel = room != null ? hotelRepository.findById(room.getHotelId()).orElse(null) : null;
        if (room == null || hotel == null || !hotel.isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
            if ("history".equals(from)) {
                return "redirect:/booking/history";
            }
            return "redirect:/booking/history";
        }

        // Nếu đã xác nhận thành công rồi thì chuyển sang lịch sử đặt phòng
        if ("CONFIRMED".equals(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/booking/history";
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // 1. QR hết hạn → hủy đồng bộ cả nhóm và chuyển về lịch sử (Kiểm tra hết hạn
        // TRƯỚC TIÊN)
        if (payment != null && payment.isQrExpired()) {
            booking.setStatus("EXPIRED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            // Đồng bộ hủy các booking con trong cùng group
            try {
                final int parentId = booking.getId();
                final int customerId = booking.getCustomer().getId();
                java.util.List<Booking> childBookings = bookingRepository
                        .findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                        .filter(b -> b.getSpecialNotes() != null
                                && b.getSpecialNotes().equals("GROUP_BOOKING_parent:" + parentId))
                        .collect(java.util.stream.Collectors.toList());
                for (Booking cb : childBookings) {
                    cb.setStatus("EXPIRED");
                    cb.setUpdatedAt(LocalDateTime.now());
                    bookingRepository.save(cb);
                }
            } catch (Exception e) {
            }

            payment.setStatus("EXPIRED");
            paymentRepository.save(payment);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This payment session has expired. Please make a new reservation.");
            return "redirect:/booking/history";
        }

        // 2. Tự động kiểm tra PayOS khi vào trang
        if ("PENDING".equals(booking.getStatus()) && payosApiKey != null && !payosApiKey.isBlank()) {
            if (checkPayOSPaymentStatus(bookingId, payment)) {
                confirmBooking(booking, payment);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Payment verified successfully! Your booking is confirmed.");
                return "redirect:/booking/history";
            }
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate checkOutDate = booking.getCheckOutDate();
        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0)
            nights = 1;

        final int targetCustomerId = booking.getCustomer().getId();
        final int parentIdVal = booking.getId();
        // Tìm tất cả các booking con trong cùng group
        java.util.List<Booking> childBookings = bookingRepository
                .findByCustomerIdOrderByCreatedAtDesc(targetCustomerId).stream()
                .filter(b -> b.getSpecialNotes() != null
                        && b.getSpecialNotes().equals("GROUP_BOOKING_parent:" + parentIdVal))
                .collect(java.util.stream.Collectors.toList());

        java.util.List<Booking> bookingsList = new java.util.ArrayList<>();
        bookingsList.add(booking);
        bookingsList.addAll(childBookings);

        java.util.Map<Integer, Integer> bookingQuantitiesMap = new java.util.HashMap<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal serviceFee = BigDecimal.ZERO;
        BigDecimal grandTotalPrice = BigDecimal.ZERO;

        for (Booking b : bookingsList) {
            BigDecimal bTotalPrice = b.getTotalPrice();
            boolean isParent = (b.getSpecialNotes() == null
                    || !b.getSpecialNotes().startsWith("GROUP_BOOKING_parent:"));
            BigDecimal bServiceFee = (isParent && bTotalPrice.compareTo(BigDecimal.valueOf(50_000)) > 0)
                    ? BigDecimal.valueOf(50_000)
                    : BigDecimal.ZERO;
            BigDecimal bSubtotalPlusTax = bTotalPrice.subtract(bServiceFee);

            BigDecimal bSubtotal = bSubtotalPlusTax.divide(BigDecimal.valueOf(1.1), 0, java.math.RoundingMode.HALF_UP);
            BigDecimal bTax = bSubtotalPlusTax.subtract(bSubtotal);

            BigDecimal singleSubtotal = calculateRoomSubtotal(b.getRoom().getPrice(), b.getCheckInDate(),
                    b.getCheckOutDate());
            int qty = 1;
            if (singleSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                qty = bSubtotal.divide(singleSubtotal, 0, java.math.RoundingMode.HALF_UP).intValue();
            }
            if (qty <= 0)
                qty = 1;

            bookingQuantitiesMap.put(b.getId(), qty);
            totalSubtotal = totalSubtotal.add(bSubtotal);
            totalTax = totalTax.add(bTax);
            serviceFee = serviceFee.add(bServiceFee);
            grandTotalPrice = grandTotalPrice.add(bTotalPrice);
        }

        // Lấy hoặc tạo lại URL mã QR
        String transferInfo = buildTransferInfo(bookingId, hotel.getName(), room.getRoomType(), nights);
        String qrUrl = getQrUrl(bookingId, grandTotalPrice, transferInfo, payment);

        LocalDateTime qrExpiresAt = payment != null ? payment.getQrExpiresAt() : LocalDateTime.now().plusMinutes(15);
        long remainingSeconds = Math.max(0, java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds());

        int totalQty = 0;
        if (bookingQuantitiesMap != null) {
            for (int q : bookingQuantitiesMap.values()) {
                totalQty += q;
            }
        }

        pushToModel(model, loggedInUser, hotel, room, checkInDate, checkOutDate,
                nights, totalSubtotal, totalTax, serviceFee, grandTotalPrice, qrUrl,
                transferInfo, bookingId, qrExpiresAt, remainingSeconds, from, totalQty,
                booking.getFullName(), booking.getPhone());

        model.addAttribute("bookingsList", bookingsList);
        model.addAttribute("bookingQuantitiesMap", bookingQuantitiesMap);

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
        if (payosApiKey != null && !payosApiKey.isBlank() && checkPayOSPaymentStatus(bookingId, payment)) {
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

        // Cập nhật tất cả các booking con trong cùng group
        try {
            java.util.List<Booking> childBookings = bookingRepository
                    .findByCustomerIdOrderByCreatedAtDesc(booking.getCustomer().getId()).stream()
                    .filter(b -> b.getSpecialNotes() != null
                            && b.getSpecialNotes().equals("GROUP_BOOKING_parent:" + booking.getId()))
                    .collect(java.util.stream.Collectors.toList());
            for (Booking cb : childBookings) {
                cb.setStatus("CONFIRMED");
                cb.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(cb);
            }
        } catch (Exception e) {
            System.err.println("[Group Booking] Error confirming child bookings: " + e.getMessage());
        }

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
            String[] res = createPayOSPaymentRequest(bookingId, totalPrice.longValue(), transferInfo, payment);
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
            int bookingId, LocalDateTime qrExpiresAt, long remainingSeconds, String from, int qty,
            String fullName, String phone) {
        model.addAttribute("user", user);
        model.addAttribute("hotel", hotel);
        model.addAttribute("bookingFullName", fullName);
        model.addAttribute("bookingPhone", phone);
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
        model.addAttribute("qty", qty);

        // Tính toán quy đổi USD cho PayPal
        double rate = exchangeRateService.getRate();
        BigDecimal exchangeRate = BigDecimal.valueOf(rate <= 0 ? 25400 : rate);
        BigDecimal usdPrice = totalPrice.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP);
        model.addAttribute("vndToUsdRate", rate);
        model.addAttribute("usdPrice", usdPrice);
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
    private boolean checkCassoTransaction(String transferInfo, BigDecimal expectedAmount, Payment payment) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-casso.vn/v2/transactions?sort=DESC&limit=20"))
                    .header("Authorization", "Apikey " + cassoApiKey)
                    .GET().build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            // Tìm nội dung chuyển khoản trong danh sách giao dịch
            if (body != null && body.contains(transferInfo)) {
                int idx = body.indexOf(transferInfo);
                String window = body.substring(Math.max(0, idx - 500), Math.min(body.length(), idx + 500));
                long expected = expectedAmount.longValue();
                boolean isMatch = false;
                if (window.contains(String.valueOf(expected))) {
                    isMatch = true;
                } else {
                    // Kiểm tra thêm nếu số tiền >= mong đợi
                    Matcher matcher = Pattern.compile("\"amount\"\\s*:\\s*(\\d+)").matcher(window);
                    while (matcher.find()) {
                        if (Long.parseLong(matcher.group(1)) >= expected) {
                            isMatch = true;
                            break;
                        }
                    }
                }

                if (isMatch) {
                    if (payment != null) {
                        String accountNumber = extractJsonField(window, "corresponsiveAccount");
                        String bankName = extractJsonField(window, "corresponsiveBankName");
                        String accountName = extractJsonField(window, "corresponsiveName");
                        String bankId = extractJsonField(window, "corresponsiveBankId");

                        if (bankName.isEmpty() && !bankId.isEmpty()) {
                            bankName = getBankNameByBin(bankId);
                        }

                        if (accountName.isEmpty() && !accountNumber.isEmpty()) {
                            String description = extractJsonField(window, "description");
                            accountName = extractSenderNameFromDescription(description, accountNumber);
                        }

                        if (!accountNumber.isEmpty())
                            payment.setSenderAccountNumber(accountNumber);
                        if (!bankName.isEmpty())
                            payment.setSenderBankName(bankName);
                        if (!accountName.isEmpty())
                            payment.setSenderAccountName(accountName);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[Casso] Lỗi gọi API: " + e.getMessage());
        }
        return false;
    }

    // Kiểm tra trạng thái thanh toán qua PayOS
    private boolean checkPayOSPaymentStatus(int bookingId, Payment payment) {
        try {
            if (payosClientId == null || payosClientId.isBlank())
                return false;

            String orderCodeStr = String.valueOf(bookingId);
            if (payment != null && payment.getTransactionId() != null && !payment.getTransactionId().isBlank()) {
                orderCodeStr = payment.getTransactionId();
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-merchant.payos.vn/v2/payment-requests/" + orderCodeStr))
                    .header("x-client-id", payosClientId)
                    .header("x-api-key", payosApiKey)
                    .GET().build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            boolean isPaid = body != null
                    && java.util.regex.Pattern.compile("\"status\"\\s*:\\s*\"PAID\"").matcher(body).find();
            if (isPaid) {
                if (payment != null) {
                    String accountNumber = extractJsonField(body, "counterAccountNumber");
                    String bankName = extractJsonField(body, "counterAccountBankName");
                    String accountName = extractJsonField(body, "counterAccountName");
                    String bankId = extractJsonField(body, "counterAccountBankId");

                    if (bankName.isEmpty() && !bankId.isEmpty()) {
                        bankName = getBankNameByBin(bankId);
                    }

                    if (accountName.isEmpty() && !accountNumber.isEmpty()) {
                        // Trích xuất mô tả của giao dịch cụ thể
                        String txDesc = "";
                        int txIdx = body.indexOf("counterAccountNumber");
                        if (txIdx != -1) {
                            String txWindow = body.substring(Math.max(0, txIdx - 200),
                                    Math.min(body.length(), txIdx + 500));
                            txDesc = extractJsonField(txWindow, "description");
                        }
                        accountName = extractSenderNameFromDescription(txDesc, accountNumber);
                    }

                    if (!accountNumber.isEmpty())
                        payment.setSenderAccountNumber(accountNumber);
                    if (!bankName.isEmpty())
                        payment.setSenderBankName(bankName);
                    if (!accountName.isEmpty())
                        payment.setSenderAccountName(accountName);
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("[PayOS] Lỗi kiểm tra trạng thái: " + e.getMessage());
        }
        return false;
    }

    // Tạo link thanh toán PayOS và lấy URL mã QR
    private String[] createPayOSPaymentRequest(int bookingId, long amount, String description, Payment payment) {
        try {
            // Sinh mã orderCode độc nhất sử dụng Unix timestamp ở đơn vị giây (khớp kiểu số
            // 32-bit của PayOS)
            long orderCode = System.currentTimeMillis() / 1000L;
            if (payment != null) {
                payment.setTransactionId(String.valueOf(orderCode));
                paymentRepository.save(payment);
            }

            // Làm sạch mô tả (PayOS chỉ chấp nhận tối đa 25 ký tự không dấu)
            description = description.replaceAll("[^a-zA-Z0-9 ]", "").trim();
            if (description.length() > 25)
                description = description.substring(0, 25);

            String cancelUrl = "http://localhost:8082/booking/qr-payment-status?bookingId=" + bookingId;
            String returnUrl = cancelUrl;

            // Tạo chữ ký bảo mật theo chuẩn PayOS (sắp xếp A-Z)
            String signatureData = "amount=" + amount + "&cancelUrl=" + cancelUrl
                    + "&description=" + description + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
            String signature = computeHmacSha256(signatureData, payosChecksumKey);

            // Tạo JSON gửi lên PayOS
            String jsonBody = "{\"orderCode\":" + orderCode + ",\"amount\":" + amount
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

    // Tra cứu tên ngân hàng theo mã BIN VietQR/Napas
    private String getBankNameByBin(String bin) {
        if (bin == null || bin.isBlank())
            return "";
        return switch (bin) {
            case "970436" -> "Vietcombank";
            case "970415" -> "VietinBank";
            case "970418" -> "BIDV";
            case "970422" -> "MB Bank";
            case "970407" -> "Techcombank";
            case "970416" -> "ACB";
            case "970432" -> "VPBank";
            case "970403" -> "Sacombank";
            case "970423" -> "TPBank";
            case "970441" -> "VIB";
            case "970443" -> "SHB";
            case "970425" -> "An Binh Bank";
            case "970405" -> "Agribank";
            case "970448" -> "OCB";
            case "970437" -> "HDBank";
            case "970428" -> "Nam A Bank";
            case "970452" -> "KienlongBank";
            case "970429" -> "Saigonbank";
            case "970409" -> "Bac A Bank";
            case "970454" -> "VietCapitalBank";
            case "970440" -> "SeABank";
            case "970438" -> "BaoVietBank";
            case "970412" -> "PVcomBank";
            case "970421" -> "VRB";
            case "970433" -> "VietBank";
            case "970431" -> "Eximbank";
            case "970426" -> "MSB";
            case "970430" -> "PG Bank";
            case "970457" -> "Woori Bank";
            case "970458" -> "Shinhan Bank";
            default -> "Bank (BIN: " + bin + ")";
        };
    }

    // Trích xuất tên người gửi từ nội dung chuyển khoản bằng Regex
    private String extractSenderNameFromDescription(String description, String accountNumber) {
        if (description == null || accountNumber == null || accountNumber.isBlank())
            return "";
        try {
            Matcher m = Pattern.compile(accountNumber + "\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})*)").matcher(description);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            // Bỏ qua lỗi regex
        }
        return "";
    }

    // Endpoint dành cho môi trường phát triển để bỏ qua/xác nhận thanh toán thủ
    // công
    @GetMapping("/booking/payment-bypass")
    public String bypassPayment(@RequestParam("bookingId") int bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null) {
            Room room = booking.getRoom();
            if (room != null) {
                Hotel hotel = hotelRepository.findById(room.getHotelId()).orElse(null);
                if (hotel == null || !hotel.isActive()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
                    return "redirect:/hotels";
                }
            }
        }

        if (booking != null && "PENDING".equals(booking.getStatus())) {
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

            // Cập nhật thông tin giao dịch giả lập thành công
            if (payment != null) {
                payment.setStatus("PAID");
                payment.setPaidAt(LocalDateTime.now());
                payment.setSenderAccountName(
                        loggedInUser.getFullName() != null ? loggedInUser.getFullName() : loggedInUser.getUsername());
                payment.setSenderAccountNumber("TEST-ACC-12345");
                payment.setSenderBankName("Demo Bank");
                paymentRepository.save(payment);
            }

            confirmBooking(booking, payment);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment confirmed via bypass! Your booking is now confirmed.");
        }
        return "redirect:/booking/history";
    }

    // ===================== PAYPAL PAYMENTS (NO JS REDIRECT FLOW)
    // =====================

    @PostMapping("/booking/paypal/pay")
    public String payWithPaypal(@RequestParam("bookingId") int bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/hotels";
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment session not found.");
            return "redirect:/hotels";
        }

        if ("PAID".equals(payment.getStatus())) {
            redirectAttributes.addFlashAttribute("successMessage", "Booking is already paid.");
            return "redirect:/booking/history";
        }

        // Đổi VND sang USD (Tỷ giá tự động từ API hoặc mặc định 25400)
        BigDecimal vndAmount = payment.getAmount();
        double rate = exchangeRateService.getRate();
        BigDecimal exchangeRate = BigDecimal.valueOf(rate <= 0 ? 25400 : rate);
        BigDecimal usdAmount = vndAmount.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP);

        // Đặt phương thức là PAYPAL
        payment.setMethod("PAYPAL");
        paymentRepository.save(payment);

        // Gọi PayPal tạo order
        String[] orderResult = createPaypalOrder(bookingId, usdAmount);
        if (orderResult != null) {
            String paypalOrderId = orderResult[0];
            String approveUrl = orderResult[1];

            // Lưu Paypal Order ID
            payment.setTransactionId(paypalOrderId);
            paymentRepository.save(payment);

            return "redirect:" + approveUrl;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to initiate PayPal payment. Please try again.");
            return "redirect:/booking/qr-payment-status?bookingId=" + bookingId;
        }
    }

    @GetMapping("/booking/paypal/success")
    public String paypalSuccess(@RequestParam("token") String orderId,
            @RequestParam("bookingId") int bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/booking/history";
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment details not found.");
            return "redirect:/booking/history";
        }

        // Thực thu tiền qua PayPal
        boolean isSuccess = capturePaypalOrder(orderId, payment, booking);
        if (isSuccess) {
            confirmBooking(booking, payment);
            redirectAttributes.addFlashAttribute("successMessage", "Payment verified via PayPal successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "PayPal payment verification failed. Please contact support.");
        }
        return "redirect:/booking/history";
    }

    @GetMapping("/booking/paypal/cancel")
    public String paypalCancel(@RequestParam("bookingId") int bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("errorMessage", "PayPal payment was cancelled.");
        return "redirect:/booking/qr-payment-status?bookingId=" + bookingId;
    }

    private PaypalServerSdkClient buildPaypalClient() {
        System.out.println("[PayPal SDK] Building client...");
        System.out.println("[PayPal SDK] Mode: " + paypalMode);
        System.out.println(
                "[PayPal SDK] Client ID length: " + (paypalClientId != null ? paypalClientId.trim().length() : "null"));
        System.out.println("[PayPal SDK] Secret length: "
                + (paypalClientSecret != null ? paypalClientSecret.trim().length() : "null"));
        Environment env = "sandbox".equalsIgnoreCase(paypalMode)
                ? Environment.SANDBOX
                : Environment.PRODUCTION;
        return new PaypalServerSdkClient.Builder()
                .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(
                        paypalClientId.trim(),
                        paypalClientSecret.trim())
                        .build())
                .environment(env)
                .build();
    }

    private String[] createPaypalOrder(int bookingId, BigDecimal usdAmount) {
        try {
            PaypalServerSdkClient client = buildPaypalClient();
            OrdersController ordersController = client.getOrdersController();

            String cancelUrl = returnBaseUrl + "/booking/paypal/cancel?bookingId=" + bookingId;
            String returnUrl = returnBaseUrl + "/booking/paypal/success?bookingId=" + bookingId;

            // Xây dựng request
            CreateOrderInput input = new CreateOrderInput.Builder(
                    null,
                    new OrderRequest.Builder(
                            CheckoutPaymentIntent.CAPTURE,
                            List.of(new PurchaseUnitRequest.Builder(
                                    new AmountWithBreakdown.Builder(
                                            "USD",
                                            String.format(Locale.US, "%.2f", usdAmount.doubleValue()))
                                            .build())
                                    .referenceId(String.valueOf(bookingId))
                                    .build()))
                            .paymentSource(new PaymentSource.Builder()
                                    .paypal(new PaypalWallet.Builder()
                                            .experienceContext(new PaypalWalletExperienceContext.Builder()
                                                    .returnUrl(returnUrl)
                                                    .cancelUrl(cancelUrl)
                                                    .userAction(PaypalExperienceUserAction.PAY_NOW)
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            ApiResponse<Order> response = ordersController.createOrderAsync(input).get();
            Order order = response.getResult();

            if (order != null && order.getId() != null) {
                String orderId = order.getId();
                String approveUrl = "";
                if (order.getLinks() != null) {
                    for (LinkDescription link : order.getLinks()) {
                        if ("approve".equals(link.getRel()) || "payer-action".equals(link.getRel())) {
                            approveUrl = link.getHref();
                            break;
                        }
                    }
                }
                if (!approveUrl.isEmpty()) {
                    return new String[] { orderId, approveUrl };
                }
            }
        } catch (Throwable e) {
            System.err.println("[PayPal] Create order EXCEPTION TYPE: " + e.getClass().getName());
            System.err.println("[PayPal] Create order EXCEPTION MSG: " + e.getMessage());
            if (e.getCause() != null)
                System.err.println("[PayPal] CAUSED BY: " + e.getCause().getMessage());
            e.printStackTrace();
            // Ghi ra file để debug
            try {
                java.io.FileWriter fw = new java.io.FileWriter("paypal_error.txt", false);
                fw.write("EXCEPTION: " + e.getClass().getName() + "\n");
                fw.write("MSG: " + e.getMessage() + "\n");
                if (e.getCause() != null)
                    fw.write("CAUSE: " + e.getCause().getMessage() + "\n");
                Throwable root = e;
                while (root.getCause() != null)
                    root = root.getCause();
                fw.write("ROOT: " + root.getClass().getName() + ": " + root.getMessage() + "\n");
                java.io.PrintWriter pw = new java.io.PrintWriter(fw);
                e.printStackTrace(pw);
                pw.close();
                fw.close();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean capturePaypalOrder(String orderId, Payment payment, Booking booking) {
        try {
            PaypalServerSdkClient client = buildPaypalClient();
            OrdersController ordersController = client.getOrdersController();

            CaptureOrderInput input = new CaptureOrderInput.Builder(orderId, null).build();
            ApiResponse<Order> response = ordersController.captureOrderAsync(input).get();
            Order order = response.getResult();

            if (order != null && "COMPLETED".equals(order.getStatus() != null ? order.getStatus().toString() : "")) {
                String captureId = orderId;
                String payerEmail = "";
                String payerName = "";

                if (order.getPurchaseUnits() != null && !order.getPurchaseUnits().isEmpty()) {
                    PurchaseUnit pu = order.getPurchaseUnits().get(0);
                    if (pu.getPayments() != null && pu.getPayments().getCaptures() != null
                            && !pu.getPayments().getCaptures().isEmpty()) {
                        OrdersCapture capture = pu.getPayments().getCaptures().get(0);
                        if (capture.getId() != null)
                            captureId = capture.getId();
                    }
                }
                if (order.getPayer() != null) {
                    if (order.getPayer().getEmailAddress() != null)
                        payerEmail = order.getPayer().getEmailAddress();
                    if (order.getPayer().getName() != null) {
                        String given = order.getPayer().getName().getGivenName() != null
                                ? order.getPayer().getName().getGivenName()
                                : "";
                        String sur = order.getPayer().getName().getSurname() != null
                                ? order.getPayer().getName().getSurname()
                                : "";
                        payerName = (given + " " + sur).trim();
                    }
                }

                payment.setTransactionId(captureId);
                payment.setSenderBankName("PayPal");
                payment.setSenderAccountNumber(payerEmail.isEmpty() ? "PayPal Account" : payerEmail);
                payment.setSenderAccountName(payerName.isEmpty()
                        ? (booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A")
                        : payerName);
                return true;
            } else {
                System.err.println("[PayPal] Capture order status: " + (order != null ? order.getStatus() : "null"));
            }
        } catch (Exception e) {
            System.err.println("[PayPal] Capture exception: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
