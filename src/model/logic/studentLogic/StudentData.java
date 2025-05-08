package model.logic.studentLogic;

import model.database.StudentDAO;
import model.enums.PerformanceTypeEnum;
import model.log.Log;
import model.models.Student;
import model.util.ModelObservable;
import model.util.ValidationService;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class that handles student-related business logic.
 * Implements Observer pattern using Java's built-in Observable/Observer.
 */
public class StudentData extends Observable implements StudentDataInterface, Observer {
    private static final Logger logger = Logger.getLogger(StudentData.class.getName());
    private static final Log log = Log.getInstance();

    // Event types for observer notifications
    public static final String EVENT_STUDENT_ADDED = "STUDENT_ADDED";
    public static final String EVENT_STUDENT_UPDATED = "STUDENT_UPDATED";
    public static final String EVENT_STUDENT_REMOVED = "STUDENT_REMOVED";
    public static final String EVENT_STUDENTS_REFRESHED = "STUDENTS_REFRESHED";
    public static final String EVENT_ERROR = "ERROR";

    private final List<Student> studentCache;
    private final StudentDAO studentDAO;
    private final ReadWriteLock cacheLock;

    // Singleton instance
    private static volatile StudentData instance;

    /**
     * Private constructor for Singleton pattern.
     * Initializes components and cache.
     */
    private StudentData() {
        this.studentCache = new ArrayList<>();
        this.studentDAO = StudentDAO.getInstance();
        this.cacheLock = new ReentrantReadWriteLock();

        // Register as observer of StudentDAO
        studentDAO.addObserver(this);

        // Load initial data
        refreshCache();
    }

    /**
     * Gets the singleton instance with double-checked locking.
     *
     * @return The singleton instance
     */
    public static StudentData getInstance() {
        if (instance == null) {
            synchronized (StudentData.class) {
                if (instance == null) {
                    instance = new StudentData();
                }
            }
        }
        return instance;
    }

    /**
     * Reloads student cache from the database.
     */
    public void refreshCache() {
        try {
            List<Student> students = studentDAO.getAll();

            cacheLock.writeLock().lock();
            try {
                studentCache.clear();
                studentCache.addAll(students);

                // Register as observer to all students
                for (Student student : students) {
                    student.addObserver(this);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }

            setChanged();
            notifyObservers(new DataEvent(EVENT_STUDENTS_REFRESHED, studentCache.size()));

            log.info("Student cache refreshed: " + students.size() + " students loaded");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading students from database: " + e.getMessage(), e);
            log.error("Error loading students from database: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error refreshing student cache", e));
        }
    }

    /**
     * Handles property change events from models and DAOs.
     */
    @Override
    public void update(Observable o, Object arg) {
        // Handle events from StudentDAO
        if (o instanceof StudentDAO) {
            if (arg instanceof StudentDAO.DatabaseEvent) {
                StudentDAO.DatabaseEvent event = (StudentDAO.DatabaseEvent) arg;

                switch (event.getEventType()) {
                    case StudentDAO.EVENT_STUDENT_CREATED:
                        handleStudentCreated((Student) event.getData());
                        break;
                    case StudentDAO.EVENT_STUDENT_UPDATED:
                        handleStudentUpdated((Student) event.getData());
                        break;
                    case StudentDAO.EVENT_STUDENT_DELETED:
                        handleStudentDeleted((Student) event.getData());
                        break;
                    case StudentDAO.EVENT_STUDENT_ERROR:
                        // Forward error event
                        setChanged();
                        notifyObservers(new ErrorEvent(EVENT_ERROR,
                                event.getData().toString(), event.getException()));
                        break;
                }
            }
        }
        // Handle events from Student objects
        else if (o instanceof Student) {
            if (arg instanceof ModelObservable.PropertyChangeInfo) {
                ModelObservable.PropertyChangeInfo info = (ModelObservable.PropertyChangeInfo) arg;

                // Forward property change events
                setChanged();
                notifyObservers(new PropertyChangeEvent(
                        info.getPropertyName(), info.getOldValue(), info.getNewValue(), (Student) o));

                // Update statistics for hasLaptop changes
                if ("hasLaptop".equals(info.getPropertyName())) {
                    setChanged();
                    notifyObservers(new CountEvent("studentsWithLaptopCount", getCountOfWhoHasLaptop()));
                }
            }
        }
    }

