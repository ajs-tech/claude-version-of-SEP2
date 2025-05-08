package model.logic.laptopLogic;

import model.database.LaptopDAO;
import model.enums.PerformanceTypeEnum;
import model.log.Log;
import model.models.Laptop;
import model.util.ModelObservable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class that handles laptop-related business logic.
 * Implements Observer pattern using Java's built-in Observable/Observer.
 */
public class LaptopData extends Observable implements LaptopDataInterface, Observer {
    private static final Logger logger = Logger.getLogger(LaptopData.class.getName());
    private static final Log log = Log.getInstance();

    // Event types for observer notifications
    public static final String EVENT_LAPTOP_ADDED = "LAPTOP_ADDED";
    public static final String EVENT_LAPTOP_UPDATED = "LAPTOP_UPDATED";
    public static final String EVENT_LAPTOP_REMOVED = "LAPTOP_REMOVED";
    public static final String EVENT_LAPTOPS_REFRESHED = "LAPTOPS_REFRESHED";
    public static final String EVENT_ERROR = "ERROR";

    private final List<Laptop> laptopCache;
    private final LaptopDAO laptopDAO;
    private final ReadWriteLock cacheLock;

    // Singleton instance
    private static volatile LaptopData instance;

    /**
     * Private constructor for Singleton pattern.
     * Initializes components and cache.
     */
    private LaptopData() {
        this.laptopCache = new ArrayList<>();
        this.laptopDAO = LaptopDAO.getInstance();
        this.cacheLock = new ReentrantReadWriteLock();

        // Register as observer of LaptopDAO
        laptopDAO.addObserver(this);

        // Load initial data
        refreshCache();
    }

    /**
     * Gets the singleton instance with double-checked locking.
     *
     * @return The singleton instance
     */
    public static LaptopData getInstance() {
        if (instance == null) {
            synchronized (LaptopData.class) {
                if (instance == null) {
                    instance = new LaptopData();
                }
            }
        }
        return instance;
    }

    /**
     * Reloads laptop cache from the database.
     */
    private void refreshCache() {
        try {
            List<Laptop> laptops = laptopDAO.getAll();

            cacheLock.writeLock().lock();
            try {
                laptopCache.clear();
                laptopCache.addAll(laptops);

                // Register as observer to all laptops
                for (Laptop laptop : laptops) {
                    laptop.addObserver(this);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }

            setChanged();
            notifyObservers(new DataEvent(EVENT_LAPTOPS_REFRESHED, laptopCache.size()));

            log.info("Laptop cache refreshed: " + laptops.size() + " laptops loaded");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading laptops from database: " + e.getMessage(), e);
            log.error("Error loading laptops from database: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error refreshing laptop cache", e));
        }
    }

    /**
     * Handles property change events from models and DAOs.
     */
    @Override
    public void update(Observable o, Object arg) {
        // Handle events from LaptopDAO
        if (o instanceof LaptopDAO) {
            if (arg instanceof LaptopDAO.DatabaseEvent) {
                LaptopDAO.DatabaseEvent event = (LaptopDAO.DatabaseEvent) arg;

                switch (event.getEventType()) {
                    case LaptopDAO.EVENT_LAPTOP_CREATED:
                        handleLaptopCreated((Laptop) event.getData());
                        break;
                    case LaptopDAO.EVENT_LAPTOP_UPDATED:
                        handleLaptopUpdated((Laptop) event.getData());
                        break;
                    case LaptopDAO.EVENT_LAPTOP_DELETED:
                        handleLaptopDeleted((Laptop) event.getData());
                        break;
                    case LaptopDAO.EVENT_LAPTOP_ERROR:
                        // Forward error event
                        setChanged();
                        notifyObservers(new ErrorEvent(EVENT_ERROR,
                                event.getData().toString(), event.getException()));
                        break;
                }
            }
        }
        // Handle events from Laptop objects
        else if (o instanceof Laptop) {
            if (arg instanceof ModelObservable.PropertyChangeInfo) {
                ModelObservable.PropertyChangeInfo info = (ModelObservable.PropertyChangeInfo) arg;

                // Forward property change events
                setChanged();
                notifyObservers(new PropertyChangeEvent(
                        info.getPropertyName(), info.getOldValue(), info.getNewValue(), (Laptop) o));

                // Update statistics for state changes
                if ("state".equals(info.getPropertyName()) ||
                        "stateClassName".equals(info.getPropertyName()) ||
                        "available".equals(info.getPropertyName())) {

                    setChanged();
                    notifyObservers(new CountEvent("availableLaptopCount", getAmountOfAvailableLaptops()));

                    setChanged();
                    notifyObservers(new CountEvent("loanedLaptopCount", getAmountOfLoanedLaptops()));
                }
            }
        }
    }

