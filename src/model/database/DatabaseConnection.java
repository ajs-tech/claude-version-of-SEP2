package model.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import model.events.SystemEvents;
import model.log.Log;
import model.util.EventBus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton klasse til at håndtere database forbindelser med connection pool.
 * Opdateret til at arbejde med Neon PostgreSQL.
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    // Connection pool
    private static ComboPooledDataSource cpds;
    private static boolean initialized = false;
    private static boolean poolClosed = false;

    // Sti til konfigurationsfilen
    private static final String CONFIG_FILE = "/Users/ajs/IdeaProjects/claude-version/src/main/resources/config/database.properties";

    // Forbindelsesstatistik
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);
    private static final AtomicInteger failedConnectionCounter = new AtomicInteger(0);

    // Maksimalt antal genforsøg
    private static final int MAX_RETRY_ATTEMPTS = 3;

    static {
        try {
            // Initialiser connection pool
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
     * Initialiserer forbindelsespoolen med værdier fra properties-fil eller standardværdier.
     */
    private static void initializeConnectionPool() {
        if (initialized && !poolClosed) {
            return;
        }

        try {
            // Opret en ny connection pool
            cpds = new ComboPooledDataSource();

            // Indlæs konfiguration fra properties-fil
            Properties dbProps = loadDatabaseProperties();

            // Sæt database driver
            cpds.setDriverClass(dbProps.getProperty("db.driver"));

            // Sæt database connection info
            cpds.setJdbcUrl(dbProps.getProperty("db.url"));
            cpds.setUser(dbProps.getProperty("db.user"));
            cpds.setPassword(dbProps.getProperty("db.password"));

            // Konfigurer pool-størrelse
            cpds.setInitialPoolSize(3);  // Start med færre forbindelser for serverless
            cpds.setMinPoolSize(1);      // Minimum kan være lavere for serverless
            cpds.setMaxPoolSize(10);     // Begræns max forbindelser for serverless
            cpds.setAcquireIncrement(1); // Forøg med 1 ad gangen

            // Konfigurer statement caching
            cpds.setMaxStatements(50);

            // Konfigurer forbindelsestest - vigtigt for serverless
            cpds.setIdleConnectionTestPeriod(60);  // Test inaktive forbindelser hvert minut
            cpds.setTestConnectionOnCheckout(true); // Test ved hentning af forbindelse
            cpds.setTestConnectionOnCheckin(true);  // Test ved returnering af forbindelse

            // Konfigurer timeouts - kortere for serverless
            cpds.setCheckoutTimeout(20000);  // 20 sekunder timeout for forbindelseshentning
            cpds.setMaxIdleTime(300);        // 5 minutter max inaktivitetstid
            cpds.setMaxConnectionAge(1800);  // 30 minutter max forbindelsesalder

            // Konfigurer automatisk forbindelsestest
            cpds.setPreferredTestQuery("SELECT 1");

            // Konfigurer retry-indstillinger
            cpds.setAcquireRetryAttempts(5);    // Flere forsøg for serverless
            cpds.setAcquireRetryDelay(500);     // Et halvt sekund mellem forsøg

            // Aktivér forbindelses-reset ved lukning
            cpds.setAutoCommitOnClose(true);

            // Tilføj forbindelsesegenskaber specifikt for Neon
            // I initializeConnectionPool-metoden
            Properties properties = new Properties();
            properties.setProperty("user", dbProps.getProperty("db.user"));
            properties.setProperty("password", dbProps.getProperty("db.password"));
            cpds.setProperties(properties);

            initialized = true;
            poolClosed = false;

            logger.info("Database forbindelsespool initialiseret med Neon PostgreSQL");
            log.info("Database forbindelsespool initialiseret med Neon PostgreSQL");

            // Nulstil tællere
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
     * Indlæser database-egenskaber fra konfigurationsfilen.
     *
     * @return Properties-objekt med databasekonfiguration
     */
    private static Properties loadDatabaseProperties() {
        Properties properties = new Properties();
        try {
            // Først forsøg med ClassLoader for at finde filen i classpath
            InputStream inputStream = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

            // Hvis ikke fundet i classpath, prøv med FileInputStream
            if (inputStream == null) {
                inputStream = new FileInputStream(CONFIG_FILE);
            }

            properties.load(inputStream);
            inputStream.close();
            logger.info("Indlæst databasekonfiguration fra " + CONFIG_FILE);
        } catch (IOException e) {
            logger.warning("Kunne ikke indlæse database.properties: " + e.getMessage());
            // Fortsæt uden konfiguration - vil fejle senere med bedre fejlbesked
        }
        return properties;
    }

    /**
     * Privat konstruktør for singleton pattern
     */
    private DatabaseConnection() {
        // Private konstruktør - kan ikke instantieres
    }

    /**
     * Henter en forbindelse fra poolen med retry-mekanisme.
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

                // Log forbindelseshentning på højt debug-niveau
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
                        // Vent før næste forsøg - længere for serverless pga. potentielle cold starts
                        Thread.sleep(1000 * attempts); // Stigende forsinkelse mellem forsøg
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Log pool-status ved fejl
        logPoolStatus();

        // Hvis vi når hertil, er alle forsøg mislykkedes
        String errorMsg = "Kunne ikke etablere databaseforbindelse efter " +
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

            // Luk poolen
            cpds.close();

            poolClosed = true;
            initialized = false;

            // Log endelige forbindelsesstatistikker
            logger.info("Total antal forbindelser oprettet: " + connectionCounter.get());
            logger.info("Total antal mislykkede forbindelsesforsøg: " + failedConnectionCounter.get());
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
     * @return String med pool-statistik
     */
    public static String getPoolStats() {
        try {
            StringBuilder stats = new StringBuilder();
            stats.append("Database Connection Pool Status:\n");
            stats.append("  Forbindelser i brug: ").append(cpds.getNumBusyConnections()).append("\n");
            stats.append("  Inaktive forbindelser: ").append(cpds.getNumIdleConnections()).append("\n");
            stats.append("  Total forbindelser: ").append(cpds.getNumConnections()).append("\n");
            stats.append("  Ventende tråde: ").append(cpds.getThreadPoolNumActiveThreads()).append("\n");
            stats.append("  Total oprettet: ").append(connectionCounter.get()).append("\n");
            stats.append("  Total mislykkede forsøg: ").append(failedConnectionCounter.get());
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
            // Luk den nuværende pool
            if (cpds != null && !poolClosed) {
                cpds.close();
                poolClosed = true;
            }

            // Nulstil for ny initialisering
            initialized = false;

            // Opret en ny pool
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