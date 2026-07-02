package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_facilities")
public class RoomFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Column(name = "free_toiletries", nullable = false)
    private boolean freeToiletries = false;

    @Column(name = "shower", nullable = false)
    private boolean shower = false;

    @Column(name = "bathrobe", nullable = false)
    private boolean bathrobe = false;

    @Column(name = "toilet", nullable = false)
    private boolean toilet = false;

    @Column(name = "towels", nullable = false)
    private boolean towels = false;

    @Column(name = "slippers", nullable = false)
    private boolean slippers = false;

    @Column(name = "hairdryer", nullable = false)
    private boolean hairdryer = false;

    @Column(name = "toilet_paper", nullable = false)
    private boolean toiletPaper = false;

    @Column(name = "air_conditioning", nullable = false)
    private boolean airConditioning = false;

    @Column(name = "safety_deposit_box", nullable = false)
    private boolean safetyDepositBox = false;

    @Column(name = "desk", nullable = false)
    private boolean desk = false;

    @Column(name = "television", nullable = false)
    private boolean television = false;

    @Column(name = "telephone", nullable = false)
    private boolean telephone = false;

    @Column(name = "iron", nullable = false)
    private boolean iron = false;

    @Column(name = "electric_kettle", nullable = false)
    private boolean electricKettle = false;

    @Column(name = "cable_channels", nullable = false)
    private boolean cableChannels = false;

    @Column(name = "wake_up_service", nullable = false)
    private boolean wakeUpService = false;

    @Column(name = "wardrobe_closet", nullable = false)
    private boolean wardrobeCloset = false;

    @Column(name = "clothes_rack", nullable = false)
    private boolean clothesRack = false;

    @Column(name = "free_bottled_water", nullable = false)
    private int freeBottledWater = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public RoomFacility() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public boolean isFreeToiletries() { return freeToiletries; }
    public void setFreeToiletries(boolean freeToiletries) { this.freeToiletries = freeToiletries; }
    public boolean isShower() { return shower; }
    public void setShower(boolean shower) { this.shower = shower; }
    public boolean isBathrobe() { return bathrobe; }
    public void setBathrobe(boolean bathrobe) { this.bathrobe = bathrobe; }
    public boolean isToilet() { return toilet; }
    public void setToilet(boolean toilet) { this.toilet = toilet; }
    public boolean isTowels() { return towels; }
    public void setTowels(boolean towels) { this.towels = towels; }
    public boolean isSlippers() { return slippers; }
    public void setSlippers(boolean slippers) { this.slippers = slippers; }
    public boolean isHairdryer() { return hairdryer; }
    public void setHairdryer(boolean hairdryer) { this.hairdryer = hairdryer; }
    public boolean isToiletPaper() { return toiletPaper; }
    public void setToiletPaper(boolean toiletPaper) { this.toiletPaper = toiletPaper; }
    public boolean isAirConditioning() { return airConditioning; }
    public void setAirConditioning(boolean airConditioning) { this.airConditioning = airConditioning; }
    public boolean isSafetyDepositBox() { return safetyDepositBox; }
    public void setSafetyDepositBox(boolean safetyDepositBox) { this.safetyDepositBox = safetyDepositBox; }
    public boolean isDesk() { return desk; }
    public void setDesk(boolean desk) { this.desk = desk; }
    public boolean isTelevision() { return television; }
    public void setTelevision(boolean television) { this.television = television; }
    public boolean isTelephone() { return telephone; }
    public void setTelephone(boolean telephone) { this.telephone = telephone; }
    public boolean isIron() { return iron; }
    public void setIron(boolean iron) { this.iron = iron; }
    public boolean isElectricKettle() { return electricKettle; }
    public void setElectricKettle(boolean electricKettle) { this.electricKettle = electricKettle; }
    public boolean isCableChannels() { return cableChannels; }
    public void setCableChannels(boolean cableChannels) { this.cableChannels = cableChannels; }
    public boolean isWakeUpService() { return wakeUpService; }
    public void setWakeUpService(boolean wakeUpService) { this.wakeUpService = wakeUpService; }
    public boolean isWardrobeCloset() { return wardrobeCloset; }
    public void setWardrobeCloset(boolean wardrobeCloset) { this.wardrobeCloset = wardrobeCloset; }
    public boolean isClothesRack() { return clothesRack; }
    public void setClothesRack(boolean clothesRack) { this.clothesRack = clothesRack; }
    public int getFreeBottledWater() { return freeBottledWater; }
    public void setFreeBottledWater(int freeBottledWater) { this.freeBottledWater = freeBottledWater; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
