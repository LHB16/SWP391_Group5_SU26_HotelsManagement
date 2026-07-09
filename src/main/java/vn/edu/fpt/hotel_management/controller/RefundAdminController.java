package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Refund;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.entity.Payment;
import vn.edu.fpt.hotel_management.repository.RefundRepository;
import vn.edu.fpt.hotel_management.repository.PaymentRepository;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class RefundAdminController {

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private HotelOwnerRepository hotelOwnerRepository;

    @Autowired
    private HotelRepository hotelRepository;

    /**
     * Trang quản lý hoàn tiền cho Admin — hiển thị tất cả yêu cầu, có lọc theo trạng thái
     */
    @GetMapping("/admin/refunds")
    public String showRefundManagement(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }
        return "redirect:/admin/dashboard?tab=refundPanel";
    }

    /**
     * Admin cập nhật trạng thái yêu cầu hoàn tiền (PROCESSED hoặc REJECTED)
     */
    @PostMapping("/admin/refunds/{id}/update")
    public String updateRefundStatus(
            @PathVariable("id") int refundId,
            @RequestParam("newStatus") String newStatus,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Refund refund = refundRepository.findById(refundId).orElse(null);
        if (refund == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Refund request not found.");
            return "redirect:/admin/dashboard?tab=refundPanel";
        }

        // Chỉ cho phép cập nhật nếu còn PENDING
        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "This refund request has already been processed.");
            return "redirect:/admin/dashboard?tab=refundPanel";
        }

        String normalizedStatus = newStatus.toUpperCase();
        if (!normalizedStatus.equals("PROCESSED") && !normalizedStatus.equals("REJECTED")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status value.");
            return "redirect:/admin/dashboard?tab=refundPanel";
        }

        refund.setStatus(normalizedStatus);
        refund.setNote(note != null ? note.trim() : null);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        // Cập nhật trạng thái Payment liên quan dựa trên quyết định duyệt/từ chối của Admin
        Booking booking = refund.getBooking();
        if (booking != null) {
            Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            if (payment != null) {
                if ("PROCESSED".equals(normalizedStatus)) {
                    payment.setStatus("REFUNDED");
                } else if ("REJECTED".equals(normalizedStatus)) {
                    payment.setStatus("PAID");
                }
                paymentRepository.save(payment);
            }
        }

        String message = normalizedStatus.equals("PROCESSED")
                ? "Refund #" + refundId + " marked as Processed successfully."
                : "Refund #" + refundId + " has been Rejected.";
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/dashboard?tab=refundPanel";
    }
}
