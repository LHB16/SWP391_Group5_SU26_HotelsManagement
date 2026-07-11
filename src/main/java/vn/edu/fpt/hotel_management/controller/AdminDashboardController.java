package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.HotelVerificationDocument;
import vn.edu.fpt.hotel_management.entity.FeedbackReply;
import vn.edu.fpt.hotel_management.entity.Review;
import vn.edu.fpt.hotel_management.entity.Refund;
import vn.edu.fpt.hotel_management.repository.BookingRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.HotelVerificationDocumentRepository;
import vn.edu.fpt.hotel_management.repository.FeedbackReplyRepository;
import vn.edu.fpt.hotel_management.repository.ReviewRepository;
import vn.edu.fpt.hotel_management.repository.RefundRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminDashboardController {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private HotelOwnerRepository hotelOwnerRepository;

    @Autowired
    private HotelVerificationDocumentRepository hotelVerificationDocumentRepository;

    @Autowired
    private FeedbackReplyRepository feedbackReplyRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RefundRepository refundRepository;

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "tab", defaultValue = "revenuePanel") String tab,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "searchType", defaultValue = "username") String searchType,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session, 
            Model model) {
            
        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        // Tự động chuyển các booking CONFIRMED đã quá giờ checkout (12:00 trưa) sang COMPLETED
        try {
            List<Booking> confirmedBookings = bookingRepository.findByStatusInOrderByCreatedAtDesc(java.util.Arrays.asList("CONFIRMED"));
            LocalDateTime now = LocalDateTime.now();
            for (Booking b : confirmedBookings) {
                if (b.getCheckOutDate() != null
                        && b.getCheckOutDate().atTime(12, 0).isBefore(now)) {
                    b.setStatus("COMPLETED");
                    b.setUpdatedAt(now);
                    bookingRepository.save(b);
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi nếu có
        }
        
        if ("hotelApprovalPanel".equals(tab)) {
            tab = "hotelOwnerAccounts";
        }
        
        model.addAttribute("user", loggedInUser);
        model.addAttribute("tab", tab);

        // Tính tổng số lượng yêu cầu xác thực cần xử lý
        long pendingOwners = hotelOwnerRepository.countByVerificationStatus("PENDING");
        long pendingHotels = hotelRepository.countByApprovalStatus("PENDING");
        long totalPendingCount = pendingOwners + pendingHotels;
        model.addAttribute("totalPendingCount", totalPendingCount);

        // Tính tổng số lượng yêu cầu hoàn tiền đang chờ duyệt
        long pendingRefundCount = refundRepository.findByStatusOrderByRequestedAtAsc("PENDING").size();
        model.addAttribute("pendingRefundCount", pendingRefundCount);

        // Khởi tạo các Pageable và biến chứa dữ liệu cần thiết
        Pageable defaultCustomerPageable = PageRequest.of(page, 5, Sort.by("userAccount.username").ascending());
        Pageable defaultOwnerPageable = PageRequest.of(page, 5, Sort.by("userAccount.username").ascending());
        Pageable defaultManagedHotelPageable = PageRequest.of(page, 5, Sort.by("id").descending());
        Pageable defaultFeedbackReplyPageable = PageRequest.of(page, 5, Sort.by("id").descending());
        Pageable defaultReviewPageable = PageRequest.of(page, 5, Sort.by("id").descending());
        Pageable searchPageable = PageRequest.of(page, 5);

        // --- Tab 1: revenuePanel (Doanh thu) ---
        if ("revenuePanel".equals(tab)) {
            // Danh sách khách sạn phục vụ lọc doanh thu
            model.addAttribute("hotels", hotelRepository.findAll());
            model.addAttribute("revenueData", Collections.emptyList()); // Loại bỏ thống kê phía Java dư thừa

            // Danh sách đặt phòng hợp lệ toàn hệ thống
            List<Booking> validBookings = bookingRepository.findByStatusInOrderByCreatedAtDesc(
                    Arrays.asList("CONFIRMED", "CHECKED_IN", "CHECKED_OUT")
            );
            List<Map<String, Object>> mappedBookings = validBookings.stream().map(b -> {
                Map<String, Object> map = new HashMap<>();
                map.put("bookingId", b.getId());
                map.put("customerId", b.getCustomer() != null ? b.getCustomer().getId() : null);
                map.put("roomType", b.getRoom() != null ? b.getRoom().getRoomType() : null);
                
                if (b.getRoom() != null) {
                    int hotelId = b.getRoom().getHotelId();
                    Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
                    map.put("hotelName", hotel != null ? hotel.getName() : "Unknown");
                } else {
                    map.put("hotelName", "Unknown");
                }
                
                map.put("checkInDate", b.getCheckInDate() != null ? b.getCheckInDate().toString() : null);
                map.put("checkOutDate", b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : null);
                map.put("totalPrice", b.getTotalPrice());
                map.put("bookingStatus", b.getStatus());
                map.put("paymentStatus", (b.getPayment() != null && b.getPayment().getStatus() != null) ? b.getPayment().getStatus() : "PENDING");
                map.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
                return map;
            }).collect(Collectors.toList());
            model.addAttribute("bookings", mappedBookings);
        } else {
            model.addAttribute("hotels", Collections.emptyList());
            model.addAttribute("revenueData", Collections.emptyList());
            model.addAttribute("bookings", Collections.emptyList());
        }

        // --- Tab 2: customerPanel (Tài khoản Khách hàng) ---
        Page<Customer> customerPage = Page.empty(defaultCustomerPageable);
        if ("customerPanel".equals(tab)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.trim();
                if ("id".equals(searchType)) {
                    try {
                        int targetId = Integer.parseInt(query);
                        customerPage = customerRepository.findById(targetId, searchPageable);
                    } catch (NumberFormatException e) {
                        customerPage = Page.empty(searchPageable);
                    }
                } else if ("fullName".equals(searchType)) {
                    customerPage = customerRepository.findByFullNameContainingIgnoreCase(query, searchPageable);
                } else if ("email".equals(searchType)) {
                    customerPage = customerRepository.findByUserAccountEmailContainingIgnoreCase(query, searchPageable);
                } else {
                    customerPage = customerRepository.findByUserAccountUsernameContainingIgnoreCase(query, searchPageable);
                }
            } else {
                customerPage = customerRepository.findAll(defaultCustomerPageable);
            }
        }
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("customerCurrentPage", "customerPanel".equals(tab) ? page : 0);
        model.addAttribute("customerTotalPages", customerPage.getTotalPages());

        // --- Tab 3: hotelOwnerAccounts (Tài khoản Chủ khách sạn) ---
        Page<HotelOwner> ownerPage = Page.empty(defaultOwnerPageable);
        Map<Integer, Long> ownerPendingCounts = new HashMap<>();
        if ("hotelOwnerAccounts".equals(tab)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.trim();
                if ("id".equals(searchType)) {
                    try {
                        int targetId = Integer.parseInt(query);
                        ownerPage = hotelOwnerRepository.findById(targetId, searchPageable);
                    } catch (NumberFormatException e) {
                        ownerPage = Page.empty(searchPageable);
                    }
                } else if ("fullName".equals(searchType)) {
                    ownerPage = hotelOwnerRepository.findByFullNameContainingIgnoreCase(query, searchPageable);
                } else if ("email".equals(searchType)) {
                    ownerPage = hotelOwnerRepository.findByUserAccountEmailContainingIgnoreCase(query, searchPageable);
                } else if ("phone".equals(searchType)) {
                    ownerPage = hotelOwnerRepository.findByPhoneContaining(query, searchPageable);
                } else {
                    ownerPage = hotelOwnerRepository.findByUserAccountUsernameContainingIgnoreCase(query, searchPageable);
                }
            } else {
                ownerPage = hotelOwnerRepository.findAllByOrderByPendingPriorityAsc(defaultOwnerPageable);
            }
            
            for (HotelOwner owner : ownerPage.getContent()) {
                long count = 0;
                if ("PENDING".equals(owner.getVerificationStatus())) {
                    count++;
                }
                count += hotelRepository.countByApprovalStatusAndOwnerId("PENDING", owner.getId());
                ownerPendingCounts.put(owner.getId(), count);
            }
        }
        model.addAttribute("owners", ownerPage.getContent());
        model.addAttribute("ownerPendingCounts", ownerPendingCounts);
        model.addAttribute("ownerCurrentPage", "hotelOwnerAccounts".equals(tab) ? page : 0);
        model.addAttribute("ownerTotalPages", ownerPage.getTotalPages());

        // --- Tab 4: hotelApprovalPanel (Duyệt tài liệu - Không sử dụng trực tiếp trên HTML, gán rỗng để tối ưu) ---
        model.addAttribute("pendingHotels", Collections.emptyList());
        model.addAttribute("hotelCurrentPage", 0);
        model.addAttribute("hotelTotalPages", 0);

        // --- Tab 5: hotelManagementPanel (Quản lý trạng thái hoạt động Khách sạn) ---
        Page<Hotel> managedHotelPage = Page.empty(defaultManagedHotelPageable);
        if ("hotelManagementPanel".equals(tab)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.trim();
                managedHotelPage = hotelRepository.findByNameContainingIgnoreCase(query, searchPageable);
            } else {
                managedHotelPage = hotelRepository.findAll(defaultManagedHotelPageable);
            }
        }
        model.addAttribute("managedHotels", managedHotelPage.getContent());
        model.addAttribute("managedHotelCurrentPage", "hotelManagementPanel".equals(tab) ? page : 0);
        model.addAttribute("managedHotelTotalPages", managedHotelPage.getTotalPages());

        // --- Tab 6: ownerFeedbackPanel (Quản lý bình luận/phản hồi) ---
        Page<FeedbackReply> feedbackReplyPage = Page.empty(defaultFeedbackReplyPageable);
        if ("ownerFeedbackPanel".equals(tab)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.trim();
                feedbackReplyPage = feedbackReplyRepository.searchFeedbackReplies(query, searchPageable);
            } else {
                feedbackReplyPage = feedbackReplyRepository.findAllWithAssociations(defaultFeedbackReplyPageable);
            }
        }
        model.addAttribute("feedbackReplies", feedbackReplyPage.getContent());
        model.addAttribute("feedbackReplyCurrentPage", "ownerFeedbackPanel".equals(tab) ? page : 0);
        model.addAttribute("feedbackReplyTotalPages", feedbackReplyPage.getTotalPages());

        // --- Tab 7: customerReviewPanel (Kiểm duyệt đánh giá của Khách hàng) ---
        Page<Review> customerReviewPage = Page.empty(defaultReviewPageable);
        if ("customerReviewPanel".equals(tab)) {
            customerReviewPage = reviewRepository.findByStatusIn(Arrays.asList("PENDING", "HIDDEN"), defaultReviewPageable);
        }
        model.addAttribute("customerReviews", customerReviewPage.getContent());
        model.addAttribute("customerReviewCurrentPage", "customerReviewPanel".equals(tab) ? page : 0);
        model.addAttribute("customerReviewTotalPages", customerReviewPage.getTotalPages());

        // --- Tab 8: refundPanel (Quản lý hoàn tiền) ---
        Page<Refund> refundPage = Page.empty();
        long pendingCount = 0;
        long processedCount = 0;
        long rejectedCount = 0;
        if ("refundPanel".equals(tab)) {
            String filterStatus = (status != null && !status.isBlank()) ? status.toUpperCase() : "ALL";
            
            // Định nghĩa Sort: PENDING lên đầu (alphabet: PENDING < PROCESSED < REJECTED), rồi requestedAt tăng dần (cũ nhất trước)
            Sort refundSort = Sort.by(Sort.Order.asc("status"), Sort.Order.asc("requestedAt"));
            Pageable refundPageable = PageRequest.of(page, 5, refundSort);

            if (searchQuery != null && !searchQuery.isBlank()) {
                String query = searchQuery.trim();
                boolean isStatusAll = "ALL".equals(filterStatus);
                
                if ("fullName".equals(searchType)) {
                    if (isStatusAll) {
                        refundPage = refundRepository.findByBookingCustomerFullNameContainingIgnoreCase(query, refundPageable);
                    } else {
                        refundPage = refundRepository.findByStatusAndBookingCustomerFullNameContainingIgnoreCase(filterStatus, query, refundPageable);
                    }
                } else if ("email".equals(searchType)) {
                    if (isStatusAll) {
                        refundPage = refundRepository.findByBookingCustomerUserAccountEmailContainingIgnoreCase(query, refundPageable);
                    } else {
                        refundPage = refundRepository.findByStatusAndBookingCustomerUserAccountEmailContainingIgnoreCase(filterStatus, query, refundPageable);
                    }
                } else if ("id".equals(searchType)) {
                    try {
                        int refundId = Integer.parseInt(query);
                        Optional<Refund> refundOpt = refundRepository.findById(refundId);
                        if (refundOpt.isPresent()) {
                            Refund r = refundOpt.get();
                            if (isStatusAll || filterStatus.equals(r.getStatus())) {
                                refundPage = new org.springframework.data.domain.PageImpl<>(
                                        Collections.singletonList(r), refundPageable, 1
                                );
                            } else {
                                refundPage = Page.empty(refundPageable);
                            }
                        } else {
                            refundPage = Page.empty(refundPageable);
                        }
                    } catch (NumberFormatException e) {
                        refundPage = Page.empty(refundPageable);
                    }
                } else if ("bookingId".equals(searchType)) {
                    try {
                        int bookingId = Integer.parseInt(query);
                        Optional<Refund> refundOpt = refundRepository.findByBookingId(bookingId);
                        if (refundOpt.isPresent()) {
                            Refund r = refundOpt.get();
                            if (isStatusAll || filterStatus.equals(r.getStatus())) {
                                refundPage = new org.springframework.data.domain.PageImpl<>(
                                        Collections.singletonList(r), refundPageable, 1
                                );
                            } else {
                                refundPage = Page.empty(refundPageable);
                            }
                        } else {
                            refundPage = Page.empty(refundPageable);
                        }
                    } catch (NumberFormatException e) {
                        refundPage = Page.empty(refundPageable);
                    }
                }
            } else {
                if ("ALL".equals(filterStatus)) {
                    refundPage = refundRepository.findAll(refundPageable);
                } else {
                    refundPage = refundRepository.findByStatus(filterStatus, refundPageable);
                }
            }

            // Đếm số lượng
            pendingCount   = refundRepository.findByStatusOrderByRequestedAtAsc("PENDING").size();
            processedCount = refundRepository.findByStatusOrderByRequestedAtAsc("PROCESSED").size();
            rejectedCount  = refundRepository.findByStatusOrderByRequestedAtAsc("REJECTED").size();
            
            model.addAttribute("refunds", refundPage.getContent());
            model.addAttribute("filterStatus", filterStatus);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("processedCount", processedCount);
            model.addAttribute("rejectedCount", rejectedCount);
            model.addAttribute("refundCurrentPage", "refundPanel".equals(tab) ? page : 0);
            model.addAttribute("refundTotalPages", refundPage.getTotalPages());
            model.addAttribute("totalCount", refundPage.getTotalElements());
        }

        // --- Lưu lại thông tin search nếu có ---
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            model.addAttribute("searchQuery", searchQuery);
            model.addAttribute("searchType", searchType);
        }

        // --- Tab 9: payoutPanel (Đối soát & Chuyển tiền cho Owner) ---
        if ("payoutPanel".equals(tab)) {
            String filterPayoutStatus = (status != null && !status.isBlank()) ? status.toUpperCase() : "PENDING";
            Pageable payoutPageable = PageRequest.of(page, 10);

            Page<Booking> payoutBookingPage;
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.trim();
                // Tìm kiếm theo tên khách sạn HOẶC tên Owner bằng Magic Method
                payoutBookingPage = bookingRepository
                        .findByStatusAndPayment_StatusAndPayoutStatusAndHotel_NameContainingIgnoreCaseOrStatusAndPayment_StatusAndPayoutStatusAndHotel_Owner_FullNameContainingIgnoreCaseOrderByCheckOutDateDesc(
                                "COMPLETED", "PAID", filterPayoutStatus, query,
                                "COMPLETED", "PAID", filterPayoutStatus, query,
                                payoutPageable
                        );
            } else {
                payoutBookingPage = bookingRepository
                        .findByStatusAndPayment_StatusAndPayoutStatusOrderByCheckOutDateDesc(
                                "COMPLETED", "PAID", filterPayoutStatus, payoutPageable
                        );
            }

            long pendingPayoutCount = bookingRepository
                    .findByStatusAndPayment_StatusAndPayoutStatusOrderByCheckOutDateDesc(
                            "COMPLETED", "PAID", "PENDING",
                            PageRequest.of(0, Integer.MAX_VALUE)
                    ).getTotalElements();
            long paidPayoutCount = bookingRepository
                    .findByStatusAndPayment_StatusAndPayoutStatusOrderByCheckOutDateDesc(
                            "COMPLETED", "PAID", "PAID",
                            PageRequest.of(0, Integer.MAX_VALUE)
                    ).getTotalElements();

            model.addAttribute("payoutBookings", payoutBookingPage.getContent());
            model.addAttribute("payoutCurrentPage", page);
            model.addAttribute("payoutTotalPages", payoutBookingPage.getTotalPages());
            model.addAttribute("payoutTotalElements", payoutBookingPage.getTotalElements());
            model.addAttribute("filterPayoutStatus", filterPayoutStatus);
            model.addAttribute("pendingPayoutCount", pendingPayoutCount);
            model.addAttribute("paidPayoutCount", paidPayoutCount);
        } else {
            model.addAttribute("payoutBookings", Collections.emptyList());
            model.addAttribute("payoutCurrentPage", 0);
            model.addAttribute("payoutTotalPages", 0);
            model.addAttribute("payoutTotalElements", 0L);
            model.addAttribute("filterPayoutStatus", "PENDING");
            model.addAttribute("pendingPayoutCount", 0L);
            model.addAttribute("paidPayoutCount", 0L);
        }

        // Hiển thị giao diện dashboard admin
        return "admin/dashboard";
    }

    @GetMapping("/admin/customer-detail")
    public String showCustomerDetail(
            @RequestParam("id") int customerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("user", loggedInUser);

        // Tìm khách hàng
        Customer selectedCustomer = customerRepository.findById(customerId).orElse(null);
        if (selectedCustomer == null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("selectedCustomer", selectedCustomer);

        // Lấy danh sách đặt phòng có phân trang (10 items/trang)
        Pageable pageable = PageRequest.of(page, 10);
        Page<Booking> bookingPage = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);

        // Map sang dữ liệu để dễ hiển thị ngoài HTML
        List<Map<String, Object>> customerBookings = bookingPage.getContent().stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getId());
            map.put("roomType", b.getRoom() != null ? b.getRoom().getRoomType() : null);
            
            if (b.getRoom() != null) {
                int hotelId = b.getRoom().getHotelId();
                Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
                map.put("hotelName", hotel != null ? hotel.getName() : "Unknown");
            } else {
                map.put("hotelName", "Unknown");
            }
            
            map.put("checkInDate", b.getCheckInDate());
            map.put("checkOutDate", b.getCheckOutDate());
            map.put("totalPrice", b.getTotalPrice());
            map.put("bookingStatus", b.getStatus());
            map.put("paymentStatus", (b.getPayment() != null && b.getPayment().getStatus() != null) ? b.getPayment().getStatus() : "PENDING");
            return map;
        }).collect(Collectors.toList());

        model.addAttribute("customerBookings", customerBookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingPage.getTotalPages());

        return "admin/customer-detail";
    }

    @PostMapping("/admin/toggle-user-status")
    public String toggleUserStatus(
            @RequestParam("userId") int userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Customer targetCustomer = customerRepository.findById(userId).orElse(null);
        if (targetCustomer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Account not found or you do not have permission to modify it.");
            return "redirect:/admin/dashboard";
        }
        User targetUser = targetCustomer.getUserAccount();

        // Đảo ngược trạng thái enabled
        boolean newStatus = !targetUser.isEnabled();
        targetUser.setEnabled(newStatus);
        userRepository.save(targetUser);

        String action = newStatus ? "enabled" : "disabled";
        redirectAttributes.addFlashAttribute("successMessage",
                "Account \"" + targetCustomer.getFullName() + "\" has been " + action + " successfully.");

        return "redirect:/admin/customer-detail?id=" + userId + "&page=" + page;
    }

    /*
     * PHƯƠNG THỨC NÀY ĐÃ BỊ DƯ THỪA (DEAD CODE):
     * Không còn được gọi từ Controller và UI do bộ lọc doanh thu đã được chuyển sang xử lý tối ưu động ở Frontend.
     * Tạm thời comment out để tránh các câu lệnh SELECT findAll() làm giảm hiệu năng hệ thống.
     *
    private List<Map<String, Object>> getHotelRevenueStatistics() {
        List<Booking> bookings = bookingRepository.findAll();
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, Hotel> hotelMap = hotels.stream().collect(Collectors.toMap(Hotel::getId, h -> h));

        // Lọc danh sách đơn đặt phòng hợp lệ
        List<Booking> filtered = bookings.stream()
                .filter(b -> b.getStatus() != null && 
                        (b.getStatus().equals("CONFIRMED") || b.getStatus().equals("CHECKED_IN") || b.getStatus().equals("CHECKED_OUT")))
                .collect(Collectors.toList());

        // Gom nhóm dữ liệu theo khách sạn và thời gian
        Map<String, Map<String, Object>> statsMap = new HashMap<>();

        for (Booking b : filtered) {
            if (b.getRoom() == null) continue;
            int hotelId = b.getRoom().getHotelId();
            Hotel hotel = hotelMap.get(hotelId);
            String hotelName = hotel != null ? hotel.getName() : "Unknown";

            LocalDateTime dateTime = b.getCreatedAt();
            if (dateTime == null) {
                dateTime = LocalDateTime.now(); // Giá trị mặc định
            }
            int year = dateTime.getYear();
            int month = dateTime.getMonthValue();
            int quarter = (month - 1) / 3 + 1;

            String key = hotelId + "-" + year + "-" + month;

            BigDecimal price = b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO;

            if (!statsMap.containsKey(key)) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("hotelId", hotelId);
                stat.put("hotelName", hotelName);
                stat.put("year", year);
                stat.put("quarter", quarter);
                stat.put("month", month);
                stat.put("revenue", price);
                stat.put("bookingCount", 1L);
                statsMap.put(key, stat);
            } else {
                Map<String, Object> stat = statsMap.get(key);
                BigDecimal currentRevenue = (BigDecimal) stat.get("revenue");
                stat.put("revenue", currentRevenue.add(price));
                stat.put("bookingCount", (Long) stat.get("bookingCount") + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(statsMap.values());
        
        // Sắp xếp kết quả theo thời gian và tên khách sạn
        result.sort((m1, m2) -> {
            int y1 = (int) m1.get("year");
            int y2 = (int) m2.get("year");
            if (y1 != y2) return Integer.compare(y2, y1);

            int mon1 = (int) m1.get("month");
            int mon2 = (int) m2.get("month");
            if (mon1 != mon2) return Integer.compare(mon2, mon1);

            String name1 = (String) m1.get("hotelName");
            String name2 = (String) m2.get("hotelName");
            return name1.compareTo(name2);
        });

        return result;
    }
    */

    @GetMapping("/admin/owner-detail")
    public String showOwnerDetail(
            @RequestParam("id") int ownerId,
            HttpSession session,
            Model model) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("user", loggedInUser);

        // Tìm hotel owner
        HotelOwner selectedOwner = hotelOwnerRepository.findById(ownerId).orElse(null);
        if (selectedOwner == null) {
            return "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
        }
        model.addAttribute("selectedOwner", selectedOwner);

        // Lấy danh sách khách sạn thuộc sở hữu của owner này
        List<Hotel> ownerHotels = hotelRepository.findByOwnerId(ownerId);
        model.addAttribute("ownerHotels", ownerHotels);

        // Tạo bản đồ tài liệu xác minh cho từng khách sạn
        Map<Integer, HotelVerificationDocument> hotelDocMap = new HashMap<>();
        for (Hotel hotel : ownerHotels) {
            List<HotelVerificationDocument> docs = hotelVerificationDocumentRepository.findByHotelId(hotel.getId());
            if (!docs.isEmpty()) {
                hotelDocMap.put(hotel.getId(), docs.get(0));
            } else {
                hotelDocMap.put(hotel.getId(), null);
            }
        }
        model.addAttribute("hotelDocMap", hotelDocMap);

        return "admin/owner-detail";
    }

    @PostMapping("/admin/verify-owner/approve")
    public String approveOwner(
            @RequestParam("ownerId") int ownerId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelOwner owner = hotelOwnerRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel Owner not found.");
            return "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
        }

        owner.setVerificationStatus("APPROVED");
        owner.setVerifiedAt(LocalDateTime.now());
        owner.setRejectionReason(null);
        hotelOwnerRepository.save(owner);

        // Enable user account as well to allow logging in
        User user = owner.getUserAccount();
        if (user != null) {
            user.setEnabled(true);
            userRepository.save(user);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Hotel Owner \"" + owner.getFullName() + "\" has been approved successfully.");
        return "redirect:/admin/owner-detail?id=" + ownerId;
    }

    @PostMapping("/admin/verify-owner/reject")
    public String rejectOwner(
            @RequestParam("ownerId") int ownerId,
            @RequestParam("rejectionReason") String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelOwner owner = hotelOwnerRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel Owner not found.");
            return "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
        }

        owner.setVerificationStatus("REJECTED");
        owner.setVerifiedAt(LocalDateTime.now());
        owner.setRejectionReason(rejectionReason);
        hotelOwnerRepository.save(owner);

        redirectAttributes.addFlashAttribute("successMessage", "Hotel Owner \"" + owner.getFullName() + "\" has been rejected.");
        return "redirect:/admin/owner-detail?id=" + ownerId;
    }

    @PostMapping("/admin/verify-hotel/approve")
    public String approveHotel(
            @RequestParam("docId") int docId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelVerificationDocument doc = hotelVerificationDocumentRepository.findById(docId).orElse(null);
        if (doc == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Verification document not found.");
            return "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
        }

        doc.setUploadStatus("APPROVED");
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setRejectionReason(null);
        hotelVerificationDocumentRepository.save(doc);

        Hotel hotel = doc.getHotel();
        int ownerId = 0;
        if (hotel != null) {
            hotel.setApprovalStatus("APPROVED");
            hotel.setActive(true);
            hotel.setApprovedAt(LocalDateTime.now());
            hotel.setRejectionReason(null);
            hotelRepository.save(hotel);
            if (hotel.getOwner() != null) {
                ownerId = hotel.getOwner().getId();
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Hotel verification documents approved successfully.");
        return ownerId > 0 ? "redirect:/admin/owner-detail?id=" + ownerId : "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
    }

    @PostMapping("/admin/verify-hotel/reject")
    public String rejectHotel(
            @RequestParam("docId") int docId,
            @RequestParam("rejectionReason") String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelVerificationDocument doc = hotelVerificationDocumentRepository.findById(docId).orElse(null);
        if (doc == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Verification document not found.");
            return "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
        }

        doc.setUploadStatus("REJECTED");
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setRejectionReason(rejectionReason);
        hotelVerificationDocumentRepository.save(doc);

        Hotel hotel = doc.getHotel();
        int ownerId = 0;
        if (hotel != null) {
            hotel.setApprovalStatus("REJECTED");
            hotel.setActive(false);
            hotel.setRejectionReason(rejectionReason);
            hotelRepository.save(hotel);
            if (hotel.getOwner() != null) {
                ownerId = hotel.getOwner().getId();
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Hotel verification documents rejected.");
        return ownerId > 0 ? "redirect:/admin/owner-detail?id=" + ownerId : "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
    }

    @PostMapping("/admin/toggle-owner-status")
    public String toggleOwnerStatus(
            @RequestParam("ownerId") int ownerId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelOwner targetOwner = hotelOwnerRepository.findById(ownerId).orElse(null);
        if (targetOwner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner account not found.");
            return "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
        }
        User targetUser = targetOwner.getUserAccount();

        // Đảo ngược trạng thái enabled
        boolean newStatus = !targetUser.isEnabled();
        targetUser.setEnabled(newStatus);
        userRepository.save(targetUser);

        if (!newStatus) {
            // Vô hiệu hóa tất cả khách sạn của Owner này khi tài khoản bị disable
            List<Hotel> ownerHotels = hotelRepository.findByOwnerId(ownerId);
            for (Hotel hotel : ownerHotels) {
                hotel.setActive(false);
                hotelRepository.save(hotel);
            }
        }

        String action = newStatus ? "enabled" : "disabled";
        redirectAttributes.addFlashAttribute("successMessage",
                "Owner account \"" + targetOwner.getFullName() + "\" has been " + action + " successfully.");

        return "redirect:/admin/owner-detail?id=" + ownerId;
    }

    @PostMapping("/admin/hotel/approve")
    public String approveHotelDirect(
            @RequestParam("hotelId") int hotelId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found.");
            return "redirect:/admin/dashboard?tab=hotelApprovalPanel";
        }

        hotel.setApprovalStatus("APPROVED");
        hotel.setActive(true);
        hotel.setApprovedAt(LocalDateTime.now());
        hotel.setRejectionReason(null);
        hotelRepository.save(hotel);

        // Nếu có tài liệu xác minh liên quan, cũng tự động approve luôn
        List<HotelVerificationDocument> docs = hotelVerificationDocumentRepository.findByHotelId(hotelId);
        for (HotelVerificationDocument doc : docs) {
            doc.setUploadStatus("APPROVED");
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setRejectionReason(null);
            hotelVerificationDocumentRepository.save(doc);
        }

        int ownerId = (hotel.getOwner() != null) ? hotel.getOwner().getId() : 0;
        redirectAttributes.addFlashAttribute("successMessage", "Hotel \"" + hotel.getName() + "\" approved successfully.");
        return ownerId > 0 ? "redirect:/admin/owner-detail?id=" + ownerId : "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
    }

    @PostMapping("/admin/hotel/reject")
    public String rejectHotelDirect(
            @RequestParam("hotelId") int hotelId,
            @RequestParam("rejectionReason") String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found.");
            return "redirect:/admin/dashboard?tab=hotelApprovalPanel";
        }

        hotel.setApprovalStatus("REJECTED");
        hotel.setActive(false);
        hotel.setRejectionReason(rejectionReason);
        hotelRepository.save(hotel);

        // Nếu có tài liệu xác minh liên quan, cũng reject luôn
        List<HotelVerificationDocument> docs = hotelVerificationDocumentRepository.findByHotelId(hotelId);
        for (HotelVerificationDocument doc : docs) {
            doc.setUploadStatus("REJECTED");
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setRejectionReason(rejectionReason);
            hotelVerificationDocumentRepository.save(doc);
        }

        int ownerId = (hotel.getOwner() != null) ? hotel.getOwner().getId() : 0;
        redirectAttributes.addFlashAttribute("successMessage", "Hotel \"" + hotel.getName() + "\" rejected.");
        return ownerId > 0 ? "redirect:/admin/owner-detail?id=" + ownerId : "redirect:/admin/dashboard?tab=hotelOwnerAccounts";
    }

    @PostMapping("/admin/hotel/toggle-active")
    public String toggleHotelActive(
            @RequestParam("hotelId") int hotelId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found.");
            return "redirect:/admin/dashboard?tab=hotelManagementPanel";
        }

        boolean newStatus = !hotel.isActive();
        hotel.setActive(newStatus);
        hotelRepository.save(hotel);

        String action = newStatus ? "activated" : "deactivated";
        redirectAttributes.addFlashAttribute("successMessage",
                "Hotel \"" + hotel.getName() + "\" has been " + action + " successfully.");

        return "redirect:/admin/dashboard?tab=hotelManagementPanel&page=" + page;
    }

    @PostMapping("/admin/owner-hotel/set-active")
    public String setOwnerHotelActive(
            @RequestParam("hotelId") int hotelId,
            @RequestParam("ownerId") int ownerId,
            @RequestParam("active") boolean active,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found.");
            return "redirect:/admin/owner-detail?id=" + ownerId;
        }

        hotel.setActive(active);
        hotelRepository.save(hotel);

        String action = active ? "active" : "inactive";
        redirectAttributes.addFlashAttribute("successMessage",
                "Hotel \"" + hotel.getName() + "\" has been set to " + action + " successfully.");

        return "redirect:/admin/owner-detail?id=" + ownerId;
    }

    @PostMapping("/admin/feedback-reply/delete")
    public String deleteFeedbackReply(
            @RequestParam("replyId") int replyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        FeedbackReply reply = feedbackReplyRepository.findById(replyId).orElse(null);
        if (reply == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner reply not found.");
            return "redirect:/admin/dashboard?tab=ownerFeedbackPanel";
        }

        feedbackReplyRepository.delete(reply);
        redirectAttributes.addFlashAttribute("successMessage", "Deleted owner reply successfully.");
        return "redirect:/admin/dashboard?tab=ownerFeedbackPanel&page=" + page;
    }

    // =====================================================
    // PAYOUT: API cho Admin đối soát chuyển tiền cho Owner
    // =====================================================

    /**
     * API AJAX: Lấy thông tin ngân hàng hiện tại của Owner theo bookingId.
     * Admin dùng để xem STK trước khi xác nhận chuyển.
     */
    @GetMapping("/admin/payout/get-bank-info")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getBankInfoForPayout(
            @RequestParam("bookingId") int bookingId,
            @RequestParam(value = "feePercent", defaultValue = "10") double feePercent,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return org.springframework.http.ResponseEntity.status(401).body("Unauthorized");
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return org.springframework.http.ResponseEntity.status(404).body("Booking not found");
        }

        HotelOwner owner = booking.getHotel() != null ? booking.getHotel().getOwner() : null;
        if (owner == null) {
            return org.springframework.http.ResponseEntity.status(404).body("Owner not found for this booking");
        }

        BigDecimal totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal feePercentDecimal = BigDecimal.valueOf(feePercent).divide(BigDecimal.valueOf(100));
        BigDecimal platformFeeAmount = totalPrice.multiply(feePercentDecimal).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal ownerPayoutAmount = totalPrice.subtract(platformFeeAmount).setScale(2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("hotelName", booking.getHotel().getName());
        result.put("ownerName", owner.getFullName());
        result.put("bankName", owner.getBankName());
        result.put("bankAccountNumber", owner.getBankAccountNumber());
        result.put("bankAccountHolder", owner.getBankAccountHolder());
        result.put("totalPrice", totalPrice);
        result.put("platformFeePercent", feePercent);
        result.put("platformFeeAmount", platformFeeAmount);
        result.put("ownerPayoutAmount", ownerPayoutAmount);

        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * API AJAX: Admin xác nhận đã chuyển tiền cho Owner.
     * Sẽ snapshot thông tin ngân hàng Owner tại thời điểm này vào booking.
     */
    @PostMapping("/admin/payout/process")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> processPayout(
            @RequestParam("bookingId") int bookingId,
            @RequestParam(value = "feePercent", defaultValue = "10") double feePercent,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return org.springframework.http.ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                return org.springframework.http.ResponseEntity.status(404).body("Booking not found");
            }

            if (!"COMPLETED".equals(booking.getStatus())) {
                return org.springframework.http.ResponseEntity.badRequest().body("Booking must be COMPLETED to process payout");
            }

            if (booking.getPayment() == null || !"PAID".equals(booking.getPayment().getStatus())) {
                return org.springframework.http.ResponseEntity.badRequest().body("Payment must be PAID to process payout");
            }

            if ("PAID".equals(booking.getPayoutStatus())) {
                return org.springframework.http.ResponseEntity.badRequest().body("Payout already processed for this booking");
            }

            HotelOwner owner = booking.getHotel() != null ? booking.getHotel().getOwner() : null;
            if (owner == null) {
                return org.springframework.http.ResponseEntity.status(404).body("Owner not found");
            }

            if (owner.getBankAccountNumber() == null || owner.getBankAccountNumber().isBlank()) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body("Owner has not set up bank account information yet");
            }

            // Tính toán phí sàn và số tiền chuyển cho Owner
            BigDecimal totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal feePercentDecimal = BigDecimal.valueOf(feePercent).divide(BigDecimal.valueOf(100));
            BigDecimal platformFeeAmount = totalPrice.multiply(feePercentDecimal).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal ownerPayoutAmount = totalPrice.subtract(platformFeeAmount).setScale(2, java.math.RoundingMode.HALF_UP);

            // Cập nhật thông tin payout vào booking
            booking.setPlatformFeePercent(BigDecimal.valueOf(feePercent));
            booking.setPlatformFeeAmount(platformFeeAmount);
            booking.setOwnerPayoutAmount(ownerPayoutAmount);
            booking.setPayoutStatus("PAID");
            booking.setPayoutAt(LocalDateTime.now());

            // SNAPSHOT thông tin ngân hàng tại thời điểm chuyển khoản
            booking.setPayoutBankName(owner.getBankName());
            booking.setPayoutBankAccountNumber(owner.getBankAccountNumber());
            booking.setPayoutBankAccountHolder(owner.getBankAccountHolder());

            bookingRepository.save(booking);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Payout processed successfully");
            result.put("ownerPayoutAmount", ownerPayoutAmount);
            result.put("platformFeeAmount", platformFeeAmount);
            result.put("payoutBankName", owner.getBankName());
            result.put("payoutBankAccountNumber", owner.getBankAccountNumber());
            result.put("payoutBankAccountHolder", owner.getBankAccountHolder());

            return org.springframework.http.ResponseEntity.ok(result);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
