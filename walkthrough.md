# Tài liệu tính năng: Đánh giá Khách sạn (Hotel Reviews - UC-11.1 & UC-11.3)

Chức năng này cho phép Khách hàng (`CUSTOMER`) viết đánh giá, chấm điểm số sao (1-5) và bình luận cho khách sạn. Đồng thời, hiển thị điểm số sao trung bình trên banner khách sạn và cho phép chính tác giả hoặc Admin xóa đánh giá.

Tác giả thực hiện: **Châu Quốc Inh (CE190593)**

---

## 1. Các thành phần đã thêm & chỉnh sửa

### 1.1. Thực thể cơ sở dữ liệu (Entities)
*   **[Review.java](file:///d:/SWP391_PROJECT/SWP391_Group5_SU26_HotelsManagement/src/main/java/vn/edu/fpt/hotel_management/entity/Review.java)** (Tạo mới): Lớp đại diện cho bảng `reviews` lưu trong Database, chứa các trường: `id`, `hotelId`, `userId`, `userFullName`, `rating` (1-5), `comment`, và `createdAt`.
    *   *Lưu ý:* Sử dụng mã số ID phẳng (`hotelId` và `userId`) để đồng bộ với cấu trúc của dự án và hạn chế lỗi liên kết JPA.

### 1.2. Lớp kết nối Database (Repositories)
*   **[ReviewRepository.java](file:///d:/SWP391_PROJECT/SWP391_Group5_SU26_HotelsManagement/src/main/java/vn/edu/fpt/hotel_management/repository/ReviewRepository.java)** (Tạo mới): Interface Spring Data JPA cung cấp:
    *   `findByHotelIdOrderByCreatedAtDesc`: Lấy tất cả đánh giá của khách sạn theo thứ tự thời gian mới nhất.
    *   `existsByHotelIdAndUserId`: Kiểm tra xem người dùng đã từng đánh giá khách sạn này chưa để ngăn ngừa spam.

### 1.3. Bộ điều khiển & Xử lý logic (Controllers)
*   **[ReviewController.java](file:///d:/SWP391_PROJECT/SWP391_Group5_SU26_HotelsManagement/src/main/java/vn/edu/fpt/hotel_management/controller/ReviewController.java)** (Tạo mới):
    *   `POST /hotels/{id}/reviews`: Tiếp nhận thông tin đánh giá mới, validate điểm số sao, phân quyền chỉ cho vai trò `CUSTOMER` gửi và giới hạn mỗi tài khoản chỉ đánh giá 1 lần.
    *   `POST /hotels/{id}/reviews/{reviewId}/delete`: Tiếp nhận yêu cầu xóa đánh giá, kiểm tra quyền sở hữu (chỉ cho tác giả đánh giá hoặc tài khoản `ADMIN` thực hiện xóa).
*   **[RoomController.java](file:///d:/SWP391_PROJECT/SWP391_Group5_SU26_HotelsManagement/src/main/java/vn/edu/fpt/hotel_management/controller/RoomController.java)** (Chỉnh sửa):
    *   Tải toàn bộ đánh giá của khách sạn và tính toán điểm trung bình số sao (`avgRating`) hiển thị ra giao diện.
    *   Truyền cờ hiệu `hasReviewed` để ẩn/hiện form đánh giá phù hợp.

### 1.4. Giao diện & Thiết kế (Views & Styling)
*   **[rooms.html](file:///d:/SWP391_PROJECT/SWP391_Group5_SU26_HotelsManagement/src/main/resources/templates/hotel/rooms.html)** (Chỉnh sửa):
    *   Hiển thị điểm trung bình thực tế và tổng số lượt đánh giá ở góc banner đầu trang.
    *   Tích hợp mục **Hotel Reviews** ở cuối trang hiển thị biểu đồ điểm đánh giá trung bình, form nhập đánh giá trực quan (có JS chọn số sao động) và danh sách phản hồi từ khách hàng.
*   **[room-list.css](file:///d:/SWP391_PROJECT/SWP391_Group5_SU26_HotelsManagement/src/main/resources/static/assets/css/room-list.css)** (Chỉnh sửa):
    *   Thêm CSS trang trí cho khung Đánh giá đồng bộ với phong cách thiết kế Lion Hotel (Deep Navy + Champagne Gold).

---

## 2. Các ràng buộc nghiệp vụ (Business Rules)
1.  **Phân quyền gửi đánh giá**: Chỉ người dùng có vai trò **`CUSTOMER`** mới hiển thị form nhập đánh giá. Người dùng chưa đăng nhập sẽ hiển thị yêu cầu đăng nhập. Các vai trò khác (`OWNER`, `ADMIN`) chỉ có quyền xem.
2.  **Giới hạn lượt đánh giá**: Mỗi tài khoản chỉ được viết đánh giá **tối đa 1 lần** cho mỗi khách sạn để tránh việc spam điểm số ảo.
3.  **Quyền xóa đánh giá**: Chỉ có chính khách hàng viết ra đánh giá đó hoặc tài khoản **`ADMIN`** mới hiển thị nút Xóa và được phép thực thi API xóa đánh giá.
