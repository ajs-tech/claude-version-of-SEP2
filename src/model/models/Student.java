package model.models;

import model.enums.PerformanceTypeEnum;
import model.util.ModelObservable;

import java.util.Date;
import java.util.Objects;

/**
 * Represents a student in the loan system.
 * Uses Java's built-in Observable pattern.
 */
public class Student extends ModelObservable {
    private int viaId;
    private String name;
    private Date degreeEndDate;
    private String degreeTitle;
    private String email;
    private int phoneNumber;
    private boolean hasLaptop;
    private PerformanceTypeEnum performanceNeeded;

    /**
     * Creates a new student.
     *
     * @param name              Student's name
     * @param degreeEndDate     End date for education
     * @param degreeTitle       Education title
     * @param viaId             Unique VIA ID
     * @param email             Email address
     * @param phoneNumber       Phone number
     * @param performanceNeeded model.models.Laptop performance needs (HIGH/LOW)
     * @throws IllegalArgumentException if input validation fails
     */
    public Student(String name, Date degreeEndDate, String degreeTitle, int viaId,
                   String email, int phoneNumber, PerformanceTypeEnum performanceNeeded) {
        validateInput(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);
        
        this.name = name;
        this.degreeEndDate = degreeEndDate;
        this.degreeTitle = degreeTitle;
        this.viaId = viaId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.hasLaptop = false;
        this.performanceNeeded = performanceNeeded;
    }
    
    /**
     * Validates all input parameters.
     * Throws exception with clear message about which validation failed.
     */
    private void validateInput(String name, Date degreeEndDate, String degreeTitle, 
                               int viaId, String email, int phoneNumber, 
                               PerformanceTypeEnum performanceNeeded) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be empty");
        }
        
        if (degreeEndDate == null) {
            throw new IllegalArgumentException("Degree end date cannot be null");
        }
        
        if (degreeTitle == null || degreeTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Degree title cannot be empty");
        }
        
        if (viaId <= 0 || !isValidViaId(viaId)) {
            throw new IllegalArgumentException("Invalid VIA ID format");
        }
        
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (phoneNumber <= 0 || !isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        
        if (performanceNeeded == null) {
            throw new IllegalArgumentException("Performance needed cannot be null");
        }
    }

    /**
     * Validates email format.
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validates VIA ID format.
     */
    private boolean isValidViaId(int viaId) {
        String viaIdStr = String.valueOf(viaId);
        return viaIdStr.matches("^[0-9]{4,8}$");
    }

    /**
     * Validates phone number format.
     */
    private boolean isValidPhoneNumber(int phoneNumber) {
        String phoneStr = String.valueOf(phoneNumber);
        return phoneStr.matches("^[0-9]{8,12}$");
    }

    // Getters

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
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be empty");
        }
        
        String oldValue = this.name;
        this.name = name;
        notifyPropertyChanged("name", oldValue, name);
    }

    public void setDegreeEndDate(Date degreeEndDate) {
        if (degreeEndDate == null) {
            throw new IllegalArgumentException("Degree end date cannot be null");
        }
        
        Date oldValue = this.degreeEndDate;
        this.degreeEndDate = degreeEndDate;
        notifyPropertyChanged("degreeEndDate", oldValue, degreeEndDate);
    }

    public void setDegreeTitle(String degreeTitle) {
        if (degreeTitle == null || degreeTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Degree title cannot be empty");
        }
        
        String oldValue = this.degreeTitle;
        this.degreeTitle = degreeTitle;
        notifyPropertyChanged("degreeTitle", oldValue, degreeTitle);
    }

    public void setEmail(String email) {
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        String oldValue = this.email;
        this.email = email;
        notifyPropertyChanged("email", oldValue, email);
    }

    public void setPhoneNumber(int phoneNumber) {
        if (phoneNumber <= 0 || !isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        
        int oldValue = this.phoneNumber;
        this.phoneNumber = phoneNumber;
        notifyPropertyChanged("phoneNumber", oldValue, phoneNumber);
    }

    public void setPerformanceNeeded(PerformanceTypeEnum performanceNeeded) {
        if (performanceNeeded == null) {
            throw new IllegalArgumentException("Performance needed cannot be null");
        }
        
        PerformanceTypeEnum oldValue = this.performanceNeeded;
        this.performanceNeeded = performanceNeeded;
        notifyPropertyChanged("performanceNeeded", oldValue, performanceNeeded);
    }

    /**
     * Toggles the hasLaptop value and notifies observers.
     */
    public void setHasLaptopToOpposite() {
        boolean oldValue = this.hasLaptop;
        boolean newValue = !oldValue;
        this.hasLaptop = newValue;
        notifyPropertyChanged("hasLaptop", oldValue, newValue);
    }

    /**
     * Sets the hasLaptop value directly.
     *
     * @param hasLaptop The new value
     */
    public void setHasLaptop(boolean hasLaptop) {
        boolean oldValue = this.hasLaptop;
        this.hasLaptop = hasLaptop;
        notifyPropertyChanged("hasLaptop", oldValue, hasLaptop);
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