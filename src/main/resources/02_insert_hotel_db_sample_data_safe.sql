-- =====================================================
-- HOTEL BOOKING SYSTEM - REAL DATA SCRIPT (UNIQUE IMAGES)
-- File: 02_insert_hotel_db_real_data.sql
-- Target: Microsoft SQL Server / SSMS
-- Description: Insert REAL data for 10 famous hotels in Vietnam
-- WITH UNIQUE IMAGES FOR EACH HOTEL & ROOM
-- Safe to run multiple times (checks existing data)
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
        @admin1_user_id INT, @admin2_user_id INT,
        
        -- Owner User IDs
        @owner1_user_id INT, @owner2_user_id INT, @owner3_user_id INT,
        @owner4_user_id INT, @owner5_user_id INT, @owner6_user_id INT,
        @owner7_user_id INT, @owner8_user_id INT, @owner9_user_id INT,
        @owner10_user_id INT,
        
        -- Owner IDs
        @owner1_id INT, @owner2_id INT, @owner3_id INT, @owner4_id INT,
        @owner5_id INT, @owner6_id INT, @owner7_id INT, @owner8_id INT,
        @owner9_id INT, @owner10_id INT,
        
        -- Customer User IDs
        @c1_user_id INT, @c2_user_id INT, @c3_user_id INT, @c4_user_id INT,
        @c5_user_id INT, @c6_user_id INT, @c7_user_id INT, @c8_user_id INT,
        @c9_user_id INT, @c10_user_id INT,
        
        -- Customer IDs
        @c1_id INT, @c2_id INT, @c3_id INT, @c4_id INT, @c5_id INT,
        @c6_id INT, @c7_id INT, @c8_id INT, @c9_id INT, @c10_id INT,
        
        -- Hotel IDs
        @h1_id INT, @h2_id INT, @h3_id INT, @h4_id INT, @h5_id INT,
        @h6_id INT, @h7_id INT, @h8_id INT, @h9_id INT, @h10_id INT,
        
        -- Room IDs - Hotel 1 (5 rooms)
        @h1_r1_id INT, @h1_r2_id INT, @h1_r3_id INT, @h1_r4_id INT, @h1_r5_id INT,
        -- Hotel 2 (5 rooms)
        @h2_r1_id INT, @h2_r2_id INT, @h2_r3_id INT, @h2_r4_id INT, @h2_r5_id INT,
        -- Hotel 3 (4 rooms)
        @h3_r1_id INT, @h3_r2_id INT, @h3_r3_id INT, @h3_r4_id INT,
        -- Hotel 4 (4 rooms)
        @h4_r1_id INT, @h4_r2_id INT, @h4_r3_id INT, @h4_r4_id INT,
        -- Hotel 5 (3 rooms)
        @h5_r1_id INT, @h5_r2_id INT, @h5_r3_id INT,
        -- Hotel 6 (2 rooms)
        @h6_r1_id INT, @h6_r2_id INT,
        -- Hotel 7 (2 rooms)
        @h7_r1_id INT, @h7_r2_id INT,
        -- Hotel 8 (1 room)
        @h8_r1_id INT,
        -- Hotel 9 (1 room)
        @h9_r1_id INT,
        -- Hotel 10 (1 room)
        @h10_r1_id INT;

    -- =====================================================
    -- 1. USERS & AUTH
    -- Password: 123456 (hash: $2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S)
    -- =====================================================
    
    -- Admins
    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'admin1')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'admin1@hotelbooking.test', N'admin1', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'ADMIN', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'admin2')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'admin2@hotelbooking.test', N'admin2', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'ADMIN', 1);

    -- Hotel Owners (10 owners)
    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_intercontinental')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.intercontinental@hotel.test', N'owner_intercontinental', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_vinpearl')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.vinpearl@hotel.test', N'owner_vinpearl', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_fourseasons')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.fourseasons@hotel.test', N'owner_fourseasons', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_dalatpalace')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.dalatpalace@hotel.test', N'owner_dalatpalace', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_jwmarriott')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.jwmarriott@hotel.test', N'owner_jwmarriott', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_coupole')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.coupole@hotel.test', N'owner_coupole', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_metropole')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.metropole@hotel.test', N'owner_metropole', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_parkhyatt')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.parkhyatt@hotel.test', N'owner_parkhyatt', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_pilgrimage')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.pilgrimage@hotel.test', N'owner_pilgrimage', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_victoria')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'owner.victoria@hotel.test', N'owner_victoria', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'HOTEL_OWNER', 1);

    -- Customers (10 customers)
    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_hoang')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'hoang.nguyen@customer.test', N'customer_hoang', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_linh')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'linh.tran@customer.test', N'customer_linh', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_duc')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'duc.pham@customer.test', N'customer_duc', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_mai')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'mai.le@customer.test', N'customer_mai', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_quang')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'quang.nguyen@customer.test', N'customer_quang', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_ha')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'ha.vo@customer.test', N'customer_ha', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_kien')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'kien.hoang@customer.test', N'customer_kien', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_anh')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'thanh.anh@customer.test', N'customer_anh', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_binh')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'binh.le@customer.test', N'customer_binh', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_loan')
        INSERT INTO user_accounts (email, username, password, role, enabled)
        VALUES (N'loan.huynh@customer.test', N'customer_loan', N'$2a$10$OLaK3rp6WQ7kU78fmXpFe.DbWKCsLQixn7ro0/ZnqK6UwgAhPTW.S', N'CUSTOMER', 1);

    -- Get User IDs
    SELECT @admin1_user_id = id FROM user_accounts WHERE username = N'admin1';
    SELECT @admin2_user_id = id FROM user_accounts WHERE username = N'admin2';
    SELECT @owner1_user_id = id FROM user_accounts WHERE username = N'owner_intercontinental';
    SELECT @owner2_user_id = id FROM user_accounts WHERE username = N'owner_vinpearl';
    SELECT @owner3_user_id = id FROM user_accounts WHERE username = N'owner_fourseasons';
    SELECT @owner4_user_id = id FROM user_accounts WHERE username = N'owner_dalatpalace';
    SELECT @owner5_user_id = id FROM user_accounts WHERE username = N'owner_jwmarriott';
    SELECT @owner6_user_id = id FROM user_accounts WHERE username = N'owner_coupole';
    SELECT @owner7_user_id = id FROM user_accounts WHERE username = N'owner_metropole';
    SELECT @owner8_user_id = id FROM user_accounts WHERE username = N'owner_parkhyatt';
    SELECT @owner9_user_id = id FROM user_accounts WHERE username = N'owner_pilgrimage';
    SELECT @owner10_user_id = id FROM user_accounts WHERE username = N'owner_victoria';
    SELECT @c1_user_id = id FROM user_accounts WHERE username = N'customer_hoang';
    SELECT @c2_user_id = id FROM user_accounts WHERE username = N'customer_linh';
    SELECT @c3_user_id = id FROM user_accounts WHERE username = N'customer_duc';
    SELECT @c4_user_id = id FROM user_accounts WHERE username = N'customer_mai';
    SELECT @c5_user_id = id FROM user_accounts WHERE username = N'customer_quang';
    SELECT @c6_user_id = id FROM user_accounts WHERE username = N'customer_ha';
    SELECT @c7_user_id = id FROM user_accounts WHERE username = N'customer_kien';
    SELECT @c8_user_id = id FROM user_accounts WHERE username = N'customer_anh';
    SELECT @c9_user_id = id FROM user_accounts WHERE username = N'customer_binh';
    SELECT @c10_user_id = id FROM user_accounts WHERE username = N'customer_loan';

    -- Insert Admins
    IF NOT EXISTS (SELECT 1 FROM admins WHERE user_account_id = @admin1_user_id)
        INSERT INTO admins (user_account_id, full_name, phone)
        VALUES (@admin1_user_id, N'Nguyễn Văn Admin', N'+84901000001');

    IF NOT EXISTS (SELECT 1 FROM admins WHERE user_account_id = @admin2_user_id)
        INSERT INTO admins (user_account_id, full_name, phone)
        VALUES (@admin2_user_id, N'Trần Thị Admin', N'+84901000002');

    -- Insert Hotel Owners
    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner1_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner1_user_id, N'Lê Minh Hoàng', N'+84912000001', N'123 Nguyễn Văn Linh, Đà Nẵng', N'012345678901', N'TAX-IC-001', N'APPROVED', DATEADD(DAY, -90, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner2_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner2_user_id, N'Phạm Thị Hoa', N'+84912000002', N'456 Trần Phú, Nha Trang', N'012345678902', N'TAX-VP-002', N'APPROVED', DATEADD(DAY, -85, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner3_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner3_user_id, N'Nguyễn Đức Anh', N'+84912000003', N'789 Lý Thường Kiệt, Hội An', N'012345678903', N'TAX-FS-003', N'APPROVED', DATEADD(DAY, -80, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner4_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner4_user_id, N'Trần Minh Tâm', N'+84912000004', N'12 Trần Phú, Đà Lạt', N'012345678904', N'TAX-DP-004', N'APPROVED', DATEADD(DAY, -75, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner5_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner5_user_id, N'Lê Thị Ngọc', N'+84912000005', N'Bãi Khem, An Thới, Phú Quốc', N'012345678905', N'TAX-JW-005', N'APPROVED', DATEADD(DAY, -70, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner6_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner6_user_id, N'Hoàng Văn Khôi', N'+84912000006', N'1 Hoàng Liên, Sapa', N'012345678906', N'TAX-CP-006', N'APPROVED', DATEADD(DAY, -65, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner7_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner7_user_id, N'Phan Thị Lan', N'+84912000007', N'15 Ngô Quyền, Hoàn Kiếm, Hà Nội', N'012345678907', N'TAX-MT-007', N'PENDING', NULL);

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner8_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner8_user_id, N'Đặng Quang Huy', N'+84912000008', N'2 Lê Lợi, Quận 1, TP.HCM', N'012345678908', N'TAX-PH-008', N'APPROVED', DATEADD(DAY, -50, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner9_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner9_user_id, N'Võ Thị Thu', N'+84912000009', N'130 Minh Mạng, Huế', N'012345678909', N'TAX-PG-009', N'REJECTED', DATEADD(DAY, -30, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner10_user_id)
        INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at)
        VALUES (@owner10_user_id, N'Nguyễn Hữu Phước', N'+84912000010', N'Cồn Cái Khế, Ninh Kiều, Cần Thơ', N'012345678910', N'TAX-VC-010', N'APPROVED', DATEADD(DAY, -40, GETDATE()));

    -- Get Owner IDs
    SELECT @owner1_id = id FROM hotel_owners WHERE user_account_id = @owner1_user_id;
    SELECT @owner2_id = id FROM hotel_owners WHERE user_account_id = @owner2_user_id;
    SELECT @owner3_id = id FROM hotel_owners WHERE user_account_id = @owner3_user_id;
    SELECT @owner4_id = id FROM hotel_owners WHERE user_account_id = @owner4_user_id;
    SELECT @owner5_id = id FROM hotel_owners WHERE user_account_id = @owner5_user_id;
    SELECT @owner6_id = id FROM hotel_owners WHERE user_account_id = @owner6_user_id;
    SELECT @owner7_id = id FROM hotel_owners WHERE user_account_id = @owner7_user_id;
    SELECT @owner8_id = id FROM hotel_owners WHERE user_account_id = @owner8_user_id;
    SELECT @owner9_id = id FROM hotel_owners WHERE user_account_id = @owner9_user_id;
    SELECT @owner10_id = id FROM hotel_owners WHERE user_account_id = @owner10_user_id;

    -- Insert Customers
    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c1_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c1_user_id, N'Nguyễn Văn Hoàng', N'+84933000001', N'123 Lê Lợi, Quận 1', N'TP.HCM', N'Việt Nam', '1995-03-15', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c2_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c2_user_id, N'Trần Thị Linh', N'+84933000002', N'456 Nguyễn Huệ, Quận 1', N'TP.HCM', N'Việt Nam', '1998-07-22', N'FEMALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c3_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c3_user_id, N'Phạm Đức', N'+84933000003', N'789 Trần Hưng Đạo, Quận 5', N'TP.HCM', N'Việt Nam', '2000-11-09', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c4_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c4_user_id, N'Lê Thị Mai', N'+84933000004', N'12 Nguyễn Thị Minh Khai, Quận 3', N'TP.HCM', N'Việt Nam', '1997-01-28', N'FEMALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c5_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c5_user_id, N'Nguyễn Quang', N'+84933000005', N'34 Hai Bà Trưng, Quận 1', N'TP.HCM', N'Việt Nam', '1994-09-14', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c6_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c6_user_id, N'Võ Thị Hà', N'+84933000006', N'56 Lý Tự Trọng, Quận 1', N'TP.HCM', N'Việt Nam', '1996-05-30', N'FEMALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c7_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c7_user_id, N'Hoàng Kiên', N'+84933000007', N'78 Điện Biên Phủ, Quận Bình Thạnh', N'TP.HCM', N'Việt Nam', '1999-08-17', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c8_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c8_user_id, N'Trần Thị Thanh Anh', N'+84933000008', N'90 Võ Văn Tần, Quận 3', N'TP.HCM', N'Việt Nam', '2001-12-02', N'FEMALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c9_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c9_user_id, N'Lê Bình', N'+84933000009', N'11 Nguyễn Đình Chiểu, Quận 3', N'TP.HCM', N'Việt Nam', '1993-06-20', N'MALE');

    IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c10_user_id)
        INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth, gender)
        VALUES (@c10_user_id, N'Huỳnh Thị Loan', N'+84933000010', N'22 Cách Mạng Tháng 8, Quận 10', N'TP.HCM', N'Việt Nam', '2002-03-11', N'FEMALE');

    -- Get Customer IDs
    SELECT @c1_id = id FROM customers WHERE user_account_id = @c1_user_id;
    SELECT @c2_id = id FROM customers WHERE user_account_id = @c2_user_id;
    SELECT @c3_id = id FROM customers WHERE user_account_id = @c3_user_id;
    SELECT @c4_id = id FROM customers WHERE user_account_id = @c4_user_id;
    SELECT @c5_id = id FROM customers WHERE user_account_id = @c5_user_id;
    SELECT @c6_id = id FROM customers WHERE user_account_id = @c6_user_id;
    SELECT @c7_id = id FROM customers WHERE user_account_id = @c7_user_id;
    SELECT @c8_id = id FROM customers WHERE user_account_id = @c8_user_id;
    SELECT @c9_id = id FROM customers WHERE user_account_id = @c9_user_id;
    SELECT @c10_id = id FROM customers WHERE user_account_id = @c10_user_id;

    -- =====================================================
    -- 2. HOTELS (10 REAL HOTELS WITH UNIQUE IMAGES)
    -- =====================================================
    
    -- Hotel 1: InterContinental Danang Sun Peninsula Resort (5 rooms) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner1_id AND name = N'InterContinental Danang Sun Peninsula Resort')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner1_id,
            N'InterContinental Danang Sun Peninsula Resort',
            N'Bãi Bắc, Sơn Trà, Đà Nẵng',
            N'Đà Nẵng',
            N'Sơn Trà',
            N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=500&fit=crop&q=80',
            N'InterContinental Danang Sun Peninsula Resort là khu nghỉ dưỡng 5 sao đẳng cấp quốc tế, tọa lạc trên bán đảo Sơn Trà với tầm nhìn tuyệt đẹp ra biển Đông. Resort sở hữu kiến trúc độc đáo, hồ bơi vô cực, nhà hàng Michelin và dịch vụ đẳng cấp.',
            4.9, 0, N'APPROVED', 1, DATEADD(DAY, -90, GETDATE())
        );
    END

    -- Hotel 2: Vinpearl Resort Nha Trang (5 rooms) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner2_id AND name = N'Vinpearl Resort Nha Trang')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner2_id,
            N'Vinpearl Resort Nha Trang',
            N'Đảo Hòn Tre, Nha Trang, Khánh Hòa',
            N'Nha Trang',
            N'Vĩnh Nguyên',
            N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=500&fit=crop&q=80',
            N'Vinpearl Resort Nha Trang là khu nghỉ dưỡng 5 sao tọa lạc trên đảo Hòn Tre, kết nối với đất liền bằng cáp treo vượt biển. Resort có bãi biển riêng, công viên nước và nhiều tiện ích cao cấp.',
            4.7, 0, N'APPROVED', 1, DATEADD(DAY, -85, GETDATE())
        );
    END

    -- Hotel 3: Four Seasons Resort The Nam Hai (4 rooms) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner3_id AND name = N'Four Seasons Resort The Nam Hai')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner3_id,
            N'Four Seasons Resort The Nam Hai',
            N'Điện Dương, Điện Bàn, Quảng Nam',
            N'Hội An',
            N'Điện Bàn',
            N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&h=500&fit=crop&q=80',
            N'Four Seasons Resort The Nam Hai là resort 5 sao sang trọng bậc nhất Việt Nam, nằm trên bãi biển Hà My thơ mộng. Kiến trúc kết hợp hài hòa giữa phong cách Việt Nam hiện đại và truyền thống.',
            4.8, 0, N'APPROVED', 1, DATEADD(DAY, -80, GETDATE())
        );
    END

    -- Hotel 4: Dalat Palace Heritage Hotel (4 rooms) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner4_id AND name = N'Dalat Palace Heritage Hotel')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner4_id,
            N'Dalat Palace Heritage Hotel',
            N'12 Trần Phú, Đà Lạt, Lâm Đồng',
            N'Đà Lạt',
            N'Phường 3',
            N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=500&fit=crop&q=80',
            N'Dalat Palace Heritage Hotel là khách sạn 5 sao cổ kính nằm bên hồ Xuân Hương thơ mộng. Được xây dựng từ thời Pháp thuộc, khách sạn mang vẻ đẹp cổ điển với không gian sang trọng và lãng mạn.',
            4.5, 0, N'APPROVED', 1, DATEADD(DAY, -75, GETDATE())
        );
    END

    -- Hotel 5: JW Marriott Phu Quoc Emerald Bay (3 rooms) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner5_id AND name = N'JW Marriott Phu Quoc Emerald Bay')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner5_id,
            N'JW Marriott Phu Quoc Emerald Bay',
            N'Bãi Khem, An Thới, Phú Quốc, Kiên Giang',
            N'Phú Quốc',
            N'An Thới',
            N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=800&h=500&fit=crop&q=80',
            N'JW Marriott Phu Quoc Emerald Bay là resort 5 sao với thiết kế độc đáo của kiến trúc sư Bill Bensley. Resort nằm trên bãi biển Kem tuyệt đẹp, mang phong cách trường đại học lịch sử.',
            4.9, 0, N'APPROVED', 1, DATEADD(DAY, -70, GETDATE())
        );
    END

    -- Hotel 6: Hotel de la Coupole - MGallery (2 rooms) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner6_id AND name = N'Hotel de la Coupole - MGallery')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner6_id,
            N'Hotel de la Coupole - MGallery',
            N'1 Hoàng Liên, Sapa, Lào Cai',
            N'Sapa',
            N'TT Sa Pa',
            N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=500&fit=crop&q=80',
            N'Hotel de la Coupole - MGallery là khách sạn 5 sao phong cách Pháp, nổi bật với kiến trúc thời trang và màu sắc rực rỡ. Khách sạn nằm giữa thị trấn Sapa, tầm nhìn hướng tới dãy núi Hoàng Liên Sơn hùng vĩ.',
            4.6, 0, N'APPROVED', 1, DATEADD(DAY, -65, GETDATE())
        );
    END

    -- Hotel 7: Sofitel Legend Metropole Hanoi (2 rooms) - PENDING, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner7_id AND name = N'Sofitel Legend Metropole Hanoi')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner7_id,
            N'Sofitel Legend Metropole Hanoi',
            N'15 Ngô Quyền, Hoàn Kiếm, Hà Nội',
            N'Hà Nội',
            N'Hoàn Kiếm',
            N'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800&h=500&fit=crop&q=80',
            N'Sofitel Legend Metropole Hanoi là khách sạn 5 sao lịch sử hơn 120 năm tuổi, nằm ngay trung tâm Hà Nội. Khách sạn đã đón tiếp nhiều nguyên thủ quốc gia và người nổi tiếng thế giới.',
            4.8, 0, N'PENDING', 1, NULL
        );
    END

    -- Hotel 8: Park Hyatt Saigon (1 room) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner8_id AND name = N'Park Hyatt Saigon')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner8_id,
            N'Park Hyatt Saigon',
            N'2 Lê Lợi, Quận 1, TP.HCM',
            N'TP.HCM',
            N'Quận 1',
            N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800&h=500&fit=crop&q=80',
            N'Park Hyatt Saigon là khách sạn 5 sao sang trọng nằm trên đường Lê Lợi, trái tim của Sài Gòn. Khách sạn sở hữu kiến trúc cổ điển, phòng ốc hiện đại và nhà hàng cao cấp.',
            4.7, 0, N'APPROVED', 1, DATEADD(DAY, -50, GETDATE())
        );
    END

    -- Hotel 9: Pilgrimage Village Boutique Resort (1 room) - REJECTED, Inactive
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner9_id AND name = N'Pilgrimage Village Boutique Resort')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner9_id,
            N'Pilgrimage Village Boutique Resort',
            N'130 Minh Mạng, Thủy Xuân, Huế',
            N'Huế',
            N'Thủy Xuân',
            N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&h=500&fit=crop&q=80',
            N'Pilgrimage Village Boutique Resort là khu nghỉ dưỡng 4 sao phong cách làng quê Việt Nam, nằm yên bình giữa thiên nhiên xanh mát, gần trung tâm Huế.',
            4.3, 0, N'REJECTED', 0, DATEADD(DAY, -30, GETDATE())
        );
    END

    -- Hotel 10: Victoria Can Tho Resort (1 room) - APPROVED, Active
    IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner10_id AND name = N'Victoria Can Tho Resort')
    BEGIN
        INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_reviews, approval_status, active, approved_at)
        VALUES (
            @owner10_id,
            N'Victoria Can Tho Resort',
            N'Cồn Cái Khế, Ninh Kiều, Cần Thơ',
            N'Cần Thơ',
            N'Ninh Kiều',
            N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&h=500&fit=crop&q=80',
            N'Victoria Can Tho Resort là resort 4 sao nằm trên một hòn đảo nhỏ giữa sông Hậu, với không gian xanh mát, hồ bơi lớn và view sông tuyệt đẹp.',
            4.4, 0, N'APPROVED', 1, DATEADD(DAY, -40, GETDATE())
        );
    END

    -- Get Hotel IDs
    SELECT @h1_id = id FROM hotel WHERE owner_id = @owner1_id AND name = N'InterContinental Danang Sun Peninsula Resort';
    SELECT @h2_id = id FROM hotel WHERE owner_id = @owner2_id AND name = N'Vinpearl Resort Nha Trang';
    SELECT @h3_id = id FROM hotel WHERE owner_id = @owner3_id AND name = N'Four Seasons Resort The Nam Hai';
    SELECT @h4_id = id FROM hotel WHERE owner_id = @owner4_id AND name = N'Dalat Palace Heritage Hotel';
    SELECT @h5_id = id FROM hotel WHERE owner_id = @owner5_id AND name = N'JW Marriott Phu Quoc Emerald Bay';
    SELECT @h6_id = id FROM hotel WHERE owner_id = @owner6_id AND name = N'Hotel de la Coupole - MGallery';
    SELECT @h7_id = id FROM hotel WHERE owner_id = @owner7_id AND name = N'Sofitel Legend Metropole Hanoi';
    SELECT @h8_id = id FROM hotel WHERE owner_id = @owner8_id AND name = N'Park Hyatt Saigon';
    SELECT @h9_id = id FROM hotel WHERE owner_id = @owner9_id AND name = N'Pilgrimage Village Boutique Resort';
    SELECT @h10_id = id FROM hotel WHERE owner_id = @owner10_id AND name = N'Victoria Can Tho Resort';

    -- =====================================================
    -- 3. HOTEL VERIFICATION DOCUMENTS
    -- =====================================================
    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h1_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h1_id, N'+842923880001', N'intercontinental.danang@hotel.com', N'/docs/hotels/intercontinental/business.pdf', N'/docs/hotels/intercontinental/land.pdf', N'/docs/hotels/intercontinental/contract.pdf', N'APPROVED', DATEADD(DAY, -90, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h2_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h2_id, N'+842923880002', N'vinpearl.nhatrang@hotel.com', N'/docs/hotels/vinpearl/business.pdf', N'/docs/hotels/vinpearl/land.pdf', N'/docs/hotels/vinpearl/contract.pdf', N'APPROVED', DATEADD(DAY, -85, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h3_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h3_id, N'+842923880003', N'fourseasons.hoian@hotel.com', N'/docs/hotels/fourseasons/business.pdf', N'/docs/hotels/fourseasons/land.pdf', N'/docs/hotels/fourseasons/contract.pdf', N'APPROVED', DATEADD(DAY, -80, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h4_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h4_id, N'+842923880004', N'dalatpalace.dalat@hotel.com', N'/docs/hotels/dalatpalace/business.pdf', N'/docs/hotels/dalatpalace/land.pdf', N'/docs/hotels/dalatpalace/contract.pdf', N'APPROVED', DATEADD(DAY, -75, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h5_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h5_id, N'+842923880005', N'jwmarriott.phuquoc@hotel.com', N'/docs/hotels/jwmarriott/business.pdf', N'/docs/hotels/jwmarriott/land.pdf', N'/docs/hotels/jwmarriott/contract.pdf', N'APPROVED', DATEADD(DAY, -70, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h6_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h6_id, N'+842923880006', N'coupole.sapa@hotel.com', N'/docs/hotels/coupole/business.pdf', N'/docs/hotels/coupole/land.pdf', N'/docs/hotels/coupole/contract.pdf', N'APPROVED', DATEADD(DAY, -65, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h7_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h7_id, N'+842923880007', N'metropole.hanoi@hotel.com', N'/docs/hotels/metropole/business.pdf', N'/docs/hotels/metropole/land.pdf', N'/docs/hotels/metropole/contract.pdf', N'PENDING', NULL);

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h8_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h8_id, N'+842923880008', N'parkhyatt.saigon@hotel.com', N'/docs/hotels/parkhyatt/business.pdf', N'/docs/hotels/parkhyatt/land.pdf', N'/docs/hotels/parkhyatt/contract.pdf', N'APPROVED', DATEADD(DAY, -50, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h9_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h9_id, N'+842923880009', N'pilgrimage.hue@hotel.com', N'/docs/hotels/pilgrimage/business.pdf', N'/docs/hotels/pilgrimage/land.pdf', N'/docs/hotels/pilgrimage/contract.pdf', N'REJECTED', DATEADD(DAY, -30, GETDATE()));

    IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h10_id)
        INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
        VALUES (@h10_id, N'+842923880010', N'victoria.cantho@hotel.com', N'/docs/hotels/victoria/business.pdf', N'/docs/hotels/victoria/land.pdf', N'/docs/hotels/victoria/contract.pdf', N'APPROVED', DATEADD(DAY, -40, GETDATE()));

    -- =====================================================
    -- 4. HOTEL FACILITIES & VIEWS
    -- =====================================================
    
    -- H1: InterContinental (5 sao - đầy đủ tiện nghi)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h1_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h1_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);

    -- H2: Vinpearl (5 sao - biển, hồ bơi)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h2_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h2_id, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1);

    -- H3: Four Seasons (5 sao - resort cao cấp)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h3_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h3_id, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1);

    -- H4: Dalat Palace (khách sạn cổ điển)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h4_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h4_id, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1);

    -- H5: JW Marriott (5 sao)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h5_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h5_id, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1);

    -- H6: Coupole (phong cách Pháp)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h6_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h6_id, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1);

    -- H7: Metropole (lịch sử, trung tâm)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h7_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h7_id, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1);

    -- H8: Park Hyatt (sang trọng)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h8_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h8_id, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1);

    -- H9: Pilgrimage (resort boutique)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h9_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h9_id, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1);

    -- H10: Victoria (sông nước)
    IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h10_id)
        INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
        VALUES (@h10_id, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1);

    -- Hotel Views
    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h1_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h1_id, 1, 1, 0, 1, 0, 1);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h2_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h2_id, 0, 1, 1, 1, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h3_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h3_id, 0, 1, 1, 1, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h4_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h4_id, 1, 0, 1, 0, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h5_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h5_id, 0, 1, 1, 1, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h6_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h6_id, 0, 0, 0, 0, 0, 1);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h7_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h7_id, 1, 0, 0, 0, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h8_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h8_id, 1, 0, 0, 0, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h9_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h9_id, 0, 0, 1, 1, 0, 0);

    IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h10_id)
        INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
        VALUES (@h10_id, 0, 0, 1, 1, 1, 0);

    -- =====================================================
    -- 5. ROOMS - DETAILED WITH UNIQUE IMAGES
    -- =====================================================

    -- HOTEL 1: InterContinental (5 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Resort King Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h1_id, N'Resort King Room', 15, N'Phòng Resort King rộng rãi với giường king, view biển tuyệt đẹp và ban công riêng.', 5500000, 42, 1, 2, 2, N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Resort Twin Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h1_id, N'Resort Twin Room', 20, N'Phòng Twin với 2 giường đơn, view hồ bơi và khu vườn nhiệt đới.', 5200000, 40, 2, 2, 2, N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Club Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h1_id, N'Club Suite', 8, N'Sang trọng với phòng khách riêng, quyền sử dụng Club Lounge và view toàn cảnh biển.', 9500000, 65, 1, 2, 3, N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'One Bedroom Villa')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h1_id, N'One Bedroom Villa', 5, N'Villa riêng với hồ bơi riêng, khu vườn và bếp nhỏ, không gian sống cực kỳ riêng tư.', 18500000, 120, 1, 2, 4, N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Two Bedroom Villa')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h1_id, N'Two Bedroom Villa', 3, N'Villa 2 phòng ngủ rộng lớn, hồ bơi riêng, tầm nhìn biển, thích hợp cho gia đình.', 28500000, 180, 2, 4, 5, N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 2: Vinpearl Nha Trang (5 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Deluxe Sea View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h2_id, N'Deluxe Sea View', 30, N'Phòng Deluxe view biển tuyệt đẹp, ban công rộng, tiện nghi hiện đại.', 3200000, 38, 1, 2, 2, N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Premium Ocean Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h2_id, N'Premium Ocean Suite', 10, N'Suite view đại dương với phòng khách, hồ bơi riêng, không gian sang trọng.', 7800000, 72, 1, 2, 3, N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Family Garden View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h2_id, N'Family Garden View', 12, N'Phòng gia đình view vườn, rộng rãi, thích hợp cho gia đình 4 người.', 4500000, 55, 2, 4, 2, N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Executive Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h2_id, N'Executive Suite', 8, N'Sang trọng với phòng khách và phòng ngủ riêng biệt, club lounge access.', 8900000, 80, 1, 2, 3, N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Vinpearl Villa')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h2_id, N'Vinpearl Villa', 6, N'Villa 3 phòng ngủ view biển, hồ bơi riêng, phòng khách và bếp cao cấp.', 15500000, 150, 3, 6, 4, N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 3: Four Seasons The Nam Hai (4 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'One-Bedroom Villa')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h3_id, N'One-Bedroom Villa', 20, N'Villa 1 phòng ngủ với hồ bơi riêng, khu vườn nhiệt đới và view biển.', 12500000, 110, 1, 2, 3, N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Two-Bedroom Villa')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h3_id, N'Two-Bedroom Villa', 12, N'Villa 2 phòng ngủ, hồ bơi riêng, khu vườn và tầm nhìn biển Hà My tuyệt đẹp.', 22500000, 170, 2, 4, 4, N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Three-Bedroom Villa')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h3_id, N'Three-Bedroom Villa', 5, N'Villa 3 phòng ngủ rộng lớn, hồ bơi riêng, khu vườn và tầm nhìn ra biển.', 35000000, 250, 3, 6, 5, N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Presidential Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h3_id, N'Presidential Suite', 2, N'Suite Tổng Thống với không gian cực kỳ sang trọng, dịch vụ butler riêng.', 65000000, 300, 1, 2, 6, N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 4: Dalat Palace (4 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Classic Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h4_id, N'Classic Room', 18, N'Phòng Classic với phong cách Pháp cổ, view hồ Xuân Hương và núi.', 3200000, 32, 1, 2, 2, N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Heritage Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h4_id, N'Heritage Suite', 6, N'Suite di sản với nội thất cổ điển, tầm nhìn ra hồ Xuân Hương.', 5800000, 55, 1, 2, 3, N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Royal Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h4_id, N'Royal Suite', 2, N'Suite Hoàng Gia sang trọng, không gian rộng lớn, view toàn cảnh thành phố.', 12500000, 90, 1, 2, 4, N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Garden Wing')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h4_id, N'Garden Wing', 10, N'Phòng Garden Wing nằm trong khu vườn xanh mát, view núi và hồ.', 4200000, 40, 1, 2, 2, N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 5: JW Marriott Phu Quoc (3 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h5_id AND room_type = N'Deluxe Garden View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h5_id, N'Deluxe Garden View', 25, N'Phòng Deluxe view vườn, thiết kế theo phong cách trường học Pháp.', 4500000, 42, 1, 2, 2, N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h5_id AND room_type = N'Deluxe Sea View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h5_id, N'Deluxe Sea View', 20, N'Phòng Deluxe view biển Kem tuyệt đẹp, ban công rộng.', 5500000, 45, 1, 2, 2, N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h5_id AND room_type = N'Emerald Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h5_id, N'Emerald Suite', 8, N'Emerald Suite là suite đặc trưng của JW Marriott với view biển và thiết kế độc đáo.', 12500000, 85, 1, 2, 3, N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 6: Coupole Sapa (2 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h6_id AND room_type = N'Superior Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h6_id, N'Superior Room', 20, N'Phòng Superior phong cách Pháp, view núi Hoàng Liên Sơn và thị trấn Sapa.', 2800000, 30, 1, 2, 2, N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h6_id AND room_type = N'Deluxe Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h6_id, N'Deluxe Suite', 8, N'Suite sang trọng với view núi, nội thất thiết kế thời trang.', 6800000, 60, 1, 2, 3, N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 7: Metropole Hanoi (2 rooms)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h7_id AND room_type = N'Grand Classic Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h7_id, N'Grand Classic Room', 15, N'Phòng Grand Classic mang phong cách Đông Dương cổ điển, nội thất gỗ quý.', 4800000, 38, 1, 2, 2, N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);

    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h7_id AND room_type = N'Metropole Suite')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h7_id, N'Metropole Suite', 6, N'Suite mang tên khách sạn, view phố Ngô Quyền, không gian sang trọng.', 12500000, 78, 1, 2, 3, N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 8: Park Hyatt Saigon (1 room)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h8_id AND room_type = N'Park King Room')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h8_id, N'Park King Room', 20, N'Phòng Park King sang trọng, view thành phố Sài Gòn, tiện nghi đẳng cấp.', 6500000, 42, 1, 2, 2, N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 9: Pilgrimage Village (1 room)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h9_id AND room_type = N'Bungalow Garden View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h9_id, N'Bungalow Garden View', 12, N'Bungalow phong cách làng quê Việt Nam, view vườn và hồ bơi.', 2200000, 35, 1, 2, 2, N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

    -- HOTEL 10: Victoria Can Tho (1 room)
    IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h10_id AND room_type = N'Superior River View')
        INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
        VALUES (@h10_id, N'Superior River View', 18, N'Phòng Superior view sông Hậu, ban công rộng và không gian thoáng mát.', 2300000, 32, 1, 2, 2, N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

    -- Get Room IDs (same as before - the actual IDs will be assigned by SQL Server)
    SELECT @h1_r1_id = id FROM room WHERE hotel_id = @h1_id AND room_type = N'Resort King Room';
    SELECT @h1_r2_id = id FROM room WHERE hotel_id = @h1_id AND room_type = N'Resort Twin Room';
    SELECT @h1_r3_id = id FROM room WHERE hotel_id = @h1_id AND room_type = N'Club Suite';
    SELECT @h1_r4_id = id FROM room WHERE hotel_id = @h1_id AND room_type = N'One Bedroom Villa';
    SELECT @h1_r5_id = id FROM room WHERE hotel_id = @h1_id AND room_type = N'Two Bedroom Villa';
    SELECT @h2_r1_id = id FROM room WHERE hotel_id = @h2_id AND room_type = N'Deluxe Sea View';
    SELECT @h2_r2_id = id FROM room WHERE hotel_id = @h2_id AND room_type = N'Premium Ocean Suite';
    SELECT @h2_r3_id = id FROM room WHERE hotel_id = @h2_id AND room_type = N'Family Garden View';
    SELECT @h2_r4_id = id FROM room WHERE hotel_id = @h2_id AND room_type = N'Executive Suite';
    SELECT @h2_r5_id = id FROM room WHERE hotel_id = @h2_id AND room_type = N'Vinpearl Villa';
    SELECT @h3_r1_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'One-Bedroom Villa';
    SELECT @h3_r2_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Two-Bedroom Villa';
    SELECT @h3_r3_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Three-Bedroom Villa';
    SELECT @h3_r4_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Presidential Suite';
    SELECT @h4_r1_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Classic Room';
    SELECT @h4_r2_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Heritage Suite';
    SELECT @h4_r3_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Royal Suite';
    SELECT @h4_r4_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Garden Wing';
    SELECT @h5_r1_id = id FROM room WHERE hotel_id = @h5_id AND room_type = N'Deluxe Garden View';
    SELECT @h5_r2_id = id FROM room WHERE hotel_id = @h5_id AND room_type = N'Deluxe Sea View';
    SELECT @h5_r3_id = id FROM room WHERE hotel_id = @h5_id AND room_type = N'Emerald Suite';
    SELECT @h6_r1_id = id FROM room WHERE hotel_id = @h6_id AND room_type = N'Superior Room';
    SELECT @h6_r2_id = id FROM room WHERE hotel_id = @h6_id AND room_type = N'Deluxe Suite';
    SELECT @h7_r1_id = id FROM room WHERE hotel_id = @h7_id AND room_type = N'Grand Classic Room';
    SELECT @h7_r2_id = id FROM room WHERE hotel_id = @h7_id AND room_type = N'Metropole Suite';
    SELECT @h8_r1_id = id FROM room WHERE hotel_id = @h8_id AND room_type = N'Park King Room';
    SELECT @h9_r1_id = id FROM room WHERE hotel_id = @h9_id AND room_type = N'Bungalow Garden View';
    SELECT @h10_r1_id = id FROM room WHERE hotel_id = @h10_id AND room_type = N'Superior River View';

  -- =====================================================
-- 6. ROOM FACILITIES (ALL 28 ROOMS - FULL CODE)
-- =====================================================

-- ===== HOTEL 1: InterContinental (5 rooms) =====

-- H1-R1: Resort King Room
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h1_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h1_r1_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4
    );

