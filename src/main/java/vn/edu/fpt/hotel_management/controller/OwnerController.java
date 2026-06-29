// src/main/java/vn/edu/fpt/hotel_management/controller/OwnerController.java
package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.fpt.hotel_management.entity.User;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        // 1. Lấy user từ session
        User user = (User) session.getAttribute("loggedInUser");

        // 2. Kiểm tra đã đăng nhập chưa?
        if (user == null) {
            return "redirect:/login";
        }


        if (!"HOTEL_OWNER".equals(user.getRole())) {
            // Nếu không phải HOTEL_OWNER -> về homepage
            return "redirect:/home";
        }

        // 4. Đúng HOTEL_OWNER -> hiển thị dashboard
        model.addAttribute("user", user);
        return "owner/dashboard";
    }
}