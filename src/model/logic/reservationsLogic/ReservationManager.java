package model.logic.reservationsLogic;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.models.Laptop;
import model.util.ModelObservable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for reservation handling and queue administration.
 * Uses Observer pattern and threads for asynchronous operations.
 */
public class ReservationManager extends Observable implements Observer {
    private static final Logger logger = Logger.getLogger(ReservationManager.class.getName());
    
    // Event types for observer notifications
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String EVENT_RESERVATION_UPDATED = "RESERVATION_UPDATED";
    public static final String EVENT_RESERVATION_STATUS_CHANGED = "RESERVATION_STATUS_CHANGED";
    public static final String EVENT_QUEUE_SIZE_CHANGED = "QUEUE_SIZE_CHANGED";
    public static final String EVENT_ERROR = "ERROR";
    
    // Collections for in-memory state
    private final List<Reservation> activeReservations;
    private final GenericQueue highPerformanceQueue;
    private final GenericQueue lowPerformanceQueue;
    
    // DAO layer
    private final QueueDAO queueDAO;
    private final ReservationDAO reservationDAO;
    private final LaptopDAO laptopDAO;
    private final StudentDAO studentDAO;
    
    // Helper classes
    private final ReservationFactory reservationFactory;
    private final Log log;
    
    // Concurrency control
    private final ReadWriteLock reservationsLock;
    private final ExecutorService executor;
    
    // Singleton instance
    private static volatile ReservationManager instance;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private ReservationManager() {
        this.activeReservations = new ArrayList<>();
        this.highPerformanceQueue = new GenericQueue(PerformanceTypeEnum.HIGH);
        this.lowPerformanceQueue = new GenericQueue(PerformanceTypeEnum.LOW);
        
        // Initialize DAOs
        this.queueDAO = QueueDAO.getInstance();
        this.reservationDAO = ReservationDAO.getInstance();
        this.laptopDAO = LaptopDAO.getInstance();
        this.studentDAO = StudentDAO.getInstance();
        
        // Initialize helper classes
        this.reservationFactory = ReservationFactory.getInstance();
        this.log = Log.getInstance();
        
        // Initialize concurrency controls
        this.reservationsLock = new ReentrantReadWriteLock();
        this.executor = Executors.newFixedThreadPool(2);
        
        // Register as observer
        this.reservationFactory.addObserver(this);
        this.highPerformanceQueue.addObserver(this);
        this.lowPerformanceQueue.addObserver(this);
        this.reservationDAO.addObserver(this);
        this.queueDAO.addObserver(this);
        
        // Load data from database at startup
        loadFromDatabase();
    }
    
