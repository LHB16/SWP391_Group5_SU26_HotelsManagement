    -- =====================================================
    -- HOTEL BOOKING SYSTEM - REAL DATA SCRIPT (FIXED)
    -- File: 02_insert_hotel_db_sample_data_safe.sql
    -- Target: Microsoft SQL Server / SSMS
    -- Description: Insert REAL data for 10 famous hotels in Vietnam
    --              ALL PASSWORDS = "12345678"
    --              + 1 NEW OWNER with 7 LUXURY HOTELS (FULL ROOM DETAILS)
    -- Safe to run multiple times (checks existing data before insert)
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
            @owner1_user_id INT, @owner2_user_id INT, @owner3_user_id INT,
            @owner4_user_id INT, @owner5_user_id INT, @owner6_user_id INT,
            @owner7_user_id INT, @owner8_user_id INT, @owner9_user_id INT,
            @owner10_user_id INT,
            @owner1_id INT, @owner2_id INT, @owner3_id INT, @owner4_id INT,
            @owner5_id INT, @owner6_id INT, @owner7_id INT, @owner8_id INT,
            @owner9_id INT, @owner10_id INT,
            @c1_user_id INT, @c2_user_id INT, @c3_user_id INT, @c4_user_id INT,
            @c5_user_id INT, @c6_user_id INT, @c7_user_id INT, @c8_user_id INT,
            @c9_user_id INT, @c10_user_id INT,
            @c1_id INT, @c2_id INT, @c3_id INT, @c4_id INT, @c5_id INT,
            @c6_id INT, @c7_id INT, @c8_id INT, @c9_id INT, @c10_id INT,
            @h1_id INT, @h2_id INT, @h3_id INT, @h4_id INT, @h5_id INT,
            @h6_id INT, @h7_id INT, @h8_id INT, @h9_id INT, @h10_id INT,
            @h1_r1_id INT, @h1_r2_id INT, @h1_r3_id INT, @h1_r4_id INT, @h1_r5_id INT,
            @h2_r1_id INT, @h2_r2_id INT, @h2_r3_id INT, @h2_r4_id INT, @h2_r5_id INT,
            @h3_r1_id INT, @h3_r2_id INT, @h3_r3_id INT, @h3_r4_id INT,
            @h4_r1_id INT, @h4_r2_id INT, @h4_r3_id INT, @h4_r4_id INT,
            @h5_r1_id INT, @h5_r2_id INT, @h5_r3_id INT,
            @h6_r1_id INT, @h6_r2_id INT,
            @h7_r1_id INT, @h7_r2_id INT,
            @h8_r1_id INT, @h9_r1_id INT, @h10_r1_id INT,
            @b1_id INT, @b2_id INT, @b3_id INT, @b4_id INT, @b5_id INT,
            @b6_id INT, @b7_id INT, @b8_id INT, @b9_id INT, @b10_id INT,
            @b11_id INT, @b12_id INT,
            @fb1_id INT, @fb2_id INT, @fb3_id INT, @fb4_id INT, @fb5_id INT, @fb6_id INT,
            @promo1_id INT, @promo2_id INT, @promo3_id INT,
            @new_owner_user_id INT, @new_owner_id INT,
            @h_lux1_id INT, @h_lux2_id INT, @h_lux3_id INT, @h_lux4_id INT,
            @h_lux5_id INT, @h_lux6_id INT, @h_lux7_id INT;

        -- =====================================================
        -- PASSWORD HASH FOR "12345678"
        -- =====================================================
        DECLARE @passwordHash NVARCHAR(255) = N'$2a$10$rV9Y8n7LOZcT/qQ.0R7/HuO0yQgA/Bb8UO6VJfAxT2XK/LBzM0eE2';

        -- =====================================================
        -- 1. USERS & AUTH (ALL PASSWORDS = "12345678")
        -- =====================================================

        -- Admins
        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'admin1')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'admin1@hotelbooking.test', N'admin1', @passwordHash, N'ADMIN', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'admin2')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'admin2@hotelbooking.test', N'admin2', @passwordHash, N'ADMIN', 1);

        -- Hotel Owners (10)
        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_intercontinental')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.intercontinental@hotel.test', N'owner_intercontinental', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_vinpearl')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.vinpearl@hotel.test', N'owner_vinpearl', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_fourseasons')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.fourseasons@hotel.test', N'owner_fourseasons', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_dalatpalace')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.dalatpalace@hotel.test', N'owner_dalatpalace', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_jwmarriott')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.jwmarriott@hotel.test', N'owner_jwmarriott', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_coupole')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.coupole@hotel.test', N'owner_coupole', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_metropole')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.metropole@hotel.test', N'owner_metropole', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_parkhyatt')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.parkhyatt@hotel.test', N'owner_parkhyatt', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_pilgrimage')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.pilgrimage@hotel.test', N'owner_pilgrimage', @passwordHash, N'HOTEL_OWNER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_victoria')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.victoria@hotel.test', N'owner_victoria', @passwordHash, N'HOTEL_OWNER', 1);

        -- Customers (10)
        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_hoang')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'hoang.nguyen@customer.test', N'customer_hoang', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_linh')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'linh.tran@customer.test', N'customer_linh', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_duc')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'duc.pham@customer.test', N'customer_duc', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_mai')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'mai.le@customer.test', N'customer_mai', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_quang')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'quang.nguyen@customer.test', N'customer_quang', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_ha')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'ha.vo@customer.test', N'customer_ha', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_kien')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'kien.hoang@customer.test', N'customer_kien', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_anh')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'thanh.anh@customer.test', N'customer_anh', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_binh')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'binh.le@customer.test', N'customer_binh', @passwordHash, N'CUSTOMER', 1);

        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'customer_loan')
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'loan.huynh@customer.test', N'customer_loan', @passwordHash, N'CUSTOMER', 1);

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
        SELECT @c1_user_id  = id FROM user_accounts WHERE username = N'customer_hoang';
        SELECT @c2_user_id  = id FROM user_accounts WHERE username = N'customer_linh';
        SELECT @c3_user_id  = id FROM user_accounts WHERE username = N'customer_duc';
        SELECT @c4_user_id  = id FROM user_accounts WHERE username = N'customer_mai';
        SELECT @c5_user_id  = id FROM user_accounts WHERE username = N'customer_quang';
        SELECT @c6_user_id  = id FROM user_accounts WHERE username = N'customer_ha';
        SELECT @c7_user_id  = id FROM user_accounts WHERE username = N'customer_kien';
        SELECT @c8_user_id  = id FROM user_accounts WHERE username = N'customer_anh';
        SELECT @c9_user_id  = id FROM user_accounts WHERE username = N'customer_binh';
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
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner1_user_id, N'Lê Minh Hoàng', N'+84912000001', N'123 Nguyễn Văn Linh, Đà Nẵng', N'012345678901', N'TAX-IC-001', N'APPROVED', DATEADD(DAY, -90, GETDATE()), N'Vietcombank', N'1000100010', N'LE MINH HOANG');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner2_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner2_user_id, N'Phạm Thị Hoa', N'+84912000002', N'456 Trần Phú, Nha Trang', N'012345678902', N'TAX-VP-002', N'APPROVED', DATEADD(DAY, -85, GETDATE()), N'Techcombank', N'2000200020', N'PHAM THI HOA');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner3_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner3_user_id, N'Nguyễn Đức Anh', N'+84912000003', N'789 Lý Thường Kiệt, Hội An', N'012345678903', N'TAX-FS-003', N'APPROVED', DATEADD(DAY, -80, GETDATE()), N'MB Bank', N'3000300030', N'NGUYEN DUC ANH');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner4_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner4_user_id, N'Trần Minh Tâm', N'+84912000004', N'12 Trần Phú, Đà Lạt', N'012345678904', N'TAX-DP-004', N'APPROVED', DATEADD(DAY, -75, GETDATE()), N'BIDV', N'4000400040', N'TRAN MINH TAM');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner5_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner5_user_id, N'Lê Thị Ngọc', N'+84912000005', N'Bãi Khem, An Thới, Phú Quốc', N'012345678905', N'TAX-JW-005', N'APPROVED', DATEADD(DAY, -70, GETDATE()), N'Agribank', N'5000500050', N'LE THI NGOC');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner6_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner6_user_id, N'Hoàng Văn Khôi', N'+84912000006', N'1 Hoàng Liên, Sapa', N'012345678906', N'TAX-CP-006', N'APPROVED', DATEADD(DAY, -65, GETDATE()), N'VPBank', N'6000600060', N'HOANG VAN KHOI');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner7_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner7_user_id, N'Phan Thị Lan', N'+84912000007', N'15 Ngô Quyền, Hoàn Kiếm, Hà Nội', N'012345678907', N'TAX-MT-007', N'PENDING', NULL, N'ACB', N'7000700070', N'PHAN THI LAN');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner8_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner8_user_id, N'Đặng Quang Huy', N'+84912000008', N'2 Lê Lợi, Quận 1, TP.HCM', N'012345678908', N'TAX-PH-008', N'APPROVED', DATEADD(DAY, -50, GETDATE()), N'Sacombank', N'8000800080', N'DANG QUANG HUY');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner9_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner9_user_id, N'Võ Thị Thu', N'+84912000009', N'130 Minh Mạng, Huế', N'012345678909', N'TAX-PG-009', N'REJECTED', DATEADD(DAY, -30, GETDATE()), N'TPBank', N'9000900090', N'VO THI THU');

        IF NOT EXISTS (SELECT 1 FROM hotel_owners WHERE user_account_id = @owner10_user_id)
            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@owner10_user_id, N'Nguyễn Hữu Phước', N'+84912000010', N'Cồn Cái Khế, Ninh Kiều, Cần Thơ', N'012345678910', N'TAX-VC-010', N'APPROVED', DATEADD(DAY, -40, GETDATE()), N'Vietinbank', N'1001001001', N'NGUYEN HUU PHUOC');

        -- Get Owner IDs
        SELECT @owner1_id  = id FROM hotel_owners WHERE user_account_id = @owner1_user_id;
        SELECT @owner2_id  = id FROM hotel_owners WHERE user_account_id = @owner2_user_id;
        SELECT @owner3_id  = id FROM hotel_owners WHERE user_account_id = @owner3_user_id;
        SELECT @owner4_id  = id FROM hotel_owners WHERE user_account_id = @owner4_user_id;
        SELECT @owner5_id  = id FROM hotel_owners WHERE user_account_id = @owner5_user_id;
        SELECT @owner6_id  = id FROM hotel_owners WHERE user_account_id = @owner6_user_id;
        SELECT @owner7_id  = id FROM hotel_owners WHERE user_account_id = @owner7_user_id;
        SELECT @owner8_id  = id FROM hotel_owners WHERE user_account_id = @owner8_user_id;
        SELECT @owner9_id  = id FROM hotel_owners WHERE user_account_id = @owner9_user_id;
        SELECT @owner10_id = id FROM hotel_owners WHERE user_account_id = @owner10_user_id;

        -- Insert Customers
        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c1_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c1_user_id, N'Nguyễn Văn Hoàng', N'+84933000001', N'123 Lê Lợi, Quận 1', N'TP.HCM', N'Việt Nam', '1995-03-15');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c2_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c2_user_id, N'Trần Thị Linh', N'+84933000002', N'456 Nguyễn Huệ, Quận 1', N'TP.HCM', N'Việt Nam', '1998-07-22');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c3_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c3_user_id, N'Phạm Đức', N'+84933000003', N'789 Trần Hưng Đạo, Quận 5', N'TP.HCM', N'Việt Nam', '2000-11-09');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c4_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c4_user_id, N'Lê Thị Mai', N'+84933000004', N'12 Nguyễn Thị Minh Khai, Quận 3', N'TP.HCM', N'Việt Nam', '1997-01-28');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c5_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c5_user_id, N'Nguyễn Quang', N'+84933000005', N'34 Hai Bà Trưng, Quận 1', N'TP.HCM', N'Việt Nam', '1994-09-14');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c6_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c6_user_id, N'Võ Thị Hà', N'+84933000006', N'56 Lý Tự Trọng, Quận 1', N'TP.HCM', N'Việt Nam', '1996-05-30');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c7_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c7_user_id, N'Hoàng Kiên', N'+84933000007', N'78 Điện Biên Phủ, Quận Bình Thạnh', N'TP.HCM', N'Việt Nam', '1999-08-17');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c8_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c8_user_id, N'Trần Thị Thanh Anh', N'+84933000008', N'90 Võ Văn Tần, Quận 3', N'TP.HCM', N'Việt Nam', '2001-12-02');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c9_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c9_user_id, N'Lê Bình', N'+84933000009', N'11 Nguyễn Đình Chiểu, Quận 3', N'TP.HCM', N'Việt Nam', '1993-06-20');

        IF NOT EXISTS (SELECT 1 FROM customers WHERE user_account_id = @c10_user_id)
            INSERT INTO customers (user_account_id, full_name, phone, address, city, country, date_of_birth)
            VALUES (@c10_user_id, N'Huỳnh Thị Loan', N'+84933000010', N'22 Cách Mạng Tháng 8, Quận 10', N'TP.HCM', N'Việt Nam', '2002-03-11');

        -- Get Customer IDs
        SELECT @c1_id  = id FROM customers WHERE user_account_id = @c1_user_id;
        SELECT @c2_id  = id FROM customers WHERE user_account_id = @c2_user_id;
        SELECT @c3_id  = id FROM customers WHERE user_account_id = @c3_user_id;
        SELECT @c4_id  = id FROM customers WHERE user_account_id = @c4_user_id;
        SELECT @c5_id  = id FROM customers WHERE user_account_id = @c5_user_id;
        SELECT @c6_id  = id FROM customers WHERE user_account_id = @c6_user_id;
        SELECT @c7_id  = id FROM customers WHERE user_account_id = @c7_user_id;
        SELECT @c8_id  = id FROM customers WHERE user_account_id = @c8_user_id;
        SELECT @c9_id  = id FROM customers WHERE user_account_id = @c9_user_id;
        SELECT @c10_id = id FROM customers WHERE user_account_id = @c10_user_id;

        -- =====================================================
        -- 2. HOTELS (10 REAL HOTELS)
        -- =====================================================

        -- H1: InterContinental Danang – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner1_id AND name = N'InterContinental Danang Sun Peninsula Resort')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner1_id, N'InterContinental Danang Sun Peninsula Resort', N'Bãi Bắc, Sơn Trà, Đà Nẵng', N'Đà Nẵng', N'Sơn Trà',
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=500&fit=crop&q=80',
                N'Khu nghỉ dưỡng 5 sao đẳng cấp quốc tế tọa lạc trên bán đảo Sơn Trà với tầm nhìn tuyệt đẹp ra biển Đông. Resort sở hữu kiến trúc độc đáo, hồ bơi vô cực, nhà hàng Michelin và dịch vụ đẳng cấp.',
                4.9, 0, N'APPROVED', 1, DATEADD(DAY, -90, GETDATE()));

        -- H2: Vinpearl Nha Trang – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner2_id AND name = N'Vinpearl Resort Nha Trang')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner2_id, N'Vinpearl Resort Nha Trang', N'Đảo Hòn Tre, Nha Trang, Khánh Hòa', N'Nha Trang', N'Vĩnh Nguyên',
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=500&fit=crop&q=80',
                N'Khu nghỉ dưỡng 5 sao trên đảo Hòn Tre, kết nối với đất liền bằng cáp treo vượt biển. Resort có bãi biển riêng, công viên nước và nhiều tiện ích cao cấp.',
                4.7, 0, N'APPROVED', 1, DATEADD(DAY, -85, GETDATE()));

        -- H3: Four Seasons Hoi An – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner3_id AND name = N'Four Seasons Resort The Nam Hai')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner3_id, N'Four Seasons Resort The Nam Hai', N'Điện Dương, Điện Bàn, Quảng Nam', N'Hội An', N'Điện Bàn',
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&h=500&fit=crop&q=80',
                N'Resort 5 sao sang trọng bậc nhất Việt Nam trên bãi biển Hà My thơ mộng. Kiến trúc kết hợp hài hòa phong cách Việt Nam hiện đại và truyền thống.',
                4.8, 0, N'APPROVED', 1, DATEADD(DAY, -80, GETDATE()));

        -- H4: Dalat Palace – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner4_id AND name = N'Dalat Palace Heritage Hotel')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner4_id, N'Dalat Palace Heritage Hotel', N'12 Trần Phú, Đà Lạt, Lâm Đồng', N'Đà Lạt', N'Phường 3',
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=500&fit=crop&q=80',
                N'Khách sạn 5 sao cổ kính bên hồ Xuân Hương thơ mộng. Xây dựng từ thời Pháp thuộc, mang vẻ đẹp cổ điển với không gian sang trọng và lãng mạn giữa xứ sương mù.',
                4.5, 0, N'APPROVED', 1, DATEADD(DAY, -75, GETDATE()));

        -- H5: JW Marriott Phu Quoc – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner5_id AND name = N'JW Marriott Phu Quoc Emerald Bay')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner5_id, N'JW Marriott Phu Quoc Emerald Bay', N'Bãi Khem, An Thới, Phú Quốc, Kiên Giang', N'Phú Quốc', N'An Thới',
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=800&h=500&fit=crop&q=80',
                N'Resort 5 sao thiết kế độc đáo của kiến trúc sư Bill Bensley. Nằm trên bãi biển Kem tuyệt đẹp, mang phong cách trường đại học lịch sử, đẳng cấp và sang trọng.',
                4.9, 0, N'APPROVED', 1, DATEADD(DAY, -70, GETDATE()));

        -- H6: Hotel de la Coupole Sapa – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner6_id AND name = N'Hotel de la Coupole - MGallery')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner6_id, N'Hotel de la Coupole - MGallery', N'1 Hoàng Liên, Sapa, Lào Cai', N'Sapa', N'TT Sa Pa',
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800&h=500&fit=crop&q=80',
                N'Khách sạn 5 sao phong cách Pháp nổi bật với kiến trúc thời trang và màu sắc rực rỡ. Nằm giữa thị trấn Sapa, tầm nhìn hướng tới dãy núi Hoàng Liên Sơn hùng vĩ.',
                4.6, 0, N'APPROVED', 1, DATEADD(DAY, -65, GETDATE()));

        -- H7: Sofitel Metropole Hanoi – PENDING
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner7_id AND name = N'Sofitel Legend Metropole Hanoi')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner7_id, N'Sofitel Legend Metropole Hanoi', N'15 Ngô Quyền, Hoàn Kiếm, Hà Nội', N'Hà Nội', N'Hoàn Kiếm',
                N'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800&h=500&fit=crop&q=80',
                N'Khách sạn 5 sao lịch sử hơn 120 năm tuổi, nằm ngay trung tâm Hà Nội. Đã đón tiếp nhiều nguyên thủ quốc gia và người nổi tiếng thế giới.',
                4.8, 0, N'PENDING', 1, NULL);

        -- H8: Park Hyatt Saigon – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner8_id AND name = N'Park Hyatt Saigon')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner8_id, N'Park Hyatt Saigon', N'2 Lê Lợi, Quận 1, TP.HCM', N'TP.HCM', N'Quận 1',
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&h=500&fit=crop&q=80',
                N'Khách sạn 5 sao sang trọng trên đường Lê Lợi, trái tim Sài Gòn. Kiến trúc cổ điển Pháp, phòng ốc hiện đại, nhà hàng cao cấp và hồ bơi tầng thượng.',
                4.7, 0, N'APPROVED', 1, DATEADD(DAY, -50, GETDATE()));

        -- H9: Pilgrimage Village Hue – REJECTED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner9_id AND name = N'Pilgrimage Village Boutique Resort')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner9_id, N'Pilgrimage Village Boutique Resort', N'130 Minh Mạng, Thủy Xuân, Huế', N'Huế', N'Thủy Xuân',
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&h=500&fit=crop&q=80',
                N'Khu nghỉ dưỡng 4 sao phong cách làng quê Việt Nam, nằm yên bình giữa thiên nhiên xanh mát, gần trung tâm Huế cổ kính.',
                4.3, 0, N'REJECTED', 0, NULL);

        -- H10: Victoria Can Tho – APPROVED
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @owner10_id AND name = N'Victoria Can Tho Resort')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@owner10_id, N'Victoria Can Tho Resort', N'Cồn Cái Khế, Ninh Kiều, Cần Thơ', N'Cần Thơ', N'Ninh Kiều',
                N'https://images.unsplash.com/photo-1560347876-aeef00ee58a1?w=800&h=500&fit=crop&q=80',
                N'Resort 4 sao nằm trên đảo nhỏ giữa sông Hậu, không gian xanh mát, hồ bơi lớn và view sông tuyệt đẹp. Điểm đến lý tưởng tại miền Tây sông nước.',
                4.4, 0, N'APPROVED', 1, DATEADD(DAY, -40, GETDATE()));

        -- Get Hotel IDs
        SELECT @h1_id  = id FROM hotel WHERE owner_id = @owner1_id  AND name = N'InterContinental Danang Sun Peninsula Resort';
        SELECT @h2_id  = id FROM hotel WHERE owner_id = @owner2_id  AND name = N'Vinpearl Resort Nha Trang';
        SELECT @h3_id  = id FROM hotel WHERE owner_id = @owner3_id  AND name = N'Four Seasons Resort The Nam Hai';
        SELECT @h4_id  = id FROM hotel WHERE owner_id = @owner4_id  AND name = N'Dalat Palace Heritage Hotel';
        SELECT @h5_id  = id FROM hotel WHERE owner_id = @owner5_id  AND name = N'JW Marriott Phu Quoc Emerald Bay';
        SELECT @h6_id  = id FROM hotel WHERE owner_id = @owner6_id  AND name = N'Hotel de la Coupole - MGallery';
        SELECT @h7_id  = id FROM hotel WHERE owner_id = @owner7_id  AND name = N'Sofitel Legend Metropole Hanoi';
        SELECT @h8_id  = id FROM hotel WHERE owner_id = @owner8_id  AND name = N'Park Hyatt Saigon';
        SELECT @h9_id  = id FROM hotel WHERE owner_id = @owner9_id  AND name = N'Pilgrimage Village Boutique Resort';
        SELECT @h10_id = id FROM hotel WHERE owner_id = @owner10_id AND name = N'Victoria Can Tho Resort';

        -- =====================================================
        -- 3. HOTEL VERIFICATION DOCUMENTS
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h1_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h1_id, N'+842363880001', N'intercontinental.danang@hotel.com', N'/docs/intercontinental/business.pdf', N'/docs/intercontinental/land.pdf', N'/docs/intercontinental/contract.pdf', N'APPROVED', DATEADD(DAY, -90, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h2_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h2_id, N'+842583880002', N'vinpearl.nhatrang@hotel.com', N'/docs/vinpearl/business.pdf', N'/docs/vinpearl/land.pdf', N'/docs/vinpearl/contract.pdf', N'APPROVED', DATEADD(DAY, -85, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h3_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h3_id, N'+842353880003', N'fourseasons.hoian@hotel.com', N'/docs/fourseasons/business.pdf', N'/docs/fourseasons/land.pdf', N'/docs/fourseasons/contract.pdf', N'APPROVED', DATEADD(DAY, -80, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h4_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h4_id, N'+842633880004', N'dalatpalace@hotel.com', N'/docs/dalatpalace/business.pdf', N'/docs/dalatpalace/land.pdf', N'/docs/dalatpalace/contract.pdf', N'APPROVED', DATEADD(DAY, -75, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h5_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h5_id, N'+842973880005', N'jwmarriott.phuquoc@hotel.com', N'/docs/jwmarriott/business.pdf', N'/docs/jwmarriott/land.pdf', N'/docs/jwmarriott/contract.pdf', N'APPROVED', DATEADD(DAY, -70, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h6_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h6_id, N'+842143880006', N'coupole.sapa@hotel.com', N'/docs/coupole/business.pdf', N'/docs/coupole/land.pdf', N'/docs/coupole/contract.pdf', N'APPROVED', DATEADD(DAY, -65, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h7_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h7_id, N'+842438880007', N'metropole.hanoi@hotel.com', N'/docs/metropole/business.pdf', N'/docs/metropole/land.pdf', N'/docs/metropole/contract.pdf', N'PENDING', NULL);

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h8_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h8_id, N'+842838880008', N'parkhyatt.saigon@hotel.com', N'/docs/parkhyatt/business.pdf', N'/docs/parkhyatt/land.pdf', N'/docs/parkhyatt/contract.pdf', N'APPROVED', DATEADD(DAY, -50, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h9_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h9_id, N'+842343880009', N'pilgrimage.hue@hotel.com', N'/docs/pilgrimage/business.pdf', N'/docs/pilgrimage/land.pdf', N'/docs/pilgrimage/contract.pdf', N'REJECTED', DATEADD(DAY, -30, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h10_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h10_id, N'+842923880010', N'victoria.cantho@hotel.com', N'/docs/victoria/business.pdf', N'/docs/victoria/land.pdf', N'/docs/victoria/contract.pdf', N'APPROVED', DATEADD(DAY, -40, GETDATE()));

        -- =====================================================
        -- 4. HOTEL FACILITIES & VIEWS
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h1_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h1_id, 1,1,1,1,1,1,1,1,1,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h2_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h2_id, 1,1,1,1,1,1,1,1,0,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h3_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h3_id, 1,1,1,1,1,1,1,1,0,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h4_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h4_id, 1,1,1,0,1,0,1,1,0,1,0,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h5_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h5_id, 1,1,1,1,1,1,1,1,0,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h6_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h6_id, 1,1,1,1,1,0,1,1,0,1,0,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h7_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h7_id, 0,1,1,1,1,0,1,1,0,1,0,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h8_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h8_id, 1,1,1,1,1,0,1,1,0,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h9_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h9_id, 1,1,1,0,1,0,1,1,0,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h10_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h10_id, 1,1,1,1,1,0,1,1,0,1,1,1);

        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h1_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h1_id,  1,1,0,1,0,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h2_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h2_id,  0,1,1,1,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h3_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h3_id,  0,1,1,1,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h4_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h4_id,  1,0,1,0,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h5_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h5_id,  0,1,1,1,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h6_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h6_id,  0,0,0,0,0,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h7_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h7_id,  1,0,0,0,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h8_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h8_id,  1,0,0,0,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h9_id)  INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h9_id,  0,0,1,1,0,0);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h10_id) INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view) VALUES (@h10_id, 0,0,1,1,1,0);

        -- =====================================================
        -- 5. PROMOTIONS
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h1_id AND title = N'Summer Peninsula Escape')
            INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
            VALUES (@h1_id, N'Summer Peninsula Escape', N'Giảm 15% khi đặt phòng Resort King hoặc Villa trong tháng 7-8.', 15.00, '2026-07-01', '2026-08-31', N'ACTIVE');

        IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h2_id AND title = N'Vinpearl Cable Car Offer')
            INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
            VALUES (@h2_id, N'Vinpearl Cable Car Offer', N'Giảm 12% kèm vé cáp treo miễn phí khi đặt từ 2 đêm.', 12.00, '2026-07-01', '2026-09-15', N'ACTIVE');

        IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h5_id AND title = N'Emerald Bay Early Bird')
            INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
            VALUES (@h5_id, N'Emerald Bay Early Bird', N'Đặt phòng trước 30 ngày giảm 20% cho tất cả loại phòng.', 20.00, '2026-07-01', '2026-12-31', N'ACTIVE');

        SELECT @promo1_id = id FROM promotions WHERE hotel_id = @h1_id AND title = N'Summer Peninsula Escape';
        SELECT @promo2_id = id FROM promotions WHERE hotel_id = @h2_id AND title = N'Vinpearl Cable Car Offer';
        SELECT @promo3_id = id FROM promotions WHERE hotel_id = @h5_id AND title = N'Emerald Bay Early Bird';

        -- =====================================================
        -- 6. ROOMS (UNIQUE IMAGES PER ROOM) - KEEP EXISTING
        -- =====================================================

        -- H1: InterContinental (5 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Resort King Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h1_id, N'Resort King Room', 15, N'Phòng Resort King rộng rãi với giường king, view biển tuyệt đẹp và ban công riêng.', 5500000, 42, 1, 2, 2,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Resort Twin Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h1_id, N'Resort Twin Room', 20, N'Phòng Twin với 2 giường đơn, view hồ bơi và khu vườn nhiệt đới.', 5200000, 40, 2, 2, 2,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Club Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h1_id, N'Club Suite', 8, N'Sang trọng với phòng khách riêng, quyền sử dụng Club Lounge và view toàn cảnh biển Sơn Trà.', 9500000, 65, 1, 2, 3,
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'One Bedroom Villa')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h1_id, N'One Bedroom Villa', 5, N'Villa riêng với hồ bơi riêng, khu vườn và bếp nhỏ, không gian sống cực kỳ riêng tư.', 18500000, 120, 1, 2, 4,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h1_id AND room_type = N'Two Bedroom Villa')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h1_id, N'Two Bedroom Villa', 3, N'Villa 2 phòng ngủ, hồ bơi riêng, tầm nhìn biển, thích hợp cho gia đình.', 28500000, 180, 2, 4, 5,
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);

        -- H2: Vinpearl (5 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Deluxe Sea View')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h2_id, N'Deluxe Sea View', 30, N'Phòng Deluxe view biển tuyệt đẹp, ban công rộng, tiện nghi hiện đại.', 3200000, 38, 1, 2, 2,
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Premium Ocean Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h2_id, N'Premium Ocean Suite', 10, N'Suite view đại dương với phòng khách, hồ bơi riêng, không gian sang trọng.', 7800000, 72, 1, 2, 3,
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Family Garden View')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h2_id, N'Family Garden View', 12, N'Phòng gia đình view vườn, rộng rãi, thích hợp cho gia đình 4 người.', 4500000, 55, 2, 4, 2,
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Executive Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h2_id, N'Executive Suite', 8, N'Sang trọng với phòng khách và phòng ngủ riêng biệt, club lounge access.', 8900000, 80, 1, 2, 3,
                N'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h2_id AND room_type = N'Vinpearl Villa')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h2_id, N'Vinpearl Villa', 5, N'Villa biển sang trọng với hồ bơi riêng, phòng khách rộng lớn, view đảo Hòn Tre.', 15000000, 150, 2, 4, 4,
                N'https://images.unsplash.com/photo-1560347876-aeef00ee58a1?w=600&h=400&fit=crop&q=80', 1);

        -- H3: Four Seasons (4 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Pool Villa 1 Bedroom')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h3_id, N'Pool Villa 1 Bedroom', 10, N'Villa 1 phòng ngủ với hồ bơi riêng, view biển Hà My, thiết kế Việt Nam sang trọng.', 12000000, 110, 1, 2, 3,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Pool Villa 3 Bedrooms')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h3_id, N'Pool Villa 3 Bedrooms', 5, N'Villa 3 phòng ngủ, hồ bơi riêng, bếp đầy đủ, dành cho gia đình hoặc nhóm bạn.', 28000000, 250, 3, 6, 6,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Deluxe Beach Front')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h3_id, N'Deluxe Beach Front', 20, N'Phòng Deluxe ngay sát bãi biển, ban công riêng, view biển tuyệt vời.', 7500000, 55, 1, 2, 2,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h3_id AND room_type = N'Nam Hai Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h3_id, N'Nam Hai Suite', 6, N'Suite danh tiếng với thiết kế nghệ thuật, hồ bơi riêng, butler service 24/7.', 20000000, 160, 1, 2, 4,
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);

        -- H4: Dalat Palace (4 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Heritage Classic Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h4_id, N'Heritage Classic Room', 20, N'Phòng cổ điển phong cách Pháp, view hồ Xuân Hương, trang trí vintage sang trọng.', 3800000, 35, 1, 2, 2,
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Palace Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h4_id, N'Palace Suite', 6, N'Suite hoàng gia rộng lớn, phòng khách riêng, bồn tắm cổ điển, fireplace ấm cúng.', 8500000, 75, 1, 2, 3,
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Deluxe Garden View')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h4_id, N'Deluxe Garden View', 15, N'Phòng Deluxe view vườn hoa, không gian yên bình thơ mộng giữa thành phố ngàn hoa.', 4800000, 45, 1, 2, 2,
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h4_id AND room_type = N'Presidential Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h4_id, N'Presidential Suite', 2, N'Suite tổng thống sang trọng nhất khách sạn, phòng khách & phòng ăn riêng, butler 24/7.', 18000000, 130, 2, 4, 5,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

        -- H5: JW Marriott Phu Quoc (3 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h5_id AND room_type = N'Emerald Bay Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h5_id, N'Emerald Bay Room', 25, N'Phòng nghỉ cao cấp với thiết kế trường đại học lịch sử, view biển Khem tuyệt đẹp.', 6500000, 48, 1, 2, 2,
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h5_id AND room_type = N'Lagoon Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h5_id, N'Lagoon Suite', 10, N'Suite với hồ bơi riêng, view đầm phá xanh mát, phong cách thực thực vật nhiệt đới.', 15000000, 95, 1, 2, 3,
                N'https://images.unsplash.com/photo-1560347876-aeef00ee58a1?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h5_id AND room_type = N'Sunset Residence')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h5_id, N'Sunset Residence', 4, N'Biệt thự với 2 phòng ngủ, hồ bơi riêng, bếp đầy đủ, tầm nhìn sunset tuyệt đỉnh.', 32000000, 200, 2, 4, 5,
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);

        -- H6: Hotel de la Coupole Sapa (2 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h6_id AND room_type = N'Deluxe Mountain View')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h6_id, N'Deluxe Mountain View', 30, N'Phòng Deluxe với tầm nhìn Hoàng Liên Sơn hùng vĩ, thiết kế Pháp rực rỡ và ấm cúng.', 4200000, 38, 1, 2, 2,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h6_id AND room_type = N'Coupole Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h6_id, N'Coupole Suite', 8, N'Suite toàn cảnh núi Fansipan, phòng khách riêng với fireplace, trang trí màu sắc độc đáo.', 9800000, 72, 1, 2, 3,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);

        -- H7: Sofitel Metropole (2 rooms)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h7_id AND room_type = N'Premium Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h7_id, N'Premium Room', 40, N'Phòng Premium cổ điển lịch sử, trang trí Pháp thuộc, nằm trong tòa nhà 1901 lịch sử.', 5800000, 35, 1, 2, 2,
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);

        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h7_id AND room_type = N'Opera Suite')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h7_id, N'Opera Suite', 6, N'Suite với tầm nhìn Nhà Hát Lớn Hà Nội, phòng khách sang trọng và phòng tắm marble.', 14500000, 80, 1, 2, 3,
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);

        -- H8: Park Hyatt Saigon (1 room)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h8_id AND room_type = N'Park King Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h8_id, N'Park King Room', 35, N'Phòng King với thiết kế Pháp cổ điển, tầm nhìn công viên hoặc hồ bơi tầng thượng tuyệt đẹp.', 7200000, 45, 1, 2, 2,
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&q=80', 1);

        -- H9: Pilgrimage Village (1 room) - REJECTED hotel
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h9_id AND room_type = N'Boutique Garden Room')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h9_id, N'Boutique Garden Room', 20, N'Phòng boutique nhỏ gọn, nép mình giữa vườn cây xanh, phong cách làng quê Việt.', 2200000, 28, 1, 2, 1,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);

        -- H10: Victoria Can Tho (1 room)
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h10_id AND room_type = N'River View Deluxe')
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h10_id, N'River View Deluxe', 25, N'Phòng Deluxe view sông Hậu thơ mộng, ban công riêng, không gian miền Tây sông nước.', 2800000, 32, 1, 2, 2,
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);

        -- Get Room IDs
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

        SELECT @h3_r1_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Pool Villa 1 Bedroom';
        SELECT @h3_r2_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Pool Villa 3 Bedrooms';
        SELECT @h3_r3_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Deluxe Beach Front';
        SELECT @h3_r4_id = id FROM room WHERE hotel_id = @h3_id AND room_type = N'Nam Hai Suite';

        SELECT @h4_r1_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Heritage Classic Room';
        SELECT @h4_r2_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Palace Suite';
        SELECT @h4_r3_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Deluxe Garden View';
        SELECT @h4_r4_id = id FROM room WHERE hotel_id = @h4_id AND room_type = N'Presidential Suite';

        SELECT @h5_r1_id = id FROM room WHERE hotel_id = @h5_id AND room_type = N'Emerald Bay Room';
        SELECT @h5_r2_id = id FROM room WHERE hotel_id = @h5_id AND room_type = N'Lagoon Suite';
        SELECT @h5_r3_id = id FROM room WHERE hotel_id = @h5_id AND room_type = N'Sunset Residence';

        SELECT @h6_r1_id = id FROM room WHERE hotel_id = @h6_id AND room_type = N'Deluxe Mountain View';
        SELECT @h6_r2_id = id FROM room WHERE hotel_id = @h6_id AND room_type = N'Coupole Suite';

        SELECT @h7_r1_id = id FROM room WHERE hotel_id = @h7_id AND room_type = N'Premium Room';
        SELECT @h7_r2_id = id FROM room WHERE hotel_id = @h7_id AND room_type = N'Opera Suite';

        SELECT @h8_r1_id = id FROM room WHERE hotel_id = @h8_id AND room_type = N'Park King Room';
        SELECT @h9_r1_id = id FROM room WHERE hotel_id = @h9_id AND room_type = N'Boutique Garden Room';
        SELECT @h10_r1_id = id FROM room WHERE hotel_id = @h10_id AND room_type = N'River View Deluxe';

        -- =====================================================
        -- 7. BOOKINGS (KEEP EXISTING)
        -- =====================================================

        -- b1: PENDING
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c1_id AND room_id = @h2_r1_id AND check_in_date = '2026-07-20')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity, platform_fee_percent, payout_status)
            VALUES (@c1_id, @h2_r1_id, @h2_id, N'+84933000001', N'Nguyễn Văn Hoàng', N'hoang.nguyen@customer.test',
                '2026-07-20', '2026-07-22', 2, 6400000, N'PENDING', N'Yêu cầu phòng tầng cao view biển.', 1, 10.00, N'PENDING');

        -- b2: CONFIRMED
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c2_id AND room_id = @h1_r3_id AND check_in_date = '2026-07-25')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, id_promotion, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity, platform_fee_percent, payout_status)
            VALUES (@c2_id, @h1_r3_id, @h1_id, N'+84933000002', N'Trần Thị Linh', N'linh.tran@customer.test',
                @promo1_id, '2026-07-25', '2026-07-28', 3, 24225000, N'CONFIRMED', N'Yêu cầu bố trí hoa chào mừng.', 1, 10.00, N'PENDING');

        -- b3: CONFIRMED
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c3_id AND room_id = @h3_r3_id AND check_in_date = '2026-07-10')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity, platform_fee_percent, payout_status)
            VALUES (@c3_id, @h3_r3_id, @h3_id, N'+84933000003', N'Phạm Đức', N'duc.pham@customer.test',
                '2026-07-10', '2026-07-13', 3, 22500000, N'CONFIRMED', N'Khách kỷ niệm 5 năm ngày cưới.', 1, 10.00, N'PENDING');

        -- b4: COMPLETED + PAID + payout PENDING
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c4_id AND room_id = @h5_r1_id AND check_in_date = '2026-06-01')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, id_promotion, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount, payout_status)
            VALUES (@c4_id, @h5_r1_id, @h5_id, N'+84933000004', N'Lê Thị Mai', N'mai.le@customer.test',
                @promo3_id, '2026-06-01', '2026-06-04', 3, 15600000, N'COMPLETED', N'Đã check-out đúng hạn.', 1,
                10.00, 1560000, 14040000, N'PENDING');

        -- b5: COMPLETED + PAID + payout PENDING
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c5_id AND room_id = @h2_r2_id AND check_in_date = '2026-05-20')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, id_promotion, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount, payout_status)
            VALUES (@c5_id, @h2_r2_id, @h2_id, N'+84933000005', N'Nguyễn Quang', N'quang.nguyen@customer.test',
                @promo2_id, '2026-05-20', '2026-05-23', 3, 20592000, N'COMPLETED', N'Khách rất hài lòng với dịch vụ.', 1,
                10.00, 2059200, 18532800, N'PENDING');

        -- b6: COMPLETED + PAID + payout PAID
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c6_id AND room_id = @h8_r1_id AND check_in_date = '2026-04-15')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount,
                payout_status, payout_at, payout_bank_name, payout_bank_account_number, payout_bank_account_holder)
            VALUES (@c6_id, @h8_r1_id, @h8_id, N'+84933000006', N'Võ Thị Hà', N'ha.vo@customer.test',
                '2026-04-15', '2026-04-17', 2, 14400000, N'COMPLETED', N'Check-out sớm 1 tiếng.', 1,
                10.00, 1440000, 12960000,
                N'PAID', DATEADD(DAY, -40, GETDATE()), N'Sacombank', N'8000800080', N'DANG QUANG HUY');

        -- b7: COMPLETED + PAID + payout PENDING
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c7_id AND room_id = @h4_r1_id AND check_in_date = '2026-05-05')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount, payout_status)
            VALUES (@c7_id, @h4_r1_id, @h4_id, N'+84933000007', N'Hoàng Kiên', N'kien.hoang@customer.test',
                '2026-05-05', '2026-05-08', 3, 11400000, N'COMPLETED', N'Yêu cầu phòng có lò sưởi.', 1,
                10.00, 1140000, 10260000, N'PENDING');

        -- b8: COMPLETED + PAID + payout PAID
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c8_id AND room_id = @h1_r4_id AND check_in_date = '2026-04-01')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount,
                payout_status, payout_at, payout_bank_name, payout_bank_account_number, payout_bank_account_holder)
            VALUES (@c8_id, @h1_r4_id, @h1_id, N'+84933000008', N'Trần Thị Thanh Anh', N'thanh.anh@customer.test',
                '2026-04-01', '2026-04-05', 4, 74000000, N'COMPLETED', N'Kỷ niệm trăng mật.', 1,
                10.00, 7400000, 66600000,
                N'PAID', DATEADD(DAY, -55, GETDATE()), N'Vietcombank', N'1000100010', N'LE MINH HOANG');

        -- b9: COMPLETED + PAID + payout PENDING
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c9_id AND room_id = @h6_r1_id AND check_in_date = '2026-06-10')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount, payout_status)
            VALUES (@c9_id, @h6_r1_id, @h6_id, N'+84933000009', N'Lê Bình', N'binh.le@customer.test',
                '2026-06-10', '2026-06-13', 3, 12600000, N'COMPLETED', N'Muốn ngắm núi sáng sớm.', 1,
                10.00, 1260000, 11340000, N'PENDING');

        -- b10: CONFIRMED
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c10_id AND room_id = @h10_r1_id AND check_in_date = '2026-07-08')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity, platform_fee_percent, payout_status)
            VALUES (@c10_id, @h10_r1_id, @h10_id, N'+84933000010', N'Huỳnh Thị Loan', N'loan.huynh@customer.test',
                '2026-07-08', '2026-07-10', 2, 5600000, N'CONFIRMED', N'Yêu cầu phòng im lặng view sông.', 1, 10.00, N'PENDING');

        -- b11: PENDING
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c1_id AND room_id = @h4_r3_id AND check_in_date = '2026-08-15')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity, platform_fee_percent, payout_status)
            VALUES (@c1_id, @h4_r3_id, @h4_id, N'+84933000001', N'Nguyễn Văn Hoàng', N'hoang.nguyen@customer.test',
                '2026-08-15', '2026-08-18', 3, 14400000, N'PENDING', N'Cần phòng yên tĩnh để làm việc.', 1, 10.00, N'PENDING');

        -- b12: COMPLETED + PAID + payout PAID
        IF NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = @c2_id AND room_id = @h2_r3_id AND check_in_date = '2026-03-20')
            INSERT INTO bookings (customer_id, room_id, hotel_id, phone, full_name, email, check_in_date, check_out_date,
                num_nights, total_price, status, special_notes, quantity,
                platform_fee_percent, platform_fee_amount, owner_payout_amount,
                payout_status, payout_at, payout_bank_name, payout_bank_account_number, payout_bank_account_holder)
            VALUES (@c2_id, @h2_r3_id, @h2_id, N'+84933000002', N'Trần Thị Linh', N'linh.tran@customer.test',
                '2026-03-20', '2026-03-24', 4, 18000000, N'COMPLETED', N'Gia đình 4 người, cần nôi cho em bé.', 1,
                10.00, 1800000, 16200000,
                N'PAID', DATEADD(DAY, -70, GETDATE()), N'Techcombank', N'2000200020', N'PHAM THI HOA');

        -- Get Booking IDs
        SELECT @b1_id  = id FROM bookings WHERE customer_id = @c1_id  AND room_id = @h2_r1_id AND check_in_date = '2026-07-20';
        SELECT @b2_id  = id FROM bookings WHERE customer_id = @c2_id  AND room_id = @h1_r3_id AND check_in_date = '2026-07-25';
        SELECT @b3_id  = id FROM bookings WHERE customer_id = @c3_id  AND room_id = @h3_r3_id AND check_in_date = '2026-07-10';
        SELECT @b4_id  = id FROM bookings WHERE customer_id = @c4_id  AND room_id = @h5_r1_id AND check_in_date = '2026-06-01';
        SELECT @b5_id  = id FROM bookings WHERE customer_id = @c5_id  AND room_id = @h2_r2_id AND check_in_date = '2026-05-20';
        SELECT @b6_id  = id FROM bookings WHERE customer_id = @c6_id  AND room_id = @h8_r1_id AND check_in_date = '2026-04-15';
        SELECT @b7_id  = id FROM bookings WHERE customer_id = @c7_id  AND room_id = @h4_r1_id AND check_in_date = '2026-05-05';
        SELECT @b8_id  = id FROM bookings WHERE customer_id = @c8_id  AND room_id = @h1_r4_id AND check_in_date = '2026-04-01';
        SELECT @b9_id  = id FROM bookings WHERE customer_id = @c9_id  AND room_id = @h6_r1_id AND check_in_date = '2026-06-10';
        SELECT @b10_id = id FROM bookings WHERE customer_id = @c10_id AND room_id = @h10_r1_id AND check_in_date = '2026-07-08';
        SELECT @b11_id = id FROM bookings WHERE customer_id = @c1_id  AND room_id = @h4_r3_id AND check_in_date = '2026-08-15';
        SELECT @b12_id = id FROM bookings WHERE customer_id = @c2_id  AND room_id = @h2_r3_id AND check_in_date = '2026-03-20';

        -- =====================================================
        -- 8. PAYMENTS (KEEP EXISTING)
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b1_id)
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b1_id, 6400000, N'QR_CODE', N'PENDING', N'/qr/b1.png', NULL, NULL, DATEADD(HOUR, 24, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b2_id AND transaction_id = N'TXN-B002-IC')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b2_id, 24225000, N'QR_CODE', N'PAID', N'/qr/b2.png', N'TXN-B002-IC', DATEADD(DAY, -2, GETDATE()), DATEADD(HOUR, 24, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b3_id AND transaction_id = N'TXN-B003-FS')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b3_id, 22500000, N'BANK_TRANSFER', N'PAID', N'/qr/b3.png', N'TXN-B003-FS', DATEADD(DAY, -3, GETDATE()), DATEADD(HOUR, 24, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b4_id AND transaction_id = N'TXN-B004-JW')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b4_id, 15600000, N'PAYPAL', N'PAID', N'/qr/b4.png', N'TXN-B004-JW', '2026-05-31 10:00:00', '2026-05-31 12:00:00');

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b5_id AND transaction_id = N'TXN-B005-VP')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b5_id, 20592000, N'QR_CODE', N'PAID', N'/qr/b5.png', N'TXN-B005-VP', '2026-05-19 14:30:00', '2026-05-19 17:00:00');

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b6_id AND transaction_id = N'TXN-B006-PH')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b6_id, 14400000, N'MOMO', N'PAID', N'/qr/b6.png', N'TXN-B006-PH', '2026-04-14 09:00:00', '2026-04-14 11:00:00');

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b7_id AND transaction_id = N'TXN-B007-DP')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b7_id, 11400000, N'QR_CODE', N'PAID', N'/qr/b7.png', N'TXN-B007-DP', '2026-05-04 08:00:00', '2026-05-04 10:00:00');

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b8_id AND transaction_id = N'TXN-B008-IC')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b8_id, 74000000, N'BANK_TRANSFER', N'PAID', N'/qr/b8.png', N'TXN-B008-IC', '2026-03-31 11:00:00', '2026-03-31 13:00:00');

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b9_id AND transaction_id = N'TXN-B009-CP')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b9_id, 12600000, N'QR_CODE', N'PAID', N'/qr/b9.png', N'TXN-B009-CP', '2026-06-09 15:00:00', '2026-06-09 17:00:00');

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b10_id AND transaction_id = N'TXN-B010-VC')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b10_id, 5600000, N'QR_CODE', N'PAID', N'/qr/b10.png', N'TXN-B010-VC', DATEADD(DAY, -1, GETDATE()), DATEADD(HOUR, 24, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b11_id)
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b11_id, 14400000, N'QR_CODE', N'PENDING', N'/qr/b11.png', NULL, NULL, DATEADD(HOUR, 12, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = @b12_id AND transaction_id = N'TXN-B012-VP')
            INSERT INTO payments (booking_id, amount, method, status, qr_code_url, transaction_id, paid_at, qr_expires_at)
            VALUES (@b12_id, 18000000, N'PAYPAL', N'PAID', N'/qr/b12.png', N'TXN-B012-VP', '2026-03-19 16:00:00', '2026-03-19 18:00:00');

        -- =====================================================
        -- 9. REFUNDS (KEEP EXISTING)
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @b1_id)
            INSERT INTO refunds (booking_id, bank_name, account_number, account_holder, refund_amount, status, note, cancellation_reason, requested_at, processed_at)
            VALUES (@b1_id, N'Vietcombank', N'1234567890', N'NGUYEN VAN HOANG', 6400000, N'PENDING',
                N'Khách hủy vì thay đổi kế hoạch du lịch.', N'Personal reasons/Trip called off', DATEADD(HOUR, -3, GETDATE()), NULL);

        IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @b7_id)
            INSERT INTO refunds (booking_id, bank_name, account_number, account_holder, refund_amount, status, note, cancellation_reason, requested_at, processed_at)
            VALUES (@b7_id, N'BIDV', N'4444444444', N'HOANG KIEN', 5700000, N'PROCESSED',
                N'Hoàn một phần theo chính sách khách sạn.', N'Change of dates or destination', DATEADD(DAY, -20, GETDATE()), DATEADD(DAY, -19, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM refunds WHERE booking_id = @b11_id)
            INSERT INTO refunds (booking_id, bank_name, account_number, account_holder, refund_amount, status, note, cancellation_reason, requested_at, processed_at)
            VALUES (@b11_id, N'Techcombank', N'9876543210', N'NGUYEN VAN HOANG', 14400000, N'PENDING',
                N'Khách chưa thanh toán, yêu cầu hủy phòng.', N'Found a different accommodation option', DATEADD(HOUR, -1, GETDATE()), NULL);

        -- =====================================================
        -- 10. FEEDBACK (KEEP EXISTING)
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c4_id AND hotel_id = @h5_id AND room_id = @h5_r1_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c4_id, @h5_id, @h5_r1_id, N'Lê Thị Mai', N'Emerald Bay Room',
                N'Resort đẹp tuyệt vời! Bãi biển Kem trong xanh, phòng rộng và sạch. Nhân viên nhiệt tình, dịch vụ butler 24/7 rất chuyên nghiệp. Sẽ quay lại!',
                5, 18, 0, N'VISIBLE');

        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c5_id AND hotel_id = @h2_id AND room_id = @h2_r2_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c5_id, @h2_id, @h2_r2_id, N'Nguyễn Quang', N'Premium Ocean Suite',
                N'Cáp treo vượt biển là trải nghiệm cực kỳ ấn tượng. Phòng suite view đại dương đẹp đến ngỡ ngàng. Giá có hơi cao nhưng xứng đáng.',
                4, 12, 1, N'VISIBLE');

        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c6_id AND hotel_id = @h8_id AND room_id = @h8_r1_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c6_id, @h8_id, @h8_r1_id, N'Võ Thị Hà', N'Park King Room',
                N'Vị trí trung tâm Sài Gòn rất tiện, phòng tuy không quá lớn nhưng thiết kế đẹp. Hồ bơi tầng thượng view thành phố rất ấn tượng.',
                4, 9, 0, N'VISIBLE');

        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c7_id AND hotel_id = @h4_id AND room_id = @h4_r1_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c7_id, @h4_id, @h4_r1_id, N'Hoàng Kiên', N'Heritage Classic Room',
                N'Không gian cổ điển Pháp thuộc rất độc đáo và lãng mạn. View hồ Xuân Hương tuyệt đẹp từ ban công. Bữa sáng ngon và phong phú.',
                5, 15, 0, N'VISIBLE');

        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c8_id AND hotel_id = @h1_id AND room_id = @h1_r4_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c8_id, @h1_id, @h1_r4_id, N'Trần Thị Thanh Anh', N'One Bedroom Villa',
                N'Honeymoon tuyệt vời tại InterContinental! Villa riêng với hồ bơi private, view biển Sơn Trà không nơi nào sánh bằng. Đây là kỳ nghỉ đáng nhớ nhất đời.',
                5, 30, 0, N'VISIBLE');

        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c9_id AND hotel_id = @h6_id AND room_id = @h6_r1_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c9_id, @h6_id, @h6_r1_id, N'Lê Bình', N'Deluxe Mountain View',
                N'Sapa từ khung cửa sổ phòng đẹp như tranh. Khách sạn phong cách Pháp độc đáo, màu sắc rực rỡ. Nhân viên thân thiện và nhiệt tình.',
                5, 20, 1, N'VISIBLE');

        IF NOT EXISTS (SELECT 1 FROM feedback WHERE customer_id = @c2_id AND hotel_id = @h2_id AND room_id = @h2_r3_id)
            INSERT INTO feedback (customer_id, hotel_id, room_id, user_full_name, room_type, comment, rating, upvote, downvote, status)
            VALUES (@c2_id, @h2_id, @h2_r3_id, N'Trần Thị Linh', N'Family Garden View',
                N'Phòng khá ổn cho gia đình, nhưng cách âm kém, nghe rõ tiếng hàng xóm. Hồ bơi đẹp nhưng đông người vào cuối tuần.', 3, 0, 0, N'PENDING');

        -- Get Feedback IDs
        SELECT @fb1_id = id FROM feedback WHERE customer_id = @c4_id AND hotel_id = @h5_id AND room_id = @h5_r1_id;
        SELECT @fb2_id = id FROM feedback WHERE customer_id = @c5_id AND hotel_id = @h2_id AND room_id = @h2_r2_id;
        SELECT @fb3_id = id FROM feedback WHERE customer_id = @c6_id AND hotel_id = @h8_id AND room_id = @h8_r1_id;
        SELECT @fb4_id = id FROM feedback WHERE customer_id = @c7_id AND hotel_id = @h4_id AND room_id = @h4_r1_id;
        SELECT @fb5_id = id FROM feedback WHERE customer_id = @c8_id AND hotel_id = @h1_id AND room_id = @h1_r4_id;
        SELECT @fb6_id = id FROM feedback WHERE customer_id = @c9_id AND hotel_id = @h6_id AND room_id = @h6_r1_id;

        -- Feedback Replies
        IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @fb1_id AND owner_id = @owner5_id)
            INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
            VALUES (@fb1_id, @owner5_id, @h5_id, N'Cảm ơn bạn đã lựa chọn JW Marriott Phu Quoc! Chúng tôi rất vui khi bạn hài lòng. Mong được đón tiếp bạn lần sau tại Emerald Bay!');

        IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @fb2_id AND owner_id = @owner2_id)
            INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
            VALUES (@fb2_id, @owner2_id, @h2_id, N'Cảm ơn góp ý chân thành! Vinpearl sẽ cải thiện chính sách giá để phù hợp hơn. Hẹn gặp lại bạn tại đảo Hòn Tre!');

        IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @fb4_id AND owner_id = @owner4_id)
            INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
            VALUES (@fb4_id, @owner4_id, @h4_id, N'Cảm ơn bạn rất nhiều! Dalat Palace rất vui khi được chia sẻ không gian cổ điển lãng mạn với bạn. Luôn chào đón bạn quay lại!');

        IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @fb5_id AND owner_id = @owner1_id)
            INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
            VALUES (@fb5_id, @owner1_id, @h1_id, N'Chúc mừng kỳ nghỉ trăng mật! InterContinental Danang rất hân hạnh được là một phần trong kỷ niệm đẹp của bạn. Hẹn gặp lại!');

        IF NOT EXISTS (SELECT 1 FROM feedback_replies WHERE feedback_id = @fb6_id AND owner_id = @owner6_id)
            INSERT INTO feedback_replies (feedback_id, owner_id, hotel_id, content)
            VALUES (@fb6_id, @owner6_id, @h6_id, N'Cảm ơn bạn! Hotel de la Coupole luôn cố gắng mang lại trải nghiệm Sapa đẹp nhất. Rất vui được phục vụ bạn!');

        -- =====================================================
        -- 11. WISHLISTS (KEEP EXISTING)
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @c1_id AND room_id = @h1_r4_id AND check_in_date = '2026-09-01')
            INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
            VALUES (@c1_id, @h1_r4_id, '2026-09-01', '2026-09-05');

        IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @c2_id AND room_id = @h5_r2_id AND check_in_date = '2026-08-20')
            INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
            VALUES (@c2_id, @h5_r2_id, '2026-08-20', '2026-08-23');

        IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @c3_id AND room_id = @h3_r1_id AND check_in_date = '2026-09-10')
            INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
            VALUES (@c3_id, @h3_r1_id, '2026-09-10', '2026-09-13');

        IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @c4_id AND room_id = @h6_r2_id AND check_in_date = '2026-12-25')
            INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
            VALUES (@c4_id, @h6_r2_id, '2026-12-25', '2026-12-28');

        IF NOT EXISTS (SELECT 1 FROM wishlists WHERE customer_id = @c5_id AND room_id = @h4_r2_id AND check_in_date = '2026-11-01')
            INSERT INTO wishlists (customer_id, room_id, check_in_date, check_out_date)
            VALUES (@c5_id, @h4_r2_id, '2026-11-01', '2026-11-04');

        -- =====================================================
        -- 12. MESSAGES (KEEP EXISTING)
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @c2_user_id AND receiver_id = @owner1_user_id AND booking_id = @b2_id)
            INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
            VALUES (@c2_user_id, @owner1_user_id, @b2_id, @h1_id, N'Chào resort, mình đặt phòng Club Suite cho tháng 7. Liệu có thể bố trí hoa và rượu champagne trong phòng trước khi check-in không?', 0, DATEADD(HOUR, -5, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @owner1_user_id AND receiver_id = @c2_user_id AND booking_id = @b2_id)
            INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
            VALUES (@owner1_user_id, @c2_user_id, @b2_id, @h1_id, N'Chào bạn! InterContinental rất vui được phục vụ. Chúng tôi sẽ bố trí hoa tươi và champagne theo yêu cầu. Xin cho biết loài hoa ưa thích!', 0, DATEADD(HOUR, -4, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @c4_user_id AND receiver_id = @owner5_user_id AND booking_id = @b4_id)
            INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
            VALUES (@c4_user_id, @owner5_user_id, @b4_id, @h5_id, N'Cảm ơn JW Marriott! Kỳ nghỉ vừa rồi tuyệt vời. Bãi biển Kem đẹp quá, nhân viên rất nhiệt tình!', 1, DATEADD(DAY, -5, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @owner5_user_id AND receiver_id = @c4_user_id AND booking_id = @b4_id)
            INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
            VALUES (@owner5_user_id, @c4_user_id, @b4_id, @h5_id, N'Cảm ơn bạn rất nhiều! Chúng tôi rất vui khi bạn có trải nghiệm tuyệt vời. Hẹn gặp lại tại Phú Quốc!', 1, DATEADD(DAY, -5, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @c10_user_id AND receiver_id = @owner10_user_id AND booking_id = @b10_id)
            INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
            VALUES (@c10_user_id, @owner10_user_id, @b10_id, @h10_id, N'Chào resort, mình sẽ đến vào ngày mai. Cho mình hỏi có dịch vụ đón sân bay về resort không?', 0, DATEADD(HOUR, -2, GETDATE()));

        IF NOT EXISTS (SELECT 1 FROM messages WHERE sender_id = @owner10_user_id AND receiver_id = @c10_user_id AND booking_id = @b10_id)
            INSERT INTO messages (sender_id, receiver_id, booking_id, hotel_id, content, is_read, sent_at)
            VALUES (@owner10_user_id, @c10_user_id, @b10_id, @h10_id, N'Chào bạn Loan! Victoria Can Tho có dịch vụ đón bằng thuyền từ bến Ninh Kiều, rất thú vị. Bạn vui lòng cung cấp giờ đến để chúng tôi sắp xếp nhé!', 0, DATEADD(HOUR, -1, GETDATE()));

        -- =====================================================
        -- 13. CREATE NEW HOTEL OWNER WITH 7 LUXURY HOTELS
        -- =====================================================

        PRINT N'📌 Creating new Hotel Owner with 7 luxury hotels (FULL ROOM DETAILS)...';

        -- Create NEW Owner
        IF NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = N'owner_luxury_hotels')
        BEGIN
            INSERT INTO user_accounts (email, username, password, role, enabled)
            VALUES (N'owner.luxury@hotel.test', N'owner_luxury_hotels', @passwordHash, N'HOTEL_OWNER', 1);
            SELECT @new_owner_user_id = SCOPE_IDENTITY();

            INSERT INTO hotel_owners (user_account_id, full_name, phone, address, id_card, tax_id, verification_status, verified_at, bank_name, bank_account_number, bank_account_holder)
            VALUES (@new_owner_user_id, N'Trần Đức Minh', N'+84918889999', N'123 Phố Huế, Quận Hai Bà Trưng, Hà Nội',
                    N'012345678999', N'TAX-LUX-999', N'APPROVED', GETDATE(),
                    N'Vietcombank', N'9999999999', N'TRAN DUC MINH');
            SELECT @new_owner_id = SCOPE_IDENTITY();
            PRINT N'✅ Created new Hotel Owner: Trần Đức Minh (ID: ' + CAST(@new_owner_id AS NVARCHAR) + N')';
        END
        ELSE
        BEGIN
            SELECT @new_owner_user_id = id FROM user_accounts WHERE username = N'owner_luxury_hotels';
            SELECT @new_owner_id = id FROM hotel_owners WHERE user_account_id = @new_owner_user_id;
            PRINT N'⚠️ Owner already exists. Using existing Owner ID: ' + CAST(@new_owner_id AS NVARCHAR);
        END

        -- =====================================================
        -- 14. CREATE 7 HOTELS FOR THE NEW OWNER
        -- =====================================================

        -- LUX1: Luxury Palace Hanoi
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Luxury Palace Hanoi')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Luxury Palace Hanoi', N'12 Lý Thường Kiệt, Hoàn Kiếm', N'Hà Nội', N'Hoàn Kiếm',
                N'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&h=500&fit=crop&q=80',
                N'Khách sạn 5 sao sang trọng bậc nhất Hà Nội, tọa lạc tại trung tâm phố cổ. Kiến trúc Pháp cổ điển kết hợp hiện đại, với tầm nhìn toàn cảnh Hồ Gươm và thành phố.',
                4.8, 0, N'APPROVED', 1, GETDATE());

        -- LUX2: Ocean Paradise Da Nang
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Ocean Paradise Da Nang')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Ocean Paradise Da Nang', N'45 Võ Nguyên Giáp, Sơn Trà', N'Đà Nẵng', N'Sơn Trà',
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=500&fit=crop&q=80',
                N'Khu nghỉ dưỡng biển 5 sao nằm trên bãi biển Mỹ Khê tuyệt đẹp, chỉ cách trung tâm thành phố 5 phút. Resort có bãi biển riêng, hồ bơi vô cực, nhà hàng hải sản tươi sống.',
                4.7, 0, N'APPROVED', 1, GETDATE());

        -- LUX3: Bay View Resort Nha Trang
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Bay View Resort Nha Trang')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Bay View Resort Nha Trang', N'78 Trần Phú, Nha Trang', N'Nha Trang', N'Vĩnh Nguyên',
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&h=500&fit=crop&q=80',
                N'Resort 5 sao sang trọng nằm dọc bãi biển Nha Trang, view vịnh Nha Trang tuyệt đẹp. Kiến trúc Địa Trung Hải với những khu vườn nhiệt đới xanh mát.',
                4.9, 0, N'APPROVED', 1, GETDATE());

        -- LUX4: Mountain Retreat Sapa
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Mountain Retreat Sapa')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Mountain Retreat Sapa', N'22 Fansipan, TT Sa Pa', N'Sapa', N'TT Sa Pa',
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800&h=500&fit=crop&q=80',
                N'Khu nghỉ dưỡng trên núi 5 sao với kiến trúc Bản Địa độc đáo, nằm giữa thung lũng Mường Hoa. Tất cả phòng đều có view hướng thẳng Fansipan - nóc nhà Đông Dương.',
                4.6, 0, N'APPROVED', 1, GETDATE());

        -- LUX5: Emerald Island Phu Quoc
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Emerald Island Phu Quoc')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Emerald Island Phu Quoc', N'Bãi Dài, Xã Gành Dầu', N'Phú Quốc', N'Gành Dầu',
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=800&h=500&fit=crop&q=80',
                N'Resort 5 sao trên đảo ngọc Phú Quốc với bãi biển riêng hoang sơ. Thiết kế hiện đại tối giản, mỗi phòng đều có tầm nhìn ra biển và khu vườn nhiệt đới.',
                4.9, 0, N'APPROVED', 1, GETDATE());

        -- LUX6: Riverside Heritage Hoi An
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Riverside Heritage Hoi An')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Riverside Heritage Hoi An', N'34 Nguyễn Trường Tộ, Hội An', N'Hội An', N'Minh An',
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=500&fit=crop&q=80',
                N'Khách sạn boutique 5 sao nằm ven sông Hoài, cách phố cổ Hội An 3 phút đi bộ. Kiến trúc cổ truyền Việt Nam pha trộn phong cách Pháp, với khu vườn xanh mát và hồ bơi nhiệt đới.',
                4.5, 0, N'APPROVED', 1, GETDATE());

        -- LUX7: Pine Hill Da Lat
        IF NOT EXISTS (SELECT 1 FROM hotel WHERE owner_id = @new_owner_id AND name = N'Pine Hill Da Lat')
            INSERT INTO hotel (owner_id, name, address, city, district, image_url, description, rating, total_feedbacks, approval_status, active, approved_at)
            VALUES (@new_owner_id, N'Pine Hill Da Lat', N'56 Đặng Thái Thân, Phường 3', N'Đà Lạt', N'Phường 3',
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=500&fit=crop&q=80',
                N'Resort 5 sao trên đồi thông Đà Lạt, tầm nhìn bao quát thành phố ngàn hoa. Không gian Châu Âu cổ kính với fireplace, thư viện, và khu vườn hoa hồng.',
                4.4, 0, N'APPROVED', 1, GETDATE());

        -- Get Hotel IDs for new hotels
        SELECT @h_lux1_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Luxury Palace Hanoi';
        SELECT @h_lux2_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Ocean Paradise Da Nang';
        SELECT @h_lux3_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Bay View Resort Nha Trang';
        SELECT @h_lux4_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Mountain Retreat Sapa';
        SELECT @h_lux5_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Emerald Island Phu Quoc';
        SELECT @h_lux6_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Riverside Heritage Hoi An';
        SELECT @h_lux7_id = id FROM hotel WHERE owner_id = @new_owner_id AND name = N'Pine Hill Da Lat';

        -- =====================================================
        -- 15. HOTEL FACILITIES & VIEWS FOR NEW HOTELS
        -- =====================================================

        -- LUX1
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux1_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux1_id, 1,1,1,1,1,1,1,1,1,1,1,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux1_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux1_id, 1,0,1,1,0,0);

        -- LUX2
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux2_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux2_id, 1,1,1,1,1,1,1,1,0,1,1,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux2_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux2_id, 0,1,1,1,0,0);

        -- LUX3
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux3_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux3_id, 1,1,1,1,1,1,1,1,0,1,1,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux3_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux3_id, 0,1,1,1,0,0);

        -- LUX4
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux4_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux4_id, 1,1,1,0,1,0,1,1,0,1,0,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux4_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux4_id, 0,0,1,0,0,1);

        -- LUX5
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux5_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux5_id, 1,1,1,1,1,1,1,1,0,1,1,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux5_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux5_id, 0,1,1,1,0,0);

        -- LUX6
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux6_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux6_id, 0,1,1,0,1,0,1,1,0,1,0,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux6_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux6_id, 1,0,1,1,1,0);

        -- LUX7
        IF NOT EXISTS (SELECT 1 FROM hotel_facilities WHERE hotel_id = @h_lux7_id)
            INSERT INTO hotel_facilities (hotel_id, parking, restaurant, breakfast_available, fitness_centre, non_smoking_rooms, airport_shuttle, spa_wellness_centre, free_wifi, ev_charging_station, wheelchair_accessible, swimming_pool, bar_pub)
            VALUES (@h_lux7_id, 1,1,1,0,1,0,1,1,0,1,0,1);
        IF NOT EXISTS (SELECT 1 FROM hotel_views WHERE hotel_id = @h_lux7_id)
            INSERT INTO hotel_views (hotel_id, city_view, beach_view, garden_view, pool_view, river_view, mountain_view)
            VALUES (@h_lux7_id, 1,0,1,0,0,1);

        -- =====================================================
        -- 16. HOTEL VERIFICATION DOCUMENTS FOR NEW HOTELS
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux1_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux1_id, N'+842488880001', N'luxury.palace@hotel.com', N'/docs/luxury1/business.pdf', N'/docs/luxury1/land.pdf', N'/docs/luxury1/contract.pdf', N'APPROVED', GETDATE());

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux2_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux2_id, N'+842366880002', N'ocean.paradise@hotel.com', N'/docs/luxury2/business.pdf', N'/docs/luxury2/land.pdf', N'/docs/luxury2/contract.pdf', N'APPROVED', GETDATE());

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux3_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux3_id, N'+842588880003', N'bay.view.nhatrang@hotel.com', N'/docs/luxury3/business.pdf', N'/docs/luxury3/land.pdf', N'/docs/luxury3/contract.pdf', N'APPROVED', GETDATE());

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux4_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux4_id, N'+842144880004', N'mountain.retreat@hotel.com', N'/docs/luxury4/business.pdf', N'/docs/luxury4/land.pdf', N'/docs/luxury4/contract.pdf', N'APPROVED', GETDATE());

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux5_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux5_id, N'+842977880005', N'emerald.island@hotel.com', N'/docs/luxury5/business.pdf', N'/docs/luxury5/land.pdf', N'/docs/luxury5/contract.pdf', N'APPROVED', GETDATE());

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux6_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux6_id, N'+842355880006', N'riverside.heritage@hotel.com', N'/docs/luxury6/business.pdf', N'/docs/luxury6/land.pdf', N'/docs/luxury6/contract.pdf', N'APPROVED', GETDATE());

        IF NOT EXISTS (SELECT 1 FROM hotel_verification_documents WHERE hotel_id = @h_lux7_id)
            INSERT INTO hotel_verification_documents (hotel_id, phone, email, business_registration_doc, land_certificate_doc, rental_contract_doc, upload_status, verified_at)
            VALUES (@h_lux7_id, N'+842633880007', N'pine.hill.dalat@hotel.com', N'/docs/luxury7/business.pdf', N'/docs/luxury7/land.pdf', N'/docs/luxury7/contract.pdf', N'APPROVED', GETDATE());

        -- =====================================================
        -- 17. CREATE ROOMS FOR NEW HOTELS WITH FULL DETAILS
        -- =====================================================
        PRINT N'📌 Creating rooms for new hotels with full details...';

        -- =====================================================
        -- LUX1: Luxury Palace Hanoi (6 room types)
        -- =====================================================

        -- Room 1: Executive King Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux1_id AND room_type = N'Executive King Suite')
        BEGIN
            DECLARE @r_lux1_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux1_id, N'Executive King Suite', 8, N'Suite sang trọng với giường King size, phòng khách riêng biệt, bàn làm việc và ban công rộng. View toàn cảnh Hồ Gươm và thành phố Hà Nội.', 7500000, 55, 1, 2, 3,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux1_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux1_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        -- Room 2: Deluxe Twin Room
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux1_id AND room_type = N'Deluxe Twin Room')
        BEGIN
            DECLARE @r_lux1_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux1_id, N'Deluxe Twin Room', 15, N'Phòng Deluxe với 2 giường đơn tiện nghi, view thành phố. Thiết kế sang trọng, không gian rộng rãi, thích hợp cho bạn bè hoặc đồng nghiệp.', 4500000, 40, 2, 2, 2,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux1_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux1_2, 1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2);
        END

        -- Room 3: Presidential Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux1_id AND room_type = N'Presidential Suite')
        BEGIN
            DECLARE @r_lux1_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux1_id, N'Presidential Suite', 2, N'Suite tổng thống đẳng cấp nhất khách sạn với phòng khách, phòng ăn, phòng làm việc và phòng ngủ riêng. Nội thất xa hoa, tầm nhìn toàn cảnh Hà Nội 360 độ.', 18500000, 120, 1, 3, 5,
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux1_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux1_3, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,6);
        END

        -- Room 4: Family Connecting Room
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux1_id AND room_type = N'Family Connecting Room')
        BEGIN
            DECLARE @r_lux1_4 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux1_id, N'Family Connecting Room', 5, N'Phòng gia đình với 2 phòng ngủ được kết nối, thích hợp cho gia đình 4-5 người. Gồm 1 giường King và 2 giường đơn, phòng khách riêng và 2 phòng tắm.', 9500000, 75, 3, 5, 4,
                N'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux1_4 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux1_4, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        -- Room 5: Superior King Room
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux1_id AND room_type = N'Superior King Room')
        BEGIN
            DECLARE @r_lux1_5 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux1_id, N'Superior King Room', 20, N'Phòng Superior với giường King, không gian ấm cúng và view thành phố. Phù hợp cho du khách kinh doanh và du lịch.', 3500000, 32, 1, 2, 2,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux1_5 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux1_5, 1,1,0,1,1,0,1,1,1,1,1,1,1,0,1,1,0,1,0,2);
        END

        -- Room 6: Executive Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux1_id AND room_type = N'Executive Suite')
        BEGIN
            DECLARE @r_lux1_6 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux1_id, N'Executive Suite', 6, N'Suite cao cấp với không gian riêng tư, phòng khách hiện đại và quyền sử dụng Executive Lounge. View thành phố từ tầng cao, bồn tắm massage và tiện nghi 5 sao.', 8800000, 65, 1, 2, 3,
                N'https://images.unsplash.com/photo-1560347876-aeef00ee58a1?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux1_6 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux1_6, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        PRINT N'✅ LUX1: 6 room types added with full facilities';

        -- =====================================================
        -- LUX2: Ocean Paradise Da Nang (5 room types)
        -- =====================================================

        -- Room 1: Ocean View King
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux2_id AND room_type = N'Ocean View King')
        BEGIN
            DECLARE @r_lux2_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux2_id, N'Ocean View King', 25, N'Phòng King với tầm nhìn biển toàn cảnh Mỹ Khê. Ban công riêng, giường King cao cấp, nội thất hiện đại và phòng tắm sang trọng.', 5200000, 42, 1, 2, 2,
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux2_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux2_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2);
        END

        -- Room 2: Beachfront Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux2_id AND room_type = N'Beachfront Suite')
        BEGIN
            DECLARE @r_lux2_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux2_id, N'Beachfront Suite', 8, N'Suite mặt biển với không gian rộng rãi, phòng khách riêng, và ban công lớn hướng biển. Đi bộ vài bước ra bãi biển.', 12500000, 72, 1, 2, 3,
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux2_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux2_2, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        -- Room 3: Family Ocean Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux2_id AND room_type = N'Family Ocean Suite')
        BEGIN
            DECLARE @r_lux2_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux2_id, N'Family Ocean Suite', 6, N'Suite gia đình 2 phòng ngủ, view biển và hồ bơi. Phòng khách rộng rãi, 2 phòng tắm và khu vực ăn uống. Phù hợp cho gia đình 4-6 người.', 9800000, 85, 2, 4, 4,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux2_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux2_3, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        -- Room 4: Deluxe Double Room
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux2_id AND room_type = N'Deluxe Double Room')
        BEGIN
            DECLARE @r_lux2_4 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux2_id, N'Deluxe Double Room', 30, N'Phòng Deluxe với giường đôi cao cấp, view biển. Không gian ấm cúng, thiết kế hiện đại, phòng tắm kính sang trọng.', 3800000, 35, 1, 2, 1,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux2_4 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux2_4, 1,1,0,1,1,0,1,1,1,1,1,1,1,0,1,1,0,1,0,2);
        END

        -- Room 5: Penthouse Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux2_id AND room_type = N'Penthouse Suite')
        BEGIN
            DECLARE @r_lux2_5 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux2_id, N'Penthouse Suite', 2, N'Suite tầng thượng xa hoa với hồ bơi riêng, sân thượng BBQ và view 360 độ biển và thành phố. Phòng khách sang trọng, bếp hiện đại, 3 phòng ngủ và nhân viên phục vụ riêng.', 28000000, 200, 3, 6, 6,
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux2_5 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux2_5, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,6);
        END

        PRINT N'✅ LUX2: 5 room types added with full facilities';

        -- =====================================================
        -- LUX3: Bay View Resort Nha Trang (4 room types)
        -- =====================================================

        -- Room 1: Bay View Deluxe
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux3_id AND room_type = N'Bay View Deluxe')
        BEGIN
            DECLARE @r_lux3_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux3_id, N'Bay View Deluxe', 20, N'Phòng Deluxe với view vịnh Nha Trang tuyệt đẹp. Ban công riêng, giường cao cấp, phòng tắm hiện đại. Nội thất sang trọng, không gian thoáng đãng.', 4800000, 40, 1, 2, 2,
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux3_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux3_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2);
        END

        -- Room 2: Oceanfront Villa
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux3_id AND room_type = N'Oceanfront Villa')
        BEGIN
            DECLARE @r_lux3_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux3_id, N'Oceanfront Villa', 5, N'Villa biển sang trọng với 2 phòng ngủ, hồ bơi riêng và khu vườn nhiệt đới. View biển trực diện, phòng khách mở, bếp đầy đủ.', 19000000, 150, 2, 4, 4,
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux3_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux3_2, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,6);
        END

        -- Room 3: Superior Twin
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux3_id AND room_type = N'Superior Twin')
        BEGIN
            DECLARE @r_lux3_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux3_id, N'Superior Twin', 15, N'Phòng Superior với 2 giường đơn tiện nghi, view thành phố và vịnh. Không gian thoải mái, đầy đủ tiện nghi cần thiết.', 3200000, 35, 2, 2, 2,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux3_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux3_3, 1,1,0,1,1,0,1,1,1,1,1,1,1,0,1,1,0,1,0,2);
        END

        -- Room 4: Romantic Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux3_id AND room_type = N'Romantic Suite')
        BEGIN
            DECLARE @r_lux3_4 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux3_id, N'Romantic Suite', 4, N'Suite lãng mạn với view hoàng hôn tuyệt đẹp, bồn tắm massage đôi, và dịch vụ trải nghiệm riêng tư. Trang trí hoa tươi, rượu vang và chocolate.', 8800000, 55, 1, 2, 3,
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux3_4 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux3_4, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        PRINT N'✅ LUX3: 4 room types added with full facilities';

        -- =====================================================
        -- LUX4: Mountain Retreat Sapa (3 room types)
        -- =====================================================

        -- Room 1: Mountain View Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux4_id AND room_type = N'Mountain View Suite')
        BEGIN
            DECLARE @r_lux4_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux4_id, N'Mountain View Suite', 12, N'Suite với tầm nhìn trực diện núi Fansipan hùng vĩ. Phòng khách ấm cúng với fireplace, giường King thoải mái, ban công rộng.', 4800000, 50, 1, 2, 3,
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux4_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux4_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,1,1,2);
        END

        -- Room 2: Cozy Mountain Room
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux4_id AND room_type = N'Cozy Mountain Room')
        BEGIN
            DECLARE @r_lux4_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux4_id, N'Cozy Mountain Room', 18, N'Phòng ấm cúng với view thung lũng và núi non. Trang trí bản địa, ấm áp với thảm len và nội thất gỗ.', 3200000, 35, 1, 2, 2,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux4_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux4_2, 1,1,0,1,1,0,1,1,1,0,1,1,1,0,1,0,0,1,0,2);
        END

        -- Room 3: Family Mountain Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux4_id AND room_type = N'Family Mountain Suite')
        BEGIN
            DECLARE @r_lux4_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux4_id, N'Family Mountain Suite', 5, N'Suite gia đình 2 phòng ngủ với view núi và vườn. Phòng khách rộng, fireplace ấm cúng. Phù hợp cho gia đình 4-6 người.', 7800000, 80, 2, 4, 4,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux4_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux4_3, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        PRINT N'✅ LUX4: 3 room types added with full facilities';

        -- =====================================================
        -- LUX5: Emerald Island Phu Quoc (3 room types)
        -- =====================================================

        -- Room 1: Emerald Beachfront Villa
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux5_id AND room_type = N'Emerald Beachfront Villa')
        BEGIN
            DECLARE @r_lux5_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux5_id, N'Emerald Beachfront Villa', 4, N'Villa mặt biển sang trọng với hồ bơi riêng, khu vườn rộng và lối đi riêng ra bãi biển. 3 phòng ngủ, phòng khách lớn và bếp hiện đại.', 25000000, 200, 3, 6, 5,
                N'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux5_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux5_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,6);
        END

        -- Room 2: Ocean View Deluxe
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux5_id AND room_type = N'Ocean View Deluxe')
        BEGIN
            DECLARE @r_lux5_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux5_id, N'Ocean View Deluxe', 25, N'Phòng Deluxe với tầm nhìn biển xanh ngọc. Ban công rộng, giường King cao cấp, phòng tắm hiện đại. Nội thất tối giản sang trọng.', 5500000, 45, 1, 2, 2,
                N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux5_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux5_2, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2);
        END

        -- Room 3: Sunset Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux5_id AND room_type = N'Sunset Suite')
        BEGIN
            DECLARE @r_lux5_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux5_id, N'Sunset Suite', 6, N'Suite với tầm nhìn hoàng hôn tuyệt đẹp. Ban công lớn hướng Tây, phòng khách rộng, giường King. Trang trí ấm áp với tông màu cam và vàng.', 9200000, 60, 1, 2, 3,
                N'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux5_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux5_3, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        PRINT N'✅ LUX5: 3 room types added with full facilities';

        -- =====================================================
        -- LUX6: Riverside Heritage Hoi An (3 room types)
        -- =====================================================

        -- Room 1: Heritage Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux6_id AND room_type = N'Heritage Suite')
        BEGIN
            DECLARE @r_lux6_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux6_id, N'Heritage Suite', 8, N'Suite cổ điển phong cách Pháp thuộc với view sông Hoài. Nội thất gỗ quý, giường cao cấp, ban công rộng. Kết hợp hài hòa giữa kiến trúc cổ xưa và tiện nghi hiện đại.', 5200000, 48, 1, 2, 3,
                N'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux6_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux6_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,1,1,2);
        END

        -- Room 2: Garden View Room
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux6_id AND room_type = N'Garden View Room')
        BEGIN
            DECLARE @r_lux6_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux6_id, N'Garden View Room', 15, N'Phòng với tầm nhìn khu vườn nhiệt đới xanh mát. Ban công nhỏ, giường đôi thoải mái, nội thất gỗ bản địa. Không gian yên tĩnh, thư giãn.', 3500000, 38, 1, 2, 2,
                N'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux6_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux6_2, 1,1,0,1,1,0,1,1,1,0,1,1,1,0,1,0,0,1,0,2);
        END

        -- Room 3: Riverside Villa
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux6_id AND room_type = N'Riverside Villa')
        BEGIN
            DECLARE @r_lux6_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux6_id, N'Riverside Villa', 3, N'Villa ven sông với 2 phòng ngủ, khu vườn riêng và hồ bơi nhỏ. View sông Hoài lãng mạn, phòng khách rộng và bếp đầy đủ.', 14500000, 130, 2, 4, 4,
                N'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux6_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux6_3, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        PRINT N'✅ LUX6: 3 room types added with full facilities';

        -- =====================================================
        -- LUX7: Pine Hill Da Lat (3 room types)
        -- =====================================================

        -- Room 1: Deluxe Pine View
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux7_id AND room_type = N'Deluxe Pine View')
        BEGIN
            DECLARE @r_lux7_1 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux7_id, N'Deluxe Pine View', 20, N'Phòng Deluxe với tầm nhìn rừng thông bạt ngàn và thành phố Đà Lạt. Fireplace ấm cúng, giường King êm ái, ban công hướng đồi thông.', 4200000, 42, 1, 2, 2,
                N'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux7_1 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux7_1, 1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,1,1,2);
        END

        -- Room 2: Family Pine Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux7_id AND room_type = N'Family Pine Suite')
        BEGIN
            DECLARE @r_lux7_2 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux7_id, N'Family Pine Suite', 5, N'Suite gia đình 2 phòng ngủ với view đồi thông và thành phố. Phòng khách ấm cúng với fireplace, bếp nhỏ và khu vực ăn uống.', 8500000, 85, 2, 4, 4,
                N'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux7_2 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux7_2, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        -- Room 3: Pine Hill Suite
        IF NOT EXISTS (SELECT 1 FROM room WHERE hotel_id = @h_lux7_id AND room_type = N'Pine Hill Suite')
        BEGIN
            DECLARE @r_lux7_3 INT;
            INSERT INTO room (hotel_id, room_type, number_rooms, description, price, acreage, bed, person, num_window, img_url, room_status)
            VALUES (@h_lux7_id, N'Pine Hill Suite', 4, N'Suite sang trọng với tầm nhìn toàn cảnh thành phố Đà Lạt và rừng thông. Phòng khách rộng, fireplace, bồn tắm massage với view núi.', 9800000, 72, 1, 2, 3,
                N'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&h=400&fit=crop&q=80', 1);
            SELECT @r_lux7_3 = SCOPE_IDENTITY();
            INSERT INTO room_facilities (room_id, free_toiletries, shower, bathrobe, toilet, towels, slippers, hairdryer, toilet_paper, air_conditioning, safety_deposit_box, desk, television, telephone, iron, electric_kettle, cable_channels, wake_up_service, wardrobe_closet, clothes_rack, free_bottled_water)
            VALUES (@r_lux7_3, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4);
        END

        PRINT N'✅ LUX7: 3 room types added with full facilities';

        -- =====================================================
        -- 18. PROMOTIONS FOR NEW HOTELS
        -- =====================================================
        IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h_lux1_id AND title = N'Luxury Palace Grand Opening')
            INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
            VALUES (@h_lux1_id, N'Luxury Palace Grand Opening', N'Ưu đãi 20% cho tất cả các loại phòng trong tháng đầu tiên khai trương. Áp dụng cho đặt phòng từ 2 đêm trở lên.', 20.00, '2026-07-01', '2026-07-31', N'ACTIVE');

        IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h_lux2_id AND title = N'Ocean Paradise Summer Escape')
            INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
            VALUES (@h_lux2_id, N'Ocean Paradise Summer Escape', N'Giảm 15% cho tất cả phòng Ocean View và Beachfront Suite trong dịp hè 2026. Kèm voucher spa 500.000 VND.', 15.00, '2026-06-01', '2026-08-31', N'ACTIVE');

        IF NOT EXISTS (SELECT 1 FROM promotions WHERE hotel_id = @h_lux5_id AND title = N'Emerald Island Early Bird')
            INSERT INTO promotions (hotel_id, title, description, discount_percent, start_date, end_date, status)
            VALUES (@h_lux5_id, N'Emerald Island Early Bird', N'Đặt phòng trước 30 ngày giảm 25% cho tất cả Villa và Suite. Kèm bữa sáng miễn phí cho 2 người.', 25.00, '2026-07-01', '2026-12-31', N'ACTIVE');

        PRINT N'';
        PRINT N'========================================';
        PRINT N'✅ ALL DATA INSERTED SUCCESSFULLY!';
        PRINT N'========================================';
        PRINT N'';
        PRINT N'📊 SUMMARY:';
        PRINT N'   - 10 existing hotels (8 APPROVED, 1 PENDING, 1 REJECTED)';
        PRINT N'   - 7 new luxury hotels (ALL APPROVED)';
        PRINT N'   - All passwords set to "12345678"';
        PRINT N'   - New Hotel Owner: Trần Đức Minh (owner_luxury_hotels)';
        PRINT N'   - Total room types: 29 existing + 27 new = 56 room types';
        PRINT N'   - All new rooms have FULL facilities (bathroom + room amenities)';
        PRINT N'   - 12 Bookings, 12 Payments, 3 Refunds, 7 Feedbacks';
        PRINT N'';
        PRINT N'🔑 LOGIN CREDENTIALS (ALL PASSWORD = "12345678"):';
        PRINT N'   - Admin: admin1 / 12345678';
        PRINT N'   - Admin: admin2 / 12345678';
        PRINT N'   - New Owner: owner_luxury_hotels / 12345678';
        PRINT N'   - All existing accounts: password = 12345678';

        COMMIT TRANSACTION;

        PRINT N'✅ Transaction committed successfully!';

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();

        PRINT N'❌ ERROR: ' + @ErrorMessage;
        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH;
    GO