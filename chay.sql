-- =============================================
-- FULL DATABASE SCRIPT - HOTEL MANAGEMENT
-- Tên Database: hotel_db
-- Tên tệp: chay.sql
-- =============================================

USE master;
GO

IF EXISTS (SELECT * FROM sys.databases WHERE name = 'hotel_db')
BEGIN
    ALTER DATABASE hotel_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE hotel_db;
END
GO

CREATE DATABASE hotel_db;
GO

USE hotel_db;
GO

-- 1. USERS
CREATE TABLE users (
    id          INT PRIMARY KEY IDENTITY(1,1),
    email       NVARCHAR(255) NOT NULL UNIQUE,
    username    NVARCHAR(255) UNIQUE,
    full_name   NVARCHAR(255),
    password    NVARCHAR(255),
    avatar_url  NVARCHAR(500),
    role        NVARCHAR(50) CONSTRAINT chk_role CHECK (role IN ('CUSTOMER','HOTEL_OWNER','ADMIN')),
    enabled     BIT DEFAULT 0,
    otp         NVARCHAR(255),
    otp_expiry  DATETIME2,
    otp_type    NVARCHAR(50) CONSTRAINT chk_otp_type CHECK (otp_type IN ('REGISTER','FORGOT_PASSWORD','CHANGE_PASSWORD','UPDATE_PROFILE')),
    created_at  DATETIME2 DEFAULT GETDATE()
);

-- 2. HOTEL
CREATE TABLE hotel (
    id        INT PRIMARY KEY IDENTITY(1,1),
    owner_id  INT NOT NULL FOREIGN KEY REFERENCES users(id),
    name      NVARCHAR(255),
    address   NVARCHAR(255),
    city      NVARCHAR(100),
    image_url NVARCHAR(255),
    price     NUMERIC(38, 2) CONSTRAINT chk_hotel_price CHECK (price >= 0),
    rating    NUMERIC(2, 1) CONSTRAINT chk_hotel_rating CHECK (rating BETWEEN 0 AND 5),
    active    BIT DEFAULT 1
);

-- 3. ROOM
CREATE TABLE room (
    id          INT PRIMARY KEY IDENTITY(1,1),
    hotel_id    INT NOT NULL FOREIGN KEY REFERENCES hotel(id),
    room_type   NVARCHAR(255),
    description NVARCHAR(255),
    price       NUMERIC(38, 2) CONSTRAINT chk_room_price CHECK (price >= 0),
    acreage     FLOAT,
    bed         INT,
    person      INT,
    num_window  INT,
    img_url     NVARCHAR(255),
    status      NVARCHAR(50) DEFAULT 'AVAILABLE'
                CONSTRAINT chk_room_status CHECK (status IN ('AVAILABLE','OCCUPIED','MAINTENANCE'))
);

-- 4. REVIEWS
CREATE TABLE reviews (
    id             INT PRIMARY KEY IDENTITY(1,1),
    user_id        INT NOT NULL FOREIGN KEY REFERENCES users(id),
    hotel_id       INT NOT NULL FOREIGN KEY REFERENCES hotel(id),
    user_full_name NVARCHAR(255),
    room_type      NVARCHAR(255),
    comment        NVARCHAR(MAX),
    rating         INT CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    created_at     DATETIME2 DEFAULT GETDATE()
);

-- 5. REVIEW_REPLIES
CREATE TABLE review_replies (
    id         INT PRIMARY KEY IDENTITY(1,1),
    review_id  INT NOT NULL FOREIGN KEY REFERENCES reviews(id),
    owner_id   INT NOT NULL FOREIGN KEY REFERENCES users(id),
    content    NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE()
);