-- H1-R2: Resort Twin Room
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h1_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h1_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4
    );

-- H1-R3: Club Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h1_r3_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h1_r3_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6
    );

-- H1-R4: One Bedroom Villa
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h1_r4_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h1_r4_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8
    );

-- H1-R5: Two Bedroom Villa
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h1_r5_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h1_r5_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 10
    );

-- ===== HOTEL 2: Vinpearl Nha Trang (5 rooms) =====

-- H2-R1: Deluxe Sea View
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h2_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h2_r1_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4
    );

-- H2-R2: Premium Ocean Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h2_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h2_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6
    );

-- H2-R3: Family Garden View
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h2_r3_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h2_r3_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4
    );

-- H2-R4: Executive Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h2_r4_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h2_r4_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6
    );

-- H2-R5: Vinpearl Villa
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h2_r5_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h2_r5_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 10
    );

-- ===== HOTEL 3: Four Seasons The Nam Hai (4 rooms) =====

-- H3-R1: One-Bedroom Villa
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h3_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h3_r1_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8
    );

-- H3-R2: Two-Bedroom Villa
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h3_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h3_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 10
    );

-- H3-R3: Three-Bedroom Villa
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h3_r3_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h3_r3_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 12
    );

-- H3-R4: Presidential Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h3_r4_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h3_r4_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 15
    );

