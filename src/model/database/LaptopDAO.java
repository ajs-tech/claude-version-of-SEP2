package model.database;

import model.enums.PerformanceTypeEnum;
import model.models.Laptop;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for model.models.Laptop entities.
 * Uses Java's built-in Observable pattern.
 */
public class LaptopDAO extends Observable {
    // Event types for observer notifications
    public static final String EVENT_LAPTOP_CREATED = "LAPTOP_CREATED";
    public static final String EVENT_LAPTOP_UPDATED = "LAPTOP_UPDATED";
    public static final String EVENT_LAPTOP_DELETED = "LAPTOP_DELETED";
    public static final String EVENT_LAPTOP_STATE_CHANGED = "LAPTOP_STATE_CHANGED";
    public static final String EVENT_LAPTOP_ERROR = "LAPTOP_ERROR";
    
    private static final Logger logger = Logger.getLogger(LaptopDAO.class.getName());
    
    // Singleton instance with lazy initialization
    private static LaptopDAO instance;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private LaptopDAO() {
        // Private constructor to prevent direct instantiation
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance
     */
    public static synchronized LaptopDAO getInstance() {
        if (instance == null) {
            instance = new LaptopDAO();
        }
        return instance;
    }
    
    /**
     * Gets all laptops from the database.
     *
     * @return List of laptops
     * @throws SQLException if a database error occurs
     */
    public List<Laptop> getAll() throws SQLException {
        List<Laptop> laptops = new ArrayList<>();
        String sql = "SELECT laptop_uuid, brand, model, gigabyte, ram, performance_type, state FROM Laptop";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Laptop laptop = mapResultSetToLaptop(rs);
                laptops.add(laptop);
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving all laptops", e);
            throw e;
        }
        
        return laptops;
    }
    
