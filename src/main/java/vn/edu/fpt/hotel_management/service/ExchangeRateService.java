package vn.edu.fpt.hotel_management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ExchangeRateService {

    @Value("${payment.paypal.vnd-to-usd-rate:25400}")
    private double defaultRate;

    private volatile double cachedRate = 25400.0;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        cachedRate = defaultRate;
        // Thực hiện lấy tỷ giá lần đầu khi chạy ứng dụng
        updateExchangeRate();
    }

    // Tự động cập nhật tỷ giá vào 1:00 AM hàng ngày
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateExchangeRate() {
        try {
            System.out.println("[ExchangeRate] Fetching latest USD to VND exchange rate from api...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://open.er-api.com/v6/latest/USD"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if ("success".equalsIgnoreCase(root.path("result").asText())) {
                    double newRate = root.path("rates").path("VND").asDouble();
                    if (newRate > 0) {
                        cachedRate = newRate;
                        System.out.println("[ExchangeRate] Exchange rate updated successfully: 1 USD = " + cachedRate + " VND");
                        return;
                    }
                }
            }
            System.err.println("[ExchangeRate] Failed to parse rate from API response. Using default: " + cachedRate);
        } catch (Exception e) {
            System.err.println("[ExchangeRate] Error fetching exchange rate: " + e.getMessage() + ". Using default: " + cachedRate);
        }
    }

    public double getRate() {
        return cachedRate;
    }
}
