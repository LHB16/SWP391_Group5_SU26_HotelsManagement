package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.hotel_management.entity.Banner;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.Promotion;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.BannerRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.PromotionRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @GetMapping({"/", "/home"})
    public String showHomePage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

        // 1. Load active banners
        List<Banner> activeBanners = bannerRepository.findByActiveTrue();
        model.addAttribute("banners", activeBanners);

        // 2. Load top rated hotels (rating desc, limit to 6)
        List<Hotel> topRatedHotels = hotelRepository.findTop6ByActiveTrueAndApprovalStatusOrderByRatingDesc("APPROVED");
        model.addAttribute("topRatedHotels", topRatedHotels);

        // 3. Load hotels with active promotions (ordered by discountPercent desc, limit to 6 in Java stream)
        List<Hotel> promoHotels = hotelRepository.findHotelsWithActivePromotions(LocalDate.now());
        if (promoHotels.size() > 6) {
            promoHotels = promoHotels.subList(0, 6);
        }
        model.addAttribute("promoHotels", promoHotels);

        // 4. Build a map of hotelId -> best discount percent for the promo badge
        Map<Integer, Integer> promoDiscountMap = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (Hotel h : promoHotels) {
            List<Promotion> promos = promotionRepository.findActivePromotionsByHotelId(h.getId(), today);
            promos.stream()
                  .filter(p -> p.getDiscountPercent() != null)
                  .mapToInt(p -> p.getDiscountPercent().intValue())
                  .max()
                  .ifPresent(max -> promoDiscountMap.put(h.getId(), max));
        }
        model.addAttribute("promoDiscountMap", promoDiscountMap);

        return "homepage/home";
    }
}
