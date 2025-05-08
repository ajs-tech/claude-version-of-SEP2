package model.database;

import model.enums.ReservationStatusEnum;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Reservation entities.
 * Uses Java's built-in Observable pattern.
 */
public class ReservationDAO extends Observable {
    // Event types for observer notifications
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String EVENT_RESERVATION_UPDATED = "RESERVATION_UPDATED";
    public static final String EVENT_RESERVATION_DELETED = "RESERVATION_DELETED";
    public static final String EVENT_RESERVATION_STATUS_CHANGED = "RESERVATION_STATUS_CHANGED";
    public static final String EVENT_RESERVATION_ERROR = "RESERVATION_ERROR";
    
    private static final Logger logger = Logger.getLogger(ReservationDAO.class.getName());
    
    // Singleton instance with lazy initialization
    private static ReservationDAO instance;
    
    // DAO dependencies
    private final LaptopDAO laptopDAO;
    private final StudentDAO studentDAO;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private ReservationDAO() {
        this.laptopDAO = LaptopDAO.getInstance();
        this.studentDAO = StudentDAO.getInstance();
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance
     */
    public static synchronized ReservationDAO getInstance() {
        if (instance == null) {
            instance = new ReservationDAO();
        }
        return instance;
    }
    
    /**
     * Gets all reservations from the database.
     *
     * @return List of reservations
     * @throws SQLException if a database error occurs
     */
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.reservation_uuid, r.status, r.laptop_uuid, r.student_via_id, r.creation_date " +
                "FROM Reservation r";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Reservation reservation = mapResultSetToReservation(rs);
                if (reservation != null) {
                    reservations.add(reservation);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving all reservations", e);
            throw e;
        }
        
        return reservations;
    }
    
    /**
     * Gets a reservation by ID.
     *
     * @param id The reservation's UUID
     * @return The reservation or null if not found
     * @throws SQLException if a database error occurs
     */
    public Reservation getById(UUID id) throws SQLException {
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE reservation_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving reservation with ID " + id, e);
            throw e;
        }
        
