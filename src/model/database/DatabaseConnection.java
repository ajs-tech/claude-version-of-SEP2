package model.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Observable;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class for managing database connections.
 * Implements thread-safe initialization and connection tracking.
 */
public class DatabaseConnection extends Observable {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    // Path to configuration file
    private static final String CONFIG_FILE = "src/main/resources/config/database.properties";

    // Singleton instance with double-checked locking
    private static volatile DatabaseConnection instance;
    private static final Lock initLock = new ReentrantLock();

    // Database configuration
    private String jdbcUrl;
    private String username;
    private String password;
    private int maxRetryAttempts;
    private int retryDelay;

    // Connection statistics
    private final AtomicInteger connectionCounter = new AtomicInteger(0);
    private final AtomicInteger failedConnectionCounter = new AtomicInteger(0);

    // Event types for notification
    public static final String EVENT_CONNECTION_ERROR = "CONNECTION_ERROR";
    public static final String EVENT_CONNECTION_SUCCESS = "CONNECTION_SUCCESS";
    public static final String EVENT_POOL_STATISTICS = "POOL_STATISTICS";

    /**
     * Private constructor for Singleton pattern.
     * Loads configuration from properties file.
     *
     * @throws RuntimeException if configuration fails
     */
    private DatabaseConnection() {
        try {
            // Load configuration
            Properties props = loadDatabaseProperties();

            // Set connection parameters
            this.jdbcUrl = props.getProperty("db.url");
            this.username = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
            this.maxRetryAttempts = Integer.parseInt(props.getProperty("db.retry.attempts", "3"));
            this.retryDelay = Integer.parseInt(props.getProperty("db.retry.delay", "500"));

            // Load driver
            Class.forName("org.postgresql.Driver");

            logger.info("Database connection configuration initialized");
        } catch (ClassNotFoundException e) {
            String errorMsg = "PostgreSQL JDBC driver not found";
            logger.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Error initializing database connection";
            logger.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Gets the singleton instance of model.database.DatabaseConnection.
     * Uses double-checked locking for thread safety.
     *
     * @return The singleton instance
     */
    public static DatabaseConnection getInstance() {
        // First check without locking
        if (instance == null) {
            // Lock for initialization
            initLock.lock();
            try {
                // Second check with locking
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            } finally {
                initLock.unlock();
            }
        }
        return instance;
    }

    /**
     * Gets a database connection with retry mechanism.
     *
     * @return A database connection
     * @throws SQLException if a connection cannot be established after retries
     */
    public Connection getConnection() throws SQLException {
        SQLException lastException = null;

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                Connection conn = DriverManager.getConnection(jdbcUrl, username, password);

                // Log successful connection
                connectionCounter.incrementAndGet();

                // Notify observers
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_CONNECTION_SUCCESS,
                        "Connection established", connectionCounter.get()));

                return conn;
            } catch (SQLException e) {
                lastException = e;
                failedConnectionCounter.incrementAndGet();

                logger.log(Level.WARNING, "Failed to establish database connection (attempt " +
                        attempt + "/" + maxRetryAttempts + "): " + e.getMessage(), e);

                // Notify observers about connection error
                setChanged();
                notifyObservers(new DatabaseEvent(EVENT_CONNECTION_ERROR, e.getMessage(), e));

                // If this isn't the last attempt, wait before retrying
                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(retryDelay * attempt); // Increasing delay between retries
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Connection attempt interrupted", ie);
                    }
                }
            }
        }

        // If we get here, all attempts failed
        throw new SQLException("Failed to establish database connection after " +
                maxRetryAttempts + " attempts", lastException);
    }

    /**
     * Tests if a connection can be established.
     *
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database connection test failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets statistics about database connections.
     *
     * @return String with connection statistics
     */
    public String getConnectionStats() {
        String stats = "Database Connection Statistics:\n" +
                "  Total successful connections: " + connectionCounter.get() + "\n" +
                "  Total failed connection attempts: " + failedConnectionCounter.get();

        // Notify observers
        setChanged();
        notifyObservers(new DatabaseEvent(EVENT_POOL_STATISTICS, stats, null));

        return stats;
    }

    /**
     * Loads database properties from configuration file.
     *
     * @return Properties object with database configuration
     * @throws RuntimeException if properties cannot be loaded
     */
    private Properties loadDatabaseProperties() {
        Properties properties = new Properties();

        try {
            // First try with ClassLoader to find file in classpath
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);

            // If not found in classpath, try with FileInputStream
            if (inputStream == null) {
                inputStream = new FileInputStream(CONFIG_FILE);
            }

            properties.load(inputStream);
            inputStream.close();

            logger.info("Database configuration loaded from " + CONFIG_FILE);
        } catch (IOException e) {
            String errorMsg = "Could not load database.properties: " + e.getMessage();
            logger.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        return properties;
    }

    /**
     * Event class for database connection notifications.
     */
    public static class DatabaseEvent {
        private final String eventType;
        private final String message;
        private final Object data;

        public DatabaseEvent(String eventType, String message, Object data) {
            this.eventType = eventType;
            this.message = message;
            this.data = data;
        }

        public String getEventType() {
            return eventType;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }
    }
}