package model.models;

import model.enums.ReservationStatusEnum;
import model.util.ModelObservable;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a reservation in the loan system.
 * Uses Java's built-in Observable pattern.
 */
public class Reservation extends ModelObservable {
    private final UUID reservationId;
    private final Student student;
    private final Laptop laptop;
    private ReservationStatusEnum status;
    private final Date creationDate;

    /**
     * Constructor for creating a new reservation with automatically generated UUID.
     *
     * @param student The student borrowing the laptop
     * @param laptop  The laptop being loaned
     * @throws IllegalArgumentException if input validation fails
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
     * @throws IllegalArgumentException if input validation fails
     */
    public Reservation(UUID reservationId, Student student, Laptop laptop,
                       ReservationStatusEnum status, Date creationDate) {
        validateInput(student, laptop, status, creationDate);

        this.reservationId = reservationId;
        this.student = student;
        this.laptop = laptop;
        this.status = status;
        this.creationDate = creationDate;
    }

    /**
     * Constructor for creating a reservation with specific UUID and status.
     * Uses current date for creation.
     *
     * @param reservationId Reservation's unique ID
     * @param student       The student borrowing the laptop
     * @param laptop        The laptop being loaned
     * @param status        Reservation status
     * @throws IllegalArgumentException if input validation fails
     */
    public Reservation(UUID reservationId, Student student, Laptop laptop, ReservationStatusEnum status) {
        this(reservationId, student, laptop, status, new Date());
    }

    /**
     * Validates all input parameters.
     * Throws exception with clear message about which validation failed.
     */
    private void validateInput(Student student, Laptop laptop, ReservationStatusEnum status, Date creationDate) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }

        if (laptop == null) {
            throw new IllegalArgumentException("Laptop cannot be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (creationDate == null) {
            throw new IllegalArgumentException("Creation date cannot be null");
        }
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
     * @throws IllegalArgumentException if newStatus is null
     */
    public void changeStatus(ReservationStatusEnum newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        ReservationStatusEnum oldStatus = this.status;
        this.status = newStatus;

        // Notify about status change
        notifyPropertyChanged("status", oldStatus, newStatus);

        // If a reservation is completed, update laptop and student state
        if (oldStatus == ReservationStatusEnum.ACTIVE &&
                (newStatus == ReservationStatusEnum.COMPLETED || newStatus == ReservationStatusEnum.CANCELLED)) {

            // Make laptop available again
            Laptop reservedLaptop = laptop;
            if (reservedLaptop.isLoaned()) {
                reservedLaptop.changeState(new AvailableState());
            }

            // Update student has laptop status if necessary
            Student reservedStudent = student;
            if (reservedStudent.isHasLaptop()) {
                reservedStudent.setHasLaptop(false);
            }

            // Notify that the reservation is completed
            notifyPropertyChanged("completed", false, true);
        }
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