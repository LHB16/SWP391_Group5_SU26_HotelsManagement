package vn.edu.fpt.hotel_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hotel_facilities")
public class HotelFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false, unique = true)
    private Hotel hotel;

    @Column(nullable = false)
    private boolean parking = false;

    @Column(nullable = false)
    private boolean restaurant = false;

    @Column(name = "breakfast_available", nullable = false)
    private boolean breakfastAvailable = false;

    @Column(name = "fitness_centre", nullable = false)
    private boolean fitnessCentre = false;

    @Column(name = "non_smoking_rooms", nullable = false)
    private boolean nonSmokingRooms = false;

    @Column(name = "airport_shuttle", nullable = false)
    private boolean airportShuttle = false;

    @Column(name = "spa_wellness_centre", nullable = false)
    private boolean spaWellnessCentre = false;

    @Column(name = "free_wifi", nullable = false)
    private boolean freeWifi = false;

    @Column(name = "ev_charging_station", nullable = false)
    private boolean evChargingStation = false;

    @Column(name = "wheelchair_accessible", nullable = false)
    private boolean wheelchairAccessible = false;

    @Column(name = "swimming_pool", nullable = false)
    private boolean swimmingPool = false;

    @Column(name = "bar_pub", nullable = false)
    private boolean barPub = false;

    @Column(name = "rent_vehicle", nullable = false, columnDefinition = "bit default 0")
    private boolean rentVehicle = false;

    public HotelFacility() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
    public boolean isParking() { return parking; }
    public void setParking(boolean parking) { this.parking = parking; }
    public boolean isRestaurant() { return restaurant; }
    public void setRestaurant(boolean restaurant) { this.restaurant = restaurant; }
    public boolean isBreakfastAvailable() { return breakfastAvailable; }
    public void setBreakfastAvailable(boolean breakfastAvailable) { this.breakfastAvailable = breakfastAvailable; }
    public boolean isFitnessCentre() { return fitnessCentre; }
    public void setFitnessCentre(boolean fitnessCentre) { this.fitnessCentre = fitnessCentre; }
    public boolean isNonSmokingRooms() { return nonSmokingRooms; }
    public void setNonSmokingRooms(boolean nonSmokingRooms) { this.nonSmokingRooms = nonSmokingRooms; }
    public boolean isAirportShuttle() { return airportShuttle; }
    public void setAirportShuttle(boolean airportShuttle) { this.airportShuttle = airportShuttle; }
    public boolean isSpaWellnessCentre() { return spaWellnessCentre; }
    public void setSpaWellnessCentre(boolean spaWellnessCentre) { this.spaWellnessCentre = spaWellnessCentre; }
    public boolean isFreeWifi() { return freeWifi; }
    public void setFreeWifi(boolean freeWifi) { this.freeWifi = freeWifi; }
    public boolean isEvChargingStation() { return evChargingStation; }
    public void setEvChargingStation(boolean evChargingStation) { this.evChargingStation = evChargingStation; }
    public boolean isWheelchairAccessible() { return wheelchairAccessible; }
    public void setWheelchairAccessible(boolean wheelchairAccessible) { this.wheelchairAccessible = wheelchairAccessible; }
    public boolean isSwimmingPool() { return swimmingPool; }
    public void setSwimmingPool(boolean swimmingPool) { this.swimmingPool = swimmingPool; }
    public boolean isBarPub() { return barPub; }
    public void setBarPub(boolean barPub) { this.barPub = barPub; }
    public boolean isRentVehicle() { return rentVehicle; }
    public void setRentVehicle(boolean rentVehicle) { this.rentVehicle = rentVehicle; }
}
