package vn.edu.fpt.hotel_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.hotel_management.entity.Message;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    // Magic Method lấy toàn bộ lịch sử tin nhắn của một khách sạn giữa khách hàng và chủ nhà, sắp xếp thời gian tăng dần
    List<Message> findByHotelIdAndSenderIdAndReceiverIdOrHotelIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
            int hotelId1, int senderId1, int receiverId1,
            int hotelId2, int senderId2, int receiverId2
    );

    // Magic Method lấy toàn bộ tin nhắn liên quan đến một User (chủ nhà hoặc khách hàng), sắp xếp thời gian giảm dần
    List<Message> findBySenderIdOrReceiverIdOrderBySentAtDesc(int senderId, int receiverId);

    // Magic Method tìm các tin nhắn chưa đọc gửi từ một người đến một người trong khách sạn cụ thể
    List<Message> findByHotelIdAndSenderIdAndReceiverIdAndIsReadFalse(int hotelId, int senderId, int receiverId);

    // Đếm số tin nhắn chưa đọc gửi đến một người nhận
    long countByReceiverIdAndIsReadFalse(int receiverId);

    // Đếm số tin nhắn chưa đọc từ một khách sạn cụ thể đến một người nhận
    long countByHotelIdAndReceiverIdAndIsReadFalse(int hotelId, int receiverId);
}
