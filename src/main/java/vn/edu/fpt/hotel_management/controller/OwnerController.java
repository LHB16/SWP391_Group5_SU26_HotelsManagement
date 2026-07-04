// OwnerController.java - Thêm mapping cho Owner Dashboard
package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final HotelOwnerRepository hotelOwnerRepository;

    public OwnerController(HotelOwnerRepository hotelOwnerRepository) {
        this.hotelOwnerRepository = hotelOwnerRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(user.getRole())) {
            return "redirect:/home";
        }

        // Lấy thông tin owner để hiển thị tên
        HotelOwner owner = hotelOwnerRepository.findByUserAccount(user).orElse(null);
        if (owner != null) {
            model.addAttribute("owner", owner);
        }
        model.addAttribute("user", user);
        return "owner/dashboard";
    }
}