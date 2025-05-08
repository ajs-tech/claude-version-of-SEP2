package model.util;

import java.util.Observable;

/**
 * Base class for observable model objects.
 * Extends Java's Observable to provide standardized notification methods.
 */
public class ModelObservable extends Observable {

    /**
     * Notifies observers about a property change.
     *
     * @param propertyName The name of the property that changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property
     */
    protected void notifyPropertyChanged(String propertyName, Object oldValue, Object newValue) {
        setChanged();
        notifyObservers(new PropertyChangeInfo(propertyName, oldValue, newValue));
    }

    /**
     * Notifies observers about an event.
     *
     * @param eventType The type of event
     * @param data Additional data associated with the event
     */
    protected void notifyEvent(String eventType, Object data) {
        setChanged();
        notifyObservers(new EventInfo(eventType, data));
    }

    /**
     * Class that encapsulates property change information.
     */
    public static class PropertyChangeInfo {
        private final String propertyName;
        private final Object oldValue;
        private final Object newValue;

        public PropertyChangeInfo(String propertyName, Object oldValue, Object newValue) {
            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;
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
    }

    /**
     * Class that encapsulates event information.
     */
    public static class EventInfo {
        private final String eventType;
        private final Object data;

        public EventInfo(String eventType, Object data) {
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
}