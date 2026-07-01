package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.BookingRepository;
import vn.edu.fpt.hotel_management.repository.PaymentRepository;
import vn.edu.fpt.hotel_management.repository.RoomRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class BookingController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Hiển thị trang lịch sử đặt phòng của người dùng đang đăng nhập
    @GetMapping("/booking/history")
    public String showBookingHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        // Kiểm tra đăng nhập, chưa đăng nhập thì redirect về trang login
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Lấy danh sách booking của user, sắp xếp mới nhất trước, phân trang 6 booking/trang
        int pageSize = 6;
        Page<Booking> bookingPage = bookingRepository.findByCustomerId(
                loggedInUser.getId(),
                PageRequest.of(page, pageSize)
        );

        // Tạo map hotelId -> tên khách sạn để hiển thị trên card
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = new HashMap<>();
        for (Hotel hotel : hotels) {
            hotelMap.put(hotel.getId(), hotel.getName());
        }

        // Truyền dữ liệu vào model để render template
        model.addAttribute("user", loggedInUser);
        model.addAttribute("bookings", bookingPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingPage.getTotalPages());
        model.addAttribute("totalItems", bookingPage.getTotalElements());
        model.addAttribute("hotelMap", hotelMap);

        return "booking/history";
    }

    // Hủy booking: chỉ cho phép hủy khi booking đang ở trạng thái CONFIRMED (đã thanh toán) và trong vòng 24 giờ kể từ lúc đặt
    @PostMapping("/booking/cancel/{id}")
    public String cancelBooking(
            @PathVariable("id") int bookingId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Tìm booking theo id
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null && booking.getCustomer().getId() == loggedInUser.getId()) {
            if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                // Kiểm tra điều kiện 24 giờ
                if (booking.getCreatedAt() != null) {
                    long hours = java.time.Duration.between(booking.getCreatedAt(), java.time.LocalDateTime.now()).toHours();
                    if (hours < 24) {
                        booking.setStatus("CANCELLED");
                        booking.setUpdatedAt(java.time.LocalDateTime.now());
                        bookingRepository.save(booking);

                        // Cập nhật trạng thái payment tương ứng nếu có
                        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
                        if (payment != null) {
                            payment.setStatus("REFUNDED");
                            paymentRepository.save(payment);
                        }

                        // Tính số tiền hoàn lại (80%)
                        java.math.BigDecimal refundAmount = booking.getTotalPrice().multiply(java.math.BigDecimal.valueOf(0.8));
                        String formattedRefund = String.format("%,.0f", refundAmount.doubleValue());
                        
                        redirectAttributes.addFlashAttribute("successMessage", 
                            "Booking cancelled successfully! 80% of your payment (" + formattedRefund + " ₫) will be refunded shortly.");
                    } else {
                        redirectAttributes.addFlashAttribute("errorMessage", 
                            "Cannot cancel this booking because it has been more than 24 hours since the booking was created.");
                    }
                } else {
                    // Nếu không có thông tin createdAt, cho phép hủy mặc định
                    booking.setStatus("CANCELLED");
                    booking.setUpdatedAt(java.time.LocalDateTime.now());
                    bookingRepository.save(booking);
                    redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully!");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Only bookings in 'Pending' status (confirmed payment) can be cancelled.");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
        }

        return "redirect:/booking/history";
    }

    // Xóa booking khỏi lịch sử của người dùng (xóa vĩnh viễn khỏi database)
    // Xóa payment trước (nếu có) để tránh lỗi foreign key, sau đó mới xóa booking
    @PostMapping("/booking/delete/{id}")
    public String deleteBooking(
            @PathVariable("id") int bookingId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Chỉ xóa nếu booking thuộc về người dùng hiện tại
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null && booking.getCustomer().getId() == loggedInUser.getId()) {
            // Giải phóng liên kết 2 chiều để tránh lỗi TransientPropertyValueException của Hibernate
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
            if (payment != null) {
                booking.setPayment(null);
                bookingRepository.saveAndFlush(booking); // Đồng bộ trạng thái Booking
                paymentRepository.delete(payment);
                paymentRepository.flush();
            }
            // Xóa booking
            bookingRepository.delete(booking);
            redirectAttributes.addFlashAttribute("successMessage", "Booking history record removed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking record not found or access denied.");
        }

        return "redirect:/booking/history";
    }

    @GetMapping({"/booking", "/booking/create"})
    public String showCreateBookingPage(
            @RequestParam(name = "hotelId", required = false) Integer hotelId,
            @RequestParam(name = "roomId", required = false) Integer roomId,
            @RequestParam(name = "roomIds", required = false) List<Integer> roomIds,
            @RequestParam(name = "checkin", required = false) String checkin,
            @RequestParam(name = "checkout", required = false) String checkout,
            HttpSession session, 
            Model model) {
        
        // Lấy thông tin user đăng nhập từ Session (nếu có)
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

        // Lấy danh sách các phòng (chỉ lấy phòng được chọn nếu roomId hoặc roomIds có giá trị)
        List<Room> rooms = new ArrayList<>();
        if (roomId != null) {
            Room r = roomRepository.findById(roomId).orElse(null);
            if (r != null) {
                rooms.add(r);
            }
            if (hotelId == null && r != null) {
                hotelId = r.getHotelId();
            }
            model.addAttribute("selectedHotelId", hotelId);
        } else if (roomIds != null && !roomIds.isEmpty()) {
            for (Integer rId : roomIds) {
                Room r = roomRepository.findById(rId).orElse(null);
                if (r != null) {
                    rooms.add(r);
                }
            }
            if (hotelId == null && !rooms.isEmpty()) {
                hotelId = rooms.get(0).getHotelId();
            }
            model.addAttribute("selectedHotelId", hotelId);
        } else if (hotelId != null) {
            rooms = roomRepository.findByHotelId(hotelId);
            model.addAttribute("selectedHotelId", hotelId);
        } else {
            rooms = roomRepository.findAll();
        }
        model.addAttribute("rooms", rooms);

        // Lấy danh sách khách sạn để map ID sang tên khách sạn dễ hiển thị
        List<Hotel> hotels = hotelRepository.findAll();
        Map<Integer, String> hotelMap = new HashMap<>();
        for (Hotel hotel : hotels) {
            hotelMap.put(hotel.getId(), hotel.getName());
        }
        model.addAttribute("hotelMap", hotelMap);
        
        // Tính toán số đêm dựa trên ngày check-in và check-out
        long nights = 1;
        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                java.time.LocalDate d1 = java.time.LocalDate.parse(checkin);
                java.time.LocalDate d2 = java.time.LocalDate.parse(checkout);
                if (d2.isAfter(d1)) {
                    nights = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
                } else {
                    d2 = d1.plusDays(1);
                    checkout = d2.toString();
                    nights = 1;
                }
            } catch (Exception e) {
                // Parse error, defaults will be handled below
            }
        } else {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);
            checkin = today.toString();
            checkout = tomorrow.toString();
            nights = 1;
        }

        // Chuẩn bị danh sách phòng được chọn
        List<Integer> selectedRoomIds = new ArrayList<>();
        if (roomIds != null) {
            selectedRoomIds.addAll(roomIds);
        }
        if (roomId != null && !selectedRoomIds.contains(roomId)) {
            selectedRoomIds.add(roomId);
        }

        // Tính toán chi phí bằng Java có tăng 20% vào ngày lễ/cuối tuần
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Integer, BigDecimal> roomPricesMap = new java.util.HashMap<>();
        if (checkin != null && checkout != null && !checkin.trim().isEmpty() && !checkout.trim().isEmpty()) {
            try {
                java.time.LocalDate d1 = java.time.LocalDate.parse(checkin.trim());
                java.time.LocalDate d2 = java.time.LocalDate.parse(checkout.trim());
                
                // Tính giá thực tế (tổng tiền) cho tất cả các phòng hiển thị
                for (Room r : rooms) {
                    BigDecimal actualSubtotal = calculateRoomSubtotal(r.getPrice(), d1, d2);
                    roomPricesMap.put(r.getId(), actualSubtotal);
                }

                // Cộng dồn subtotal cho các phòng được chọn
                for (Integer rId : selectedRoomIds) {
                    Room r = roomRepository.findById(rId).orElse(null);
                    if (r != null) {
                        subtotal = subtotal.add(calculateRoomSubtotal(r.getPrice(), d1, d2));
                    }
                }
            } catch (Exception e) {
                System.err.println("BookingController subtotal date parse error: " + e.getMessage());
                for (Room r : rooms) {
                    roomPricesMap.put(r.getId(), r.getPrice().multiply(BigDecimal.valueOf(nights)));
                }
                for (Integer rId : selectedRoomIds) {
                    Room r = roomRepository.findById(rId).orElse(null);
                    if (r != null) {
                        subtotal = subtotal.add(r.getPrice().multiply(BigDecimal.valueOf(nights)));
                    }
                }
            }
        } else {
            for (Room r : rooms) {
                roomPricesMap.put(r.getId(), r.getPrice().multiply(BigDecimal.valueOf(nights)));
            }
            for (Integer rId : selectedRoomIds) {
                Room r = roomRepository.findById(rId).orElse(null);
                if (r != null) {
                    subtotal = subtotal.add(r.getPrice().multiply(BigDecimal.valueOf(nights)));
                }
            }
        }
        model.addAttribute("roomPricesMap", roomPricesMap);

        BigDecimal serviceFee = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50000) : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(tax).add(serviceFee);

        // Truyền các giá trị tính toán vào model
        model.addAttribute("today", java.time.LocalDate.now().toString());
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("nights", nights);
        model.addAttribute("selectedRoomIds", selectedRoomIds);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("serviceFee", serviceFee);
        model.addAttribute("grandTotal", grandTotal);
        
        // Trả về file giao diện templates/booking/create.html
        return "booking/create";
    }

    private boolean isHolidayOrWeekend(java.time.LocalDate date) {
        // 1. Kiểm tra cuối tuần (Thứ 7 & Chủ Nhật)
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        // 2. Kiểm tra ngày lễ
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        // Lễ dương lịch VN cố định
        if (m == 1 && d == 1) return true;   // Tết Dương Lịch
        if (m == 4 && d == 30) return true;  // Giải phóng Miền Nam
        if (m == 5 && d == 1) return true;   // Quốc tế Lao động
        if (m == 9 && d == 2) return true;   // Quốc khánh

        // Các ngày lễ đặc biệt yêu cầu thêm
        if (m == 2 && d == 14) return true;  // Valentine
        if (m == 3 && d == 8) return true;   // Quốc tế Phụ nữ
        if (m == 6 && d == 1) return true;   // Quốc tế Thiếu nhi
        if (m == 10 && d == 20) return true; // Phụ nữ VN
        if (m == 11 && d == 20) return true; // Nhà giáo VN
        if (m == 12 && d == 25) return true; // Giáng sinh

        // Tết Âm Lịch năm 2025 (Từ 28/01 đến 03/02/2025)
        if (date.getYear() == 2025) {
            if (m == 1 && d >= 28) return true;
            if (m == 2 && d <= 3) return true;
        }
        // Tết Âm Lịch năm 2026 (Từ 16/02 đến 22/02/2026)
        if (date.getYear() == 2026) {
            if (m == 2 && d >= 16 && d <= 22) return true;
        }

        return false;
    }

    private BigDecimal calculateRoomSubtotal(BigDecimal basePrice, java.time.LocalDate checkin, java.time.LocalDate checkout) {
        BigDecimal total = BigDecimal.ZERO;
        java.time.LocalDate temp = checkin;
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