-- 6. BOOKINGS
CREATE TABLE bookings (
    id             INT PRIMARY KEY IDENTITY(1,1),
    customer_id    INT NOT NULL FOREIGN KEY REFERENCES users(id),
    room_id        INT NOT NULL FOREIGN KEY REFERENCES room(id),
    check_in_date  DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price    NUMERIC(38, 2),
    status         NVARCHAR(50) DEFAULT 'PENDING'
                   CONSTRAINT chk_booking_status CHECK (status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
    created_at     DATETIME2 DEFAULT GETDATE(),
    updated_at     DATETIME2
);

-- 7. PAYMENTS
CREATE TABLE payments (
    id          INT PRIMARY KEY IDENTITY(1,1),
    booking_id  INT NOT NULL UNIQUE FOREIGN KEY REFERENCES bookings(id),
    amount      NUMERIC(38, 2) CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    method      NVARCHAR(50) DEFAULT 'QR_CODE' 
                CONSTRAINT chk_payment_method CHECK (method IN ('QR_CODE')),
    status      NVARCHAR(50) DEFAULT 'PENDING'
                CONSTRAINT chk_payment_status CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    qr_code_url NVARCHAR(500),
    paid_at     DATETIME2
);

-- 8. WISHLISTS
CREATE TABLE wishlists (
    id          INT PRIMARY KEY IDENTITY(1,1),
    customer_id INT NOT NULL FOREIGN KEY REFERENCES users(id),
    room_id     INT NOT NULL FOREIGN KEY REFERENCES room(id),
    note        NVARCHAR(255),
    added_at    DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT uq_wishlist UNIQUE (customer_id, room_id)
);

-- 9. PROMOTIONS
CREATE TABLE promotions (
    id               INT PRIMARY KEY IDENTITY(1,1),
    hotel_id         INT NOT NULL FOREIGN KEY REFERENCES hotel(id),
    title            NVARCHAR(255),
    description      NVARCHAR(MAX),
    discount_percent NUMERIC(5, 2),
    start_date       DATE,
    end_date         DATE,
    status           NVARCHAR(50) DEFAULT 'DRAFT'
                     CONSTRAINT chk_promo_status CHECK (status IN ('ACTIVE','EXPIRED','DRAFT')),
    created_at       DATETIME2 DEFAULT GETDATE()
);

-- 10. MESSAGES
CREATE TABLE messages (
    id          INT PRIMARY KEY IDENTITY(1,1),
    sender_id   INT NOT NULL FOREIGN KEY REFERENCES users(id),
    receiver_id INT NOT NULL FOREIGN KEY REFERENCES users(id),
    booking_id  INT FOREIGN KEY REFERENCES bookings(id),
    content     NVARCHAR(MAX),
    is_read     BIT DEFAULT 0,
    sent_at     DATETIME2 DEFAULT GETDATE()
);
GO

-- =============================================
-- CHÈN DỮ LIỆU MẪU (SEED DATA)
-- Mật khẩu mặc định của các tài khoản mẫu: 123456
-- (Mã hóa BCrypt: $2a$10$tESl.k5G0YshQh2k27v4euN.mS6l3R1T8bA5v.n1p6h5lZ22vK6fC)
-- =============================================

-- Chèn tài khoản users mẫu trước (để gán khóa ngoại cho hotel, bookings...)
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, email, username, full_name, password, role, enabled) VALUES 
(1, N'owner@gmail.com', N'owner', N'Hotel Owner', N'$2a$10$tESl.k5G0YshQh2k27v4euN.mS6l3R1T8bA5v.n1p6h5lZ22vK6fC', N'HOTEL_OWNER', 1),
(2, N'customer@gmail.com', N'customer', N'Customer User', N'$2a$10$tESl.k5G0YshQh2k27v4euN.mS6l3R1T8bA5v.n1p6h5lZ22vK6fC', N'CUSTOMER', 1),
(3, N'admin@gmail.com', N'admin', N'Admin User', N'$2a$10$tESl.k5G0YshQh2k27v4euN.mS6l3R1T8bA5v.n1p6h5lZ22vK6fC', N'ADMIN', 1);
SET IDENTITY_INSERT users OFF;
GO

-- Chèn khách sạn hotel mẫu
SET IDENTITY_INSERT hotel ON;
INSERT INTO hotel (id, owner_id, name, address, city, image_url, price, rating, active) VALUES 
(1, 1, N'Lion 6', N'Can Tho', N'Cần Thơ', N'/assets/images/hotel/hotel_lion6.jpg', 420000.00, 5.0, 1),
(2, 1, N'Sheraton', N'Can Tho', N'Cần Thơ', N'/assets/images/hotel/1781766228236_Sheraton.jpg', 5000000.00, 4.0, 1),
(3, 1, N'Muong Thanh', N'Can Tho', N'Cần Thơ', N'/assets/images/hotel/1781766318151_MuongThanh.jpg', 15000000.00, 3.0, 1),
(4, 1, N'Riverside 1 Hotel', N'Can Tho', N'Cần Thơ', N'/assets/images/hotel/1781766356470_Riverside1Hotel.jpg', 50000000.00, 2.0, 1),
(5, 1, N'Kim Tho', N'Can Tho', N'Cần Thơ', N'/assets/images/hotel/1781766429274_26637342.webp', 10000.00, 1.0, 1),
(6, 1, N'Minh Minh Nam 3', N'Can Tho', N'Cần Thơ', N'/assets/images/hotel/1781766529899_823722369.webp', 25000000.00, 3.0, 1);
SET IDENTITY_INSERT hotel OFF;
GO

