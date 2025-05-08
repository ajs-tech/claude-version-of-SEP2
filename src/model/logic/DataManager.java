package model.logic;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.log.Log;
import model.logic.laptopLogic.LaptopData;
import model.logic.reservationsLogic.ReservationManager;
import model.logic.studentLogic.StudentData;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager class that serves as facade to system functionality.
 * Implements Single Responsibility Principle by delegating tasks to specialized components.
 * Functions as entry point for ViewModels in MVVM architecture.
 */
public class DataManager extends Observable implements DataModel, Observer {
    private static final Logger logger = Logger.getLogger(DataManager.class.getName());

    // Event types for observer notifications
    public static final String EVENT_DATA_REFRESHED = "DATA_REFRESHED";
    public static final String EVENT_LAPTOP_CREATED = "LAPTOP_CREATED";
    public static final String EVENT_LAPTOP_UPDATED = "LAPTOP_UPDATED";
    public static final String EVENT_STUDENT_CREATED = "STUDENT_CREATED";
    public static final String EVENT_STUDENT_UPDATED = "STUDENT_UPDATED";
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String EVENT_RESERVATION_UPDATED = "RESERVATION_UPDATED";
    public static final String EVENT_ERROR = "ERROR";

    // Core components
    private final LaptopData laptopData;
    private final StudentData studentData;
    private final ReservationManager reservationManager;
    private final Log log;

    // Concurrency support
    private final ReadWriteLock operationLock;
    private final ExecutorService executor;

    // Singleton instance
    private static volatile DataManager instance;
    private static final Object lock = new Object();

    /**
     * Private constructor for Singleton pattern.
     * Initializes all components.
     */
    public DataManager() {
        // Initialize components
        this.laptopData = LaptopData.getInstance();
        this.studentData = StudentData.getInstance();
        this.reservationManager = ReservationManager.getInstance();
        this.log = Log.getInstance();

        // Initialize concurrency controls
        this.operationLock = new ReentrantReadWriteLock();
        this.executor = Executors.newCachedThreadPool();

        // Register as observer to all components
        this.laptopData.addObserver(this);
        this.studentData.addObserver(this);
        this.reservationManager.addObserver(this);

        log.info("DataManager initialized");
    }

