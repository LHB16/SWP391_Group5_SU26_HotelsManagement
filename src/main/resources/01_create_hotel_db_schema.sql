-- =====================================================
-- HOTEL BOOKING SYSTEM - FULL DATABASE SCRIPT
-- File: 01_create_hotel_db_schema.sql
-- Target: Microsoft SQL Server / SSMS
-- Description: Drop old hotel_db database if exists, then create full schema
-- =====================================================

USE master;
GO

IF DB_ID(N'hotel_db') IS NOT NULL
BEGIN
    ALTER DATABASE hotel_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE hotel_db;
END
GO

CREATE DATABASE hotel_db;
GO

USE hotel_db;
GO

-- =====================================================
-- 1. USERS & AUTHENTICATION
-- =====================================================
CREATE TABLE user_accounts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL UNIQUE,
    username NVARCHAR(255) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    role NVARCHAR(50) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT DF_user_accounts_enabled DEFAULT 1,
    otp NVARCHAR(255) NULL,
    otp_expiry DATETIME2 NULL,
    otp_type NVARCHAR(50) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_user_accounts_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_user_accounts_updated_at DEFAULT GETDATE(),
    CONSTRAINT CK_user_accounts_role CHECK (role IN (N'CUSTOMER', N'HOTEL_OWNER', N'ADMIN')),
    CONSTRAINT CK_user_accounts_otp_type CHECK (otp_type IS NULL OR otp_type IN (N'REGISTER', N'FORGOT_PASSWORD', N'LOGIN', N'UPDATE_PROFILE'))
);
GO

CREATE TABLE admins (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_account_id INT NOT NULL UNIQUE,
    full_name NVARCHAR(255) NOT NULL,
    phone NVARCHAR(20) NULL
);
GO

CREATE TABLE customers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_account_id INT NOT NULL UNIQUE,
    full_name NVARCHAR(255) NOT NULL,
    phone NVARCHAR(20) NULL,
    address NVARCHAR(500) NULL,
    city NVARCHAR(100) NULL,
    country NVARCHAR(100) NULL,
    date_of_birth DATE NULL,
    gender NVARCHAR(10) NULL,
    CONSTRAINT CK_customers_gender CHECK (gender IS NULL OR gender IN (N'MALE', N'FEMALE', N'OTHER'))
);
GO

CREATE TABLE hotel_owners (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_account_id INT NOT NULL UNIQUE,
    full_name NVARCHAR(255) NOT NULL,
    phone NVARCHAR(20) NULL,
    address NVARCHAR(500) NULL,
    id_card NVARCHAR(20) NULL,
    tax_id NVARCHAR(20) NULL,
    verification_status NVARCHAR(50) NULL,
    verified_at DATETIME2 NULL,
    rejection_reason NVARCHAR(MAX) NULL,
    id_card_document NVARCHAR(500) NULL,
    CONSTRAINT CK_hotel_owners_verification_status CHECK (
        verification_status IS NULL OR verification_status IN (N'PENDING', N'APPROVED', N'REJECTED')
    )
);
GO

-- =====================================================
-- 2. HOTEL MANAGEMENT
-- =====================================================
CREATE TABLE hotel (
    id INT IDENTITY(1,1) PRIMARY KEY,
    owner_id INT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    city NVARCHAR(100) NULL,
    district NVARCHAR(100) NULL,
    image_url NVARCHAR(500) NULL,
    description NVARCHAR(MAX) NULL,
    rating FLOAT NOT NULL CONSTRAINT DF_hotel_rating DEFAULT 0,
    total_reviews INT NOT NULL CONSTRAINT DF_hotel_total_reviews DEFAULT 0,
    approval_status NVARCHAR(50) NULL,
    active BIT NOT NULL CONSTRAINT DF_hotel_active DEFAULT 1,
    approved_at DATETIME2 NULL,
    rejection_reason NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_hotel_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_hotel_updated_at DEFAULT GETDATE(),
    CONSTRAINT CK_hotel_rating CHECK (rating >= 0 AND rating <= 5),
    CONSTRAINT CK_hotel_total_reviews CHECK (total_reviews >= 0),
    CONSTRAINT CK_hotel_approval_status CHECK (
        approval_status IS NULL OR approval_status IN (N'PENDING', N'APPROVED', N'REJECTED')
    )
);
GO

