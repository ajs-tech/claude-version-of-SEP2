package model.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single log entry with text, timestamp, and level.
 */
class LogLine {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
    
    private final String text;
    private final LocalDateTime timestamp;
    private final Log.LogLevel logLevel;
    
    /**
     * Creates a new LogLine with specified level.
     *
     * @param text     The log text
     * @param logLevel The log level
     */
    public LogLine(String text, Log.LogLevel logLevel) {
        this.text = text;
        this.timestamp = LocalDateTime.now();
        this.logLevel = logLevel;
    }
    
    /**
     * Gets the log text.
     *
     * @return The log text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets the time.
     *
     * @return The time as a string (HH:MM:SS)
     */
    public String getTime() {
        return timestamp.format(TIME_FORMATTER);
    }
    
    /**
     * Gets the date and time.
     *
     * @return The date and time as a string (HH:MM:SS DD-MM-YYYY)
     */
    public String getDateTime() {
        return timestamp.format(DATE_TIME_FORMATTER);
    }
    
    /**
     * Gets the timestamp.
     *
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the log level.
     *
     * @return The log level
     */
    public Log.LogLevel getLogLevel() {
        return logLevel;
    }
    
    /**
     * Returns a formatted string representation of the log entry.
     * Format: [LEVEL] Text >> HH:MM:SS DD-MM-YYYY
     *
     * @return Formatted log string
     */
    @Override
    public String toString() {
        return String.format("[%s] %s >> %s",
                logLevel.name(),
                text,
                timestamp.format(DATE_TIME_FORMATTER));
    }
}