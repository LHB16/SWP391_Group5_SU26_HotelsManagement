package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @GetMapping({"/booking", "/booking/create"})
    public String showCreateBookingPage(
            @RequestParam(name = "hotelId", required = false) Integer hotelId,
            @RequestParam(name = "roomId", required = false) Integer roomId,
            @RequestParam(name = "checkin", required = false) String checkin,
            @RequestParam(name = "checkout", required = false) String checkout,
            HttpSession session, 
            Model model) {
        
        if (roomId != null) {
            model.addAttribute("selectedRoomId", roomId);
        }
        // Lấy thông tin user đăng nhập từ Session (nếu có)
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

        // Lấy danh sách các phòng
        List<Room> rooms;
        if (hotelId != null) {
            rooms = roomRepository.findByHotelId(hotelId);
            model.addAttribute("selectedHotelId", hotelId);
        } else {
            rooms = roomRepository.findAll();
        }
        model.addAttribute("rooms", rooms);

        // Lấy danh sách khách sạn để map ID sang tên khách sạn dễ hiển thị
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = hotels.stream()
                .collect(Collectors.toMap(Hotel::getId, Hotel::getName, (h1, h2) -> h1));
        model.addAttribute("hotelMap", hotelMap);
        
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        
        // Trả về file giao diện templates/booking/create.html
        return "booking/create";
    }
}
