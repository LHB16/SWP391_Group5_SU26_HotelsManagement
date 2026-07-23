package vn.edu.fpt.hotel_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.Promotion;
import vn.edu.fpt.hotel_management.repository.BookingRepository;
import vn.edu.fpt.hotel_management.repository.HotelRepository;
import vn.edu.fpt.hotel_management.repository.PromotionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    public PromotionService(PromotionRepository promotionRepository, HotelRepository hotelRepository, BookingRepository bookingRepository) {
        this.promotionRepository = promotionRepository;
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Promotion> getPromotionsByHotelId(int hotelId) {
        return promotionRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);
    }

    @Transactional(readOnly = true)
    public List<Promotion> getPromotionsByOwnerId(int ownerId) {
        return promotionRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Promotion> getActivePromotionsByHotelId(int hotelId) {
        return promotionRepository.findActivePromotionsByHotelId(hotelId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionByIdAndHotelId(int id, int hotelId) {
        return promotionRepository.findByIdAndHotelId(id, hotelId);
    }

    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionById(int id) {
        return promotionRepository.findById(id);
    }

    @Transactional
    public Promotion createPromotion(int hotelId, String title, String description,
                                     BigDecimal discountPercent, LocalDate startDate,
                                     LocalDate endDate, String status) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found"));

        Promotion promotion = new Promotion();
        promotion.setHotel(hotel);
        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setDiscountPercent(discountPercent.setScale(2, RoundingMode.HALF_UP));
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setStatus(determineStatus(status, startDate, endDate));
        promotion.setCreatedAt(java.time.LocalDateTime.now());
        promotion.setUpdatedAt(java.time.LocalDateTime.now());

        return promotionRepository.save(promotion);
    }

    @Transactional
    public Promotion updatePromotion(int id, int hotelId, String title, String description,
                                     BigDecimal discountPercent, LocalDate startDate,
                                     LocalDate endDate, String status) {
        Promotion promotion = promotionRepository.findByIdAndHotelId(id, hotelId)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setDiscountPercent(discountPercent.setScale(2, RoundingMode.HALF_UP));
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setStatus(determineStatus(status, startDate, endDate));
        promotion.setUpdatedAt(java.time.LocalDateTime.now());

        return promotionRepository.save(promotion);
    }

    @Transactional
    public void deletePromotion(int id, int hotelId) {
        Promotion promotion = promotionRepository.findByIdAndHotelId(id, hotelId)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        bookingRepository.unlinkPromotion(id);
        promotionRepository.delete(promotion);
    }

    @Transactional
    public void updatePromotionStatuses() {
        LocalDate today = LocalDate.now();
        List<Promotion> allPromotions = promotionRepository.findAll();

        for (Promotion p : allPromotions) {
            String newStatus = determineStatus(p.getStatus(), p.getStartDate(), p.getEndDate());
            if (!newStatus.equals(p.getStatus())) {
                p.setStatus(newStatus);
                p.setUpdatedAt(java.time.LocalDateTime.now());
                promotionRepository.save(p);
            }
        }
    }

    private String determineStatus(String currentStatus, LocalDate startDate, LocalDate endDate) {
        if ("INACTIVE".equals(currentStatus)) {
            return "INACTIVE";
        }

        LocalDate today = LocalDate.now();

        if (startDate == null || endDate == null) {
            return "INACTIVE";
        }

        if (today.isBefore(startDate)) {
            return "ACTIVE";
        } else if (today.isAfter(endDate)) {
            return "EXPIRED";
        } else {
            return "ACTIVE";
        }
    }

    public BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, Promotion promotion) {
        if (promotion == null || promotion.getDiscountPercent() == null) {
            return originalPrice;
        }

        BigDecimal discount = originalPrice.multiply(
                promotion.getDiscountPercent().divide(BigDecimal.valueOf(100))
        );
        return originalPrice.subtract(discount).setScale(0, RoundingMode.HALF_UP);
    }

    public boolean isPromotionActive(Promotion promotion) {
        if (promotion == null || !"ACTIVE".equals(promotion.getStatus())) {
            return false;
        }

        LocalDate today = LocalDate.now();
        return promotion.getStartDate() != null &&
                promotion.getEndDate() != null &&
                !today.isBefore(promotion.getStartDate()) &&
                !today.isAfter(promotion.getEndDate());
    }

    public String getStatusLabel(String status) {
        if (status == null) return "UNKNOWN";
        return switch (status) {
            case "ACTIVE" -> "ACTIVE";
            case "EXPIRED" -> "EXPIRED";
            case "INACTIVE" -> "INACTIVE";
            default -> status;
        };
    }

    public String getStatusBadgeClass(String status) {
        if (status == null) return "badge-status-neutral";
        return switch (status) {
            case "ACTIVE" -> "badge-status-success";
            case "EXPIRED" -> "badge-status-neutral";
            case "INACTIVE" -> "badge-status-danger";
            default -> "badge-status-neutral";
        };
    }
}