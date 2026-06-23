package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class HotelController {

    // Inject HotelRepository để truy vấn dữ liệu khách sạn
    private final HotelRepository hotelRepository;

    public HotelController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @GetMapping("/hotels")
    public String showHotelsPage(
            // Lọc theo số sao: chọn nhiều giá trị cùng lúc (multi-select)
            // URL ví dụ: /hotels?ratings=3&ratings=4&ratings=5
            @RequestParam(value = "ratings", required = false) List<Integer> ratings,
            // Lọc theo khoảng giá: minPrice mặc định 0 VND
            @RequestParam(value = "minPrice", required = false, defaultValue = "0") BigDecimal minPrice,
            // Lọc theo khoảng giá: maxPrice mặc định 50,000,000 VND
            @RequestParam(value = "maxPrice", required = false, defaultValue = "50000000") BigDecimal maxPrice,
            HttpSession session,
            Model model
    ) {
        // Lấy danh sách khách sạn theo bộ lọc từ database
        List<Hotel> hotels;
        if (ratings == null || ratings.isEmpty()) {
            // Chọn "All" hoặc không chọn sao nào → lấy tất cả theo khoảng giá
            hotels = hotelRepository.findByPriceRange(minPrice, maxPrice);
        } else {
            // Chọn 1 hoặc nhiều mức sao → lọc theo danh sách sao + khoảng giá
            hotels = hotelRepository.filterByRatingsAndPrice(ratings, minPrice, maxPrice);
        }

        // Gửi danh sách khách sạn sang giao diện
        model.addAttribute("hotels", hotels);

        // Gửi các giá trị filter hiện tại để giữ trạng thái trên UI
        // Nếu ratings null thì gán danh sách rỗng để Thymeleaf không bị lỗi
        model.addAttribute("ratings", ratings != null ? ratings : List.of());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // Tổng số kết quả để hiển thị trên UI
        model.addAttribute("totalResults", hotels.size());

        // Gửi thông tin user đăng nhập (nếu có) để navbar hiển thị đúng
        model.addAttribute("user", session.getAttribute("loggedInUser"));

        // Trả về template hotel/hotel-list.html
        return "hotel/hotel-list";
    }
}
