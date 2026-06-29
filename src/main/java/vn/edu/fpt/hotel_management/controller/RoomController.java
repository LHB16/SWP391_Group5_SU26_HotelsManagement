package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.entity.Review;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.ReviewRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;

@Controller
public class RoomController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;

    // Lấy đường dẫn thư mục static từ classpath (absolute khi runtime)
    // Ảnh phòng lưu trong: {project}/src/main/resources/static/assets/images/room/
    private static final String ROOM_IMAGE_SUBDIR = "assets/images/room";

    public RoomController(RoomRepository roomRepository, HotelRepository hotelRepository, ReviewRepository reviewRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Tính đường dẫn tuyệt đối đến thư mục lưu ảnh trong static resources.
     * Dùng user.dir (working directory khi chạy IDE = project root).
     */
    private Path resolveStaticDir(String subDir) throws IOException {
        // Khi chạy từ IDE: working dir = project root (e.g. e:\FPT\...\SWP391_...)
        Path path = Paths.get(System.getProperty("user.dir"),
                              "src", "main", "resources", "static", subDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    // ======================== GET /hotels/{id}/rooms ========================

    @GetMapping("/hotels/{id}/rooms")
    public String showRoomsPage(
            @PathVariable("id") int id,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "minPrice", required = false, defaultValue = "0") long minPrice,
            @RequestParam(value = "maxPrice", required = false, defaultValue = "50000000") long maxPrice,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            HttpSession session,
            Model model
    ) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        List<Room> rooms;
        if (types == null || types.isEmpty()) {
            rooms = roomRepository.findByHotelIdAndPriceRange(id, BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice));
        } else {
            rooms = roomRepository.filterByHotelAndTypesAndPrice(id, types, BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice));
        }

        List<String> allTypes = roomRepository.findDistinctTypesByHotelId(id);

        // Tải danh sách đánh giá và tính toán số sao trung bình của khách sạn
        List<Review> reviews = reviewRepository.findByHotelIdOrderByCreatedAtDesc(id);
        double avgRating = 0.0;
        if (!reviews.isEmpty()) {
            double sum = 0;
            for (Review r : reviews) {
                sum += r.getRating();
            }
            avgRating = sum / reviews.size();
        }
        avgRating = Math.round(avgRating * 10.0) / 10.0;

        boolean hasReviewed = false;
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            hasReviewed = reviewRepository.existsByHotelIdAndUserId(id, loggedInUser.getId());
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", rooms);
        model.addAttribute("allTypes", allTypes);
        model.addAttribute("selectedTypes", types != null ? types : List.of());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("totalResults", rooms.size());
        model.addAttribute("user", loggedInUser);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", reviews.size());
        model.addAttribute("hasReviewed", hasReviewed);

        return "hotel/rooms";
    }

    // ======================== GET /hotels/{id}/rooms/new ========================

    @GetMapping("/hotels/{id}/rooms/new")
    public String showCreateRoomForm(
            @PathVariable("id") int id,
            HttpSession session,
            Model model
    ) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        model.addAttribute("hotel", hotel);
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        return "hotel/room-create";
    }

    // ======================== POST /hotels/{id}/rooms/new ========================

    @PostMapping("/hotels/{id}/rooms/new")
    public String createRoom(
            @PathVariable("id") int id,
            @RequestParam("type")                               String type,
            @RequestParam("price")                              long   price,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam(value = "window",  defaultValue = "0") int    window,
            @RequestParam(value = "bed",     defaultValue = "0") int    bed,
            @RequestParam("acreage")                            double acreage,
            @RequestParam("person")                             int    person,
            @RequestParam(value = "facilities", defaultValue = "") String facilities,
            @RequestParam(value = "bathroomAmenities", defaultValue = "") String bathroomAmenities,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) {
        // Kiểm tra khách sạn tồn tại
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        String imgUrl = null;

        // Xử lý upload ảnh nếu có file
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Tạo tên file an toàn, tránh trùng
                String original = imageFile.getOriginalFilename();
                String safeName = System.currentTimeMillis() + "_"
                        + (original != null
                            ? original.replaceAll("[^a-zA-Z0-9._-]", "_")
                            : "room.jpg");

                // Lấy đường dẫn tuyệt đối thư mục lưu ảnh
                Path uploadDir = resolveStaticDir(ROOM_IMAGE_SUBDIR);
                Path filePath  = uploadDir.resolve(safeName);

                // Ghi file
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // URL để lưu vào DB (trỏ đến /assets/images/room/...)
                imgUrl = "/assets/images/room/" + safeName;

            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Image upload failed: " + e.getMessage());
                return "redirect:/hotels/" + id + "/rooms/new";
            }
        }

        // Tạo và lưu Room vào DB
        Room room = new Room();
        room.setHotelId(id);
        room.setRoomType(type.trim());     // field: roomType
        room.setPrice(BigDecimal.valueOf(price));
        room.setDescription(description.trim());
        room.setNumWindow(window);         // field: numWindow
        room.setBed(bed);
        room.setAcreage(acreage);
        room.setPerson(person);
        room.setImgUrl(imgUrl);
        room.setFacilities(facilities.trim());
        room.setBathroomAmenities(bathroomAmenities.trim());

        roomRepository.save(room);

        redirectAttributes.addFlashAttribute("successMessage",
                "Room \"" + type + "\" added successfully!");
        return "redirect:/hotels/" + id + "/rooms";
    }
}
