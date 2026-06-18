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
    @Column(nullable = false)
    private String type;

    // Giá phòng mỗi đêm (VND)
    @Column(nullable = false)
    private double price;

    // Mô tả chi tiết phòng
    @Column(columnDefinition = "TEXT")
    private String description;

    // Đường dẫn ảnh đại diện phòng
    @Column(name = "img_url")
    private String imgUrl;

    // Số lượng cửa sổ
    @Column(nullable = false)
    private int window = 0;

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

    public Room(int hotelId, String type, double price, String description,
                String imgUrl, int window, int bed, double acreage, int person) {
        this.hotelId = hotelId;
        this.type = type;
        this.price = price;
        this.description = description;
        this.imgUrl = imgUrl;
        this.window = window;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getWindow() {
        return window;
    }

    public void setWindow(int window) {
        this.window = window;
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
