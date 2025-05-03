package model.models;

import model.enums.PerformanceTypeEnum;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a laptop in the loan system.
 * Modified to use standard Java types instead of JavaFX properties.
 */
public class Laptop implements PropertyChangeNotifier {
    private UUID id;
    private String brand;
    private String model;
    private int gigabyte;
    private int ram;
    private PerformanceTypeEnum performanceType;
    private LaptopState state;
    private final PropertyChangeSupport changeSupport;

    /**
     * Constructor for creating a new laptop with a random UUID
     *
     * @param brand            Laptop brand
     * @param model            Laptop model
     * @param gigabyte         Hard disk capacity in GB
     * @param ram              RAM in GB
     * @param performanceType  Performance category (HIGH/LOW)
     */
    public Laptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        this(UUID.randomUUID(), brand, model, gigabyte, ram, performanceType);
    }

    /**
     * Constructor for creating a laptop with a specific UUID (used when loading from database)
     *
     * @param id               Unique ID (UUID)
     * @param brand            Laptop brand
     * @param model            Laptop model
     * @param gigabyte         Hard disk capacity in GB
     * @param ram              RAM in GB
     * @param performanceType  Performance category (HIGH/LOW)
     */
    public Laptop(UUID id, String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.gigabyte = gigabyte;
        this.ram = ram;
        this.performanceType = performanceType;
        this.state = new AvailableState();
        this.changeSupport = new PropertyChangeSupport(this);
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
        String oldValue = this.brand;
        this.brand = brand;
        firePropertyChange("brand", oldValue, brand);
    }

    public void setModel(String model) {
        String oldValue = this.model;
        this.model = model;
        firePropertyChange("model", oldValue, model);
    }

    public void setGigabyte(int gigabyte) {
        int oldValue = this.gigabyte;
        this.gigabyte = gigabyte;
        firePropertyChange("gigabyte", oldValue, gigabyte);
    }

    public void setRam(int ram) {
        int oldValue = this.ram;
        this.ram = ram;
        firePropertyChange("ram", oldValue, ram);
    }

    public void setPerformanceType(PerformanceTypeEnum performanceType) {
        PerformanceTypeEnum oldValue = this.performanceType;
        this.performanceType = performanceType;
        firePropertyChange("performanceType", oldValue, performanceType);
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

        // Notify all listeners about state change
        firePropertyChange("state", oldState, newState);
        firePropertyChange("stateClassName", oldStateName, newStateName);

        // Specific event when laptop becomes available
        if (newState instanceof AvailableState) {
            firePropertyChange("available", false, true);
        } else if (oldState instanceof AvailableState) {
            firePropertyChange("available", true, false);
        }
    }

    /**
     * Sets the laptop state based on the class name from the database
     * @param stateName name of the state class (e.g. "AvailableState" or "LoanedState")
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

    // PropertyChangeNotifier implementation

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
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