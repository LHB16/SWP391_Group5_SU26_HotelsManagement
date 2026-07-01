package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hotel")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "owner_id", nullable = true)
    private Integer ownerId;



    // Tên khách sạn
    @Column(nullable = false)
    private String name;

    // Địa chỉ khách sạn
    @Column(nullable = false)
    private String address;

    // Số sao (1-5)
    @Column(nullable = false)
    private double rating;


    // Giá/đêm (VND) – dùng long để tránh floating-point precision
    @Column(nullable = true)
    private BigDecimal price;

    // Đường dẫn ảnh
    @Column(name = "image_url")
    private String imageUrl;

    // true = đang hoạt động
    @Column(nullable = false)
    private boolean active = true;

    // ===================== Constructors =====================

    public Hotel() {
    }

    public Hotel(String name, String address, double rating, String imageUrl) {
        this.name = name;
        this.address = address;
        this.rating = rating;
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

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
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

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
