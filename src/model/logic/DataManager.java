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

public class DataManager implements PropertyChangeListener, DataModel {
    private static final Logger logger = Logger.getLogger(DataManager.class.getName());

    private final ReservationManager reservationManager;
    private final Log log;

    private final LaptopDAO laptopDAO;
    private final StudentDAO studentDAO;
    private final ReservationDAO reservationDAO;
    private final QueueDAO queueDAO;

    private final List<Laptop> laptopCache;
    private final List<Student> studentCache;

    private final PropertyChangeSupport changeSupport;

    public DataManager() {
        this.reservationManager = new ReservationManager();
        this.log = Log.getInstance();

        this.laptopDAO = new LaptopDAO();
        this.studentDAO = new StudentDAO();
        this.reservationDAO = new ReservationDAO();
        this.queueDAO = new QueueDAO();

        this.laptopCache = new ArrayList<>();
        this.studentCache = new ArrayList<>();

        this.changeSupport = new PropertyChangeSupport(this);

        this.reservationManager.addPropertyChangeListener(this);

        refreshCaches();

        log.addToLog("DataManager initialiseret");
    }

    public void refreshCaches() {
        try {
            laptopCache.clear();
            laptopCache.addAll(laptopDAO.getAll());

            studentCache.clear();
            studentCache.addAll(studentDAO.getAll());

            firePropertyChange("laptopsRefreshed", null, laptopCache.size());
            firePropertyChange("studentsRefreshed", null, studentCache.size());

            log.addToLog("Caches opdateret: " + laptopCache.size() + " laptops, " +
                    studentCache.size() + " studerende");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved genopfriskning af caches: " + e.getMessage(), e);
            log.addToLog("Fejl ved genopfriskning af caches: " + e.getMessage());
        }
    }

    // LAPTOP METODER

    public List<Laptop> getAllLaptops() {
        try {
            return laptopDAO.getAll();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af laptops: " + e.getMessage(), e);
            log.addToLog("Fejl ved hentning af laptops: " + e.getMessage());
            return new ArrayList<>(laptopCache);
        }
    }

