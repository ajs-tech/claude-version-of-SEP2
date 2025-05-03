package model.models;

import model.enums.ReservationStatusEnum;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a reservation in the loan system.
 * Modified to use standard Java types instead of JavaFX properties.
 */
public class Reservation implements PropertyChangeNotifier {
    private UUID reservationId;
    private Student student;
    private Laptop laptop;
    private ReservationStatusEnum status;
    private Date creationDate;
    private final PropertyChangeSupport changeSupport;

    /**
     * Constructor for creating a new reservation with automatically generated UUID.
     *
     * @param student The student borrowing the laptop
     * @param laptop  The laptop being loaned
     */
    public Reservation(Student student, Laptop laptop) {
        this(UUID.randomUUID(), student, laptop, ReservationStatusEnum.ACTIVE, new Date());
    }

    /**
     * Constructor for creating a reservation with specific UUID and status.
     * Used when loading from database.
     *
     * @param reservationId Reservation's unique ID
     * @param student       The student borrowing the laptop
     * @param laptop        The laptop being loaned
     * @param status        Reservation status
     * @param creationDate  Date of reservation creation
     */
    public Reservation(UUID reservationId, Student student, Laptop laptop,
                       ReservationStatusEnum status, Date creationDate) {
        this.reservationId = reservationId;
        this.student = student;
        this.laptop = laptop;
        this.status = status;
        this.creationDate = creationDate;
        this.changeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Constructor for creating a reservation with specific UUID and status.
     * Uses current date for creation.
     *
     * @param reservationId Reservation's unique ID
     * @param student       The student borrowing the laptop
     * @param laptop        The laptop being loaned
     * @param status        Reservation status
     */
    public Reservation(UUID reservationId, Student student, Laptop laptop, ReservationStatusEnum status) {
        this(reservationId, student, laptop, status, new Date());
    }

    // Getters

    public UUID getReservationId() {
        return reservationId;
    }

    public Student getStudent() {
        return student;
    }

    public String getStudentDetailsString() {
        return student.toString();
    }

    public Laptop getLaptop() {
        return laptop;
    }

    public String getLaptopDetailsString() {
        return laptop.toString();
    }

    public ReservationStatusEnum getStatus() {
        return status;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Changes the reservation status.
     * If an active reservation is completed (COMPLETED or CANCELLED),
     * the associated laptop is released and the student's hasLaptop status is updated.
     *
     * @param newStatus The new status for the reservation
     */
    public void changeStatus(ReservationStatusEnum newStatus) {
        ReservationStatusEnum oldStatus = this.status;
        this.status = newStatus;

        // Notify about status change
        firePropertyChange("status", oldStatus, newStatus);

        // If a reservation is completed, update laptop and student state
        if (oldStatus == ReservationStatusEnum.ACTIVE &&
                (newStatus == ReservationStatusEnum.COMPLETED || newStatus == ReservationStatusEnum.CANCELLED)) {

            // Make laptop available again
            Laptop reservedLaptop = laptop;
            if (reservedLaptop.isLoaned()) {
                reservedLaptop.changeState(AvailableState.INSTANCE);
            }

            // Update student has laptop status if necessary
            Student reservedStudent = student;
            if (reservedStudent.isHasLaptop()) {
                reservedStudent.setHasLaptopToOpposite();
            }

            // Notify that the reservation is completed
            firePropertyChange("completed", false, true);
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
        return "Reservation: " + student.getName() + " - " +
                laptop.getModel() + " (" + status + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(reservationId, that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }
}