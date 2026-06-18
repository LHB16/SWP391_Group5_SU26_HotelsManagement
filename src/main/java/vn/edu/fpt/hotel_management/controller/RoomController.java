package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.Room;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;

import java.util.List;

@Controller
public class RoomController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public RoomController(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    /**
     * Hiển thị trang danh sách phòng của một khách sạn.
     * URL: GET /hotels/{id}/rooms
     *
     * @param id       ID của khách sạn
     * @param types    Danh sách loại phòng được chọn để lọc (multi-select)
     * @param minPrice Giá tối thiểu (mặc định 0 VND)
     * @param maxPrice Giá tối đa (mặc định 50,000,000 VND)
     */
    @GetMapping("/hotels/{id}/rooms")
    public String showRoomsPage(
            @PathVariable("id") int id,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "minPrice", required = false, defaultValue = "0") double minPrice,
            @RequestParam(value = "maxPrice", required = false, defaultValue = "50000000") double maxPrice,
            HttpSession session,
            Model model
    ) {
        // Lấy thông tin khách sạn từ database
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) {
            return "redirect:/hotels";
        }

        // Lấy danh sách phòng theo bộ lọc
        List<Room> rooms;
        if (types == null || types.isEmpty()) {
            rooms = roomRepository.findByHotelIdAndPriceRange(id, minPrice, maxPrice);
        } else {
            rooms = roomRepository.filterByHotelAndTypesAndPrice(id, types, minPrice, maxPrice);
        }

        // Lấy danh sách loại phòng duy nhất để hiển thị trên filter bar
        List<String> allTypes = roomRepository.findDistinctTypesByHotelId(id);

        // Truyền dữ liệu sang view
        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", rooms);
        model.addAttribute("allTypes", allTypes);
        model.addAttribute("selectedTypes", types != null ? types : List.of());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("totalResults", rooms.size());

        // Thông tin user đăng nhập (nếu có)
        model.addAttribute("user", session.getAttribute("loggedInUser"));

        return "hotel/rooms";
    }
}