    /**
     * Handles student creation events.
     */
    private void handleStudentCreated(Student student) {
        boolean added = false;

        cacheLock.writeLock().lock();
        try {
            // Check if student is already in cache
            boolean exists = false;
            for (Student s : studentCache) {
                if (s.getViaId() == student.getViaId()) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                studentCache.add(student);
                student.addObserver(this);
                added = true;
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        if (added) {
            setChanged();
            notifyObservers(new DataEvent(EVENT_STUDENT_ADDED, student));
        }
    }

    /**
     * Handles student update events.
     */
    private void handleStudentUpdated(Student student) {
        boolean updated = false;
        Student oldStudent = null;

        cacheLock.writeLock().lock();
        try {
            for (int i = 0; i < studentCache.size(); i++) {
                if (studentCache.get(i).getViaId() == student.getViaId()) {
                    oldStudent = studentCache.get(i);
                    studentCache.set(i, student);
                    student.addObserver(this);
                    updated = true;
                    break;
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        if (updated) {
            setChanged();
            notifyObservers(new UpdateEvent(EVENT_STUDENT_UPDATED, oldStudent, student));
        }
    }

    /**
     * Handles student deletion events.
     */
    private void handleStudentDeleted(Student student) {
        boolean removed = false;

        cacheLock.writeLock().lock();
        try {
            Iterator<Student> iterator = studentCache.iterator();
            while (iterator.hasNext()) {
                Student s = iterator.next();
                if (s.getViaId() == student.getViaId()) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        if (removed) {
            setChanged();
            notifyObservers(new DataEvent(EVENT_STUDENT_REMOVED, student));
        }
    }

    // StudentDataInterface implementation

    @Override
    public ArrayList<Student> getAllStudents() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(studentCache);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public int getStudentCount() {
        cacheLock.readLock().lock();
        try {
            return studentCache.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public Student getStudentByID(int id) {
        // Search in cache first
        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.getViaId() == id) {
                    return student;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // If not found in cache, try database
        try {
            Student student = studentDAO.getById(id);
            if (student != null) {
                // Add to cache
                handleStudentCreated(student);
            }
            return student;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error retrieving student with ID " + id + ": " + e.getMessage(), e);
            log.warning("Error retrieving student with ID " + id + ": " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error retrieving student", e));

            return null;
        }
    }

    @Override
    public ArrayList<Student> getStudentWithHighPowerNeeds() {
        ArrayList<Student> highPowerStudents = new ArrayList<>();

        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                    highPowerStudents.add(student);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return highPowerStudents;
    }

    @Override
    public int getStudentCountOfHighPowerNeeds() {
        int count = 0;

        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                    count++;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return count;
    }

    @Override
    public ArrayList<Student> getStudentWithLowPowerNeeds() {
        ArrayList<Student> lowPowerStudents = new ArrayList<>();

        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.getPerformanceNeeded() == PerformanceTypeEnum.LOW) {
                    lowPowerStudents.add(student);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return lowPowerStudents;
    }

    @Override
    public int getStudentCountOfLowPowerNeeds() {
        int count = 0;

        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.getPerformanceNeeded() == PerformanceTypeEnum.LOW) {
                    count++;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return count;
    }

    @Override
    public ArrayList<Student> getThoseWhoHaveLaptop() {
        ArrayList<Student> studentsWithLaptop = new ArrayList<>();

        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.isHasLaptop()) {
                    studentsWithLaptop.add(student);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return studentsWithLaptop;
    }

    @Override
    public int getCountOfWhoHasLaptop() {
        int count = 0;

        cacheLock.readLock().lock();
        try {
            for (Student student : studentCache) {
                if (student.isHasLaptop()) {
                    count++;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return count;
    }

    @Override
    public Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                                 int viaId, String email, int phoneNumber,
                                 PerformanceTypeEnum performanceNeeded) {
        try {
            // Validate input
            validateStudentData(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);

            // Create student object
            Student student = new Student(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);

            // Save to database
            boolean success = studentDAO.insert(student);

            if (success) {
                // Student is already added to cache via event handling
                log.info("Student created: " + name + " (VIA ID: " + viaId + ")");
                return student;
            } else {
                log.error("Could not create student in database");

                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Failed to create student in database", null));

                return null;
            }
        } catch (IllegalArgumentException e) {
            log.warning("Invalid student data: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Invalid student data: " + e.getMessage(), e));

            return null;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating student: " + e.getMessage(), e);
            log.error("Error creating student: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error creating student", e));

            return null;
        }
    }

    /**
     * Validates student data.
     */
    private void validateStudentData(String name, Date degreeEndDate, String degreeTitle,
                                     int viaId, String email, int phoneNumber,
                                     PerformanceTypeEnum performanceNeeded) {
        if (!ValidationService.isValidPersonName(name)) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }

        if (degreeEndDate == null) {
            throw new IllegalArgumentException("Degree end date cannot be null");
        }

        if (!ValidationService.isValidDegreeTitle(degreeTitle)) {
            throw new IllegalArgumentException("Invalid degree title: " + degreeTitle);
        }

        if (!ValidationService.isValidViaId(viaId)) {
            throw new IllegalArgumentException("Invalid VIA ID: " + viaId);
        }

        if (!ValidationService.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }

        if (!ValidationService.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number: " + phoneNumber);
        }

        if (performanceNeeded == null) {
            throw new IllegalArgumentException("Performance needed cannot be null");
        }
    }

    @Override
    public boolean updateStudent(Student student) {
        try {
            boolean success = studentDAO.update(student);

            if (success) {
                // Student is already updated in cache via event handling
                log.info("Student updated: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
            } else {
                log.error("Could not update student in database: " + student.getViaId());

                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Failed to update student in database", null));
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating student: " + e.getMessage(), e);
            log.error("Error updating student: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error updating student", e));

            return false;
        }
    }

    @Override
    public boolean deleteStudent(int viaId) {
        try {
            // Get student first to send event after deletion
            Student student = getStudentByID(viaId);
            if (student == null) {
                return false;
            }

            boolean success = studentDAO.delete(viaId);

            if (success) {
                // Student is already removed from cache via event handling
                log.info("Student deleted: " + viaId);
            } else {
                log.error("Could not delete student from database: " + viaId);

                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Failed to delete student from database", null));
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting student: " + e.getMessage(), e);
            log.error("Error deleting student: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error deleting student", e));

            return false;
        }
    }

    /**
     * Closes resources and removes observers.
     */
    public void close() {
        studentDAO.deleteObserver(this);

        cacheLock.writeLock().lock();
        try {
            for (Student student : studentCache) {
                student.deleteObserver(this);
            }
            studentCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    // Event classes

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
     * Event class for update operations.
     */
    public static class UpdateEvent extends DataEvent {
        private final Object oldValue;
        private final Object newValue;

        public UpdateEvent(String eventType, Object oldValue, Object newValue) {
            super(eventType, newValue);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }
    }

    /**
     * Event class for property changes.
     */
    public static class PropertyChangeEvent extends DataEvent {
        private final String propertyName;
        private final Object oldValue;
        private final Object newValue;
        private final Object source;

        public PropertyChangeEvent(String propertyName, Object oldValue, Object newValue, Object source) {
            super("propertyChange", newValue);
            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.source = source;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public Object getSource() {
            return source;
        }
    }

    /**
     * Event class for count updates.
     */
    public static class CountEvent {
        private final String countType;
        private final int count;

        public CountEvent(String countType, int count) {
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