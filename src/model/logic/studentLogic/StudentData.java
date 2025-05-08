package model.logic.studentLogic;

import model.enums.PerformanceTypeEnum;
import model.util.ValidationService;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service-klasse der håndterer student-relateret forretningslogik.
 * Opdateret til MVVM-arkitektur med databinding support og events.
 */
public class StudentData implements StudentDataInterface, PropertyChangeNotifier {
    private static final Logger logger = Logger.getLogger(StudentData.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    private final List<Student> studentCache;
    private final StudentDAO studentDAO;
    private final PropertyChangeSupport changeSupport;

    /**
     * Konstruktør der initialiserer komponenter og cache.
     */
    public StudentData() {
        this.studentCache = new ArrayList<>();
        this.studentDAO = new StudentDAO();
        this.changeSupport = new PropertyChangeSupport(this);

        // Forsøg at indlæse cache fra database
        refreshCache();

        // Registrer som event subscriber
        eventBus.subscribe(SystemEvents.StudentCreatedEvent.class,
                event -> handleStudentCreated(event.getStudent()));

        eventBus.subscribe(SystemEvents.StudentUpdatedEvent.class,
                event -> handleStudentUpdated(event.getStudent()));

        eventBus.subscribe(SystemEvents.StudentDeletedEvent.class,
                event -> handleStudentDeleted(event.getStudent()));

        eventBus.subscribe(SystemEvents.StudentHasLaptopChangedEvent.class,
                event -> handleStudentHasLaptopChanged(event.getStudent(),
                        event.getOldHasLaptop(),
                        event.getNewHasLaptop()));
    }

    /**
     * Genindlæser student-cache fra databasen.
     */
    private void refreshCache() {
        try {
            studentCache.clear();
            studentCache.addAll(studentDAO.getAll());

            // Registrer som listener til alle studerende
            for (Student student : studentCache) {
                student.addPropertyChangeListener(this::handleStudentPropertyChange);
            }

            firePropertyChange("studentsRefreshed", null, studentCache.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved indlæsning af studerende fra database: " + e.getMessage(), e);
            log.error("Fejl ved indlæsning af studerende fra database: " + e.getMessage());
        }
    }

    /**
     * Håndterer propertyChange events fra Student objekter.
     */
    private void handleStudentPropertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Student) {
            Student student = (Student) evt.getSource();

            // Propagér relevante events
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            // Ved hasLaptop ændringer, opdateres statistikker
            if ("hasLaptop".equals(evt.getPropertyName())) {
                firePropertyChange("studentsWithLaptopCount", null, getCountOfWhoHasLaptop());
            }
        }
    }

    // Event handlers

    private void handleStudentCreated(Student student) {
        if (!studentCache.contains(student)) {
            studentCache.add(student);
            student.addPropertyChangeListener(this::handleStudentPropertyChange);

            firePropertyChange("studentAdded", null, student);
            firePropertyChange("studentCount", studentCache.size() - 1, studentCache.size());
        }
    }

    private void handleStudentUpdated(Student student) {
        // Find den eksisterende student i cachen
        for (int i = 0; i < studentCache.size(); i++) {
            if (studentCache.get(i).getViaId() == student.getViaId()) {
                Student oldStudent = studentCache.get(i);
                studentCache.set(i, student);

                // Overfør listeners til den nye student
                for (PropertyChangeListener listener : getListenersFromStudent(oldStudent)) {
                    student.addPropertyChangeListener(listener);
                }

                firePropertyChange("studentUpdated", oldStudent, student);
                break;
            }
        }
    }

    private PropertyChangeListener[] getListenersFromStudent(Student student) {
        // Dette er en dummy-implementering, da der ikke er direkte adgang
        // til listeners i en Student. I en rigtig implementering skulle
        // PropertyChangeListener[] hentes fra student objektet.
        return new PropertyChangeListener[0];
    }

    private void handleStudentDeleted(Student student) {
        for (int i = 0; i < studentCache.size(); i++) {
            if (studentCache.get(i).getViaId() == student.getViaId()) {
                Student removedStudent = studentCache.remove(i);

                firePropertyChange("studentRemoved", removedStudent, null);
                firePropertyChange("studentCount", studentCache.size() + 1, studentCache.size());
                break;
            }
        }
    }

    private void handleStudentHasLaptopChanged(Student student, boolean oldHasLaptop, boolean newHasLaptop) {
        // Opdater statistikker
        firePropertyChange("studentsWithLaptopCount", null, getCountOfWhoHasLaptop());
    }

    // StudentDataInterface implementation

    @Override
    public ArrayList<Student> getAllStudents() {
        return new ArrayList<>(studentCache);
    }

    @Override
    public int getStudentCount() {
        return studentCache.size();
    }

