package model.events;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;
import model.util.EventBus;

import java.util.UUID;

/**
 * Definerer alle event-typer for systemet.
 * Bruges af EventBus til at dekoble komponenter.
 */
public class SystemEvents {

    /**
     * Base-interface for alle entitets-events
     */
    public interface EntityEvent extends EventBus.Event {
    }

    /**
     * Base-interface for alle operations-events
     */
    public interface OperationEvent extends EventBus.Event {
    }

    // ============= LAPTOP EVENTS =============

    /**
     * Base-interface for alle laptop-relaterede events
     */
    public interface LaptopEvent extends EntityEvent {
        Laptop getLaptop();
    }

    /**
     * Event der udløses når en laptop oprettes
     */
    public static class LaptopCreatedEvent implements LaptopEvent {
        private final Laptop laptop;

        public LaptopCreatedEvent(Laptop laptop) {
            this.laptop = laptop;
        }

        @Override
        public Laptop getLaptop() {
            return laptop;
        }
    }

    /**
     * Event der udløses når en laptop opdateres
     */
    public static class LaptopUpdatedEvent implements LaptopEvent {
        private final Laptop laptop;

        public LaptopUpdatedEvent(Laptop laptop) {
            this.laptop = laptop;
        }

        @Override
        public Laptop getLaptop() {
            return laptop;
        }
    }

    /**
     * Event der udløses når en laptop slettes
     */
    public static class LaptopDeletedEvent implements LaptopEvent {
        private final Laptop laptop;

        public LaptopDeletedEvent(Laptop laptop) {
            this.laptop = laptop;
        }

        @Override
        public Laptop getLaptop() {
            return laptop;
        }
    }

    /**
     * Event der udløses når en laptop skifter tilstand
     */
    public static class LaptopStateChangedEvent implements LaptopEvent {
        private final Laptop laptop;
        private final String oldState;
        private final String newState;
        private final boolean isNowAvailable;

        public LaptopStateChangedEvent(Laptop laptop, String oldState, String newState, boolean isNowAvailable) {
            this.laptop = laptop;
            this.oldState = oldState;
            this.newState = newState;
            this.isNowAvailable = isNowAvailable;
        }

        @Override
        public Laptop getLaptop() {
            return laptop;
        }

        public String getOldState() {
            return oldState;
        }

        public String getNewState() {
            return newState;
        }

        public boolean isNowAvailable() {
            return isNowAvailable;
        }
    }

    // ============= STUDENT EVENTS =============

    /**
     * Base-interface for alle student-relaterede events
     */
    public interface StudentEvent extends EntityEvent {
        Student getStudent();
    }

    /**
     * Event der udløses når en student oprettes
     */
    public static class StudentCreatedEvent implements StudentEvent {
        private final Student student;

        public StudentCreatedEvent(Student student) {
            this.student = student;
        }

        @Override
        public Student getStudent() {
            return student;
        }
    }

    /**
     * Event der udløses når en student opdateres
     */
    public static class StudentUpdatedEvent implements StudentEvent {
        private final Student student;

        public StudentUpdatedEvent(Student student) {
            this.student = student;
        }

        @Override
        public Student getStudent() {
            return student;
        }
    }

    /**
     * Event der udløses når en student slettes
     */
    public static class StudentDeletedEvent implements StudentEvent {
        private final Student student;

        public StudentDeletedEvent(Student student) {
            this.student = student;
        }

        @Override
        public Student getStudent() {
            return student;
        }
    }

    /**
     * Event der udløses når en students hasLaptop status ændres
     */
    public static class StudentHasLaptopChangedEvent implements StudentEvent {
        private final Student student;
        private final boolean oldHasLaptop;
        private final boolean newHasLaptop;

        public StudentHasLaptopChangedEvent(Student student, boolean oldHasLaptop, boolean newHasLaptop) {
            this.student = student;
            this.oldHasLaptop = oldHasLaptop;
            this.newHasLaptop = newHasLaptop;
        }

        @Override
        public Student getStudent() {
            return student;
        }

        public boolean getOldHasLaptop() {
            return oldHasLaptop;
        }

        public boolean getNewHasLaptop() {
            return newHasLaptop;
        }
    }