CREATE TABLE hotel_verification_documents (
    id INT IDENTITY(1,1) PRIMARY KEY,
    hotel_id INT NOT NULL UNIQUE,
    phone NVARCHAR(20) NULL,
    email NVARCHAR(255) NULL,
    business_registration_doc NVARCHAR(500) NULL,
    land_certificate_doc NVARCHAR(500) NULL,
    rental_contract_doc NVARCHAR(500) NULL,
    upload_status NVARCHAR(50) NULL,
    rejection_reason NVARCHAR(MAX) NULL,
    verified_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_hotel_verification_documents_created_at DEFAULT GETDATE(),
    CONSTRAINT CK_hotel_verification_documents_upload_status CHECK (
        upload_status IS NULL OR upload_status IN (N'PENDING', N'APPROVED', N'REJECTED')
    )
);
GO

CREATE TABLE hotel_facilities (
    id INT IDENTITY(1,1) PRIMARY KEY,
    hotel_id INT NOT NULL UNIQUE,
    parking BIT NOT NULL CONSTRAINT DF_hotel_facilities_parking DEFAULT 0,
    restaurant BIT NOT NULL CONSTRAINT DF_hotel_facilities_restaurant DEFAULT 0,
    breakfast_available BIT NOT NULL CONSTRAINT DF_hotel_facilities_breakfast_available DEFAULT 0,
    fitness_centre BIT NOT NULL CONSTRAINT DF_hotel_facilities_fitness_centre DEFAULT 0,
    non_smoking_rooms BIT NOT NULL CONSTRAINT DF_hotel_facilities_non_smoking_rooms DEFAULT 0,
    airport_shuttle BIT NOT NULL CONSTRAINT DF_hotel_facilities_airport_shuttle DEFAULT 0,
    spa_wellness_centre BIT NOT NULL CONSTRAINT DF_hotel_facilities_spa_wellness_centre DEFAULT 0,
    free_wifi BIT NOT NULL CONSTRAINT DF_hotel_facilities_free_wifi DEFAULT 0,
    ev_charging_station BIT NOT NULL CONSTRAINT DF_hotel_facilities_ev_charging_station DEFAULT 0,
    wheelchair_accessible BIT NOT NULL CONSTRAINT DF_hotel_facilities_wheelchair_accessible DEFAULT 0,
    swimming_pool BIT NOT NULL CONSTRAINT DF_hotel_facilities_swimming_pool DEFAULT 0,
    bar_pub BIT NOT NULL CONSTRAINT DF_hotel_facilities_bar_pub DEFAULT 0,
    rent_vehicle BIT NOT NULL CONSTRAINT DF_hotel_facilities_rent_vehicle DEFAULT 0
);
GO

CREATE TABLE hotel_views (
    id INT IDENTITY(1,1) PRIMARY KEY,
    hotel_id INT NOT NULL UNIQUE,
    city_view BIT NOT NULL CONSTRAINT DF_hotel_views_city_view DEFAULT 0,
    beach_view BIT NOT NULL CONSTRAINT DF_hotel_views_beach_view DEFAULT 0,
    garden_view BIT NOT NULL CONSTRAINT DF_hotel_views_garden_view DEFAULT 0,
    pool_view BIT NOT NULL CONSTRAINT DF_hotel_views_pool_view DEFAULT 0,
    river_view BIT NOT NULL CONSTRAINT DF_hotel_views_river_view DEFAULT 0,
    mountain_view BIT NOT NULL CONSTRAINT DF_hotel_views_mountain_view DEFAULT 0
);
GO

CREATE TABLE promotions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    hotel_id INT NOT NULL,
    title NVARCHAR(255) NULL,
    description NVARCHAR(MAX) NULL,
    discount_percent DECIMAL(5,2) NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status NVARCHAR(50) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_promotions_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_promotions_updated_at DEFAULT GETDATE(),
    CONSTRAINT CK_promotions_discount_percent CHECK (discount_percent IS NULL OR (discount_percent >= 0 AND discount_percent <= 100)),
    CONSTRAINT CK_promotions_date CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
    CONSTRAINT CK_promotions_status CHECK (status IS NULL OR status IN (N'ACTIVE', N'INACTIVE', N'EXPIRED'))
);
GO

