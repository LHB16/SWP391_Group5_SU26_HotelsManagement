package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Controller
public class HotelController {

    // Inject HotelRepository để truy vấn dữ liệu khách sạn
    private final HotelRepository hotelRepository;

    // Thư mục lưu ảnh khách sạn (tương đối với static resources)
    private static final String HOTEL_IMAGE_DIR = "src/main/resources/static/assets/images/hotel/";
    private static final String HOTEL_IMAGE_URL_PREFIX = "/assets/images/hotel/";

    public HotelController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    // ======================== GET /hotels – Danh sách khách sạn ========================

    @GetMapping("/hotels")
    public String showHotelsPage(
            // Lọc theo số sao: chọn nhiều giá trị cùng lúc (multi-select)
            @RequestParam(value = "ratings", required = false) List<Integer> ratings,
            // Lọc theo khoảng giá: minPrice mặc định 0 VND
            @RequestParam(value = "minPrice", required = false, defaultValue = "0") double minPrice,
            // Lọc theo khoảng giá: maxPrice mặc định 50,000,000 VND
            @RequestParam(value = "maxPrice", required = false, defaultValue = "50000000") double maxPrice,
            HttpSession session,
            Model model
    ) {
        // Lấy danh sách khách sạn theo bộ lọc từ database
        List<Hotel> hotels;
        if (ratings == null || ratings.isEmpty()) {
            hotels = hotelRepository.findByPriceRange(minPrice, maxPrice);
        } else {
            hotels = hotelRepository.filterByRatingsAndPrice(ratings, minPrice, maxPrice);
        }

        model.addAttribute("hotels", hotels);
        model.addAttribute("ratings", ratings != null ? ratings : List.of());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("totalResults", hotels.size());
        model.addAttribute("user", session.getAttribute("loggedInUser"));

        return "hotel/hotel-list";
    }

    // ======================== GET /hotels/new – Form thêm khách sạn ========================

    @GetMapping("/hotels/new")
    public String showCreateHotelForm(Model model, HttpSession session) {
        model.addAttribute("hotel", new Hotel());
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        return "hotel/hotel-create";
    }

    // ======================== POST /hotels/new – Xử lý thêm khách sạn ========================

    @PostMapping("/hotels/new")
    public String createHotel(
            @RequestParam("name")        String name,
            @RequestParam("address")     String address,
            @RequestParam("rating")      int rating,
            @RequestParam("price")       double price,
            @RequestParam("active")      boolean active,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) {
        String imageUrl = null;

        // Xử lý upload ảnh nếu có file được chọn
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Tạo tên file an toàn: bỏ khoảng trắng, ký tự đặc biệt
                String originalFilename = imageFile.getOriginalFilename();
                String safeFilename = System.currentTimeMillis() + "_"
                        + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "hotel.jpg");

                // Tạo thư mục nếu chưa tồn tại
                Path uploadPath = Paths.get(HOTEL_IMAGE_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Lưu file vào thư mục
                Path filePath = uploadPath.resolve(safeFilename);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Đường dẫn URL để lưu vào DB
                imageUrl = HOTEL_IMAGE_URL_PREFIX + safeFilename;

            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed: " + e.getMessage());
                return "redirect:/hotels/new";
            }
        }

        // Tạo và lưu entity Hotel vào database
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setRating(rating);
        hotel.setPrice(price);
        hotel.setActive(active);
        hotel.setImageUrl(imageUrl);

        hotelRepository.save(hotel);

        redirectAttributes.addFlashAttribute("successMessage", "Hotel \"" + name + "\" added successfully!");
        return "redirect:/hotels";
    }
}
