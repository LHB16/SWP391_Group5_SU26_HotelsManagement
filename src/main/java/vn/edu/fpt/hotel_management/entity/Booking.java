package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "phone")
    private String phone;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "num_nights")
    private Integer numNights;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "status")
    private String status;

    @Column(name = "special_notes", columnDefinition = "NVARCHAR(MAX)")
    private String specialNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "booking", fetch = FetchType.EAGER)
    private Payment payment;

    public Booking() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
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

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNumNights() {
        return numNights;
    }

    public void setNumNights(Integer numNights) {
        this.numNights = numNights;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpecialNotes() {
        return specialNotes;
    }

    public void setSpecialNotes(String specialNotes) {
        this.specialNotes = specialNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isCancelable() {
        return "CONFIRMED".equalsIgnoreCase(this.status) 
            && this.checkInDate != null 
            && java.time.LocalDate.now().isBefore(this.checkInDate);
    }

    public boolean isCancelable() {
        return "CONFIRMED".equalsIgnoreCase(this.status) 
            && this.checkInDate != null 
            && java.time.LocalDate.now().isBefore(this.checkInDate);
    }
}