    /**
     * Gets a laptop by ID.
     *
     * @param id The laptop's UUID
     * @return The laptop or null if not found
     * @throws SQLException if a database error occurs
     */
    public Laptop getById(UUID id) throws SQLException {
        String sql = "SELECT laptop_uuid, brand, model, gigabyte, ram, performance_type, state FROM Laptop WHERE laptop_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLaptop(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving laptop with ID " + id, e);
            throw e;
        }
        
        return null;
    }
    
    /**
     * Inserts a new laptop into the database.
     *
     * @param laptop The laptop to insert
     * @return true if insertion was successful
     * @throws SQLException if a database error occurs
     */
    public boolean insert(Laptop laptop) throws SQLException {
        String sql = "INSERT INTO Laptop (laptop_uuid, brand, model, gigabyte, ram, performance_type, state) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, laptop.getId());
            stmt.setString(2, laptop.getBrand());
            stmt.setString(3, laptop.getModel());
            stmt.setInt(4, laptop.getGigabyte());
            stmt.setInt(5, laptop.getRam());
            stmt.setString(6, laptop.getPerformanceType().name());
            stmt.setString(7, laptop.getStateClassName());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("model.models.Laptop created: " + laptop.getBrand() + " " + laptop.getModel() +
                        " (ID: " + laptop.getId() + ")");
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_LAPTOP_CREATED, laptop));
                
                return true;
            } else {
                logger.warning("Failed to create laptop in database: " + laptop.getId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error inserting laptop: " + laptop.getId(), e);
            throw e;
        }
    }
    
    /**
     * Updates an existing laptop in the database.
     *
     * @param laptop The laptop with updated information
     * @return true if update was successful
     * @throws SQLException if a database error occurs
     */
    public boolean update(Laptop laptop) throws SQLException {
        String sql = "UPDATE Laptop SET brand = ?, model = ?, gigabyte = ?, ram = ?, performance_type = ?, state = ? " +
                "WHERE laptop_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, laptop.getBrand());
            stmt.setString(2, laptop.getModel());
            stmt.setInt(3, laptop.getGigabyte());
            stmt.setInt(4, laptop.getRam());
            stmt.setString(5, laptop.getPerformanceType().name());
            stmt.setString(6, laptop.getStateClassName());
            stmt.setObject(7, laptop.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("model.models.Laptop updated: " + laptop.getBrand() + " " + laptop.getModel() +
                        " (ID: " + laptop.getId() + ")");
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_LAPTOP_UPDATED, laptop));
                
                return true;
            } else {
                logger.warning("Failed to update laptop in database: " + laptop.getId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error updating laptop: " + laptop.getId(), e);
            throw e;
        }
    }
    
    /**
     * Updates only the state of a laptop in the database.
     *
     * @param laptop The laptop with the new state
     * @return true if update was successful
     * @throws SQLException if a database error occurs
     */
    public boolean updateState(Laptop laptop) throws SQLException {
        String sql = "UPDATE Laptop SET state = ? WHERE laptop_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, laptop.getStateClassName());
            stmt.setObject(2, laptop.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("model.models.Laptop state updated: " + laptop.getBrand() + " " + laptop.getModel() +
                        " (ID: " + laptop.getId() + ") to " + laptop.getStateClassName());
                
                // Notify observers
                boolean isNowAvailable = laptop.isAvailable();
                setChanged();
                notifyObservers(new LaptopStateEvent(
                        EVENT_LAPTOP_STATE_CHANGED,
                        laptop,
                        isNowAvailable ? "LoanedState" : "model.models.AvailableState",
                        laptop.getStateClassName(),
                        isNowAvailable
                ));
                
                return true;
            } else {
                logger.warning("Failed to update laptop state in database: " + laptop.getId());
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error updating laptop state: " + laptop.getId(), e);
            throw e;
        }
    }
    
    /**
     * Deletes a laptop from the database.
     *
     * @param id The laptop's UUID
     * @return true if deletion was successful
     * @throws SQLException if a database error occurs
     */
    public boolean delete(UUID id) throws SQLException {
        // First get the laptop to notify observers after deletion
        Laptop laptop = getById(id);
        
        if (laptop == null) {
            return false;
        }
        
        // Delete related records first (reservations)
        try {
            deleteRelatedRecords(id);
        } catch (SQLException e) {
            logger.warning("Warning: Could not delete all related records for laptop " + id + ": " + e.getMessage());
            // Continue anyway to try deleting the laptop
        }
        
        // Delete the laptop
        String sql = "DELETE FROM Laptop WHERE laptop_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("model.models.Laptop deleted: ID " + id);
                
                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_LAPTOP_DELETED, laptop));
                
                return true;
            } else {
                logger.warning("Failed to delete laptop from database: " + id);
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Error deleting laptop: " + id, e);
            throw e;
        }
    }
    
    /**
     * Deletes related records (reservations) for a laptop.
     *
     * @param laptopId The laptop's UUID
     * @throws SQLException if a database error occurs
     */
    private void deleteRelatedRecords(UUID laptopId) throws SQLException {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Delete reservations
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Reservation WHERE laptop_uuid = ?")) {
                stmt.setObject(1, laptopId);
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
     * Gets all available laptops with a specific performance type.
     *
     * @param performanceType The performance type to filter by
     * @return List of available laptops with the specified performance type
     * @throws SQLException if a database error occurs
     */
    public List<Laptop> getAvailableLaptopsByPerformance(PerformanceTypeEnum performanceType) throws SQLException {
        List<Laptop> laptops = new ArrayList<>();
        String sql = "SELECT laptop_uuid, brand, model, gigabyte, ram, performance_type, state FROM Laptop " +
                "WHERE performance_type = ? AND state = 'model.models.AvailableState'";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, performanceType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Laptop laptop = mapResultSetToLaptop(rs);
                    laptops.add(laptop);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Error retrieving available laptops with performance type " + performanceType, e);
            throw e;
        }
        
        return laptops;
    }
    
    /**
     * Counts the number of laptops in the database.
     *
     * @return The number of laptops
     * @throws SQLException if a database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Laptop";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            handleSQLException("Error counting laptops", e);
            throw e;
        }
    }
    
    /**
     * Counts the number of laptops with a specific state.
     *
     * @param state The state to count
     * @return The number of laptops with the specified state
     * @throws SQLException if a database error occurs
     */
    public int countByState(String state) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Laptop WHERE state = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, state);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            handleSQLException("Error counting laptops with state " + state, e);
            throw e;
        }
    }
    
    /**
     * Counts the number of available laptops.
     *
     * @return The number of available laptops
     * @throws SQLException if a database error occurs
     */
    public int countAvailable() throws SQLException {
        return countByState("model.models.AvailableState");
    }
    
    /**
     * Counts the number of loaned laptops.
     *
     * @return The number of loaned laptops
     * @throws SQLException if a database error occurs
     */
    public int countLoaned() throws SQLException {
        return countByState("LoanedState");
    }
    
    /**
     * Checks if a laptop exists in the database.
     *
     * @param id The laptop's UUID
     * @return true if the laptop exists
     * @throws SQLException if a database error occurs
     */
    public boolean exists(UUID id) throws SQLException {
        String sql = "SELECT 1 FROM Laptop WHERE laptop_uuid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("Error checking if laptop exists: " + id, e);
            throw e;
        }
    }
    
    /**
     * Maps a ResultSet row to a model.models.Laptop object.
     *
     * @param rs The ResultSet to map
     * @return A model.models.Laptop object
     * @throws SQLException if a database error occurs
     */
    private Laptop mapResultSetToLaptop(ResultSet rs) throws SQLException {
        UUID laptopId = UUID.fromString(rs.getString("laptop_uuid"));
        String brand = rs.getString("brand");
        String model = rs.getString("model");
        int gigabyte = rs.getInt("gigabyte");
        int ram = rs.getInt("ram");
        PerformanceTypeEnum performanceType = PerformanceTypeEnum.valueOf(rs.getString("performance_type"));
        
        // Create laptop
        Laptop laptop = new Laptop(laptopId, brand, model, gigabyte, ram, performanceType);
        
        // Set state based on database value
        String stateName = rs.getString("state");
        if (stateName != null) {
            laptop.setStateFromDatabase(stateName);
        }
        
        return laptop;
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
        notifyObservers(new DatabaseEvent(EVENT_LAPTOP_ERROR, message + ": " + e.getMessage(), e));
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
     * Event class specifically for laptop state changes.
     */
    public static class LaptopStateEvent extends DatabaseEvent {
        private final String oldState;
        private final String newState;
        private final boolean isNowAvailable;
        
        public LaptopStateEvent(String eventType, Object data, String oldState, String newState, boolean isNowAvailable) {
            super(eventType, data);
            this.oldState = oldState;
            this.newState = newState;
            this.isNowAvailable = isNowAvailable;
        }
        
        public String getOldState() {
            return oldState;
        }
        
        public String getNewState() {
            return newState;
        }
        
        public boolean isNowAvailable() {
            return isNowAvailable;
        }
    }
}