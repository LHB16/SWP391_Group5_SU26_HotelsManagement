package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Tên khách sạn
    @Column(nullable = false)
    private String name;

    // Địa chỉ khách sạn
    @Column(nullable = false)
    private String address;

    // Số sao (1-5)
    @Column(nullable = false)
    private int rating;

    // Giá/đêm (VND)
    @Column(nullable = false)
    private double price;

    // Đường dẫn ảnh
    @Column
    private String imageUrl;

    // true = đang hoạt động
    @Column(nullable = false)
    private boolean active = true;

    // ===================== Constructors =====================

    public Hotel() {
    }

    public Hotel(String name, String address, int rating, double price, String imageUrl) {
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.price = price;
        this.imageUrl = imageUrl;
        this.active = true;
    }

    // ===================== Getters & Setters =====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
