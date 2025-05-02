package model.logic.laptopLogic;

import model.database.LaptopDAO;
import model.enums.PerformanceTypeEnum;
import model.events.SystemEvents;
import model.log.Log;
import model.models.Laptop;
import model.util.EventBus;
import model.util.PropertyChangeNotifier;
import model.util.PropertyChangeSupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service-klasse der håndterer laptop-relateret forretningslogik.
 * Opdateret til MVVM-arkitektur med databinding support og events.
 */
public class LaptopData implements LaptopDataInterface, PropertyChangeNotifier {
    private static final Logger logger = Logger.getLogger(LaptopData.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    private final List<Laptop> laptopCache;
    private final LaptopDAO laptopDAO;
    private final PropertyChangeSupport changeSupport;

    /**
     * Konstruktør der initialiserer komponenter og cache.
     */
    public LaptopData() {
        this.laptopCache = new ArrayList<>();
        this.laptopDAO = new LaptopDAO();
        this.changeSupport = new PropertyChangeSupport(this);

        // Forsøg at indlæse cache fra database
        refreshCache();

        // Registrer som event subscriber
        eventBus.subscribe(SystemEvents.LaptopCreatedEvent.class,
                event -> handleLaptopCreated(event.getLaptop()));

        eventBus.subscribe(SystemEvents.LaptopUpdatedEvent.class,
                event -> handleLaptopUpdated(event.getLaptop()));

        eventBus.subscribe(SystemEvents.LaptopDeletedEvent.class,
                event -> handleLaptopDeleted(event.getLaptop()));

        eventBus.subscribe(SystemEvents.LaptopStateChangedEvent.class,
                event -> handleLaptopStateChanged(event.getLaptop(),
                        event.getOldState(),
                        event.getNewState(),
                        event.isNowAvailable()));
    }

    /**
     * Genindlæser laptop-cache fra databasen.
     */
    private void refreshCache() {
        try {
            laptopCache.clear();
            laptopCache.addAll(laptopDAO.getAll());

            // Registrer som listener til alle laptops
            for (Laptop laptop : laptopCache) {
                laptop.addPropertyChangeListener(this::handleLaptopPropertyChange);
            }

            firePropertyChange("laptopsRefreshed", null, laptopCache.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved indlæsning af laptops fra database: " + e.getMessage(), e);
            log.error("Fejl ved indlæsning af laptops fra database: " + e.getMessage());
        }
    }

    /**
     * Håndterer propertyChange events fra Laptop objekter.
     */
    private void handleLaptopPropertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Laptop) {
            Laptop laptop = (Laptop) evt.getSource();

            // Propagér relevante events
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            // Ved state-ændringer, opdateres statistikker
            if ("state".equals(evt.getPropertyName()) || "stateClassName".equals(evt.getPropertyName())) {
                firePropertyChange("availableLaptopCount", null, getAmountOfAvailableLaptops());
                firePropertyChange("loanedLaptopCount", null, getAmountOfLoanedLaptops());
            }
        }
    }

    // Event handlers

    private void handleLaptopCreated(Laptop laptop) {
        if (!laptopCache.contains(laptop)) {
            laptopCache.add(laptop);
            laptop.addPropertyChangeListener(this::handleLaptopPropertyChange);

            firePropertyChange("laptopAdded", null, laptop);
            firePropertyChange("laptopCount", laptopCache.size() - 1, laptopCache.size());
        }
    }

    private void handleLaptopUpdated(Laptop laptop) {
        // Find den eksisterende laptop i cachen
        for (int i = 0; i < laptopCache.size(); i++) {
            if (laptopCache.get(i).getId().equals(laptop.getId())) {
                Laptop oldLaptop = laptopCache.get(i);
                laptopCache.set(i, laptop);

                // Overfør listeners til den nye laptop
                for (PropertyChangeListener listener : getListenersFromLaptop(oldLaptop)) {
                    laptop.addPropertyChangeListener(listener);
                }

                firePropertyChange("laptopUpdated", oldLaptop, laptop);
                break;
            }
        }
    }

    private PropertyChangeListener[] getListenersFromLaptop(Laptop laptop) {
        // Dette er en dummy-implementering, da der ikke er direkte adgang
        // til listeners i en Laptop. I en rigtig implementering skulle
        // PropertyChangeListener[] hentes fra laptop objektet.
        return new PropertyChangeListener[0];
    }

    private void handleLaptopDeleted(Laptop laptop) {
        for (int i = 0; i < laptopCache.size(); i++) {
            if (laptopCache.get(i).getId().equals(laptop.getId())) {
                Laptop removedLaptop = laptopCache.remove(i);

                firePropertyChange("laptopRemoved", removedLaptop, null);
                firePropertyChange("laptopCount", laptopCache.size() + 1, laptopCache.size());
                break;
            }
        }
    }

