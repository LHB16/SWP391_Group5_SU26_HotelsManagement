package vn.edu.fpt.hotel_management.service;

import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.HotelOwnerRepository;

import java.util.Optional;

@Service
public class OwnerService {

    private final HotelOwnerRepository hotelOwnerRepository;

    public OwnerService(HotelOwnerRepository hotelOwnerRepository) {
        this.hotelOwnerRepository = hotelOwnerRepository;
    }

    /**
     * Kiểm tra trạng thái xác minh của Owner
     * @param user User đang đăng nhập
     * @return Optional chứa HotelOwner nếu tìm thấy, ngược lại empty
     */
    public Optional<HotelOwner> getOwnerByUser(User user) {
        if (user == null) {
            return Optional.empty();
        }
        return hotelOwnerRepository.findByUserAccount(user);
    }

    /**
     * Kiểm tra Owner đã được duyệt chưa
     * @param user User đang đăng nhập
     * @return true nếu Owner đã được APPROVED, false nếu chưa
     */
    public boolean isOwnerApproved(User user) {
        if (user == null) {
            return false;
        }
        if (!"HOTEL_OWNER".equals(user.getRole())) {
            return false;
        }
        Optional<HotelOwner> ownerOpt = hotelOwnerRepository.findByUserAccount(user);
        return ownerOpt.map(owner -> "APPROVED".equals(owner.getVerificationStatus())).orElse(false);
    }

    /**
     * Lấy trạng thái xác minh của Owner
     * @param user User đang đăng nhập
     * @return Trạng thái: APPROVED, PENDING, REJECTED, hoặc null nếu không tìm thấy
     */
    public String getVerificationStatus(User user) {
        if (user == null) {
            return null;
        }
        Optional<HotelOwner> ownerOpt = hotelOwnerRepository.findByUserAccount(user);
        return ownerOpt.map(HotelOwner::getVerificationStatus).orElse(null);
    }

    /**
     * Lấy thông tin HotelOwner từ User
     */
    public HotelOwner getOwnerOrThrow(User user) {
        return hotelOwnerRepository.findByUserAccount(user)
                .orElseThrow(() -> new RuntimeException("Hotel owner profile not found!"));
    }
}