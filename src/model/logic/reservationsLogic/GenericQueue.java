package model.logic.reservationsLogic;

import model.enums.PerformanceTypeEnum;
import model.log.Log;
import model.models.Student;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Generic implementering af kø-system for studerende.
 * Erstatter de separate queue-klasser med én implementering der kan håndtere begge køer.
 * Implementerer ren Java uden JavaFX-afhængigheder.
 */
public class GenericQueue implements PropertyChangeNotifier {
    private final Queue<Student> queue;
    private final PerformanceTypeEnum performanceType;
    private final Log log;
    private final PropertyChangeSupport changeSupport;

    /**
     * Opretter en ny kø for en specificeret performance type.
     *
     * @param performanceType Typen af kø (HIGH/LOW)
     */
    public GenericQueue(PerformanceTypeEnum performanceType) {
        this.queue = new ArrayDeque<>();
        this.performanceType = performanceType;
        this.log = Log.getInstance();
        this.changeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Returnerer størrelsen af køen.
     *
     * @return Antal studerende i køen
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Henter den næste student i køen uden at fjerne dem.
     *
     * @return Den næste student i køen, eller null hvis køen er tom
     */
    public Student peekNextInLine() {
        return queue.peek();
    }

    /**
     * Henter og fjerner den næste student i køen.
     *
     * @return Den næste student i køen, eller null hvis køen er tom
     */
    public Student getAndRemoveNextInLine() {
        Student student = queue.poll();

        if (student != null) {
            log.addToLog("Student [" + student.getName() + ", VIA ID: " + student.getViaId() +
                    "] fjernet fra " + getQueueTypeName() + "-ydelses kø");

            // Notificér om at køen er blevet ændret
            firePropertyChange("queueSize", getQueueSize() + 1, getQueueSize());
            firePropertyChange("studentRemoved", null, student);
        }

        return student;
    }

    /**
     * Tilføjer en student til køen.
     *
     * @param student Studenten der skal tilføjes
     */
    public void addToQueue(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }

        int oldSize = getQueueSize();
        queue.offer(student);

        log.addToLog("Student [" + student.getName() + ", VIA ID: " + student.getViaId() +
                "] tilføjet til " + getQueueTypeName() + "-ydelses kø");

        // Notificér om at køen er blevet ændret
        firePropertyChange("queueSize", oldSize, getQueueSize());
        firePropertyChange("studentAdded", null, student);
    }

    /**
     * Returnerer en liste af alle studerende i køen uden at fjerne dem.
     *
     * @return Liste af studerende i køen
     */
    public List<Student> getAllStudentsInQueue() {
        return new ArrayList<>(queue);
    }

    /**
     * Fjerner en specifik student fra køen baseret på VIA ID.
     *
     * @param viaId VIA ID for den student der skal fjernes
     * @return true hvis studenten blev fjernet, ellers false
     */
    public boolean removeStudentById(int viaId) {
        Student studentToRemove = null;

        // Find studenten med det givne ID
        for (Student student : queue) {
            if (student.getViaId() == viaId) {
                studentToRemove = student;
                break;
            }
        }

        // Fjern studenten hvis den blev fundet
        if (studentToRemove != null) {
            int oldSize = getQueueSize();
            boolean removed = queue.remove(studentToRemove);

            if (removed) {
                log.addToLog("Student [" + studentToRemove.getName() + ", VIA ID: " +
                        studentToRemove.getViaId() + "] fjernet fra " + getQueueTypeName() + "-ydelses kø");

                // Notificér om at køen er blevet ændret
                firePropertyChange("queueSize", oldSize, getQueueSize());
                firePropertyChange("studentRemoved", studentToRemove, null);
            }

            return removed;
        }

        return false;
    }

    /**
     * Tjekker om køen indeholder en student med et specifikt VIA ID.
     *
     * @param viaId VIA ID der skal tjekkes for
     * @return true hvis studenten er i køen, ellers false
     */
    public boolean containsStudent(int viaId) {
        for (Student student : queue) {
            if (student.getViaId() == viaId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returnerer typen af kø (HIGH/LOW).
     *
     * @return Køens performancetype
     */
    public PerformanceTypeEnum getPerformanceType() {
        return performanceType;
    }

    /**
     * Returnerer en læservenlig streng for køens type.
     *
     * @return "høj" for HIGH performancetype, "lav" for LOW
     */
    private String getQueueTypeName() {
        return performanceType == PerformanceTypeEnum.HIGH ? "høj" : "lav";
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