        return null;
    }
    
    /**
     * Inserts a new reservation into the database.
     *
     * @param reservation The reservation to insert
     * @return true if insertion was successful
     * @throws SQLException if a database error occurs
     */
    public boolean insert(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO Reservation (reservation_uuid, laptop_uuid, student_via_id, status, creation_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, reservation.getReservationId());
            stmt.setObject(2, reservation.getLaptop().getId());
            stmt.setInt(3, reservation.getStudent().getViaId());
            stmt.setString(4, reservation.getStatus().name());
            stmt.setTimestamp(5, new Timestamp(reservation.getCreationDate().getTime()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Reservation created: " + reservation.getReservationId() +
                        " (Student: " + reservation.getStudent().getName() +
                        ", model.models.Laptop: " + reservation.getLaptop().getBrand() + " " + reservation.getLaptop().getModel() + ")");
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_RESERVATION_CREATED, reservation));
                
                return true;
            } else {
                logger.warning("Failed to create reservation in database: " + reservation.getReservationId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error inserting reservation: " + reservation.getReservationId(), e);
            throw e;
        }
    }
    
    /**
     * Updates an existing reservation in the database.
     *
     * @param reservation The reservation with updated information
     * @return true if update was successful
     * @throws SQLException if a database error occurs
     */
    public boolean update(Reservation reservation) throws SQLException {
        String sql = "UPDATE Reservation SET status = ? WHERE reservation_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, reservation.getStatus().name());
            stmt.setObject(2, reservation.getReservationId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Reservation updated: " + reservation.getReservationId() +
                        " (Status: " + reservation.getStatus().getDisplayName() + ")");
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_RESERVATION_UPDATED, reservation));
                
                return true;
            } else {
                logger.warning("Failed to update reservation in database: " + reservation.getReservationId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error updating reservation: " + reservation.getReservationId(), e);
            throw e;
        }
    }
    
    /**
     * Deletes a reservation from the database.
     *
     * @param id The reservation's UUID
     * @return true if deletion was successful
     * @throws SQLException if a database error occurs
     */
    public boolean delete(UUID id) throws SQLException {
        // First get the reservation to notify observers after deletion
        Reservation reservation = getById(id);
        
        if (reservation == null) {
            return false;
        }
        
        String sql = "DELETE FROM Reservation WHERE reservation_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Reservation deleted: " + id);
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_RESERVATION_DELETED, reservation));
                
                return true;
            } else {
                logger.warning("Failed to delete reservation from database: " + id);
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error deleting reservation: " + id, e);
            throw e;
        }
    }
    
    /**
     * Gets all reservations for a specific student.
     *
     * @param studentViaId The student's VIA ID
     * @return List of reservations for the student
     * @throws SQLException if a database error occurs
     */
    public List<Reservation> getByStudentId(int studentViaId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE student_via_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentViaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapResultSetToReservation(rs);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving reservations for student " + studentViaId, e);
            throw e;
        }
        
        return reservations;
    }
    
    /**
     * Gets all reservations for a specific laptop.
     *
     * @param laptopId The laptop's UUID
     * @return List of reservations for the laptop
     * @throws SQLException if a database error occurs
     */
    public List<Reservation> getByLaptopId(UUID laptopId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE laptop_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, laptopId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapResultSetToReservation(rs);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving reservations for laptop " + laptopId, e);
            throw e;
        }
        
        return reservations;
    }
    
    /**
     * Gets all active reservations.
     *
     * @return List of active reservations
     * @throws SQLException if a database error occurs
     */
    public List<Reservation> getActiveReservations() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE status = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ReservationStatusEnum.ACTIVE.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapResultSetToReservation(rs);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving active reservations", e);
            throw e;
        }
        
        return reservations;
    }
    
    /**
     * Creates a reservation with transaction support, updating laptop and student status.
     *
     * @param reservation The reservation to create
     * @return true if the operation was successful
     * @throws SQLException if a database error occurs
     */
    public boolean createReservationWithTransaction(Reservation reservation) throws SQLException {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insert reservation
            String sql = "INSERT INTO Reservation (reservation_uuid, laptop_uuid, student_via_id, status, creation_date) " +
                    "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, reservation.getReservationId());
                stmt.setObject(2, reservation.getLaptop().getId());
                stmt.setInt(3, reservation.getStudent().getViaId());
                stmt.setString(4, reservation.getStatus().name());
                stmt.setTimestamp(5, new Timestamp(reservation.getCreationDate().getTime()));
                stmt.executeUpdate();
            }
            
            // 2. Update laptop state
            sql = "UPDATE Laptop SET state = 'LoanedState' WHERE laptop_uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, reservation.getLaptop().getId());
                stmt.executeUpdate();
            }
            
            // 3. Update student has_laptop
            sql = "UPDATE Student SET has_laptop = TRUE WHERE via_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reservation.getStudent().getViaId());
                stmt.executeUpdate();
            }
            
            // Commit the transaction
            conn.commit();
            
            logger.info("Reservation created with transaction: " + reservation.getReservationId() +
                    " (Student: " + reservation.getStudent().getName() +
                    ", model.models.Laptop: " + reservation.getLaptop().getBrand() + " " + reservation.getLaptop().getModel() + ")");
            
            // Notify observers
            setChanged();
            notifyObservers(new DatabaseEvent(EVENT_RESERVATION_CREATED, reservation));
            
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warning("Transaction rolled back: " + e.getMessage());
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Error during rollback: " + ex.getMessage(), ex);
                }
            }
            
            handleSQLException("Error creating reservation with transaction: " + reservation.getReservationId(), e);
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
     * Updates reservation status with transaction support, updating laptop and student status.
     *
     * @param reservation The reservation with the new status
     * @return true if the operation was successful
     * @throws SQLException if a database error occurs
     */
    public boolean updateStatusWithTransaction(Reservation reservation) throws SQLException {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // 1. Get the current status
            String selectSql = "SELECT status FROM Reservation WHERE reservation_uuid = ?";
            ReservationStatusEnum currentStatus;
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setObject(1, reservation.getReservationId());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return false; // Reservation not found
                    }
                    currentStatus = ReservationStatusEnum.valueOf(rs.getString("status"));
                }
            }
            
            // 2. Update reservation status
            String updateSql = "UPDATE Reservation SET status = ? WHERE reservation_uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, reservation.getStatus().name());
                stmt.setObject(2, reservation.getReservationId());
                stmt.executeUpdate();
            }
            
            // 3. If status changes from Active to Completed or Cancelled
            if (currentStatus == ReservationStatusEnum.ACTIVE &&
                    (reservation.getStatus() == ReservationStatusEnum.COMPLETED ||
                            reservation.getStatus() == ReservationStatusEnum.CANCELLED)) {
                
                // Update laptop state to Available
                String laptopSql = "UPDATE Laptop SET state = 'model.models.AvailableState' WHERE laptop_uuid = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(laptopSql)) {
                    stmt.setObject(1, reservation.getLaptop().getId());
                    stmt.executeUpdate();
                }
                
                // Update student has_laptop status
                String studentSql = "UPDATE Student SET has_laptop = FALSE WHERE via_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(studentSql)) {
                    stmt.setInt(1, reservation.getStudent().getViaId());
                    stmt.executeUpdate();
                }
            }
            
            // Commit the transaction
            conn.commit();
            
            logger.info("Reservation status updated from " + currentStatus.getDisplayName() + 
                    " to " + reservation.getStatus().getDisplayName() + 
                    " (ID: " + reservation.getReservationId() + ")");
            
            // Notify observers
            setChanged();
            notifyObservers(new ReservationStatusEvent(
                    EVENT_RESERVATION_STATUS_CHANGED,
                    reservation,
                    currentStatus,
                    reservation.getStatus()
            ));
            
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warning("Transaction rolled back: " + e.getMessage());
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Error during rollback: " + ex.getMessage(), ex);
                }
            }
            
            handleSQLException("Error updating reservation status with transaction: " + reservation.getReservationId(), e);
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
     * Counts the number of reservations in the database.
     *
     * @return The number of reservations
     * @throws SQLException if a database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Reservation";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            handleSQLException("Error counting reservations", e);
            throw e;
        }
    }
    
    /**
     * Checks if a reservation exists in the database.
     *
     * @param id The reservation's UUID
     * @return true if the reservation exists
     * @throws SQLException if a database error occurs
     */
    public boolean exists(UUID id) throws SQLException {
        String sql = "SELECT 1 FROM Reservation WHERE reservation_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("Error checking if reservation exists: " + id, e);
            throw e;
        }
    }
    
    /**
     * Maps a ResultSet row to a Reservation object.
     *
     * @param rs The ResultSet to map
     * @return A Reservation object
     * @throws SQLException if a database error occurs
     */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        UUID reservationId = UUID.fromString(rs.getString("reservation_uuid"));
        ReservationStatusEnum status = ReservationStatusEnum.valueOf(rs.getString("status"));
        UUID laptopUUID = UUID.fromString(rs.getString("laptop_uuid"));
        int studentViaId = rs.getInt("student_via_id");
        Timestamp creationTimestamp = rs.getTimestamp("creation_date");
        Date creationDate = creationTimestamp != null ? new Date(creationTimestamp.getTime()) : new Date();
        
        // Get associated model.models.Laptop and Student using their DAOs
        Laptop laptop = laptopDAO.getById(laptopUUID);
        Student student = studentDAO.getById(studentViaId);
        
        // Check if model.models.Laptop and Student were found
        if (laptop == null) {
            logger.warning("model.models.Laptop with UUID " + laptopUUID + " not found for reservation " + reservationId);
            return null;
        }
        
        if (student == null) {
            logger.warning("Student with VIA ID " + studentViaId + " not found for reservation " + reservationId);
            return null;
        }
        
        // Create Reservation object
        return new Reservation(reservationId, student, laptop, status, creationDate);
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
        notifyObservers(new DatabaseEvent(EVENT_RESERVATION_ERROR, message + ": " + e.getMessage(), e));
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
    
    /**
     * Event class specifically for reservation status changes.
     */
    public static class ReservationStatusEvent extends DatabaseEvent {
        private final ReservationStatusEnum oldStatus;
        private final ReservationStatusEnum newStatus;
        
        public ReservationStatusEvent(String eventType, Object data, 
                                     ReservationStatusEnum oldStatus, ReservationStatusEnum newStatus) {
            super(eventType, data);
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }
        
        public ReservationStatusEnum getOldStatus() {
            return oldStatus;
        }
        
        public ReservationStatusEnum getNewStatus() {
            return newStatus;
        }
        
        public boolean isCompleted() {
            return newStatus == ReservationStatusEnum.COMPLETED;
        }
        
        public boolean isCancelled() {
            return newStatus == ReservationStatusEnum.CANCELLED;
        }
    }
}