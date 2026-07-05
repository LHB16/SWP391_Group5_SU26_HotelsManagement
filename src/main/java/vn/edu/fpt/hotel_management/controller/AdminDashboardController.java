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
import vn.edu.fpt.hotel_management.repository.BookingRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.HotelVerificationDocumentRepository;

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

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "tab", defaultValue = "revenuePanel") String tab,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "searchType", defaultValue = "username") String searchType,
            HttpSession session, 
            Model model) {
            
        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", loggedInUser);
        model.addAttribute("tab", tab);

        // Thống kê dữ liệu doanh thu độc lập cơ sở dữ liệu
        List<Map<String, Object>> revenueData = getHotelRevenueStatistics();
        model.addAttribute("revenueData", revenueData);

        // Danh sách khách sạn phục vụ lọc doanh thu
        model.addAttribute("hotels", hotelRepository.findAll());

        // Danh sách khách hàng & chủ khách sạn kèm phân trang và tìm kiếm
        Pageable defaultCustomerPageable = PageRequest.of(page, 5, Sort.by("userAccount.username").ascending());
        Pageable defaultOwnerPageable = PageRequest.of(page, 5, Sort.by("userAccount.username").ascending());
        Pageable searchPageable = PageRequest.of(page, 5); // When searching, sorting could be by relevance or default

        Page<Customer> customerPage = Page.empty(defaultCustomerPageable);
        Page<HotelOwner> ownerPage = Page.empty(defaultOwnerPageable);

        // --- Xử lý Customer ---
        if ("customerPanel".equals(tab) && searchQuery != null && !searchQuery.trim().isEmpty()) {
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
            Pageable customerPageable = "customerPanel".equals(tab) ? defaultCustomerPageable : PageRequest.of(0, 5, Sort.by("userAccount.username").ascending());
            customerPage = customerRepository.findAll(customerPageable);
        }
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("customerCurrentPage", "customerPanel".equals(tab) ? page : 0);
        model.addAttribute("customerTotalPages", customerPage.getTotalPages());

        // --- Xử lý Owner ---
        if ("verificationPanel".equals(tab) && searchQuery != null && !searchQuery.trim().isEmpty()) {
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
            Pageable ownerPageable = "verificationPanel".equals(tab) ? defaultOwnerPageable : PageRequest.of(0, 5, Sort.by("userAccount.username").ascending());
            ownerPage = hotelOwnerRepository.findAll(ownerPageable);
        }
        model.addAttribute("owners", ownerPage.getContent());
        model.addAttribute("ownerCurrentPage", "verificationPanel".equals(tab) ? page : 0);
        model.addAttribute("ownerTotalPages", ownerPage.getTotalPages());

        // --- Lưu lại thông tin search nếu có ---
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            model.addAttribute("searchQuery", searchQuery);
            model.addAttribute("searchType", searchType);
        }

        // Danh sách đặt phòng toàn hệ thống
        // Ánh xạ sang Map để tránh Lazy Loading Exception khi chuyển sang JSON
        List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> mappedBookings = allBookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getId());
            map.put("customerId", b.getCustomer() != null ? b.getCustomer().getId() : null);
            map.put("roomType", b.getRoom() != null ? b.getRoom().getRoomType() : null);
            
            // Lấy thông tin tên khách sạn
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

    /**
     * Thống kê doanh thu độc lập Database, chạy trực tiếp trên Java
     */
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
            return "redirect:/admin/dashboard?tab=verificationPanel";
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
            return "redirect:/admin/dashboard?tab=verificationPanel";
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
            return "redirect:/admin/dashboard?tab=verificationPanel";
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
            return "redirect:/admin/dashboard?tab=verificationPanel";
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
        return ownerId > 0 ? "redirect:/admin/owner-detail?id=" + ownerId : "redirect:/admin/dashboard?tab=verificationPanel";
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
            return "redirect:/admin/dashboard?tab=verificationPanel";
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
        return ownerId > 0 ? "redirect:/admin/owner-detail?id=" + ownerId : "redirect:/admin/dashboard?tab=verificationPanel";
    }
}
