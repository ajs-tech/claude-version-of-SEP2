package model.logic;

import model.database.LaptopDAO;
import model.database.QueueDAO;
import model.database.ReservationDAO;
import model.database.StudentDAO;
import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.log.Log;
import model.logic.reservationsLogic.ReservationManager;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager-klasse der fungerer som facade til systemets funktionalitet.
 * Implementerer the Single Responsibility Principle ved at delegere opgaver til specialiserede komponenter.
 * Fungerer som entry point for ViewModels i MVVM-arkitekturen.
 */
public class DataManager implements PropertyChangeListener, PropertyChangeNotifier {
    private static final Logger logger = Logger.getLogger(DataManager.class.getName());

    // Core components
    private final ReservationManager reservationManager;
    private final Log log;

    // DAOs
    private final LaptopDAO laptopDAO;
    private final StudentDAO studentDAO;
    private final ReservationDAO reservationDAO;
    private final QueueDAO queueDAO;

    // Caches
    private final List<Laptop> laptopCache;
    private final List<Student> studentCache;

    // Support for property change events
    private final PropertyChangeSupport changeSupport;

    /**
     * Opretter en ny DataManager instans.
     * Fungerer som central adgangspunkt til systemet.
     */
    public DataManager() {
        // Initialiser komponenterne
        this.reservationManager = new ReservationManager();
        this.log = Log.getInstance();

        // Initialiser DAOs
        this.laptopDAO = new LaptopDAO();
        this.studentDAO = new StudentDAO();
        this.reservationDAO = new ReservationDAO();
        this.queueDAO = new QueueDAO();

        // Initialiser caches
        this.laptopCache = new ArrayList<>();
        this.studentCache = new ArrayList<>();

        // Initialiser PropertyChangeSupport
        this.changeSupport = new PropertyChangeSupport(this);

        // Tilføj DataManager som listener til ReservationManager
        this.reservationManager.addPropertyChangeListener(this);

        // Indlæs caches fra database
        refreshCaches();

        log.addToLog("DataManager initialiseret");
    }

    /**
     * Genindlæser alle caches fra databasen
     */
    public void refreshCaches() {
        try {
            // Laptop cache
            laptopCache.clear();
            laptopCache.addAll(laptopDAO.getAll());

            // Student cache
            studentCache.clear();
            studentCache.addAll(studentDAO.getAll());

            // Notificér om opdateringer
            firePropertyChange("laptopsRefreshed", null, laptopCache.size());
            firePropertyChange("studentsRefreshed", null, studentCache.size());

            log.addToLog("Caches opdateret: " + laptopCache.size() + " laptops, " +
                    studentCache.size() + " studerende");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved genopfriskning af caches: " + e.getMessage(), e);
            log.addToLog("Fejl ved genopfriskning af caches: " + e.getMessage());
        }
    }

    // ===============================
    // = Laptop Management Methods =
    // ===============================

    /**
     * Returnerer alle laptops i systemet
     *
     * @return Liste af alle laptops
     */
    public List<Laptop> getAllLaptops() {
        try {
            return laptopDAO.getAll();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af laptops: " + e.getMessage(), e);
            log.addToLog("Fejl ved hentning af laptops: " + e.getMessage());
            // Returner cache som fallback
            return new ArrayList<>(laptopCache);
        }
    }

