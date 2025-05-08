

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.models.Laptop;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ViewModel for reservation operations following MVVM architecture.
 * Integrates with ReservationManager for reservation and queue management.
 * Implements Observer pattern and supports background threading.
 */
public class ReservationViewModel extends Observable implements Observer {
    private static final Logger logger = Logger.getLogger(ReservationViewModel.class.getName());
    
    // Event types for observer notifications
    public static final String EVENT_RESERVATIONS_LOADED = "RESERVATIONS_LOADED";
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String EVENT_RESERVATION_STATUS_UPDATED = "RESERVATION_STATUS_UPDATED";
    public static final String EVENT_QUEUE_STATUS_UPDATED = "QUEUE_STATUS_UPDATED";
    public static final String EVENT_OPERATION_ERROR = "OPERATION_ERROR";
    
    private final ReservationManager reservationManager;
    private final ReservationDAO reservationDAO;
    private final ExecutorService executor;
    
    /**
     * Creates a new ReservationViewModel.
     */
    public ReservationViewModel() {
        this.reservationManager = ReservationManager.getInstance();
        this.reservationDAO = ReservationDAO.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Register as observer
        this.reservationManager.addObserver(this);
        this.reservationDAO.addObserver(this);
        
        // Initial load is handled by the ReservationManager
    }
    
    /**
     * Gets all active reservations.
     * 
     * @return List of active reservations
     */
    public List<Reservation> getActiveReservations() {
        return reservationManager.getAllActiveReservations();
    }
    
    /**
     * Gets the number of active reservations.
     * 
     * @return Number of active reservations
     */
    public int getActiveReservationsCount() {
        return reservationManager.getActiveReservationsCount();
    }
    
    /**
     * Creates a reservation for a laptop and student.
     * 
     * @param laptop The laptop to loan
     * @param student The student borrowing the laptop
     * @return true if creation was successful
     */
    public boolean createReservation(Laptop laptop, Student student) {
        try {
            Reservation reservation = reservationManager.createReservation(laptop, student);
            return reservation != null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating reservation: " + e.getMessage(), e);
            
            // Notify observers about error
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error creating reservation", e));
            
            return false;
        }
    }
    