    public int getAmountOfAvailableLaptops() {
        int count = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    public int getAmountOfLoanedLaptops() {
        int count = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.isLoaned()) {
                count++;
            }
        }
        return count;
    }

    public Laptop findAvailableLaptop(PerformanceTypeEnum performanceType) {
        try {
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(performanceType);
            return availableLaptops.isEmpty() ? null : availableLaptops.get(0);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved søgning efter tilgængelig laptop: " + e.getMessage(), e);
            log.addToLog("Fejl ved søgning efter tilgængelig laptop: " + e.getMessage());

            for (Laptop laptop : laptopCache) {
                if (laptop.isAvailable() && laptop.getPerformanceType() == performanceType) {
                    return laptop;
                }
            }
            return null;
        }
    }

    public Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        try {
            if (brand == null || brand.trim().isEmpty() ||
                    model == null || model.trim().isEmpty() ||
                    gigabyte <= 0 || ram <= 0 || performanceType == null) {
                log.addToLog("Fejl: Ugyldige laptop-data");
                return null;
            }

            Laptop laptop = new Laptop(brand, model, gigabyte, ram, performanceType);

            laptop.addPropertyChangeListener(this);

            boolean success = laptopDAO.insert(laptop);

            if (success) {
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

    public boolean updateLaptop(Laptop laptop) {
        try {
            boolean success = laptopDAO.update(laptop);

            if (success) {
                log.addToLog("Laptop opdateret: " + laptop.getBrand() + " " + laptop.getModel());
                firePropertyChange("laptopUpdated", null, laptop);

                refreshCaches();
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af laptop: " + e.getMessage(), e);
            log.addToLog("Fejl ved opdatering af laptop: " + e.getMessage());
            return false;
        }
    }

    // STUDENT METODER

    public List<Student> getAllStudents() {
        try {
            return studentDAO.getAll();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af studerende: " + e.getMessage(), e);
            log.addToLog("Fejl ved hentning af studerende: " + e.getMessage());
            return new ArrayList<>(studentCache);
        }
    }

    public int getStudentCount() {
        return studentCache.size();
    }

    public Student getStudentByID(int viaId) {
        for (Student student : studentCache) {
            if (student.getViaId() == viaId) {
                return student;
            }
        }

        try {
            return studentDAO.getById(viaId);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved søgning efter student med ID " + viaId + ": " + e.getMessage(), e);
            log.addToLog("Fejl ved søgning efter student med ID " + viaId + ": " + e.getMessage());
            return null;
        }
    }

    public List<Student> getStudentWithHighPowerNeeds() {
        List<Student> highPowerStudents = new ArrayList<>();

        for (Student student : studentCache) {
            if (student.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                highPowerStudents.add(student);
            }
        }

        return highPowerStudents;
    }

    public int getStudentCountOfHighPowerNeeds() {
        return getStudentWithHighPowerNeeds().size();
    }

    public List<Student> getStudentWithLowPowerNeeds() {
        List<Student> lowPowerStudents = new ArrayList<>();

        for (Student student : studentCache) {
            if (student.getPerformanceNeeded() == PerformanceTypeEnum.LOW) {
                lowPowerStudents.add(student);
            }
        }

        return lowPowerStudents;
    }

    public int getStudentCountOfLowPowerNeeds() {
        return getStudentWithLowPowerNeeds().size();
    }

    public List<Student> getThoseWhoHaveLaptop() {
        List<Student> studentsWithLaptop = new ArrayList<>();

        for (Student student : studentCache) {
            if (student.isHasLaptop()) {
                studentsWithLaptop.add(student);
            }
        }

        return studentsWithLaptop;
    }

    public int getCountOfWhoHasLaptop() {
        return getThoseWhoHaveLaptop().size();
    }

    public Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                                 int viaId, String email, int phoneNumber,
                                 PerformanceTypeEnum performanceNeeded) {
        try {
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

            Student student = new Student(name, degreeEndDate, degreeTitle, viaId,
                    email, phoneNumber, performanceNeeded);

            student.addPropertyChangeListener(this);

            boolean success = studentDAO.insert(student);

            if (success) {
                studentCache.add(student);

                log.addToLog("Student oprettet: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
                firePropertyChange("studentCreated", null, student);

                Laptop availableLaptop = findAvailableLaptop(student.getPerformanceNeeded());

                if (availableLaptop != null) {
                    reservationManager.createReservation(availableLaptop, student);
                } else {
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

    public boolean updateStudent(Student student) {
        try {
            boolean success = studentDAO.update(student);

            if (success) {
                log.addToLog("Student opdateret: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
                firePropertyChange("studentUpdated", null, student);

                refreshCaches();
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af student: " + e.getMessage(), e);
            log.addToLog("Fejl ved opdatering af student: " + e.getMessage());
            return false;
        }
    }

    // RESERVATIONS MANAGER Metoder

    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    public Reservation createReservation(Laptop laptop, Student student) {
        return reservationManager.createReservation(laptop, student);
    }

    public boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus) {
        return reservationManager.updateReservationStatus(reservationId, newStatus);
    }

    public int getAmountOfActiveReservations() {
        return reservationManager.getAmountOfActiveReservations();
    }

    public List<Reservation> getAllActiveReservations() {
        return reservationManager.getAllActiveReservations();
    }


    //QUEUE metoder

    public void addToHighPerformanceQueue(Student student) {
        reservationManager.addToHighPerformanceQueue(student);
    }

    public void addToLowPerformanceQueue(Student student) {
        reservationManager.addToLowPerformanceQueue(student);
    }

    public int getHighNeedingQueueSize() {
        return reservationManager.getHighNeedingQueueSize();
    }

    public int getLowNeedingQueueSize() {
        return reservationManager.getLowNeedingQueueSize();
    }

    public List<Student> getStudentsInHighPerformanceQueue() {
        return reservationManager.getStudentsInHighPerformanceQueue();
    }

    public List<Student> getStudentsInLowPerformanceQueue() {
        return reservationManager.getStudentsInLowPerformanceQueue();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof ReservationManager) {
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            if ("reservationCreated".equals(evt.getPropertyName()) ||
                    "reservationStatusUpdated".equals(evt.getPropertyName())) {
                refreshCaches();
            }
        }
        else if (evt.getSource() instanceof Laptop) {
            if ("state".equals(evt.getPropertyName()) ||
                    "available".equals(evt.getPropertyName())) {

                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                firePropertyChange("laptopStateChanged", evt.getOldValue(), evt.getNewValue());

                firePropertyChange("availableLaptopCount", null, getAmountOfAvailableLaptops());
                firePropertyChange("loanedLaptopCount", null, getAmountOfLoanedLaptops());
            }
        }
        else if (evt.getSource() instanceof Student) {
            if ("hasLaptop".equals(evt.getPropertyName())) {
                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                firePropertyChange("studentHasLaptopChanged", evt.getOldValue(), evt.getNewValue());

                firePropertyChange("studentsWithLaptopCount", null, getCountOfWhoHasLaptop());
            }
        }
    }


    // OBSERVER MØNSTER METODER

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