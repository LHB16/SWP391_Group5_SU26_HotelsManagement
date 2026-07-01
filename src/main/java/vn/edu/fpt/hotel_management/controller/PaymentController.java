package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    // Read from application.properties: payment.bank-code, etc.
    @Value("${payment.bank-code:MB}")
    private String bankCode;

    @Value("${payment.bank-account:0326781606}")
    private String bankAccount;

    @Value("${payment.account-name:CHAU QUOC INH}")
    private String accountName;

    // Casso API key – must be set for real verification
    @Value("${payment.casso.api-key:}")
    private String cassoApiKey;

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

    // ======================== GET /booking/qr-payment ========================

    @GetMapping("/booking/qr-payment")
    public String showQrPaymentPage(
            @RequestParam(value = "roomId",   required = false) Integer roomId,
            @RequestParam(value = "roomIds",  required = false) java.util.List<Integer> roomIds,
            @RequestParam(value = "checkin",  required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            HttpSession session,
            Model model
    ) {
        // 1. Require login
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Resolve roomId from list when single id not supplied
        if (roomId == null) {
            if (roomIds != null && !roomIds.isEmpty()) {
                roomId = roomIds.get(0);
            } else {
                return "redirect:/hotels";
            }
        }

        // 2. Load room and hotel
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return "redirect:/hotels";
        }

        Hotel hotel = hotelRepository.findById(room.getHotelId()).orElse(null);
        if (hotel == null) {
            return "redirect:/hotels";
        }

        // 3. Default dates when not provided
        if (checkin == null || checkin.isBlank())  checkin  = LocalDate.now().toString();
        if (checkout == null || checkout.isBlank()) checkout = LocalDate.now().plusDays(1).toString();

        // 4. Parse dates
        LocalDate checkInDate  = LocalDate.parse(checkin);
        LocalDate checkOutDate = LocalDate.parse(checkout);

        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0) nights = 1;

        // 5. Calculate price
        BigDecimal subtotal   = calculateRoomSubtotal(room.getPrice(), checkInDate, checkOutDate);
        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50_000) : BigDecimal.ZERO;
        BigDecimal tax        = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPrice = subtotal.add(tax).add(serviceFee);

        // 6. Idempotency: reuse existing PENDING booking ONLY if its QR is still valid.
        //    If QR has expired the user must be allowed to create a fresh booking.
        java.util.Optional<Booking> existingOpt = bookingRepository
                .findByCustomerIdAndRoomIdAndCheckInDateAndCheckOutDateAndStatusOrderByCreatedAtDesc(
                        loggedInUser.getId(), roomId, checkInDate, checkOutDate, "PENDING")
                .stream()
                .filter(b -> {
                    // Accept booking only when its payment QR is still within the valid window
                    Payment p = paymentRepository.findByBookingId(b.getId()).orElse(null);
                    return p != null && !p.isQrExpired();
                })
                .findFirst();

        if (existingOpt.isPresent()) {
            // QR still valid — redirect to the existing QR page (same BK{id})
            int existingBookingId = existingOpt.get().getId();
            return "redirect:/booking/qr-payment-status?bookingId=" + existingBookingId
                    + "&hotelId=" + hotel.getId()
                    + "&roomId="  + roomId
                    + "&checkin=" + checkInDate
                    + "&checkout=" + checkOutDate;
        }

        // No valid PENDING booking found → create a new one (first visit or after expiry)
        Booking booking = new Booking();
        booking.setCustomer(loggedInUser);
        booking.setRoom(room);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("PENDING");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // 8. Create Payment record with PENDING status (first visit only)
        LocalDateTime qrExpiresAt = LocalDateTime.now().plusMinutes(15);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(totalPrice);
        payment.setMethod("QR_CODE");
        payment.setStatus("PENDING");
        payment.setPaidAt(null);
        payment.setQrExpiresAt(qrExpiresAt);
        paymentRepository.save(payment);

        // 9. Build QR URL (VietQR)
        String transferInfo = buildTransferInfo(booking.getId(), hotel.getName(), room.getRoomType());
        String encodedAccount = accountName.replace(" ", "%20");
        String qrUrl = "https://img.vietqr.io/image/" + bankCode + "-" + bankAccount + "-compact.png"
                + "?amount=" + totalPrice.longValue()
                + "&addInfo=" + transferInfo
                + "&accountName=" + encodedAccount;

        // Compute remaining seconds for the timer (passed to template)
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds();

        // 9. Push to model
        model.addAttribute("user",          loggedInUser);
        model.addAttribute("hotel",         hotel);
        model.addAttribute("room",          room);
        model.addAttribute("checkIn",       checkInDate);
        model.addAttribute("checkOut",      checkOutDate);
        model.addAttribute("nights",        nights);
        model.addAttribute("subtotal",      subtotal);
        model.addAttribute("tax",           tax);
        model.addAttribute("serviceFee",    serviceFee);
        model.addAttribute("totalPrice",    totalPrice);
        model.addAttribute("qrUrl",         qrUrl);
        model.addAttribute("bankCode",      bankCode + " Bank");
        model.addAttribute("bankAccount",   bankAccount);
        model.addAttribute("accountName",   accountName);
        model.addAttribute("transferInfo",  transferInfo);
        model.addAttribute("bookingId",     booking.getId());
        model.addAttribute("qrExpiresAt",      qrExpiresAt);
        model.addAttribute("remainingSeconds",  remainingSeconds);

        return "booking/qr-payment";
    }

    // ======================== POST /booking/confirm-casso ========================

    @PostMapping("/booking/confirm-casso")
    public String confirmViaCasso(
            @RequestParam("bookingId") int bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // 1. Load booking
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/hotels";
        }

        // 2. Load payment
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // 3. Check QR expiry
        if (payment != null && payment.isQrExpired()) {
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "QR code has expired. Please start a new booking.");
            int hotelId = (booking.getRoom() != null) ? booking.getRoom().getHotelId() : 0;
            return "redirect:/hotels/" + hotelId + "/rooms";
        }

        // 4. Already confirmed — redirect with success
        if (payment != null && "SUCCESS".equals(payment.getStatus())) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/hotels";
        }

        // 5. Query Casso API for real transaction verification
        Room roomForCasso = booking.getRoom();
        Hotel hotelForCasso = (roomForCasso != null) ? hotelRepository.findById(roomForCasso.getHotelId()).orElse(null) : null;
        String transferInfo = buildTransferInfo(bookingId,
                hotelForCasso != null ? hotelForCasso.getName() : "",
                roomForCasso != null ? roomForCasso.getRoomType() : "");
        BigDecimal expectedAmount = (payment != null) ? payment.getAmount() : booking.getTotalPrice();
        boolean verified = false;

        if (cassoApiKey != null && !cassoApiKey.isBlank()) {
            verified = checkCassoTransaction(transferInfo, expectedAmount);
        }

        if (verified) {
            // Mark payment and booking as successful
            booking.setStatus("CONFIRMED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            if (payment != null) {
                payment.setStatus("SUCCESS");
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment verified successfully! Your booking is confirmed.");
            return "redirect:/hotels";
        } else {
            // Payment not yet detected
            redirectAttributes.addFlashAttribute("errorMessage",
                    "We haven't detected your payment yet. Please complete the bank transfer with the exact reference code, then click \"Verify Payment\" again.");
            int hotelId = (booking.getRoom() != null) ? booking.getRoom().getHotelId() : 0;
            // Re-show the QR page by redirecting — dates and room info are stored in booking
            LocalDate ci = booking.getCheckInDate();
            LocalDate co = booking.getCheckOutDate();
            int rid = (booking.getRoom() != null) ? booking.getRoom().getId() : 0;
            return "redirect:/booking/qr-payment-status?bookingId=" + bookingId
                    + "&hotelId=" + hotelId
                    + "&roomId=" + rid
                    + "&checkin=" + ci
                    + "&checkout=" + co;
        }
    }

    // ======================== GET /booking/qr-payment-status ========================
    // Re-display the QR page for an existing booking (for "not found yet" redirect)

    @GetMapping("/booking/qr-payment-status")
    public String showExistingQrPage(
            @RequestParam("bookingId") int bookingId,
            @RequestParam(value = "hotelId", required = false, defaultValue = "0") int hotelId,
            @RequestParam(value = "roomId", required = false, defaultValue = "0") int roomId,
            @RequestParam(value = "checkin",  required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "redirect:/hotels";
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        // Kiểm tra nếu mã QR đã hết hạn (quá 15 phút)
        if (payment != null && payment.isQrExpired()) {
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            payment.setStatus("FAILED");
            paymentRepository.save(payment);

            redirectAttributes.addFlashAttribute("errorMessage", 
                "This payment session has expired. Please make a new reservation.");
            return "redirect:/booking/history";
        }

        Room room = booking.getRoom();
        Hotel hotel = (room != null) ? hotelRepository.findById(room.getHotelId()).orElse(null) : null;
        if (room == null || hotel == null) {
            return "redirect:/hotels";
        }

        LocalDate checkInDate  = booking.getCheckInDate();
        LocalDate checkOutDate = booking.getCheckOutDate();
        long nights = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        if (nights <= 0) nights = 1;

        BigDecimal totalPrice = booking.getTotalPrice();
        BigDecimal subtotal = calculateRoomSubtotal(room.getPrice(), checkInDate, checkOutDate);
        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50_000) : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);

        String transferInfo = buildTransferInfo(bookingId, hotel.getName(), room.getRoomType());
        String encodedAccount = accountName.replace(" ", "%20");
        String qrUrl = "https://img.vietqr.io/image/" + bankCode + "-" + bankAccount + "-compact.png"
                + "?amount=" + totalPrice.longValue()
                + "&addInfo=" + transferInfo
                + "&accountName=" + encodedAccount;

        LocalDateTime qrExpiresAt = (payment != null) ? payment.getQrExpiresAt() : LocalDateTime.now().plusMinutes(15);
        long remainingSeconds = Math.max(0, java.time.Duration.between(LocalDateTime.now(), qrExpiresAt).getSeconds());

        model.addAttribute("user",             loggedInUser);
        model.addAttribute("hotel",            hotel);
        model.addAttribute("room",             room);
        model.addAttribute("checkIn",          checkInDate);
        model.addAttribute("checkOut",         checkOutDate);
        model.addAttribute("nights",           nights);
        model.addAttribute("subtotal",         subtotal);
        model.addAttribute("tax",              tax);
        model.addAttribute("serviceFee",       serviceFee);
        model.addAttribute("totalPrice",       totalPrice);
        model.addAttribute("qrUrl",            qrUrl);
        model.addAttribute("bankCode",         bankCode + " Bank");
        model.addAttribute("bankAccount",      bankAccount);
        model.addAttribute("accountName",      accountName);
        model.addAttribute("transferInfo",     transferInfo);
        model.addAttribute("bookingId",        bookingId);
        model.addAttribute("qrExpiresAt",      qrExpiresAt);
        model.addAttribute("remainingSeconds", remainingSeconds);

        return "booking/qr-payment";
    }

    // ======================== Casso API helper ========================

    /**
     * Calls the real Casso API and checks if a transaction with matching
     * transferInfo (reference code) and sufficient amount has been received.
     */
    private boolean checkCassoTransaction(String transferInfo, BigDecimal expectedAmount) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.casso.vn/v2/transactions?sort=DESC&limit=20"))
                    .header("Authorization", "Apikey " + cassoApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Simple JSON scan – search for the transfer info keyword and verify amount.
            // The Casso response contains "description" fields; we look for our unique code.
            if (body != null && body.contains(transferInfo)) {
                // Extra check: ensure the amount field near our description matches.
                // Casso JSON format has "amount" fields; we do a proximity check.
                int idx = body.indexOf(transferInfo);
                // Look in a surrounding window of 500 chars for amount value
                int start = Math.max(0, idx - 500);
                int end   = Math.min(body.length(), idx + 500);
                String window = body.substring(start, end);

                long expected = expectedAmount.longValue();
                // Check if the expected amount (or higher) appears in this window
                if (window.contains(String.valueOf(expected)) || containsHigherAmount(window, expected)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[Casso] API call failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Searches a JSON window string for a numeric value >= expectedAmount.
     * Scans comma-separated numbers that could represent amounts.
     */
    private boolean containsHigherAmount(String window, long expectedAmount) {
        // Look for "amount":NUMBER patterns in the JSON window
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"amount\"\\s*:\\s*(\\d+)").matcher(window);
        while (matcher.find()) {
            try {
                long found = Long.parseLong(matcher.group(1));
                if (found >= expectedAmount) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    // ======================== Transfer info builder ========================

    /**
     * Builds the bank transfer reference code that includes:
     * - Booking ID (unique key for Casso matching)
     * - Abbreviated hotel name (first 10 alphanumeric chars, uppercase)
     * - Room type (first 10 alphanumeric chars, uppercase)
     *
     * Example: "BK12 GRANDHOTEL DELUXE"
     * Max total length: ~30 chars, safe for all Vietnamese bank memo fields.
     */
    private String buildTransferInfo(int bookingId, String hotelName, String roomType) {
        String hotelAbbr = sanitizeForTransfer(hotelName, 10);
        String roomAbbr  = sanitizeForTransfer(roomType,  10);
        String info = "BK" + bookingId;
        if (!hotelAbbr.isEmpty()) info += " " + hotelAbbr;
        if (!roomAbbr.isEmpty())  info += " " + roomAbbr;
        return info;
    }

    /** Strips non-alphanumeric characters and truncates to maxLen, uppercase. */
    private String sanitizeForTransfer(String raw, int maxLen) {
        if (raw == null || raw.isBlank()) return "";
        String clean = raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    // ======================== Price calculation helpers ========================

    private boolean isHolidayOrWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        if (m == 1  && d == 1)  return true;   // New Year's Day
        if (m == 4  && d == 30) return true;    // Reunification Day
        if (m == 5  && d == 1)  return true;    // International Labour Day
        if (m == 9  && d == 2)  return true;    // National Day
        if (m == 2  && d == 14) return true;    // Valentine's Day
        if (m == 3  && d == 8)  return true;    // International Women's Day
        if (m == 6  && d == 1)  return true;    // International Children's Day
        if (m == 10 && d == 20) return true;    // Vietnamese Women's Day
        if (m == 11 && d == 20) return true;    // Vietnamese Teachers' Day
        if (m == 12 && d == 25) return true;    // Christmas Day

        // Lunar New Year 2025 (Jan 28 – Feb 3)
        if (date.getYear() == 2025) {
            if (m == 1 && d >= 28) return true;
            if (m == 2 && d <= 3)  return true;
        }
        // Lunar New Year 2026 (Feb 16 – Feb 22)
        if (date.getYear() == 2026) {
            if (m == 2 && d >= 16 && d <= 22) return true;
        }

        return false;
    }

    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, LocalDate checkin, LocalDate checkout) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate temp = checkin;
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