    @Override
    public Student getStudentByID(int id) {
        // Søg først i cache
        for (Student student : studentCache) {
            if (student.getViaId() == id) {
                return student;
            }
        }

        // Hvis ikke fundet, søg i database
        try {
            Student student = studentDAO.getById(id);
            if (student != null) {
                // Tilføj til cache
                handleStudentCreated(student);
            }
            return student;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af student med ID " + id + ": " + e.getMessage(), e);
            log.warning("Fejl ved hentning af student med ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public ArrayList<Student> getStudentWithHighPowerNeeds() {
        ArrayList<Student> highPowerStudents = new ArrayList<>();
        for (Student student : studentCache) {
            if (student.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                highPowerStudents.add(student);
            }
        }
        return highPowerStudents;
    }

    @Override
    public int getStudentCountOfHighPowerNeeds() {
        return getStudentWithHighPowerNeeds().size();
    }

    @Override
    public ArrayList<Student> getStudentWithLowPowerNeeds() {
        ArrayList<Student> lowPowerStudents = new ArrayList<>();
        for (Student student : studentCache) {
            if (student.getPerformanceNeeded() == PerformanceTypeEnum.LOW) {
                lowPowerStudents.add(student);
            }
        }
        return lowPowerStudents;
    }

    @Override
    public int getStudentCountOfLowPowerNeeds() {
        return getStudentWithLowPowerNeeds().size();
    }

    @Override
    public ArrayList<Student> getThoseWhoHaveLaptop() {
        ArrayList<Student> studentsWithLaptop = new ArrayList<>();
        for (Student student : studentCache) {
            if (student.isHasLaptop()) {
                studentsWithLaptop.add(student);
            }
        }
        return studentsWithLaptop;
    }

    @Override
    public int getCountOfWhoHasLaptop() {
        return getThoseWhoHaveLaptop().size();
    }

    @Override
    public Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                                 int viaId, String email, int phoneNumber,
                                 PerformanceTypeEnum performanceNeeded) {
        // Validér input
        if (!validateStudentData(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber)) {
            log.warning("Ugyldig student data ved oprettelse");
            return null;
        }

        try {
            // Opret student-objekt
            Student student = new Student(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);

            // Gem i database
            boolean success = studentDAO.insert(student);

            if (success) {
                // Student-objektet er allerede tilføjet til cachen via event
                log.info("Student oprettet: " + name + " (VIA ID: " + viaId + ")");
                return student;
            } else {
                log.error("Kunne ikke oprette student i database");
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved oprettelse af student: " + e.getMessage(), e);
            log.error("Fejl ved oprettelse af student: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validerer student data.
     */
    private boolean validateStudentData(String name, Date degreeEndDate, String degreeTitle,
                                        int viaId, String email, int phoneNumber) {
        if (!ValidationService.isValidPersonName(name)) {
            log.warning("Ugyldigt navn: " + name);
            return false;
        }

        if (degreeEndDate == null) {
            log.warning("Ugyldig uddannelse slutdato: null");
            return false;
        }

        if (!ValidationService.isValidDegreeTitle(degreeTitle)) {
            log.warning("Ugyldig uddannelsestitel: " + degreeTitle);
            return false;
        }

        if (!ValidationService.isValidViaId(viaId)) {
            log.warning("Ugyldigt VIA ID: " + viaId);
            return false;
        }

        if (!ValidationService.isValidEmail(email)) {
            log.warning("Ugyldig email: " + email);
            return false;
        }

        if (!ValidationService.isValidPhoneNumber(phoneNumber)) {
            log.warning("Ugyldigt telefonnummer: " + phoneNumber);
            return false;
        }

        return true;
    }

    /**
     * Opdaterer en eksisterende student.
     *
     * @param student Student at opdatere
     * @return true hvis operationen lykkedes
     */
    public boolean updateStudent(Student student) {
        try {
            boolean success = studentDAO.update(student);

            if (success) {
                // Student-objektet er allerede opdateret i cachen via event
                log.info("Student opdateret: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
            } else {
                log.error("Kunne ikke opdatere student i database: " + student.getViaId());
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af student: " + e.getMessage(), e);
            log.error("Fejl ved opdatering af student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sletter en student.
     *
     * @param viaId Student VIA ID
     * @return true hvis operationen lykkedes
     */
    public boolean deleteStudent(int viaId) {
        try {
            // Find først student for at kunne sende event
            Student student = getStudentByID(viaId);
            if (student == null) {
                return false;
            }

            boolean success = studentDAO.delete(viaId);

            if (success) {
                // Student er allerede fjernet fra cache via event
                log.info("Student slettet: " + viaId);
            } else {
                log.error("Kunne ikke slette student fra database: " + viaId);
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved sletning af student: " + e.getMessage(), e);
            log.error("Fejl ved sletning af student: " + e.getMessage());
            return false;
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