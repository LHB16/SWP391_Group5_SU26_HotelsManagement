package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "hotel_id", nullable = false)
    private int hotelId;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "user_full_name", nullable = false)
    private String userFullName;

    @Column(nullable = false)
    private int rating; // Điểm đánh giá từ 1 đến 5 sao

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Review() {
    }

    public Review(int hotelId, int userId, String userFullName, int rating, String comment) {
        this.hotelId = hotelId;
        this.userId = userId;
        this.userFullName = userFullName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
