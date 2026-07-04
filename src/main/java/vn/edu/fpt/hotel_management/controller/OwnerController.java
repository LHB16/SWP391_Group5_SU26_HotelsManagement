// src/main/java/vn/edu/fpt/hotel_management/controller/OwnerController.java
package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final HotelOwnerRepository hotelOwnerRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    private static final String HOTEL_IMAGE_SUBDIR = "assets/images/hotel";
    private static final String HOTEL_IMAGE_URL_PREFIX = "/assets/images/hotel/";

    public OwnerController(HotelOwnerRepository hotelOwnerRepository,
                           HotelRepository hotelRepository,
                           RoomRepository roomRepository,
                           BookingRepository bookingRepository,
                           PaymentRepository paymentRepository) {
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    // ===================== RESOLVE STATIC DIR =====================
    private Path resolveStaticDir(String subDir) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", subDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    // ===================== DASHBOARD =====================
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "tab", defaultValue = "overview") String tab,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(user.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(user).orElse(null);
        if (owner == null) {
            return "redirect:/home";
        }

        // Lấy danh sách khách sạn của owner
        List<Hotel> hotels = hotelRepository.findByOwnerId(owner.getId());
        List<Integer> hotelIds = hotels.stream().map(Hotel::getId).collect(Collectors.toList());

        // Map số phòng cho từng hotel
        Map<Integer, Integer> roomCountMap = new HashMap<>();
        for (Integer hotelId : hotelIds) {
            List<Room> rooms = roomRepository.findByHotelId(hotelId);
            roomCountMap.put(hotelId, rooms.size());
        }

        // Thống kê cơ bản
        long totalHotels = hotels.size();
        long totalRooms = roomCountMap.values().stream().mapToInt(Integer::intValue).sum();
        long totalBookings = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // Lấy tất cả booking của các khách sạn thuộc owner
        List<Booking> allBookings = bookingRepository.findAll().stream()
                .filter(b -> hotelIds.contains(b.getHotel().getId()))
                .collect(Collectors.toList());

        totalBookings = allBookings.size();

        for (Booking b : allBookings) {
            if (b.getPayment() != null && "PAID".equals(b.getPayment().getStatus())) {
                if (b.getTotalPrice() != null) {
                    totalRevenue = totalRevenue.add(b.getTotalPrice());
                }
            }
        }

        // Recent bookings (10 booking mới nhất)
        List<Booking> recentBookings = allBookings.stream()
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());

        // Map booking sang dạng dễ hiển thị
        List<Map<String, Object>> mappedBookings = recentBookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getId());
            map.put("customerName", b.getCustomer() != null ? b.getCustomer().getFullName() : "N/A");
            map.put("hotelName", b.getHotel() != null ? b.getHotel().getName() : "N/A");
            map.put("roomType", b.getRoom() != null ? b.getRoom().getRoomType() : "N/A");
            map.put("checkInDate", b.getCheckInDate() != null ? b.getCheckInDate().toString() : null);
            map.put("checkOutDate", b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : null);
            map.put("totalPrice", b.getTotalPrice());
            map.put("bookingStatus", b.getStatus());
            map.put("paymentStatus", (b.getPayment() != null && b.getPayment().getStatus() != null)
                    ? b.getPayment().getStatus() : "PENDING");
            map.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            return map;
        }).collect(Collectors.toList());

        // Đưa dữ liệu vào model
        model.addAttribute("user", user);
        model.addAttribute("owner", owner);
        model.addAttribute("tab", tab);
        model.addAttribute("totalHotels", totalHotels);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBookings", mappedBookings);
        model.addAttribute("hotels", hotels);
        model.addAttribute("roomCountMap", roomCountMap);

        return "owner/dashboard";
    }

    // ===================== ADD HOTEL (FROM DASHBOARD MODAL) =====================
    @PostMapping("/dashboard/add-hotel")
    public String addHotelFromDashboard(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "active", defaultValue = "true") boolean active,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner profile not found!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        // Upload image
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFilename = imageFile.getOriginalFilename();
                String safeFilename = System.currentTimeMillis() + "_"
                        + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "hotel.jpg");
                Path uploadPath = resolveStaticDir(HOTEL_IMAGE_SUBDIR);
                Files.copy(imageFile.getInputStream(),
                        uploadPath.resolve(safeFilename),
                        StandardCopyOption.REPLACE_EXISTING);
                imageUrl = HOTEL_IMAGE_URL_PREFIX + safeFilename;
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed: " + e.getMessage());
                return "redirect:/owner/dashboard?tab=hotels";
            }
        }

        // Create hotel
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city);
        hotel.setDistrict(district);
        hotel.setDescription(description);
        hotel.setImageUrl(imageUrl);
        hotel.setActive(active);
        hotel.setOwner(owner);
        hotel.setRating(0.0);
        hotel.setTotalReviews(0);
        hotel.setApprovalStatus("PENDING");

        try {
            hotelRepository.save(hotel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + name + "\" added successfully! Waiting for admin approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save hotel: " + e.getMessage());
            return "redirect:/owner/dashboard?tab=hotels";
        }

        return "redirect:/owner/dashboard?tab=hotels";
    }

    // ===================== DELETE HOTEL (FROM DASHBOARD) =====================
    @PostMapping("/dashboard/delete-hotel")
    public String deleteHotelFromDashboard(
            @RequestParam("hotelId") int hotelId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner profile not found!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId()).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found or you don't have permission!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        try {
            String hotelName = hotel.getName();
            hotelRepository.delete(hotel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + hotelName + "\" deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete hotel: " + e.getMessage());
        }

        return "redirect:/owner/dashboard?tab=hotels";
    }
}