

import model.enums.PerformanceTypeEnum;
import model.models.Laptop;
import model.util.ModelObservable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ViewModel for laptop operations following MVVM architecture.
 * Implements Observer pattern and supports background threading.
 */
public class LaptopViewModel extends Observable implements Observer {
    private static final Logger logger = Logger.getLogger(LaptopViewModel.class.getName());
    
    // Event types for observer notifications
    public static final String EVENT_LAPTOPS_LOADED = "LAPTOPS_LOADED";
    public static final String EVENT_LAPTOP_CREATED = "LAPTOP_CREATED";
    public static final String EVENT_LAPTOP_UPDATED = "LAPTOP_UPDATED";
    public static final String EVENT_LAPTOP_DELETED = "LAPTOP_DELETED";
    public static final String EVENT_LAPTOP_STATE_CHANGED = "LAPTOP_STATE_CHANGED";
    public static final String EVENT_OPERATION_ERROR = "OPERATION_ERROR";
    
    private final LaptopDAO laptopDAO;
    private final List<Laptop> laptopsCache;
    private final ExecutorService executor;
    
    /**
     * Creates a new LaptopViewModel.
     */
    public LaptopViewModel() {
        this.laptopDAO = LaptopDAO.getInstance();
        this.laptopsCache = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Register as observer of LaptopDAO
        laptopDAO.addObserver(this);
        
        // Initial load
        loadLaptops();
    }
    
    /**
     * Loads all laptops from the database asynchronously.
     */
    public void loadLaptops() {
        executor.submit(() -> {
            try {
                List<Laptop> laptops = laptopDAO.getAll();
                
                synchronized (laptopsCache) {
                    laptopsCache.clear();
                    laptopsCache.addAll(laptops);
                    
                    // Register as observer for each laptop
                    for (Laptop laptop : laptops) {
                        laptop.addObserver(this);
                    }
                }
                
                // Notify observers
                setChanged();
                notifyObservers(new ViewModelEvent(EVENT_LAPTOPS_LOADED, laptops));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error loading laptops: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error loading laptops", e));
            }
        });
    }
    
    /**
     * Gets all laptops from the cache.
     *
     * @return List of all laptops
     */
    public List<Laptop> getAllLaptops() {
        synchronized (laptopsCache) {
            return new ArrayList<>(laptopsCache);
        }
    }
    
