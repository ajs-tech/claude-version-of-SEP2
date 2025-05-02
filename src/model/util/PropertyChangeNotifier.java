package model.util;

import java.beans.PropertyChangeListener;

/**
 * Interface der definerer kontrakten for klasser, der understøtter property change events.
 * Implementerer Observer Pattern for konsistent implementation på tværs af modeller.
 */
public interface PropertyChangeNotifier {

    /**
     * Tilføjer en listener der notificeres ved alle property changes.
     *
     * @param listener Lytteren der skal tilføjes
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Fjerner en listener.
     *
     * @param listener Lytteren der skal fjernes
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Tilføjer en listener der kun notificeres ved ændringer af en specifik property.
     *
     * @param propertyName Navnet på property der skal lyttes på
     * @param listener Lytteren der skal tilføjes
     */
    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    /**
     * Fjerner en listener fra en specifik property.
     *
     * @param propertyName Navnet på property der ikke længere skal lyttes på
     * @param listener Lytteren der skal fjernes
     */
    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);
}
