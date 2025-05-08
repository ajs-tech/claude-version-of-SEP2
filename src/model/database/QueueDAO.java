package model.database;

import model.enums.PerformanceTypeEnum;
import model.models.Student;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for queue management with database persistence.
 * Uses Java's built-in Observable pattern.
 */
public class QueueDAO extends Observable {
    // Event types for observer notifications
    public static final String EVENT_STUDENT_ADDED_TO_QUEUE = "STUDENT_ADDED_TO_QUEUE";
    public static final String EVENT_STUDENT_REMOVED_FROM_QUEUE = "STUDENT_REMOVED_FROM_QUEUE";
    public static final String EVENT_QUEUE_ERROR = "QUEUE_ERROR";
    
    private static final Logger logger = Logger.getLogger(QueueDAO.class.getName());
    
    // Singleton instance with lazy initialization
    private static QueueDAO instance;
    
    // DAO dependencies
    private final StudentDAO studentDAO;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private QueueDAO() {
        this.studentDAO = StudentDAO.getInstance();
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance
     */
    public static synchronized QueueDAO getInstance() {
        if (instance == null) {
            instance = new QueueDAO();
        }
        return instance;
    }
    
    /**
     * Adds a student to the performance queue in the database.
     *
     * @param student The student to add
     * @param performanceType The performance type queue (HIGH/LOW)
     * @return true if addition was successful
     * @throws SQLException if a database error occurs
     */
    public boolean addToQueue(Student student, PerformanceTypeEnum performanceType) throws SQLException {
        String sql = "INSERT INTO QueueEntry (student_via_id, performance_type, entry_date) VALUES (?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, student.getViaId());
            stmt.setString(2, performanceType.name());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Student added to " + performanceType + " queue: " + 
                        student.getName() + " (VIA ID: " + student.getViaId() + ")");
                
                // Get new queue size for notification
                int newQueueSize = getQueueSize(performanceType);
                
                // Notify observers
                setChanged();
                notifyObservers(new QueueEvent(
                        EVENT_STUDENT_ADDED_TO_QUEUE,
                        student,
                        performanceType,
                        newQueueSize
                ));
                
                return true;
            } else {
                logger.warning("Failed to add student to queue in database: VIA ID " + student.getViaId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error adding student to queue: " + student.getViaId(), e);
            throw e;
        }
    }
    
    /**
     * Gets students in queue for a specific performance type, ordered by entry date.
     *
     * @param performanceType The performance type to get students for
     * @return List of students in the queue
     * @throws SQLException if a database error occurs
     */
    public List<Student> getStudentsInQueue(PerformanceTypeEnum performanceType) throws SQLException {
        List<Student> studentsInQueue = new ArrayList<>();
        String sql = "SELECT student_via_id FROM QueueEntry WHERE performance_type = ? ORDER BY entry_date ASC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, performanceType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int studentViaId = rs.getInt("student_via_id");
                    Student student = studentDAO.getById(studentViaId);
                    if (student != null) {
                        studentsInQueue.add(student);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving students in " + performanceType + " queue", e);
            throw e;
        }
        
        return studentsInQueue;
    }
    
    /**
     * Removes a student from a queue.
     *
     * @param studentViaId The student's VIA ID
     * @param performanceType The performance type queue
     * @return true if removal was successful
     * @throws SQLException if a database error occurs
     */
    public boolean removeFromQueue(int studentViaId, PerformanceTypeEnum performanceType) throws SQLException {
        // First get the student to notify observers after removal
        Student student = studentDAO.getById(studentViaId);
        
        if (student == null) {
            return false;
        }
        
        String sql = "DELETE FROM QueueEntry WHERE student_via_id = ? AND performance_type = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentViaId);
            stmt.setString(2, performanceType.name());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Student removed from " + performanceType + " queue: " + 
                        student.getName() + " (VIA ID: " + studentViaId + ")");
                
                // Get new queue size for notification
                int newQueueSize = getQueueSize(performanceType);
                
                // Notify observers - assume not assigned laptop here
                setChanged();
                notifyObservers(new QueueRemovalEvent(
                        EVENT_STUDENT_REMOVED_FROM_QUEUE,
                        student,
                        performanceType,
                        newQueueSize,
                        false
                ));
                
