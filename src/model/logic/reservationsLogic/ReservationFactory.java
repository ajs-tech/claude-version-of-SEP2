package model.logic.reservationsLogic;

import model.log.Log;
import model.models.Laptop;
import model.models.LoanedState;
import model.models.Reservation;
import model.models.Student;

import java.util.Observable;
import java.util.logging.Logger;

/**
 * Factory class for creating reservations.
 * Implements Factory Method pattern and Observable pattern.
 */
public class ReservationFactory extends Observable {
    private static final Logger logger = Logger.getLogger(ReservationFactory.class.getName());
    
    // Event types for observer notifications
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    
    // Singleton instance with lazy initialization
    private static ReservationFactory instance;
    private final Log log;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private ReservationFactory() {
        this.log = Log.getInstance();
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance
     */
    public static synchronized ReservationFactory getInstance() {
        if (instance == null) {
            instance = new ReservationFactory();
        }
        return instance;
    }
    
    /**
     * Creates a new reservation with proper setup of dependent objects.
     *
     * @param laptop  The laptop to loan
     * @param student The student borrowing the laptop
     * @return        The created reservation
     * @throws IllegalArgumentException if validation fails
     */
    public Reservation createReservation(Laptop laptop, Student student) {
        // Validate input
        validateInput(laptop, student);
        
        // Update laptop state
        laptop.changeState(new LoanedState());
        
        // Update student status
        student.setHasLaptop(true);
        
        // Create reservation
        Reservation reservation = new Reservation(student, laptop);
        
        // Log creation
        log.info("Reservation created with ID: " + reservation.getReservationId() +
                " >> model.models.Laptop [" + laptop.getBrand() + " " + laptop.getModel() +
                "] assigned to student [" + student.getName() + "]");
        
        // Notify observers
        setChanged();
        notifyObservers(new ReservationEvent(EVENT_RESERVATION_CREATED, reservation));
        
        return reservation;
    }
    
    /**
     * Validates input parameters for reservation creation.
     *
     * @param laptop  The laptop to check
     * @param student The student to check
     * @throws IllegalArgumentException if validation fails
     */
    private void validateInput(Laptop laptop, Student student) {
        if (laptop == null) {
            throw new IllegalArgumentException("model.models.Laptop cannot be null");
        }
        
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }
        
        if (!laptop.isAvailable()) {
            throw new IllegalArgumentException("model.models.Laptop is not available: " + laptop.getId());
        }
        
        if (student.isHasLaptop()) {
            throw new IllegalArgumentException("Student already has a laptop: " + student.getViaId());
        }
    }
    
    /**
     * Event class for reservation operations.
     */
    public static class ReservationEvent {
        private final String eventType;
        private final Reservation reservation;
        
        public ReservationEvent(String eventType, Reservation reservation) {
            this.eventType = eventType;
            this.reservation = reservation;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public Reservation getReservation() {
            return reservation;
        }
    }
}