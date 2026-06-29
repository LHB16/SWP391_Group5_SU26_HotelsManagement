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

        // KHÔNG KIỂM TRA ĐĂNG NHẬP - AI CŨNG VÀO ĐƯỢC

        // Tạo user giả để hiển thị giao diện
        User fakeUser = new User();
        fakeUser.setId(1);
        fakeUser.setFullName("Hotel Owner");
        fakeUser.setUsername("owner");
        fakeUser.setEmail("owner@gmail.com");
        fakeUser.setRole("HOTEL_OWNER");

        model.addAttribute("user", fakeUser);
        return "owner/dashboard";
    }
}