-- ===== HOTEL 4: Dalat Palace (4 rooms) =====

-- H4-R1: Classic Room
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h4_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h4_r1_id, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2
    );

-- H4-R2: Heritage Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h4_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h4_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 4
    );

-- H4-R3: Royal Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h4_r3_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h4_r3_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6
    );

-- H4-R4: Garden Wing
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h4_r4_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h4_r4_id, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2
    );

-- ===== HOTEL 5: JW Marriott Phu Quoc (3 rooms) =====

-- H5-R1: Deluxe Garden View
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h5_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h5_r1_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 4
    );

-- H5-R2: Deluxe Sea View
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h5_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h5_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4
    );

-- H5-R3: Emerald Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h5_r3_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h5_r3_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6
    );

-- ===== HOTEL 6: Coupole Sapa (2 rooms) =====

-- H6-R1: Superior Room
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h6_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h6_r1_id, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2
    );

-- H6-R2: Deluxe Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h6_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h6_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 4
    );

-- ===== HOTEL 7: Metropole Hanoi (2 rooms) =====

-- H7-R1: Grand Classic Room
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h7_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h7_r1_id, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2
    );

-- H7-R2: Metropole Suite
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h7_r2_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h7_r2_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 6
    );

-- ===== HOTEL 8: Park Hyatt Saigon (1 room) =====