    /**
     * Returnerer antal tilgængelige laptops
     *
     * @return Antal tilgængelige laptops
     */
    public int getAmountOfAvailableLaptops() {
        int count = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returnerer antal udlånte laptops
     *
     * @return Antal udlånte laptops
     */
    public int getAmountOfLoanedLaptops() {
        int count = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.isLoaned()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Finder en tilgængelig laptop med en specifik ydelsestype
     *
     * @param performanceType Ønsket ydelsestype (HIGH/LOW)
     * @return Tilgængelig laptop eller null hvis ingen findes
     */
    public Laptop findAvailableLaptop(PerformanceTypeEnum performanceType) {
        try {
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(performanceType);
            return availableLaptops.isEmpty() ? null : availableLaptops.get(0);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved søgning efter tilgængelig laptop: " + e.getMessage(), e);
            log.addToLog("Fejl ved søgning efter tilgængelig laptop: " + e.getMessage());

            // Fallback til cache-søgning
            for (Laptop laptop : laptopCache) {
                if (laptop.isAvailable() && laptop.getPerformanceType() == performanceType) {
                    return laptop;
                }
            }
            return null;
        }
    }

    /**
     * Opretter en ny laptop
     *
     * @param brand           Laptopens mærke
     * @param model           Laptopens model
     * @param gigabyte        Harddiskkapacitet i GB
     * @param ram             RAM i GB
     * @param performanceType Ydelsestype (HIGH/LOW)
     * @return Den oprettede laptop eller null ved fejl
     */
    public Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        try {
            // Tjek for valide input
            if (brand == null || brand.trim().isEmpty() ||
                    model == null || model.trim().isEmpty() ||
                    gigabyte <= 0 || ram <= 0 || performanceType == null) {
                log.addToLog("Fejl: Ugyldige laptop-data");
                return null;
            }

            // Opret laptop-objekt
            Laptop laptop = new Laptop(brand, model, gigabyte, ram, performanceType);

            // Tilføj listener til laptop
            laptop.addPropertyChangeListener(this);

            // Gem i database
            boolean success = laptopDAO.insert(laptop);

            if (success) {
                // Tilføj til cache
                laptopCache.add(laptop);

                log.addToLog("Laptop oprettet: " + laptop.getBrand() + " " + laptop.getModel());
                firePropertyChange("laptopCreated", null, laptop);

                return laptop;
            } else {
                log.addToLog("Fejl: Kunne ikke gemme laptop i database");
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved oprettelse af laptop: " + e.getMessage(), e);
            log.addToLog("Fejl ved oprettelse af laptop: " + e.getMessage());
            return null;
        }
    }

    /**
     * Opdaterer en eksisterende laptop
     *
     * @param laptop Den opdaterede laptop
     * @return true hvis operationen lykkedes
     */
    public boolean updateLaptop(Laptop laptop) {
        try {
            boolean success = laptopDAO.update(laptop);

            if (success) {
                log.addToLog("Laptop opdateret: " + laptop.getBrand() + " " + laptop.getModel());
                firePropertyChange("laptopUpdated", null, laptop);

                // Opdater cache
                refreshCaches();
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af laptop: " + e.getMessage(), e);
            log.addToLog("Fejl ved opdatering af laptop: " + e.getMessage());
            return false;
        }
    }

    // ===============================
    // = Student Management Methods =
    // ===============================

    /**
     * Returnerer alle studerende i systemet
     *
     * @return Liste af alle studerende
     */
    public List<Student> getAllStudents() {
        try {
            return studentDAO.getAll();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af studerende: " + e.getMessage(), e);
            log.addToLog("Fejl ved hentning af studerende: " + e.getMessage());
            // Returner cache som fallback
            return new ArrayList<>(studentCache);
        }
    }

    /**
     * Returnerer antal studerende i systemet
     *
     * @return Antal studerende
     */
    public int getStudentCount() {
        return studentCache.size();
    }

    /**
     * Finder en student baseret på VIA ID
     *
     * @param viaId ID at søge efter
     * @return Student hvis fundet, ellers null
     */
    public Student getStudentByID(int viaId) {
        // Søg først i cache
        for (Student student : studentCache) {
            if (student.getViaId() == viaId) {
                return student;
            }
        }

        // Hvis ikke fundet, søg i database
        try {
            return studentDAO.getById(viaId);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved søgning efter student med ID " + viaId + ": " + e.getMessage(), e);
            log.addToLog("Fejl ved søgning efter student med ID " + viaId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returnerer studerende med behov for høj ydelse
     *
     * @return Liste af studerende med høj-ydelse behov
     */
    public List<Student> getStudentWithHighPowerNeeds() {
        List<Student> highPowerStudents = new ArrayList<>();

        for (Student student : studentCache) {
            if (student.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                highPowerStudents.add(student);
            }
        }

        return highPowerStudents;
    }

    /**
     * Returnerer antal studerende med behov for høj ydelse
     *
     * @return Antal studerende med høj-ydelse behov
     */
    public int getStudentCountOfHighPowerNeeds() {
        return getStudentWithHighPowerNeeds().size();
    }

    /**
     * Returnerer studerende med behov for lav ydelse
     *
     * @return Liste af studerende med lav-ydelse behov
     */
    public List<Student> getStudentWithLowPowerNeeds() {
        List<Student> lowPowerStudents = new ArrayList<>();

        for (Student student : studentCache) {
            if (student.getPerformanceNeeded() == PerformanceTypeEnum.LOW) {
                lowPowerStudents.add(student);
            }
        }

        return lowPowerStudents;
    }

    /**
     * Returnerer antal studerende med behov for lav ydelse
     *
     * @return Antal studerende med lav-ydelse behov
     */
    public int getStudentCountOfLowPowerNeeds() {
        return getStudentWithLowPowerNeeds().size();
    }

    /**
     * Returnerer studerende der har en laptop
     *
     * @return Liste af studerende med laptop
     */
    public List<Student> getThoseWhoHaveLaptop() {
        List<Student> studentsWithLaptop = new ArrayList<>();

        for (Student student : studentCache) {
            if (student.isHasLaptop()) {
                studentsWithLaptop.add(student);
            }
        }

        return studentsWithLaptop;
    }

    /**
     * Returnerer antal studerende der har en laptop
     *
     * @return Antal studerende med laptop
     */
    public int getCountOfWhoHasLaptop() {
        return getThoseWhoHaveLaptop().size();
    }

    /**
     * Opretter en ny student med intelligent tildeling af laptop
     *
     * @param name              Studentens navn
     * @param degreeEndDate     Slutdato for uddannelse
     * @param degreeTitle       Uddannelsestitel
     * @param viaId             VIA ID
     * @param email             Email-adresse
     * @param phoneNumber       Telefonnummer
     * @param performanceNeeded Behov for laptoptype
     * @return Den oprettede student eller null ved fejl
     */
    public Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                                 int viaId, String email, int phoneNumber,
                                 PerformanceTypeEnum performanceNeeded) {
        try {
            // Tjek for valide input
            if (name == null || name.trim().isEmpty() ||
                    degreeEndDate == null ||
                    degreeTitle == null || degreeTitle.trim().isEmpty() ||
                    viaId <= 0 ||
                    email == null || email.trim().isEmpty() ||
                    phoneNumber <= 0 ||
                    performanceNeeded == null) {

                log.addToLog("Fejl: Ugyldige student-data");
                return null;
            }

            // Opret student-objekt
            Student student = new Student(name, degreeEndDate, degreeTitle, viaId,
                    email, phoneNumber, performanceNeeded);

            // Tilføj listener til student
            student.addPropertyChangeListener(this);

            // Gem i database
            boolean success = studentDAO.insert(student);

            if (success) {
                // Tilføj til cache
                studentCache.add(student);

                log.addToLog("Student oprettet: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
                firePropertyChange("studentCreated", null, student);

                // Intelligent tildeling af laptop eller tilføjelse til kø
                Laptop availableLaptop = findAvailableLaptop(student.getPerformanceNeeded());

                if (availableLaptop != null) {
                    // Tildel laptop med det samme
                    reservationManager.createReservation(availableLaptop, student);
                } else {
                    // Tilføj til kø
                    if (PerformanceTypeEnum.LOW.equals(student.getPerformanceNeeded())) {
                        reservationManager.addToLowPerformanceQueue(student);
                    } else if (PerformanceTypeEnum.HIGH.equals(student.getPerformanceNeeded())) {
                        reservationManager.addToHighPerformanceQueue(student);
                    }
                }

                return student;
            } else {
                log.addToLog("Fejl: Kunne ikke gemme student i database");
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved oprettelse af student: " + e.getMessage(), e);
            log.addToLog("Fejl ved oprettelse af student: " + e.getMessage());
            return null;
        }
    }

    /**
     * Opdaterer en eksisterende student
     *
     * @param student Den opdaterede student
     * @return true hvis operationen lykkedes
     */
    public boolean updateStudent(Student student) {
        try {
            boolean success = studentDAO.update(student);

            if (success) {
                log.addToLog("Student opdateret: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
                firePropertyChange("studentUpdated", null, student);

                // Opdater cache
                refreshCaches();
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af student: " + e.getMessage(), e);
            log.addToLog("Fejl ved opdatering af student: " + e.getMessage());
            return false;
        }
    }

    // ===================================
    // = Reservation Management Methods =
    // ===================================

    /**
     * Giver adgang til ReservationManager-objektet
     *
     * @return ReservationManager-instansen
     */
    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    /**
     * Opretter en ny reservation
     *
     * @param laptop  Laptopen der skal udlånes
     * @param student Studenten der skal låne laptopen
     * @return Den oprettede reservation eller null ved fejl
     */
    public Reservation createReservation(Laptop laptop, Student student) {
        return reservationManager.createReservation(laptop, student);
    }

    /**
     * Opdaterer en reservations status
     *
     * @param reservationId Reservationens UUID
     * @param newStatus     Den nye status
     * @return true hvis operationen lykkedes
     */
    public boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus) {
        return reservationManager.updateReservationStatus(reservationId, newStatus);
    }

    /**
     * Returnerer antal aktive reservationer
     *
     * @return Antal aktive reservationer
     */
    public int getAmountOfActiveReservations() {
        return reservationManager.getAmountOfActiveReservations();
    }

    /**
     * Returnerer alle aktive reservationer
     *
     * @return Liste af aktive reservationer
     */
    public List<Reservation> getAllActiveReservations() {
        return reservationManager.getAllActiveReservations();
    }

    // =============================
    // = Queue Management Methods =
    // =============================

    /**
     * Tilføjer en student til høj-ydelses køen
     *
     * @param student Studenten der skal tilføjes
     */
    public void addToHighPerformanceQueue(Student student) {
        reservationManager.addToHighPerformanceQueue(student);
    }

    /**
     * Tilføjer en student til lav-ydelses køen
     *
     * @param student Studenten der skal tilføjes
     */
    public void addToLowPerformanceQueue(Student student) {
        reservationManager.addToLowPerformanceQueue(student);
    }

    /**
     * Returnerer antal studerende i høj-ydelses køen
     *
     * @return Antal studerende i køen
     */
    public int getHighNeedingQueueSize() {
        return reservationManager.getHighNeedingQueueSize();
    }

    /**
     * Returnerer antal studerende i lav-ydelses køen
     *
     * @return Antal studerende i køen
     */
    public int getLowNeedingQueueSize() {
        return reservationManager.getLowNeedingQueueSize();
    }

    /**
     * Returnerer studerende i høj-ydelses køen
     *
     * @return Liste af studerende i køen
     */
    public List<Student> getStudentsInHighPerformanceQueue() {
        return reservationManager.getStudentsInHighPerformanceQueue();
    }

    /**
     * Returnerer studerende i lav-ydelses køen
     *
     * @return Liste af studerende i køen
     */
    public List<Student> getStudentsInLowPerformanceQueue() {
        return reservationManager.getStudentsInLowPerformanceQueue();
    }

    // =============================
    // = PropertyChangeListener implementation =
    // =============================

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Propagér relevante events til lyttere

        // ReservationManager events
        if (evt.getSource() instanceof ReservationManager) {
            // Propagér alle events fra ReservationManager
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            // Efter visse events, opdater vores caches
            if ("reservationCreated".equals(evt.getPropertyName()) ||
                    "reservationStatusUpdated".equals(evt.getPropertyName())) {
                refreshCaches();
            }
        }
        // Laptop events
        else if (evt.getSource() instanceof Laptop) {
            // Propagér state ændringer
            if ("state".equals(evt.getPropertyName()) ||
                    "available".equals(evt.getPropertyName())) {

                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                firePropertyChange("laptopStateChanged", evt.getOldValue(), evt.getNewValue());

                // Opdater statistik-properties
                firePropertyChange("availableLaptopCount", null, getAmountOfAvailableLaptops());
                firePropertyChange("loanedLaptopCount", null, getAmountOfLoanedLaptops());
            }
        }
        // Student events
        else if (evt.getSource() instanceof Student) {
            // Propagér hasLaptop ændringer
            if ("hasLaptop".equals(evt.getPropertyName())) {
                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                firePropertyChange("studentHasLaptopChanged", evt.getOldValue(), evt.getNewValue());

                // Opdater statistik-properties
                firePropertyChange("studentsWithLaptopCount", null, getCountOfWhoHasLaptop());
            }
        }
    }

    // =============================
    // = PropertyChangeNotifier implementation =
    // =============================

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
}