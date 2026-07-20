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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class HotelController {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final HotelVerificationDocumentRepository hotelVerificationDocumentRepository;
    private final RoomFacilityRepository roomFacilityRepository;
    private final OwnerService ownerService;
    private final BookingRepository bookingRepository;

    private static final String HOTEL_IMAGE_SUBDIR = "assets/images/hotel";
    private static final String HOTEL_IMAGE_URL_PREFIX = "/assets/images/hotel/";
    private static final String HOTEL_DOCS_SUBDIR = "assets/docs/hotel_docs";

    public HotelController(HotelRepository hotelRepository,
                           RoomRepository roomRepository,
                           HotelOwnerRepository hotelOwnerRepository,
                           HotelVerificationDocumentRepository hotelVerificationDocumentRepository,
                           RoomFacilityRepository roomFacilityRepository,
                           OwnerService ownerService,
                           BookingRepository bookingRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.hotelVerificationDocumentRepository = hotelVerificationDocumentRepository;
        this.roomFacilityRepository = roomFacilityRepository;
        this.ownerService = ownerService;
        this.bookingRepository = bookingRepository;
    }

    private String saveUploadedFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Kiểm tra kích thước tệp tin (Giới hạn tối đa 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds the 10MB limit!");
        }

        // 2. Kiểm tra định dạng tệp tin dựa trên loại tài liệu
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IOException("Invalid file type!");
        }

        if ("owner_docs".equals(subDir)) {
            // Đối với tài liệu CMND/CCCD của Owner: cho phép ảnh hoặc PDF
            if (!contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
                throw new IOException("ID Card document must be an image (PNG, JPG) or a PDF file!");
            }
        } else if ("hotel_docs".equals(subDir)) {
            // Đối với tài liệu xác minh khách sạn: chỉ cho phép PDF
            if (!contentType.equals("application/pdf")) {
                throw new IOException("Hotel verification document must be a PDF file!");
            }
        }

        // 3. Tiến hành lưu tệp tin vào thư mục src
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

        // 4. Đồng bộ sang thư mục target/classes để hiển thị ngay lập tức ở runtime
        Path classesPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "static", "assets", "docs", subDir).toAbsolutePath().normalize();
        if (Files.exists(Paths.get(System.getProperty("user.dir"), "target", "classes", "static"))) {
            if (!Files.exists(classesPath)) {
                Files.createDirectories(classesPath);
            }
            Files.copy(target, classesPath.resolve(safeFilename), StandardCopyOption.REPLACE_EXISTING);
        }

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

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    @GetMapping("/hotels")
    public String showHotelsPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "hotelFacilities", required = false) List<String> hotelFacilities,
            @RequestParam(value = "hotelViews", required = false) List<String> hotelViews,
            @RequestParam(value = "roomFacilities", required = false) List<String> roomFacilities,
            @RequestParam(value = "persons", required = false) Integer persons,
            @RequestParam(value = "rooms", required = false) Integer rooms,
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

        if (search != null && !search.trim().isEmpty()) {
            String cleanSearch = removeAccent(search.trim().toLowerCase());
            hotels = hotels.stream().filter(h -> 
                h.getCity() != null && removeAccent(h.getCity().toLowerCase()).contains(cleanSearch)
            ).collect(java.util.stream.Collectors.toList());
        }

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

        // Nếu checkin hoặc checkout chưa có, gán mặc định là hôm nay và ngày mai
        if (checkin == null || checkin.trim().isEmpty()) {
            checkin = java.time.LocalDate.now().toString();
        }
        if (checkout == null || checkout.trim().isEmpty()) {
            checkout = java.time.LocalDate.now().plusDays(1).toString();
        }

        // Lưu bộ lọc checkin/checkout của list hotel vào session
        session.setAttribute("hotelCheckinFilter", checkin);
        session.setAttribute("hotelCheckoutFilter", checkout);


        long nights = 1;
        boolean isFiltered = false;
        java.util.Map<Integer, BigDecimal> hotelPricesMap = new java.util.HashMap<>();

        java.time.LocalDate d1 = java.time.LocalDate.now();
        java.time.LocalDate d2 = java.time.LocalDate.now().plusDays(1);

        try {
            if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
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
            }
        } catch (Exception e) {
            isFiltered = false;
            d1 = java.time.LocalDate.now();
            d2 = java.time.LocalDate.now().plusDays(1);
        }

        final java.time.LocalDate finalD1 = d1;
        final java.time.LocalDate finalD2 = d2;
        final boolean finalIsFiltered = isFiltered;

        List<Hotel> filteredHotels = new java.util.ArrayList<>();

        for (Hotel h : hotels) {
            List<Room> hotelRooms = roomRepository.findByHotelId(h.getId());
            if (hotelRooms == null || hotelRooms.isEmpty()) {
                continue;
            }
            
            // Tìm các phòng thỏa mãn tiện ích phòng, sức chứa và tính tổng số phòng trống khả dụng
            int totalAvailableRooms = 0;
            java.util.List<Room> roomsWithAvailable = new java.util.ArrayList<>();
            
            for (Room r : hotelRooms) {
                // 1. Kiểm tra phòng phải active (roomStatus == true)
                if (r.getNumberRooms() == null || r.getNumberRooms() <= 0) continue;
                if (r.getRoomStatus() != null && !r.getRoomStatus()) continue;
                
                // 2. Lọc theo số lượng người (persons)
                int reqPersons = (persons != null) ? persons : 1;
                if (r.getPerson() < reqPersons) continue;

                // 3. Lọc theo tiện ích phòng (roomFacilities) nếu có
                if (roomFacilities != null && !roomFacilities.isEmpty()) {
                    RoomFacility rf = r.getFacility();
                    if (rf == null) continue;
                    boolean facilityMatch = true;
                    for (String f : roomFacilities) {
                        if ("freeToiletries".equals(f) && !rf.isFreeToiletries()) { facilityMatch = false; break; }
                        if ("shower".equals(f) && !rf.isShower()) { facilityMatch = false; break; }
                        if ("bathrobe".equals(f) && !rf.isBathrobe()) { facilityMatch = false; break; }
                        if ("toilet".equals(f) && !rf.isToilet()) { facilityMatch = false; break; }
                        if ("towels".equals(f) && !rf.isTowels()) { facilityMatch = false; break; }
                        if ("slippers".equals(f) && !rf.isSlippers()) { facilityMatch = false; break; }
                        if ("hairdryer".equals(f) && !rf.isHairdryer()) { facilityMatch = false; break; }
                        if ("toiletPaper".equals(f) && !rf.isToiletPaper()) { facilityMatch = false; break; }
                        if ("airConditioning".equals(f) && !rf.isAirConditioning()) { facilityMatch = false; break; }
                        if ("safetyDepositBox".equals(f) && !rf.isSafetyDepositBox()) { facilityMatch = false; break; }
                        if ("desk".equals(f) && !rf.isDesk()) { facilityMatch = false; break; }
                        if ("television".equals(f) && !rf.isTelevision()) { facilityMatch = false; break; }
                        if ("telephone".equals(f) && !rf.isTelephone()) { facilityMatch = false; break; }
                        if ("iron".equals(f) && !rf.isIron()) { facilityMatch = false; break; }
                        if ("electricKettle".equals(f) && !rf.isElectricKettle()) { facilityMatch = false; break; }
                        if ("cableChannels".equals(f) && !rf.isCableChannels()) { facilityMatch = false; break; }
                        if ("wakeUpService".equals(f) && !rf.isWakeUpService()) { facilityMatch = false; break; }
                        if ("wardrobeCloset".equals(f) && !rf.isWardrobeCloset()) { facilityMatch = false; break; }
                        if ("clothesRack".equals(f) && !rf.isClothesRack()) { facilityMatch = false; break; }
                        if ("freeBottledWater".equals(f) && rf.getFreeBottledWater() <= 0) { facilityMatch = false; break; }
                    }
                    if (!facilityMatch) continue;
                }

                // 4. Kiểm tra phòng còn trống trong khoảng thời gian đã chọn hay không
                long bookedCount = bookingRepository.sumQuantityForConfirmedAndPending(
                        r.getId(),
                        finalD2,
                        finalD1
                );
                int available = r.getNumberRooms() - (int) bookedCount;
                if (available > 0) {
                    totalAvailableRooms += available;
                    roomsWithAvailable.add(r);
                }
            }

            // Kiểm tra xem tổng số phòng trống của các phòng thỏa mãn filter có đủ số lượng phòng yêu cầu (rooms) không
            int reqRooms = (rooms != null) ? rooms : 1;
            if (totalAvailableRooms < reqRooms || roomsWithAvailable.isEmpty()) {
                continue;
            }

            // Lấy phòng có giá thấp nhất trong các phòng hợp lệ
            BigDecimal basePrice = roomsWithAvailable.stream()
                    .map(Room::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal actualPrice = finalIsFiltered ? calculateHotelSubtotal(basePrice, finalD1, finalD2) : basePrice;
            hotelPricesMap.put(h.getId(), actualPrice);
            filteredHotels.add(h);
        }
        hotels = filteredHotels;

        hotels = hotels.stream()
                .sorted((h1, h2) -> {
                    double r1 = h1.getRating();
                    double r2 = h2.getRating();
                    int ratingCompare = Double.compare(r2, r1); // Giảm dần
                    
                    if (ratingCompare != 0) {
                        return ratingCompare;
                    }
                    
                    BigDecimal p1 = hotelPricesMap.getOrDefault(h1.getId(), BigDecimal.ZERO);
                    BigDecimal p2 = hotelPricesMap.getOrDefault(h2.getId(), BigDecimal.ZERO);
                    return p1.compareTo(p2); // Tăng dần
                })
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("hotels", hotels);
        model.addAttribute("hotelPricesMap", hotelPricesMap);
        model.addAttribute("nights", nights);
        model.addAttribute("isFiltered", isFiltered);
        model.addAttribute("search", search);
        model.addAttribute("rating", rating);
        model.addAttribute("minPrice", resolvedMinPrice);
        model.addAttribute("maxPrice", resolvedMaxPrice);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("today", java.time.LocalDate.now().toString());
        model.addAttribute("minCheckout", java.time.LocalDate.parse(checkin).plusDays(1).toString());
        model.addAttribute("totalResults", hotels.size());

        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("today", java.time.LocalDate.now().toString());

        model.addAttribute("selectedHotelFacilities", hotelFacilities != null ? hotelFacilities : List.of());
        model.addAttribute("selectedHotelViews", hotelViews != null ? hotelViews : List.of());
        model.addAttribute("selectedRoomFacilities", roomFacilities != null ? roomFacilities : List.of());
        model.addAttribute("persons", persons != null ? persons : 1);
        model.addAttribute("rooms", rooms != null ? rooms : 1);

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
            @RequestParam("description") String description,
            @RequestParam(value = "ownerId", required = false) Integer ownerId,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "active", required = false, defaultValue = "false") boolean active,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "businessRegistrationDoc", required = false) MultipartFile businessRegistrationDoc,
            @RequestParam(value = "landCertificateDoc", required = false) MultipartFile landCertificateDoc,
            @RequestParam(value = "rentalContractDoc", required = false) MultipartFile rentalContractDoc,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (description == null || description.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel description is required!");
            return "redirect:/hotels/new";
        }

        if (imageFile == null || imageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel image is required!");
            return "redirect:/hotels/new";
        }

        String imageContentType = imageFile.getContentType();
        if (imageContentType == null || !imageContentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel image must be an image file (JPG, PNG, WEBP, etc.)!");
            return "redirect:/hotels/new";
        }

        if (businessRegistrationDoc != null && !businessRegistrationDoc.isEmpty()) {
            String bizContentType = businessRegistrationDoc.getContentType();
            if (bizContentType == null || !bizContentType.equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Business Registration must be a PDF file!");
                return "redirect:/hotels/new";
            }
        }

        if (landCertificateDoc != null && !landCertificateDoc.isEmpty()) {
            String landContentType = landCertificateDoc.getContentType();
            if (landContentType == null || !landContentType.equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Land Certificate must be a PDF file!");
                return "redirect:/hotels/new";
            }
        }

        if (rentalContractDoc != null && !rentalContractDoc.isEmpty()) {
            String rentalContentType = rentalContractDoc.getContentType();
            if (rentalContentType == null || !rentalContentType.equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rental Contract must be a PDF file!");
                return "redirect:/hotels/new";
            }
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
            return "redirect:/hotels/new";
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

            HotelVerificationDocument doc = new HotelVerificationDocument();
            doc.setHotel(hotel);
            doc.setUploadStatus("PENDING");

            if (businessRegistrationDoc != null && !businessRegistrationDoc.isEmpty()) {
                try {
                    String path = saveUploadedFile(businessRegistrationDoc, "hotel_docs");
                    doc.setBusinessRegistrationDoc(path);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Failed to upload Business Registration: " + e.getMessage());
                    return "redirect:/hotels/new";
                }
            }

            if (landCertificateDoc != null && !landCertificateDoc.isEmpty()) {
                try {
                    String path = saveUploadedFile(landCertificateDoc, "hotel_docs");
                    doc.setLandCertificateDoc(path);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Failed to upload Land Certificate: " + e.getMessage());
                    return "redirect:/hotels/new";
                }
            }

            if (rentalContractDoc != null && !rentalContractDoc.isEmpty()) {
                try {
                    String path = saveUploadedFile(rentalContractDoc, "hotel_docs");
                    doc.setRentalContractDoc(path);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Failed to upload Rental Contract: " + e.getMessage());
                    return "redirect:/hotels/new";
                }
            }

            hotelVerificationDocumentRepository.save(doc);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Hotel \"" + name + "\" added successfully! Documents submitted for verification.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save hotel: " + e.getMessage());
            return "redirect:/hotels/new";
        }

        if ("ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/owner/dashboard?tab=hotels";
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

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            session.setAttribute("errorMessage",
                    "Your account is pending admin approval. You cannot add hotels yet.");
            return "redirect:/owner/dashboard";
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

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            session.setAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        model.addAttribute("hotel", hotel);
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        model.addAttribute("rooms", rooms);
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

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            session.setAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        int roomCount = rooms != null ? rooms.size() : 0;

        model.addAttribute("hotel", hotel);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("roomCount", roomCount);

        return "owner/hotel-edit";
    }

    @PostMapping("/owner/hotels/{id}/edit")
    public String updateOwnerHotel(
            @PathVariable("id") int hotelId,
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam("description") String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "active", required = false, defaultValue = "false") boolean active,
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
            return "redirect:/owner/hotels/" + hotelId + "/edit";
        }

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city);
        hotel.setDistrict(district);
        hotel.setDescription(description);
        hotel.setApprovalStatus("PENDING");
        hotel.setActive(false);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFilename = imageFile.getOriginalFilename();
                String safeFilename = System.currentTimeMillis() + "_"
                        + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "hotel.jpg");
                Path uploadPath = resolveStaticDir(HOTEL_IMAGE_SUBDIR);
                Path targetFile = uploadPath.resolve(safeFilename);
                Files.copy(imageFile.getInputStream(),
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING);
                hotel.setImageUrl(HOTEL_IMAGE_URL_PREFIX + safeFilename);

                Path classesPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "static", HOTEL_IMAGE_SUBDIR).toAbsolutePath().normalize();
                if (Files.exists(Paths.get(System.getProperty("user.dir"), "target", "classes", "static"))) {
                    if (!Files.exists(classesPath)) {
                        Files.createDirectories(classesPath);
                    }
                    Files.copy(targetFile, classesPath.resolve(safeFilename), StandardCopyOption.REPLACE_EXISTING);
                }
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

        if (!ownerService.isOwnerApproved(loggedInUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is pending admin approval.");
            return "redirect:/owner/dashboard";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new RuntimeException("Hotel not found or you don't have permission!"));

        try {
            String hotelName = hotel.getName();

            List<HotelVerificationDocument> docs = hotelVerificationDocumentRepository.findByHotelId(hotelId);
            if (!docs.isEmpty()) {
                hotelVerificationDocumentRepository.deleteAll(docs);
                hotelVerificationDocumentRepository.flush();
            }

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