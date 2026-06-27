package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import java.util.List;
import java.util.Map;

@Controller
public class AdminDashboardController {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(
            @RequestParam(value = "customerid", required = false) Integer customerId,
            HttpSession session, 
            Model model) {
            
        // Kiểm tra phân quyền admin
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", loggedInUser);

        // 1. Dữ liệu doanh thu cho biểu đồ và bảng thống kê
        List<Map<String, Object>> revenueData = hotelRepository.getHotelRevenueStatistics();
        model.addAttribute("revenueData", revenueData);

        // 2. Danh sách khách sạn cho bộ lọc doanh thu
        model.addAttribute("hotels", hotelRepository.findAll());

        // 3. Danh sách khách hàng (Role: CUSTOMER)
        List<User> customers = userRepository.findByRole("CUSTOMER");
        model.addAttribute("customers", customers);

        // 4. Danh sách hóa đơn đặt phòng của toàn bộ hệ thống (dùng cho JS lọc doanh thu)
        List<Map<String, Object>> bookings = hotelRepository.getAllCustomerBookings();
        model.addAttribute("bookings", bookings);

        // 5. Nếu có tham số customerid truyền lên (đã nhấp chọn xem chi tiết Customer)
        if (customerId != null) {
            User selectedCustomer = userRepository.findById(customerId).orElse(null);
            if (selectedCustomer != null && "CUSTOMER".equals(selectedCustomer.getRole())) {
                model.addAttribute("selectedCustomer", selectedCustomer);
                
                List<Map<String, Object>> customerBookings = hotelRepository.getBookingsByCustomerId(customerId);
                model.addAttribute("customerBookings", customerBookings);
            }
        }

        // Trả về templates/admin/dashboard.html
        return "admin/dashboard";
    }
}