    /**
     * Handles laptop creation events.
     */
    private void handleLaptopCreated(Laptop laptop) {
        boolean added = false;

        cacheLock.writeLock().lock();
        try {
            // Check if laptop is already in cache
            boolean exists = false;
            for (Laptop l : laptopCache) {
                if (l.getId().equals(laptop.getId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                laptopCache.add(laptop);
                laptop.addObserver(this);
                added = true;
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        if (added) {
            setChanged();
            notifyObservers(new DataEvent(EVENT_LAPTOP_ADDED, laptop));
        }
    }

    /**
     * Handles laptop update events.
     */
    private void handleLaptopUpdated(Laptop laptop) {
        boolean updated = false;
        Laptop oldLaptop = null;

        cacheLock.writeLock().lock();
        try {
            for (int i = 0; i < laptopCache.size(); i++) {
                if (laptopCache.get(i).getId().equals(laptop.getId())) {
                    oldLaptop = laptopCache.get(i);
                    laptopCache.set(i, laptop);
                    laptop.addObserver(this);
                    updated = true;
                    break;
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        if (updated) {
            setChanged();
            notifyObservers(new UpdateEvent(EVENT_LAPTOP_UPDATED, oldLaptop, laptop));
        }
    }

    /**
     * Handles laptop deletion events.
     */
    private void handleLaptopDeleted(Laptop laptop) {
        boolean removed = false;

        cacheLock.writeLock().lock();
        try {
            Iterator<Laptop> iterator = laptopCache.iterator();
            while (iterator.hasNext()) {
                Laptop l = iterator.next();
                if (l.getId().equals(laptop.getId())) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        if (removed) {
            setChanged();
            notifyObservers(new DataEvent(EVENT_LAPTOP_REMOVED, laptop));
        }
    }

    // LaptopDataInterface implementation

    @Override
    public ArrayList<Laptop> getAllLaptops() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(laptopCache);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public int getAmountOfAvailableLaptops() {
        int count = 0;

        cacheLock.readLock().lock();
        try {
            for (Laptop laptop : laptopCache) {
                if (laptop.isAvailable()) {
                    count++;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return count;
    }

    @Override
    public int getAmountOfLoanedLaptops() {
        int count = 0;

        cacheLock.readLock().lock();
        try {
            for (Laptop laptop : laptopCache) {
                if (laptop.isLoaned()) {
                    count++;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return count;
    }

    @Override
    public int getAmountOfLaptopsByState(String classSimpleName) {
        int count = 0;

        cacheLock.readLock().lock();
        try {
            for (Laptop laptop : laptopCache) {
                if (laptop.getStateClassName().equals(classSimpleName)) {
                    count++;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        return count;
    }

    @Override
    public Laptop findAvailableLaptop(PerformanceTypeEnum performanceTypeEnum) {
        // First check cache
        cacheLock.readLock().lock();
        try {
            for (Laptop laptop : laptopCache) {
                if (laptop.isAvailable() && laptop.getPerformanceType() == performanceTypeEnum) {
                    return laptop;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // If not found in cache, try database
        try {
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(performanceTypeEnum);
            if (!availableLaptops.isEmpty()) {
                Laptop laptop = availableLaptops.get(0);

                // Add to cache
                handleLaptopCreated(laptop);

                return laptop;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error searching for available laptop in database: " + e.getMessage(), e);
            log.warning("Error searching for available laptop in database: " + e.getMessage());
        }

        return null;
    }

    @Override
    public Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        try {
            // Validate input
            validateLaptopData(brand, model, gigabyte, ram, performanceType);

            // Create laptop object
            Laptop laptop = new Laptop(brand, model, gigabyte, ram, performanceType);

            // Save to database
            boolean success = laptopDAO.insert(laptop);

            if (success) {
                // Laptop is already added to cache via event handling
                log.info("Laptop created: " + brand + " " + model);
                return laptop;
            } else {
                log.error("Could not create laptop in database");

                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Failed to create laptop in database", null));

                return null;
            }
        } catch (IllegalArgumentException e) {
            log.warning("Invalid laptop data: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Invalid laptop data: " + e.getMessage(), e));

            return null;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating laptop: " + e.getMessage(), e);
            log.error("Error creating laptop: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error creating laptop", e));

            return null;
        }
    }

    /**
     * Validates laptop data.
     */
    private void validateLaptopData(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be empty");
        }

        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be empty");
        }

        if (gigabyte <= 0 || gigabyte > 4000) {
            throw new IllegalArgumentException("Gigabyte must be between 1 and 4000");
        }

        if (ram <= 0 || ram > 128) {
            throw new IllegalArgumentException("RAM must be between 1 and 128");
        }

        if (performanceType == null) {
            throw new IllegalArgumentException("Performance type cannot be null");
        }
    }

    @Override
    public Laptop getLaptopById(UUID id) {
        // Search in cache first
        cacheLock.readLock().lock();
        try {
            for (Laptop laptop : laptopCache) {
                if (laptop.getId().equals(id)) {
                    return laptop;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // If not found in cache, try database
        try {
            Laptop laptop = laptopDAO.getById(id);
            if (laptop != null) {
                // Add to cache
                handleLaptopCreated(laptop);
            }
            return laptop;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error retrieving laptop with ID " + id + ": " + e.getMessage(), e);
            log.warning("Error retrieving laptop with ID " + id + ": " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error retrieving laptop", e));

            return null;
        }
    }

    @Override
    public boolean updateLaptop(Laptop laptop) {
        try {
            boolean success = laptopDAO.update(laptop);

            if (success) {
                // Laptop is already updated in cache via event handling
                log.info("Laptop updated: " + laptop.getBrand() + " " + laptop.getModel());
            } else {
                log.error("Could not update laptop in database: " + laptop.getId());

                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Failed to update laptop in database", null));
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating laptop: " + e.getMessage(), e);
            log.error("Error updating laptop: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error updating laptop", e));

            return false;
        }
    }

    @Override
    public boolean deleteLaptop(UUID id) {
        try {
            // Get laptop first to send event after deletion
            Laptop laptop = getLaptopById(id);
            if (laptop == null) {
                return false;
            }

            boolean success = laptopDAO.delete(id);

            if (success) {
                // Laptop is already removed from cache via event handling
                log.info("Laptop deleted: " + id);
            } else {
                log.error("Could not delete laptop from database: " + id);

                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Failed to delete laptop from database", null));
            }

            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting laptop: " + e.getMessage(), e);
            log.error("Error deleting laptop: " + e.getMessage());

            setChanged();
            notifyObservers(new ErrorEvent(EVENT_ERROR, "Error deleting laptop", e));

            return false;
        }
    }

    /**
     * Closes resources and removes observers.
     */
    public void close() {
        laptopDAO.deleteObserver(this);

        cacheLock.writeLock().lock();
        try {
            for (Laptop laptop : laptopCache) {
                laptop.deleteObserver(this);
            }
            laptopCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    // Event classes

    /**
     * Event class for data operations.
     */
    public static class DataEvent {
        private final String eventType;
        private final Object data;

        public DataEvent(String eventType, Object data) {
            this.eventType = eventType;
            this.data = data;
        }

        public String getEventType() {
            return eventType;
        }

        public Object getData() {
            return data;
        }
    }

    /**
     * Event class for update operations.
     */
    public static class UpdateEvent extends DataEvent {
        private final Object oldValue;
        private final Object newValue;

        public UpdateEvent(String eventType, Object oldValue, Object newValue) {
            super(eventType, newValue);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }
    }

    /**
     * Event class for property changes.
     */
    public static class PropertyChangeEvent extends DataEvent {
        private final String propertyName;
        private final Object oldValue;
        private final Object newValue;
        private final Object source;

        public PropertyChangeEvent(String propertyName, Object oldValue, Object newValue, Object source) {
            super("propertyChange", newValue);
            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.source = source;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public Object getSource() {
            return source;
        }
    }

    /**
     * Event class for count updates.
     */
    public static class CountEvent {
        private final String countType;
        private final int count;

        public CountEvent(String countType, int count) {
            this.countType = countType;
            this.count = count;
        }

        public String getCountType() {
            return countType;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Event class for errors.
     */
    public static class ErrorEvent {
        private final String eventType;
        private final String message;
        private final Exception exception;

        public ErrorEvent(String eventType, String message, Exception exception) {
            this.eventType = eventType;
            this.message = message;
            this.exception = exception;
        }

        public String getEventType() {
            return eventType;
        }

        public String getMessage() {
            return message;
        }

        public Exception getException() {
            return exception;
        }
    }
}