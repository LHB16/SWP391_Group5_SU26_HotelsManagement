package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "method")
    private String method;

    @Column(name = "status")
    private String status;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;
    
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "sender_account_number")
    private String senderAccountNumber;

    @Column(name = "sender_bank_name")
    private String senderBankName;

    @Column(name = "sender_account_name")
    private String senderAccountName;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "qr_expires_at")
    private LocalDateTime qrExpiresAt;

    public Payment() {
    }

    public boolean isQrExpired() {
        if (!"PENDING".equalsIgnoreCase(this.status)) {
            return false;
        }
        return qrExpiresAt != null && LocalDateTime.now().isAfter(qrExpiresAt);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getSenderAccountNumber() { return senderAccountNumber; }
    public void setSenderAccountNumber(String senderAccountNumber) { this.senderAccountNumber = senderAccountNumber; }
    public String getSenderBankName() { return senderBankName; }
    public void setSenderBankName(String senderBankName) { this.senderBankName = senderBankName; }
    public String getSenderAccountName() { return senderAccountName; }
    public void setSenderAccountName(String senderAccountName) { this.senderAccountName = senderAccountName; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getQrExpiresAt() { return qrExpiresAt; }
    public void setQrExpiresAt(LocalDateTime qrExpiresAt) { this.qrExpiresAt = qrExpiresAt; }
}
