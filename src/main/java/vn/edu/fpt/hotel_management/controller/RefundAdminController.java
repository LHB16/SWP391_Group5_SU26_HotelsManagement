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
import vn.edu.fpt.hotel_management.repository.RefundRepository;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class RefundAdminController {

    @Autowired
    private RefundRepository refundRepository;

    /**
     * Trang quản lý hoàn tiền cho Admin — hiển thị tất cả yêu cầu, có lọc theo trạng thái
     */
    @GetMapping("/admin/refunds")
    public String showRefundManagement(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session,
            Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        List<Refund> refunds;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            refunds = refundRepository.findByStatusOrderByRequestedAtDesc(status.toUpperCase());
        } else {
            refunds = refundRepository.findAllByOrderByRequestedAtDesc();
        }

        // Đếm số lượng theo từng trạng thái
        long pendingCount   = refundRepository.findByStatusOrderByRequestedAtDesc("PENDING").size();
        long processedCount = refundRepository.findByStatusOrderByRequestedAtDesc("PROCESSED").size();
        long rejectedCount  = refundRepository.findByStatusOrderByRequestedAtDesc("REJECTED").size();

        model.addAttribute("user", loggedInUser);
        model.addAttribute("refunds", refunds);
        model.addAttribute("filterStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("processedCount", processedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalCount", refunds.size());

        return "admin/refund-management";
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
            return "redirect:/admin/refunds";
        }

        // Chỉ cho phép cập nhật nếu còn PENDING
        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "This refund request has already been processed.");
            return "redirect:/admin/refunds";
        }

        String normalizedStatus = newStatus.toUpperCase();
        if (!normalizedStatus.equals("PROCESSED") && !normalizedStatus.equals("REJECTED")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status value.");
            return "redirect:/admin/refunds";
        }

        refund.setStatus(normalizedStatus);
        refund.setNote(note != null ? note.trim() : null);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        String message = normalizedStatus.equals("PROCESSED")
                ? "Refund #" + refundId + " marked as Processed successfully."
                : "Refund #" + refundId + " has been Rejected.";
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/refunds";
    }
}
