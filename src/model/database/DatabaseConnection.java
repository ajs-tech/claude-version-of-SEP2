package model.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import model.events.SystemEvents;
import model.log.Log;
import model.util.EventBus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton klasse til at håndtere database forbindelser med connection pool.
 * Har forbedret robust fejlhåndtering og overvågning af forbindelser.
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    // Connection pool
    private static ComboPooledDataSource cpds;
    private static boolean initialized = false;
    private static boolean poolClosed = false;

    // Database configuration
    private static final String DEFAULT_DB_DRIVER = "org.postgresql.Driver";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DEFAULT_DB_USER = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "Seshej1991";

    // Connection statistics
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);
    private static final AtomicInteger failedConnectionCounter = new AtomicInteger(0);

    // Maximum retry attempts
    private static final int MAX_RETRY_ATTEMPTS = 3;

    static {
        try {
            // Initialize connection pool
            initializeConnectionPool();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fejl ved initialisering af database forbindelsespool", e);
            log.critical("Fejl ved initialisering af database forbindelsespool: " + e.getMessage());

            // Post database error event
            eventBus.post(new SystemEvents.DatabaseErrorEvent(
                    "Fejl ved initialisering af database forbindelsespool",
                    null,
                    e));
        }
    }

    /**
     * Initialiserer forbindelsespoolen med standardværdier eller konfiguration fra properties.
     */
    private static void initializeConnectionPool() {
        if (initialized && !poolClosed) {
            return;
        }

        try {
            // Create a new connection pool
            cpds = new ComboPooledDataSource();

            // Set database driver
            cpds.setDriverClass(getDbProperty("db.driver", DEFAULT_DB_DRIVER));

            // Set database connection info
            cpds.setJdbcUrl(getDbProperty("db.url", DEFAULT_DB_URL));
            cpds.setUser(getDbProperty("db.user", DEFAULT_DB_USER));
            cpds.setPassword(getDbProperty("db.password", DEFAULT_DB_PASSWORD));

            // Configure pool size
            cpds.setInitialPoolSize(5);
            cpds.setMinPoolSize(5);
            cpds.setMaxPoolSize(20);
            cpds.setAcquireIncrement(5);

            // Configure statement caching
            cpds.setMaxStatements(100);

            // Configure connection testing
            cpds.setIdleConnectionTestPeriod(300);
            cpds.setTestConnectionOnCheckout(false);
            cpds.setTestConnectionOnCheckin(true);

            // Configure timeouts
            cpds.setCheckoutTimeout(10000); // 10 seconds timeout for connection checkout
            cpds.setMaxIdleTime(1800);      // 30 minutes max idle time
            cpds.setMaxConnectionAge(14400); // 4 hours max connection age

            // Configure automatic connection testing
            cpds.setPreferredTestQuery("SELECT 1");

            // Configure retry settings
            cpds.setAcquireRetryAttempts(3);
            cpds.setAcquireRetryDelay(1000); // 1 second between retries

            initialized = true;
            poolClosed = false;

            logger.info("Database forbindelsespool initialiseret");
            log.info("Database forbindelsespool initialiseret");

            // Reset counters
            connectionCounter.set(0);
            failedConnectionCounter.set(0);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fejl ved konfiguration af database forbindelsespool", e);
            log.critical("Fejl ved konfiguration af database forbindelsespool: " + e.getMessage());

            // Post database error event
            eventBus.post(new SystemEvents.DatabaseErrorEvent(
                    "Fejl ved konfiguration af database forbindelsespool",
                    null,
                    e));

            throw new RuntimeException("Kunne ikke initialisere database forbindelsespool", e);
        }
    }

    /**
     * Henter en databaseindstilling fra system properties eller environment variables.
     *
     * @param key          Property nøgle
     * @param defaultValue Standardværdi
     * @return Indstillingens værdi
     */
    private static String getDbProperty(String key, String defaultValue) {
        // Check system properties
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        // Check environment variables (convert db.url to DB_URL)
        String envKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        // Return default value
        return defaultValue;
    }

    /**
     * Privat konstruktør for singleton pattern
     */
    private DatabaseConnection() {
        // Private constructor - can't be instantiated
    }

    /**
     * Henter en forbindelse fra poolen med retry mekanisme.
     *
     * @return Database forbindelse
     * @throws SQLException hvis der er problemer med at etablere forbindelsen
     */
    public static Connection getConnection() throws SQLException {
        if (poolClosed) {
            throw new SQLException("Connection pool er lukket");
        }

        if (!initialized) {
            initializeConnectionPool();
        }

        int attempts = 0;
        SQLException lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Connection conn = cpds.getConnection();

                // Log connection checkout
                connectionCounter.incrementAndGet();

                // Log connection retrieval at high debug level
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Database forbindelse hentet fra pool (total: " +
                            connectionCounter.get() + ")");
                }

                return conn;
            } catch (SQLException e) {
                attempts++;
                lastException = e;

                failedConnectionCounter.incrementAndGet();
                logger.log(Level.WARNING, "Fejl ved hentning af database forbindelse (forsøg " +
                        attempts + "): " + e.getMessage(), e);

                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        // Wait before retrying
                        Thread.sleep(1000 * attempts); // Increasing delay between retries
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Log pool status on failure
        logPoolStatus();

        // If we get here, all attempts failed
        String errorMsg = "Kunne ikke oprette databaseforbindelse efter " +
                MAX_RETRY_ATTEMPTS + " forsøg";

        log.error(errorMsg + ": " +
                (lastException != null ? lastException.getMessage() : "Ukendt fejl"));

        // Post database error event
        eventBus.post(new SystemEvents.DatabaseErrorEvent(
                errorMsg,
                lastException != null ? lastException.getSQLState() : null,
                lastException));

        throw lastException != null ? lastException :
                new SQLException(errorMsg);
    }

    /**
     * Lukker forbindelsespoolen - kald denne ved programafslutning.
     */
    public static void closePool() {
        if (cpds != null && !poolClosed) {
            logger.info("Lukker database forbindelsespool");
            log.info("Lukker database forbindelsespool");

            // Close the pool
            cpds.close();

            poolClosed = true;
            initialized = false;

            // Log final connection statistics
            logger.info("Total connections created: " + connectionCounter.get());
            logger.info("Total failed connection attempts: " + failedConnectionCounter.get());
        }
    }

    /**
     * Tester om databasen er tilgængelig.
     *
     * @return true hvis databasen er tilgængelig
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database forbindelsestest fejlede: " + e.getMessage(), e);
            log.warning("Database forbindelsestest fejlede: " + e.getMessage());
            return false;
        }
    }

    /**
     * Henter statistik om connection pool status.
     *
     * @return String med pool statistik
     */
    public static String getPoolStats() {
        try {
            StringBuilder stats = new StringBuilder();
            stats.append("Database Connection Pool Status:\n");
            stats.append("  Connections in use: ").append(cpds.getNumBusyConnections()).append("\n");
            stats.append("  Idle connections: ").append(cpds.getNumIdleConnections()).append("\n");
            stats.append("  Total connections: ").append(cpds.getNumConnections()).append("\n");
            stats.append("  Threads waiting: ").append(cpds.getThreadPoolNumActiveThreads()).append("\n");
            stats.append("  Total created: ").append(connectionCounter.get()).append("\n");
            stats.append("  Total failed attempts: ").append(failedConnectionCounter.get());
            return stats.toString();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af pool statistik", e);
            return "Kunne ikke hente pool statistik: " + e.getMessage();
        }
    }

    /**
     * Logger detaljeret statistik om connection pool status.
     */
    public static void logPoolStatus() {
        try {
            logger.info(getPoolStats());
            log.info(getPoolStats());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fejl ved logging af pool status", e);
        }
    }

    /**
     * Forsøger at geninitialisere connection pool ved alvorlige problemer.
     */
    public static void reinitializePool() {
        logger.warning("Geninitialiserer database forbindelsespool");
        log.warning("Geninitialiserer database forbindelsespool");

        try {
            // Close the current pool
            if (cpds != null && !poolClosed) {
                cpds.close();
                poolClosed = true;
            }

            // Reset for new initialization
            initialized = false;

            // Create a new pool
            initializeConnectionPool();

            logger.info("Database forbindelsespool geninitialiseret succesfuldt");
            log.info("Database forbindelsespool geninitialiseret succesfuldt");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fejl ved geninitialisering af database forbindelsespool", e);
            log.critical("Fejl ved geninitialisering af database forbindelsespool: " + e.getMessage());

            // Post database error event
            eventBus.post(new SystemEvents.DatabaseErrorEvent(
                    "Fejl ved geninitialisering af database forbindelsespool",
                    null,
                    e));

            throw new RuntimeException("Kunne ikke geninitialisere database forbindelsespool", e);
        }
    }
}