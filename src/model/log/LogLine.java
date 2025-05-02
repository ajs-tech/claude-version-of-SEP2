package model.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Repræsenterer en enkelt loghændelse med tekst, tidspunkt og niveau.
 */
public class LogLine {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    private final String text;
    private final LocalDateTime timestamp;
    private final Log.LogLevel logLevel;

    /**
     * Opretter en ny LogLine med specificeret niveau.
     *
     * @param text     Logteksten
     * @param logLevel Log-niveauet
     */
    public LogLine(String text, Log.LogLevel logLevel) {
        this.text = text;
        this.timestamp = LocalDateTime.now();
        this.logLevel = logLevel;
    }

    /**
     * Opretter en ny LogLine med standard INFO niveau.
     *
     * @param text Logteksten
     */
    public LogLine(String text) {
        this(text, Log.LogLevel.INFO);
    }

    /**
     * Returnerer logteksten.
     *
     * @return Logteksten
     */
    public String getText() {
        return text;
    }

    /**
     * Returnerer tidspunktet.
     *
     * @return Tidspunktet som streng (TT:MM:SS)
     */
    public String getTime() {
        return timestamp.format(TIME_FORMATTER);
    }

    /**
     * Returnerer tidspunktet og datoen.
     *
     * @return Tidspunktet og datoen som streng (TT:MM:SS DD-MM-ÅÅÅÅ)
     */
    public String getDateTime() {
        return timestamp.format(DATE_TIME_FORMATTER);
    }

    /**
     * Returnerer tidsstemplet.
     *
     * @return Tidsstemplet
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returnerer log-niveauet.
     *
     * @return Log-niveauet
     */
    public Log.LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Returnerer en formateret streng-repræsentation af loghændelsen.
     * Format: [NIVEAU] Tekst >> TT:MM:SS DD-MM-ÅÅÅÅ
     *
     * @return Formateret log-streng
     */
    @Override
    public String toString() {
        return String.format("[%s] %s >> %s",
                logLevel.name(),
                text,
                timestamp.format(DATE_TIME_FORMATTER));
    }
}