package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Wishlist;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.WishlistRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final RoomRepository roomRepository;

    public WishlistController(WishlistRepository wishlistRepository, RoomRepository roomRepository) {
        this.wishlistRepository = wishlistRepository;
        this.roomRepository = roomRepository;
    }

    // ======================== GET /wishlist ========================
    @GetMapping("/wishlist")
    public String showWishlistPage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        List<Wishlist> wishlists = wishlistRepository.findByCustomerIdOrderByAddedAtDesc(loggedInUser.getId());

        // Tính toán số đêm và giá thực tế của từng wishlist room dựa trên filter ngày lưu kèm
        Map<Integer, BigDecimal> wishlistPricesMap = new HashMap<>();
        Map<Integer, Long> wishlistNightsMap = new HashMap<>();
        Map<Integer, Boolean> wishlistFilteredMap = new HashMap<>();

        for (Wishlist wl : wishlists) {
            Room room = wl.getRoom();
            LocalDate d1 = wl.getCheckInDate();
            LocalDate d2 = wl.getCheckOutDate();

            if (d1 != null && d2 != null && d2.isAfter(d1)) {
                long nights = ChronoUnit.DAYS.between(d1, d2);
                BigDecimal actualPrice = calculateRoomSubtotal(room.getPrice(), d1, d2);
                
                wishlistPricesMap.put(wl.getId(), actualPrice);
                wishlistNightsMap.put(wl.getId(), nights);
                wishlistFilteredMap.put(wl.getId(), true);
            } else {
                wishlistPricesMap.put(wl.getId(), room.getPrice());
                wishlistNightsMap.put(wl.getId(), 1L);
                wishlistFilteredMap.put(wl.getId(), false);
            }
        }

        model.addAttribute("wishlists", wishlists);
        model.addAttribute("wishlistPricesMap", wishlistPricesMap);
        model.addAttribute("wishlistNightsMap", wishlistNightsMap);
        model.addAttribute("wishlistFilteredMap", wishlistFilteredMap);
        model.addAttribute("user", loggedInUser);

        return "hotel/wishlist";
    }

    // ======================== GET /wishlist/toggle (AJAX API) ========================
    // ======================== GET /wishlist/toggle ========================
    @GetMapping("/wishlist/toggle")
    public String toggleWishlist(
            @RequestParam("roomId") int roomId,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "types", required = false) String types,
            HttpSession session
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            Optional<Wishlist> existing = wishlistRepository.findByCustomerIdAndRoomId(loggedInUser.getId(), roomId);
            if (existing.isPresent()) {
                wishlistRepository.delete(existing.get());
            } else {
                LocalDate inDate = null;
                LocalDate outDate = null;
                if (checkin != null && !checkin.trim().isEmpty() && checkout != null && !checkout.trim().isEmpty()) {
                    try {
                        inDate = LocalDate.parse(checkin.trim());
                        outDate = LocalDate.parse(checkout.trim());
                    } catch (Exception e) {
                        // ignore
                    }
                }
                Wishlist wl = new Wishlist(loggedInUser, room, inDate, outDate);
                wishlistRepository.save(wl);
            }
            
            StringBuilder redirectUrl = new StringBuilder("redirect:/hotels/" + room.getHotelId() + "/rooms");
            boolean hasParam = false;
            if (checkin != null && !checkin.trim().isEmpty()) {
                redirectUrl.append("?checkin=").append(checkin);
                hasParam = true;
            }
            if (checkout != null && !checkout.trim().isEmpty()) {
                redirectUrl.append(hasParam ? "&" : "?").append("checkout=").append(checkout);
                hasParam = true;
            }
            if (types != null && !types.trim().isEmpty()) {
                redirectUrl.append(hasParam ? "&" : "?").append("types=").append(types);
            }
            return redirectUrl.toString();
        }
        return "redirect:/hotels";
    }

    // ======================== GET /wishlist/remove ========================
    @GetMapping("/wishlist/remove")
    public String removeWishlist(@RequestParam("id") int id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Optional<Wishlist> wl = wishlistRepository.findById(id);
        if (wl.isPresent() && wl.get().getCustomer().getId() == loggedInUser.getId()) {
            wishlistRepository.delete(wl.get());
        }
        return "redirect:/wishlist";
    }

    // ======================== GET /wishlist/update-filter ========================
    @GetMapping("/wishlist/update-filter")
    public String updateWishlistFilter(
            @RequestParam("id") int id,
            @RequestParam("checkin") String checkin,
            @RequestParam("checkout") String checkout,
            HttpSession session
    ) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Optional<Wishlist> wlOpt = wishlistRepository.findById(id);
        if (wlOpt.isPresent() && wlOpt.get().getCustomer().getId() == loggedInUser.getId()) {
            Wishlist wl = wlOpt.get();
            LocalDate inDate = null;
            LocalDate outDate = null;
            if (checkin != null && !checkin.trim().isEmpty() && checkout != null && !checkout.trim().isEmpty()) {
                try {
                    inDate = LocalDate.parse(checkin.trim());
                    outDate = LocalDate.parse(checkout.trim());
                } catch (Exception e) {
                    // ignore
                }
            }
            wl.setCheckInDate(inDate);
            wl.setCheckOutDate(outDate);
            wishlistRepository.save(wl);
        }
        return "redirect:/wishlist";
    }

    // ======================== Helper Methods ========================
    private boolean isHolidayOrWeekend(LocalDate date) {
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

    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, LocalDate checkin, LocalDate checkout) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate temp = checkin;
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