    /**
     * Updates a reservation status asynchronously.
     * 
     * @param reservationId The reservation's UUID
     * @param newStatus The new status
     */
    public void updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus) {
        executor.submit(() -> {
            try {
                boolean success = reservationManager.updateReservationStatus(reservationId, newStatus);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to update reservation status", null));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error updating reservation status: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error updating reservation status", e));
            }
        });
    }
    
    /**
     * Adds a student to the high-performance queue.
     * 
     * @param student The student to add
     */
    public void addToHighPerformanceQueue(Student student) {
        executor.submit(() -> {
            try {
                reservationManager.addToHighPerformanceQueue(student);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error adding to high-performance queue: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                        "Error adding to high-performance queue", e));
            }
        });
    }
    
    /**
     * Adds a student to the low-performance queue.
     * 
     * @param student The student to add
     */
    public void addToLowPerformanceQueue(Student student) {
        executor.submit(() -> {
            try {
                reservationManager.addToLowPerformanceQueue(student);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error adding to low-performance queue: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                        "Error adding to low-performance queue", e));
            }
        });
    }
    
    /**
     * Removes a student from a queue.
     * 
     * @param viaId The student's VIA ID
     * @param performanceType The performance type queue
     * @return true if removal was successful
     */
    public boolean removeFromQueue(int viaId, PerformanceTypeEnum performanceType) {
        try {
            return reservationManager.removeFromQueue(viaId, performanceType);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error removing from queue: " + e.getMessage(), e);
            
            // Notify observers about error
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error removing from queue", e));
            
            return false;
        }
    }
    
    /**
     * Gets the size of the high-performance queue.
     * 
     * @return Number of students in high-performance queue
     */
    public int getHighPerformanceQueueSize() {
        return reservationManager.getHighPerformanceQueueSize();
    }
    
    /**
     * Gets the size of the low-performance queue.
     * 
     * @return Number of students in low-performance queue
     */
    public int getLowPerformanceQueueSize() {
        return reservationManager.getLowPerformanceQueueSize();
    }
    
    /**
     * Gets students in the high-performance queue.
     * 
     * @return List of students in high-performance queue
     */
    public List<Student> getStudentsInHighPerformanceQueue() {
        return reservationManager.getStudentsInHighPerformanceQueue();
    }
    
    /**
     * Gets students in the low-performance queue.
     * 
     * @return List of students in low-performance queue
     */
    public List<Student> getStudentsInLowPerformanceQueue() {
        return reservationManager.getStudentsInLowPerformanceQueue();
    }
    
    /**
     * Updates the ViewModel when receiving events from observed objects.
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof ReservationManager) {
            if (arg instanceof ReservationManager.ReservationEvent) {
                ReservationManager.ReservationEvent event = (ReservationManager.ReservationEvent) arg;
                handleReservationManagerEvent(event);
            } else if (arg instanceof ReservationManager.ErrorEvent) {
                ReservationManager.ErrorEvent event = (ReservationManager.ErrorEvent) arg;
                
                // Forward error event
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, event.getMessage(), event.getException()));
            }
        } else if (o instanceof ReservationDAO) {
            if (arg instanceof ReservationDAO.DatabaseEvent) {
                ReservationDAO.DatabaseEvent event = (ReservationDAO.DatabaseEvent) arg;
                
                if (ReservationDAO.EVENT_RESERVATION_ERROR.equals(event.getEventType())) {
                    // Forward error event
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            event.getData().toString(), event.getException()));
                }
            }
        }
    }
    
    /**
     * Handles events from ReservationManager.
     * 
     * @param event The event to handle
     */
    private void handleReservationManagerEvent(ReservationManager.ReservationEvent event) {
        switch (event.getEventType()) {
            case ReservationManager.EVENT_RESERVATION_CREATED:
                // Forward event
                setChanged();
                notifyObservers(new ViewModelEvent(EVENT_RESERVATION_CREATED, event.getReservation()));
                
                // Update reservation count
                setChanged();
                notifyObservers(new CountsEvent("activeReservationsCount", getActiveReservationsCount()));
                break;
                
            case ReservationManager.EVENT_RESERVATION_STATUS_CHANGED:
                if (event instanceof ReservationManager.StatusChangedEvent) {
                    ReservationManager.StatusChangedEvent statusEvent = 
                            (ReservationManager.StatusChangedEvent) event;
                    
                    // Forward event
                    setChanged();
                    notifyObservers(new StatusChangedEvent(EVENT_RESERVATION_STATUS_UPDATED, 
                            statusEvent.getReservation(), 
                            statusEvent.getOldStatus(), 
                            statusEvent.getNewStatus()));
                    
                    // Update reservation count
                    setChanged();
                    notifyObservers(new CountsEvent("activeReservationsCount", getActiveReservationsCount()));
                }
                break;
                
            case ReservationManager.EVENT_QUEUE_SIZE_CHANGED:
                if (event instanceof ReservationManager.QueueSizeEvent) {
                    ReservationManager.QueueSizeEvent queueEvent = 
                            (ReservationManager.QueueSizeEvent) event;
                    
                    // Forward event
                    setChanged();
                    notifyObservers(new QueueSizeEvent(EVENT_QUEUE_STATUS_UPDATED, 
                            queueEvent.getQueueType(), 
                            queueEvent.getOldSize(), 
                            queueEvent.getNewSize()));
                    
                    // Update queue size counts
                    String countType = queueEvent.getQueueType() == PerformanceTypeEnum.HIGH ? 
                            "highPerformanceQueueSize" : "lowPerformanceQueueSize";
                    
                    setChanged();
                    notifyObservers(new CountsEvent(countType, queueEvent.getNewSize()));
                }
                break;
        }
    }
    
    /**
     * Closes resources when the ViewModel is no longer needed.
     */
    public void close() {
        reservationManager.deleteObserver(this);
        reservationDAO.deleteObserver(this);
        executor.shutdown();
    }
    
    /**
     * Event class for ViewModel operations.
     */
    public static class ViewModelEvent {
        private final String eventType;
        private final Object data;
        
        public ViewModelEvent(String eventType, Object data) {
            this.eventType = eventType;
            this.data = data;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public Object getData() {
            return data;
        }
    }
    
    /**
     * Event class for count updates.
     */
    public static class CountsEvent {
        private final String countType;
        private final int count;
        
        public CountsEvent(String countType, int count) {
            this.countType = countType;
            this.count = count;
        }
        
        public String getCountType() {
            return countType;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * Event class for status changes.
     */
    public static class StatusChangedEvent extends ViewModelEvent {
        private final ReservationStatusEnum oldStatus;
        private final ReservationStatusEnum newStatus;
        
        public StatusChangedEvent(String eventType, Reservation reservation, 
                                 ReservationStatusEnum oldStatus, ReservationStatusEnum newStatus) {
            super(eventType, reservation);
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }
        
        public ReservationStatusEnum getOldStatus() {
            return oldStatus;
        }
        
        public ReservationStatusEnum getNewStatus() {
            return newStatus;
        }
    }
    
    /**
     * Event class for queue size changes.
     */
    public static class QueueSizeEvent extends ViewModelEvent {
        private final PerformanceTypeEnum queueType;
        private final int oldSize;
        private final int newSize;
        
        public QueueSizeEvent(String eventType, PerformanceTypeEnum queueType, int oldSize, int newSize) {
            super(eventType, queueType);
            this.queueType = queueType;
            this.oldSize = oldSize;
            this.newSize = newSize;
        }
        
        public PerformanceTypeEnum getQueueType() {
            return queueType;
        }
        
        public int getOldSize() {
            return oldSize;
        }
        
        public int getNewSize() {
            return newSize;
        }
    }
    
    /**
     * Event class for errors.
     */
    public static class ErrorEvent extends ViewModelEvent {
        private final Exception exception;
        
        public ErrorEvent(String eventType, String message, Exception exception) {
            super(eventType, message);
            this.exception = exception;
        }
        
        public Exception getException() {
            return exception;
        }
    }
}