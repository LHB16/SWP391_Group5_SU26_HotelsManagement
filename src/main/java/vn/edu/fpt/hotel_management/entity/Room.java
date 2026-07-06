package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private Double acreage;

    @Column
    private Integer bed;

    @Column(name = "hotel_id", nullable = false)
    private int hotelId;

    @Column
    private Integer person;

    @Column
    private BigDecimal price;

    @Column(name = "num_window")
    private Integer window;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "room_type", nullable = false)
    private String type;

    @Column(name = "number_rooms")
    private Integer numberRooms;

    @Column(name = "room_status")
    private Boolean roomStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

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

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAcreage() { return acreage != null ? acreage : 0; }
    public void setAcreage(double acreage) { this.acreage = acreage; }
    public int getBed() { return bed != null ? bed : 0; }
    public void setBed(int bed) { this.bed = bed; }
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }
    public int getPerson() { return person != null ? person : 0; }
    public void setPerson(int person) { this.person = person; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getWindow() { return window != null ? window : 0; }
    public void setWindow(int window) { this.window = window; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    @OneToOne(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private RoomFacility facility;

    public RoomFacility getFacility() { return facility; }
    public void setFacility(RoomFacility facility) { this.facility = facility; }

    public String getRoomType() { return this.type; }
    public void setRoomType(String roomType) { this.type = roomType; }
    public int getNumWindow() { return this.window != null ? this.window : 0; }
    public void setNumWindow(int numWindow) { this.window = numWindow; }
    public Integer getNumberRooms() { return numberRooms; }
    public void setNumberRooms(Integer numberRooms) { this.numberRooms = numberRooms; }
    public Boolean getRoomStatus() { return roomStatus; }
    public void setRoomStatus(Boolean roomStatus) { this.roomStatus = roomStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
