package model.models;

import model.enums.PerformanceTypeEnum;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Objects;

/**
 * Represents a student in the loan system.
 * Modified to use standard Java types instead of JavaFX properties.
 */
public class Student implements PropertyChangeNotifier {
    private int viaId;
    private String name;
    private Date degreeEndDate;
    private String degreeTitle;
    private String email;
    private int phoneNumber;
    private boolean hasLaptop;
    private PerformanceTypeEnum performanceNeeded;
    private final PropertyChangeSupport changeSupport;

    /**
     * Creates a new student.
     *
     * @param name              Student's name
     * @param degreeEndDate     End date for education
     * @param degreeTitle       Education title
     * @param viaId             Unique VIA ID
     * @param email             Email address
     * @param phoneNumber       Phone number
     * @param performanceNeeded Laptop performance needs (HIGH/LOW)
     */
    public Student(String name, Date degreeEndDate, String degreeTitle, int viaId,
                   String email, int phoneNumber, PerformanceTypeEnum performanceNeeded) {
        this.name = name;
        this.degreeEndDate = degreeEndDate;
        this.degreeTitle = degreeTitle;
        this.viaId = viaId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.hasLaptop = false;
        this.performanceNeeded = performanceNeeded;
        this.changeSupport = new PropertyChangeSupport(this);
    }

    // Value getters

    public String getName() {
        return name;
    }

    public Date getDegreeEndDate() {
        return degreeEndDate;
    }

    public String getDegreeTitle() {
        return degreeTitle;
    }

    public int getViaId() {
        return viaId;
    }

    public String getEmail() {
        return email;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public PerformanceTypeEnum getPerformanceNeeded() {
        return performanceNeeded;
    }

    public boolean isHasLaptop() {
        return hasLaptop;
    }

    // Setters

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public void setDegreeEndDate(Date degreeEndDate) {
        Date oldValue = this.degreeEndDate;
        this.degreeEndDate = degreeEndDate;
        firePropertyChange("degreeEndDate", oldValue, degreeEndDate);
    }

    public void setDegreeTitle(String degreeTitle) {
        String oldValue = this.degreeTitle;
        this.degreeTitle = degreeTitle;
        firePropertyChange("degreeTitle", oldValue, degreeTitle);
    }

    public void setEmail(String email) {
        String oldValue = this.email;
        this.email = email;
        firePropertyChange("email", oldValue, email);
    }

    public void setPhoneNumber(int phoneNumber) {
        int oldValue = this.phoneNumber;
        this.phoneNumber = phoneNumber;
        firePropertyChange("phoneNumber", oldValue, phoneNumber);
    }

    public void setPerformanceNeeded(PerformanceTypeEnum performanceNeeded) {
        PerformanceTypeEnum oldValue = this.performanceNeeded;
        this.performanceNeeded = performanceNeeded;
        firePropertyChange("performanceNeeded", oldValue, performanceNeeded);
    }

    /**
     * Toggles the hasLaptop value and notifies listeners.
     */
    public void setHasLaptopToOpposite() {
        boolean oldValue = this.hasLaptop;
        boolean newValue = !oldValue;
        this.hasLaptop = newValue;
        firePropertyChange("hasLaptop", oldValue, newValue);
    }

    /**
     * Sets the hasLaptop value directly.
     *
     * @param hasLaptop The new value
     */
    public void setHasLaptop(boolean hasLaptop) {
        boolean oldValue = this.hasLaptop;
        this.hasLaptop = hasLaptop;
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
        return name + " (VIA ID: " + viaId + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return viaId == student.viaId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(viaId);
    }
}