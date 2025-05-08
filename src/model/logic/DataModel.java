package model.logic;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Central interface for the laptop loan system.
 * Serves as facade to model layer in MVVM architecture.
 * ViewModels interact only with this interface.
 */
public interface DataModel {

    // ================= System Operations =================

    /**
     * Reloads all caches from the database
     */
    void refreshCaches();

    /**
     * Adds an observer to receive data change notifications
     *
     * @param listener The observer to add
     */
    void addObserver(java.util.Observer listener);

    /**
     * Removes an observer from data change notifications
     *
     * @param listener The observer to remove
     */
    void removeObserver(java.util.Observer listener);

    // ================= Laptop Management =================

    /**
     * Returns all laptops in the system
     * @return List of all laptops
     */
    List<Laptop> getAllLaptops();

    /**
     * Returns the number of available laptops
     * @return Number of available laptops
     */
    int getAmountOfAvailableLaptops();

    /**
     * Returns the number of loaned laptops
     * @return Number of loaned laptops
     */
    int getAmountOfLoanedLaptops();

    /**
     * Finds an available laptop with a specific performance type
     * @param performanceType Desired performance type (HIGH/LOW)
     * @return Available laptop or null if none found
     */
    Laptop findAvailableLaptop(PerformanceTypeEnum performanceType);

    /**
     * Creates a new laptop
     * @param brand           Laptop brand
     * @param model           Laptop model
     * @param gigabyte        Hard disk capacity in GB
     * @param ram             RAM in GB
     * @param performanceType Performance type (HIGH/LOW)
     * @return The created laptop or null on error
     */
    Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType);

    /**
     * Updates an existing laptop
     * @param laptop The updated laptop
     * @return true if the operation was successful
     */
    boolean updateLaptop(Laptop laptop);

    /**
     * Gets a laptop by ID
     *
     * @param id Laptop UUID
     * @return The laptop if found, otherwise null
     */
    Laptop getLaptopById(UUID id);

    // ================= Student Management =================

    /**
     * Returns all students in the system
     * @return List of all students
     */
    List<Student> getAllStudents();

    /**
     * Returns the number of students
     * @return Number of students
     */
    int getStudentCount();

    /**
     * Finds a student based on VIA ID
     * @param viaId ID to search for
     * @return Student if found, otherwise null
     */
    Student getStudentByID(int viaId);

    /**
     * Returns students with high performance needs
     * @return List of students with high-performance needs
     */
    List<Student> getStudentWithHighPowerNeeds();

    /**
     * Returns the number of students with high performance needs
     * @return Number of students with high-performance needs
     */
    int getStudentCountOfHighPowerNeeds();

    /**
     * Returns students with low performance needs
     * @return List of students with low-performance needs
     */
    List<Student> getStudentWithLowPowerNeeds();

    /**
     * Returns the number of students with low performance needs
     * @return Number of students with low-performance needs
     */
    int getStudentCountOfLowPowerNeeds();

    /**
     * Returns students who have a laptop
     * @return List of students with laptop
     */
    List<Student> getThoseWhoHaveLaptop();

    /**
     * Returns the number of students who have a laptop
     * @return Number of students with laptop
     */
    int getCountOfWhoHasLaptop();

    /**
     * Creates a new student with intelligent laptop assignment
     * @param name              Student's name
     * @param degreeEndDate     Degree end date
     * @param degreeTitle       Degree title
     * @param viaId             VIA ID
     * @param email             Email address
     * @param phoneNumber       Phone number
     * @param performanceNeeded Performance needs (HIGH/LOW)
     * @return The created student or null on error
     */
    Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                          int viaId, String email, int phoneNumber,
                          PerformanceTypeEnum performanceNeeded);

    /**
     * Updates an existing student
     * @param student The updated student
     * @return true if the operation was successful
     */
    boolean updateStudent(Student student);

    /**
     * Deletes a student
     *
     * @param viaId Student VIA ID
     * @return true if the operation was successful
     */
    boolean deleteStudent(int viaId);

    // ================= Reservation Management =================

    /**
     * Creates a new reservation
     * @param laptop  The laptop to loan
     * @param student The student borrowing the laptop
     * @return The created reservation or null on error
     */
    Reservation createReservation(Laptop laptop, Student student);

    /**
     * Updates a reservation status
     * @param reservationId Reservation UUID
     * @param newStatus     The new status
     * @return true if the operation was successful
     */
    boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus);

    /**
     * Returns the number of active reservations
     * @return Number of active reservations
     */
    int getAmountOfActiveReservations();

    /**
     * Returns all active reservations
     * @return List of active reservations
     */
    List<Reservation> getAllActiveReservations();

    /**
     * Gets a reservation by ID
     *
     * @param id Reservation UUID
     * @return The reservation if found, otherwise null
     */
    Reservation getReservationById(UUID id);

    // ================= Queue Management =================

    /**
     * Adds a student to the high-performance queue
     * @param student The student to add
     */
    void addToHighPerformanceQueue(Student student);

    /**
     * Adds a student to the low-performance queue
     * @param student The student to add
     */
    void addToLowPerformanceQueue(Student student);

    /**
     * Returns the number of students in the high-performance queue
     * @return Number of students in the queue
     */
    int getHighPerformanceQueueSize();

    /**
     * Returns the number of students in the low-performance queue
     * @return Number of students in the queue
     */
    int getLowPerformanceQueueSize();

    /**
     * Returns students in the high-performance queue
     * @return List of students in the queue
     */
    List<Student> getStudentsInHighPerformanceQueue();

    /**
     * Returns students in the low-performance queue
     * @return List of students in the queue
     */
    List<Student> getStudentsInLowPerformanceQueue();

    /**
     * Removes a student from a queue
     *
     * @param viaId Student VIA ID
     * @param performanceType The queue type (HIGH/LOW)
     * @return true if removal was successful
     */
    boolean removeFromQueue(int viaId, PerformanceTypeEnum performanceType);

    /**
     * Closes resources when the model is no longer needed
     */
    void close();
}