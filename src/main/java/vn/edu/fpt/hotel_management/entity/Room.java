package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Khách sạn chứa phòng này
    @Column(name = "hotel_id", nullable = false)
    private int hotelId;

    // Loại phòng: Standard, Deluxe, Suite, Family...
    // Đổi field name sang roomType để tránh conflict với JPQL reserved word "type"
    @Column(name = "room_type", nullable = false)
    private String roomType;

    // Giá phòng mỗi đêm (VND)
    @Column(nullable = false)
    private double price;

    // Mô tả chi tiết phòng
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // Đường dẫn ảnh đại diện phòng
    @Column(name = "img_url")
    private String imgUrl;

    // Số lượng cửa sổ – dùng num_window tránh reserved word "window"
    @Column(name = "num_window", nullable = false)
    private int numWindow = 0;

    // Số lượng giường
    @Column(nullable = false)
    private int bed = 0;

    // Diện tích phòng (m²)
    @Column(nullable = false)
    private double acreage;

    // Số người tối đa
    @Column(nullable = false)
    private int person;

    // ===================== Constructors =====================

    public Room() {
    }

    public Room(int hotelId, String roomType, double price, String description,
            String imgUrl, int numWindow, int bed, double acreage, int person) {
        this.hotelId = hotelId;
        this.roomType = roomType;
        this.price = price;
        this.description = description;
        this.imgUrl = imgUrl;
        this.numWindow = numWindow;
        this.bed = bed;
        this.acreage = acreage;
        this.person = person;
    }

    // ===================== Getters & Setters =====================

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

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public int getNumWindow() {
        return numWindow;
    }

    public void setNumWindow(int numWindow) {
        this.numWindow = numWindow;
    }

    public int getBed() {
        return bed;
    }

    public void setBed(int bed) {
        this.bed = bed;
    }

    public double getAcreage() {
        return acreage;
    }

    public void setAcreage(double acreage) {
        this.acreage = acreage;
    }

    public int getPerson() {
        return person;
    }

    public void setPerson(int person) {
        this.person = person;
    }
}
