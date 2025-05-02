package model.logic.reservationsLogic;

import model.database.LaptopDAO;
import model.database.QueueDAO;
import model.database.ReservationDAO;
import model.database.StudentDAO;
import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.log.Log;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for reservations-håndtering og kø-administration.
 * Implementerer Observer Pattern for at reagere på ændringer i laptops.
 */
public class ReservationManager implements PropertyChangeListener, PropertyChangeNotifier {
    private static final Logger logger = Logger.getLogger(ReservationManager.class.getName());

    // Collections
    private final List<Reservation> activeReservations;
    private final GenericQueue highPerformanceQueue;
    private final GenericQueue lowPerformanceQueue;

    // DAO-lag
    private final QueueDAO queueDAO;
    private final ReservationDAO reservationDAO;
    private final LaptopDAO laptopDAO;
    private final StudentDAO studentDAO;

    // Hjælpeklasser
    private final ReservationFactory reservationFactory;
    private final Log log;
    private final PropertyChangeSupport changeSupport;

    /**
     * Opretter en ny ReservationManager instans.
     */
    public ReservationManager() {
        this.activeReservations = new ArrayList<>();
        this.highPerformanceQueue = new GenericQueue(PerformanceTypeEnum.HIGH);
        this.lowPerformanceQueue = new GenericQueue(PerformanceTypeEnum.LOW);
        this.reservationFactory = new ReservationFactory();

        // Initialiser DAOs
        this.queueDAO = new QueueDAO();
        this.reservationDAO = new ReservationDAO();
        this.laptopDAO = new LaptopDAO();
        this.studentDAO = new StudentDAO();

        // Initialiser log og PropertyChangeSupport
        this.log = Log.getInstance();
        this.changeSupport = new PropertyChangeSupport(this);

        // Tilføj ReservationManager som listener til reservationFactory
        this.reservationFactory.addPropertyChangeListener(this);

        // Tilføj ReservationManager som listener til køer
        this.highPerformanceQueue.addPropertyChangeListener(this);
        this.lowPerformanceQueue.addPropertyChangeListener(this);

        // Indlæs data fra databasen ved opstart
        try {
            loadReservationsFromDatabase();
            loadQueuesFromDatabase();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved indlæsning fra database ved opstart: " + e.getMessage(), e);
            log.addToLog("Fejl ved indlæsning fra database: " + e.getMessage());
        }
    }

    /**
     * Indlæs aktive reservationer fra databasen ved opstart
     */
    private void loadReservationsFromDatabase() throws SQLException {
        List<Reservation> dbReservations = reservationDAO.getAllReservations();
        int count = 0;

        for (Reservation reservation : dbReservations) {
            if (reservation.getStatus() == ReservationStatusEnum.ACTIVE) {
                activeReservations.add(reservation);

                // Tilføj lytter til reservationen
                reservation.addPropertyChangeListener(this);
                count++;
            }
        }

        logger.info("Indlæst " + count + " aktive reservationer fra databasen");
        log.addToLog("Indlæst " + count + " aktive reservationer fra databasen");
    }

    /**
     * Indlæs køer fra databasen ved opstart
     */
    private void loadQueuesFromDatabase() throws SQLException {
        // Indlæs lav-ydelses kø
        List<Student> lowPerformanceStudents = queueDAO.getStudentsInQueue(PerformanceTypeEnum.LOW);
        for (Student student : lowPerformanceStudents) {
            lowPerformanceQueue.addToQueue(student);
        }

        // Indlæs høj-ydelses kø
        List<Student> highPerformanceStudents = queueDAO.getStudentsInQueue(PerformanceTypeEnum.HIGH);
        for (Student student : highPerformanceStudents) {
            highPerformanceQueue.addToQueue(student);
        }

        logger.info("Indlæst " + lowPerformanceStudents.size() + " studerende i lav-ydelses kø");
        logger.info("Indlæst " + highPerformanceStudents.size() + " studerende i høj-ydelses kø");
        log.addToLog("Indlæst køer fra databasen: " + lowPerformanceStudents.size() +
                " i lav-ydelses kø, " + highPerformanceStudents.size() + " i høj-ydelses kø");
    }

    // Reservations-relaterede metoder

