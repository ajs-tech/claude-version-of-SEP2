package model.models;

import javafx.beans.property.*;
import model.enums.PerformanceTypeEnum;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Objects;

/**
 * Repræsenterer en student i udlånssystemet.
 * Implementerer MVVM-kompatible properties for databinding.
 */
public class Student implements PropertyChangeNotifier {
    private final IntegerProperty viaId;
    private final StringProperty name;
    private final ObjectProperty<Date> degreeEndDate;
    private final StringProperty degreeTitle;
    private final StringProperty email;
    private final IntegerProperty phoneNumber;
    private final BooleanProperty hasLaptop;
    private final ObjectProperty<PerformanceTypeEnum> performanceNeeded;
    private final PropertyChangeSupport changeSupport;

    /**
     * Opretter en ny student.
     *
     * @param name              Studentens navn
     * @param degreeEndDate     Slutdato for uddannelse
     * @param degreeTitle       Uddannelsestitel
     * @param viaId             Unikt VIA ID
     * @param email             Email-adresse
     * @param phoneNumber       Telefonnummer
     * @param performanceNeeded Behov for laptoptypen (HIGH/LOW)
     */
    public Student(String name, Date degreeEndDate, String degreeTitle, int viaId,
                   String email, int phoneNumber, PerformanceTypeEnum performanceNeeded) {
        this.name = new SimpleStringProperty(this, "name", name);
        this.degreeEndDate = new SimpleObjectProperty<>(this, "degreeEndDate", degreeEndDate);
        this.degreeTitle = new SimpleStringProperty(this, "degreeTitle", degreeTitle);
        this.viaId = new SimpleIntegerProperty(this, "viaId", viaId);
        this.email = new SimpleStringProperty(this, "email", email);
        this.phoneNumber = new SimpleIntegerProperty(this, "phoneNumber", phoneNumber);
        this.hasLaptop = new SimpleBooleanProperty(this, "hasLaptop", false);
        this.performanceNeeded = new SimpleObjectProperty<>(this, "performanceNeeded", performanceNeeded);
        this.changeSupport = new PropertyChangeSupport(this);
    }

    // Property getters (for JavaFX binding)

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Date> degreeEndDateProperty() {
        return degreeEndDate;
    }

    public StringProperty degreeTitleProperty() {
        return degreeTitle;
    }

    public IntegerProperty viaIdProperty() {
        return viaId;
    }

    public StringProperty emailProperty() {
        return email;
    }

    public IntegerProperty phoneNumberProperty() {
        return phoneNumber;
    }

    public BooleanProperty hasLaptopProperty() {
        return hasLaptop;
    }

    public ObjectProperty<PerformanceTypeEnum> performanceNeededProperty() {
        return performanceNeeded;
    }

    // Value getters

    public String getName() {
        return name.get();
    }

    public Date getDegreeEndDate() {
        return degreeEndDate.get();
    }

    public String getDegreeTitle() {
        return degreeTitle.get();
    }

    public int getViaId() {
        return viaId.get();
    }

    public String getEmail() {
        return email.get();
    }

    public int getPhoneNumber() {
        return phoneNumber.get();
    }

    public PerformanceTypeEnum getPerformanceNeeded() {
        return performanceNeeded.get();
    }

    public boolean isHasLaptop() {
        return hasLaptop.get();
    }

    // Setters

    public void setName(String name) {
        this.name.set(name);
    }

    public void setDegreeEndDate(Date degreeEndDate) {
        this.degreeEndDate.set(degreeEndDate);
    }

    public void setDegreeTitle(String degreeTitle) {
        this.degreeTitle.set(degreeTitle);
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber.set(phoneNumber);
    }

    public void setPerformanceNeeded(PerformanceTypeEnum performanceNeeded) {
        PerformanceTypeEnum oldValue = this.performanceNeeded.get();
        this.performanceNeeded.set(performanceNeeded);
        firePropertyChange("performanceNeeded", oldValue, performanceNeeded);
    }

    /**
     * Skifter hasLaptop-værdien til det modsatte og notificerer listeners.
     */
    public void setHasLaptopToOpposite() {
        boolean oldValue = this.hasLaptop.get();
        boolean newValue = !oldValue;
        this.hasLaptop.set(newValue);
        firePropertyChange("hasLaptop", oldValue, newValue);
    }

    /**
     * Sætter hasLaptop-værdien direkte.
     *
     * @param hasLaptop Den nye værdi
     */
    public void setHasLaptop(boolean hasLaptop) {
        boolean oldValue = this.hasLaptop.get();
        this.hasLaptop.set(hasLaptop);
        firePropertyChange("hasLaptop", oldValue, hasLaptop);
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
        return name.get() + " (VIA ID: " + viaId.get() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return viaId.get() == student.viaId.get();
    }

    @Override
    public int hashCode() {
        return Objects.hash(viaId.get());
    }
}