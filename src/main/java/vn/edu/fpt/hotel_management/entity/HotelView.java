package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hotel_views")
public class HotelView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false, unique = true)
    private Hotel hotel;

    @Column(name = "city_view", nullable = false)
    private boolean cityView = false;

    @Column(name = "beach_view", nullable = false)
    private boolean beachView = false;

    @Column(name = "garden_view", nullable = false)
    private boolean gardenView = false;

    @Column(name = "pool_view", nullable = false)
    private boolean poolView = false;

    @Column(name = "river_view", nullable = false)
    private boolean riverView = false;

    @Column(name = "mountain_view", nullable = false)
    private boolean mountainView = false;

    public HotelView() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
    public boolean isCityView() { return cityView; }
    public void setCityView(boolean cityView) { this.cityView = cityView; }
    public boolean isBeachView() { return beachView; }
    public void setBeachView(boolean beachView) { this.beachView = beachView; }
    public boolean isGardenView() { return gardenView; }
    public void setGardenView(boolean gardenView) { this.gardenView = gardenView; }
    public boolean isPoolView() { return poolView; }
    public void setPoolView(boolean poolView) { this.poolView = poolView; }
    public boolean isRiverView() { return riverView; }
    public void setRiverView(boolean riverView) { this.riverView = riverView; }
    public boolean isMountainView() { return mountainView; }
    public void setMountainView(boolean mountainView) { this.mountainView = mountainView; }
}
