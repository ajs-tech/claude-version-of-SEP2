package model.models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.enums.ReservationStatusEnum;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Repræsenterer en reservation i udlånssystemet.
 * Implementerer MVVM-kompatible properties for databinding.
 */
public class Reservation implements PropertyChangeNotifier {
    private final ObjectProperty<UUID> reservationId;
    private final ObjectProperty<Student> student;
    private final ObjectProperty<Laptop> laptop;
    private final ObjectProperty<ReservationStatusEnum> status;
    private final ObjectProperty<Date> creationDate;
    private final PropertyChangeSupport changeSupport;

    /**
     * Konstruktør til oprettelse af en ny reservation med automatisk genereret UUID.
     *
     * @param student Studenten, der låner laptopen
     * @param laptop  Laptopen, der udlånes
     */
    public Reservation(Student student, Laptop laptop) {
        this(UUID.randomUUID(), student, laptop, ReservationStatusEnum.ACTIVE, new Date());
    }

    /**
     * Konstruktør til oprettelse af en reservation med specifikt UUID og status.
     * Bruges ved indlæsning fra databasen.
     *
     * @param reservationId Reservationens unikke ID
     * @param student       Studenten, der låner laptopen
     * @param laptop        Laptopen, der udlånes
     * @param status        Reservationens status
     * @param creationDate  Dato for oprettelse af reservationen
     */
    public Reservation(UUID reservationId, Student student, Laptop laptop,
                       ReservationStatusEnum status, Date creationDate) {
        this.reservationId = new SimpleObjectProperty<>(this, "reservationId", reservationId);
        this.student = new SimpleObjectProperty<>(this, "student", student);
        this.laptop = new SimpleObjectProperty<>(this, "laptop", laptop);
        this.status = new SimpleObjectProperty<>(this, "status", status);
        this.creationDate = new SimpleObjectProperty<>(this, "creationDate", creationDate);
        this.changeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Konstruktør til oprettelse af en reservation med specifikt UUID og status.
     * Bruger nuværende dato for oprettelse.
     *
     * @param reservationId Reservationens unikke ID
     * @param student       Studenten, der låner laptopen
     * @param laptop        Laptopen, der udlånes
     * @param status        Reservationens status
     */
    public Reservation(UUID reservationId, Student student, Laptop laptop, ReservationStatusEnum status) {
        this(reservationId, student, laptop, status, new Date());
    }

    // Property getters (for JavaFX binding)

    public ObjectProperty<UUID> reservationIdProperty() {
        return reservationId;
    }

    public ObjectProperty<Student> studentProperty() {
        return student;
    }

    public ObjectProperty<Laptop> laptopProperty() {
        return laptop;
    }

    public ObjectProperty<ReservationStatusEnum> statusProperty() {
        return status;
    }

    public ObjectProperty<Date> creationDateProperty() {
        return creationDate;
    }

    // Value getters

    public UUID getReservationId() {
        return reservationId.get();
    }

    public Student getStudent() {
        return student.get();
    }

    public String getStudentDetailsString() {
        return student.get().toString();
    }

    public Laptop getLaptop() {
        return laptop.get();
    }

    public String getLaptopDetailsString() {
        return laptop.get().toString();
    }

    public ReservationStatusEnum getStatus() {
        return status.get();
    }

    public Date getCreationDate() {
        return creationDate.get();
    }

    /**
     * Ændrer reservationens status.
     * Hvis en aktiv reservation afsluttes (COMPLETED eller CANCELLED),
     * frigives den tilknyttede laptop og opdaterer studentens hasLaptop status.
     *
     * @param newStatus Den nye status for reservationen
     */
    public void changeStatus(ReservationStatusEnum newStatus) {
        ReservationStatusEnum oldStatus = this.status.get();
        this.status.set(newStatus);

        // Notificér om statusændringen
        firePropertyChange("status", oldStatus, newStatus);

        // Hvis en reservation afsluttes, opdater laptop og student tilstand
        if (oldStatus == ReservationStatusEnum.ACTIVE &&
                (newStatus == ReservationStatusEnum.COMPLETED || newStatus == ReservationStatusEnum.CANCELLED)) {

            // Gør laptopen tilgængelig igen
            Laptop reservedLaptop = laptop.get();
            if (reservedLaptop.isLoaned()) {
                reservedLaptop.changeState(AvailableState.INSTANCE);
            }

            // Opdater student har laptop status hvis nødvendigt
            Student reservedStudent = student.get();
            if (reservedStudent.isHasLaptop()) {
                reservedStudent.setHasLaptopToOpposite();
            }

            // Notificér om at reservationen er afsluttet
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
        return "Reservation: " + student.get().getName() + " - " +
                laptop.get().getModel() + " (" + status.get() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(reservationId.get(), that.reservationId.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId.get());
    }
}