    /**
     * Oprettelse af reservation med database persistering
     *
     * @param laptop  Laptopen der skal udlånes
     * @param student Studenten der skal låne laptopen
     * @return        Den oprettede reservation eller null ved fejl
     */
    public Reservation createReservation(Laptop laptop, Student student) {
        try {
            // Tjek forudsætninger
            if (laptop == null || student == null) {
                logger.warning("Null reference: Laptop eller Student er null");
                log.addToLog("Fejl: Kan ikke oprette reservation med null references");
                return null;
            }

            if (!laptop.isAvailable()) {
                logger.warning("Laptop er ikke tilgængelig: " + laptop.getId());
                log.addToLog("Fejl: Kan ikke oprette reservation med unavailable laptop: " + laptop.getBrand() + " " + laptop.getModel());
                return null;
            }

            if (student.isHasLaptop()) {
                logger.warning("Student har allerede en laptop: " + student.getViaId());
                log.addToLog("Fejl: Kan ikke oprette reservation for student der allerede har laptop: " + student.getName());
                return null;
            }

            // Opret reservation objekt
            Reservation reservation = reservationFactory.createReservation(laptop, student);

            // Gem i databasen med transaction support
            boolean success = reservationDAO.createReservationWithTransaction(reservation);

            if (success) {
                // Opdater in-memory liste
                activeReservations.add(reservation);

                // Tilføj lytter til den nye reservation
                reservation.addPropertyChangeListener(this);

                // Notificér om den nye reservation
                firePropertyChange("reservationCreated", null, reservation);
                firePropertyChange("activeReservationsCount", activeReservations.size() - 1, activeReservations.size());

                return reservation;
            } else {
                logger.warning("Kunne ikke oprette reservation i databasen");
                log.addToLog("Fejl: Kunne ikke oprette reservation i databasen");
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved oprettelse af reservation: " + e.getMessage(), e);
            log.addToLog("Fejl ved oprettelse af reservation: " + e.getMessage());
            return null;
        }
    }

    /**
     * Opdaterer en reservations status med database persistering
     *
     * @param reservationId Reservationens UUID
     * @param newStatus     Den nye status
     * @return              true hvis operationen lykkedes
     */
    public boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus) {
        try {
            // Find reservationen i hukommelsen
            Reservation reservation = findReservationById(reservationId);

            if (reservation == null) {
                // Hvis ikke i hukommelsen, prøv at hente fra databasen
                reservation = reservationDAO.getById(reservationId);
                if (reservation == null) {
                    logger.warning("Reservation ikke fundet: " + reservationId);
                    log.addToLog("Fejl: Reservation ikke fundet: " + reservationId);
                    return false;
                }
            }

            // Opdater status
            ReservationStatusEnum oldStatus = reservation.getStatus();
            reservation.changeStatus(newStatus);

            // Opdater i databasen med transaktionssupport
            boolean success = reservationDAO.updateStatusWithTransaction(reservation);

            if (success) {
                // Fjern fra in-memory listen hvis cancelled/completed
                if (newStatus == ReservationStatusEnum.CANCELLED ||
                        newStatus == ReservationStatusEnum.COMPLETED) {

                    int oldSize = activeReservations.size();
                    activeReservations.removeIf(r -> r.getReservationId().equals(reservationId));

                    // Notificér om ændringen i aktive reservationer
                    if (oldSize != activeReservations.size()) {
                        firePropertyChange("activeReservationsCount", oldSize, activeReservations.size());
                    }
                }

                log.addToLog("Reservation " + reservationId + " opdateret til status: " + newStatus);
                firePropertyChange("reservationStatusUpdated", oldStatus, newStatus);
                return true;
            } else {
                logger.warning("Kunne ikke opdatere reservation i database");
                log.addToLog("Fejl: Kunne ikke opdatere reservation i database");
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af reservation: " + e.getMessage(), e);
            log.addToLog("Fejl ved opdatering af reservation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finder en reservation baseret på ID
     *
     * @param reservationId Reservationens UUID
     * @return              Reservation hvis fundet, ellers null
     */
    private Reservation findReservationById(UUID reservationId) {
        for (Reservation r : activeReservations) {
            if (r.getReservationId().equals(reservationId)) {
                return r;
            }
        }
        return null;
    }

    // Kø-relaterede metoder

    /**
     * Tilføj student til høj-ydelses kø med database persistering
     *
     * @param student Studenten der skal tilføjes til køen
     */
    public void addToHighPerformanceQueue(Student student) {
        try {
            // Tjek først om studenten allerede har en laptop eller er i en anden kø
            if (student.isHasLaptop()) {
                logger.info("Student " + student.getName() + " har allerede en laptop, tilføjes ikke til kø");
                log.addToLog("Student " + student.getName() + " har allerede en laptop, tilføjes ikke til kø");
                return;
            }

            if (queueDAO.isStudentInAnyQueue(student.getViaId())) {
                logger.info("Student " + student.getName() + " er allerede i en kø");
                log.addToLog("Student " + student.getName() + " er allerede i en kø");
                return;
            }

            // Tjek om studentens ydelsesbehov passer til køen
            if (student.getPerformanceNeeded() != PerformanceTypeEnum.HIGH) {
                logger.info("Student " + student.getName() + " har ikke behov for høj ydelse, omdirigerer");
                addToLowPerformanceQueue(student);
                return;
            }

            // Tjek om der er en tilgængelig laptop med det rette ydelsesniveau
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(PerformanceTypeEnum.HIGH);
            if (!availableLaptops.isEmpty()) {
                // Der er en tilgængelig laptop, tildel den med det samme i stedet for at tilføje til kø
                Laptop laptop = availableLaptops.get(0);
                createReservation(laptop, student);
                logger.info("Student " + student.getName() + " fik tildelt laptop direkte i stedet for at blive sat i kø");
                log.addToLog("Student " + student.getName() + " fik tildelt laptop direkte i stedet for at blive sat i kø");
                return;
            }

            // Tilføj til database kø
            boolean added = queueDAO.addToQueue(student, PerformanceTypeEnum.HIGH);

            if (added) {
                // Tilføj til in-memory kø
                highPerformanceQueue.addToQueue(student);
                log.addToLog("Student " + student.getName() + " tilføjet til høj-ydelses kø");

                // Notificér om ændringen i køen
                firePropertyChange("highQueueSize", highPerformanceQueue.getQueueSize() - 1, highPerformanceQueue.getQueueSize());
            } else {
                logger.warning("Kunne ikke tilføje student til kø i databasen");
                log.addToLog("Fejl: Kunne ikke tilføje student til kø i databasen");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved tilføjelse til høj-ydelses kø: " + e.getMessage(), e);
            log.addToLog("Fejl ved tilføjelse til høj-ydelses kø: " + e.getMessage());
        }
    }

    /**
     * Tilføj student til lav-ydelses kø med database persistering
     *
     * @param student Studenten der skal tilføjes til køen
     */
    public void addToLowPerformanceQueue(Student student) {
        try {
            // Tjek først om studenten allerede har en laptop eller er i en anden kø
            if (student.isHasLaptop()) {
                logger.info("Student " + student.getName() + " har allerede en laptop, tilføjes ikke til kø");
                log.addToLog("Student " + student.getName() + " har allerede en laptop, tilføjes ikke til kø");
                return;
            }

            if (queueDAO.isStudentInAnyQueue(student.getViaId())) {
                logger.info("Student " + student.getName() + " er allerede i en kø");
                log.addToLog("Student " + student.getName() + " er allerede i en kø");
                return;
            }

            // Tjek om studentens ydelsesbehov passer til køen
            if (student.getPerformanceNeeded() != PerformanceTypeEnum.LOW) {
                logger.info("Student " + student.getName() + " har behov for høj ydelse, omdirigerer");
                addToHighPerformanceQueue(student);
                return;
            }

            // Tjek om der er en tilgængelig laptop med det rette ydelsesniveau
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(PerformanceTypeEnum.LOW);
            if (!availableLaptops.isEmpty()) {
                // Der er en tilgængelig laptop, tildel den med det samme i stedet for at tilføje til kø
                Laptop laptop = availableLaptops.get(0);
                createReservation(laptop, student);
                logger.info("Student " + student.getName() + " fik tildelt laptop direkte i stedet for at blive sat i kø");
                log.addToLog("Student " + student.getName() + " fik tildelt laptop direkte i stedet for at blive sat i kø");
                return;
            }

            // Tilføj til database kø
            boolean added = queueDAO.addToQueue(student, PerformanceTypeEnum.LOW);

            if (added) {
                // Tilføj til in-memory kø
                lowPerformanceQueue.addToQueue(student);
                log.addToLog("Student " + student.getName() + " tilføjet til lav-ydelses kø");

                // Notificér om ændringen i køen
                firePropertyChange("lowQueueSize", lowPerformanceQueue.getQueueSize() - 1, lowPerformanceQueue.getQueueSize());
            } else {
                logger.warning("Kunne ikke tilføje student til kø i databasen");
                log.addToLog("Fejl: Kunne ikke tilføje student til kø i databasen");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved tilføjelse til lav-ydelses kø: " + e.getMessage(), e);
            log.addToLog("Fejl ved tilføjelse til lav-ydelses kø: " + e.getMessage());
        }
    }

    /**
     * Fjerner en student fra kø baseret på VIA ID og ydelsestype
     *
     * @param viaId          Studentens VIA ID
     * @param performanceType Ydelsestypen for køen (HIGH/LOW)
     * @return               true hvis studenten blev fjernet, ellers false
     */
    public boolean removeFromQueue(int viaId, PerformanceTypeEnum performanceType) {
        try {
            // Fjern fra database
            boolean removed = queueDAO.removeFromQueue(viaId, performanceType);

            if (removed) {
                // Fjern fra in-memory kø
                GenericQueue queue = (performanceType == PerformanceTypeEnum.HIGH) ?
                        highPerformanceQueue : lowPerformanceQueue;

                int oldSize = queue.getQueueSize();
                boolean inMemoryRemoved = queue.removeStudentById(viaId);

                if (inMemoryRemoved) {
                    // Notificér om ændringen
                    String propertyName = (performanceType == PerformanceTypeEnum.HIGH) ?
                            "highQueueSize" : "lowQueueSize";

                    firePropertyChange(propertyName, oldSize, queue.getQueueSize());
                    return true;
                }
            }

            return removed;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved fjernelse fra kø: " + e.getMessage(), e);
            log.addToLog("Fejl ved fjernelse fra kø: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tildeler automatisk den næste student i køen til en ledig laptop
     *
     * @param laptop Laptop der er blevet ledig
     * @return       Den oprettede reservation eller null hvis ingen studerende i køen
     */
    private Reservation assignNextStudentFromQueue(Laptop laptop) {
        PerformanceTypeEnum laptopType = laptop.getPerformanceType();
        GenericQueue queue;
        String queueTypeName;

        if (laptopType == PerformanceTypeEnum.HIGH) {
            queue = highPerformanceQueue;
            queueTypeName = "høj-ydelses";
        } else {
            queue = lowPerformanceQueue;
            queueTypeName = "lav-ydelses";
        }

        if (queue.getQueueSize() > 0) {
            try {
                // Hent næste student fra databasen
                Student nextStudent = queueDAO.getAndRemoveNextInQueue(laptopType);

                if (nextStudent != null) {
                    // Opdater in-memory kø
                    queue.getAndRemoveNextInLine();

                    // Opret reservation
                    Reservation reservation = createReservation(laptop, nextStudent);

                    if (reservation != null) {
                        log.addToLog("Automatisk tildeling: Student " + nextStudent.getName() +
                                " tildelt laptop " + laptop.getBrand() + " " + laptop.getModel() +
                                " fra " + queueTypeName + " kø");

                        return reservation;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Fejl ved automatisk tildeling fra kø: " + e.getMessage(), e);
                log.addToLog("Fejl ved automatisk tildeling fra kø: " + e.getMessage());
            }
        }

        return null;
    }

    // Metoder til at hente kø-information

    /**
     * Returnerer antal studerende i høj-ydelses køen
     *
     * @return Antal studerende i køen
     */
    public int getHighNeedingQueueSize() {
        try {
            return queueDAO.getQueueSize(PerformanceTypeEnum.HIGH);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af høj-ydelses kø størrelse: " + e.getMessage(), e);
            // Returner in-memory størrelse som fallback
            return highPerformanceQueue.getQueueSize();
        }
    }

    /**
     * Returnerer antal studerende i lav-ydelses køen
     *
     * @return Antal studerende i køen
     */
    public int getLowNeedingQueueSize() {
        try {
            return queueDAO.getQueueSize(PerformanceTypeEnum.LOW);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af lav-ydelses kø størrelse: " + e.getMessage(), e);
            // Returner in-memory størrelse som fallback
            return lowPerformanceQueue.getQueueSize();
        }
    }

    /**
     * Returnerer liste af studerende i høj-ydelses køen
     *
     * @return Liste af studerende i køen
     */
    public List<Student> getStudentsInHighPerformanceQueue() {
        try {
            return queueDAO.getStudentsInQueue(PerformanceTypeEnum.HIGH);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af studerende i høj-ydelses kø: " + e.getMessage(), e);
            // Returner in-memory liste som fallback
            return highPerformanceQueue.getAllStudentsInQueue();
        }
    }

    /**
     * Returnerer liste af studerende i lav-ydelses køen
     *
     * @return Liste af studerende i køen
     */
    public List<Student> getStudentsInLowPerformanceQueue() {
        try {
            return queueDAO.getStudentsInQueue(PerformanceTypeEnum.LOW);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af studerende i lav-ydelses kø: " + e.getMessage(), e);
            // Returner in-memory liste som fallback
            return lowPerformanceQueue.getAllStudentsInQueue();
        }
    }

    // Metoder til at hente information om reservationer

    /**
     * Returnerer det totale antal reservationer i systemet
     *
     * @return Antal reservationer
     */
    public int getAmountOfReservationsToDate() {
        try {
            return reservationDAO.getAllReservations().size();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af alle reservationer: " + e.getMessage(), e);
            // Returner in-memory størrelse som fallback
            return activeReservations.size();
        }
    }

    /**
     * Returnerer antal aktive reservationer
     *
     * @return Antal aktive reservationer
     */
    public int getAmountOfActiveReservations() {
        return activeReservations.size();
    }

    /**
     * Returnerer liste af alle aktive reservationer
     *
     * @return Liste af aktive reservationer
     */
    public ArrayList<Reservation> getAllActiveReservations() {
        return new ArrayList<>(activeReservations);
    }

    /**
     * Returnerer den sidst tilføjede reservation
     *
     * @return Seneste reservation eller null hvis ingen findes
     */
    public Reservation getLastReservationAdded() {
        if (activeReservations.isEmpty()) {
            return null;
        }
        return activeReservations.get(activeReservations.size() - 1);
    }

    // Observer mønster implementation - PropertyChangeListener

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Håndter events fra laptops
        if (evt.getSource() instanceof Laptop) {
            handleLaptopEvent(evt);
        }
        // Håndter events fra reservationer
        else if (evt.getSource() instanceof Reservation) {
            handleReservationEvent(evt);
        }
        // Håndter events fra køer
        else if (evt.getSource() instanceof GenericQueue) {
            handleQueueEvent(evt);
        }
        // Håndter events fra ReservationFactory
        else if (evt.getSource() instanceof ReservationFactory) {
            handleFactoryEvent(evt);
        }
    }

    /**
     * Håndterer events fra Laptop objekter
     */
    private void handleLaptopEvent(PropertyChangeEvent evt) {
        // Reagér på at en laptop bliver tilgængelig
        if ("available".equals(evt.getPropertyName()) && (boolean) evt.getNewValue()) {
            Laptop laptop = (Laptop) evt.getSource();
            logger.info("Laptop " + laptop.getId() + " er blevet tilgængelig, tjekker køer");
            log.addToLog("Laptop " + laptop.getBrand() + " " + laptop.getModel() + " er blevet tilgængelig");

            try {
                // Opdater laptop tilstand i databasen
                laptopDAO.updateState(laptop);

                // Automatisk tildel laptop til næste student i køen hvis muligt
                assignNextStudentFromQueue(laptop);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Fejl ved håndtering af tilgængelig laptop: " + e.getMessage(), e);
                log.addToLog("Fejl ved håndtering af tilgængelig laptop: " + e.getMessage());
            }
        }
    }

    /**
     * Håndterer events fra Reservation objekter
     */
    private void handleReservationEvent(PropertyChangeEvent evt) {
        // Reagér på at en reservation ændrer status
        if ("status".equals(evt.getPropertyName())) {
            Reservation reservation = (Reservation) evt.getSource();
            ReservationStatusEnum newStatus = (ReservationStatusEnum) evt.getNewValue();

            if (newStatus == ReservationStatusEnum.COMPLETED || newStatus == ReservationStatusEnum.CANCELLED) {
                // Fjern fra aktive reservationer-listen
                activeReservations.remove(reservation);

                // Notificér om ændringen
                firePropertyChange("activeReservationsCount", activeReservations.size() + 1, activeReservations.size());
            }
        }
    }

    /**
     * Håndterer events fra GenericQueue objekter
     */
    private void handleQueueEvent(PropertyChangeEvent evt) {
        GenericQueue queue = (GenericQueue) evt.getSource();

        // Propagér relevante kø-events
        if ("queueSize".equals(evt.getPropertyName())) {
            String propertyName = (queue.getPerformanceType() == PerformanceTypeEnum.HIGH) ?
                    "highQueueSize" : "lowQueueSize";

            firePropertyChange(propertyName, evt.getOldValue(), evt.getNewValue());
        }
    }

    /**
     * Håndterer events fra ReservationFactory
     */
    private void handleFactoryEvent(PropertyChangeEvent evt) {
        // Propagér relevante factory-events
        if ("reservationCreated".equals(evt.getPropertyName())) {
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
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
}