                return true;
            } else {
                logger.warning("Failed to remove student from queue: VIA ID " + studentViaId);
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error removing student from queue: " + studentViaId, e);
            throw e;
        }
    }
    
    /**
     * Removes a student from all queues.
     *
     * @param studentViaId The student's VIA ID
     * @return true if removal was successful
     * @throws SQLException if a database error occurs
     */
    public boolean removeFromAllQueues(int studentViaId) throws SQLException {
        // First get the student to notify observers after removal
        Student student = studentDAO.getById(studentViaId);
        
        if (student == null) {
            return false;
        }
        
        // Check which queues the student is in before deletion
        boolean inHighQueue = isStudentInQueue(studentViaId, PerformanceTypeEnum.HIGH);
        boolean inLowQueue = isStudentInQueue(studentViaId, PerformanceTypeEnum.LOW);
        
        String sql = "DELETE FROM QueueEntry WHERE student_via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentViaId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Student removed from all queues: " + 
                        student.getName() + " (VIA ID: " + studentViaId + ")");
                
                // Post events for each queue the student was in
                if (inHighQueue) {
                    int newHighQueueSize = getQueueSize(PerformanceTypeEnum.HIGH);
                    setChanged();
                    notifyObservers(new QueueRemovalEvent(
                            EVENT_STUDENT_REMOVED_FROM_QUEUE,
                            student,
                            PerformanceTypeEnum.HIGH,
                            newHighQueueSize,
                            false
                    ));
                }
                
                if (inLowQueue) {
                    int newLowQueueSize = getQueueSize(PerformanceTypeEnum.LOW);
                    setChanged();
                    notifyObservers(new QueueRemovalEvent(
                            EVENT_STUDENT_REMOVED_FROM_QUEUE,
                            student,
                            PerformanceTypeEnum.LOW,
                            newLowQueueSize,
                            false
                    ));
                }
                
                return true;
            } else {
                logger.warning("Failed to remove student from queues or student was not in any queue: VIA ID " + studentViaId);
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error removing student from all queues: " + studentViaId, e);
            throw e;
        }
    }
    
    /**
     * Gets the next student in queue for a performance type and removes them with transaction safety.
     *
     * @param performanceType The performance type to get a student from
     * @return The student or null if the queue is empty
     * @throws SQLException if a database error occurs
     */
    public Student getAndRemoveNextInQueue(PerformanceTypeEnum performanceType) throws SQLException {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Get the next student in queue
            String selectSql = "SELECT student_via_id, entry_id FROM QueueEntry WHERE performance_type = ? ORDER BY entry_date ASC LIMIT 1";
            Student student = null;
            int entryId = -1;
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, performanceType.name());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int studentViaId = rs.getInt("student_via_id");
                        entryId = rs.getInt("entry_id");
                        student = studentDAO.getById(studentViaId);
                    }
                }
            }
            
            // Remove from queue if found
            if (student != null && entryId > 0) {
                String deleteSql = "DELETE FROM QueueEntry WHERE entry_id = ?";
                
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, entryId);
                    deleteStmt.executeUpdate();
                }
                
                logger.info("Student retrieved and removed from " + performanceType + " queue: " + 
                        student.getName() + " (VIA ID: " + student.getViaId() + ")");
                
                // Get new queue size for notification
                int newQueueSize = getQueueSize(performanceType);
                
                // Notify observers
                setChanged();
                notifyObservers(new QueueRemovalEvent(
                        EVENT_STUDENT_REMOVED_FROM_QUEUE,
                        student,
                        performanceType,
                        newQueueSize,
                        true
                ));
            }
            
            conn.commit();
            return student;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warning("Transaction rolled back: " + e.getMessage());
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Error during rollback: " + ex.getMessage(), ex);
                }
            }
            
            handleSQLException("Error retrieving and removing student from queue", e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error resetting auto-commit: " + e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Checks if a student is in a specific queue.
     *
     * @param studentViaId The student's VIA ID
     * @param performanceType The performance type queue
     * @return true if the student is in the queue
     * @throws SQLException if a database error occurs
     */
    public boolean isStudentInQueue(int studentViaId, PerformanceTypeEnum performanceType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry WHERE student_via_id = ? AND performance_type = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentViaId);
            stmt.setString(2, performanceType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error checking if student is in queue: " + studentViaId, e);
            throw e;
        }
        
        return false;
    }
    
    /**
     * Checks if a student is in any queue.
     *
     * @param studentViaId The student's VIA ID
     * @return true if the student is in any queue
     * @throws SQLException if a database error occurs
     */
    public boolean isStudentInAnyQueue(int studentViaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry WHERE student_via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentViaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error checking if student is in any queue: " + studentViaId, e);
            throw e;
        }
        
        return false;
    }
    
    /**
     * Gets the size of a specific queue.
     *
     * @param performanceType The performance type to check
     * @return The number of students in the queue
     * @throws SQLException if a database error occurs
     */
    public int getQueueSize(PerformanceTypeEnum performanceType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry WHERE performance_type = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, performanceType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error getting queue size for " + performanceType, e);
            throw e;
        }
        
        return 0;
    }
    
    /**
     * Gets the total number of students in queues.
     *
     * @return Total number of students in all queues
     * @throws SQLException if a database error occurs
     */
    public int getTotalQueueSize() throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            handleSQLException("Error getting total queue size", e);
            throw e;
        }
        
        return 0;
    }
    
    /**
     * Clears a specific queue completely.
     *
     * @param performanceType The performance type queue to clear
     * @return Number of students removed
     * @throws SQLException if a database error occurs
     */
    public int clearQueue(PerformanceTypeEnum performanceType) throws SQLException {
        // First get students in the queue to notify observers
        List<Student> studentsInQueue = getStudentsInQueue(performanceType);
        
        String sql = "DELETE FROM QueueEntry WHERE performance_type = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, performanceType.name());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Cleared " + performanceType + " queue: " + affectedRows + " students removed");
                
                // Post events for each student
                for (Student student : studentsInQueue) {
                    setChanged();
                    notifyObservers(new QueueRemovalEvent(
                            EVENT_STUDENT_REMOVED_FROM_QUEUE,
                            student,
                            performanceType,
                            0,
                            false
                    ));
                }
            }
            
            return affectedRows;
        } catch (SQLException e) {
            handleSQLException("Error clearing " + performanceType + " queue", e);
            throw e;
        }
    }
    
    /**
     * Clears all queues.
     *
     * @return Number of students removed
     * @throws SQLException if a database error occurs
     */
    public int clearAllQueues() throws SQLException {
        // First get students in the queues to notify observers
        List<Student> highQueueStudents = getStudentsInQueue(PerformanceTypeEnum.HIGH);
        List<Student> lowQueueStudents = getStudentsInQueue(PerformanceTypeEnum.LOW);
        
        String sql = "DELETE FROM QueueEntry";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            int affectedRows = stmt.executeUpdate(sql);
            
            if (affectedRows > 0) {
                logger.info("Cleared all queues: " + affectedRows + " students removed");
                
                // Post events for each student in HIGH queue
                for (Student student : highQueueStudents) {
                    setChanged();
                    notifyObservers(new QueueRemovalEvent(
                            EVENT_STUDENT_REMOVED_FROM_QUEUE,
                            student,
                            PerformanceTypeEnum.HIGH,
                            0,
                            false
                    ));
                }
                
                // Post events for each student in LOW queue
                for (Student student : lowQueueStudents) {
                    setChanged();
                    notifyObservers(new QueueRemovalEvent(
                            EVENT_STUDENT_REMOVED_FROM_QUEUE,
                            student,
                            PerformanceTypeEnum.LOW,
                            0,
                            false
                    ));
                }
            }
            
            return affectedRows;
        } catch (SQLException e) {
            handleSQLException("Error clearing all queues", e);
            throw e;
        }
    }
    
    /**
     * Handles SQLException by logging and notifying observers.
     *
     * @param message Error message
     * @param e SQLException that occurred
     */
    private void handleSQLException(String message, SQLException e) {
        logger.log(Level.SEVERE, message + ": " + e.getMessage(), e);
        
        // Notify observers about the error
        setChanged();
        notifyObservers(new DatabaseEvent(EVENT_QUEUE_ERROR, message + ": " + e.getMessage(), e));
    }
    
    /**
     * Base event class for queue operations.
     */
    public static class DatabaseEvent {
        private final String eventType;
        private final Object data;
        private final SQLException exception;
        
        public DatabaseEvent(String eventType, Object data) {
            this(eventType, data, null);
        }
        
        public DatabaseEvent(String eventType, Object data, SQLException exception) {
            this.eventType = eventType;
            this.data = data;
            this.exception = exception;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public Object getData() {
            return data;
        }
        
        public SQLException getException() {
            return exception;
        }
    }
    
    /**
     * Event class for queue additions.
     */
    public static class QueueEvent extends DatabaseEvent {
        private final Student student;
        private final PerformanceTypeEnum queueType;
        private final int queueSize;
        
        public QueueEvent(String eventType, Student student, 
                         PerformanceTypeEnum queueType, int queueSize) {
            super(eventType, student);
            this.student = student;
            this.queueType = queueType;
            this.queueSize = queueSize;
        }
        
        public Student getStudent() {
            return student;
        }
        
        public PerformanceTypeEnum getQueueType() {
            return queueType;
        }
        
        public int getQueueSize() {
            return queueSize;
        }
    }
    
    /**
     * Event class for queue removals.
     */
    public static class QueueRemovalEvent extends QueueEvent {
        private final boolean wasAssignedLaptop;
        
        public QueueRemovalEvent(String eventType, Student student, 
                                PerformanceTypeEnum queueType, int queueSize, 
                                boolean wasAssignedLaptop) {
            super(eventType, student, queueType, queueSize);
            this.wasAssignedLaptop = wasAssignedLaptop;
        }
        
        public boolean wasAssignedLaptop() {
            return wasAssignedLaptop;
        }
    }
}