    /**
     * Gets the number of available laptops.
     *
     * @return Number of available laptops
     */
    public int getAvailableLaptopsCount() {
        int count = 0;
        synchronized (laptopsCache) {
            for (Laptop laptop : laptopsCache) {
                if (laptop.isAvailable()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Gets the number of loaned laptops.
     *
     * @return Number of loaned laptops
     */
    public int getLoanedLaptopsCount() {
        int count = 0;
        synchronized (laptopsCache) {
            for (Laptop laptop : laptopsCache) {
                if (laptop.isLoaned()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Gets a laptop by ID.
     *
     * @param id The laptop's UUID
     * @return model.models.Laptop or null if not found
     */
    public Laptop getLaptopById(UUID id) {
        synchronized (laptopsCache) {
            for (Laptop laptop : laptopsCache) {
                if (laptop.getId().equals(id)) {
                    return laptop;
                }
            }
        }
        
        // If not found in cache, try database
        try {
            Laptop laptop = laptopDAO.getById(id);
            if (laptop != null) {
                laptop.addObserver(this);
                
                // Add to cache
                synchronized (laptopsCache) {
                    laptopsCache.add(laptop);
                }
            }
            return laptop;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error retrieving laptop with ID " + id + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Finds an available laptop with a specific performance type.
     *
     * @param performanceType The performance type to find
     * @return An available laptop or null if none found
     */
    public Laptop findAvailableLaptop(PerformanceTypeEnum performanceType) {
        // First check cache
        synchronized (laptopsCache) {
            for (Laptop laptop : laptopsCache) {
                if (laptop.isAvailable() && laptop.getPerformanceType() == performanceType) {
                    return laptop;
                }
            }
        }
        
        // If not found in cache, try database
        try {
            List<Laptop> availableLaptops = laptopDAO.getAvailableLaptopsByPerformance(performanceType);
            if (!availableLaptops.isEmpty()) {
                Laptop laptop = availableLaptops.get(0);
                laptop.addObserver(this);
                
                // Add to cache
                synchronized (laptopsCache) {
                    laptopsCache.add(laptop);
                }
                
                return laptop;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error finding available laptop: " + e.getMessage(), e);
            
            // Notify observers about error
            setChanged();
            notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error finding available laptop", e));
        }
        
        return null;
    }
    
    /**
     * Creates a laptop asynchronously.
     *
     * @param brand Brand
     * @param model Model
     * @param gigabyte Hard disk capacity in GB
     * @param ram RAM in GB
     * @param performanceType Performance type
     */
    public void createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType) {
        executor.submit(() -> {
            try {
                // Validate input
                if (brand == null || brand.trim().isEmpty()) {
                    throw new IllegalArgumentException("Brand cannot be empty");
                }
                if (model == null || model.trim().isEmpty()) {
                    throw new IllegalArgumentException("Model cannot be empty");
                }
                if (gigabyte <= 0 || gigabyte > 4000) {
                    throw new IllegalArgumentException("Hard disk capacity must be between 1 and 4000 GB");
                }
                if (ram <= 0 || ram > 128) {
                    throw new IllegalArgumentException("RAM must be between 1 and 128 GB");
                }
                if (performanceType == null) {
                    throw new IllegalArgumentException("Performance type cannot be null");
                }
                
                // Create laptop
                Laptop laptop = new Laptop(brand, model, gigabyte, ram, performanceType);
                laptop.addObserver(this);
                
                boolean success = laptopDAO.insert(laptop);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to create laptop in database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error creating laptop: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error creating laptop", e));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Invalid input for laptop: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, e.getMessage(), e));
            }
        });
    }
    
    /**
     * Updates a laptop asynchronously.
     *
     * @param laptop The laptop to update
     */
    public void updateLaptop(Laptop laptop) {
        executor.submit(() -> {
            try {
                boolean success = laptopDAO.update(laptop);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to update laptop in database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error updating laptop: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error updating laptop", e));
            }
        });
    }
    
    /**
     * Updates only the state of a laptop asynchronously.
     *
     * @param laptop The laptop with the new state
     */
    public void updateLaptopState(Laptop laptop) {
        executor.submit(() -> {
            try {
                boolean success = laptopDAO.updateState(laptop);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to update laptop state in database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error updating laptop state: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error updating laptop state", e));
            }
        });
    }
    
    /**
     * Deletes a laptop asynchronously.
     *
     * @param id The laptop's UUID
     */
    public void deleteLaptop(UUID id) {
        executor.submit(() -> {
            try {
                boolean success = laptopDAO.delete(id);
                
                if (!success) {
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                            "Failed to delete laptop from database", null));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error deleting laptop: " + e.getMessage(), e);
                
                // Notify observers about error
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, "Error deleting laptop", e));
            }
        });
    }
    
    /**
     * Updates the cache when receiving events from LaptopDAO or model.models.Laptop objects.
     */
    @Override
    public void update(Observable o, Object arg) {
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
                    case LaptopDAO.EVENT_LAPTOP_STATE_CHANGED:
                        if (event instanceof LaptopDAO.LaptopStateEvent) {
                            LaptopDAO.LaptopStateEvent stateEvent = (LaptopDAO.LaptopStateEvent) event;
                            handleLaptopStateChanged((Laptop) stateEvent.getData(), 
                                    stateEvent.getOldState(), stateEvent.getNewState());
                        }
                        break;
                    case LaptopDAO.EVENT_LAPTOP_ERROR:
                        // Forward error events
                        setChanged();
                        notifyObservers(new ErrorEvent(EVENT_OPERATION_ERROR, 
                                event.getData().toString(), event.getException()));
                        break;
                }
            }
        } else if (o instanceof Laptop) {
            // Handle events from individual model.models.Laptop objects
            if (arg instanceof ModelObservable.PropertyChangeInfo) {
                ModelObservable.PropertyChangeInfo info = (ModelObservable.PropertyChangeInfo) arg;
                
                if ("state".equals(info.getPropertyName()) || "stateClassName".equals(info.getPropertyName())) {
                    // State has changed, update statistics
                    setChanged();
                    notifyObservers(new CountsEvent("availableLaptopsCount", getAvailableLaptopsCount()));
                    setChanged();
                    notifyObservers(new CountsEvent("loanedLaptopsCount", getLoanedLaptopsCount()));
                }
            }
        }
    }
    
    /**
     * Handles laptop creation events.
     *
     * @param laptop The created laptop
     */
    private void handleLaptopCreated(Laptop laptop) {
        boolean added = false;
        
        synchronized (laptopsCache) {
            // Check if laptop is already in cache
            boolean exists = false;
            for (Laptop l : laptopsCache) {
                if (l.getId().equals(laptop.getId())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                laptopsCache.add(laptop);
                laptop.addObserver(this);
                added = true;
            }
        }
        
        if (added) {
            // Notify observers
            setChanged();
            notifyObservers(new ViewModelEvent(EVENT_LAPTOP_CREATED, laptop));
            
            // Update counts
            setChanged();
            notifyObservers(new CountsEvent("availableLaptopsCount", getAvailableLaptopsCount()));
            setChanged();
            notifyObservers(new CountsEvent("loanedLaptopsCount", getLoanedLaptopsCount()));
        }
    }
    
    /**
     * Handles laptop update events.
     *
     * @param laptop The updated laptop
     */
    private void handleLaptopUpdated(Laptop laptop) {
        boolean updated = false;
        
        synchronized (laptopsCache) {
            for (int i = 0; i < laptopsCache.size(); i++) {
                if (laptopsCache.get(i).getId().equals(laptop.getId())) {
                    Laptop oldLaptop = laptopsCache.get(i);
                    laptopsCache.set(i, laptop);
                    laptop.addObserver(this);
                    updated = true;
                    break;
                }
            }
        }
        
        if (updated) {
            // Notify observers
            setChanged();
            notifyObservers(new ViewModelEvent(EVENT_LAPTOP_UPDATED, laptop));
        }
    }
    
    /**
     * Handles laptop deletion events.
     *
     * @param laptop The deleted laptop
     */
    private void handleLaptopDeleted(Laptop laptop) {
        boolean removed = false;
        
        synchronized (laptopsCache) {
            Iterator<Laptop> iterator = laptopsCache.iterator();
            while (iterator.hasNext()) {
                Laptop l = iterator.next();
                if (l.getId().equals(laptop.getId())) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
        }
        
        if (removed) {
            // Notify observers
            setChanged();
            notifyObservers(new ViewModelEvent(EVENT_LAPTOP_DELETED, laptop));
            
            // Update counts
            setChanged();
            notifyObservers(new CountsEvent("availableLaptopsCount", getAvailableLaptopsCount()));
            setChanged();
            notifyObservers(new CountsEvent("loanedLaptopsCount", getLoanedLaptopsCount()));
        }
    }
    
    /**
     * Handles laptop state change events.
     *
     * @param laptop The laptop whose state changed
     * @param oldState The old state
     * @param newState The new state
     */
    private void handleLaptopStateChanged(Laptop laptop, String oldState, String newState) {
        // Notify observers
        setChanged();
        notifyObservers(new StateChangedEvent(EVENT_LAPTOP_STATE_CHANGED, laptop, oldState, newState));
        
        // Update counts
        setChanged();
        notifyObservers(new CountsEvent("availableLaptopsCount", getAvailableLaptopsCount()));
        setChanged();
        notifyObservers(new CountsEvent("loanedLaptopsCount", getLoanedLaptopsCount()));
    }
    
    /**
     * Closes resources when the ViewModel is no longer needed.
     */
    public void close() {
        laptopDAO.deleteObserver(this);
        
        synchronized (laptopsCache) {
            for (Laptop laptop : laptopsCache) {
                laptop.deleteObserver(this);
            }
        }
        
        executor.shutdown();
    }
    
    /**
     * Event class for ViewModel operations.
     */
    public static class ViewModelEvent {
        private final String eventType;
        private final Object data;
        
        public ViewModelEvent(String eventType, Object data) {
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
     * Event class for count updates.
     */
    public static class CountsEvent {
        private final String countType;
        private final int count;
        
        public CountsEvent(String countType, int count) {
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
     * Event class for state changes.
     */
    public static class StateChangedEvent extends ViewModelEvent {
        private final String oldState;
        private final String newState;
        
        public StateChangedEvent(String eventType, Laptop laptop, String oldState, String newState) {
            super(eventType, laptop);
            this.oldState = oldState;
            this.newState = newState;
        }
        
        public String getOldState() {
            return oldState;
        }
        
        public String getNewState() {
            return newState;
        }
    }
    
    /**
     * Event class for errors.
     */
    public static class ErrorEvent extends ViewModelEvent {
        private final Exception exception;
        
        public ErrorEvent(String eventType, String message, Exception exception) {
            super(eventType, message);
            this.exception = exception;
        }
        
        public Exception getException() {
            return exception;
        }
    }
}