package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.BookingRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;

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

        // 1. Dữ liệu doanh thu cho biểu đồ và bảng thống kê (Đã chuyển sang dạng thuần Java / DB-Independent)
        List<Map<String, Object>> revenueData = getHotelRevenueStatistics();
        model.addAttribute("revenueData", revenueData);

        // 2. Danh sách khách sạn cho bộ lọc doanh thu
        model.addAttribute("hotels", hotelRepository.findAll());

        // 3. Danh sách khách hàng (Role: CUSTOMER) có phân trang và tìm kiếm dùng Magic Methods
        Pageable pageable = PageRequest.of(page, 10);
        Page<User> customerPage;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.trim();
            if ("id".equals(searchType)) {
                try {
                    int targetId = Integer.parseInt(query);
                    customerPage = userRepository.findByRoleAndId("CUSTOMER", targetId, pageable);
                } catch (NumberFormatException e) {
                    customerPage = Page.empty(pageable);
                }
            } else if ("fullName".equals(searchType)) {
                customerPage = userRepository.findByRoleAndFullNameContainingIgnoreCase("CUSTOMER", query, pageable);
            } else if ("email".equals(searchType)) {
                customerPage = userRepository.findByRoleAndEmailContainingIgnoreCase("CUSTOMER", query, pageable);
            } else {
                customerPage = userRepository.findByRoleAndUsernameContainingIgnoreCase("CUSTOMER", query, pageable);
            }
            model.addAttribute("searchQuery", searchQuery);
            model.addAttribute("searchType", searchType);
        } else {
            customerPage = userRepository.findByRole("CUSTOMER", pageable);
        }
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customerPage.getTotalPages());

        // 4. Danh sách hóa đơn đặt phòng của toàn bộ hệ thống (dùng cho JS lọc doanh thu)
        // Ánh xạ sang Map đơn giản để tránh Lazy Loading Exception khi chuyển sang JSON bằng Thymeleaf
        List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> mappedBookings = allBookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getId());
            map.put("customerId", b.getCustomer() != null ? b.getCustomer().getId() : null);
            map.put("roomType", b.getRoom() != null ? b.getRoom().getRoomType() : null);
            
            // Lấy tên khách sạn
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

        // Trả về templates/admin/dashboard.html
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
        User selectedCustomer = userRepository.findById(customerId).orElse(null);
        if (selectedCustomer == null || !"CUSTOMER".equals(selectedCustomer.getRole())) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("selectedCustomer", selectedCustomer);

        // Lấy danh sách đặt phòng có phân trang (10 items/trang)
        Pageable pageable = PageRequest.of(page, 10);
        Page<Booking> bookingPage = bookingRepository.findByCustomerId(customerId, pageable);

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

    /**
     * Thống kê doanh thu độc lập Database, chạy trực tiếp trên Java
     */
    private List<Map<String, Object>> getHotelRevenueStatistics() {
        List<Booking> bookings = bookingRepository.findAll();
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, Hotel> hotelMap = hotels.stream().collect(Collectors.toMap(Hotel::getId, h -> h));

        // Lọc theo trạng thái hợp lệ
        List<Booking> filtered = bookings.stream()
                .filter(b -> b.getStatus() != null && 
                        (b.getStatus().equals("CONFIRMED") || b.getStatus().equals("CHECKED_IN") || b.getStatus().equals("CHECKED_OUT")))
                .collect(Collectors.toList());

        // Gom nhóm theo hotelId, year, month
        Map<String, Map<String, Object>> statsMap = new HashMap<>();

        for (Booking b : filtered) {
            if (b.getRoom() == null) continue;
            int hotelId = b.getRoom().getHotelId();
            Hotel hotel = hotelMap.get(hotelId);
            String hotelName = hotel != null ? hotel.getName() : "Unknown";

            LocalDateTime dateTime = b.getCreatedAt();
            if (dateTime == null) {
                dateTime = LocalDateTime.now(); // Fallback
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
        
        // Sắp xếp: year DESC, month DESC, hotelName ASC
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
}
