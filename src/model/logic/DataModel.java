package model.logic;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.logic.reservationsLogic.ReservationManager;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;
import model.util.PropertyChangeNotifier;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Centralt interface for laptopudlånssystem.
 * Fungerer som facade til modellag i MVVM-arkitekturen.
 * ViewModels interagerer kun med dette interface.
 */
public interface DataModel extends PropertyChangeNotifier {

    // ================= System Operations =================

    /**
     * Genindlæser alle caches fra databasen
     */
    void refreshCaches();

    // ================= Laptop Management =================

    /**
     * Returnerer alle laptops i systemet
     * @return Liste af alle laptops
     */
    List<Laptop> getAllLaptops();

    /**
     * Returnerer antal tilgængelige laptops
     * @return Antal tilgængelige laptops
     */
    int getAmountOfAvailableLaptops();

    /**
     * Returnerer antal udlånte laptops
     * @return Antal udlånte laptops
     */
    int getAmountOfLoanedLaptops();

    /**
     * Finder en tilgængelig laptop med en specifik ydelsestype
     * @param performanceType Ønsket ydelsestype (HIGH/LOW)
     * @return Tilgængelig laptop eller null hvis ingen findes
     */
    Laptop findAvailableLaptop(PerformanceTypeEnum performanceType);

    /**
     * Opretter en ny laptop
     * @param brand           Laptopens mærke
     * @param model           Laptopens model
     * @param gigabyte        Harddiskkapacitet i GB
     * @param ram             RAM i GB
     * @param performanceType Ydelsestype (HIGH/LOW)
     * @return Den oprettede laptop eller null ved fejl
     */
    Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType);

    /**
     * Opdaterer en eksisterende laptop
     * @param laptop Den opdaterede laptop
     * @return true hvis operationen lykkedes
     */
    boolean updateLaptop(Laptop laptop);

    // ================= Student Management =================

    /**
     * Returnerer alle studerende i systemet
     * @return Liste af alle studerende
     */
    List<Student> getAllStudents();

    /**
     * Returnerer antal studerende i systemet
     * @return Antal studerende
     */
    int getStudentCount();

    /**
     * Finder en student baseret på VIA ID
     * @param viaId ID at søge efter
     * @return Student hvis fundet, ellers null
     */
    Student getStudentByID(int viaId);

    /**
     * Returnerer studerende med behov for høj ydelse
     * @return Liste af studerende med høj-ydelse behov
     */
    List<Student> getStudentWithHighPowerNeeds();

    /**
     * Returnerer antal studerende med behov for høj ydelse
     * @return Antal studerende med høj-ydelse behov
     */
    int getStudentCountOfHighPowerNeeds();

    /**
     * Returnerer studerende med behov for lav ydelse
     * @return Liste af studerende med lav-ydelse behov
     */
    List<Student> getStudentWithLowPowerNeeds();

    /**
     * Returnerer antal studerende med behov for lav ydelse
     * @return Antal studerende med lav-ydelse behov
     */
    int getStudentCountOfLowPowerNeeds();

    /**
     * Returnerer studerende der har en laptop
     * @return Liste af studerende med laptop
     */
    List<Student> getThoseWhoHaveLaptop();

    /**
     * Returnerer antal studerende der har en laptop
     * @return Antal studerende med laptop
     */
    int getCountOfWhoHasLaptop();

    /**
     * Opretter en ny student med intelligent tildeling af laptop
     * @param name              Studentens navn
     * @param degreeEndDate     Slutdato for uddannelse
     * @param degreeTitle       Uddannelsestitel
     * @param viaId             VIA ID
     * @param email             Email-adresse
     * @param phoneNumber       Telefonnummer
     * @param performanceNeeded Behov for laptoptype
     * @return Den oprettede student eller null ved fejl
     */
    Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                          int viaId, String email, int phoneNumber,
                          PerformanceTypeEnum performanceNeeded);

    /**
     * Opdaterer en eksisterende student
     * @param student Den opdaterede student
     * @return true hvis operationen lykkedes
     */
    boolean updateStudent(Student student);

    // ================= Reservation Management =================

    /**
     * Giver adgang til ReservationManager-objektet
     * @return ReservationManager-instansen
     */
    ReservationManager getReservationManager();

    /**
     * Opretter en ny reservation
     * @param laptop  Laptopen der skal udlånes
     * @param student Studenten der skal låne laptopen
     * @return Den oprettede reservation eller null ved fejl
     */
    Reservation createReservation(Laptop laptop, Student student);

    /**
     * Opdaterer en reservations status
     * @param reservationId Reservationens UUID
     * @param newStatus     Den nye status
     * @return true hvis operationen lykkedes
     */
    boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus);

    /**
     * Returnerer antal aktive reservationer
     * @return Antal aktive reservationer
     */
    int getAmountOfActiveReservations();

    /**
     * Returnerer alle aktive reservationer
     * @return Liste af aktive reservationer
     */
    List<Reservation> getAllActiveReservations();

    // ================= Queue Management =================

    /**
     * Tilføjer en student til høj-ydelses køen
     * @param student Studenten der skal tilføjes
     */
    void addToHighPerformanceQueue(Student student);

    /**
     * Tilføjer en student til lav-ydelses køen
     * @param student Studenten der skal tilføjes
     */
    void addToLowPerformanceQueue(Student student);

    /**
     * Returnerer antal studerende i høj-ydelses køen
     * @return Antal studerende i køen
     */
    int getHighNeedingQueueSize();

    /**
     * Returnerer antal studerende i lav-ydelses køen
     * @return Antal studerende i køen
     */
    int getLowNeedingQueueSize();

    /**
     * Returnerer studerende i høj-ydelses køen
     * @return Liste af studerende i køen
     */
    List<Student> getStudentsInHighPerformanceQueue();

    /**
     * Returnerer studerende i lav-ydelses køen
     * @return Liste af studerende i køen
     */
    List<Student> getStudentsInLowPerformanceQueue();

}