    /**
     * Returns the existing instance or creates a new one
     *
     * @return DataManager instance
     */
    public static DataManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DataManager();
                }
            }
        }
        return instance;
    }

    /**
     * Reloads all caches from the database asynchronously
     */
    @Override
    public void refreshCaches() {
        executor.submit(() -> {
            try {
                log.info("Refreshing all caches...");

                // Delegate to specialized components
                // Each component will notify about its specific updates

                // Refresh laptop data
                executor.submit(() -> {
                    try {
                        laptopData.refreshCache();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error refreshing laptop cache: " + e.getMessage(), e);
                        log.error("Error refreshing laptop cache: " + e.getMessage());
                    }
                });

                // Refresh student data
                executor.submit(() -> {
                    try {
                        studentData.refreshCache();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error refreshing student cache: " + e.getMessage(), e);
                        log.error("Error refreshing student cache: " + e.getMessage());
                    }
                });

                // Refresh reservation data
                executor.submit(() -> {
                    try {
                        reservationManager.refreshData();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error refreshing reservation data: " + e.getMessage(), e);
                        log.error("Error refreshing reservation data: " + e.getMessage());
                    }
                });

                // Notify that refresh was initiated
                setChanged();
                notifyObservers(new DataEvent(EVENT_DATA_REFRESHED, null));

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during cache refresh: " + e.getMessage(), e);
                log.error("Error during cache refresh: " + e.getMessage());

                // Notify about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Error refreshing caches", e));
            }
        });
    }

    /**
     * Adds an observer to receive notifications
     *
     * @param o The observer to add
     */
    @Override
    public void addObserver(Observer o) {
        super.addObserver(o);
    }

    /**
     * Removes an observer
     *
     * @param o The observer to remove
     */
    @Override
    public void removeObserver(Observer o) {
        super.deleteObserver(o);
    }

    // ===============================
    // = Laptop Management Methods =
    // ===============================

    /**
     * Returns all laptops in the system
     *
     * @return List of all laptops
     */
    @Override
    public List<Laptop> getAllLaptops() {
        return laptopData.getAllLaptops();
    }

    /**
     * Returns the number of available laptops
     *
     * @return Number of available laptops
     */
    @Override
    public int getAmountOfAvailableLaptops() {
        return laptopData.getAmountOfAvailableLaptops();
    }

    /**
     * Returns the number of loaned laptops
     *
     * @return Number of loaned laptops
     */
    @Override
    public int getAmountOfLoanedLaptops() {
        return laptopData.getAmountOfLoanedLaptops();
    }

    /**
     * Finds an available laptop with a specific performance type
     *
     * @param performanceType Desired performance type (HIGH/LOW)
     * @return Available laptop or null if none found
     */
    @Override
    public Laptop findAvailableLaptop(PerformanceTypeEnum performanceType) {
        return laptopData.findAvailableLaptop(performanceType);
    }

    /**
     * Creates a new laptop
     *
     * @param brand           Laptop brand
     * @param model           Laptop model
     * @param gigabyte        Hard disk capacity in GB
     * @param ram             RAM in GB
     * @param performanceType Performance type (HIGH/LOW)
     * @return The created laptop or null on error
     */
    @Override
    public Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        Laptop laptop = laptopData.createLaptop(brand, model, gigabyte, ram, performanceType);

        if (laptop != null) {
            // Forward event
            setChanged();
            notifyObservers(new DataEvent(EVENT_LAPTOP_CREATED, laptop));
        }

        return laptop;
    }

    /**
     * Updates an existing laptop
     *
     * @param laptop The updated laptop
     * @return true if the operation was successful
     */
    @Override
    public boolean updateLaptop(Laptop laptop) {
        boolean success = laptopData.updateLaptop(laptop);

        if (success) {
            // Forward event
            setChanged();
            notifyObservers(new DataEvent(EVENT_LAPTOP_UPDATED, laptop));
        }

        return success;
    }

    /**
     * Gets a laptop by ID
     *
     * @param id Laptop UUID
     * @return The laptop if found, otherwise null
     */
    @Override
    public Laptop getLaptopById(UUID id) {
        return laptopData.getLaptopById(id);
    }

    // ===============================
    // = Student Management Methods =
    // ===============================

    /**
     * Returns all students in the system
     *
     * @return List of all students
     */
    @Override
    public List<Student> getAllStudents() {
        return studentData.getAllStudents();
    }

    /**
     * Returns the number of students in the system
     *
     * @return Number of students
     */
    @Override
    public int getStudentCount() {
        return studentData.getStudentCount();
    }

    /**
     * Finds a student based on VIA ID
     *
     * @param viaId ID to search for
     * @return Student if found, otherwise null
     */
    @Override
    public Student getStudentByID(int viaId) {
        return studentData.getStudentByID(viaId);
    }

    /**
     * Returns students with high performance needs
     *
     * @return List of students with high-performance needs
     */
    @Override
    public List<Student> getStudentWithHighPowerNeeds() {
        return studentData.getStudentWithHighPowerNeeds();
    }

    /**
     * Returns the number of students with high performance needs
     *
     * @return Number of students with high-performance needs
     */
    @Override
    public int getStudentCountOfHighPowerNeeds() {
        return studentData.getStudentCountOfHighPowerNeeds();
    }

    /**
     * Returns students with low performance needs
     *
     * @return List of students with low-performance needs
     */
    @Override
    public List<Student> getStudentWithLowPowerNeeds() {
        return studentData.getStudentWithLowPowerNeeds();
    }

    /**
     * Returns the number of students with low performance needs
     *
     * @return Number of students with low-performance needs
     */
    @Override
    public int getStudentCountOfLowPowerNeeds() {
        return studentData.getStudentCountOfLowPowerNeeds();
    }

    /**
     * Returns students who have a laptop
     *
     * @return List of students with laptop
     */
    @Override
    public List<Student> getThoseWhoHaveLaptop() {
        return studentData.getThoseWhoHaveLaptop();
    }

    /**
     * Returns the number of students who have a laptop
     *
     * @return Number of students with laptop
     */
    @Override
    public int getCountOfWhoHasLaptop() {
        return studentData.getCountOfWhoHasLaptop();
    }

    /**
     * Creates a new student with intelligent laptop assignment
     *
     * @param name              Student's name
     * @param degreeEndDate     Degree end date
     * @param degreeTitle       Degree title
     * @param viaId             VIA ID
     * @param email             Email address
     * @param phoneNumber       Phone number
     * @param performanceNeeded Performance needs (HIGH/LOW)
     * @return The created student or null on error
     */
    @Override
    public Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                                 int viaId, String email, int phoneNumber,
                                 PerformanceTypeEnum performanceNeeded) {
        Student student = studentData.createStudent(name, degreeEndDate, degreeTitle, viaId,
                email, phoneNumber, performanceNeeded);

        if (student != null) {
            // Forward event
            setChanged();
            notifyObservers(new DataEvent(EVENT_STUDENT_CREATED, student));

            // Intelligent assignment logic - try to assign a laptop
            Laptop availableLaptop = findAvailableLaptop(performanceNeeded);
            if (availableLaptop != null) {
                createReservation(availableLaptop, student);
            } else {
                // Add to queue if no laptop available
                if (performanceNeeded == PerformanceTypeEnum.HIGH) {
                    addToHighPerformanceQueue(student);
                } else {
                    addToLowPerformanceQueue(student);
                }
            }
        }

        return student;
    }

    /**
     * Updates an existing student
     *
     * @param student The updated student
     * @return true if the operation was successful
     */
    @Override
    public boolean updateStudent(Student student) {
        boolean success = studentData.updateStudent(student);

        if (success) {
            // Forward event
            setChanged();
            notifyObservers(new DataEvent(EVENT_STUDENT_UPDATED, student));
        }

        return success;
    }

    /**
     * Deletes a student
     *
     * @param viaId Student VIA ID
     * @return true if the operation was successful
     */
    @Override
    public boolean deleteStudent(int viaId) {
        return studentData.deleteStudent(viaId);
    }

    // ===================================
    // = Reservation Management Methods =
    // ===================================

    /**
     * Creates a new reservation
     *
     * @param laptop  The laptop to loan
     * @param student The student borrowing the laptop
     * @return The created reservation or null on error
     */
    @Override
    public Reservation createReservation(Laptop laptop, Student student) {
        Reservation reservation = reservationManager.createReservation(laptop, student);

        if (reservation != null) {
            // Forward event
            setChanged();
            notifyObservers(new DataEvent(EVENT_RESERVATION_CREATED, reservation));
        }

        return reservation;
    }

    /**
     * Updates a reservation status
     *
     * @param reservationId Reservation UUID
     * @param newStatus     The new status
     * @return true if the operation was successful
     */
    @Override
    public boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus) {
        boolean success = reservationManager.updateReservationStatus(reservationId, newStatus);

        if (success) {
            Reservation reservation = getReservationById(reservationId);
            if (reservation != null) {
                // Forward event
                setChanged();
                notifyObservers(new DataEvent(EVENT_RESERVATION_UPDATED, reservation));
            }
        }

        return success;
    }

    /**
     * Returns the number of active reservations
     *
     * @return Number of active reservations
     */
    @Override
    public int getAmountOfActiveReservations() {
        return reservationManager.getActiveReservationsCount();
    }

    /**
     * Returns all active reservations
     *
     * @return List of active reservations
     */
    @Override
    public List<Reservation> getAllActiveReservations() {
        return reservationManager.getAllActiveReservations();
    }

    /**
     * Gets a reservation by ID
     *
     * @param id Reservation UUID
     * @return The reservation if found, otherwise null
     */
    @Override
    public Reservation getReservationById(UUID id) {
        return reservationManager.getReservationById(id);
    }

    // =============================
    // = Queue Management Methods =
    // =============================

    /**
     * Adds a student to the high-performance queue
     *
     * @param student The student to add
     */
    @Override
    public void addToHighPerformanceQueue(Student student) {
        reservationManager.addToHighPerformanceQueue(student);
    }

    /**
     * Adds a student to the low-performance queue
     *
     * @param student The student to add
     */
    @Override
    public void addToLowPerformanceQueue(Student student) {
        reservationManager.addToLowPerformanceQueue(student);
    }

    /**
     * Returns the number of students in the high-performance queue
     *
     * @return Number of students in the queue
     */
    @Override
    public int getHighPerformanceQueueSize() {
        return reservationManager.getHighPerformanceQueueSize();
    }

    /**
     * Returns the number of students in the low-performance queue
     *
     * @return Number of students in the queue
     */
    @Override
    public int getLowPerformanceQueueSize() {
        return reservationManager.getLowPerformanceQueueSize();
    }

    /**
     * Returns students in the high-performance queue
     *
     * @return List of students in the queue
     */
    @Override
    public List<Student> getStudentsInHighPerformanceQueue() {
        return reservationManager.getStudentsInHighPerformanceQueue();
    }

    /**
     * Returns students in the low-performance queue
     *
     * @return List of students in the queue
     */
    @Override
    public List<Student> getStudentsInLowPerformanceQueue() {
        return reservationManager.getStudentsInLowPerformanceQueue();
    }

    /**
     * Removes a student from a queue
     *
     * @param viaId Student VIA ID
     * @param performanceType The queue type (HIGH/LOW)
     * @return true if removal was successful
     */
    @Override
    public boolean removeFromQueue(int viaId, PerformanceTypeEnum performanceType) {
        return reservationManager.removeFromQueue(viaId, performanceType);
    }

    /**
     * Handles updates from observed components
     */
    @Override
    public void update(Observable o, Object arg) {
        // Forward events to observers with appropriate transformations

        // Handle LaptopData events
        if (o instanceof LaptopData) {
            if (arg instanceof LaptopData.DataEvent) {
                LaptopData.DataEvent event = (LaptopData.DataEvent) arg;
                forwardEvent(event.getEventType(), event.getData());
            } else if (arg instanceof LaptopData.ErrorEvent) {
                LaptopData.ErrorEvent event = (LaptopData.ErrorEvent) arg;
                forwardError(event.getMessage(), event.getException());
            }
        }
        // Handle StudentData events
        else if (o instanceof StudentData) {
            if (arg instanceof StudentData.DataEvent) {
                StudentData.DataEvent event = (StudentData.DataEvent) arg;
                forwardEvent(event.getEventType(), event.getData());
            } else if (arg instanceof StudentData.ErrorEvent) {
                StudentData.ErrorEvent event = (StudentData.ErrorEvent) arg;
                forwardError(event.getMessage(), event.getException());
            }
        }
        // Handle ReservationManager events
        else if (o instanceof ReservationManager) {
            if (arg instanceof ReservationManager.ReservationEvent) {
                ReservationManager.ReservationEvent event = (ReservationManager.ReservationEvent) arg;
                forwardEvent(event.getEventType(), event.getReservation());
            } else if (arg instanceof ReservationManager.ErrorEvent) {
                ReservationManager.ErrorEvent event = (ReservationManager.ErrorEvent) arg;
                forwardError(event.getMessage(), event.getException());
            }
        }
    }

    /**
     * Forwards an event to observers
     *
     * @param eventType Event type
     * @param data Event data
     */
    private void forwardEvent(String eventType, Object data) {
        setChanged();
        notifyObservers(new DataEvent(eventType, data));
    }

    /**
     * Forwards an error to observers
     *
     * @param message Error message
     * @param exception Exception that occurred
     */
    private void forwardError(String message, Exception exception) {
        log.error(message);
        setChanged();
        notifyObservers(new ErrorEvent(EVENT_ERROR, message, exception));
    }

    /**
     * Closes resources and shuts down components
     */
    @Override
    public void close() {
        executor.shutdown();

        // Remove observers
        deleteObservers();

        // Close component resources
        laptopData.close();
        studentData.close();
        reservationManager.shutdown();

        log.info("DataManager resources released");
    }

    /**
     * Event class for data operations.
     */
    public static class DataEvent {
        private final String eventType;
        private final Object data;

        public DataEvent(String eventType, Object data) {
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