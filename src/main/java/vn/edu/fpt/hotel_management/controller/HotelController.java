package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
public class HotelController {

    // Inject HotelRepository và RoomRepository để truy vấn dữ liệu
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelOwnerRepository hotelOwnerRepository;

    // Thư mục con trong static resources để lưu ảnh khách sạn
    private static final String HOTEL_IMAGE_SUBDIR = "assets/images/hotel";
    private static final String HOTEL_IMAGE_URL_PREFIX = "/assets/images/hotel/";

    public HotelController(HotelRepository hotelRepository, RoomRepository roomRepository, HotelOwnerRepository hotelOwnerRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
    }

    /**
     * Tính đường dẫn tuyệt đối đến thư mục static resources khi runtime.
     * Khi chạy từ IDE: user.dir = project root (ví dụ: e:\FPT\...\SWP391_...)
     */
    private Path resolveStaticDir(String subDir) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"),
                              "src", "main", "resources", "static", subDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    // ======================== GET /hotels – Danh sách khách sạn ========================

    @GetMapping("/hotels")
    public String showHotelsPage(
            // Lọc theo số sao đơn lẻ
            @RequestParam(value = "rating", required = false) Double rating,
            // Lọc theo khoảng giá: minPrice mặc định 0 VND
            @RequestParam(value = "minPrice", required = false, defaultValue = "0") BigDecimal minPrice,
            // Lọc theo khoảng giá: maxPrice mặc định 50,000,000 VND
            @RequestParam(value = "maxPrice", required = false, defaultValue = "50000000") BigDecimal maxPrice,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            HttpSession session,
            Model model
    ) {
        // Lấy danh sách khách sạn theo bộ lọc từ database
        List<Hotel> hotels;
        if (rating == null || rating <= 0) {
            hotels = hotelRepository.findByPriceRange(minPrice, maxPrice);
        } else {
            hotels = hotelRepository.filterByRatingAndPrice(rating, minPrice, maxPrice);
        }

        // Tính toán số đêm và giá thực tế của từng khách sạn
        long nights = 1;
        boolean isFiltered = false;
        java.util.Map<Integer, BigDecimal> hotelPricesMap = new java.util.HashMap<>();
        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                java.time.LocalDate d1 = java.time.LocalDate.parse(checkin.trim());
                java.time.LocalDate d2 = java.time.LocalDate.parse(checkout.trim());
                if (d2.isAfter(d1)) {
                    nights = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
                    isFiltered = true;
                } else {
                    d2 = d1.plusDays(1);
                    checkout = d2.toString();
                    nights = 1;
                    isFiltered = true;
                }
                for (Hotel h : hotels) {
                    BigDecimal basePrice = roomRepository.findMinPriceByHotelId(h.getId());
                    if (basePrice == null) basePrice = BigDecimal.ZERO;
                    BigDecimal actualPrice = calculateHotelSubtotal(basePrice, d1, d2);
                    hotelPricesMap.put(h.getId(), actualPrice);
                }
            } catch (Exception e) {
                isFiltered = false;
                for (Hotel h : hotels) {
                    BigDecimal basePrice = roomRepository.findMinPriceByHotelId(h.getId());
                    hotelPricesMap.put(h.getId(), basePrice != null ? basePrice : BigDecimal.ZERO);
                }
            }
        } else {
            for (Hotel h : hotels) {
                BigDecimal basePrice = roomRepository.findMinPriceByHotelId(h.getId());
                hotelPricesMap.put(h.getId(), basePrice != null ? basePrice : BigDecimal.ZERO);
            }
        }

        model.addAttribute("hotels", hotels);
        model.addAttribute("hotelPricesMap", hotelPricesMap);
        model.addAttribute("nights", nights);
        model.addAttribute("isFiltered", isFiltered);
        model.addAttribute("rating", rating);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("totalResults", hotels.size());
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("today", java.time.LocalDate.now().toString());

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
            @RequestParam("rating")      double rating,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam("active")      boolean active,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            HttpSession session,
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

                // Lấy absolute path đến thư mục lưu ảnh
                Path uploadPath = resolveStaticDir(HOTEL_IMAGE_SUBDIR);

                // Lưu file vào thư mục
                Files.copy(imageFile.getInputStream(),
                           uploadPath.resolve(safeFilename),
                           StandardCopyOption.REPLACE_EXISTING);

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
        hotel.setActive(active);
        hotel.setImageUrl(imageUrl);

        // Gán owner_id bắt buộc (SQL NOT NULL constraint)
        try {
            User loggedInUser = (User) session.getAttribute("loggedInUser");
            if (loggedInUser != null) {
                // Thử tìm HotelOwner ứng với User này
                hotelOwnerRepository.findByUserAccountId(loggedInUser.getId()).ifPresent(hotel::setOwner);
            }
        } catch (Exception e) {
            // Bỏ qua lỗi cast class nếu session bị cũ
        }
        
        // Nếu không có owner do lỗi hoặc Admin đang tạo, tạm thời lấy một owner đầu tiên hoặc báo lỗi
        if (hotel.getOwner() == null) {
             hotelOwnerRepository.findAll().stream().findFirst().ifPresent(hotel::setOwner);
        }

        try {
            hotelRepository.save(hotel);
            redirectAttributes.addFlashAttribute("successMessage", "Hotel \"" + name + "\" added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save hotel: " + e.getMessage());
            return "redirect:/hotels/new";
        }

        return "redirect:/hotels";
    }

    private boolean isHolidayOrWeekend(java.time.LocalDate date) {
        // 1. Kiểm tra cuối tuần (Thứ 7 & Chủ Nhật)
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        // 2. Kiểm tra ngày lễ
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        // Lễ dương lịch VN cố định
        if (m == 1 && d == 1) return true;   // Tết Dương Lịch
        if (m == 4 && d == 30) return true;  // Giải phóng Miền Nam
        if (m == 5 && d == 1) return true;   // Quốc tế Lao động
        if (m == 9 && d == 2) return true;   // Quốc khánh

        // Các ngày lễ đặc biệt yêu cầu thêm
        if (m == 2 && d == 14) return true;  // Valentine
        if (m == 3 && d == 8) return true;   // Quốc tế Phụ nữ
        if (m == 6 && d == 1) return true;   // Quốc tế Thiếu nhi
        if (m == 10 && d == 20) return true; // Phụ nữ VN
        if (m == 11 && d == 20) return true; // Nhà giáo VN
        if (m == 12 && d == 25) return true; // Giáng sinh

        // Tết Âm Lịch năm 2025 (Từ 28/01 đến 03/02/2025)
        if (date.getYear() == 2025) {
            if (m == 1 && d >= 28) return true;
            if (m == 2 && d <= 3) return true;
        }
        // Tết Âm Lịch năm 2026 (Từ 16/02 đến 22/02/2026)
        if (date.getYear() == 2026) {
            if (m == 2 && d >= 16 && d <= 22) return true;
        }

        return false;
    }

    private BigDecimal calculateHotelSubtotal(BigDecimal basePrice, java.time.LocalDate checkin, java.time.LocalDate checkout) {
        BigDecimal total = BigDecimal.ZERO;
        java.time.LocalDate temp = checkin;
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