-- H8-R1: Park King Room
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h8_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h8_r1_id, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4
    );

-- ===== HOTEL 9: Pilgrimage Village (1 room) =====

-- H9-R1: Bungalow Garden View
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h9_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h9_r1_id, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2
    );

-- ===== HOTEL 10: Victoria Can Tho (1 room) =====

-- H10-R1: Superior River View
IF NOT EXISTS (SELECT 1 FROM room_facilities WHERE room_id = @h10_r1_id)
    INSERT INTO room_facilities (
        room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, 
        hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, 
        television, telephone, iron, electric_kettle, cable_channels, 
        wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water
    )
    VALUES (
        @h10_r1_id, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 2
    );

  -- =====================================================
-- 7. PROMOTIONS (4 per hotel = 40 promotions)
-- Each hotel has 4 promotions with 4 different statuses:
-- ACTIVE, INACTIVE, EXPIRED, and one more ACTIVE/UPCOMING
-- =====================================================

-- H1: InterContinental Danang (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h1_id AND title = N'Early Bird Special')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h1_id, N'Early Bird Special', N'Đặt phòng trước 30 ngày nhận giảm giá 15% trên tổng hóa đơn.', 15.00, DATEADD(DAY, -30, GETDATE()), DATEADD(DAY, 60, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h1_id AND title = N'Weekend Getaway')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h1_id, N'Weekend Getaway', N'Ưu đãi cuối tuần - giảm 10% cho đặt phòng từ thứ 6 đến Chủ nhật.', 10.00, DATEADD(DAY, -15, GETDATE()), DATEADD(DAY, 15, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h1_id AND title = N'Summer Escape')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h1_id, N'Summer Escape', N'Khuyến mãi mùa hè - giảm 20% cho đặt phòng từ 3 đêm trở lên.', 20.00, DATEADD(DAY, -60, GETDATE()), DATEADD(DAY, -5, GETDATE()), N'EXPIRED');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h1_id AND title = N'Loyalty Member')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h1_id, N'Loyalty Member', N'Thành viên trung thành được giảm 12% trên mọi đặt phòng.', 12.00, DATEADD(DAY, 10, GETDATE()), DATEADD(DAY, 90, GETDATE()), N'INACTIVE');

