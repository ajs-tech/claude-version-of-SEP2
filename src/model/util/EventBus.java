package model.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * EventBus til dekobling af komponenter gennem event-baseret kommunikation.
 * Implementerer Singleton pattern.
 */
public class EventBus {
    private static EventBus instance;
    private static final Object lock = new Object();

    // Event subscribers mappet efter event type
    private final Map<Class<?>, List<EventSubscriber>> subscribers;

    /**
     * Event interface der definerer alle event typer.
     */
    public interface Event {
    }

    /**
     * Interface for event subscribers.
     *
     * @param <T> Event typen
     */
    public interface EventSubscriber<T extends Event> {
        void onEvent(T event);
    }

    /**
     * Privat konstruktør for Singleton.
     */
    private EventBus() {
        this.subscribers = new ConcurrentHashMap<>();
    }

    /**
     * Returnerer den eksisterende instans eller opretter en ny.
     *
     * @return EventBus instansen
     */
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

    /**
     * Registrerer en subscriber for en specifik event type.
     *
     * @param <T>        Event typen
     * @param eventType  Class objektet for event typen
     * @param subscriber Subscriber at registrere
     */
    public <T extends Event> void subscribe(Class<T> eventType, EventSubscriber<T> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    /**
     * Fjerner en subscriber for en specifik event type.
     *
     * @param <T>        Event typen
     * @param eventType  Class objektet for event typen
     * @param subscriber Subscriber at fjerne
     * @return true hvis subscriber blev fjernet
     */
    public <T extends Event> boolean unsubscribe(Class<T> eventType, EventSubscriber<T> subscriber) {
        List<EventSubscriber> eventSubscribers = subscribers.get(eventType);
        return eventSubscribers != null && eventSubscribers.remove(subscriber);
    }

    /**
     * Udløser et event til alle relevante subscribers.
     *
     * @param <T>   Event typen
     * @param event Event objektet
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void post(T event) {
        if (event == null) {
            return;
        }

        // Hent subscribers direkte til event typen
        List<EventSubscriber> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            for (EventSubscriber subscriber : eventSubscribers) {
                try {
                    subscriber.onEvent(event);
                } catch (Exception e) {
                    // Log fejl, men fortsæt til næste subscriber
                    System.err.println("Fejl i event subscriber: " + e.getMessage());
                }
            }
        }

        // Hent subscribers til superklasser/interfaces
        for (Map.Entry<Class<?>, List<EventSubscriber>> entry : subscribers.entrySet()) {
            if (entry.getKey() != event.getClass() && entry.getKey().isAssignableFrom(event.getClass())) {
                for (EventSubscriber subscriber : entry.getValue()) {
                    try {
                        subscriber.onEvent(event);
                    } catch (Exception e) {
                        // Log fejl, men fortsæt til næste subscriber
                        System.err.println("Fejl i event subscriber: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Fjerner alle subscribers.
     */
    public void clearAllSubscribers() {
        subscribers.clear();
    }

    /**
     * Fjerner alle subscribers for en specifik event type.
     *
     * @param <T>       Event typen
     * @param eventType Class objektet for event typen
     */
    public <T extends Event> void clearSubscribers(Class<T> eventType) {
        subscribers.remove(eventType);
    }

    /**
     * Returnerer antal subscribers for en specifik event type.
     *
     * @param <T>       Event typen
     * @param eventType Class objektet for event typen
     * @return Antal subscribers
     */
    public <T extends Event> int getSubscriberCount(Class<T> eventType) {
        List<EventSubscriber> eventSubscribers = subscribers.get(eventType);
        return eventSubscribers != null ? eventSubscribers.size() : 0;
    }

    /**
     * Returnerer det totale antal subscribers.
     *
     * @return Totalt antal subscribers
     */
    public int getTotalSubscriberCount() {
        int count = 0;
        for (List<EventSubscriber> eventSubscribers : subscribers.values()) {
            count += eventSubscribers.size();
        }
        return count;
    }
}