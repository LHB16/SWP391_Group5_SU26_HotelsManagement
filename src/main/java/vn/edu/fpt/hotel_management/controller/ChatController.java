package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private final MessageRepository messageRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final HotelOwnerRepository hotelOwnerRepository;

    public ChatController(MessageRepository messageRepository,
                          HotelRepository hotelRepository,
                          UserRepository userRepository,
                          HotelOwnerRepository hotelOwnerRepository) {
        this.messageRepository = messageRepository;
        this.hotelRepository = hotelRepository;
        this.userRepository = userRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
    }

    // ===================== KHÁCH HÀNG (CUSTOMER) =====================

    // Giao diện chính của khung Chat phía khách hàng
    @GetMapping("/customer/chat")
    public String customerChatPage(@RequestParam("hotelId") int hotelId,
                                   HttpSession session,
                                   Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return "redirect:/hotels";
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("user", loggedInUser);
        return "booking/chat";
    }

    // Trang nội dung tin nhắn của khách hàng (hiển thị trong iframe để auto-refresh)
    @GetMapping("/customer/chat/messages")
    public String customerChatMessages(@RequestParam("hotelId") int hotelId,
                                       HttpSession session,
                                       Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "auth/login"; // Trả về trang đăng nhập đơn giản cho iframe
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return "booking/chat-messages";
        }

        int customerUserId = loggedInUser.getId();
        int ownerUserId = hotel.getOwner().getUserAccount().getId();

        // Đánh dấu toàn bộ tin nhắn nhận được (chủ nhà gửi cho khách hàng này) thành Đã đọc
        List<Message> unreadMessages = messageRepository.findByHotelIdAndSenderIdAndReceiverIdAndIsReadFalse(hotelId, ownerUserId, customerUserId);
        if (!unreadMessages.isEmpty()) {
            for (Message msg : unreadMessages) {
                msg.setRead(true);
            }
            messageRepository.saveAll(unreadMessages);
        }

        // Lấy lịch sử chat
        List<Message> chatHistory = messageRepository.findByHotelIdAndSenderIdAndReceiverIdOrHotelIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                hotelId, customerUserId, ownerUserId,
                hotelId, ownerUserId, customerUserId
        );

        model.addAttribute("chatHistory", chatHistory);
        model.addAttribute("currentUser", loggedInUser);
        model.addAttribute("refreshUrl", "/customer/chat/messages?hotelId=" + hotelId + "#bottom");
        return "booking/chat-messages";
    }

    // Gửi tin nhắn từ phía Khách hàng
    @PostMapping("/customer/chat/send")
    public String customerSendMessage(@RequestParam("hotelId") int hotelId,
                                      @RequestParam(value = "content", required = false) String content,
                                      @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                      HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel != null) {
            boolean hasContent = content != null && !content.trim().isEmpty();
            boolean hasImage = imageFile != null && !imageFile.isEmpty();
            
            if (hasContent || hasImage) {
                Message message = new Message();
                message.setSender(loggedInUser);
                message.setReceiver(hotel.getOwner().getUserAccount());
                message.setHotel(hotel);
                if (hasContent) {
                    message.setContent(content.trim());
                }
                if (hasImage) {
                    String savedFileName = saveUploadedImage(imageFile);
                    if (savedFileName != null) {
                        message.setImageUrl(savedFileName);
                    }
                }
                message.setRead(false);
                message.setSentAt(LocalDateTime.now());
                messageRepository.save(message);
            }
        }

        return "redirect:/customer/chat?hotelId=" + hotelId;
    }

    // ===================== CHỦ KHÁCH SẠN (OWNER) =====================

    // Giao diện Inbox của chủ khách sạn (Khớp với liên kết trên navbar)
    @GetMapping("/chat/inbox")
    public String ownerChatPage(@RequestParam(value = "hotelId", required = false) Integer hotelId,
                                @RequestParam(value = "customerId", required = false) Integer customerId,
                                HttpSession session,
                                Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (!"HOTEL_OWNER".equals(loggedInUser.getRole())) {
            return "redirect:/home";
        }

        HotelOwner owner = hotelOwnerRepository.findByUserAccount(loggedInUser).orElse(null);
        if (owner == null) {
            return "redirect:/home";
        }

        // Lấy danh sách khách sạn của chủ nhà
        List<Hotel> ownerHotels = hotelRepository.findByOwnerId(owner.getId());
        model.addAttribute("hotels", ownerHotels);

        if (ownerHotels.isEmpty()) {
            model.addAttribute("activeHotel", null);
            model.addAttribute("customers", Collections.emptyList());
            model.addAttribute("activeCustomer", null);
            return "owner/inbox";
        }

        // Xác định khách sạn đang được chọn
        Hotel activeHotel = null;
        if (hotelId != null) {
            activeHotel = hotelRepository.findById(hotelId).orElse(null);
        }
        if (activeHotel == null || activeHotel.getOwner().getId() != owner.getId()) {
            activeHotel = ownerHotels.get(0);
        }
        model.addAttribute("activeHotel", activeHotel);

        // Lấy tất cả tin nhắn liên quan tới chủ nhà này để lọc ra các khách hàng đã chat với khách sạn được chọn
        int ownerUserId = loggedInUser.getId();
        int activeHotelId = activeHotel.getId();
        List<Message> allOwnerMessages = messageRepository.findBySenderIdOrReceiverIdOrderBySentAtDesc(ownerUserId, ownerUserId);

        // Lọc ra các tin nhắn thuộc khách sạn đang active
        List<Message> hotelMessages = allOwnerMessages.stream()
                .filter(m -> m.getHotel().getId() == activeHotelId)
                .collect(Collectors.toList());

        // Gom nhóm lấy danh sách khách hàng duy nhất đã từng chat
        List<User> customers = new ArrayList<>();
        Set<Integer> addedUserIds = new HashSet<>();
        for (Message m : hotelMessages) {
            User partner = m.getSender().getId() == ownerUserId ? m.getReceiver() : m.getSender();
            // Đối tác phải có vai trò CUSTOMER
            if ("CUSTOMER".equals(partner.getRole()) && !addedUserIds.contains(partner.getId())) {
                addedUserIds.add(partner.getId());
                // Thiết lập fullName tạm thời từ email hoặc thông tin cơ bản nếu cần
                partner.setFullName(partner.getUsername()); // Fallback sang username
                customers.add(partner);
            }
        }
        model.addAttribute("customers", customers);

        // Xác định khách hàng đang được chọn để chat
        User activeCustomer = null;
        if (customerId != null) {
            activeCustomer = userRepository.findById(customerId).orElse(null);
        }
        if (activeCustomer == null && !customers.isEmpty()) {
            activeCustomer = customers.get(0);
        }
        model.addAttribute("activeCustomer", activeCustomer);
        model.addAttribute("user", loggedInUser);

        return "owner/inbox";
    }

    // Trang nội dung tin nhắn của chủ nhà (hiển thị trong iframe để auto-refresh)
    @GetMapping("/owner/chat/messages")
    public String ownerChatMessages(@RequestParam("hotelId") int hotelId,
                                    @RequestParam("customerId") int customerId,
                                    HttpSession session,
                                    Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "auth/login";
        }

        User customer = userRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return "booking/chat-messages";
        }

        int ownerUserId = loggedInUser.getId();

        // Đánh dấu toàn bộ tin nhắn từ khách hàng này gửi tới khách sạn này thành Đã đọc
        List<Message> unreadMessages = messageRepository.findByHotelIdAndSenderIdAndReceiverIdAndIsReadFalse(hotelId, customerId, ownerUserId);
        if (!unreadMessages.isEmpty()) {
            for (Message msg : unreadMessages) {
                msg.setRead(true);
            }
            messageRepository.saveAll(unreadMessages);
        }

        // Lấy lịch sử chat
        List<Message> chatHistory = messageRepository.findByHotelIdAndSenderIdAndReceiverIdOrHotelIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                hotelId, customerId, ownerUserId,
                hotelId, ownerUserId, customerId
        );

        model.addAttribute("chatHistory", chatHistory);
        model.addAttribute("currentUser", loggedInUser);
        model.addAttribute("refreshUrl", "/owner/chat/messages?hotelId=" + hotelId + "&customerId=" + customerId + "#bottom");
        return "booking/chat-messages";
    }

    // Gửi tin nhắn từ phía Chủ khách sạn
    @PostMapping("/owner/chat/send")
    public String ownerSendMessage(@RequestParam("hotelId") int hotelId,
                                   @RequestParam("customerId") int customerId,
                                   @RequestParam(value = "content", required = false) String content,
                                   @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                   HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        User customer = userRepository.findById(customerId).orElse(null);
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);

        if (customer != null && hotel != null) {
            boolean hasContent = content != null && !content.trim().isEmpty();
            boolean hasImage = imageFile != null && !imageFile.isEmpty();
            
            if (hasContent || hasImage) {
                Message message = new Message();
                message.setSender(loggedInUser);
                message.setReceiver(customer);
                message.setHotel(hotel);
                if (hasContent) {
                    message.setContent(content.trim());
                }
                if (hasImage) {
                    String savedFileName = saveUploadedImage(imageFile);
                    if (savedFileName != null) {
                        message.setImageUrl(savedFileName);
                    }
                }
                message.setRead(false);
                message.setSentAt(LocalDateTime.now());
                messageRepository.save(message);
            }
        }

        return "redirect:/chat/inbox?hotelId=" + hotelId + "&customerId=" + customerId;
    }

    // API xử lý thả reaction của tin nhắn
    @GetMapping("/chat/react")
    public String reactToMessage(@RequestParam("messageId") int messageId,
                                 @RequestParam("emoji") String emoji,
                                 @RequestParam("redirectUrl") String redirectUrl,
                                 HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null) {
            // Nếu đã thả emoji trùng thì gỡ bỏ (set null), ngược lại cập nhật emoji mới
            if (emoji.equals(message.getReaction())) {
                message.setReaction(null);
            } else {
                message.setReaction(emoji);
            }
            messageRepository.save(message);
        }

        return "redirect:" + redirectUrl;
    }

    // Hàm helper lưu trữ hình ảnh tải lên vào thư mục tĩnh của dự án
    private String saveUploadedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            // 1. Đường dẫn lưu vào mã nguồn (persistent)
            Path sourcePath = Paths.get("src/main/resources/static/assets/images/chat").toAbsolutePath().normalize();
            if (!Files.exists(sourcePath)) {
                Files.createDirectories(sourcePath);
            }
            Path sourceTarget = sourcePath.resolve(fileName);
            Files.copy(file.getInputStream(), sourceTarget, StandardCopyOption.REPLACE_EXISTING);

            // 2. Đường dẫn lưu vào target build classes (để hiển thị trực tiếp lên trình duyệt)
            Path classesPath = Paths.get("target/classes/static/assets/images/chat").toAbsolutePath().normalize();
            if (Files.exists(Paths.get("target/classes/static"))) {
                if (!Files.exists(classesPath)) {
                    Files.createDirectories(classesPath);
                }
                Path classesTarget = classesPath.resolve(fileName);
                Files.copy(file.getInputStream(), classesTarget, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