-- H2: Vinpearl Nha Trang (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h2_id AND title = N'Family Holiday')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h2_id, N'Family Holiday', N'Gói gia đình - giảm 18% cho phòng Family và Villa.', 18.00, DATEADD(DAY, -20, GETDATE()), DATEADD(DAY, 40, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h2_id AND title = N'Honeymoon Package')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h2_id, N'Honeymoon Package', N'Gói tuần trăng mật - giảm 15% + bữa tối lãng mạn.', 15.00, DATEADD(DAY, 5, GETDATE()), DATEADD(DAY, 95, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h2_id AND title = N'Last Minute Deal')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h2_id, N'Last Minute Deal', N'Đặt phòng trong vòng 3 ngày nhận giảm 25%', 25.00, DATEADD(DAY, -90, GETDATE()), DATEADD(DAY, -10, GETDATE()), N'EXPIRED');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h2_id AND title = N'Group Booking')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h2_id, N'Group Booking', N'Đặt từ 5 phòng trở lên giảm 20% và tặng 1 phòng miễn phí.', 20.00, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, 25, GETDATE()), N'INACTIVE');

-- H3: Four Seasons The Nam Hai (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h3_id AND title = N'Luxury Retreat')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h3_id, N'Luxury Retreat', N'Nghỉ dưỡng sang trọng - giảm 15% cho Villas 2 đêm trở lên.', 15.00, DATEADD(DAY, -25, GETDATE()), DATEADD(DAY, 35, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h3_id AND title = N'Wellness Escape')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h3_id, N'Wellness Escape', N'Gói sức khỏe - giảm 10% + 1 buổi yoga và spa miễn phí.', 10.00, DATEADD(DAY, 15, GETDATE()), DATEADD(DAY, 105, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h3_id AND title = N'Long Stay')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h3_id, N'Long Stay', N'Ở 5 đêm tặng 1 đêm miễn phí + giảm 15%', 15.00, DATEADD(DAY, -120, GETDATE()), DATEADD(DAY, -20, GETDATE()), N'EXPIRED');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h3_id AND title = N'Gourmet Package')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h3_id, N'Gourmet Package', N'Gói ẩm thực - giảm 12% + bữa tối 3 món tại nhà hàng.', 12.00, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, 28, GETDATE()), N'INACTIVE');

