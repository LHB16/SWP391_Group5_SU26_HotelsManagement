package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private double acreage;

    @Column(nullable = false)
    private int bed;

    @Column(name = "hotel_id", nullable = false)
    private int hotelId;

    @Column(nullable = false)
    private int person;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "num_window", nullable = false)
    private int window;

    @Column(name = "description")
    private String description;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "room_type", nullable = false)
    private String type;

    // ===================== Constructors =====================
    public Room() {
    }

    public Room(double acreage, int bed, int hotelId, int person, BigDecimal price, int window, String description, String imgUrl, String type) {
        this.acreage = acreage;
        this.bed = bed;
        this.hotelId = hotelId;
        this.person = person;
        this.price = price;
        this.window = window;
        this.description = description;
        this.imgUrl = imgUrl;
        this.type = type;
    }

    // ===================== Getters & Setters =====================
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAcreage() {
        return acreage;
    }

    public void setAcreage(double acreage) {
        this.acreage = acreage;
    }

    public int getBed() {
        return bed;
    }

    public void setBed(int bed) {
        this.bed = bed;
    }

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    public int getPerson() {
        return person;
    }

    public void setPerson(int person) {
        this.person = person;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getWindow() {
        return window;
    }

    public void setWindow(int window) {
        this.window = window;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomType() {
        return this.type;
    }

    public void setRoomType(String roomType) {
        this.type = roomType;
    }

    public int getNumWindow() {
        return this.window;
    }

    public void setNumWindow(int numWindow) {
        this.window = numWindow;
    }
}
