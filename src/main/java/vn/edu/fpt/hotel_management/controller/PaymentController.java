package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import vn.edu.fpt.hotel_management.service.ExchangeRateService;
import vn.edu.fpt.hotel_management.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final CustomerRepository customerRepository;
    private final ExchangeRateService exchangeRateService;
    private final PromotionRepository promotionRepository;
    private final PaymentService paymentService;

    public PaymentController(BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            CustomerRepository customerRepository,
            ExchangeRateService exchangeRateService,
            PromotionRepository promotionRepository,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.customerRepository = customerRepository;
        this.exchangeRateService = exchangeRateService;
        this.promotionRepository = promotionRepository;
        this.paymentService = paymentService;
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
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        java.util.List<BigDecimal> roomSubtotals = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> detailedDiscounts = new java.util.ArrayList<>();

        for (int i = 0; i < validRoomIds.size(); i++) {
            Room r = roomRepository.findById(validRoomIds.get(i)).orElse(null);
            int qtyVal = validQuantities.get(i);

            // Verify room availability before creating bookings
            long bookedCount = bookingRepository.sumQuantityForConfirmedAndPending(r.getId(), checkOutDate, checkInDate);
            int available = r.getNumberRooms() - (int) bookedCount;
            if (available < 0) {
                available = 0;
            }
            if (qtyVal > available) {
                redirectAttributes.addFlashAttribute("errorMessage", "Only " + available + " room(s) of type '" + r.getType() + "' are available for this stay.");
                return "redirect:/hotels/" + hotel.getId() + "/rooms?checkin=" + checkin + "&checkout=" + checkout;
            }

            BigDecimal baseSub = paymentService.calculateRoomSubtotal(r.getPrice(), checkInDate, checkOutDate);
            BigDecimal rSubOriginal = baseSub.multiply(BigDecimal.valueOf(qtyVal));
            BigDecimal rSub = rSubOriginal;

            // Áp dụng khuyến mãi nếu có
            Integer promoId = promotionIds != null && promotionIds.size() > i ? promotionIds.get(i) : null;
            if (promoId != null && promoId > 0) {
                Promotion promo = promotionRepository.findById(promoId).orElse(null);
                if (promo != null && "ACTIVE".equalsIgnoreCase(promo.getStatus()) && promo.getHotel() != null
                        && promo.getHotel().getId() == r.getHotelId()) {
                    // Kiểm tra xem promotion đã được khách hàng này sử dụng chưa
                    if (!bookingRepository.existsByCustomerIdAndIdPromotion(customerId, promoId)) {
                        BigDecimal discountRate = promo.getDiscountPercent().divide(BigDecimal.valueOf(100));
                        BigDecimal discountAmount = rSubOriginal.multiply(discountRate);
                        rSub = rSubOriginal.subtract(discountAmount);

                        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                            java.util.Map<String, Object> dMap = new java.util.HashMap<>();
                            dMap.put("title", promo.getTitle());
                            dMap.put("amount", discountAmount);
                            detailedDiscounts.add(dMap);
                        }
                    }
                }
            }

            originalSubtotal = originalSubtotal.add(rSubOriginal);
            totalSubtotal = totalSubtotal.add(rSub);
            roomSubtotals.add(rSub);
        }

        BigDecimal totalDiscount = originalSubtotal.subtract(totalSubtotal);

        BigDecimal serviceFee = originalSubtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50_000)
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
            qrExpiresAt = LocalDateTime.now().plusMinutes(1); // QR hết hạn sau 15 phút
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

        String redirectUrl = "redirect:/booking/qr-payment-status?bookingId=" + parentBooking.getId();
        if (from != null && !from.isBlank()) {
            redirectUrl += "&from=" + from;
        }
        return redirectUrl;
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
        String transferInfo = paymentService.buildTransferInfo(bookingId,
                hotel != null ? hotel.getName() : "",
                room != null ? room.getRoomType() : "", nights);
        BigDecimal expectedAmount = payment != null ? payment.getAmount() : booking.getTotalPrice();

        // Kiểm tra qua PayOS hoặc Casso
        boolean verified = paymentService.verifyPayment(bookingId, payment, transferInfo, expectedAmount);

        if (verified) {
            // Xác nhận booking và payment thành công
            paymentService.confirmBooking(booking, payment);
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
        if (paymentService.checkAndUpdatePayOSStatus(booking, payment)) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/booking/history";
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
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal serviceFee = BigDecimal.ZERO;
        BigDecimal grandTotalPrice = BigDecimal.ZERO;

        java.util.List<java.util.Map<String, Object>> detailedDiscounts = new java.util.ArrayList<>();

        for (Booking b : bookingsList) {
            BigDecimal bTotalPrice = b.getTotalPrice();
            boolean isParent = (b.getSpecialNotes() == null
                    || !b.getSpecialNotes().startsWith("GROUP_BOOKING_parent:"));
            BigDecimal bServiceFee = (isParent && bTotalPrice.compareTo(BigDecimal.valueOf(50_000)) >= 0)
                    ? BigDecimal.valueOf(50_000)
                    : BigDecimal.ZERO;
            BigDecimal bSubtotalPlusTax = bTotalPrice.subtract(bServiceFee);

            BigDecimal bSubtotal = bSubtotalPlusTax.divide(BigDecimal.valueOf(1.1), 0, java.math.RoundingMode.HALF_UP);
            BigDecimal bTax = bSubtotalPlusTax.subtract(bSubtotal);

            BigDecimal singleSubtotal = paymentService.calculateRoomSubtotal(b.getRoom().getPrice(), b.getCheckInDate(),
                    b.getCheckOutDate());
            int qty = b.getQuantity() != null ? b.getQuantity() : 1;
            BigDecimal bOriginalSubtotal = singleSubtotal.multiply(BigDecimal.valueOf(qty));

            bookingQuantitiesMap.put(b.getId(), qty);
            totalSubtotal = totalSubtotal.add(bSubtotal);
            originalSubtotal = originalSubtotal.add(bOriginalSubtotal);
            totalTax = totalTax.add(bTax);
            serviceFee = serviceFee.add(bServiceFee);
            grandTotalPrice = grandTotalPrice.add(bTotalPrice);

            // Thu thập detailed discount từ db
            if (b.getIdPromotion() != null && b.getIdPromotion() > 0) {
                Promotion promo = promotionRepository.findById(b.getIdPromotion()).orElse(null);
                if (promo != null) {
                    BigDecimal discountRate = promo.getDiscountPercent().divide(BigDecimal.valueOf(100));
                    BigDecimal discountAmount = bOriginalSubtotal.multiply(discountRate);
                    if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                        java.util.Map<String, Object> dMap = new java.util.HashMap<>();
                        dMap.put("title", promo.getTitle());
                        dMap.put("amount", discountAmount);
                        detailedDiscounts.add(dMap);
                    }
                }
            }
        }

        BigDecimal totalDiscount = originalSubtotal.subtract(totalSubtotal);

        // Lấy hoặc tạo lại URL mã QR
        String transferInfo = paymentService.buildTransferInfo(bookingId, hotel.getName(), room.getRoomType(), nights);
        String qrUrl = paymentService.getQrUrl(bookingId, grandTotalPrice, transferInfo, payment);

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

        model.addAttribute("originalSubtotal", originalSubtotal);
        model.addAttribute("discount", totalDiscount);
        model.addAttribute("detailedDiscounts", detailedDiscounts);
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
        if (paymentService.checkAndUpdatePayOSStatusAjax(bookingId, booking, payment)) {
            return "CONFIRMED";
        }

        return booking.getStatus(); // Trả về PENDING nếu chưa thanh toán
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

        // Đổi VND sang USD
        BigDecimal vndAmount = payment.getAmount();
        double rate = exchangeRateService.getRate();
        BigDecimal exchangeRate = BigDecimal.valueOf(rate <= 0 ? 25400 : rate);
        BigDecimal usdAmount = vndAmount.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP);

        // Đặt phương thức là PAYPAL
        payment.setMethod("PAYPAL");
        paymentRepository.save(payment);

        // Gọi PayPal tạo order
        String[] orderResult = paymentService.handlePaypalOrderCreation(bookingId, usdAmount);
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
        boolean isSuccess = paymentService.handlePaypalOrderCapture(orderId, payment, booking);
        if (isSuccess) {
            paymentService.confirmBooking(booking, payment);
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

            paymentService.confirmBooking(booking, payment);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment confirmed via bypass! Your booking is now confirmed.");
        }
        return "redirect:/booking/history";
    }

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
        model.addAttribute("bankCode", paymentService.getBankCode() + " Bank");
        model.addAttribute("bankAccount", paymentService.getBankAccount());
        model.addAttribute("accountName", paymentService.getAccountName());
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
}
