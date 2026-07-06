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
import vn.edu.fpt.hotel_management.repository.WishlistRepository;
import vn.edu.fpt.hotel_management.entity.Wishlist;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;

import vn.edu.fpt.hotel_management.repository.BookingRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;

@Controller
public class RoomController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    // Lấy đường dẫn thư mục static từ classpath (absolute khi runtime)
    // Ảnh phòng lưu trong: {project}/src/main/resources/static/assets/images/room/
    private static final String ROOM_IMAGE_SUBDIR = "assets/images/room";

    public RoomController(RoomRepository roomRepository,
                          HotelRepository hotelRepository,
                          ReviewRepository reviewRepository,
                          WishlistRepository wishlistRepository,
                          CustomerRepository customerRepository,
                          BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.reviewRepository = reviewRepository;
        this.wishlistRepository = wishlistRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
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
            @RequestParam(value = "bookingId", required = false) Integer bookingId,
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

        // Tải danh sách đánh giá (sắp xếp đánh giá tốt lên trước, rồi mới tới ngày mới nhất)
        List<Review> reviews = reviewRepository.findByHotelIdOrderByRatingDescCreatedAtDesc(id);
        double avgRating = 0.0;
        if (!reviews.isEmpty()) {
            double sum = 0;
            for (Review r : reviews) {
                sum += r.getRating();
            }
            avgRating = sum / reviews.size();
        }
        avgRating = Math.round(avgRating * 10.0) / 10.0;

        java.util.Map<Integer, Booking> reviewBookings = new java.util.HashMap<>();
        for (Review r : reviews) {
            if (r.getCustomer() != null) {
                List<Booking> bkList = bookingRepository.findBookingsByCustomerAndHotel(
                    r.getCustomer().getId(),
                    id,
                    List.of("COMPLETED")
                );
                Booking match = null;
                if (r.getRoom() != null) {
                    match = bkList.stream()
                        .filter(b -> b.getRoom().getId() == r.getRoom().getId())
                        .findFirst()
                        .orElse(bkList.isEmpty() ? null : bkList.get(0));
                } else if (!bkList.isEmpty()) {
                    match = bkList.get(0);
                }
                if (match != null) {
                    reviewBookings.put(r.getId(), match);
                }
            }
        }

        boolean hasReviewed = false;
        Integer currentCustomerId = null;
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        java.util.Set<Integer> wishlistRoomIds = new java.util.HashSet<>();
        List<Booking> customerBookings = List.of();
        Room latestBookedRoom = null;
        boolean hasBooked = false;
        Integer resolvedBookingId = null;

        if (loggedInUser != null) {
            Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
            if (customer != null) {
                currentCustomerId = customer.getId();
                List<Wishlist> userWishlist = wishlistRepository.findByCustomerIdOrderByAddedAtDesc(customer.getId());
                for (Wishlist wl : userWishlist) {
                    wishlistRoomIds.add(wl.getRoom().getId());
                }
                
                customerBookings = bookingRepository.findBookingsByCustomerAndHotel(
                    customer.getId(),
                    id,
                    List.of("COMPLETED")
                );
                
                List<Review> customerReviews = reviewRepository.findByHotelIdAndCustomerId(id, customer.getId());
                
                if (bookingId != null) {
                    Booking specifiedBooking = bookingRepository.findById(bookingId).orElse(null);
                    if (specifiedBooking != null && specifiedBooking.getCustomer().getId() == customer.getId() 
                            && specifiedBooking.getHotel().getId() == id 
                            && "COMPLETED".equals(specifiedBooking.getStatus())) {
                        
                        boolean alreadyReviewed = customerReviews.stream()
                                .anyMatch(r -> r.getBooking() != null && r.getBooking().getId() == bookingId);
                        
                        if (!alreadyReviewed) {
                            resolvedBookingId = bookingId;
                            latestBookedRoom = specifiedBooking.getRoom();
                            hasBooked = true;
                            hasReviewed = false;
                        }
                    }
                }
                
                if (resolvedBookingId == null) {
                    java.util.Map<Integer, Long> roomBookingCounts = customerBookings.stream()
                        .collect(java.util.stream.Collectors.groupingBy(b -> b.getRoom().getId(), java.util.stream.Collectors.counting()));
                        
                    java.util.Map<Integer, Long> roomReviewCounts = customerReviews.stream()
                        .collect(java.util.stream.Collectors.groupingBy(r -> r.getRoom().getId(), java.util.stream.Collectors.counting()));
                    
                    Booking unreviewedBooking = null;
                    for (Booking b : customerBookings) {
                        int roomId = b.getRoom().getId();
                        long booked = roomBookingCounts.getOrDefault(roomId, 0L);
                        long reviewed = roomReviewCounts.getOrDefault(roomId, 0L);
                        if (reviewed < booked) {
                            unreviewedBooking = b;
                            break;
                        }
                    }
                    
                    hasBooked = (unreviewedBooking != null);
                    if (hasBooked) {
                        resolvedBookingId = unreviewedBooking.getId();
                        latestBookedRoom = unreviewedBooking.getRoom();
                        hasReviewed = false;
                    } else {
                        latestBookedRoom = null;
                        hasReviewed = !customerBookings.isEmpty();
                    }
                }
            }
        }
        model.addAttribute("resolvedBookingId", resolvedBookingId);

        // Tính toán số đêm và giá thực tế của từng phòng
        long nights = 1;
        boolean isFiltered = false;
        java.util.Map<Integer, BigDecimal> roomPricesMap = new java.util.HashMap<>();
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
                for (Room r : rooms) {
                    BigDecimal actualPrice = calculateRoomSubtotal(r.getPrice(), d1, d2);
                    roomPricesMap.put(r.getId(), actualPrice);
                }
            } catch (Exception e) {
                isFiltered = false;
                for (Room r : rooms) {
                    roomPricesMap.put(r.getId(), r.getPrice());
                }
            }
        } else {
            for (Room r : rooms) {
                roomPricesMap.put(r.getId(), r.getPrice());
            }
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomPricesMap", roomPricesMap);
        model.addAttribute("wishlistRoomIds", wishlistRoomIds);
        model.addAttribute("nights", nights);
        model.addAttribute("isFiltered", isFiltered);
        model.addAttribute("allTypes", allTypes);
        model.addAttribute("selectedTypes", types != null ? types : List.of());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("totalResults", rooms.size());
        model.addAttribute("user", loggedInUser);
        model.addAttribute("currentCustomerId", currentCustomerId);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", reviews.size());
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("latestBookedRoom", latestBookedRoom);
        model.addAttribute("hasBooked", hasBooked);
        model.addAttribute("reviewBookings", reviewBookings);
        model.addAttribute("today", java.time.LocalDate.now().toString());

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

        roomRepository.save(room);

        redirectAttributes.addFlashAttribute("successMessage",
                "Room \"" + type + "\" added successfully!");
        return "redirect:/hotels/" + id + "/rooms";
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

    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, java.time.LocalDate checkin, java.time.LocalDate checkout) {
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
