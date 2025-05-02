package model.exceptions;

/**
 * Base exception klasse for laptop-udlånssystemet.
 * Alle andre exceptions i systemet arver fra denne.
 */
public class LaptopSystemException extends Exception {

    /**
     * Opretter en ny systemexception med en besked.
     *
     * @param message Fejlbeskeden
     */
    public LaptopSystemException(String message) {
        super(message);
    }

    /**
     * Opretter en ny systemexception med en besked og en underliggende årsag.
     *
     * @param message Fejlbeskeden
     * @param cause   Den underliggende årsag (f.eks. SQLException)
     */
    public LaptopSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}