-- =====================================================
-- 3. ROOM MANAGEMENT
-- =====================================================
CREATE TABLE room (
    id INT IDENTITY(1,1) PRIMARY KEY,
    hotel_id INT NOT NULL,
    room_type NVARCHAR(255) NULL,
    number_rooms INT NULL,
    description NVARCHAR(MAX) NULL,
    price DECIMAL(38,2) NULL,
    acreage FLOAT NULL,
    bed INT NULL,
    person INT NULL,
    num_window INT NULL,
    img_url NVARCHAR(500) NULL,
    room_status BIT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_room_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_room_updated_at DEFAULT GETDATE(),
    CONSTRAINT CK_room_number_rooms CHECK (number_rooms IS NULL OR number_rooms >= 0),
    CONSTRAINT CK_room_price CHECK (price IS NULL OR price >= 0),
    CONSTRAINT CK_room_acreage CHECK (acreage IS NULL OR acreage >= 0),
    CONSTRAINT CK_room_bed CHECK (bed IS NULL OR bed >= 0),
    CONSTRAINT CK_room_person CHECK (person IS NULL OR person >= 0),
    CONSTRAINT CK_room_num_window CHECK (num_window IS NULL OR num_window >= 0)
);
GO

CREATE TABLE room_facilities (
    id INT IDENTITY(1,1) PRIMARY KEY,
    room_id INT NOT NULL UNIQUE,
    free_toiletries BIT NOT NULL CONSTRAINT DF_room_facilities_free_toiletries DEFAULT 0,
    shower BIT NOT NULL CONSTRAINT DF_room_facilities_shower DEFAULT 0,
    bathrobe BIT NOT NULL CONSTRAINT DF_room_facilities_bathrobe DEFAULT 0,
    toilet BIT NOT NULL CONSTRAINT DF_room_facilities_toilet DEFAULT 0,
    towels BIT NOT NULL CONSTRAINT DF_room_facilities_towels DEFAULT 0,
    slippers BIT NOT NULL CONSTRAINT DF_room_facilities_slippers DEFAULT 0,
    hairdryer BIT NOT NULL CONSTRAINT DF_room_facilities_hairdryer DEFAULT 0,
    toilet_paper BIT NOT NULL CONSTRAINT DF_room_facilities_toilet_paper DEFAULT 0,
    air_conditioning BIT NOT NULL CONSTRAINT DF_room_facilities_air_conditioning DEFAULT 0,
    safety_deposit_box BIT NOT NULL CONSTRAINT DF_room_facilities_safety_deposit_box DEFAULT 0,
    desk BIT NOT NULL CONSTRAINT DF_room_facilities_desk DEFAULT 0,
    television BIT NOT NULL CONSTRAINT DF_room_facilities_television DEFAULT 0,
    telephone BIT NOT NULL CONSTRAINT DF_room_facilities_telephone DEFAULT 0,
    iron BIT NOT NULL CONSTRAINT DF_room_facilities_iron DEFAULT 0,
    electric_kettle BIT NOT NULL CONSTRAINT DF_room_facilities_electric_kettle DEFAULT 0,
    cable_channels BIT NOT NULL CONSTRAINT DF_room_facilities_cable_channels DEFAULT 0,
    wake_up_service BIT NOT NULL CONSTRAINT DF_room_facilities_wake_up_service DEFAULT 0,
    wardrobe_closet BIT NOT NULL CONSTRAINT DF_room_facilities_wardrobe_closet DEFAULT 0,
    clothes_rack BIT NOT NULL CONSTRAINT DF_room_facilities_clothes_rack DEFAULT 0,
    free_bottled_water INT NOT NULL CONSTRAINT DF_room_facilities_free_bottled_water DEFAULT 0,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_room_facilities_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_room_facilities_updated_at DEFAULT GETDATE(),
    CONSTRAINT CK_room_facilities_free_bottled_water CHECK (free_bottled_water >= 0)
);
GO

-- =====================================================
-- 4. BOOKING, PAYMENT, REFUND
-- =====================================================
CREATE TABLE bookings (
    id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT NOT NULL,
    room_id INT NOT NULL,
    hotel_id INT NOT NULL,
    phone NVARCHAR(20) NULL,
    check_in_date DATE NULL,
    check_out_date DATE NULL,
    num_nights INT NULL,
    total_price DECIMAL(38,2) NULL,
    status NVARCHAR(50) NULL,
    special_notes NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_bookings_created_at DEFAULT GETDATE(),
    CONSTRAINT CK_bookings_num_nights CHECK (num_nights IS NULL OR num_nights > 0),
    CONSTRAINT CK_bookings_total_price CHECK (total_price IS NULL OR total_price >= 0),
    CONSTRAINT CK_bookings_date CHECK (check_in_date IS NULL OR check_out_date IS NULL OR check_out_date > check_in_date),
    CONSTRAINT CK_bookings_status CHECK (
        status IS NULL OR status IN (N'PENDING', N'CONFIRMED', N'COMPLETED', N'CANCELLED', N'REJECTED')
    )
);
GO

