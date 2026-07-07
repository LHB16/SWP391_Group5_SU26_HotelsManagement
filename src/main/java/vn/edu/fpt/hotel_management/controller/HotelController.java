package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.HotelFacility;
import vn.edu.fpt.hotel_management.entity.HotelView;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.entity.RoomFacility;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HotelController {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelOwnerRepository hotelOwnerRepository;

    private static final String HOTEL_IMAGE_SUBDIR = "assets/images/hotel";
    private static final String HOTEL_IMAGE_URL_PREFIX = "/assets/images/hotel/";

    public HotelController(HotelRepository hotelRepository,
                           RoomRepository roomRepository,
                           HotelOwnerRepository hotelOwnerRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
    }

    private Path resolveStaticDir(String subDir) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", subDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    @GetMapping("/hotels")
    public String showHotelsPage(
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "hotelFacilities", required = false) List<String> hotelFacilities,
            @RequestParam(value = "hotelViews", required = false) List<String> hotelViews,
            @RequestParam(value = "roomFacilities", required = false) List<String> roomFacilities,
            HttpSession session,
            Model model
    ) {
        BigDecimal resolvedMinPrice = (minPrice != null) ? minPrice : BigDecimal.valueOf(100000);
        BigDecimal resolvedMaxPrice = (maxPrice != null) ? maxPrice : BigDecimal.valueOf(50000000);

        List<Hotel> hotels;
        if (rating == null || rating <= 0) {
            hotels = hotelRepository.findByPriceRange(resolvedMinPrice, resolvedMaxPrice);
        } else {
            hotels = hotelRepository.filterByRatingAndPrice(rating, resolvedMinPrice, resolvedMaxPrice);
        }

        // Lọc in-memory theo hotel facilities (AND logic)
        if (hotelFacilities != null && !hotelFacilities.isEmpty()) {
            hotels = hotels.stream().filter(h -> {
                HotelFacility fac = h.getFacility();
                if (fac == null) return false;
                for (String f : hotelFacilities) {
                    if ("parking".equals(f) && !fac.isParking()) return false;
                    if ("restaurant".equals(f) && !fac.isRestaurant()) return false;
                    if ("breakfastAvailable".equals(f) && !fac.isBreakfastAvailable()) return false;
                    if ("fitnessCentre".equals(f) && !fac.isFitnessCentre()) return false;
                    if ("nonSmokingRooms".equals(f) && !fac.isNonSmokingRooms()) return false;
                    if ("airportShuttle".equals(f) && !fac.isAirportShuttle()) return false;
                    if ("spaWellnessCentre".equals(f) && !fac.isSpaWellnessCentre()) return false;
                    if ("freeWifi".equals(f) && !fac.isFreeWifi()) return false;
                    if ("evChargingStation".equals(f) && !fac.isEvChargingStation()) return false;
                    if ("wheelchairAccessible".equals(f) && !fac.isWheelchairAccessible()) return false;
                    if ("swimmingPool".equals(f) && !fac.isSwimmingPool()) return false;
                    if ("barPub".equals(f) && !fac.isBarPub()) return false;
                    if ("rentVehicle".equals(f) && !fac.isRentVehicle()) return false;
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());
        }

        // Lọc in-memory theo hotel views (AND logic)
        if (hotelViews != null && !hotelViews.isEmpty()) {
            hotels = hotels.stream().filter(h -> {
                HotelView view = h.getView();
                if (view == null) return false;
                for (String v : hotelViews) {
                    if ("cityView".equals(v) && !view.isCityView()) return false;
                    if ("beachView".equals(v) && !view.isBeachView()) return false;
                    if ("gardenView".equals(v) && !view.isGardenView()) return false;
                    if ("poolView".equals(v) && !view.isPoolView()) return false;
                    if ("riverView".equals(v) && !view.isRiverView()) return false;
                    if ("mountainView".equals(v) && !view.isMountainView()) return false;
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());
        }

        // Lọc in-memory theo room facilities (Khách sạn có ít nhất 1 phòng thỏa mãn TẤT CẢ tiện ích phòng được chọn)
        if (roomFacilities != null && !roomFacilities.isEmpty()) {
            hotels = hotels.stream().filter(h -> {
                List<Room> rooms = roomRepository.findByHotelId(h.getId());
                if (rooms == null || rooms.isEmpty()) return false;
                for (Room r : rooms) {
                    RoomFacility rf = r.getFacility();
                    if (rf == null) continue;
                    boolean matchAll = true;
                    for (String f : roomFacilities) {
                        if ("freeToiletries".equals(f) && !rf.isFreeToiletries()) { matchAll = false; break; }
                        if ("shower".equals(f) && !rf.isShower()) { matchAll = false; break; }
                        if ("bathrobe".equals(f) && !rf.isBathrobe()) { matchAll = false; break; }
                        if ("toilet".equals(f) && !rf.isToilet()) { matchAll = false; break; }
                        if ("towels".equals(f) && !rf.isTowels()) { matchAll = false; break; }
                        if ("slippers".equals(f) && !rf.isSlippers()) { matchAll = false; break; }
                        if ("hairdryer".equals(f) && !rf.isHairdryer()) { matchAll = false; break; }
                        if ("toiletPaper".equals(f) && !rf.isToiletPaper()) { matchAll = false; break; }
                        if ("airConditioning".equals(f) && !rf.isAirConditioning()) { matchAll = false; break; }
                        if ("safetyDepositBox".equals(f) && !rf.isSafetyDepositBox()) { matchAll = false; break; }
                        if ("desk".equals(f) && !rf.isDesk()) { matchAll = false; break; }
                        if ("television".equals(f) && !rf.isTelevision()) { matchAll = false; break; }
                        if ("telephone".equals(f) && !rf.isTelephone()) { matchAll = false; break; }
                        if ("iron".equals(f) && !rf.isIron()) { matchAll = false; break; }
                        if ("electricKettle".equals(f) && !rf.isElectricKettle()) { matchAll = false; break; }
                        if ("cableChannels".equals(f) && !rf.isCableChannels()) { matchAll = false; break; }
                        if ("wakeUpService".equals(f) && !rf.isWakeUpService()) { matchAll = false; break; }
                        if ("wardrobeCloset".equals(f) && !rf.isWardrobeCloset()) { matchAll = false; break; }
                        if ("clothesRack".equals(f) && !rf.isClothesRack()) { matchAll = false; break; }
                        if ("freeBottledWater".equals(f) && rf.getFreeBottledWater() <= 0) { matchAll = false; break; }
                    }
                    if (matchAll) return true;
                }
                return false;
            }).collect(java.util.stream.Collectors.toList());
        }

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
                    BigDecimal basePrice = roomRepository.findFirstByHotelIdOrderByPriceAsc(h.getId())
                                            .map(vn.edu.fpt.hotel_management.entity.Room::getPrice)
                                            .orElse(BigDecimal.ZERO);
                    BigDecimal actualPrice = calculateHotelSubtotal(basePrice, d1, d2);
                    hotelPricesMap.put(h.getId(), actualPrice);
                }
            } catch (Exception e) {
                isFiltered = false;
                for (Hotel h : hotels) {
                    BigDecimal basePrice = roomRepository.findFirstByHotelIdOrderByPriceAsc(h.getId())
                                            .map(vn.edu.fpt.hotel_management.entity.Room::getPrice)
                                            .orElse(BigDecimal.ZERO);
                    hotelPricesMap.put(h.getId(), basePrice);
                }
            }
        } else {
            for (Hotel h : hotels) {
                BigDecimal basePrice = roomRepository.findFirstByHotelIdOrderByPriceAsc(h.getId())
                                            .map(vn.edu.fpt.hotel_management.entity.Room::getPrice)
                                            .orElse(BigDecimal.ZERO);
                hotelPricesMap.put(h.getId(), basePrice);
            }
        }

        model.addAttribute("hotels", hotels);
        model.addAttribute("hotelPricesMap", hotelPricesMap);
        model.addAttribute("nights", nights);
        model.addAttribute("isFiltered", isFiltered);
        model.addAttribute("rating", rating);
        model.addAttribute("minPrice", resolvedMinPrice);
        model.addAttribute("maxPrice", resolvedMaxPrice);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("totalResults", hotels.size());
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("today", java.time.LocalDate.now().toString());

        // Truyền ngược lại view để duy trì trạng thái tick chọn
        model.addAttribute("selectedHotelFacilities", hotelFacilities != null ? hotelFacilities : List.of());
        model.addAttribute("selectedHotelViews", hotelViews != null ? hotelViews : List.of());
        model.addAttribute("selectedRoomFacilities", roomFacilities != null ? roomFacilities : List.of());

        return "hotel/hotel-list";
    }

    @GetMapping("/hotels/new")
    public String showCreateHotelForm(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(loggedInUser.getRole()) && !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        model.addAttribute("hotel", new Hotel());
        model.addAttribute("user", loggedInUser);

        if ("ADMIN".equals(loggedInUser.getRole())) {
            model.addAttribute("owners", hotelOwnerRepository.findAll());
        }

        return "hotel/hotel-create";
    }

    @PostMapping("/hotels/new")
    public String createHotel(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "ownerId", required = false) Integer ownerId,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam("active") boolean active,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

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
                return "redirect:/hotels/new";
            }
        }

        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city);
        hotel.setDistrict(district);
        hotel.setDescription(description);
        hotel.setImageUrl(imageUrl);
        hotel.setActive(active);

        HotelOwner owner = null;

        if ("ADMIN".equals(loggedInUser.getRole()) && ownerId != null) {
            owner = hotelOwnerRepository.findById(ownerId).orElse(null);
        } else if ("HOTEL_OWNER".equals(loggedInUser.getRole())) {
            owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        }

        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner not found!");
            return "redirect:/hotels/new";
        }

        hotel.setOwner(owner);

        if ("ADMIN".equals(loggedInUser.getRole())) {
            hotel.setRating(rating != null ? rating : 0.0);
        } else {
            hotel.setRating(0.0);
        }

        hotel.setTotalReviews(0);
        hotel.setApprovalStatus("PENDING");

        try {
            hotelRepository.save(hotel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + name + "\" added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save hotel: " + e.getMessage());
            return "redirect:/hotels/new";
        }

        if ("ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/owner/hotels";
        }
    }

    @GetMapping("/owner/hotels")
    public String showOwnerHotelsPage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        List<Hotel> hotels = new ArrayList<>();

        if (owner != null) {
            hotels = hotelRepository.findByOwnerId(owner.getId());
        }

        model.addAttribute("hotels", hotels);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("hasHotels", hotels != null && !hotels.isEmpty());

        return "owner/hotel-list";
    }

    @GetMapping("/owner/hotels/new")
    public String showOwnerCreateHotelForm(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        model.addAttribute("hotel", new Hotel());
        model.addAttribute("user", loggedInUser);
        return "owner/hotel-create";
    }

    @GetMapping("/owner/hotels/{id}")
    public String showOwnerHotelDetail(@PathVariable("id") int hotelId,
                                       HttpSession session,
                                       Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        model.addAttribute("hotel", hotel);
        model.addAttribute("user", loggedInUser);
        return "owner/hotel-detail";
    }

    @GetMapping("/owner/hotels/{id}/edit")
    public String showOwnerEditHotelForm(@PathVariable("id") int hotelId,
                                         HttpSession session,
                                         Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        model.addAttribute("hotel", hotel);
        model.addAttribute("user", loggedInUser);
        return "owner/hotel-edit";
    }

    @PostMapping("/owner/hotels/{id}/edit")
    public String updateOwnerHotel(
            @PathVariable("id") int hotelId,
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam("active") boolean active,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city);
        hotel.setDistrict(district);
        hotel.setDescription(description);
        hotel.setActive(active);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFilename = imageFile.getOriginalFilename();
                String safeFilename = System.currentTimeMillis() + "_"
                        + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "hotel.jpg");
                Path uploadPath = resolveStaticDir(HOTEL_IMAGE_SUBDIR);
                Files.copy(imageFile.getInputStream(),
                        uploadPath.resolve(safeFilename),
                        StandardCopyOption.REPLACE_EXISTING);
                hotel.setImageUrl(HOTEL_IMAGE_URL_PREFIX + safeFilename);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed: " + e.getMessage());
                return "redirect:/owner/hotels/" + hotelId + "/edit";
            }
        }

        try {
            hotelRepository.save(hotel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + name + "\" updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update hotel: " + e.getMessage());
            return "redirect:/owner/hotels/" + hotelId + "/edit";
        }

        return "redirect:/owner/hotels/" + hotelId;
    }

    @PostMapping("/owner/hotels/{id}/delete")
    public String deleteOwnerHotel(@PathVariable("id") int hotelId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        try {
            String hotelName = hotel.getName();
            hotelRepository.delete(hotel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + hotelName + "\" deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete hotel: " + e.getMessage());
        }

        return "redirect:/owner/hotels";
    }

    private boolean isHolidayOrWeekend(java.time.LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        if (m == 1 && d == 1) return true;
        if (m == 4 && d == 30) return true;
        if (m == 5 && d == 1) return true;
        if (m == 9 && d == 2) return true;
        if (m == 2 && d == 14) return true;
        if (m == 3 && d == 8) return true;
        if (m == 6 && d == 1) return true;
        if (m == 10 && d == 20) return true;
        if (m == 11 && d == 20) return true;
        if (m == 12 && d == 25) return true;

        if (date.getYear() == 2025) {
            if (m == 1 && d >= 28) return true;
            if (m == 2 && d <= 3) return true;
        }
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