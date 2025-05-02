package model.log;

import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Forbedret logningsklasse med forskellige log-niveauer og notifikationssupport.
 * Implementerer Singleton pattern for at sikre én instans på tværs af systemet.
 */
public class Log implements PropertyChangeNotifier {
    private static final String DEFAULT_LOG_PATH = "src/model/log/loglines.txt";

    // Log-niveauer
    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, CRITICAL
    }

    private static Log instance;
    private static final Object lock = new Object();

    private final List<LogLine> logLines;
    private final String logFilePath;
    private LogLevel minFileLogLevel;  // Minimum niveau til fil-logning
    private LogLevel minMemoryLogLevel; // Minimum niveau til memory-logning
    private final PropertyChangeSupport changeSupport;

    /**
     * Privat konstruktør, sikrer Singleton pattern
     */
    private Log() {
        this(DEFAULT_LOG_PATH);
    }

    /**
     * Privat konstruktør med specifik log-fil
     *
     * @param logFilePath Sti til log-filen
     */
    private Log(String logFilePath) {
        this.logLines = new ArrayList<>();
        this.logFilePath = logFilePath;
        this.minFileLogLevel = LogLevel.INFO;     // Default: INFO og højere til fil
        this.minMemoryLogLevel = LogLevel.DEBUG;  // Default: ALT til memory
        this.changeSupport = new PropertyChangeSupport(this);

        createFile();
    }

    /**
     * Returnerer den eksisterende instans eller opretter en ny
     *
     * @return Log instansen
     */
    public static Log getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Log();
                }
            }
        }
        return instance;
    }

    /**
     * Getter for loglineSize
     *
     * @return Antal loghændelser i hukommelsen
     */
    public int getLogLineSize() {
        return logLines.size();
    }

    /**
     * Returnerer den senest tilføjede loghændelse
     *
     * @return Seneste LogLine eller null hvis ingen findes
     */
    public LogLine getLastAddedLogLine() {
        return logLines.isEmpty() ? null : logLines.get(logLines.size() - 1);
    }

    /**
     * Returnerer en umodificerbar liste af alle loghændelser
     *
     * @return Liste af LogLine objekter
     */
    public List<LogLine> getAllLogLines() {
        return Collections.unmodifiableList(logLines);
    }

    /**
     * Sikrer at log-filen eksisterer
     */
    private void createFile() {
        try {
            File file = new File(logFilePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            if (file.createNewFile()) {
                System.out.println("Log-fil oprettet på sti: " + file.getAbsolutePath());
            } else {
                System.out.println("Log-fil eksisterer allerede på sti: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Fejl ved oprettelse af log-fil: " + e.getMessage());
        }
    }

    /**
     * Tilføjer en DEBUG-niveau loghændelse
     *
     * @param text Logteksten
     */
    public void debug(String text) {
        addToLog(text, LogLevel.DEBUG);
    }

    /**
     * Tilføjer en INFO-niveau loghændelse
     *
     * @param text Logteksten
     */
    public void info(String text) {
        addToLog(text, LogLevel.INFO);
    }

    /**
     * Tilføjer en WARNING-niveau loghændelse
     *
     * @param text Logteksten
     */
    public void warning(String text) {
        addToLog(text, LogLevel.WARNING);
    }

    /**
     * Tilføjer en ERROR-niveau loghændelse
     *
     * @param text Logteksten
     */
    public void error(String text) {
        addToLog(text, LogLevel.ERROR);
    }

    /**
     * Tilføjer en CRITICAL-niveau loghændelse
     *
     * @param text Logteksten
     */
    public void critical(String text) {
        addToLog(text, LogLevel.CRITICAL);
    }

    /**
     * Tilføjer en loghændelse med standard INFO niveau (for bagudkompatibilitet)
     *
     * @param text Logteksten
     */
    public void addToLog(String text) {
        addToLog(text, LogLevel.INFO);
    }

    /**
     * Tilføjer en loghændelse med specificeret niveau
     *
     * @param text     Logteksten
     * @param logLevel Log-niveauet
     */
    public void addToLog(String text, LogLevel logLevel) {
        // Opret LogLine objekt
        LogLine logLine = new LogLine(text, logLevel);

        // Tilføj til memory hvis niveauet er højt nok
        if (logLevel.ordinal() >= minMemoryLogLevel.ordinal()) {
            logLines.add(logLine);

            // Notificér lyttere
            firePropertyChange("logLineAdded", null, logLine);
        }

        // Skriv til fil hvis niveauet er højt nok
        if (logLevel.ordinal() >= minFileLogLevel.ordinal()) {
            addToFile(logLine);
        }
    }

    /**
     * Skriver en loghændelse til fil
     *
     * @param logLine LogLine objektet der skal skrives
     */
    private void addToFile(LogLine logLine) {
        try {
            Files.write(
                    Paths.get(logFilePath),
                    (logLine.toString() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Fejl ved skrivning til log-fil: " + e.getMessage());
        }
    }

    /**
     * Sætter minimum log-niveau for fil-logning
     *
     * @param level Det nye minimum niveau
     */
    public void setMinFileLogLevel(LogLevel level) {
        this.minFileLogLevel = level;
    }

    /**
     * Sætter minimum log-niveau for hukommelses-logning
     *
     * @param level Det nye minimum niveau
     */
    public void setMinMemoryLogLevel(LogLevel level) {
        this.minMemoryLogLevel = level;
    }

    /**
     * Rydder alle loghændelser fra hukommelsen
     */
    public void clearMemoryLog() {
        int oldSize = logLines.size();
        logLines.clear();
        firePropertyChange("logCleared", oldSize, 0);
    }

    /**
     * Læser hele log-filen og returnerer indholdet som en streng
     *
     * @return Log-filens indhold
     */
    public String readFullLog() {
        try {
            return new String(Files.readAllBytes(Paths.get(logFilePath)));
        } catch (IOException e) {
            System.err.println("Fejl ved læsning af log-fil: " + e.getMessage());
            return "Kunne ikke læse log-fil: " + e.getMessage();
        }
    }

    // PropertyChangeNotifier implementation

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}