package model.exceptions;

import java.sql.SQLException;

/**
 * Exception der kastes ved database-relaterede fejl.
 */
public class DatabaseException extends LaptopSystemException {
    private final String sqlState;
    private final int errorCode;

    /**
     * Opretter en ny database-exception med en besked.
     *
     * @param message Fejlbeskeden
     */
    public DatabaseException(String message) {
        super(message);
        this.sqlState = null;
        this.errorCode = 0;
    }

    /**
     * Opretter en ny database-exception baseret på en SQLException.
     *
     * @param message Fejlbeskeden
     * @param cause   Den underliggende SQLException
     */
    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
        this.sqlState = cause.getSQLState();
        this.errorCode = cause.getErrorCode();
    }

    /**
     * Returnerer SQL-state koden.
     *
     * @return SQL-state eller null hvis ikke tilgængelig
     */
    public String getSqlState() {
        return sqlState;
    }

    /**
     * Returnerer fejlkoden.
     *
     * @return SQL fejlkode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Tjekker om fejlen er relateret til connection-problemer.
     *
     * @return true hvis det er et connection-problem
     */
    public boolean isConnectionError() {
        if (sqlState == null) {
            return false;
        }
        // SQL-state koder der indikerer connection-fejl
        return sqlState.startsWith("08");
    }

    /**
     * Tjekker om fejlen er relateret til unik constraints.
     *
     * @return true hvis det er en unique constraint violation
     */
    public boolean isUniqueConstraintViolation() {
        if (sqlState == null) {
            return false;
        }
        // SQL-state koder der indikerer unique constraint violations
        return sqlState.equals("23505") || // PostgreSQL
                sqlState.equals("23000");   // MySQL/SQLite
    }
}