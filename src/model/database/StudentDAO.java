package model.database;

import model.enums.PerformanceTypeEnum;
import model.models.Student;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Student entities.
 * Uses Java's built-in Observable pattern.
 */
public class StudentDAO extends Observable {
    // Event types for observer notifications
    public static final String EVENT_STUDENT_CREATED = "STUDENT_CREATED";
    public static final String EVENT_STUDENT_UPDATED = "STUDENT_UPDATED";
    public static final String EVENT_STUDENT_DELETED = "STUDENT_DELETED";
    public static final String EVENT_STUDENT_ERROR = "STUDENT_ERROR";
    
    private static final Logger logger = Logger.getLogger(StudentDAO.class.getName());
    
    // Singleton instance with lazy initialization
    private static StudentDAO instance;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private StudentDAO() {
        // Private constructor to prevent direct instantiation
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance
     */
    public static synchronized StudentDAO getInstance() {
        if (instance == null) {
            instance = new StudentDAO();
        }
        return instance;
    }
    
    /**
     * Gets all students from the database.
     *
     * @return List of students
     * @throws SQLException if a database error occurs
     */
    public List<Student> getAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
                students.add(student);
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving all students", e);
            throw e;
        }
        
        return students;
    }
    
    /**
     * Gets a student by ID.
     *
     * @param id The student's VIA ID
     * @return The student or null if not found
     * @throws SQLException if a database error occurs
     */
    public Student getById(int id) throws SQLException {
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student WHERE via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving student with ID " + id, e);
            throw e;
        }
        
        return null;
    }
    
    /**
     * Inserts a new student into the database.
     *
     * @param student The student to insert
     * @return true if insertion was successful
     * @throws SQLException if a database error occurs
     */
    public boolean insert(Student student) throws SQLException {
        String sql = "INSERT INTO Student (via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, student.getViaId());
            stmt.setString(2, student.getName());
            stmt.setDate(3, new java.sql.Date(student.getDegreeEndDate().getTime()));
            stmt.setString(4, student.getDegreeTitle());
            stmt.setString(5, student.getEmail());
            stmt.setInt(6, student.getPhoneNumber());
            stmt.setString(7, student.getPerformanceNeeded().name());
            stmt.setBoolean(8, student.isHasLaptop());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Student created: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_STUDENT_CREATED, student));
                
                return true;
            } else {
                logger.warning("Failed to create student in database: " + student.getViaId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error inserting student: " + student.getViaId(), e);
            throw e;
        }
    }
    
    /**
     * Updates an existing student in the database.
     *
     * @param student The student with updated information
     * @return true if update was successful
     * @throws SQLException if a database error occurs
     */
    public boolean update(Student student) throws SQLException {
        String sql = "UPDATE Student SET name = ?, degree_end_date = ?, degree_title = ?, email = ?, " +
                "phone_number = ?, performance_needed = ?, has_laptop = ? WHERE via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, student.getName());
            stmt.setDate(2, new java.sql.Date(student.getDegreeEndDate().getTime()));
            stmt.setString(3, student.getDegreeTitle());
            stmt.setString(4, student.getEmail());
            stmt.setInt(5, student.getPhoneNumber());
            stmt.setString(6, student.getPerformanceNeeded().name());
            stmt.setBoolean(7, student.isHasLaptop());
            stmt.setInt(8, student.getViaId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Student updated: " + student.getName() + " (VIA ID: " + student.getViaId() + ")");
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_STUDENT_UPDATED, student));
                
                return true;
            } else {
                logger.warning("Failed to update student in database: " + student.getViaId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error updating student: " + student.getViaId(), e);
            throw e;
        }
    }
    
    /**
     * Deletes a student from the database.
     *
     * @param id The student's VIA ID
     * @return true if deletion was successful
     * @throws SQLException if a database error occurs
     */
    public boolean delete(int id) throws SQLException {
        // First get the student to notify observers after deletion
        Student student = getById(id);
        
        if (student == null) {
            return false;
        }
        
        // Delete related records first (reservations and queue entries)
        try {
            deleteRelatedRecords(id);
        } catch (SQLException e) {
            logger.warning("Warning: Could not delete all related records for student " + id + ": " + e.getMessage());
            // Continue anyway to try deleting the student
        }
        
        // Delete the student
        String sql = "DELETE FROM Student WHERE via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Student deleted: VIA ID " + id);
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_STUDENT_DELETED, student));
                
                return true;
            } else {
                logger.warning("Failed to delete student from database: " + id);
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error deleting student: " + id, e);
            throw e;
        }
    }
    
    /**
     * Deletes related records (reservations and queue entries) for a student.
     *
     * @param studentId The student's VIA ID
     * @throws SQLException if a database error occurs
     */
    private void deleteRelatedRecords(int studentId) throws SQLException {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Delete reservations
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Reservation WHERE student_via_id = ?")) {
                stmt.setInt(1, studentId);
                stmt.executeUpdate();
            }
            
            // Delete queue entries
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM QueueEntry WHERE student_via_id = ?")) {
                stmt.setInt(1, studentId);
                stmt.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Error during rollback: " + ex.getMessage(), ex);
                }
            }
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
     * Counts the number of students in the database.
     *
     * @return The number of students
     * @throws SQLException if a database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Student";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            handleSQLException("Error counting students", e);
            throw e;
        }
    }
    
    /**
     * Checks if a student exists in the database.
     *
     * @param id The student's VIA ID
     * @return true if the student exists
     * @throws SQLException if a database error occurs
     */
    public boolean exists(int id) throws SQLException {
        String sql = "SELECT 1 FROM Student WHERE via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("Error checking if student exists: " + id, e);
            throw e;
        }
    }
    
    /**
     * Gets students by performance type.
     *
     * @param performanceType The performance type to filter by
     * @return List of students with the specified performance type
     * @throws SQLException if a database error occurs
     */
    public List<Student> getByPerformanceType(PerformanceTypeEnum performanceType) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student WHERE performance_needed = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, performanceType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Student student = mapResultSetToStudent(rs);
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving students by performance type: " + performanceType, e);
            throw e;
        }
        
        return students;
    }
    
    /**
     * Gets students by laptop status.
     *
     * @param hasLaptop true for students with laptops, false for those without
     * @return List of students with the specified laptop status
     * @throws SQLException if a database error occurs
     */
    public List<Student> getByHasLaptop(boolean hasLaptop) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student WHERE has_laptop = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, hasLaptop);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Student student = mapResultSetToStudent(rs);
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving students by laptop status: " + hasLaptop, e);
            throw e;
        }
        
        return students;
    }
    
    /**
     * Maps a ResultSet row to a Student object.
     *
     * @param rs The ResultSet to map
     * @return A Student object
     * @throws SQLException if a database error occurs
     */
    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        int viaId = rs.getInt("via_id");
        String name = rs.getString("name");
        java.util.Date degreeEndDate = rs.getDate("degree_end_date");
        String degreeTitle = rs.getString("degree_title");
        String email = rs.getString("email");
        int phoneNumber = rs.getInt("phone_number");
        PerformanceTypeEnum performanceNeeded = PerformanceTypeEnum.valueOf(rs.getString("performance_needed"));
        boolean hasLaptop = rs.getBoolean("has_laptop");
        
        Student student = new Student(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);
        
        // Set hasLaptop explicitly if different from default
        if (hasLaptop != student.isHasLaptop()) {
            student.setHasLaptop(hasLaptop);
        }
        
        return student;
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
        notifyObservers(new DatabaseEvent(EVENT_STUDENT_ERROR, message + ": " + e.getMessage(), e));
    }
    
    /**
     * Event class for database operations.
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
}