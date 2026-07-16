package vn.edu.fpt.hotel_management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.repository.HotelRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class ChatBotService {

    @Value("${groq.api.key:}")
    private String apiKey;

    private final HotelRepository hotelRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatBotService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public String getAiResponse(String userMessage) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_GROQ_API_KEY_HERE".equals(apiKey)) {
            return "AI Chat is currently offline. Please configure a valid 'groq.api.key' in your application.properties.";
        }

        try {
            // 1. Fetch system context (real-time hotel database)
            List<Hotel> hotels = hotelRepository.findAll();
            StringBuilder hotelContext = new StringBuilder();
            hotelContext.append("Here is the real-time list of hotels available in our system:\n");
            for (Hotel h : hotels) {
                if (h.isActive()) {
                    hotelContext.append(String.format("- Hotel Name: %s | City: %s | District: %s | Address: %s | Rating: %.1f stars | Description: %s\n",
                            h.getName(), h.getCity(), h.getDistrict(), h.getAddress(), h.getRating(), h.getDescription()));
                }
            }

            // 2. Build the system prompt with strict constraints
            String systemInstruction = "You are Booking Hotels AI Assistant. Your ONLY role is to consult users on hotel details, room choices, prices, and booking recommendations from our database.\n\n"
                    + "CRITICAL CONSTRAINTS:\n"
                    + "1. Only answer questions related to hotel details, room availability, prices, amenities, and hotel booking recommendations.\n"
                    + "2. If the user asks about ANYTHING ELSE (including local tourist attractions, sightseeing schedules, tours, politics, general history, science, politics like 'Hoàng Sa Trường Sa', personal opinions, or general knowledge outside hotel bookings), you MUST politely decline to answer. Translate the refusal appropriately (Vietnamese for Vietnamese queries, English for English queries). For example: 'I can only assist you with hotel details and booking options.' or 'Tôi chỉ có thể tư vấn về thông tin khách sạn và lựa chọn đặt phòng.'\n"
                    + "3. You MUST respond in the EXACT same language as the user's message. If the user writes in English, reply in English. If the user writes in Vietnamese, reply in Vietnamese.\n"
                    + "4. Keep your answers professional, concise, and helpful. Format your responses in clean HTML paragraphs (use <p> tags, <strong> tags for emphasis, etc. but do NOT wrap the whole response in <html> or <body> tags).\n"
                    + "5. Recommend specific hotels from the database list below when users ask for recommendations. Give exact hotel names. Do NOT recommend or make up any hotels that are not listed in the provided hotel list.\n\n"
                    + hotelContext.toString() + "\n"
                    + "User asks: " + userMessage;

            // 3. Prepare HTTP Request payload for Groq using Jackson
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", "user");
            messageNode.put("content", systemInstruction);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            messagesArray.add(messageNode);

            ObjectNode payloadNode = objectMapper.createObjectNode();
            payloadNode.set("messages", messagesArray);
            payloadNode.put("model", "llama-3.1-8b-instant");

            String payload = objectMapper.writeValueAsString(payloadNode);

            // 4. Send request using Java 21 native HttpClient
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromGroqResponse(response.body());
            } else {
                return "<p>Sorry, I encountered an error communicating with the AI service. Status code: " + response.statusCode() + "</p>";
            }

        } catch (Exception e) {
            return "<p>Error generating AI response: " + e.getMessage() + "</p>";
        }
    }

    private String extractTextFromGroqResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            return "<p>Could not parse AI response correctly.</p>";
        } catch (Exception e) {
            return "<p>Error parsing AI response: " + e.getMessage() + "</p>";
        }
    }
}
