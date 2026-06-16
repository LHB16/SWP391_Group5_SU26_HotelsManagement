package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.hotel_management.entity.User;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String showHomePage(HttpSession session, Model model) {
        // Lấy thông tin người dùng đã đăng nhập từ Session
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        // Nếu đã đăng nhập, gửi thông tin user sang giao diện HTML
        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }
        
        // Trả về file giao diện HomePage/home.html trong thư mục templates
        return "HomePage/home";
    }

    // Xử lý chuyển hướng đến trang View Profile
    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        // Lấy thông tin user đăng nhập từ Session
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        // Nếu chưa đăng nhập, chuyển hướng người dùng về trang đăng nhập
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        // Gửi thông tin user sang giao diện Profile
        model.addAttribute("user", loggedInUser);
        
        // Trả về file giao diện User/profile.html trong thư mục templates
        return "User/profile";
    }
}
