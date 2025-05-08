package model.models;

import model.enums.PerformanceTypeEnum;
import model.util.ModelObservable;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a laptop in the loan system.
 * Uses Java's built-in Observable pattern and State pattern.
 */
public class Laptop extends ModelObservable {
    private UUID id;
    private String brand;
    private String model;
    private int gigabyte;
    private int ram;
    private PerformanceTypeEnum performanceType;
    private LaptopState state;

    /**
     * Constructor for creating a new laptop with a random UUID
     *
     * @param brand            model.models.Laptop brand
     * @param model            model.models.Laptop model
     * @param gigabyte         Hard disk capacity in GB
     * @param ram              RAM in GB
     * @param performanceType  Performance category (HIGH/LOW)
     * @throws IllegalArgumentException if input validation fails
     */
    public Laptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        this(UUID.randomUUID(), brand, model, gigabyte, ram, performanceType);
    }

    /**
     * Constructor for creating a laptop with a specific UUID (used when loading from database)
     *
     * @param id               Unique ID (UUID)
     * @param brand            model.models.Laptop brand
     * @param model            model.models.Laptop model
     * @param gigabyte         Hard disk capacity in GB
     * @param ram              RAM in GB
     * @param performanceType  Performance category (HIGH/LOW)
     * @throws IllegalArgumentException if input validation fails
     */
    public Laptop(UUID id, String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        validateInput(brand, model, gigabyte, ram, performanceType);
        
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.gigabyte = gigabyte;
        this.ram = ram;
        this.performanceType = performanceType;
        this.state = new AvailableState();
    }
    
    /**
     * Validates all input parameters.
     * Throws exception with clear message about which validation failed.
     */
    private void validateInput(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be empty");
        }
        
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be empty");
        }
        
        if (gigabyte <= 0 || gigabyte > 4000) {
            throw new IllegalArgumentException("Hard disk capacity must be between 1 and 4000 GB");
        }
        
        if (ram <= 0 || ram > 128) {
            throw new IllegalArgumentException("RAM must be between 1 and 128 GB");
        }
        
        if (performanceType == null) {
            throw new IllegalArgumentException("Performance type cannot be null");
        }
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getGigabyte() {
        return gigabyte;
    }

    public int getRam() {
        return ram;
    }

    public PerformanceTypeEnum getPerformanceType() {
        return performanceType;
    }

    public LaptopState getState() {
        return state;
    }

    // Setters

    public void setBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be empty");
        }
        
        String oldValue = this.brand;
        this.brand = brand;
        notifyPropertyChanged("brand", oldValue, brand);
    }

    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be empty");
        }
        
        String oldValue = this.model;
        this.model = model;
        notifyPropertyChanged("model", oldValue, model);
    }

    public void setGigabyte(int gigabyte) {
        if (gigabyte <= 0 || gigabyte > 4000) {
            throw new IllegalArgumentException("Hard disk capacity must be between 1 and 4000 GB");
        }
        
        int oldValue = this.gigabyte;
        this.gigabyte = gigabyte;
        notifyPropertyChanged("gigabyte", oldValue, gigabyte);
    }

    public void setRam(int ram) {
        if (ram <= 0 || ram > 128) {
            throw new IllegalArgumentException("RAM must be between 1 and 128 GB");
        }
        
        int oldValue = this.ram;
        this.ram = ram;
        notifyPropertyChanged("ram", oldValue, ram);
    }

    public void setPerformanceType(PerformanceTypeEnum performanceType) {
        if (performanceType == null) {
            throw new IllegalArgumentException("Performance type cannot be null");
        }
        
        PerformanceTypeEnum oldValue = this.performanceType;
        this.performanceType = performanceType;
        notifyPropertyChanged("performanceType", oldValue, performanceType);
    }

    /**
     * Gets the state class name for use with the database
     */
    public String getStateClassName() {
        return state.getClass().getSimpleName();
    }

    /**
     * Checks if laptop is available
     * @return true if laptop is in Available state
     */
    public boolean isAvailable() {
        return state instanceof AvailableState;
    }

    /**
     * Checks if laptop is loaned
     * @return true if laptop is in Loaned state
     */
    public boolean isLoaned() {
        return state instanceof LoanedState;
    }

    /**
     * Changes the laptop's state
     * Notifies observers when state changes to Available
     *
     * @param newState The new state
     */
    public void changeState(LaptopState newState) {
        LaptopState oldState = this.state;
        String oldStateName = this.getStateClassName();
        this.state = newState;
        String newStateName = this.getStateClassName();

        // Notify observers about state change
        notifyPropertyChanged("state", oldState, newState);
        notifyPropertyChanged("stateClassName", oldStateName, newStateName);

        // Specific event when laptop becomes available
        if (newState instanceof AvailableState) {
            notifyPropertyChanged("available", false, true);
        } else if (oldState instanceof AvailableState) {
            notifyPropertyChanged("available", true, false);
        }
    }

    /**
     * Sets the laptop state based on the class name from the database
     * @param stateName name of the state class (e.g. "model.models.AvailableState" or "LoanedState")
     */
    public void setStateFromDatabase(String stateName) {
        if ("LoanedState".equals(stateName)) {
            if (!(state instanceof LoanedState)) {
                changeState(new LoanedState());
            }
        } else {
            if (!(state instanceof AvailableState)) {
                changeState(new AvailableState());
            }
        }
    }

    @Override
    public String toString() {
        return brand + " " + model + " (" + performanceType + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Laptop laptop = (Laptop) o;
        return Objects.equals(id, laptop.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}