package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.Promotion;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.service.OwnerService;
import vn.edu.fpt.hotel_management.service.PromotionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/owner/promotions")
public class PromotionController {

    private final PromotionService promotionService;
    private final HotelRepository hotelRepository;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final OwnerService ownerService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PromotionController(PromotionService promotionService,
                               HotelRepository hotelRepository,
                               HotelOwnerRepository hotelOwnerRepository,
                               OwnerService ownerService) {
        this.promotionService = promotionService;
        this.hotelRepository = hotelRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.ownerService = ownerService;
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> getPromotions(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            response.put("error", "Unauthorized");
            return response;
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            response.put("error", "Owner not found");
            return response;
        }

        List<Hotel> hotels = hotelRepository.findByOwnerId(owner.getId());
        List<Promotion> allPromotions = promotionService.getPromotionsByOwnerId(owner.getId());

        List<Map<String, Object>> hotelList = new ArrayList<>();
        for (Hotel h : hotels) {
            Map<String, Object> hotelMap = new HashMap<>();
            hotelMap.put("id", h.getId());
            hotelMap.put("name", h.getName());
            hotelList.add(hotelMap);
        }
        response.put("hotels", hotelList);

        Map<Integer, List<Map<String, Object>>> promoMap = new HashMap<>();
        for (Promotion p : allPromotions) {
            int hotelId = p.getHotel().getId();
            Map<String, Object> promoData = new HashMap<>();
            promoData.put("id", p.getId());
            promoData.put("title", p.getTitle());
            promoData.put("description", p.getDescription());
            promoData.put("discountPercent", p.getDiscountPercent());
            promoData.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
            promoData.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
            promoData.put("status", p.getStatus());
            promoData.put("hotelId", hotelId);

            promoMap.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(promoData);
        }
        response.put("promotionsByHotel", promoMap);

        return response;
    }

    @PostMapping("/create")
    public String createPromotion(@RequestParam int hotelId,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam BigDecimal discountPercent,
                                  @RequestParam String startDate,
                                  @RequestParam String endDate,
                                  @RequestParam(required = false) String status,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner not found");
            return "redirect:/owner/dashboard?tab=promotions";
        }

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId()).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found");
            return "redirect:/owner/dashboard?tab=promotions";
        }

        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

            String finalStatus = (status != null && !status.isEmpty()) ? status : "ACTIVE";

            promotionService.createPromotion(
                    hotelId, title, description, discountPercent, start, end, finalStatus
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Promotion \"" + title + "\" created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to create promotion: " + e.getMessage());
        }

        return "redirect:/owner/dashboard?tab=promotions";
    }

    @PostMapping("/update")
    public String updatePromotion(@RequestParam int promotionId,
                                  @RequestParam int hotelId,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam BigDecimal discountPercent,
                                  @RequestParam String startDate,
                                  @RequestParam String endDate,
                                  @RequestParam(required = false) String status,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner not found");
            return "redirect:/owner/dashboard?tab=promotions";
        }

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId()).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found");
            return "redirect:/owner/dashboard?tab=promotions";
        }

        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

            String finalStatus = (status != null && !status.isEmpty()) ? status : "ACTIVE";

            promotionService.updatePromotion(
                    promotionId, hotelId, title, description, discountPercent, start, end, finalStatus
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Promotion \"" + title + "\" updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update promotion: " + e.getMessage());
        }

        return "redirect:/owner/dashboard?tab=promotions";
    }

    @PostMapping("/delete")
    public String deletePromotion(@RequestParam int promotionId,
                                  @RequestParam int hotelId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Owner not found");
            return "redirect:/owner/dashboard?tab=promotions";
        }

        Hotel hotel = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId()).orElse(null);
        if (hotel == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hotel not found");
            return "redirect:/owner/dashboard?tab=promotions";
        }

        try {
            promotionService.deletePromotion(promotionId, hotelId);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete promotion: " + e.getMessage());
        }

        return "redirect:/owner/dashboard?tab=promotions";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Map<String, Object> getPromotion(@PathVariable int id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            response.put("error", "Unauthorized");
            return response;
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            response.put("error", "Owner not found");
            return response;
        }

        Promotion promotion = promotionService.getPromotionById(id).orElse(null);
        if (promotion == null) {
            response.put("error", "Promotion not found");
            return response;
        }

        Hotel hotel = hotelRepository.findByIdAndOwnerId(promotion.getHotel().getId(), owner.getId()).orElse(null);
        if (hotel == null) {
            response.put("error", "Access denied");
            return response;
        }

        response.put("id", promotion.getId());
        response.put("hotelId", promotion.getHotel().getId());
        response.put("title", promotion.getTitle());
        response.put("description", promotion.getDescription());
        response.put("discountPercent", promotion.getDiscountPercent());
        response.put("startDate", promotion.getStartDate() != null ? promotion.getStartDate().toString() : "");
        response.put("endDate", promotion.getEndDate() != null ? promotion.getEndDate().toString() : "");
        response.put("status", promotion.getStatus());

        return response;
    }
}