package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.hotel_management.entity.User;

@Controller
public class HomeController {

    @GetMapping("/")
    public String showHomePage(HttpSession session, Model model) {
        // Lấy thông tin người dùng đã đăng nhập từ Session
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        // Nếu đã đăng nhập, gửi thông tin user sang giao diện HTML
        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }
        
        // Trả về file giao diện home.html trong thư mục templates
        return "home";
    }
}