    /**
     * Gets the singleton instance with double-checked locking.
     *
     * @return The singleton instance
     */
    public static ReservationManager getInstance() {
        if (instance == null) {
            synchronized (ReservationManager.class) {
                if (instance == null) {
                    instance = new ReservationManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Loads initial data from the database.
     */
    private void loadFromDatabase() {
        executor.submit(() -> {
            try {
                loadReservationsFromDatabase();
                loadQueuesFromDatabase();
                
                logger.info("Initial data loaded from database");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error loading data from database: " + e.getMessage(), e);
                log.error("Error loading data from database: " + e.getMessage());
                
                // Notify observers
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Error loading data from database", e));
            }
        });
    }
    
    /**
     * Loads active reservations from the database.
     */
    private void loadReservationsFromDatabase() throws SQLException {
        List<Reservation> dbReservations = reservationDAO.getActiveReservations();
        
        reservationsLock.writeLock().lock();
        try {
            activeReservations.clear();
            activeReservations.addAll(dbReservations);
            
            // Add observers to each reservation
            for (Reservation reservation : activeReservations) {
                reservation.addObserver(this);
            }
            
            logger.info("Loaded " + dbReservations.size() + " active reservations from database");
            log.info("Loaded " + dbReservations.size() + " active reservations from database");
        } finally {
            reservationsLock.writeLock().unlock();
        }
    }
    
    /**
     * Loads queues from the database.
     */
    private void loadQueuesFromDatabase() throws SQLException {
        // Load low-performance queue
        List<Student> lowPerformanceStudents = queueDAO.getStudentsInQueue(PerformanceTypeEnum.LOW);
        for (Student student : lowPerformanceStudents) {
            lowPerformanceQueue.addToQueue(student);
        }
        
        // Load high-performance queue
        List<Student> highPerformanceStudents = queueDAO.getStudentsInQueue(PerformanceTypeEnum.HIGH);
        for (Student student : highPerformanceStudents) {
            highPerformanceQueue.addToQueue(student);
        }
        
        logger.info("Loaded " + lowPerformanceStudents.size() + " students in low-performance queue");
        logger.info("Loaded " + highPerformanceStudents.size() + " students in high-performance queue");
        log.info("Loaded queues from database: " + lowPerformanceStudents.size() +
                " in low-performance queue, " + highPerformanceStudents.size() + " in high-performance queue");
    }
    
    /**
     * Creates a reservation with database persistence.
     *
     * @param laptop  The laptop to loan
     * @param student The student borrowing the laptop
     * @return        The created reservation or null on error
     */
    public Reservation createReservation(Laptop laptop, Student student) {
        try {
            // Check preconditions
            if (laptop == null || student == null) {
                logger.warning("Null reference: model.models.Laptop or Student is null");
                log.warning("Error: Cannot create reservation with null references");
                return null;
            }
            
            if (!laptop.isAvailable()) {
                logger.warning("model.models.Laptop is not available: " + laptop.getId());
                log.warning("Error: Cannot create reservation with unavailable laptop: " + laptop.getBrand() + " " + laptop.getModel());
                return null;
            }
            
            if (student.isHasLaptop()) {
                logger.warning("Student already has a laptop: " + student.getViaId());
                log.warning("Error: Cannot create reservation for student who already has a laptop: " + student.getName());
                return null;
            }
            
            // Create reservation object using factory
            Reservation reservation = reservationFactory.createReservation(laptop, student);
            
            // Save to database with transaction support
            boolean success = reservationDAO.createReservationWithTransaction(reservation);
            
            if (success) {
                // Update in-memory list
                reservationsLock.writeLock().lock();
                try {
                    activeReservations.add(reservation);
                    
                    // Add observer to the new reservation
                    reservation.addObserver(this);
                } finally {
                    reservationsLock.writeLock().unlock();
                }
                
                // Notify observers
                setChanged();
                notifyObservers(new ReservationEvent(EVENT_RESERVATION_CREATED, reservation));
                
                return reservation;
            } else {
                logger.warning("Could not create reservation in database");
                log.warning("Error: Could not create reservation in database");
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating reservation: " + e.getMessage(), e);
            log.error("Error creating reservation: " + e.getMessage());
            
            // Notify observers
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error creating reservation", e));
            
            return null;
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid input for reservation: " + e.getMessage(), e);
            log.warning("Error creating reservation: " + e.getMessage());
            
            // Notify observers
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Invalid input for reservation", e));
            
            return null;
        }
    }
    
    /**
     * Updates a reservation status with database persistence.
     *
     * @param reservationId The reservation's UUID
     * @param newStatus     The new status
     * @return              true if the operation was successful
     */
    public boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus) {
        try {
            // Find the reservation in memory
            Reservation reservation = findReservationById(reservationId);
            
            if (reservation == null) {
                // If not in memory, try to get from database
                reservation = reservationDAO.getById(reservationId);
                if (reservation == null) {
                    logger.warning("Reservation not found: " + reservationId);
                    log.warning("Error: Reservation not found: " + reservationId);
                    return false;
                }
            }
            
            // Update status
            ReservationStatusEnum oldStatus = reservation.getStatus();
            reservation.changeStatus(newStatus);
            
            // Update in database with transaction support
            boolean success = reservationDAO.updateStatusWithTransaction(reservation);
            
            if (success) {
                // Remove from in-memory list if cancelled/completed
                if (newStatus == ReservationStatusEnum.CANCELLED ||
                        newStatus == ReservationStatusEnum.COMPLETED) {
                    
                    reservationsLock.writeLock().lock();
                    try {
                        activeReservations.removeIf(r -> r.getReservationId().equals(reservationId));
                    } finally {
                        reservationsLock.writeLock().unlock();
                    }
                }
                
                log.info("Reservation " + reservationId + " updated to status: " + newStatus);
                
                // Notify observers
                setChanged();
                notifyObservers(new StatusChangedEvent(EVENT_RESERVATION_STATUS_CHANGED, 
                        reservation, oldStatus, newStatus));
                
                return true;
            } else {
                logger.warning("Could not update reservation in database");
                log.warning("Error: Could not update reservation in database");
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating reservation: " + e.getMessage(), e);
            log.error("Error updating reservation: " + e.getMessage());
            
            // Notify observers
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error updating reservation", e));
            
            return false;
        }
    }
    
    /**
     * Finds a reservation by ID in the in-memory list.
     *
     * @param reservationId The reservation's UUID
     * @return              The reservation or null if not found
     */
    private Reservation findReservationById(UUID reservationId) {
        reservationsLock.readLock().lock();
        try {
            for (Reservation r : activeReservations) {
                if (r.getReservationId().equals(reservationId)) {
                    return r;
                }
            }
            return null;
        } finally {
            reservationsLock.readLock().unlock();
        }
    }
    
    /**
     * Adds a student to the high-performance queue with database persistence.
     *
     * @param student The student to add
     */
    public void addToHighPerformanceQueue(Student student) {
        executor.submit(() -> {
            try {
                // Check if student already has a laptop or is in a queue
                if (student.isHasLaptop()) {
                    logger.info("Student " + student.getName() + " already has a laptop, not adding to queue");
                    log.info("Student " + student.getName() + " already has a laptop, not adding to queue");
                    return;
                }
                
                if (queueDAO.isStudentInAnyQueue(student.getViaId())) {
                    logger.info("Student " + student.getName() + " is already in a queue");
                    log.info("Student " + student.getName() + " is already in a queue");
                    return;
                }
                
                // Check if student's performance need matches the queue
                if (student.getPerformanceNeeded() != PerformanceTypeEnum.HIGH) {
                    logger.info("Student " + student.getName() + " does not need high performance, redirecting");
                    addToLowPerformanceQueue(student);
                    return;
                }
                
                // Check if there's an available laptop with the right performance level
                List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(PerformanceTypeEnum.HIGH);
                if (!availableLaptops.isEmpty()) {
                    // There's an available laptop, assign it directly instead of adding to queue
                    Laptop laptop = availableLaptops.get(0);
                    createReservation(laptop, student);
                    logger.info("Student " + student.getName() + " was assigned a laptop directly instead of being added to queue");
                    log.info("Student " + student.getName() + " was assigned a laptop directly instead of being added to queue");
                    return;
                }
                
                // Add to database queue
                boolean added = queueDAO.addToQueue(student, PerformanceTypeEnum.HIGH);
                
                if (added) {
                    // Add to in-memory queue
                    highPerformanceQueue.addToQueue(student);
                    log.info("Student " + student.getName() + " added to high-performance queue");
                } else {
                    logger.warning("Could not add student to queue in database");
                    log.warning("Error: Could not add student to queue in database");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error adding to high-performance queue: " + e.getMessage(), e);
                log.error("Error adding to high-performance queue: " + e.getMessage());
                
                // Notify observers
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Error adding to high-performance queue", e));
            }
        });
    }
    
    /**
     * Adds a student to the low-performance queue with database persistence.
     *
     * @param student The student to add
     */
    public void addToLowPerformanceQueue(Student student) {
        executor.submit(() -> {
            try {
                // Check if student already has a laptop or is in a queue
                if (student.isHasLaptop()) {
                    logger.info("Student " + student.getName() + " already has a laptop, not adding to queue");
                    log.info("Student " + student.getName() + " already has a laptop, not adding to queue");
                    return;
                }
                
                if (queueDAO.isStudentInAnyQueue(student.getViaId())) {
                    logger.info("Student " + student.getName() + " is already in a queue");
                    log.info("Student " + student.getName() + " is already in a queue");
                    return;
                }
                
                // Check if student's performance need matches the queue
                if (student.getPerformanceNeeded() != PerformanceTypeEnum.LOW) {
                    logger.info("Student " + student.getName() + " needs high performance, redirecting");
                    addToHighPerformanceQueue(student);
                    return;
                }
                
                // Check if there's an available laptop with the right performance level
                List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(PerformanceTypeEnum.LOW);
                if (!availableLaptops.isEmpty()) {
                    // There's an available laptop, assign it directly instead of adding to queue
                    Laptop laptop = availableLaptops.get(0);
                    createReservation(laptop, student);
                    logger.info("Student " + student.getName() + " was assigned a laptop directly instead of being added to queue");
                    log.info("Student " + student.getName() + " was assigned a laptop directly instead of being added to queue");
                    return;
                }
                
                // Add to database queue
                boolean added = queueDAO.addToQueue(student, PerformanceTypeEnum.LOW);
                
                if (added) {
                    // Add to in-memory queue
                    lowPerformanceQueue.addToQueue(student);
                    log.info("Student " + student.getName() + " added to low-performance queue");
                } else {
                    logger.warning("Could not add student to queue in database");
                    log.warning("Error: Could not add student to queue in database");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error adding to low-performance queue: " + e.getMessage(), e);
                log.error("Error adding to low-performance queue: " + e.getMessage());
                
                // Notify observers
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Error adding to low-performance queue", e));
            }
        });
    }
    
    /**
     * Removes a student from a queue.
     *
     * @param viaId           The student's VIA ID
     * @param performanceType The performance type queue
     * @return                true if removal was successful
     */
    public boolean removeFromQueue(int viaId, PerformanceTypeEnum performanceType) {
        try {
            // Remove from database
            boolean removed = queueDAO.removeFromQueue(viaId, performanceType);
            
            if (removed) {
                // Remove from in-memory queue
                GenericQueue queue = (performanceType == PerformanceTypeEnum.HIGH) ?
                        highPerformanceQueue : lowPerformanceQueue;
                
                queue.removeStudentById(viaId);
                return true;
            }
            
            return removed;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error removing from queue: " + e.getMessage(), e);
            log.error("Error removing from queue: " + e.getMessage());
            
            // Notify observers
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error removing from queue", e));
            
            return false;
        }
    }
    
    /**
     * Assigns the next student in queue to an available laptop.
     *
     * @param laptop The laptop that has become available
     * @return       The created reservation or null if no students in queue
     */
    public Reservation assignNextStudentFromQueue(Laptop laptop) {
        PerformanceTypeEnum laptopType = laptop.getPerformanceType();
        GenericQueue queue;
        String queueTypeName;
        
        if (laptopType == PerformanceTypeEnum.HIGH) {
            queue = highPerformanceQueue;
            queueTypeName = "high-performance";
        } else {
            queue = lowPerformanceQueue;
            queueTypeName = "low-performance";
        }
        
        if (queue.getQueueSize() > 0) {
            try {
                // Get next student from database
                Student nextStudent = queueDAO.getAndRemoveNextInQueue(laptopType);
                
                if (nextStudent != null) {
                    // Update in-memory queue
                    queue.getAndRemoveNextInLine();
                    
                    // Create reservation
                    Reservation reservation = createReservation(laptop, nextStudent);
                    
                    if (reservation != null) {
                        log.info("Automatic assignment: Student " + nextStudent.getName() +
                                " assigned laptop " + laptop.getBrand() + " " + laptop.getModel() +
                                " from " + queueTypeName + " queue");
                        
                        return reservation;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error during automatic assignment from queue: " + e.getMessage(), e);
                log.error("Error during automatic assignment from queue: " + e.getMessage());
                
                // Notify observers
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Error during automatic assignment from queue", e));
            }
        }
        
        return null;
    }
    
    /**
     * Handles events from observed objects.
     */
    @Override
    public void update(Observable o, Object arg) {
        // Handle events from ReservationFactory
        if (o instanceof ReservationFactory && arg instanceof ReservationFactory.ReservationEvent) {
            ReservationFactory.ReservationEvent event = (ReservationFactory.ReservationEvent) arg;
            if (ReservationFactory.EVENT_RESERVATION_CREATED.equals(event.getEventType())) {
                // Forward event
                setChanged();
                notifyObservers(new ReservationEvent(EVENT_RESERVATION_CREATED, event.getReservation()));
            }
        }
        // Handle events from model.logic.reservationsLogic.GenericQueue
        else if (o instanceof GenericQueue && arg instanceof GenericQueue.QueueSizeEvent) {
            GenericQueue.QueueSizeEvent event = (GenericQueue.QueueSizeEvent) arg;
            if (GenericQueue.EVENT_QUEUE_SIZE_CHANGED.equals(event.getEventType())) {
                // Forward event with queue type info
                PerformanceTypeEnum queueType = ((GenericQueue) o).getPerformanceType();
                setChanged();
                notifyObservers(new QueueSizeEvent(EVENT_QUEUE_SIZE_CHANGED, 
                        queueType, event.getOldSize(), event.getNewSize()));
            }
        }
        // Handle events from Reservation objects
        else if (o instanceof Reservation) {
            if (arg instanceof ModelObservable.PropertyChangeInfo) {
                ModelObservable.PropertyChangeInfo event = (ModelObservable.PropertyChangeInfo) arg;
                // If status changed
                if ("status".equals(event.getPropertyName())) {
                    Reservation reservation = (Reservation) o;
                    ReservationStatusEnum oldStatus = (ReservationStatusEnum) event.getOldValue();
                    ReservationStatusEnum newStatus = (ReservationStatusEnum) event.getNewValue();
                    
                    // Forward event
                    setChanged();
                    notifyObservers(new StatusChangedEvent(EVENT_RESERVATION_STATUS_CHANGED, 
                            reservation, oldStatus, newStatus));
                    
                    // Handle completed or cancelled reservations
                    if (oldStatus == ReservationStatusEnum.ACTIVE && 
                            (newStatus == ReservationStatusEnum.COMPLETED || 
                             newStatus == ReservationStatusEnum.CANCELLED)) {
                        
                        // Remove from active reservations
                        reservationsLock.writeLock().lock();
                        try {
                            activeReservations.remove(reservation);
                        } finally {
                            reservationsLock.writeLock().unlock();
                        }
                        
                        // Check if there are students in the queue for this laptop type
                        assignNextStudentFromQueue(reservation.getLaptop());
                    }
                }
            }
        }
        // Handle events from DAO classes
        else if ((o instanceof ReservationDAO && arg instanceof ReservationDAO.DatabaseEvent) ||
                 (o instanceof QueueDAO && arg instanceof QueueDAO.DatabaseEvent)) {
            // Handle error events
            if (arg instanceof ReservationDAO.DatabaseEvent) {
                ReservationDAO.DatabaseEvent event = (ReservationDAO.DatabaseEvent) arg;
                if (ReservationDAO.EVENT_RESERVATION_ERROR.equals(event.getEventType())) {
                    // Forward error event
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_ERROR, event.getData().toString(), event.getException()));
                }
            } else if (arg instanceof QueueDAO.DatabaseEvent) {
                QueueDAO.DatabaseEvent event = (QueueDAO.DatabaseEvent) arg;
                if (QueueDAO.EVENT_QUEUE_ERROR.equals(event.getEventType())) {
                    // Forward error event
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_ERROR, event.getData().toString(), event.getException()));
                }
            }
        }
    }
    
    /**
     * Gets the number of active reservations.
     *
     * @return Number of active reservations
     */
    public int getActiveReservationsCount() {
        reservationsLock.readLock().lock();
        try {
            return activeReservations.size();
        } finally {
            reservationsLock.readLock().unlock();
        }
    }
    
    /**
     * Gets all active reservations.
     *
     * @return List of active reservations
     */
    public List<Reservation> getAllActiveReservations() {
        reservationsLock.readLock().lock();
        try {
            return new ArrayList<>(activeReservations);
        } finally {
            reservationsLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the size of the high-performance queue.
     *
     * @return Number of students in high-performance queue
     */
    public int getHighPerformanceQueueSize() {
        try {
            return queueDAO.getQueueSize(PerformanceTypeEnum.HIGH);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error getting high-performance queue size: " + e.getMessage(), e);
            // Fall back to in-memory queue size
            return highPerformanceQueue.getQueueSize();
        }
    }
    
    /**
     * Gets the size of the low-performance queue.
     *
     * @return Number of students in low-performance queue
     */
    public int getLowPerformanceQueueSize() {
        try {
            return queueDAO.getQueueSize(PerformanceTypeEnum.LOW);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error getting low-performance queue size: " + e.getMessage(), e);
            // Fall back to in-memory queue size
            return lowPerformanceQueue.getQueueSize();
        }
    }
    
    /**
     * Gets students in the high-performance queue.
     *
     * @return List of students in high-performance queue
     */
    public List<Student> getStudentsInHighPerformanceQueue() {
        try {
            return queueDAO.getStudentsInQueue(PerformanceTypeEnum.HIGH);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error getting students in high-performance queue: " + e.getMessage(), e);
            // Fall back to in-memory queue
            return highPerformanceQueue.getAllStudentsInQueue();
        }
    }
    
    /**
     * Gets students in the low-performance queue.
     *
     * @return List of students in low-performance queue
     */
    public List<Student> getStudentsInLowPerformanceQueue() {
        try {
            return queueDAO.getStudentsInQueue(PerformanceTypeEnum.LOW);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error getting students in low-performance queue: " + e.getMessage(), e);
            // Fall back to in-memory queue
            return lowPerformanceQueue.getAllStudentsInQueue();
        }
    }
    
    /**
     * Shutdown the manager and release resources.
     */
    public void shutdown() {
        // Remove observers
        reservationFactory.deleteObserver(this);
        highPerformanceQueue.deleteObserver(this);
        lowPerformanceQueue.deleteObserver(this);
        reservationDAO.deleteObserver(this);
        queueDAO.deleteObserver(this);
        
        // Shutdown executor
        executor.shutdown();
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
    
    /**
     * Event class for reservation status changes.
     */
    public static class StatusChangedEvent extends ReservationEvent {
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
    public static class QueueSizeEvent {
        private final String eventType;
        private final PerformanceTypeEnum queueType;
        private final int oldSize;
        private final int newSize;
        
        public QueueSizeEvent(String eventType, PerformanceTypeEnum queueType, int oldSize, int newSize) {
            this.eventType = eventType;
            this.queueType = queueType;
            this.oldSize = oldSize;
            this.newSize = newSize;
        }
        
        public String getEventType() {
            return eventType;
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
    public static class ErrorEvent {
        private final String eventType;
        private final String message;
        private final Exception exception;
        
        public ErrorEvent(String eventType, String message, Exception exception) {
            this.eventType = eventType;
            this.message = message;
            this.exception = exception;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Exception getException() {
            return exception;
        }
    }
}