package model.models;

import javafx.beans.property.*;
import model.enums.PerformanceTypeEnum;
import model.util.PropertyChangeNotifier;

import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.UUID;

/**
 * Repræsenterer en laptop i udlånssystemet.
 * Implementerer MVVM-kompatible properties for databinding.
 */
public class Laptop implements PropertyChangeNotifier {
    private final ObjectProperty<UUID> id;
    private final StringProperty brand;
    private final StringProperty model;
    private final IntegerProperty gigabyte;
    private final IntegerProperty ram;
    private final ObjectProperty<PerformanceTypeEnum> performanceType;
    private LaptopState state;
    private final PropertyChangeSupport changeSupport;

    /**
     * Konstruktør til oprettelse af en ny laptop med et tilfældigt UUID
     *
     * @param brand            Laptopens mærke
     * @param model            Laptopens model
     * @param gigabyte         Harddiskkapacitet i GB
     * @param ram              RAM i GB
     * @param performanceType  Ydelseskategori (HIGH/LOW)
     */
    public Laptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        this(UUID.randomUUID(), brand, model, gigabyte, ram, performanceType);
    }

    /**
     * Konstruktør til oprettelse af en laptop med et specifikt UUID (bruges ved indlæsning fra database)
     *
     * @param id               Unikt ID (UUID)
     * @param brand            Laptopens mærke
     * @param model            Laptopens model
     * @param gigabyte         Harddiskkapacitet i GB
     * @param ram              RAM i GB
     * @param performanceType  Ydelseskategori (HIGH/LOW)
     */
    public Laptop(UUID id, String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        this.id = new SimpleObjectProperty<>(this, "id", id);
        this.brand = new SimpleStringProperty(this, "brand", brand);
        this.model = new SimpleStringProperty(this, "model", model);
        this.gigabyte = new SimpleIntegerProperty(this, "gigabyte", gigabyte);
        this.ram = new SimpleIntegerProperty(this, "ram", ram);
        this.performanceType = new SimpleObjectProperty<>(this, "performanceType", performanceType);
        this.state = new AvailableState();
        this.changeSupport = new PropertyChangeSupport(this);
    }

    // Getters for properties (for JavaFX binding)

    public ObjectProperty<UUID> idProperty() {
        return id;
    }

    public StringProperty brandProperty() {
        return brand;
    }

    public StringProperty modelProperty() {
        return model;
    }

    public IntegerProperty gigabyteProperty() {
        return gigabyte;
    }

    public IntegerProperty ramProperty() {
        return ram;
    }

    public ObjectProperty<PerformanceTypeEnum> performanceTypeProperty() {
        return performanceType;
    }

    // Getters for values

    public UUID getId() {
        return id.get();
    }

    public String getBrand() {
        return brand.get();
    }

    public String getModel() {
        return model.get();
    }

    public int getGigabyte() {
        return gigabyte.get();
    }

    public int getRam() {
        return ram.get();
    }

    public PerformanceTypeEnum getPerformanceType() {
        return performanceType.get();
    }

    public LaptopState getState() {
        return state;
    }

    // Setters

    public void setBrand(String brand) {
        this.brand.set(brand);
    }

    public void setModel(String model) {
        this.model.set(model);
    }

    public void setGigabyte(int gigabyte) {
        this.gigabyte.set(gigabyte);
    }

    public void setRam(int ram) {
        this.ram.set(ram);
    }

    /**
     * Henter statens klassenavn til brug for databasen
     */
    public String getStateClassName() {
        return state.getClass().getSimpleName();
    }

    /**
     * Tjekker om laptopen er tilgængelig
     * @return true hvis laptopen er i Available tilstand
     */
    public boolean isAvailable() {
        return state instanceof AvailableState;
    }

    /**
     * Tjekker om laptopen er udlånt
     * @return true hvis laptopen er i Loaned tilstand
     */
    public boolean isLoaned() {
        return state instanceof LoanedState;
    }

    /**
     * Ændrer laptopens tilstand
     * Notificerer observere når tilstanden ændres til Available
     *
     * @param newState Den nye tilstand
     */
    public void changeState(LaptopState newState) {
        LaptopState oldState = this.state;
        String oldStateName = this.getStateClassName();
        this.state = newState;
        String newStateName = this.getStateClassName();

        // Notificér alle lyttere om state-ændringen
        firePropertyChange("state", oldState, newState);
        firePropertyChange("stateClassName", oldStateName, newStateName);

        // Specifik event når laptop bliver tilgængelig
        if (newState instanceof AvailableState) {
            firePropertyChange("available", false, true);
        } else if (oldState instanceof AvailableState) {
            firePropertyChange("available", true, false);
        }
    }

    /**
     * Sætter laptoppens tilstand baseret på klassens navn fra databasen
     * @param stateName navnet på tilstandsklassen (fx "AvailableState" eller "LoanedState")
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
        return brand.get() + " " + model.get() + " (" + performanceType.get() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Laptop laptop = (Laptop) o;
        return Objects.equals(id.get(), laptop.id.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.get());
    }
}