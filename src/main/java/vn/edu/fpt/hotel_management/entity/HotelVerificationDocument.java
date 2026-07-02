package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_verification_documents")
public class HotelVerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "business_registration_doc")
    private String businessRegistrationDoc;

    @Column(name = "land_certificate_doc")
    private String landCertificateDoc;

    @Column(name = "rental_contract_doc")
    private String rentalContractDoc;

    @Column(name = "upload_status")
    private String uploadStatus = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public HotelVerificationDocument() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBusinessRegistrationDoc() {
        return businessRegistrationDoc;
    }

    public void setBusinessRegistrationDoc(String businessRegistrationDoc) {
        this.businessRegistrationDoc = businessRegistrationDoc;
    }

    public String getLandCertificateDoc() {
        return landCertificateDoc;
    }

    public void setLandCertificateDoc(String landCertificateDoc) {
        this.landCertificateDoc = landCertificateDoc;
    }

    public String getRentalContractDoc() {
        return rentalContractDoc;
    }

    public void setRentalContractDoc(String rentalContractDoc) {
        this.rentalContractDoc = rentalContractDoc;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