    private void handleLaptopStateChanged(Laptop laptop, String oldState, String newState, boolean isNowAvailable) {
        // Opdater statistikker
        firePropertyChange("availableLaptopCount", null, getAmountOfAvailableLaptops());
        firePropertyChange("loanedLaptopCount", null, getAmountOfLoanedLaptops());
    }

    // LaptopDataInterface implementation

    @Override
    public ArrayList<Laptop> getAllLaptops() {
        return new ArrayList<>(laptopCache);
    }

    @Override
    public int getAmountOfAvailableLaptops() {
        int availableLaptops = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.isAvailable()) {
                availableLaptops++;
            }
        }
        return availableLaptops;
    }

    @Override
    public int getAmountOfLoanedLaptops() {
        int loanedLaptops = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.isLoaned()) {
                loanedLaptops++;
            }
        }
        return loanedLaptops;
    }

    @Override
    public int getAmountOfLaptopsByState(String classSimpleName) {
        int numberOfLaptops = 0;
        for (Laptop laptop : laptopCache) {
            if (laptop.getStateClassName().equals(classSimpleName)) {
                numberOfLaptops++;
            }
        }
        return numberOfLaptops;
    }

    @Override
    public Laptop findAvailableLaptop(PerformanceTypeEnum performanceTypeEnum) {
        for (Laptop laptop : laptopCache) {
            if (laptop.isAvailable() && laptop.getPerformanceType().equals(performanceTypeEnum)) {
                return laptop;
            }
        }

        // Hvis ikke fundet i cache, forsøg at hente fra database
        try {
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(performanceTypeEnum);
            if (!availableLaptops.isEmpty()) {
                Laptop laptop = availableLaptops.get(0);

                // Opdater cache
                handleLaptopCreated(laptop);

                return laptop;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved søgning efter tilgængelig laptop i database: " + e.getMessage(), e);
            log.warning("Fejl ved søgning efter tilgængelig laptop i database: " + e.getMessage());
        }

        return null;
    }

    @Override
    public Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        try {
            // Opret laptop-objekt
            Laptop laptop = new Laptop(brand, model, gigabyte, ram, performanceType);

            // Gem i database
            boolean success = laptopDAO.insert(laptop);

            if (success) {
                // Laptop-objektet er allerede tilføjet til cachen via event
                log.info("Laptop oprettet: " + brand + " " + model);
                return laptop;
            } else {
                log.error("Kunne ikke oprette laptop i database");
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved oprettelse af laptop: " + e.getMessage(), e);
            log.error("Fejl ved oprettelse af laptop: " + e.getMessage());
            return null;
        }
    }

    /**
     * Finder en laptop baseret på ID
     *
     * @param id Laptop UUID
     * @return Laptop hvis fundet, ellers null
     */
    public Laptop getLaptopById(UUID id) {
        // Søg først i cache
        for (Laptop laptop : laptopCache) {
            if (laptop.getId().equals(id)) {
                return laptop;
            }
        }

        // Hvis ikke fundet, søg i database
        try {
            Laptop laptop = laptopDAO.getById(id);
            if (laptop != null) {
                // Tilføj til cache
                handleLaptopCreated(laptop);
            }
            return laptop;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fejl ved hentning af laptop med ID " + id + ": " + e.getMessage(), e);
            log.warning("Fejl ved hentning af laptop med ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Opdaterer en eksisterende laptop
     *
     * @param laptop Laptop at opdatere
     * @return true hvis operationen lykkedes
     */
    public boolean updateLaptop(Laptop laptop) {
        try {
            boolean success = laptopDAO.update(laptop);

            if (success) {
                // Laptop-objektet er allerede opdateret i cachen via event
                log.info("Laptop opdateret: " + laptop.getBrand() + " " + laptop.getModel());
            } else {
                log.error("Kunne ikke opdatere laptop i database: " + laptop.getId());
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved opdatering af laptop: " + e.getMessage(), e);
            log.error("Fejl ved opdatering af laptop: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sletter en laptop
     *
     * @param id Laptop UUID
     * @return true hvis operationen lykkedes
     */
    public boolean deleteLaptop(UUID id) {
        try {
            // Find først laptop for at kunne sende event
            Laptop laptop = getLaptopById(id);
            if (laptop == null) {
                return false;
            }

            boolean success = laptopDAO.delete(id);

            if (success) {
                // Laptop er allerede fjernet fra cache via event
                log.info("Laptop slettet: " + id);
            } else {
                log.error("Kunne ikke slette laptop fra database: " + id);
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Fejl ved sletning af laptop: " + e.getMessage(), e);
            log.error("Fejl ved sletning af laptop: " + e.getMessage());
            return false;
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