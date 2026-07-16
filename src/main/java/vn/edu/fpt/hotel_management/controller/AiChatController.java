package vn.edu.fpt.hotel_management.controller;

import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.hotel_management.service.ChatBotService;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-chat")
public class AiChatController {

    private final ChatBotService chatBotService;

    public AiChatController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }

    @PostMapping("/consult")
    public Map<String, String> consult(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of("response", "<p>Please type a message.</p>");
        }
        String aiResponse = chatBotService.getAiResponse(message);
        return Map.of("response", aiResponse);
    }
}
