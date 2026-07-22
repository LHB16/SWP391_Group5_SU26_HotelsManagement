package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogoutController {

    // =====================================================
    // CHỨC NĂNG: ĐĂNG XUẤT (LOGOUT)
    // Mô tả: Xóa toàn bộ dữ liệu phiên làm việc (Session) của người dùng hiện tại
    // và điều hướng người dùng về trang đăng nhập với thông báo đăng xuất thành công.
    // =====================================================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Hủy bỏ session hiện tại để xóa sạch các thông tin lưu trữ tạm thời (như loggedInUser)
        session.invalidate();
        
        // Điều hướng (Redirect) về trang đăng nhập kèm theo tham số báo đăng xuất
        return "redirect:/login?logout";
    }
}