-- H4: Dalat Palace (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h4_id AND title = N'Romantic Getaway')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h4_id, N'Romantic Getaway', N'Gói lãng mạn - giảm 15% + sâm banh và hoa trong phòng.', 15.00, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, 50, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h4_id AND title = N'Heritage Experience')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h4_id, N'Heritage Experience', N'Khám phá di sản Đà Lạt - giảm 12% + tour thành phố miễn phí.', 12.00, DATEADD(DAY, 20, GETDATE()), DATEADD(DAY, 110, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h4_id AND title = N'Flower Festival')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h4_id, N'Flower Festival', N'Đặc biệt mùa hoa Đà Lạt - giảm 20% từ tháng 12 đến tháng 2.', 20.00, DATEADD(DAY, -150, GETDATE()), DATEADD(DAY, -30, GETDATE()), N'EXPIRED');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h4_id AND title = N'Stay & Dine')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h4_id, N'Stay & Dine', N'Ăn uống và nghỉ dưỡng - giảm 10% + bữa tối miễn phí.', 10.00, DATEADD(DAY, -7, GETDATE()), DATEADD(DAY, 23, GETDATE()), N'INACTIVE');

-- H5: JW Marriott Phu Quoc (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h5_id AND title = N'Emerald Escape')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h5_id, N'Emerald Escape', N'Ưu đãi Emerald - giảm 15% cho tất cả phòng, đặt 2 đêm trở lên.', 15.00, DATEADD(DAY, -40, GETDATE()), DATEADD(DAY, 20, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h5_id AND title = N'Beach Lovers')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h5_id, N'Beach Lovers', N'Gói tình yêu biển - giảm 20% và tặng 1 buổi snorkeling.', 20.00, DATEADD(DAY, 30, GETDATE()), DATEADD(DAY, 120, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h5_id AND title = N'Corporate Rate')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h5_id, N'Corporate Rate', N'Giá doanh nghiệp - giảm 18% cho booking công tác.', 18.00, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, 25, GETDATE()), N'INACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h5_id AND title = N'New Year Special')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h5_id, N'New Year Special', N'Đón năm mới - giảm 25% cho đặt phòng 3 đêm dịp Tết.', 25.00, DATEADD(DAY, -200, GETDATE()), DATEADD(DAY, -40, GETDATE()), N'EXPIRED');

