package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.hotel_management.entity.*;
import vn.edu.fpt.hotel_management.repository.*;
import vn.edu.fpt.hotel_management.service.OwnerService;

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
    private final OwnerService ownerService;
    private final CustomerRepository customerRepository;

    public ChatController(MessageRepository messageRepository,
                          HotelRepository hotelRepository,
                          UserRepository userRepository,
                          HotelOwnerRepository hotelOwnerRepository,
                          OwnerService ownerService) {
                          CustomerRepository customerRepository) {
        this.messageRepository = messageRepository;
        this.hotelRepository = hotelRepository;
        this.userRepository = userRepository;
        this.hotelOwnerRepository = hotelOwnerRepository;
        this.ownerService = ownerService;
        this.customerRepository = customerRepository;
    }

    // ===================== KHÁCH HÀNG (CUSTOMER) =====================

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

    @GetMapping("/customer/chat/messages")
    public String customerChatMessages(@RequestParam("hotelId") int hotelId,
                                       HttpSession session,
                                       Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "auth/login";
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return "booking/chat-messages";
        }

        int customerUserId = loggedInUser.getId();
        int ownerUserId = hotel.getOwner().getUserAccount().getId();

        List<Message> unreadMessages = messageRepository.findByHotelIdAndSenderIdAndReceiverIdAndIsReadFalse(hotelId, ownerUserId, customerUserId);
        if (!unreadMessages.isEmpty()) {
            for (Message msg : unreadMessages) {
                msg.setRead(true);
            }
            messageRepository.saveAll(unreadMessages);
        }

        List<Message> chatHistory = messageRepository.findByHotelIdAndSenderIdAndReceiverIdOrHotelIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                hotelId, customerUserId, ownerUserId,
                hotelId, ownerUserId, customerUserId
        );

        populateUserFullNames(chatHistory);

        model.addAttribute("chatHistory", chatHistory);
        model.addAttribute("currentUser", loggedInUser);
        model.addAttribute("refreshUrl", "/customer/chat/messages?hotelId=" + hotelId + "#bottom");
        return "booking/chat-messages";
    }

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

        // Kiểm tra Owner đã được duyệt
        if (!ownerService.isOwnerApproved(loggedInUser)) {
            session.setAttribute("errorMessage",
                    "Your account is pending admin approval. Chat is not available.");
            return "redirect:/owner/dashboard";
        }

        HotelOwner owner = ownerService.getOwnerByUser(loggedInUser).orElse(null);
        if (owner == null) {
            return "redirect:/home";
        }

        List<Hotel> ownerHotels = hotelRepository.findByOwnerId(owner.getId());
        model.addAttribute("hotels", ownerHotels);

        if (ownerHotels.isEmpty()) {
            model.addAttribute("activeHotel", null);
            model.addAttribute("customers", Collections.emptyList());
            model.addAttribute("activeCustomer", null);
            return "owner/inbox";
        }

        Hotel activeHotel = null;
        if (hotelId != null) {
            activeHotel = hotelRepository.findById(hotelId).orElse(null);
        }
        if (activeHotel == null || activeHotel.getOwner().getId() != owner.getId()) {
            activeHotel = ownerHotels.get(0);
        }
        model.addAttribute("activeHotel", activeHotel);

        int ownerUserId = loggedInUser.getId();
        int activeHotelId = activeHotel.getId();
        List<Message> allOwnerMessages = messageRepository.findBySenderIdOrReceiverIdOrderBySentAtDesc(ownerUserId, ownerUserId);

        List<Message> hotelMessages = allOwnerMessages.stream()
                .filter(m -> m.getHotel().getId() == activeHotelId)
                .collect(Collectors.toList());

        List<User> customers = new ArrayList<>();
        Set<Integer> addedUserIds = new HashSet<>();
        for (Message m : hotelMessages) {
            User partner = m.getSender().getId() == ownerUserId ? m.getReceiver() : m.getSender();
            if ("CUSTOMER".equals(partner.getRole()) && !addedUserIds.contains(partner.getId())) {
                addedUserIds.add(partner.getId());
                // Nạp tên đầy đủ thực tế của khách hàng từ CustomerRepository
                customerRepository.findByUserAccountId(partner.getId()).ifPresent(c -> {
                    partner.setFullName(c.getFullName());
                });
                if (partner.getFullName() == null || partner.getFullName().isBlank()) {
                    partner.setFullName(partner.getUsername());
                }
                customers.add(partner);
            }
        }
        model.addAttribute("customers", customers);

        User activeCustomer = null;
        if (customerId != null) {
            activeCustomer = userRepository.findById(customerId).orElse(null);
            if (activeCustomer != null) {
                final User finalCust = activeCustomer;
                customerRepository.findByUserAccountId(activeCustomer.getId()).ifPresent(c -> {
                    finalCust.setFullName(c.getFullName());
                });
            }
        }
        if (activeCustomer == null && !customers.isEmpty()) {
            activeCustomer = customers.get(0);
        }
        model.addAttribute("activeCustomer", activeCustomer);
        model.addAttribute("user", loggedInUser);

        return "owner/inbox";
    }

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

        List<Message> unreadMessages = messageRepository.findByHotelIdAndSenderIdAndReceiverIdAndIsReadFalse(hotelId, customerId, ownerUserId);
        if (!unreadMessages.isEmpty()) {
            for (Message msg : unreadMessages) {
                msg.setRead(true);
            }
            messageRepository.saveAll(unreadMessages);
        }

        List<Message> chatHistory = messageRepository.findByHotelIdAndSenderIdAndReceiverIdOrHotelIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                hotelId, customerId, ownerUserId,
                hotelId, ownerUserId, customerId
        );

        populateUserFullNames(chatHistory);

        model.addAttribute("chatHistory", chatHistory);
        model.addAttribute("currentUser", loggedInUser);
        model.addAttribute("refreshUrl", "/owner/chat/messages?hotelId=" + hotelId + "&customerId=" + customerId + "#bottom");
        return "booking/chat-messages";
    }

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



    // Hàm đối chiếu lấy FullName cho người gửi/nhận trong lịch sử chat
    private void populateUserFullNames(List<Message> chatHistory) {
        if (chatHistory == null) return;
        for (Message msg : chatHistory) {
            User sender = msg.getSender();
            if (sender != null && (sender.getFullName() == null || sender.getFullName().equals(sender.getUsername()))) {
                if ("CUSTOMER".equals(sender.getRole())) {
                    customerRepository.findByUserAccountId(sender.getId()).ifPresent(c -> {
                        sender.setFullName(c.getFullName());
                    });
                } else if ("HOTEL_OWNER".equals(sender.getRole())) {
                    hotelOwnerRepository.findByUserAccountId(sender.getId()).ifPresent(o -> {
                        sender.setFullName(o.getFullName());
                    });
                }
            }
            User receiver = msg.getReceiver();
            if (receiver != null && (receiver.getFullName() == null || receiver.getFullName().equals(receiver.getUsername()))) {
                if ("CUSTOMER".equals(receiver.getRole())) {
                    customerRepository.findByUserAccountId(receiver.getId()).ifPresent(c -> {
                        receiver.setFullName(c.getFullName());
                    });
                } else if ("HOTEL_OWNER".equals(receiver.getRole())) {
                    hotelOwnerRepository.findByUserAccountId(receiver.getId()).ifPresent(o -> {
                        receiver.setFullName(o.getFullName());
                    });
                }
            }
        }
    }

    // ===== HELPER: LƯU ẢNH =====
    private String saveUploadedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            Path sourcePath = Paths.get("src/main/resources/static/assets/images/chat").toAbsolutePath().normalize();
            if (!Files.exists(sourcePath)) {
                Files.createDirectories(sourcePath);
            }
            Path sourceTarget = sourcePath.resolve(fileName);
            Files.copy(file.getInputStream(), sourceTarget, StandardCopyOption.REPLACE_EXISTING);

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