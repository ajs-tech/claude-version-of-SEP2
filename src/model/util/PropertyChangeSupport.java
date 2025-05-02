package model.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hjælpeklasse til håndtering af PropertyChangeListeners.
 * Mere robust implementation end Java's standard PropertyChangeSupport.
 */
public class PropertyChangeSupport {
    private final Object source;
    private final Map<String, List<PropertyChangeListener>> namedListeners = new HashMap<>();
    private final List<PropertyChangeListener> generalListeners = new ArrayList<>();

    /**
     * Opretter en ny PropertyChangeSupport instans.
     *
     * @param source Objektet som er kilden til property ændringer
     */
    public PropertyChangeSupport(Object source) {
        this.source = source;
    }

    /**
     * Tilføjer en PropertyChangeListener, der lytter på alle properties.
     *
     * @param listener Lytteren der skal tilføjes
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (generalListeners) {
            if (!generalListeners.contains(listener)) {
                generalListeners.add(listener);
            }
        }
    }

    /**
     * Fjerner en PropertyChangeListener, der lytter på alle properties.
     *
     * @param listener Lytteren der skal fjernes
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (generalListeners) {
            generalListeners.remove(listener);
        }
    }

    /**
     * Tilføjer en PropertyChangeListener, der kun lytter på en specifik property.
     *
     * @param propertyName Navnet på property der skal lyttes på
     * @param listener Lytteren der skal tilføjes
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null || propertyName.isEmpty()) {
            return;
        }

        synchronized (namedListeners) {
            List<PropertyChangeListener> listeners = namedListeners.computeIfAbsent(
                    propertyName, k -> new ArrayList<>());

            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Fjerner en PropertyChangeListener, der kun lytter på en specifik property.
     *
     * @param propertyName Navnet på property der ikke længere skal lyttes på
     * @param listener Lytteren der skal fjernes
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null || propertyName.isEmpty()) {
            return;
        }

        synchronized (namedListeners) {
            List<PropertyChangeListener> listeners = namedListeners.get(propertyName);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    namedListeners.remove(propertyName);
                }
            }
        }
    }

    /**
     * Udløser en property change event til alle relevante lyttere.
     *
     * @param propertyName Navnet på property der er ændret
     * @param oldValue Den gamle værdi
     * @param newValue Den nye værdi
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyName == null) {
            throw new NullPointerException("Property name cannot be null");
        }

        // Skip hvis værdierne er ens (inkl. begge null)
        if (oldValue == null && newValue == null) {
            return;
        }

        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }

        PropertyChangeEvent event = new PropertyChangeEvent(source, propertyName, oldValue, newValue);

        // Notificér property-specifikke lyttere
        List<PropertyChangeListener> specific;
        synchronized (namedListeners) {
            specific = namedListeners.get(propertyName);
            if (specific != null) {
                specific = new ArrayList<>(specific); // Kopiér for thread-sikkerhed
            }
        }

        if (specific != null) {
            for (PropertyChangeListener listener : specific) {
                try {
                    listener.propertyChange(event);
                } catch (Exception e) {
                    // Log fejl, men fortsæt til næste listener
                    System.err.println("Error in property change listener: " + e.getMessage());
                }
            }
        }

        // Notificér generelle lyttere
        List<PropertyChangeListener> general;
        synchronized (generalListeners) {
            general = new ArrayList<>(generalListeners); // Kopiér for thread-sikkerhed
        }

        for (PropertyChangeListener listener : general) {
            try {
                listener.propertyChange(event);
            } catch (Exception e) {
                // Log fejl, men fortsæt til næste listener
                System.err.println("Error in property change listener: " + e.getMessage());
            }
        }
    }

    /**
     * Returnerer antal lyttere registreret til dette objekt.
     *
     * @return Samlet antal registrerede lyttere
     */
    public int getListenerCount() {
        int count = generalListeners.size();

        synchronized (namedListeners) {
            for (List<PropertyChangeListener> listeners : namedListeners.values()) {
                count += listeners.size();
            }
        }

        return count;
    }

    /**
     * Returnerer antal lyttere registreret til en specifik property.
     *
     * @param propertyName Navn på property
     * @return Antal registrerede lyttere for property
     */
    public int getListenerCount(String propertyName) {
        if (propertyName == null) {
            return 0;
        }

        synchronized (namedListeners) {
            List<PropertyChangeListener> listeners = namedListeners.get(propertyName);
            return listeners != null ? listeners.size() : 0;
        }
    }
}