-- H6: Coupole Sapa (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h6_id AND title = N'Mountain View')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h6_id, N'Mountain View', N'Phòng view núi - giảm 12% cho Deluxe Suite.', 12.00, DATEADD(DAY, -15, GETDATE()), DATEADD(DAY, 45, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h6_id AND title = N'Winter Escape')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h6_id, N'Winter Escape', N'Trốn đông - giảm 18% cho tháng 12, 1, 2.', 18.00, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, 50, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h6_id AND title = N'Trekking Package')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h6_id, N'Trekking Package', N'Gói trekking - giảm 10% và tour leo Fansipan.', 10.00, DATEADD(DAY, 5, GETDATE()), DATEADD(DAY, 35, GETDATE()), N'INACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h6_id AND title = N'Honeymoon Sapa')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h6_id, N'Honeymoon Sapa', N'Tuần trăng mật Sapa - giảm 15% + bữa tối trên mái.', 15.00, DATEADD(DAY, -100, GETDATE()), DATEADD(DAY, -20, GETDATE()), N'EXPIRED');

-- H7: Metropole Hanoi (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h7_id AND title = N'Heritage Stay')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h7_id, N'Heritage Stay', N'Giá di sản - giảm 15% cho phòng Grand Classic và Suite.', 15.00, DATEADD(DAY, -30, GETDATE()), DATEADD(DAY, 30, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h7_id AND title = N'Weekend in Hanoi')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h7_id, N'Weekend in Hanoi', N'Cuối tuần ở Hà Nội - giảm 12% từ thứ 6 đến Chủ nhật.', 12.00, DATEADD(DAY, 10, GETDATE()), DATEADD(DAY, 100, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h7_id AND title = N'Business Travel')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h7_id, N'Business Travel', N'Giá doanh nhân - giảm 20% cho booking công tác 5 đêm trở lên.', 20.00, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, 27, GETDATE()), N'INACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h7_id AND title = N'Christmas Special')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h7_id, N'Christmas Special', N'Giáng Sinh ấm áp - giảm 15% và tặng bữa tối Giáng Sinh.', 15.00, DATEADD(DAY, -150, GETDATE()), DATEADD(DAY, -10, GETDATE()), N'EXPIRED');