CREATE TABLE payments (
    id INT IDENTITY(1,1) PRIMARY KEY,
    booking_id INT NOT NULL,
    amount DECIMAL(38,2) NULL,
    method NVARCHAR(50) NULL,
    status NVARCHAR(50) NULL,
    qr_code_url NVARCHAR(500) NULL,
    transaction_id NVARCHAR(255) NULL,
    paid_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_payments_created_at DEFAULT GETDATE(),
    qr_expires_at DATETIME2 NULL,
    CONSTRAINT CK_payments_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT CK_payments_method CHECK (method IS NULL OR method IN (N'QR_CODE', N'CASH', N'BANK_TRANSFER', N'MOMO', N'VNPAY')),
    CONSTRAINT CK_payments_status CHECK (status IS NULL OR status IN (N'PENDING', N'PAID', N'FAILED', N'EXPIRED', N'REFUNDED'))
);
GO

CREATE TABLE refunds (
    id INT IDENTITY(1,1) PRIMARY KEY,
    booking_id INT NOT NULL UNIQUE,
    bank_name NVARCHAR(255) NULL,
    account_number NVARCHAR(50) NULL,
    account_holder NVARCHAR(255) NULL,
    refund_amount DECIMAL(38,2) NOT NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_refunds_status DEFAULT N'PENDING',
    note NVARCHAR(MAX) NULL,
    requested_at DATETIME2 NOT NULL CONSTRAINT DF_refunds_requested_at DEFAULT GETDATE(),
    processed_at DATETIME2 NULL,
    CONSTRAINT CK_refunds_refund_amount CHECK (refund_amount >= 0),
    CONSTRAINT CK_refunds_status CHECK (status IN (N'PENDING', N'PROCESSED', N'REJECTED'))
);
GO

-- =====================================================
-- 5. WISHLIST, MESSAGE, FEEDBACK
-- =====================================================
CREATE TABLE wishlists (
    id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT NOT NULL,
    room_id INT NOT NULL,
    check_in_date DATE NULL,
    check_out_date DATE NULL,
    added_at DATETIME2 NOT NULL CONSTRAINT DF_wishlists_added_at DEFAULT GETDATE(),
    CONSTRAINT CK_wishlists_date CHECK (check_in_date IS NULL OR check_out_date IS NULL OR check_out_date > check_in_date)
);
GO

CREATE TABLE messages (
    id INT IDENTITY(1,1) PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    booking_id INT NULL,
    content NVARCHAR(MAX) NULL,
    is_read BIT NOT NULL CONSTRAINT DF_messages_is_read DEFAULT 0,
    sent_at DATETIME2 NOT NULL CONSTRAINT DF_messages_sent_at DEFAULT GETDATE()
);
GO

CREATE TABLE feedback (
    id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT NOT NULL,
    hotel_id INT NOT NULL,
    room_id INT NOT NULL,
    user_full_name NVARCHAR(255) NULL,
    room_type NVARCHAR(255) NULL,
    comment NVARCHAR(MAX) NULL,
    rating INT NULL,
    upvote INT NOT NULL CONSTRAINT DF_feedback_upvote DEFAULT 0,
    downvote INT NOT NULL CONSTRAINT DF_feedback_downvote DEFAULT 0,
    status NVARCHAR(50) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_feedback_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_feedback_updated_at DEFAULT GETDATE(),
    CONSTRAINT CK_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT CK_feedback_vote CHECK (upvote >= 0 AND downvote >= 0),
    CONSTRAINT CK_feedback_status CHECK (status IS NULL OR status IN (N'VISIBLE', N'HIDDEN', N'PENDING'))
);
GO

CREATE TABLE feedback_replies (
    id INT IDENTITY(1,1) PRIMARY KEY,
    feedback_id INT NOT NULL,
    owner_id INT NOT NULL,
    hotel_id INT NOT NULL,
    content NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_feedback_replies_created_at DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_feedback_replies_updated_at DEFAULT GETDATE()
);
GO

