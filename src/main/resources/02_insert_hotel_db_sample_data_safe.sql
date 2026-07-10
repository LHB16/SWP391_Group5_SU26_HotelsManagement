-- =====================================================
-- HOTEL BOOKING SYSTEM - SAMPLE DATA SCRIPT
-- File: 02_insert_hotel_db_sample_data_NO_DELETE.sql
-- Target: Microsoft SQL Server / SSMS / DataGrip
-- Description: Insert sample data WITHOUT DELETE and WITHOUT DBCC CHECKIDENT.
-- Safe to run multiple times because it checks existing data before insert.
-- =====================================================

IF DB_ID(N'hotel_db') IS NULL
BEGIN
    THROW 50000, N'Database hotel_db does not exist. Run 01_create_hotel_db_schema.sql first.', 1;
END
GO

USE hotel_db;
GO

SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    -- =====================================================
    -- 0. VARIABLES
    -- =====================================================
    DECLARE
        @admin1_user_id INT,
        @admin2_user_id INT,
        @owner1_user_id INT,
        @owner2_user_id INT,
        @owner3_user_id INT,
        @customer1_user_id INT,
        @customer2_user_id INT,
        @customer3_user_id INT,
        @customer4_user_id INT,
        @customer5_user_id INT,

        @admin1_id INT,
        @admin2_id INT,
        @owner1_id INT,
        @owner2_id INT,
        @owner3_id INT,
        @customer1_id INT,
        @customer2_id INT,
        @customer3_id INT,
        @customer4_id INT,
        @customer5_id INT,

        @hotel1_id INT,
        @hotel2_id INT,
        @hotel3_id INT,

        @room1_id INT,
        @room2_id INT,
        @room3_id INT,
        @room4_id INT,
        @room5_id INT,
        @room6_id INT,
        @room7_id INT,
        @room8_id INT,

        @booking1_id INT,
        @booking2_id INT,
        @booking3_id INT,
        @booking4_id INT,
        @booking5_id INT,

        @feedback1_id INT,
        @feedback2_id INT,
        @feedback3_id INT,
        @feedback4_id INT;

    -- =====================================================
    -- 1. USERS & AUTH
    -- Password demo: 123456
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'admin1')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'admin1@hotelbooking.test', N'admin1', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'ADMIN', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'admin2')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'admin2@hotelbooking.test', N'admin2', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'ADMIN', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_thaodiem')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'owner.thaodiem@hotelbooking.test', N'owner_thaodiem', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_sunhotel')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'owner.sunhotel@hotelbooking.test', N'owner_sunhotel', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_rose')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'owner.rose@hotelbooking.test', N'owner_rose', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_binh')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'binh.customer@hotelbooking.test', N'customer_binh', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_lan')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'lan.customer@hotelbooking.test', N'customer_lan', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1, N'284910', DATEADD(MINUTE, 10, GETDATE()), N'FORGOT_PASSWORD');

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_minh')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'minh.customer@hotelbooking.test', N'customer_minh', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_anh')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'anh.customer@hotelbooking.test', N'customer_anh', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1, NULL, NULL, NULL);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_khoa')
        INSERT INTO user_accounts (email, username, password, role, enabled, otp, otp_expiry, otp_type)
        VALUES (N'khoa.customer@hotelbooking.test', N'customer_khoa', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1, NULL, NULL, NULL);

    SELECT @admin1_user_id = id FROM user_accounts WHERE username = N'admin1';
    SELECT @admin2_user_id = id FROM user_accounts WHERE username = N'admin2';
    SELECT @owner1_user_id = id FROM user_accounts WHERE username = N'owner_thaodiem';
    SELECT @owner2_user_id = id FROM user_accounts WHERE username = N'owner_sunhotel';
    SELECT @owner3_user_id = id FROM user_accounts WHERE username = N'owner_rose';
    SELECT @customer1_user_id = id FROM user_accounts WHERE username = N'customer_binh';
    SELECT @customer2_user_id = id FROM user_accounts WHERE username = N'customer_lan';
    SELECT @customer3_user_id = id FROM user_accounts WHERE username = N'customer_minh';
    SELECT @customer4_user_id = id FROM user_accounts WHERE username = N'customer_anh';
    SELECT @customer5_user_id = id FROM user_accounts WHERE username = N'customer_khoa';

    IF NOT EXISTS (SELECT 1 FROM admins WHERE user_account_id = @admin1_user_id)
        INSERT INTO admins (user_account_id, full_name, phone)
        VALUES (@admin1_user_id, N'Nguyễn Minh Admin', N'+84901000001');

    IF NOT EXISTS (SELECT 1 FROM admins WHERE user_account_id = @admin2_user_id)
        INSERT INTO admins (user_account_id, full_name, phone)
        VALUES (@admin2_user_id, N'Trần Bảo Admin', N'+84901000002');

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner1_user_id)
        INSERT INTO hotel_owners (
            user_account_id, full_name, phone, address, id_card, tax_id,
            verification_status, verified_at, rejection_reason, id_card_document
        )
        VALUES (
            @owner1_user_id, N'Lê Thảo Điểm', N'+84912000001',
            N'12 Hai Bà Trưng, Ninh Kiều, Cần Thơ',
            N'092345678901', N'TAX-TD-001',
            N'APPROVED', DATEADD(DAY, -40, GETDATE()), NULL,
            N'/docs/owners/owner_thaodiem_id_card.pdf'
        );

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner2_user_id)
        INSERT INTO hotel_owners (
            user_account_id, full_name, phone, address, id_card, tax_id,
            verification_status, verified_at, rejection_reason, id_card_document
        )
        VALUES (
            @owner2_user_id, N'Phạm Nhật Minh', N'+84912000002',
            N'88 Trần Văn Khéo, Ninh Kiều, Cần Thơ',
            N'092345678902', N'TAX-SUN-002',
            N'APPROVED', DATEADD(DAY, -35, GETDATE()), NULL,
            N'/docs/owners/owner_sunhotel_id_card.pdf'
        );

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner3_user_id)
        INSERT INTO hotel_owners (
            user_account_id, full_name, phone, address, id_card, tax_id,
            verification_status, verified_at, rejection_reason, id_card_document
        )
        VALUES (
            @owner3_user_id, N'Võ Ngọc Rose', N'+84912000003',
            N'25 Nguyễn Văn Cừ, Ninh Kiều, Cần Thơ',
            N'092345678903', N'TAX-ROSE-003',
            N'APPROVED', DATEADD(DAY, -30, GETDATE()), NULL,
            N'/docs/owners/owner_rose_id_card.pdf'
        );

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @customer1_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@customer1_user_id, N'Lưu Hữu Bình', N'+84933000001', N'Xuân Khánh, Ninh Kiều', N'Cần Thơ', N'Việt Nam', '2005-03-16', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @customer2_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@customer2_user_id, N'Nguyễn Thị Lan', N'+84933000002', N'Cái Răng', N'Cần Thơ', N'Việt Nam', '2002-07-22', N'FEMALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @customer3_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@customer3_user_id, N'Trần Đức Minh', N'+84933000003', N'Bình Thủy', N'Cần Thơ', N'Việt Nam', '2001-11-09', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @customer4_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@customer4_user_id, N'Phạm Hoài Anh', N'+84933000004', N'Ô Môn', N'Cần Thơ', N'Việt Nam', '2003-01-28', N'FEMALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @customer5_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@customer5_user_id, N'Lê Gia Khoa', N'+84933000005', N'Thốt Nốt', N'Cần Thơ', N'Việt Nam', '2000-09-14', N'MALE');

    SELECT @admin1_id = id FROM admins WHERE user_account_id = @admin1_user_id;
    SELECT @admin2_id = id FROM admins WHERE user_account_id = @admin2_user_id;
    SELECT @owner1_id = id FROM hotel_owners WHERE user_account_id = @owner1_user_id;
    SELECT @owner2_id = id FROM hotel_owners WHERE user_account_id = @owner2_user_id;
    SELECT @owner3_id = id FROM hotel_owners WHERE user_account_id = @owner3_user_id;
    SELECT @customer1_id = id FROM customers WHERE user_account_id = @customer1_user_id;
    SELECT @customer2_id = id FROM customers WHERE user_account_id = @customer2_user_id;
    SELECT @customer3_id = id FROM customers WHERE user_account_id = @customer3_user_id;
    SELECT @customer4_id = id FROM customers WHERE user_account_id = @customer4_user_id;
    SELECT @customer5_id = id FROM customers WHERE user_account_id = @customer5_user_id;

    -- =====================================================
    -- 2. HOTELS
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner1_id AND name = N'Thảo Điểm')
        INSERT INTO hotel (
            owner_id, name, address, city, district, image_url, description,
            rating, total_reviews, approval_status, active, approved_at, rejection_reason
        )
        VALUES (
            @owner1_id, N'Thảo Điểm', N'15 Hai Bà Trưng, Bến Ninh Kiều',
            N'Cần Thơ', N'Ninh Kiều', N'/images/hotels/thao-diem.jpg',
            N'Khách sạn gần bến Ninh Kiều, phù hợp cho khách du lịch và công tác.',
            4.8, 2, N'APPROVED', 1, DATEADD(DAY, -25, GETDATE()), NULL
        );

    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner2_id AND name = N'Sun Hotel')
        INSERT INTO hotel (
            owner_id, name, address, city, district, image_url, description,
            rating, total_reviews, approval_status, active, approved_at, rejection_reason
        )
        VALUES (
            @owner2_id, N'Sun Hotel', N'88 Trần Văn Khéo',
            N'Cần Thơ', N'Ninh Kiều', N'/images/hotels/sun-hotel.jpg',
            N'Khách sạn hiện đại, phòng rộng, thuận tiện di chuyển trong trung tâm thành phố.',
            4.2, 1, N'APPROVED', 1, DATEADD(DAY, -22, GETDATE()), NULL
        );

    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner3_id AND name = N'Rose Boutique')
        INSERT INTO hotel (
            owner_id, name, address, city, district, image_url, description,
            rating, total_reviews, approval_status, active, approved_at, rejection_reason
        )
        VALUES (
            @owner3_id, N'Rose Boutique', N'25 Nguyễn Văn Cừ',
            N'Cần Thơ', N'Ninh Kiều', N'/images/hotels/rose-boutique.jpg',
            N'Boutique hotel phong cách nhẹ nhàng, thích hợp cho cặp đôi và gia đình nhỏ.',
            4.6, 1, N'APPROVED', 1, DATEADD(DAY, -20, GETDATE()), NULL
        );

    SELECT @hotel1_id = id FROM hotel WHERE owner_id = @owner1_id AND name = N'Thảo Điểm';
    SELECT @hotel2_id = id FROM hotel WHERE owner_id = @owner2_id AND name = N'Sun Hotel';
    SELECT @hotel3_id = id FROM hotel WHERE owner_id = @owner3_id AND name = N'Rose Boutique';

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @hotel1_id)
        INSERT INTO hotel_verification_documents (
            hotel_id, phone, email, business_registration_doc, land_certificate_doc,
            rental_contract_doc, upload_status, rejection_reason, verified_at
        )
        VALUES (
            @hotel1_id, N'+842923880001', N'thaodiem@hotelbooking.test',
            N'/docs/hotels/thao-diem/business_registration.pdf',
            N'/docs/hotels/thao-diem/land_certificate.pdf',
            N'/docs/hotels/thao-diem/rental_contract.pdf',
            N'APPROVED', NULL, DATEADD(DAY, -25, GETDATE())
        );

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @hotel2_id)
        INSERT INTO hotel_verification_documents (
            hotel_id, phone, email, business_registration_doc, land_certificate_doc,
            rental_contract_doc, upload_status, rejection_reason, verified_at
        )
        VALUES (
            @hotel2_id, N'+842923880002', N'sunhotel@hotelbooking.test',
            N'/docs/hotels/sun-hotel/business_registration.pdf',
            N'/docs/hotels/sun-hotel/land_certificate.pdf',
            N'/docs/hotels/sun-hotel/rental_contract.pdf',
            N'APPROVED', NULL, DATEADD(DAY, -22, GETDATE())
        );

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @hotel3_id)
        INSERT INTO hotel_verification_documents (
            hotel_id, phone, email, business_registration_doc, land_certificate_doc,
            rental_contract_doc, upload_status, rejection_reason, verified_at
        )
        VALUES (
            @hotel3_id, N'+842923880003', N'roseboutique@hotelbooking.test',
            N'/docs/hotels/rose-boutique/business_registration.pdf',
            N'/docs/hotels/rose-boutique/land_certificate.pdf',
            N'/docs/hotels/rose-boutique/rental_contract.pdf',
            N'APPROVED', NULL, DATEADD(DAY, -20, GETDATE())
        );

    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @hotel1_id)
        INSERT INTO hotel_facilities (
            hotel_id, parking, restaurant, breakfast_available, fitness_centre,
            non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi,
            ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub
        )
        VALUES (@hotel1_id, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1);

    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @hotel2_id)
        INSERT INTO hotel_facilities (
            hotel_id, parking, restaurant, breakfast_available, fitness_centre,
            non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi,
            ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub
        )
        VALUES (@hotel2_id, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1);

    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @hotel3_id)
        INSERT INTO hotel_facilities (
            hotel_id, parking, restaurant, breakfast_available, fitness_centre,
            non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi,
            ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub
        )
        VALUES (@hotel3_id, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 1);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @hotel1_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@hotel1_id, 1, 0, 0, 0, 1, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @hotel2_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@hotel2_id, 1, 0, 0, 1, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @hotel3_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@hotel3_id, 0, 0, 1, 0, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @hotel1_id AND title = N'Ưu đãi cuối tuần')
        INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
        VALUES (@hotel1_id, N'Ưu đãi cuối tuần', N'Giảm giá cho khách đặt phòng từ thứ Sáu đến Chủ Nhật.', 10.00, '2026-07-01', '2026-08-31', N'ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @hotel2_id AND title = N'Summer Deal')
        INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
        VALUES (@hotel2_id, N'Summer Deal', N'Ưu đãi mùa hè cho phòng Deluxe và Suite.', 15.00, '2026-07-01', '2026-09-15', N'ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @hotel3_id AND title = N'Rose Couple Package')
        INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
        VALUES (@hotel3_id, N'Rose Couple Package', N'Gói phòng cho cặp đôi kèm bữa sáng.', 12.50, '2026-07-01', '2026-08-15', N'ACTIVE');

    -- =====================================================
    -- 3. ROOMS
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel1_id AND room_type = N'Standard Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel1_id, N'Standard Room', 10, N'Phòng tiêu chuẩn, đầy đủ tiện nghi cơ bản.', 900000, 22, 1, 2, 1, N'/images/rooms/thao-diem-standard.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel1_id AND room_type = N'Deluxe Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel1_id, N'Deluxe Room', 8, N'Phòng Deluxe có view thành phố, phù hợp cho 2 khách.', 1500000, 30, 1, 2, 2, N'/images/rooms/thao-diem-deluxe.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel1_id AND room_type = N'Suite Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel1_id, N'Suite Room', 3, N'Phòng Suite rộng, có khu vực tiếp khách riêng.', 3200000, 55, 2, 4, 3, N'/images/rooms/thao-diem-suite.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel2_id AND room_type = N'Superior Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel2_id, N'Superior Room', 12, N'Phòng Superior hiện đại, thích hợp cho khách công tác.', 1200000, 28, 1, 2, 2, N'/images/rooms/sun-superior.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel2_id AND room_type = N'Double Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel2_id, N'Double Room', 10, N'Phòng giường đôi, không gian rộng và thoải mái.', 1800000, 35, 2, 4, 2, N'/images/rooms/sun-double.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel2_id AND room_type = N'Deluxe City View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel2_id, N'Deluxe City View', 6, N'Phòng Deluxe có view thành phố và hồ bơi.', 2500000, 42, 1, 2, 3, N'/images/rooms/sun-deluxe-city-view.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel3_id AND room_type = N'Standard Garden Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel3_id, N'Standard Garden Room', 9, N'Phòng tiêu chuẩn có view sân vườn yên tĩnh.', 1100000, 25, 1, 2, 1, N'/images/rooms/rose-standard-garden.jpg', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @hotel3_id AND room_type = N'Rose Boutique Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@hotel3_id, N'Rose Boutique Suite', 4, N'Phòng Suite cao cấp phong cách boutique.', 4500000, 60, 2, 4, 3, N'/images/rooms/rose-suite.jpg', 1);

    SELECT @room1_id = id FROM room WHERE hotel_id = @hotel1_id AND room_type = N'Standard Room';
    SELECT @room2_id = id FROM room WHERE hotel_id = @hotel1_id AND room_type = N'Deluxe Room';
    SELECT @room3_id = id FROM room WHERE hotel_id = @hotel1_id AND room_type = N'Suite Room';
    SELECT @room4_id = id FROM room WHERE hotel_id = @hotel2_id AND room_type = N'Superior Room';
    SELECT @room5_id = id FROM room WHERE hotel_id = @hotel2_id AND room_type = N'Double Room';
    SELECT @room6_id = id FROM room WHERE hotel_id = @hotel2_id AND room_type = N'Deluxe City View';
    SELECT @room7_id = id FROM room WHERE hotel_id = @hotel3_id AND room_type = N'Standard Garden Room';
    SELECT @room8_id = id FROM room WHERE hotel_id = @hotel3_id AND room_type = N'Rose Boutique Suite';

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room1_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room1_id, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room2_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 2);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room3_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room3_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room4_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room4_id, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 2);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room5_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room5_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room6_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room6_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room7_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room7_id, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2);

    IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @room8_id)
        INSERT INTO room_facilities (
            room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers,
            hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk,
            television, telephone, iron, electric_kettle, cable_channels,
            wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
        )
        VALUES (@room8_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4);

    -- =====================================================
    -- 4. BOOKINGS
    -- =====================================================
    IF NOT EXISTS (
        SELECT 1 FROM bookings
        WHERE customer_id = @customer1_id AND room_id = @room2_id
          AND check_in_date = '2026-07-10' AND check_out_date = '2026-07-12'
    )
        INSERT INTO bookings (
            customer_id, room_id, hotel_id, phone, full_name, check_in_date, check_out_date,
            num_nights, total_price, status, special_notes
        )
        VALUES (@customer1_id, @room2_id, @hotel1_id, N'+84933000001', N'Lưu Hữu Bình', '2026-07-10', '2026-07-12', 2, 3000000, N'CONFIRMED', N'Khách muốn nhận phòng gần thang máy.');

    IF NOT EXISTS (
        SELECT 1 FROM bookings
        WHERE customer_id = @customer2_id AND room_id = @room6_id
          AND check_in_date = '2026-07-15' AND check_out_date = '2026-07-17'
    )
        INSERT INTO bookings (
            customer_id, room_id, hotel_id, phone, full_name, check_in_date, check_out_date,
            num_nights, total_price, status, special_notes
        )
        VALUES (@customer2_id, @room6_id, @hotel2_id, N'+84933000002', N'Nguyễn Thị Lan', '2026-07-15', '2026-07-17', 2, 5000000, N'PENDING', N'Yêu cầu phòng tầng cao.');

    IF NOT EXISTS (
        SELECT 1 FROM bookings
        WHERE customer_id = @customer3_id AND room_id = @room8_id
          AND check_in_date = '2026-08-01' AND check_out_date = '2026-08-03'
    )
        INSERT INTO bookings (
            customer_id, room_id, hotel_id, phone, full_name, check_in_date, check_out_date,
            num_nights, total_price, status, special_notes
        )
        VALUES (@customer3_id, @room8_id, @hotel3_id, N'+84933000003', N'Trần Đức Minh', '2026-08-01', '2026-08-03', 2, 9000000, N'CONFIRMED', N'Cần thêm gối và nước suối.');

    IF NOT EXISTS (
        SELECT 1 FROM bookings
        WHERE customer_id = @customer4_id AND room_id = @room1_id
          AND check_in_date = '2026-06-05' AND check_out_date = '2026-06-07'
    )
        INSERT INTO bookings (
            customer_id, room_id, hotel_id, phone, full_name, check_in_date, check_out_date,
            num_nights, total_price, status, special_notes
        )
        VALUES (@customer4_id, @room1_id, @hotel1_id, N'+84933000004', N'Phạm Hoài Anh', '2026-06-05', '2026-06-07', 2, 1800000, N'COMPLETED', N'Khách đã check out đúng hạn.');

    IF NOT EXISTS (
        SELECT 1 FROM bookings
        WHERE customer_id = @customer5_id AND room_id = @room4_id
          AND check_in_date = '2026-06-20' AND check_out_date = '2026-06-22'
    )
        INSERT INTO bookings (
            customer_id, room_id, hotel_id, phone, full_name, check_in_date, check_out_date,
            num_nights, total_price, status, special_notes
        )
        VALUES (@customer5_id, @room4_id, @hotel2_id, N'+84933000005', N'Lê Gia Khoa', '2026-06-20', '2026-06-22', 2, 2400000, N'COMPLETED', N'Khách hài lòng với dịch vụ.');

    SELECT @booking1_id = id FROM bookings WHERE customer_id = @customer1_id AND room_id = @room2_id AND check_in_date = '2026-07-10' AND check_out_date = '2026-07-12';
    SELECT @booking2_id = id FROM bookings WHERE customer_id = @customer2_id AND room_id = @room6_id AND check_in_date = '2026-07-15' AND check_out_date = '2026-07-17';
    SELECT @booking3_id = id FROM bookings WHERE customer_id = @customer3_id AND room_id = @room8_id AND check_in_date = '2026-08-01' AND check_out_date = '2026-08-03';
    SELECT @booking4_id = id FROM bookings WHERE customer_id = @customer4_id AND room_id = @room1_id AND check_in_date = '2026-06-05' AND check_out_date = '2026-06-07';
    SELECT @booking5_id = id FROM bookings WHERE customer_id = @customer5_id AND room_id = @room4_id AND check_in_date = '2026-06-20' AND check_out_date = '2026-06-22';

    -- =====================================================
    -- 5. PAYMENTS
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @booking1_id AND transaction_id = N'TXN-BK001-20260701')
        INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
        VALUES (@booking1_id, 3000000, N'QR_CODE', N'PAID', N'/qr/payments/booking-1.png', N'TXN-BK001-20260701', DATEADD(DAY, -1, GETDATE()), DATEADD(HOUR, 24, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @booking2_id AND qr_code_url = N'/qr/payments/booking-2.png')
        INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
        VALUES (@booking2_id, 5000000, N'QR_CODE', N'PENDING', N'/qr/payments/booking-2.png', NULL, NULL, DATEADD(HOUR, 24, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @booking3_id AND transaction_id = N'TXN-BK003-20260701')
        INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
        VALUES (@booking3_id, 9000000, N'BANK_TRANSFER', N'PAID', N'/qr/payments/booking-3.png', N'TXN-BK003-20260701', DATEADD(DAY, -2, GETDATE()), DATEADD(HOUR, 24, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @booking4_id AND transaction_id = N'TXN-BK004-20260604')
        INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
        VALUES (@booking4_id, 1800000, N'MOMO', N'PAID', N'/qr/payments/booking-4.png', N'TXN-BK004-20260604', '2026-06-04 20:15:00', '2026-06-04 23:59:00');

    IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @booking5_id AND qr_code_url = N'/qr/payments/booking-5.png')
        INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
        VALUES (@booking5_id, 2400000, N'QR_CODE', N'PENDING', N'/qr/payments/booking-5.png', NULL, NULL, DATEADD(HOUR, 12, GETDATE()));

    -- =====================================================
    -- 6. REFUNDS
    -- refunds.booking_id is UNIQUE, so insert one refund per booking only.
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @booking1_id)
        INSERT INTO refunds (
            booking_id, bank_name, account_number, account_holder,
            refund_amount, status, note, cancellation_reason, requested_at, processed_at
        )
        VALUES (
            @booking1_id, N'Vietcombank', N'0123456789', N'LUU HUU BINH',
            1500000, N'PENDING',
            N'Khách vừa gửi yêu cầu hoàn tiền do muốn đổi lịch đặt phòng.',
            N'Personal reasons/Trip called off',
            DATEADD(HOUR, -2, GETDATE()), NULL
        );

    IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @booking2_id)
        INSERT INTO refunds (
            booking_id, bank_name, account_number, account_holder,
            refund_amount, status, note, cancellation_reason, requested_at, processed_at
        )
        VALUES (
            @booking2_id, N'Techcombank', N'0987654321', N'NGUYEN THI LAN',
            5000000, N'PROCESSED',
            N'Hoàn tiền thành công do khách hủy trong thời gian miễn phí.',
            N'Found a different accommodation option',
            DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -4, GETDATE())
        );

    IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @booking3_id)
        INSERT INTO refunds (
            booking_id, bank_name, account_number, account_holder,
            refund_amount, status, note, cancellation_reason, requested_at, processed_at
        )
        VALUES (
            @booking3_id, N'MB Bank', N'1234567890', N'TRAN DUC MINH',
            4500000, N'PROCESSED',
            N'Hoàn tiền một phần theo chính sách của khách sạn.',
            N'Change of dates or destination',
            DATEADD(DAY, -7, GETDATE()), DATEADD(DAY, -6, GETDATE())
        );

    IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @booking4_id)
        INSERT INTO refunds (
            booking_id, bank_name, account_number, account_holder,
            refund_amount, status, note, cancellation_reason, requested_at, processed_at
        )
        VALUES (
            @booking4_id, N'ACB', N'1122334455', N'PHAM HOAI ANH',
            1800000, N'REJECTED',
            N'Từ chối do yêu cầu hoàn tiền sau thời hạn policy.',
            N'Change in the number or needs of travelers',
            DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -2, GETDATE())
        );

    -- =====================================================
    -- 7. FEEDBACK + OWNER REPLIES
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @customer1_id AND hotel_id = @hotel1_id AND room_id = @room2_id)
        INSERT INTO feedback (
            customer_id, hotel_id, room_id, user_full_name, room_type,
            comment, rating, upvote, downvote, status
        )
        VALUES (@customer1_id, @hotel1_id, @room2_id, N'Lưu Hữu Bình', N'Deluxe Room', N'Phòng sạch, vị trí gần trung tâm, nhân viên hỗ trợ nhanh.', 5, 8, 0, N'VISIBLE');

    IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @customer2_id AND hotel_id = @hotel2_id AND room_id = @room6_id)
        INSERT INTO feedback (
            customer_id, hotel_id, room_id, user_full_name, room_type,
            comment, rating, upvote, downvote, status
        )
        VALUES (@customer2_id, @hotel2_id, @room6_id, N'Nguyễn Thị Lan', N'Deluxe City View', N'View đẹp, hồ bơi ổn nhưng thời gian check-in hơi lâu.', 4, 5, 1, N'VISIBLE');

    IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @customer4_id AND hotel_id = @hotel1_id AND room_id = @room1_id)
        INSERT INTO feedback (
            customer_id, hotel_id, room_id, user_full_name, room_type,
            comment, rating, upvote, downvote, status
        )
        VALUES (@customer4_id, @hotel1_id, @room1_id, N'Phạm Hoài Anh', N'Standard Room', N'Phòng ổn so với giá, nhưng cách âm chưa tốt.', 3, 3, 2, N'VISIBLE');

    IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @customer5_id AND hotel_id = @hotel3_id AND room_id = @room8_id)
        INSERT INTO feedback (
            customer_id, hotel_id, room_id, user_full_name, room_type,
            comment, rating, upvote, downvote, status
        )
        VALUES (@customer5_id, @hotel3_id, @room8_id, N'Lê Gia Khoa', N'Rose Boutique Suite', N'Không gian đẹp, phù hợp nghỉ dưỡng, bữa sáng ngon.', 5, 10, 0, N'VISIBLE');

    SELECT @feedback1_id = id FROM feedback WHERE customer_id = @customer1_id AND hotel_id = @hotel1_id AND room_id = @room2_id;
    SELECT @feedback2_id = id FROM feedback WHERE customer_id = @customer2_id AND hotel_id = @hotel2_id AND room_id = @room6_id;
    SELECT @feedback3_id = id FROM feedback WHERE customer_id = @customer4_id AND hotel_id = @hotel1_id AND room_id = @room1_id;
    SELECT @feedback4_id = id FROM feedback WHERE customer_id = @customer5_id AND hotel_id = @hotel3_id AND room_id = @room8_id;

    IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @feedback1_id AND owner_id = @owner1_id)
        INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
        VALUES (@feedback1_id, @owner1_id, @hotel1_id, N'Cảm ơn bạn đã lựa chọn Thảo Điểm. Khách sạn rất vui khi bạn hài lòng với trải nghiệm.');

    IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @feedback2_id AND owner_id = @owner2_id)
        INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
        VALUES (@feedback2_id, @owner2_id, @hotel2_id, N'Cảm ơn góp ý của bạn. Sun Hotel sẽ cải thiện quy trình check-in để phục vụ nhanh hơn.');

    IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @feedback3_id AND owner_id = @owner1_id)
        INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
        VALUES (@feedback3_id, @owner1_id, @hotel1_id, N'Cảm ơn phản hồi của bạn. Khách sạn sẽ kiểm tra lại vấn đề cách âm phòng.');

    IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @feedback4_id AND owner_id = @owner3_id)
        INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
        VALUES (@feedback4_id, @owner3_id, @hotel3_id, N'Rose Boutique rất cảm ơn đánh giá của bạn và mong được đón tiếp bạn lần sau.');

    -- Thêm feedback PENDING và HIDDEN để kiểm duyệt
    IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @customer2_id AND hotel_id = @hotel1_id AND room_id = @room1_id)
        INSERT INTO feedback (
            customer_id, hotel_id, room_id, user_full_name, room_type,
            comment, rating, upvote, downvote, status
        )
        VALUES (@customer2_id, @hotel1_id, @room1_id, N'Nguyễn Thị Lan', N'Standard Room', N'Phòng có quá nhiều muỗi và thái độ phục vụ của nhân viên rất tệ!', 1, 0, 0, N'PENDING');

    IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @customer3_id AND hotel_id = @hotel1_id AND room_id = @room1_id)
        INSERT INTO feedback (
            customer_id, hotel_id, room_id, user_full_name, room_type,
            comment, rating, upvote, downvote, status
        )
        VALUES (@customer3_id, @hotel1_id, @room1_id, N'Trần Văn Đạo', N'Standard Room', N'Khách sạn này lừa đảo, quảng cáo sai sự thật!', 1, 0, 0, N'HIDDEN');

    -- =====================================================
    -- 7.5. FEEDBACK VOTES (SAMPLE DATA)
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM feedback_votes WHERE feedback_id = @feedback1_id AND customer_id = @customer2_id)
        INSERT INTO feedback_votes (feedback_id, customer_id, vote_type)
        VALUES (@feedback1_id, @customer2_id, N'UPVOTE');

    IF NOT EXISTS (SELECT 1 FROM feedback_votes WHERE feedback_id = @feedback1_id AND customer_id = @customer3_id)
        INSERT INTO feedback_votes (feedback_id, customer_id, vote_type)
        VALUES (@feedback1_id, @customer3_id, N'UPVOTE');

    IF NOT EXISTS (SELECT 1 FROM feedback_votes WHERE feedback_id = @feedback2_id AND customer_id = @customer1_id)
        INSERT INTO feedback_votes (feedback_id, customer_id, vote_type)
        VALUES (@feedback2_id, @customer1_id, N'UPVOTE');

    IF NOT EXISTS (SELECT 1 FROM feedback_votes WHERE feedback_id = @feedback3_id AND customer_id = @customer1_id)
        INSERT INTO feedback_votes (feedback_id, customer_id, vote_type)
        VALUES (@feedback3_id, @customer1_id, N'DOWNVOTE');

    -- =====================================================
    -- 8. WISHLISTS
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @customer1_id AND room_id = @room8_id AND check_in_date = '2026-08-10')
        INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
        VALUES (@customer1_id, @room8_id, '2026-08-10', '2026-08-12');

    IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @customer2_id AND room_id = @room3_id AND check_in_date = '2026-07-20')
        INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
        VALUES (@customer2_id, @room3_id, '2026-07-20', '2026-07-22');

    IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @customer3_id AND room_id = @room5_id AND check_in_date = '2026-09-01')
        INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
        VALUES (@customer3_id, @room5_id, '2026-09-01', '2026-09-03');

    IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @customer4_id AND room_id = @room6_id AND check_in_date = '2026-07-25')
        INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
        VALUES (@customer4_id, @room6_id, '2026-07-25', '2026-07-27');

    IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @customer5_id AND room_id = @room2_id AND check_in_date = '2026-08-05')
        INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
        VALUES (@customer5_id, @room2_id, '2026-08-05', '2026-08-07');

    -- =====================================================
    -- 9. MESSAGES
    -- sender_id / receiver_id reference user_accounts.id
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @customer1_user_id AND receiver_id = @owner1_user_id AND booking_id = @booking1_id AND content = N'Chào khách sạn, mình có thể check-in sớm lúc 12h không?')
        INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
        VALUES (@customer1_user_id, @owner1_user_id, @booking1_id, @hotel1_id, N'Chào khách sạn, mình có thể check-in sớm lúc 12h không?', 1, DATEADD(HOUR, -6, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @owner1_user_id AND receiver_id = @customer1_user_id AND booking_id = @booking1_id AND content = N'Chào bạn, khách sạn sẽ hỗ trợ nếu phòng sẵn sàng trước giờ check-in.')
        INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
        VALUES (@owner1_user_id, @customer1_user_id, @booking1_id, @hotel1_id, N'Chào bạn, khách sạn sẽ hỗ trợ nếu phòng sẵn sàng trước giờ check-in.', 0, DATEADD(HOUR, -5, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @customer2_user_id AND receiver_id = @owner2_user_id AND booking_id = @booking2_id AND content = N'Phòng Deluxe City View có bao gồm bữa sáng không ạ?')
        INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
        VALUES (@customer2_user_id, @owner2_user_id, @booking2_id, @hotel2_id, N'Phòng Deluxe City View có bao gồm bữa sáng không ạ?', 1, DATEADD(HOUR, -4, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @owner2_user_id AND receiver_id = @customer2_user_id AND booking_id = @booking2_id AND content = N'Dạ có, giá phòng đã bao gồm bữa sáng cho 2 khách.')
        INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
        VALUES (@owner2_user_id, @customer2_user_id, @booking2_id, @hotel2_id, N'Dạ có, giá phòng đã bao gồm bữa sáng cho 2 khách.', 0, DATEADD(HOUR, -3, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @customer3_user_id AND receiver_id = @owner3_user_id AND booking_id = @booking3_id AND content = N'Mình muốn yêu cầu thêm gối cho phòng Suite.')
        INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
        VALUES (@customer3_user_id, @owner3_user_id, @booking3_id, @hotel3_id, N'Mình muốn yêu cầu thêm gối cho phòng Suite.', 0, DATEADD(HOUR, -2, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @customer5_user_id AND receiver_id = @owner2_user_id AND booking_id = @booking5_id AND content = N'Mình đã thanh toán nhưng hệ thống vẫn hiện pending, nhờ khách sạn kiểm tra giúp.')
        INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
        VALUES (@customer5_user_id, @owner2_user_id, @booking5_id, @hotel2_id, N'Mình đã thanh toán nhưng hệ thống vẫn hiện pending, nhờ khách sạn kiểm tra giúp.', 0, DATEADD(HOUR, -1, GETDATE()));

    COMMIT TRANSACTION;

    PRINT N'✅ Sample data inserted successfully without DELETE and without DBCC CHECKIDENT.';
    PRINT N'You can run this script multiple times. Existing rows will be skipped.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
    DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
    DECLARE @ErrorState INT = ERROR_STATE();

    RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
END CATCH;
GO
