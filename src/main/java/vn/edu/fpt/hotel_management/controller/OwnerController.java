package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import vn.edu.fpt.hotel_management.service.OwnerService;
import vn.edu.fpt.hotel_management.service.PromotionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final HotelOwnerRepository hotelOwnerRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final HotelVerificationDocumentRepository hotelVerificationDocumentRepository;
    private final RoomFacilityRepository roomFacilityRepository;
    private final OwnerService ownerService;
    private final PromotionService promotionService;

    private static final String HOTEL_IMAGE_SUBDIR = "assets/images/hotel";
    private static final String HOTEL_IMAGE_URL_PREFIX = "/assets/images/hotel/";
    private static final String HOTEL_DOCS_SUBDIR = "assets/docs/hotel_docs";

    public OwnerController(HotelOwnerRepository hotelOwnerRepository,
                           HotelRepository hotelRepository,
                           RoomRepository roomRepository,
                           BookingRepository bookingRepository,
                           PaymentRepository paymentRepository,
                           HotelVerificationDocumentRepository hotelVerificationDocumentRepository,
                           RoomFacilityRepository roomFacilityRepository,
                           OwnerService ownerService,
                           PromotionService promotionService) {
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.hotelVerificationDocumentRepository = hotelVerificationDocumentRepository;
        this.roomFacilityRepository = roomFacilityRepository;
        this.ownerService = ownerService;
        this.promotionService = promotionService;
    }

    private String saveUploadedFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Path uploadPath = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", "assets", "docs", subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFilename = file.getOriginalFilename();
        String safeFilename = System.currentTimeMillis() + "_" +
                (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "document.pdf");
        Path target = uploadPath.resolve(safeFilename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/assets/docs/" + subDir + "/" + safeFilename;
    }

    private Path resolveStaticDir(String subDir) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", subDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    // Helper method to extract check-in/out status from specialNotes
    private boolean extractCheckStatus(Booking b, String marker) {
        if (b.getSpecialNotes() == null) return false;
        Pattern pattern = Pattern.compile(marker + ":(true|false)");
        Matcher matcher = pattern.matcher(b.getSpecialNotes());
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "tab", defaultValue = "overview") String tab,
            @RequestParam(value = "searchCustomer", required = false) String searchCustomer,
            @RequestParam(value = "filterHotel", required = false) Integer filterHotel,
            @RequestParam(value = "filterCheckin", required = false) String filterCheckin,
            @RequestParam(value = "filterCheckout", required = false) String filterCheckout,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(user.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = ownerService.getOwnerByUser(user).orElse(null);
        if (owner == null) {
            return "redirect:/home";
        }

        // Tự động chuyển các booking CONFIRMED đã quá giờ checkout (12:00 trưa) sang COMPLETED
        try {
            List<Booking> confirmedBookings = bookingRepository.findByStatusInOrderByCreatedAtDesc(java.util.Arrays.asList("CONFIRMED"));
            LocalDateTime now = LocalDateTime.now();
            for (Booking b : confirmedBookings) {
                if (b.getCheckOutDate() != null
                        && b.getCheckOutDate().atTime(12, 0).isBefore(now)) {
                    b.setStatus("COMPLETED");
                    b.setUpdatedAt(now);
                    bookingRepository.save(b);
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi nếu có
        }

        String verificationStatus = owner.getVerificationStatus();
        model.addAttribute("verificationStatus", verificationStatus);
        model.addAttribute("user", user);
        model.addAttribute("owner", owner);
        model.addAttribute("tab", tab);

        if (!"APPROVED".equals(verificationStatus)) {
            model.addAttribute("totalHotels", 0);
            model.addAttribute("totalRooms", 0);
            model.addAttribute("totalBookings", 0);
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("recentBookings", Collections.emptyList());
            model.addAttribute("hotels", Collections.emptyList());
            model.addAttribute("roomCountMap", Collections.emptyMap());
            model.addAttribute("approvedHotels", Collections.emptyList());
            model.addAttribute("totalPromotions", 0);
            return "owner/dashboard";
        }

        List<Hotel> allHotels = hotelRepository.findByOwnerId(owner.getId());
        List<Hotel> approvedHotels = allHotels.stream()
                .filter(h -> "APPROVED".equals(h.getApprovalStatus()))
                .collect(Collectors.toList());

        List<Integer> hotelIds = approvedHotels.stream().map(Hotel::getId).collect(Collectors.toList());

        Map<Integer, Integer> roomCountMap = new HashMap<>();
        for (Integer hotelId : hotelIds) {
            List<Room> rooms = roomRepository.findByHotelId(hotelId);
            roomCountMap.put(hotelId, rooms.size());
        }

        long totalHotels = allHotels.size();
        long totalRooms = roomCountMap.values().stream().mapToInt(Integer::intValue).sum();

        // ===== FILTER BOOKINGS =====
        LocalDate checkinDate = null;
        LocalDate checkoutDate = null;
        try {
            if (filterCheckin != null && !filterCheckin.isEmpty()) {
                checkinDate = LocalDate.parse(filterCheckin);
            }
            if (filterCheckout != null && !filterCheckout.isEmpty()) {
                checkoutDate = LocalDate.parse(filterCheckout);
            }
        } catch (Exception e) {
            // ignore
        }

        List<Booking> filteredBookings;
        if (hotelIds.isEmpty()) {
            filteredBookings = new ArrayList<>();
        } else if (searchCustomer != null && !searchCustomer.trim().isEmpty()) {
            filteredBookings = bookingRepository.findByHotelIdsAndCustomerName(hotelIds, searchCustomer.trim());
        } else if (filterHotel != null && filterHotel > 0) {
            filteredBookings = bookingRepository.findByHotelIdInOrderByCreatedAtDesc(List.of(filterHotel));
        } else if (filterCheckin != null && !filterCheckin.isEmpty()) {
            try {
                LocalDate ci = LocalDate.parse(filterCheckin);
                filteredBookings = bookingRepository.findByHotelIdInAndCheckInDateOrderByCreatedAtDesc(hotelIds, ci);
            } catch (Exception e) {
                filteredBookings = bookingRepository.findByHotelIdInOrderByCreatedAtDesc(hotelIds);
            }
        } else if (filterCheckout != null && !filterCheckout.isEmpty()) {
            try {
                LocalDate co = LocalDate.parse(filterCheckout);
                filteredBookings = bookingRepository.findByHotelIdInAndCheckOutDateOrderByCreatedAtDesc(hotelIds, co);
            } catch (Exception e) {
                filteredBookings = bookingRepository.findByHotelIdInOrderByCreatedAtDesc(hotelIds);
            }
        } else {
            filteredBookings = bookingRepository.findByHotelIdInOrderByCreatedAtDesc(hotelIds);
        }

        // Calculate total bookings and revenue
        long totalBookings = filteredBookings.size();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Booking b : filteredBookings) {
            if (b.getPayment() != null && "PAID".equals(b.getPayment().getStatus())) {
                if (b.getTotalPrice() != null) {
                    totalRevenue = totalRevenue.add(b.getTotalPrice());
                }
            }
        }

        // Get recent bookings (limit 50)
        List<Booking> recentBookings = filteredBookings.stream()
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .limit(50)
                .collect(Collectors.toList());

        List<Map<String, Object>> mappedBookings = recentBookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getId());
            map.put("customerName", (b.getFullName() != null && !b.getFullName().isEmpty()) ? b.getFullName() : (b.getCustomer() != null ? b.getCustomer().getFullName() : "N/A"));
            map.put("hotelName", b.getHotel() != null ? b.getHotel().getName() : "N/A");
            map.put("roomType", b.getRoom() != null ? b.getRoom().getRoomType() : "N/A");
            map.put("checkInDate", b.getCheckInDate() != null ? b.getCheckInDate().toString() : null);
            map.put("checkOutDate", b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : null);
            map.put("totalPrice", b.getTotalPrice());
            map.put("bookingStatus", b.getStatus());
            map.put("paymentStatus", (b.getPayment() != null && b.getPayment().getStatus() != null)
                    ? b.getPayment().getStatus() : "PENDING");
            map.put("payoutStatus", b.getPayoutStatus() != null ? b.getPayoutStatus() : "");
            map.put("ownerPayoutAmount", b.getOwnerPayoutAmount() != null ? b.getOwnerPayoutAmount() : BigDecimal.ZERO);
            map.put("platformFeePercent", b.getPlatformFeePercent() != null ? b.getPlatformFeePercent() : BigDecimal.valueOf(10));
            map.put("payoutAt", b.getPayoutAt() != null ? b.getPayoutAt().toString() : null);
            map.put("payoutBankName", b.getPayoutBankName() != null ? b.getPayoutBankName() : "");
            map.put("payoutBankAccountNumber", b.getPayoutBankAccountNumber() != null ? b.getPayoutBankAccountNumber() : "");
            map.put("payoutBankAccountHolder", b.getPayoutBankAccountHolder() != null ? b.getPayoutBankAccountHolder() : "");
            // Check-in / Check-out status
            map.put("checkInStatus", extractCheckStatus(b, "CHECKED_IN"));
            map.put("checkOutStatus", extractCheckStatus(b, "CHECKED_OUT"));
            map.put("specialNotes", b.getSpecialNotes() != null ? b.getSpecialNotes() : "");
            return map;
        }).collect(Collectors.toList());

        // Đếm số booking COMPLETED đã được thanh toán nhưng payout chưa xử lý
        long pendingPayoutCount = 0;
        long completedPayoutCount = 0;
        if (!hotelIds.isEmpty()) {
            for (Booking b : bookingRepository.findByHotelIdInOrderByCreatedAtDesc(hotelIds)) {
                if ("COMPLETED".equals(b.getStatus())
                        && b.getPayment() != null && "PAID".equals(b.getPayment().getStatus())) {
                    if ("PAID".equals(b.getPayoutStatus())) {
                        completedPayoutCount++;
                    } else {
                        pendingPayoutCount++;
                    }
                }
            }
        }
        model.addAttribute("pendingPayoutCount", pendingPayoutCount);
        model.addAttribute("completedPayoutCount", completedPayoutCount);

        // ===== PROMOTIONS =====
        Map<Integer, List<Promotion>> promotionsByHotel = new HashMap<>();
        int totalPromotions = 0;
        for (Hotel hotel : approvedHotels) {
            List<Promotion> promotions = promotionService.getPromotionsByHotelId(hotel.getId());
            promotionsByHotel.put(hotel.getId(), promotions);
            totalPromotions += promotions.size();
        }

        model.addAttribute("totalHotels", totalHotels);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBookings", mappedBookings);
        model.addAttribute("hotels", allHotels);
        model.addAttribute("approvedHotels", approvedHotels);
        model.addAttribute("roomCountMap", roomCountMap);
        model.addAttribute("promotionsByHotel", promotionsByHotel);
        model.addAttribute("totalPromotions", totalPromotions);
        model.addAttribute("allPromotions", promotionService.getPromotionsByOwnerId(owner.getId()));

        // Keep filter values
        model.addAttribute("searchCustomer", searchCustomer);
        model.addAttribute("filterHotel", filterHotel);
        model.addAttribute("filterCheckin", filterCheckin);
        model.addAttribute("filterCheckout", filterCheckout);

        return "owner/dashboard";
    }

    @PostMapping("/dashboard/add-hotel")
    public String addHotelFromDashboard(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam("description") String description,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam(value = "businessRegistrationDoc", required = false) MultipartFile businessRegistrationDoc,
            @RequestParam(value = "landCertificateDoc", required = false) MultipartFile landCertificateDoc,
            @RequestParam(value = "rentalContractDoc", required = false) MultipartFile rentalContractDoc,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        if (description == null || description.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel description is required!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        if (imageFile == null || imageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel image is required!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        String imageContentType = imageFile.getContentType();
        if (imageContentType == null || !imageContentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel image must be an image file (JPG, PNG, WEBP, etc.)!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        if (businessRegistrationDoc != null && !businessRegistrationDoc.isEmpty()) {
            String bizContentType = businessRegistrationDoc.getContentType();
            if (bizContentType == null || !bizContentType.equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Business Registration must be a PDF file!");
                return "redirect:/owner/dashboard?tab=hotels";
            }
        }

        if (landCertificateDoc != null && !landCertificateDoc.isEmpty()) {
            String landContentType = landCertificateDoc.getContentType();
            if (landContentType == null || !landContentType.equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Land Certificate must be a PDF file!");
                return "redirect:/owner/dashboard?tab=hotels";
            }
        }

        if (rentalContractDoc != null && !rentalContractDoc.isEmpty()) {
            String rentalContentType = rentalContractDoc.getContentType();
            if (rentalContentType == null || !rentalContentType.equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rental Contract must be a PDF file!");
                return "redirect:/owner/dashboard?tab=hotels";
            }
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval. Please wait for verification.");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner profile not found!");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        String imageUrl = null;
        try {
            String originalFilename = imageFile.getOriginalFilename();
            String safeFilename = System.currentTimeMillis() + "_"
                    + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "hotel.jpg");
            Path uploadPath = resolveStaticDir(HOTEL_IMAGE_SUBDIR);
            Path targetFile = uploadPath.resolve(safeFilename);
            Files.copy(imageFile.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING);
            imageUrl = HOTEL_IMAGE_URL_PREFIX + safeFilename;

            Path classesPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "static", HOTEL_IMAGE_SUBDIR).toAbsolutePath().normalize();
            if (Files.exists(Paths.get(System.getProperty("user.dir"), "target", "classes", "static"))) {
                if (!Files.exists(classesPath)) {
                    Files.createDirectories(classesPath);
                }
                Files.copy(targetFile, classesPath.resolve(safeFilename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed: " + e.getMessage());
            return "redirect:/owner/dashboard?tab=hotels";
        }

        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city);
        hotel.setDistrict(district);
        hotel.setDescription(description);
        hotel.setImageUrl(imageUrl);
        hotel.setActive(false);
        hotel.setOwner(owner);
        hotel.setRating(0.0);
        hotel.setTotalReviews(0);
        hotel.setApprovalStatus("PENDING");

        try {
            hotelRepository.save(hotel);

            HotelVerificationDocument doc = new HotelVerificationDocument();
            doc.setHotel(hotel);
            doc.setUploadStatus("PENDING");
            doc.setPhone(phone);
            doc.setEmail(email);

            if (businessRegistrationDoc != null && !businessRegistrationDoc.isEmpty()) {
                try {
                    String path = saveUploadedFile(businessRegistrationDoc, "hotel_docs");
                    doc.setBusinessRegistrationDoc(path);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Failed to upload Business Registration: " + e.getMessage());
                    return "redirect:/owner/dashboard?tab=hotels";
                }
            }

            if (landCertificateDoc != null && !landCertificateDoc.isEmpty()) {
                try {
                    String path = saveUploadedFile(landCertificateDoc, "hotel_docs");
                    doc.setLandCertificateDoc(path);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Failed to upload Land Certificate: " + e.getMessage());
                    return "redirect:/owner/dashboard?tab=hotels";
                }
            }

            if (rentalContractDoc != null && !rentalContractDoc.isEmpty()) {
                try {
                    String path = saveUploadedFile(rentalContractDoc, "hotel_docs");
                    doc.setRentalContractDoc(path);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Failed to upload Rental Contract: " + e.getMessage());
                    return "redirect:/owner/dashboard?tab=hotels";
                }
            }

            hotelVerificationDocumentRepository.save(doc);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + name + "\" added successfully! Documents submitted for verification.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save hotel: " + e.getMessage());
            return "redirect:/owner/dashboard?tab=hotels";
        }

        return "redirect:/owner/dashboard?tab=hotels";
    }

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

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval. Please wait for verification.");
            return "redirect:/owner/dashboard?tab=hotels";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
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

            // ===== 1. XÓA PROMOTIONS TRƯỚC =====
            List<Promotion> promotions = promotionService.getPromotionsByHotelId(hotelId);
            if (!promotions.isEmpty()) {
                for (Promotion p : promotions) {
                    promotionService.deletePromotion(p.getId(), hotelId);
                }
            }

            // ===== 2. XÓA HOTEL VERIFICATION DOCUMENTS =====
            List<HotelVerificationDocument> docs = hotelVerificationDocumentRepository.findByHotelId(hotelId);
            if (!docs.isEmpty()) {
                hotelVerificationDocumentRepository.deleteAll(docs);
                hotelVerificationDocumentRepository.flush();
            }

            // ===== 3. XÓA ROOMS VÀ ROOM FACILITIES =====
            List<Room> rooms = roomRepository.findByHotelId(hotelId);
            if (!rooms.isEmpty()) {
                for (Room room : rooms) {
                    Optional<RoomFacility> facility = roomFacilityRepository.findByRoom(room);
                    facility.ifPresent(roomFacilityRepository::delete);
                }
                roomFacilityRepository.flush();
                roomRepository.deleteAll(rooms);
                roomRepository.flush();
            }

            // ===== 4. XÓA HOTEL =====
            hotelRepository.delete(hotel);
            hotelRepository.flush();

            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + hotelName + "\" deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete hotel: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/owner/dashboard?tab=hotels";
    }

    // =====================================================
    // GET BOOKING DETAIL FOR MODAL (AJAX)
    // =====================================================

    @GetMapping("/owner/booking/detail-data")
    @ResponseBody
    public ResponseEntity<?> getBookingDetailData(
            @RequestParam("bookingId") int bookingId,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Account not approved"));
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Booking not found"));
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || booking.getHotel().getOwner().getId() != owner.getId()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Permission denied"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", booking.getId());
        data.put("customerName", booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A");
        data.put("hotelName", booking.getHotel() != null ? booking.getHotel().getName() : "N/A");
        data.put("roomType", booking.getRoom() != null ? booking.getRoom().getRoomType() : "N/A");
        data.put("checkInDate", booking.getCheckInDate() != null ? booking.getCheckInDate().toString() : null);
        data.put("checkOutDate", booking.getCheckOutDate() != null ? booking.getCheckOutDate().toString() : null);
        data.put("totalPrice", booking.getTotalPrice());
        data.put("bookingStatus", booking.getStatus());
        data.put("paymentStatus", booking.getPayment() != null ? booking.getPayment().getStatus() : "PENDING");
        data.put("payoutStatus", booking.getPayoutStatus());
        data.put("ownerPayoutAmount", booking.getOwnerPayoutAmount());
        data.put("platformFeePercent", booking.getPlatformFeePercent());
        data.put("payoutBankName", booking.getPayoutBankName());
        data.put("payoutBankAccountNumber", booking.getPayoutBankAccountNumber());
        data.put("payoutBankAccountHolder", booking.getPayoutBankAccountHolder());
        data.put("payoutAt", booking.getPayoutAt() != null ? booking.getPayoutAt().toString() : null);
        data.put("checkInStatus", extractCheckStatus(booking, "CHECKED_IN"));
        data.put("checkOutStatus", extractCheckStatus(booking, "CHECKED_OUT"));
        data.put("specialNotes", booking.getSpecialNotes());

        return ResponseEntity.ok(data);
    }

    // =====================================================
    // UPDATE CHECK-IN STATUS (AJAX)
    // =====================================================

    @PostMapping("/owner/booking/update-checkin")
    @ResponseBody
    public ResponseEntity<?> updateCheckInStatus(
            @RequestParam("bookingId") int bookingId,
            @RequestParam("checkedIn") boolean checkedIn,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Account not approved"));
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Booking not found"));
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || booking.getHotel().getOwner().getId() != owner.getId()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Permission denied"));
        }

        // Chỉ cho phép update khi booking ở trạng thái CONFIRMED hoặc COMPLETED
        if (!"CONFIRMED".equals(booking.getStatus()) && !"COMPLETED".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    "Cannot update check-in status for booking with status: " + booking.getStatus()));
        }

        // Lưu check-in status vào specialNotes với định dạng key-value
        String currentNotes = booking.getSpecialNotes() != null ? booking.getSpecialNotes() : "";
        String checkInMarker = "CHECKED_IN:" + checkedIn;

        // Xóa marker cũ nếu có
        String cleaned = currentNotes.replaceAll("CHECKED_IN:(true|false),?", "");
        // Thêm marker mới
        if (!cleaned.isEmpty() && !cleaned.endsWith(",")) {
            cleaned += ",";
        }
        cleaned += checkInMarker;
        booking.setSpecialNotes(cleaned);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of("success", true, "message", "Check-in status updated"));
    }

    // =====================================================
    // UPDATE CHECK-OUT STATUS (AJAX)
    // =====================================================

    @PostMapping("/owner/booking/update-checkout")
    @ResponseBody
    public ResponseEntity<?> updateCheckOutStatus(
            @RequestParam("bookingId") int bookingId,
            @RequestParam("checkedOut") boolean checkedOut,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Account not approved"));
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Booking not found"));
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null || booking.getHotel().getOwner().getId() != owner.getId()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Permission denied"));
        }

        // Chỉ cho phép update khi booking ở trạng thái CONFIRMED hoặc COMPLETED
        if (!"CONFIRMED".equals(booking.getStatus()) && !"COMPLETED".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    "Cannot update check-out status for booking with status: " + booking.getStatus()));
        }

        // Lưu check-out status vào specialNotes với định dạng key-value
        String currentNotes = booking.getSpecialNotes() != null ? booking.getSpecialNotes() : "";
        String checkOutMarker = "CHECKED_OUT:" + checkedOut;

        // Xóa marker cũ nếu có
        String cleaned = currentNotes.replaceAll("CHECKED_OUT:(true|false),?", "");
        // Thêm marker mới
        if (!cleaned.isEmpty() && !cleaned.endsWith(",")) {
            cleaned += ",";
        }
        cleaned += checkOutMarker;
        booking.setSpecialNotes(cleaned);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of("success", true, "message", "Check-out status updated"));
    }
}