    // ============= RESERVATION EVENTS =============

    /**
     * Base-interface for alle reservations-relaterede events
     */
    public interface ReservationEvent extends EntityEvent {
        Reservation getReservation();
    }

    /**
     * Event der udløses når en reservation oprettes
     */
    public static class ReservationCreatedEvent implements ReservationEvent {
        private final Reservation reservation;

        public ReservationCreatedEvent(Reservation reservation) {
            this.reservation = reservation;
        }

        @Override
        public Reservation getReservation() {
            return reservation;
        }
    }

    /**
     * Event der udløses når en reservations status ændres
     */
    public static class ReservationStatusChangedEvent implements ReservationEvent {
        private final Reservation reservation;
        private final ReservationStatusEnum oldStatus;
        private final ReservationStatusEnum newStatus;

        public ReservationStatusChangedEvent(Reservation reservation,
                                             ReservationStatusEnum oldStatus,
                                             ReservationStatusEnum newStatus) {
            this.reservation = reservation;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        @Override
        public Reservation getReservation() {
            return reservation;
        }

        public ReservationStatusEnum getOldStatus() {
            return oldStatus;
        }

        public ReservationStatusEnum getNewStatus() {
            return newStatus;
        }

        public boolean isCompleted() {
            return newStatus == ReservationStatusEnum.COMPLETED;
        }

        public boolean isCancelled() {
            return newStatus == ReservationStatusEnum.CANCELLED;
        }
    }

    // ============= QUEUE EVENTS =============

    /**
     * Base-interface for alle kø-relaterede events
     */
    public interface QueueEvent extends OperationEvent {
        PerformanceTypeEnum getQueueType();
    }

    /**
     * Event der udløses når en student tilføjes til en kø
     */
    public static class StudentAddedToQueueEvent implements QueueEvent, StudentEvent {
        private final Student student;
        private final PerformanceTypeEnum queueType;
        private final int queueSize;

        public StudentAddedToQueueEvent(Student student, PerformanceTypeEnum queueType, int queueSize) {
            this.student = student;
            this.queueType = queueType;
            this.queueSize = queueSize;
        }

        @Override
        public Student getStudent() {
            return student;
        }

        @Override
        public PerformanceTypeEnum getQueueType() {
            return queueType;
        }

        public int getQueueSize() {
            return queueSize;
        }
    }

    /**
     * Event der udløses når en student fjernes fra en kø
     */
    public static class StudentRemovedFromQueueEvent implements QueueEvent, StudentEvent {
        private final Student student;
        private final PerformanceTypeEnum queueType;
        private final int queueSize;
        private final boolean wasAssignedLaptop;

        public StudentRemovedFromQueueEvent(Student student, PerformanceTypeEnum queueType,
                                            int queueSize, boolean wasAssignedLaptop) {
            this.student = student;
            this.queueType = queueType;
            this.queueSize = queueSize;
            this.wasAssignedLaptop = wasAssignedLaptop;
        }

        @Override
        public Student getStudent() {
            return student;
        }

        @Override
        public PerformanceTypeEnum getQueueType() {
            return queueType;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public boolean wasAssignedLaptop() {
            return wasAssignedLaptop;
        }
    }

    // ============= SYSTEM OPERATION EVENTS =============

    /**
     * Event der udløses når systemet starter op
     */
    public static class SystemStartupEvent implements OperationEvent {
        private final long timestamp;

        public SystemStartupEvent() {
            this.timestamp = System.currentTimeMillis();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Event der udløses når systemet lukker ned
     */
    public static class SystemShutdownEvent implements OperationEvent {
        private final long timestamp;

        public SystemShutdownEvent() {
            this.timestamp = System.currentTimeMillis();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Event der udløses når der opstår en databasefejl
     */
    public static class DatabaseErrorEvent implements OperationEvent {
        private final String errorMessage;
        private final String sqlState;
        private final Exception exception;

        public DatabaseErrorEvent(String errorMessage, String sqlState, Exception exception) {
            this.errorMessage = errorMessage;
            this.sqlState = sqlState;
            this.exception = exception;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getSqlState() {
            return sqlState;
        }

        public Exception getException() {
            return exception;
        }
    }
}