-- Chèn phòng room mẫu
SET IDENTITY_INSERT room ON;
INSERT INTO room (id, hotel_id, room_type, description, price, acreage, bed, person, num_window, img_url, status) VALUES 
(1, 1, N'STANDARD DOUBLE', N'With an area of 20 m², the hotel room is designed in a modern style, fully integrated with amenities including luxurious single bed, television, high-speed wifi.', 419996.00, 20, 1, 2, 0, N'/assets/images/room/1781767856409_i_57_794_IMG_6006.jpg', 'AVAILABLE'),
(2, 1, N'STANDARD DOUBLE', N'With an area of 20 m², the hotel room is designed in a modern style, fully integrated with amenities including luxurious single bed, television, high-speed wifi.', 419996.00, 20, 1, 2, 0, N'/assets/images/room/1781767880315_i_57_794_IMG_6006.jpg', 'AVAILABLE'),
(3, 1, N'STANDARD DOUBLE', N'With an area of 20 m², the hotel room is designed in a modern style, fully integrated with amenities including luxurious single bed, television, high-speed wifi.', 420000.00, 18, 1, 2, 0, N'/assets/images/room/1781767922865_i_57_794_IMG_6006.jpg', 'AVAILABLE');
SET IDENTITY_INSERT room OFF;
GO

-- Chèn dữ liệu bookings mẫu cho các quý và tháng trong năm 2025 và 2026
SET IDENTITY_INSERT bookings ON;
INSERT INTO bookings (id, customer_id, room_id, check_in_date, check_out_date, total_price, status, created_at) VALUES 
(1, 2, 1, '2025-01-10', '2025-01-12', 840000.00, 'CHECKED_OUT', '2025-01-10 10:00:00'),
(2, 2, 2, '2025-04-15', '2025-04-18', 1260000.00, 'CHECKED_OUT', '2025-04-15 14:00:00'),
(3, 2, 3, '2025-07-20', '2025-07-22', 840000.00, 'CHECKED_OUT', '2025-07-20 09:30:00'),
(4, 2, 1, '2025-10-05', '2025-10-10', 2100000.00, 'CHECKED_OUT', '2025-10-05 11:15:00'),
(5, 2, 2, '2026-02-12', '2026-02-15', 1260000.00, 'CONFIRMED', '2026-02-12 08:00:00'),
(6, 2, 3, '2026-05-20', '2026-05-25', 2100000.00, 'CHECKED_IN', '2026-05-20 15:45:00');
SET IDENTITY_INSERT bookings OFF;
GO

-- Chèn dữ liệu payments mẫu tương ứng với các bookings
SET IDENTITY_INSERT payments ON;
INSERT INTO payments (id, booking_id, amount, method, status, paid_at) VALUES 
(1, 1, 840000.00, 'QR_CODE', 'SUCCESS', '2025-01-10 10:05:00'),
(2, 2, 1260000.00, 'QR_CODE', 'SUCCESS', '2025-04-15 14:10:00'),
(3, 3, 840000.00, 'QR_CODE', 'SUCCESS', '2025-07-20 09:35:00'),
(4, 4, 2100000.00, 'QR_CODE', 'SUCCESS', '2025-10-05 11:20:00'),
(5, 5, 1260000.00, 'QR_CODE', 'SUCCESS', '2026-02-12 08:05:00'),
(6, 6, 2100000.00, 'QR_CODE', 'SUCCESS', '2026-05-20 15:50:00');
SET IDENTITY_INSERT payments OFF;
GO

USE master;
GO
ALTER DATABASE hotel_db SET READ_WRITE;
GO