-- H8: Park Hyatt Saigon (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h8_id AND title = N'City Break')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h8_id, N'City Break', N'Nghỉ dưỡng giữa lòng Sài Gòn - giảm 15% cho đặt 2 đêm trở lên.', 15.00, DATEADD(DAY, -20, GETDATE()), DATEADD(DAY, 40, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h8_id AND title = N'Executive Deal')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h8_id, N'Executive Deal', N'Giá điều hành - giảm 20% và club lounge access miễn phí.', 20.00, DATEADD(DAY, 5, GETDATE()), DATEADD(DAY, 65, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h8_id AND title = N'Anniversary Special')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h8_id, N'Anniversary Special', N'Kỷ niệm - giảm 25% cho đặt phòng trong tháng 3.', 25.00, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, 28, GETDATE()), N'INACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h8_id AND title = N'Family Fun')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h8_id, N'Family Fun', N'Gia đình vui vẻ - trẻ em dưới 12 tuổi ở miễn phí + giảm 10%.', 10.00, DATEADD(DAY, -120, GETDATE()), DATEADD(DAY, -30, GETDATE()), N'EXPIRED');

-- H9: Pilgrimage Village (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h9_id AND title = N'Countryside Escape')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h9_id, N'Countryside Escape', N'Thoát khỏi phố thị - giảm 15% cho bungalow, 2 đêm trở lên.', 15.00, DATEADD(DAY, -25, GETDATE()), DATEADD(DAY, 35, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h9_id AND title = N'Cultural Package')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h9_id, N'Cultural Package', N'Gói văn hóa Huế - giảm 12% + tour kinh thành miễn phí.', 12.00, DATEADD(DAY, 15, GETDATE()), DATEADD(DAY, 105, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h9_id AND title = N'Wellness Retreat')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h9_id, N'Wellness Retreat', N'Thư giãn và chăm sóc sức khỏe - giảm 18% + spa trị liệu.', 18.00, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 29, GETDATE()), N'INACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h9_id AND title = N'Festival Season')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h9_id, N'Festival Season', N'Mùa lễ hội Huế - giảm 20% trong tháng 4 và 5.', 20.00, DATEADD(DAY, -180, GETDATE()), DATEADD(DAY, -60, GETDATE()), N'EXPIRED');

-- H10: Victoria Can Tho (4 promotions - 4 statuses)
IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h10_id AND title = N'River Retreat')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h10_id, N'River Retreat', N'Nghỉ dưỡng sông nước - giảm 12% cho phòng view sông.', 12.00, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, 50, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h10_id AND title = N'Floating Market Tour')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h10_id, N'Floating Market Tour', N'Gói chợ nổi Cái Răng - giảm 10% + tour chợ nổi.', 10.00, DATEADD(DAY, 25, GETDATE()), DATEADD(DAY, 115, GETDATE()), N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h10_id AND title = N'Delta Discovery')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h10_id, N'Delta Discovery', N'Khám phá miền Tây - giảm 15% đặt 3 đêm trở lên.', 15.00, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, 25, GETDATE()), N'INACTIVE');

IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h10_id AND title = N'Weekend in Can Tho')
    INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
    VALUES (@h10_id, N'Weekend in Can Tho', N'Cuối tuần ở Cần Thơ - giảm 8% từ thứ 6 đến Chủ nhật.', 8.00, DATEADD(DAY, -90, GETDATE()), DATEADD(DAY, -15, GETDATE()), N'EXPIRED');

    -- =====================================================
    -- 8-14. BOOKINGS, PAYMENTS, REFUNDS, FEEDBACK, ETC.
    -- =====================================================
    -- [All remaining code same as previous version]

    COMMIT TRANSACTION;

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