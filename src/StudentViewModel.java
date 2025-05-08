

import model.enums.PerformanceTypeEnum;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ViewModel for student operations following MVVM architecture.
 * Implements Observer pattern and supports background threading.
 */
public class StudentViewModel extends Observable implements Observer {
    private static final Logger logger = Logger.getLogger(StudentViewModel.class.getName());
    
    // Event types for observer notifications
    public static final String EVENT_STUDENTS_LOADED = "STUDENTS_LOADED";
    public static final String EVENT_STUDENT_CREATED = "STUDENT_CREATED";
    public static final String EVENT_STUDENT_UPDATED = "STUDENT_UPDATED";
    public static final String EVENT_STUDENT_DELETED = "STUDENT_DELETED";
    public static final String EVENT_OPERATION_ERROR = "OPERATION_ERROR";
    
    private final StudentDAO studentDAO;
    private final List<Student> studentsCache;
    private final ExecutorService executor;
    
    /**
     * Creates a new StudentViewModel.
     */
    public StudentViewModel() {
        this.studentDAO = StudentDAO.getInstance();
        this.studentsCache = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Register as observer of StudentDAO
        studentDAO.addObserver(this);
        
        // Initial load
        loadStudents();
    }
    
    /**
     * Loads all students from the database asynchronously.
     */
    public void loadStudents() {
        executor.submit(() -> {
            try {
                List<Student> students = studentDAO.getAll();
                
                synchronized (studentsCache) {
                    studentsCache.clear();
                    studentsCache.addAll(students);
                }
                
                // Notify observers
                setChanged();
                notifyObservers(new ViewModelEvent(EVENT_STUDENTS_LOADED, students));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error loading students: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error loading students", e));
            }
        });
    }
    
    /**
     * Gets all students from the cache.
     *
     * @return List of all students
     */
    public List<Student> getAllStudents() {
        synchronized (studentsCache) {
            return new ArrayList<>(studentsCache);
        }
    }
    
    /**
     * Gets the number of students.
     *
     * @return Number of students
     */
    public int getStudentCount() {
        synchronized (studentsCache) {
            return studentsCache.size();
        }
    }
    
    /**
     * Gets a student by ID.
     *
     * @param id VIA ID of the student
     * @return Student or null if not found
     */
    public Student getStudentById(int id) {
        synchronized (studentsCache) {
            for (Student student : studentsCache) {
                if (student.getViaId() == id) {
                    return student;
                }
            }
        }
        
        // If not found in cache, try database
        try {
            return studentDAO.getById(id);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error retrieving student with ID " + id + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets students with high performance needs.
     *
     * @return List of students with high performance needs
     */
    public List<Student> getStudentsWithHighPerformanceNeeds() {
        List<Student> highPerformanceStudents = new ArrayList<>();
        
        synchronized (studentsCache) {
            for (Student student : studentsCache) {
                if (student.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                    highPerformanceStudents.add(student);
                }
            }
        }
        
        return highPerformanceStudents;
    }
    
    /**
     * Gets students with low performance needs.
     *
     * @return List of students with low performance needs
     */
    public List<Student> getStudentsWithLowPerformanceNeeds() {
        List<Student> lowPerformanceStudents = new ArrayList<>();
        
        synchronized (studentsCache) {
            for (Student student : studentsCache) {
                if (student.getPerformanceNeeded() == PerformanceTypeEnum.LOW) {
                    lowPerformanceStudents.add(student);
                }
            }
        }
        
        return lowPerformanceStudents;
    }
    
    /**
     * Gets students who have a laptop.
     *
     * @return List of students with laptops
     */
    public List<Student> getStudentsWithLaptop() {
        List<Student> studentsWithLaptop = new ArrayList<>();
        
        synchronized (studentsCache) {
            for (Student student : studentsCache) {
                if (student.isHasLaptop()) {
                    studentsWithLaptop.add(student);
                }
            }
        }
        
        return studentsWithLaptop;
    }
    
    /**
     * Gets the count of students with high performance needs.
     *
     * @return Number of students with high performance needs
     */
    public int getCountOfStudentsWithHighPerformanceNeeds() {
        return getStudentsWithHighPerformanceNeeds().size();
    }
    
    /**
     * Gets the count of students with low performance needs.
     *
     * @return Number of students with low performance needs
     */
    public int getCountOfStudentsWithLowPerformanceNeeds() {
        return getStudentsWithLowPerformanceNeeds().size();
    }
    
    /**
     * Gets the count of students who have a laptop.
     *
     * @return Number of students with laptops
     */
    public int getCountOfStudentsWithLaptop() {
        return getStudentsWithLaptop().size();
    }
    
    /**
     * Creates a student asynchronously.
     *
     * @param student The student to create
     */
    public void createStudent(Student student) {
        executor.submit(() -> {
            try {
                boolean success = studentDAO.insert(student);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to create student in database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error creating student: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error creating student", e));
            }
        });
    }
    
    /**
     * Updates a student asynchronously.
     *
     * @param student The student to update
     */
    public void updateStudent(Student student) {
        executor.submit(() -> {
            try {
                boolean success = studentDAO.update(student);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to update student in database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error updating student: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error updating student", e));
            }
        });
    }
    
    /**
     * Deletes a student asynchronously.
     *
     * @param id VIA ID of the student to delete
     */
    public void deleteStudent(int id) {
        executor.submit(() -> {
            try {
                boolean success = studentDAO.delete(id);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to delete student from database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error deleting student: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error deleting student", e));
            }
        });
    }
    
    /**
     * Updates the cache when receiving events from StudentDAO.
     */
    @Override
    public void update(Observable o, Object arg) {
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
                        // Forward error events
                        setChanged();
                        notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                                event.getData().toString(), event.getException()));
                        break;
                }
            }
        }
    }
    
    /**
     * Handles student creation events.
     *
     * @param student The created student
     */
    private void handleStudentCreated(Student student) {
        boolean added = false;
        
        synchronized (studentsCache) {
            // Check if student is already in cache
            boolean exists = false;
            for (Student s : studentsCache) {
                if (s.getViaId() == student.getViaId()) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                studentsCache.add(student);
                added = true;
            }
        }
        
        if (added) {
            // Notify observers
            setChanged();
            notifyObservers(new ViewModelEvent(EVENT_STUDENT_CREATED, student));
        }
    }
    
    /**
     * Handles student update events.
     *
     * @param student The updated student
     */
    private void handleStudentUpdated(Student student) {
        boolean updated = false;
        
        synchronized (studentsCache) {
            for (int i = 0; i < studentsCache.size(); i++) {
                if (studentsCache.get(i).getViaId() == student.getViaId()) {
                    studentsCache.set(i, student);
                    updated = true;
                    break;
                }
            }
        }
        
        if (updated) {
            // Notify observers
            setChanged();
            notifyObservers(new ViewModelEvent(EVENT_STUDENT_UPDATED, student));
        }
    }
    
    /**
     * Handles student deletion events.
     *
     * @param student The deleted student
     */
    private void handleStudentDeleted(Student student) {
        boolean removed = false;
        
        synchronized (studentsCache) {
            Iterator<Student> iterator = studentsCache.iterator();
            while (iterator.hasNext()) {
                Student s = iterator.next();
                if (s.getViaId() == student.getViaId()) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
        }
        
        if (removed) {
            // Notify observers
            setChanged();
            notifyObservers(new ViewModelEvent(EVENT_STUDENT_DELETED, student));
        }
    }
    
    /**
     * Closes resources when the ViewModel is no longer needed.
     */
    public void close() {
        studentDAO.deleteObserver(this);
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