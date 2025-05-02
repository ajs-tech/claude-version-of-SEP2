package model.enums;

/**
 * Enum der definerer de mulige statuser for reservationer.
 */
public enum ReservationStatusEnum {
    ACTIVE("ACTIVE", "Aktiv"),
    COMPLETED("COMPLETED", "Afsluttet"),
    CANCELLED("CANCELLED", "Annulleret");

    private final String dbValue;
    private final String displayName;

    /**
     * Konstruktør
     *
     * @param dbValue     Værdi der bruges i databasen
     * @param displayName Brugervenlig visningstekst
     */
    ReservationStatusEnum(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    /**
     * Returnerer den værdi der bruges i databasen
     *
     * @return Database-værdien
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Returnerer en brugervenlig tekst til visning i UI
     *
     * @return Læsbar tekst for status
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}