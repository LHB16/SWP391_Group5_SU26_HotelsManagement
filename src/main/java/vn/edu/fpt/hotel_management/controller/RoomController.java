package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import vn.edu.fpt.hotel_management.service.OwnerService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RoomController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final CustomerRepository customerRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final RoomFacilityRepository roomFacilityRepository;
    private final OwnerService ownerService;
    private final BookingRepository bookingRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;

    private static final String ROOM_IMAGE_SUBDIR = "assets/images/room";

    public RoomController(RoomRepository roomRepository,
                          HotelRepository hotelRepository,
                          ReviewRepository reviewRepository,
                          WishlistRepository wishlistRepository,
                          CustomerRepository customerRepository,
                          HotelOwnerRepository hotelOwnerRepository,
                          RoomFacilityRepository roomFacilityRepository,
                          OwnerService ownerService,
                          BookingRepository bookingRepository,
                          FeedbackReplyRepository feedbackReplyRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.reviewRepository = reviewRepository;
        this.wishlistRepository = wishlistRepository;
        this.customerRepository = customerRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.roomFacilityRepository = roomFacilityRepository;
        this.ownerService = ownerService;
        this.bookingRepository = bookingRepository;
        this.feedbackReplyRepository = feedbackReplyRepository;
    }

    // ===== RESOLVE STATIC DIR =====
    private Path resolveStaticDir(String subDir) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", subDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    // ===================== GET /hotels/{id}/rooms =====================
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
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        
        // Nếu tham số rỗng, đọc lại từ session trước khi fallback
        if (checkin == null || checkin.trim().isEmpty()) {
            checkin = (String) session.getAttribute("hotelCheckinFilter");
        }
        if (checkout == null || checkout.trim().isEmpty()) {
            checkout = (String) session.getAttribute("hotelCheckoutFilter");
        }

        // Fallback về today / tomorrow nếu session cũng không có
        if (checkin == null || checkin.trim().isEmpty()) {
            checkin = java.time.LocalDate.now().toString();
        }
        if (checkout == null || checkout.trim().isEmpty()) {
            checkout = java.time.LocalDate.now().plusDays(1).toString();
        }
        String minCheckout = java.time.LocalDate.parse(checkin).plusDays(1).toString();

        if (hotel == null || !hotel.isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
            return "redirect:/hotels";
        }

        Integer defaultRoomId = null;
        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null && booking.getRoom() != null) {
                defaultRoomId = booking.getRoom().getId();
            }
        }

        List<Room> rooms;
        if (types == null || types.isEmpty()) {
            rooms = roomRepository.findByHotelIdAndPriceBetween(id, BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice));
        } else {
            rooms = roomRepository.findByHotelIdAndTypeInAndPriceBetween(id, types, BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice));
        }

        List<String> allTypes = roomRepository.findDistinctTypesByHotelId(id);

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
        if (loggedInUser != null) {
            Customer customer = customerRepository.findByUserAccount(loggedInUser).orElse(null);
            if (customer != null) {
                currentCustomerId = customer.getId();
                hasReviewed = reviewRepository.existsByHotelIdAndCustomerId(id, customer.getId());
                List<Wishlist> userWishlist = wishlistRepository.findByCustomerIdOrderByAddedAtDesc(customer.getId());
                for (Wishlist wl : userWishlist) {
                    wishlistRoomIds.add(wl.getRoom().getId());
                }
            }
        }

        long nights = 1;
        boolean isFiltered = false;
        java.util.Map<Integer, BigDecimal> roomPricesMap = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> availableRoomsMap = new java.util.HashMap<>();

        java.time.LocalDate d1 = java.time.LocalDate.now();
        java.time.LocalDate d2 = java.time.LocalDate.now().plusDays(1);
        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                d1 = java.time.LocalDate.parse(checkin.trim());
                d2 = java.time.LocalDate.parse(checkout.trim());
                if (d2.isAfter(d1)) {
                    nights = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
                    isFiltered = true;
                } else {
                    d2 = d1.plusDays(1);
                    checkout = d2.toString();
                    nights = 1;
                    isFiltered = true;
                }
            } catch (Exception e) {
                isFiltered = false;
                d1 = java.time.LocalDate.now();
                d2 = java.time.LocalDate.now().plusDays(1);
            }
        }

        for (Room r : rooms) {
            BigDecimal actualPrice = isFiltered ? calculateRoomSubtotal(r.getPrice(), d1, d2) : r.getPrice();
            roomPricesMap.put(r.getId(), actualPrice);

            long bookedCount = bookingRepository.countByRoomIdAndStatusAndCheckInDateBeforeAndCheckOutDateAfter(
                    r.getId(),
                    "CONFIRMED",
                    d2,
                    d1
            );
            int available = r.getNumberRooms() - (int) bookedCount;
            if (available < 0) {
                available = 0;
            }
            availableRoomsMap.put(r.getId(), available);
        }

        boolean isHotelOwner = false;
        if (loggedInUser != null && "HOTEL_OWNER".equals(loggedInUser.getRole())) {
            HotelOwner currentOwner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
            if (currentOwner != null && hotel.getOwner() != null && hotel.getOwner().getId() == currentOwner.getId()) {
                isHotelOwner = true;
            }
        }

        List<FeedbackReply> replies = feedbackReplyRepository.findByHotelId(id);
        Map<Integer, FeedbackReply> repliesMap = new HashMap<>();
        for (FeedbackReply r : replies) {
            if (r.getFeedback() != null) {
                repliesMap.put(r.getFeedback().getId(), r);
            }
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomPricesMap", roomPricesMap);
        model.addAttribute("availableRoomsMap", availableRoomsMap);
        model.addAttribute("wishlistRoomIds", wishlistRoomIds);
        model.addAttribute("nights", nights);
        model.addAttribute("isFiltered", isFiltered);
        model.addAttribute("allTypes", allTypes);
        model.addAttribute("selectedTypes", types != null ? types : List.of());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("minCheckout", minCheckout);
        model.addAttribute("totalResults", rooms.size());
        model.addAttribute("user", loggedInUser);
        model.addAttribute("currentCustomerId", currentCustomerId);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", reviews.size());
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("isHotelOwner", isHotelOwner);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("defaultRoomId", defaultRoomId);

        return "hotel/room-list";
    }

    // ===================== GET /hotels/{id}/rooms/{roomId} =====================
    @GetMapping("/hotels/{id}/rooms/{roomId}")
    public String showRoomDetailPage(
            @PathVariable("id") int id,
            @PathVariable("roomId") int roomId,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (checkin == null || checkin.trim().isEmpty()) {
            checkin = java.time.LocalDate.now().toString();
        }
        if (checkout == null || checkout.trim().isEmpty()) {
            checkout = java.time.LocalDate.now().plusDays(1).toString();
        }
        String minCheckout = java.time.LocalDate.parse(checkin).plusDays(1).toString();
        if (hotel == null || !hotel.isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This hotel is currently inactive.");
            return "redirect:/hotels";
        }

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getHotelId() != id) {
            return "redirect:/hotels/" + id + "/rooms";
        }

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        long nights = 1;
        boolean isFiltered = false;
        BigDecimal roomPrice = room.getPrice();

        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                java.time.LocalDate d1 = java.time.LocalDate.parse(checkin.trim());
                java.time.LocalDate d2 = java.time.LocalDate.parse(checkout.trim());
                if (d2.isAfter(d1)) {
                    nights = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
                    isFiltered = true;
                    roomPrice = calculateRoomSubtotal(room.getPrice(), d1, d2);
                }
            } catch (Exception e) {
                isFiltered = false;
            }
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("roomPrice", roomPrice);
        model.addAttribute("nights", nights);
        model.addAttribute("isFiltered", isFiltered);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("minCheckout", minCheckout);
        model.addAttribute("user", loggedInUser);

        if (loggedInUser != null && "HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "owner/room-detail";
        }
        return "hotel/room-detail";
    }

    // ===================== GET /hotels/{id}/rooms/new =====================
    @GetMapping("/hotels/{id}/rooms/new")
    public String showCreateRoomForm(
            @PathVariable("id") int id,
            HttpSession session,
            Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        // Kiểm tra Owner đã được duyệt
        if (!ownerService.isOwnerApproved(loggedInUser)) {
            session.setAttribute("errorMessage",
                    "Your account is pending admin approval. You cannot add rooms yet.");
            return "redirect:/owner/dashboard";
        }

        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || hotel.getOwner().getId() != owner.getId()) {
            return "redirect:/home";
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("user", loggedInUser);
        return "owner/room-create";
    }

    // ===================== POST /hotels/{id}/rooms/new =====================
    @PostMapping("/hotels/{id}/rooms/new")
    public String createRoom(
            @PathVariable("id") int id,
            @RequestParam("type") String type,
            @RequestParam("price") long price,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam(value = "window", defaultValue = "0") int window,
            @RequestParam(value = "bed", defaultValue = "0") int bed,
            @RequestParam("acreage") double acreage,
            @RequestParam("person") int person,
            @RequestParam(value = "numberRooms", defaultValue = "1") int numberRooms,
            @RequestParam(value = "roomStatus", required = false) Boolean roomStatus,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "freeToiletries", required = false) Boolean freeToiletries,
            @RequestParam(value = "shower", required = false) Boolean shower,
            @RequestParam(value = "bathrobe", required = false) Boolean bathrobe,
            @RequestParam(value = "toilet", required = false) Boolean toilet,
            @RequestParam(value = "towels", required = false) Boolean towels,
            @RequestParam(value = "slippers", required = false) Boolean slippers,
            @RequestParam(value = "hairdryer", required = false) Boolean hairdryer,
            @RequestParam(value = "toiletPaper", required = false) Boolean toiletPaper,
            @RequestParam(value = "airConditioning", required = false) Boolean airConditioning,
            @RequestParam(value = "safetyDepositBox", required = false) Boolean safetyDepositBox,
            @RequestParam(value = "desk", required = false) Boolean desk,
            @RequestParam(value = "television", required = false) Boolean television,
            @RequestParam(value = "telephone", required = false) Boolean telephone,
            @RequestParam(value = "iron", required = false) Boolean iron,
            @RequestParam(value = "electricKettle", required = false) Boolean electricKettle,
            @RequestParam(value = "cableChannels", required = false) Boolean cableChannels,
            @RequestParam(value = "wakeUpService", required = false) Boolean wakeUpService,
            @RequestParam(value = "wardrobeCloset", required = false) Boolean wardrobeCloset,
            @RequestParam(value = "clothesRack", required = false) Boolean clothesRack,
            @RequestParam(value = "freeBottledWater", defaultValue = "0") int freeBottledWater,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        // Kiểm tra Owner đã được duyệt
        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval. You cannot add rooms yet.");
            return "redirect:/owner/dashboard";
        }

        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || hotel.getOwner().getId() != owner.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to add rooms to this hotel.");
            return "redirect:/home";
        }

        boolean hasBathroom = (freeToiletries != null && freeToiletries)
                || (shower != null && shower)
                || (bathrobe != null && bathrobe)
                || (toilet != null && toilet)
                || (towels != null && towels)
                || (slippers != null && slippers)
                || (hairdryer != null && hairdryer)
                || (toiletPaper != null && toiletPaper);

        boolean hasRoom = (airConditioning != null && airConditioning)
                || (safetyDepositBox != null && safetyDepositBox)
                || (desk != null && desk)
                || (television != null && television)
                || (telephone != null && telephone)
                || (iron != null && iron)
                || (electricKettle != null && electricKettle)
                || (cableChannels != null && cableChannels)
                || (wakeUpService != null && wakeUpService)
                || (wardrobeCloset != null && wardrobeCloset)
                || (clothesRack != null && clothesRack);

        if (!hasBathroom || !hasRoom) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least 1 Bathroom Amenity and 1 Room Amenity.");
            return "redirect:/hotels/" + id + "/rooms/new";
        }

        String imgUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String original = imageFile.getOriginalFilename();
                String safeName = System.currentTimeMillis() + "_"
                        + (original != null ? original.replaceAll("[^a-zA-Z0-9._-]", "_") : "room.jpg");
                Path uploadDir = resolveStaticDir(ROOM_IMAGE_SUBDIR);
                Path filePath = uploadDir.resolve(safeName);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                imgUrl = "/assets/images/room/" + safeName;

                // Sync to target/classes for instant hot reload
                Path classesPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "static", ROOM_IMAGE_SUBDIR).toAbsolutePath().normalize();
                if (Files.exists(Paths.get(System.getProperty("user.dir"), "target", "classes", "static"))) {
                    if (!Files.exists(classesPath)) {
                        Files.createDirectories(classesPath);
                    }
                    Files.copy(filePath, classesPath.resolve(safeName), StandardCopyOption.REPLACE_EXISTING);
                }

            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed: " + e.getMessage());
                return "redirect:/hotels/" + id + "/rooms/new";
            }
        }

        Room room = new Room();
        room.setHotelId(id);
        room.setRoomType(type.trim());
        room.setPrice(BigDecimal.valueOf(price));
        room.setDescription(description.trim());
        room.setNumWindow(window);
        room.setBed(bed);
        room.setAcreage(acreage);
        room.setPerson(person);
        room.setImgUrl(imgUrl);
        room.setNumberRooms(numberRooms);
        room.setRoomStatus(roomStatus != null ? roomStatus : true);
        roomRepository.save(room);

        RoomFacility facility = new RoomFacility();
        facility.setRoom(room);
        facility.setFreeToiletries(freeToiletries != null && freeToiletries);
        facility.setShower(shower != null && shower);
        facility.setBathrobe(bathrobe != null && bathrobe);
        facility.setToilet(toilet != null && toilet);
        facility.setTowels(towels != null && towels);
        facility.setSlippers(slippers != null && slippers);
        facility.setHairdryer(hairdryer != null && hairdryer);
        facility.setToiletPaper(toiletPaper != null && toiletPaper);
        facility.setAirConditioning(airConditioning != null && airConditioning);
        facility.setSafetyDepositBox(safetyDepositBox != null && safetyDepositBox);
        facility.setDesk(desk != null && desk);
        facility.setTelevision(television != null && television);
        facility.setTelephone(telephone != null && telephone);
        facility.setIron(iron != null && iron);
        facility.setElectricKettle(electricKettle != null && electricKettle);
        facility.setCableChannels(cableChannels != null && cableChannels);
        facility.setWakeUpService(wakeUpService != null && wakeUpService);
        facility.setWardrobeCloset(wardrobeCloset != null && wardrobeCloset);
        facility.setClothesRack(clothesRack != null && clothesRack);
        facility.setFreeBottledWater(freeBottledWater);
        roomFacilityRepository.save(facility);

        redirectAttributes.addFlashAttribute("successMessage", "Room \"" + type + "\" added successfully!");
        return "redirect:/owner/hotels/" + id;
    }

    // ===== HELPER METHODS =====
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

    // ===================== GET /owner/hotels/{id}/rooms/{roomId}/edit =====================
    @GetMapping("/owner/hotels/{id}/rooms/{roomId}/edit")
    public String showEditRoomForm(
            @PathVariable("id") int id,
            @PathVariable("roomId") int roomId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            session.setAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || hotel.getOwner().getId() != owner.getId()) {
            return "redirect:/home";
        }

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getHotelId() != id) {
            return "redirect:/owner/hotels/" + id;
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("user", loggedInUser);
        return "owner/room-modify";
    }

    // ===================== POST /owner/hotels/{id}/rooms/{roomId}/edit =====================
    @PostMapping("/owner/hotels/{id}/rooms/{roomId}/edit")
    public String editRoom(
            @PathVariable("id") int id,
            @PathVariable("roomId") int roomId,
            @RequestParam("type") String type,
            @RequestParam("price") long price,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam(value = "window", defaultValue = "0") int window,
            @RequestParam(value = "bed", defaultValue = "0") int bed,
            @RequestParam("acreage") double acreage,
            @RequestParam("person") int person,
            @RequestParam(value = "numberRooms", defaultValue = "1") int numberRooms,
            @RequestParam(value = "roomStatus", required = false) Boolean roomStatus,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "freeToiletries", required = false) Boolean freeToiletries,
            @RequestParam(value = "shower", required = false) Boolean shower,
            @RequestParam(value = "bathrobe", required = false) Boolean bathrobe,
            @RequestParam(value = "toilet", required = false) Boolean toilet,
            @RequestParam(value = "towels", required = false) Boolean towels,
            @RequestParam(value = "slippers", required = false) Boolean slippers,
            @RequestParam(value = "hairdryer", required = false) Boolean hairdryer,
            @RequestParam(value = "toiletPaper", required = false) Boolean toiletPaper,
            @RequestParam(value = "airConditioning", required = false) Boolean airConditioning,
            @RequestParam(value = "safetyDepositBox", required = false) Boolean safetyDepositBox,
            @RequestParam(value = "desk", required = false) Boolean desk,
            @RequestParam(value = "television", required = false) Boolean television,
            @RequestParam(value = "telephone", required = false) Boolean telephone,
            @RequestParam(value = "iron", required = false) Boolean iron,
            @RequestParam(value = "electricKettle", required = false) Boolean electricKettle,
            @RequestParam(value = "cableChannels", required = false) Boolean cableChannels,
            @RequestParam(value = "wakeUpService", required = false) Boolean wakeUpService,
            @RequestParam(value = "wardrobeCloset", required = false) Boolean wardrobeCloset,
            @RequestParam(value = "clothesRack", required = false) Boolean clothesRack,
            @RequestParam(value = "freeBottledWater", defaultValue = "0") int freeBottledWater,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || hotel.getOwner().getId() != owner.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission.");
            return "redirect:/home";
        }

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getHotelId() != id) {
            redirectAttributes.addFlashAttribute("errorMessage", "Room not found.");
            return "redirect:/owner/hotels/" + id;
        }

        boolean hasBathroom = (freeToiletries != null && freeToiletries)
                || (shower != null && shower)
                || (bathrobe != null && bathrobe)
                || (toilet != null && toilet)
                || (towels != null && towels)
                || (slippers != null && slippers)
                || (hairdryer != null && hairdryer)
                || (toiletPaper != null && toiletPaper);

        boolean hasRoom = (airConditioning != null && airConditioning)
                || (safetyDepositBox != null && safetyDepositBox)
                || (desk != null && desk)
                || (television != null && television)
                || (telephone != null && telephone)
                || (iron != null && iron)
                || (electricKettle != null && electricKettle)
                || (cableChannels != null && cableChannels)
                || (wakeUpService != null && wakeUpService)
                || (wardrobeCloset != null && wardrobeCloset)
                || (clothesRack != null && clothesRack);

        if (!hasBathroom || !hasRoom) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least 1 Bathroom Amenity and 1 Room Amenity.");
            return "redirect:/owner/hotels/" + id + "/rooms/" + roomId + "/edit";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String original = imageFile.getOriginalFilename();
                String safeName = System.currentTimeMillis() + "_"
                        + (original != null ? original.replaceAll("[^a-zA-Z0-9._-]", "_") : "room.jpg");
                Path uploadDir = resolveStaticDir(ROOM_IMAGE_SUBDIR);
                Path filePath = uploadDir.resolve(safeName);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                room.setImgUrl("/assets/images/room/" + safeName);

                // Sync to target/classes
                Path classesPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "static", ROOM_IMAGE_SUBDIR).toAbsolutePath().normalize();
                if (Files.exists(Paths.get(System.getProperty("user.dir"), "target", "classes", "static"))) {
                    if (!Files.exists(classesPath)) {
                        Files.createDirectories(classesPath);
                    }
                    Files.copy(filePath, classesPath.resolve(safeName), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed: " + e.getMessage());
                return "redirect:/owner/hotels/" + id + "/rooms/" + roomId + "/edit";
            }
        }

        room.setRoomType(type.trim());
        room.setPrice(BigDecimal.valueOf(price));
        room.setDescription(description.trim());
        room.setNumWindow(window);
        room.setBed(bed);
        room.setAcreage(acreage);
        room.setPerson(person);
        room.setNumberRooms(numberRooms);
        room.setRoomStatus(roomStatus != null ? roomStatus : true);
        roomRepository.save(room);

        RoomFacility facility = roomFacilityRepository.findByRoom(room).orElse(null);
        if (facility == null) {
            facility = new RoomFacility();
            facility.setRoom(room);
        }
        facility.setFreeToiletries(freeToiletries != null && freeToiletries);
        facility.setShower(shower != null && shower);
        facility.setBathrobe(bathrobe != null && bathrobe);
        facility.setToilet(toilet != null && toilet);
        facility.setTowels(towels != null && towels);
        facility.setSlippers(slippers != null && slippers);
        facility.setHairdryer(hairdryer != null && hairdryer);
        facility.setToiletPaper(toiletPaper != null && toiletPaper);
        facility.setAirConditioning(airConditioning != null && airConditioning);
        facility.setSafetyDepositBox(safetyDepositBox != null && safetyDepositBox);
        facility.setDesk(desk != null && desk);
        facility.setTelevision(television != null && television);
        facility.setTelephone(telephone != null && telephone);
        facility.setIron(iron != null && iron);
        facility.setElectricKettle(electricKettle != null && electricKettle);
        facility.setCableChannels(cableChannels != null && cableChannels);
        facility.setWakeUpService(wakeUpService != null && wakeUpService);
        facility.setWardrobeCloset(wardrobeCloset != null && wardrobeCloset);
        facility.setClothesRack(clothesRack != null && clothesRack);
        facility.setFreeBottledWater(freeBottledWater);
        roomFacilityRepository.save(facility);

        redirectAttributes.addFlashAttribute("successMessage", "Room \"" + type + "\" updated successfully!");
        return "redirect:/owner/hotels/" + id;
    }

    // ===================== POST /owner/hotels/{id}/rooms/{roomId}/delete =====================
    @PostMapping("/owner/hotels/{id}/rooms/{roomId}/delete")
    public String deleteRoom(
            @PathVariable("id") int id,
            @PathVariable("roomId") int roomId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) return "redirect:/hotels";

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || hotel.getOwner().getId() != owner.getId()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission.");
            return "redirect:/home";
        }

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getHotelId() != id) {
            redirectAttributes.addFlashAttribute("errorMessage", "Room not found.");
            return "redirect:/owner/hotels/" + id;
        }

        try {
            // Delete RoomFacility first
            RoomFacility facility = roomFacilityRepository.findByRoom(room).orElse(null);
            if (facility != null) {
                roomFacilityRepository.delete(facility);
                roomFacilityRepository.flush();
            }

            // Delete Room
            roomRepository.delete(room);
            roomRepository.flush();

            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete this room because it is associated with existing bookings.");
        }

        return "redirect:/owner/hotels/" + id;
    }
}