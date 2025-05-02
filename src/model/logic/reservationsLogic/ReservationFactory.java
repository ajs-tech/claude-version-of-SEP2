package model.logic.reservationsLogic;

import model.log.Log;
import model.models.Laptop;
import model.models.LoanedState;
import model.models.Reservation;
import model.models.Student;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeListener;

/**
 * Factory-klasse til oprettelse af reservationer.
 * Implementerer Factory Pattern for at kapsle oprettelseslogik.
 */
public class ReservationFactory implements PropertyChangeNotifier {
    private final Log log;
    private final PropertyChangeSupport changeSupport;

    /**
     * Opretter en ny ReservationFactory instans.
     */
    public ReservationFactory() {
        this.log = Log.getInstance();
        this.changeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Opretter en ny reservation med korrekt setup af afhængige objekter.
     *
     * @param laptop  Laptopen der skal udlånes
     * @param student Studenten der skal låne laptopen
     * @return        Den oprettede reservation
     */
    public Reservation createReservation(Laptop laptop, Student student) {
        // Opdater laptop tilstand
        laptop.changeState(LoanedState.INSTANCE);

        // Opdater student status
        student.setHasLaptop(true);

        // Opret reservation
        Reservation reservation = new Reservation(student, laptop);

        // Log oprettelsen
        log.addToLog("Reservation oprettet med id:" + reservation.getReservationId() +
                " >> Laptop [" + laptop.getBrand() + " " + laptop.getModel() +
                "] tildelt til student [" + student.getName() + "]");

        // Notificér om oprettelsen
        firePropertyChange("reservationCreated", null, reservation);

        return reservation;
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