-- =====================================================
-- 6. FOREIGN KEY RELATIONSHIPS
-- Note: Use NO ACTION to avoid SQL Server multiple cascade paths.
-- =====================================================
ALTER TABLE admins
ADD CONSTRAINT FK_admins_user_accounts
FOREIGN KEY (user_account_id) REFERENCES user_accounts(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE customers
ADD CONSTRAINT FK_customers_user_accounts
FOREIGN KEY (user_account_id) REFERENCES user_accounts(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE hotel_owners
ADD CONSTRAINT FK_hotel_owners_user_accounts
FOREIGN KEY (user_account_id) REFERENCES user_accounts(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE hotel
ADD CONSTRAINT FK_hotel_hotel_owners
FOREIGN KEY (owner_id) REFERENCES hotel_owners(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE hotel_verification_documents
ADD CONSTRAINT FK_hotel_verification_documents_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE hotel_facilities
ADD CONSTRAINT FK_hotel_facilities_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE hotel_views
ADD CONSTRAINT FK_hotel_views_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE promotions
ADD CONSTRAINT FK_promotions_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE room
ADD CONSTRAINT FK_room_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE room_facilities
ADD CONSTRAINT FK_room_facilities_room
FOREIGN KEY (room_id) REFERENCES room(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE bookings
ADD CONSTRAINT FK_bookings_customers
FOREIGN KEY (customer_id) REFERENCES customers(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE bookings
ADD CONSTRAINT FK_bookings_room
FOREIGN KEY (room_id) REFERENCES room(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE bookings
ADD CONSTRAINT FK_bookings_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE payments
ADD CONSTRAINT FK_payments_bookings
FOREIGN KEY (booking_id) REFERENCES bookings(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE refunds
ADD CONSTRAINT FK_refunds_bookings
FOREIGN KEY (booking_id) REFERENCES bookings(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE wishlists
ADD CONSTRAINT FK_wishlists_customers
FOREIGN KEY (customer_id) REFERENCES customers(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE wishlists
ADD CONSTRAINT FK_wishlists_room
FOREIGN KEY (room_id) REFERENCES room(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE messages
ADD CONSTRAINT FK_messages_sender_user_accounts
FOREIGN KEY (sender_id) REFERENCES user_accounts(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE messages
ADD CONSTRAINT FK_messages_receiver_user_accounts
FOREIGN KEY (receiver_id) REFERENCES user_accounts(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE messages
ADD CONSTRAINT FK_messages_bookings
FOREIGN KEY (booking_id) REFERENCES bookings(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE feedback
ADD CONSTRAINT FK_feedback_customers
FOREIGN KEY (customer_id) REFERENCES customers(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE feedback
ADD CONSTRAINT FK_feedback_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE feedback
ADD CONSTRAINT FK_feedback_room
FOREIGN KEY (room_id) REFERENCES room(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE feedback_replies
ADD CONSTRAINT FK_feedback_replies_feedback
FOREIGN KEY (feedback_id) REFERENCES feedback(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE feedback_replies
ADD CONSTRAINT FK_feedback_replies_hotel_owners
FOREIGN KEY (owner_id) REFERENCES hotel_owners(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

ALTER TABLE feedback_replies
ADD CONSTRAINT FK_feedback_replies_hotel
FOREIGN KEY (hotel_id) REFERENCES hotel(id)
ON DELETE NO ACTION ON UPDATE NO ACTION;
GO

-- =====================================================
-- 7. INDEXES FOR SEARCHING / TESTING
-- =====================================================
CREATE INDEX IX_user_accounts_role ON user_accounts(role);
CREATE INDEX IX_hotel_owner_id ON hotel(owner_id);
CREATE INDEX IX_hotel_city ON hotel(city);
CREATE INDEX IX_hotel_approval_status ON hotel(approval_status);
CREATE INDEX IX_room_hotel_id ON room(hotel_id);
CREATE INDEX IX_bookings_customer_id ON bookings(customer_id);
CREATE INDEX IX_bookings_room_id ON bookings(room_id);
CREATE INDEX IX_bookings_hotel_id ON bookings(hotel_id);
CREATE INDEX IX_bookings_status ON bookings(status);
CREATE INDEX IX_payments_booking_id ON payments(booking_id);
CREATE INDEX IX_refunds_booking_id ON refunds(booking_id);
CREATE INDEX IX_feedback_hotel_id ON feedback(hotel_id);
CREATE INDEX IX_messages_sender_receiver ON messages(sender_id, receiver_id);
GO

PRINT N'✅ hotel_db database schema created successfully.';
GO
