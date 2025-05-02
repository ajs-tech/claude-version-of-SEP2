package model.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception der kastes ved valideringsfejl.
 * Kan indeholde flere validationsfejl for forskellige felter.
 */
public class ValidationException extends LaptopSystemException {
    private final Map<String, String> validationErrors;

    /**
     * Opretter en ny validerings-exception med én fejl.
     *
     * @param fieldName Navnet på feltet med fejl
     * @param errorMessage Fejlbeskeden
     */
    public ValidationException(String fieldName, String errorMessage) {
        super("Valideringsfejl: " + fieldName + " - " + errorMessage);
        this.validationErrors = new HashMap<>();
        this.validationErrors.put(fieldName, errorMessage);
    }

    /**
     * Opretter en ny validerings-exception med flere fejl.
     *
     * @param validationErrors Map med felt-navne og tilhørende fejlbeskeder
     */
    public ValidationException(Map<String, String> validationErrors) {
        super("Flere valideringsfejl: " + validationErrors.size() + " fejl");
        this.validationErrors = new HashMap<>(validationErrors);
    }

    /**
     * Returnerer alle valideringsfejl.
     *
     * @return Map med felt-navne og tilhørende fejlbeskeder
     */
    public Map<String, String> getValidationErrors() {
        return new HashMap<>(validationErrors);
    }

    /**
     * Tjekker om der er fejl for et specifikt felt.
     *
     * @param fieldName Feltnavn at tjekke
     * @return true hvis der er fejl for feltet
     */
    public boolean hasErrorForField(String fieldName) {
        return validationErrors.containsKey(fieldName);
    }

    /**
     * Henter fejlbesked for et specifikt felt.
     *
     * @param fieldName Feltnavn at hente fejl for
     * @return Fejlbesked eller null hvis ingen fejl for feltet
     */
    public String getErrorForField(String fieldName) {
        return validationErrors.get(fieldName);
    }

    /**
     * Returnerer antal fejl.
     *
     * @return Antal felter med fejl
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
}