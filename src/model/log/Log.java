package model.log;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced logging class with different log levels and notification support.
 * Implements Singleton pattern and thread safety.
 */
public class Log extends Observable {
    private static final Logger logger = Logger.getLogger(Log.class.getName());
    private static final String DEFAULT_LOG_PATH = "src/model/log/loglines.txt";

    // Event types for observer notifications
    public static final String EVENT_LOG_ADDED = "LOG_ADDED";
    public static final String EVENT_LOG_CLEARED = "LOG_CLEARED";

    // Log levels
    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, CRITICAL
    }

    // Singleton instance with thread safety
    private static volatile Log instance;
    private static final Lock initLock = new ReentrantLock();

    private final List<model.log.LogLine> logLines;
    private final String logFilePath;
    private Log.LogLevel minFileLogLevel;  // Minimum level for file logging
    private Log.LogLevel minMemoryLogLevel; // Minimum level for memory logging
    private final Lock logLock;

    /**
     * Private constructor for Singleton pattern.
     */
    private Log() {
        this(DEFAULT_LOG_PATH);
    }

    /**
     * Private constructor with specific log file.
     *
     * @param logFilePath Path to the log file
     */
    private Log(String logFilePath) {
        this.logLines = new ArrayList<>();
        this.logFilePath = logFilePath;
        this.minFileLogLevel = Log.LogLevel.INFO;     // Default: INFO and higher to file
        this.minMemoryLogLevel = Log.LogLevel.DEBUG;  // Default: ALL to memory
        this.logLock = new ReentrantLock();

        createFile();
    }

    /**
     * Gets the singleton instance with double-checked locking.
     *
     * @return The singleton instance
     */
    public static Log getInstance() {
        if (instance == null) {
            initLock.lock();
            try {
                if (instance == null) {
                    instance = new Log();
                }
            } finally {
                initLock.unlock();
            }
        }
        return instance;
    }

    /**
     * Creates the log file if it doesn't exist.
     */
    private void createFile() {
        try {
            File file = new File(logFilePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            if (file.createNewFile()) {
                System.out.println("Log file created at path: " + file.getAbsolutePath());
            } else {
                System.out.println("Log file already exists at path: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating log file: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the number of log entries.
     *
     * @return Number of log entries in memory
     */
    public int getLogLineSize() {
        logLock.lock();
        try {
            return logLines.size();
        } finally {
            logLock.unlock();
        }
    }

    /**
     * Gets the most recently added log entry.
     *
     * @return The latest LogLine or null if none exists
     */
    public model.log.LogLine getLastAddedLogLine() {
        logLock.lock();
        try {
            return logLines.isEmpty() ? null : logLines.get(logLines.size() - 1);
        } finally {
            logLock.unlock();
        }
    }

    /**
     * Gets all log entries.
     *
     * @return Unmodifiable list of all LogLine objects
     */
    public List<model.log.LogLine> getAllLogLines() {
        logLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(logLines));
        } finally {
            logLock.unlock();
        }
    }

    /**
     * Adds a DEBUG level log entry.
     *
     * @param text The log text
     */
    public void debug(String text) {
        addToLog(text, Log.LogLevel.DEBUG);
    }

    /**
     * Adds an INFO level log entry.
     *
     * @param text The log text
     */
    public void info(String text) {
        addToLog(text, Log.LogLevel.INFO);
    }

    /**
     * Adds a WARNING level log entry.
     *
     * @param text The log text
     */
    public void warning(String text) {
        addToLog(text, Log.LogLevel.WARNING);
    }

    /**
     * Adds an ERROR level log entry.
     *
     * @param text The log text
     */
    public void error(String text) {
        addToLog(text, Log.LogLevel.ERROR);
    }

    /**
     * Adds a CRITICAL level log entry.
     *
     * @param text The log text
     */
    public void critical(String text) {
        addToLog(text, Log.LogLevel.CRITICAL);
    }

    /**
     * Adds a log entry with default INFO level (for backward compatibility).
     *
     * @param text The log text
     */
    public void addToLog(String text) {
        addToLog(text, Log.LogLevel.INFO);
    }

    /**
     * Adds a log entry with specified level.
     *
     * @param text     The log text
     * @param logLevel The log level
     */
    public void addToLog(String text, Log.LogLevel logLevel) {
        // Create LogLine object
        model.log.LogLine logLine = new model.log.LogLine(text, logLevel);

        logLock.lock();
        try {
            // Add to memory if level is high enough
            if (logLevel.ordinal() >= minMemoryLogLevel.ordinal()) {
                logLines.add(logLine);

                // Notify observers
                setChanged();
                notifyObservers(new Log.LogEvent(EVENT_LOG_ADDED, logLine));
            }
        } finally {
            logLock.unlock();
        }

        // Write to file if level is high enough
        if (logLevel.ordinal() >= minFileLogLevel.ordinal()) {
            addToFile(logLine);
        }
    }

    /**
     * Writes a log entry to the file.
     *
     * @param logLine The LogLine to write
     */
    private void addToFile(model.log.LogLine logLine) {
        try {
            Files.write(
                    Paths.get(logFilePath),
                    (logLine.toString() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error writing to log file: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the minimum log level for file logging.
     *
     * @param level The new minimum level
     */
    public void setMinFileLogLevel(Log.LogLevel level) {
        this.minFileLogLevel = level;
    }

    /**
     * Sets the minimum log level for memory logging.
     *
     * @param level The new minimum level
     */
    public void setMinMemoryLogLevel(Log.LogLevel level) {
        this.minMemoryLogLevel = level;
    }

    /**
     * Clears all log entries from memory.
     */
    public void clearMemoryLog() {
        logLock.lock();
        try {
            int oldSize = logLines.size();
            logLines.clear();

            // Notify observers
            setChanged();
            notifyObservers(new Log.LogClearEvent(EVENT_LOG_CLEARED, oldSize));
        } finally {
            logLock.unlock();
        }
    }

    /**
     * Reads the entire log file and returns its content.
     *
     * @return The log file's content
     */
    public String readFullLog() {
        try {
            return new String(Files.readAllBytes(Paths.get(logFilePath)));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading log file: " + e.getMessage(), e);
            return "Could not read log file: " + e.getMessage();
        }
    }

    /**
     * Event class for log operations.
     */
    public static class LogEvent {
        private final String eventType;
        private final model.log.LogLine logLine;

        public LogEvent(String eventType, model.log.LogLine logLine) {
            this.eventType = eventType;
            this.logLine = logLine;
        }

        public String getEventType() {
            return eventType;
        }

        public model.log.LogLine getLogLine() {
            return logLine;
        }
    }

    /**
     * Event class for log clear operations.
     */
    public static class LogClearEvent {
        private final String eventType;
        private final int oldSize;

        public LogClearEvent(String eventType, int oldSize) {
            this.eventType = eventType;
            this.oldSize = oldSize;
        }

        public String getEventType() {
            return eventType;
        }

        public int getOldSize() {
            return oldSize;
        }
    }
}
