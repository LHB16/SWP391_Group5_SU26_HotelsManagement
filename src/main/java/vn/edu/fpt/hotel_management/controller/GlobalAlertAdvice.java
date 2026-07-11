package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.fpt.hotel_management.repository.MessageRepository;
import vn.edu.fpt.hotel_management.entity.User;

@ControllerAdvice
public class GlobalAlertAdvice {

    private final MessageRepository messageRepository;

    public GlobalAlertAdvice(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @ModelAttribute
    public void handleAlerts(HttpSession session, Model model) {
        String successMessage = (String) session.getAttribute("successMessage");
        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
            session.removeAttribute("successMessage");
        }

        String errorMessage = (String) session.getAttribute("errorMessage");
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            session.removeAttribute("errorMessage");
        }

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            long unreadMessageCount = messageRepository.countByReceiverIdAndIsReadFalse(loggedInUser.getId());
            model.addAttribute("unreadMessageCount", unreadMessageCount);
        }
    }
}
