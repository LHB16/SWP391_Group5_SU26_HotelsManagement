package vn.edu.fpt.hotel_management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.models.*;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.Environment;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final HotelRepository hotelRepository;
    private final PromotionRepository promotionRepository;
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


    public PaymentService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          HotelRepository hotelRepository,
                          PromotionRepository promotionRepository,
                          ExchangeRateService exchangeRateService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.hotelRepository = hotelRepository;
        this.promotionRepository = promotionRepository;
        this.exchangeRateService = exchangeRateService;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getAccountName() {
        return accountName;
    }

    public boolean isPayosConfigured() {
        return payosApiKey != null && !payosApiKey.isBlank();
    }

    // Xác nhận booking và payment thành công
    public void confirmBooking(Booking booking, Payment payment) {
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
    public String getQrUrl(int bookingId, BigDecimal totalPrice, String transferInfo, Payment payment) {
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
        if (isPayosConfigured()) {
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

    // Tạo nội dung chuyển khoản ngắn gọn (tối đa 25 ký tự)
    public String buildTransferInfo(int bookingId, String hotelName, String roomType, long nights) {
        String hotel = sanitizeForTransfer(hotelName, 6);
        String room = sanitizeForTransfer(roomType, 5);
        String dayText = nights == 1 ? "1 Day" : nights + " Days";
        String info = "BK" + bookingId + " " + hotel + " " + room + " " + dayText;
        if (info.length() > 25)
            info = info.substring(0, 25);
        return info.trim();
    }

    // Xóa ký tự đặc biệt và giới hạn độ dài
    public String sanitizeForTransfer(String raw, int maxLen) {
        if (raw == null || raw.isBlank())
            return "";
        String clean = raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    // Tính tổng tiền phòng (tăng 20% vào ngày lễ/cuối tuần)
    public BigDecimal calculateRoomSubtotal(BigDecimal basePrice, LocalDate checkin, LocalDate checkout) {
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

    // Xác thực thanh toán thông qua PayOS hoặc Casso
    public boolean verifyPayment(int bookingId, Payment payment, String transferInfo, BigDecimal expectedAmount) {
        if (isPayosConfigured()) {
            return checkPayOSPaymentStatus(bookingId, payment);
        } else if (cassoApiKey != null && !cassoApiKey.isBlank()) {
            return checkCassoTransaction(transferInfo, expectedAmount, payment);
        }
        return false;
    }

    // Tự động kiểm tra PayOS khi vào trang
    public boolean checkAndUpdatePayOSStatus(Booking booking, Payment payment) {
        if ("PENDING".equals(booking.getStatus()) && isPayosConfigured()) {
            if (checkPayOSPaymentStatus(booking.getId(), payment)) {
                confirmBooking(booking, payment);
                return true;
            }
        }
        return false;
    }

    // Tự động kiểm tra PayOS AJAX
    public boolean checkAndUpdatePayOSStatusAjax(int bookingId, Booking booking, Payment payment) {
        if (isPayosConfigured() && checkPayOSPaymentStatus(bookingId, payment)) {
            confirmBooking(booking, payment);
            return true;
        }
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

            if (payment == null || payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
                return false;
            }
            String orderCodeStr = payment.getTransactionId();

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

            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String cancelUrl = baseUrl + "/booking/qr-payment-status?bookingId=" + bookingId;
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

    // Gọi PayPal tạo order
    public String[] handlePaypalOrderCreation(int bookingId, BigDecimal usdAmount) {
        return createPaypalOrder(bookingId, usdAmount);
    }

    // Capture PayPal order
    public boolean handlePaypalOrderCapture(String orderId, Payment payment, Booking booking) {
        return capturePaypalOrder(orderId, payment, booking);
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

            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String cancelUrl = baseUrl + "/booking/paypal/cancel?bookingId=" + bookingId;
            String returnUrl = baseUrl + "/booking/paypal/success?bookingId=" + bookingId;

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
