package model.database;

import model.enums.PerformanceTypeEnum;
import model.events.SystemEvents;
import model.log.Log;
import model.models.AvailableState;
import model.models.Laptop;
import model.models.LoanedState;
import model.util.EventBus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Laptop entiteter med forbedret implementering.
 * Implementerer GenericDAO for standardiserede databaseoperationer.
 */
public class LaptopDAO implements GenericDAO<Laptop, UUID> {
    private static final Logger logger = Logger.getLogger(LaptopDAO.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    /**
     * Henter alle laptops fra databasen.
     *
     * @return Liste af laptops
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public List<Laptop> getAll() throws SQLException {
        List<Laptop> laptops = new ArrayList<>();
        String sql = "SELECT laptop_uuid, brand, model, gigabyte, ram, performance_type, state FROM Laptop";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Laptop laptop = mapResultSetToLaptop(rs);
                laptops.add(laptop);
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af alle laptops", e);
            throw e;
        }
        return laptops;
    }

    /**
     * Henter laptop baseret på UUID.
     *
     * @param id Laptop UUID
     * @return Laptop object eller null hvis ikke fundet
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public Laptop getById(UUID id) throws SQLException {
        String sql = "SELECT laptop_uuid, brand, model, gigabyte, ram, performance_type, state FROM Laptop WHERE laptop_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLaptop(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af laptop med ID " + id, e);
            throw e;
        }
        return null;
    }

    /**
     * Indsætter en ny laptop i databasen.
     *
     * @param laptop Laptop objekt
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean insert(Laptop laptop) throws SQLException {
        String sql = "INSERT INTO Laptop (laptop_uuid, brand, model, gigabyte, ram, performance_type, state) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, laptop.getId().toString());
            stmt.setString(2, laptop.getBrand());
            stmt.setString(3, laptop.getModel());
            stmt.setInt(4, laptop.getGigabyte());
            stmt.setInt(5, laptop.getRam());
            stmt.setString(6, laptop.getPerformanceType().name());
            stmt.setString(7, laptop.getStateClassName());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Laptop [" + laptop.getBrand() + " " + laptop.getModel() +
                        ", ID: " + laptop.getId() + "] oprettet i database");

                // Post event
                eventBus.post(new SystemEvents.LaptopCreatedEvent(laptop));
            } else {
                log.warning("Kunne ikke oprette laptop i database: " + laptop.getId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved indsættelse af laptop: " + laptop.getId(), e);
            throw e;
        }
    }

    /**
     * Opdaterer en eksisterende laptop.
     *
     * @param laptop Laptop objekt med opdaterede oplysninger
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean update(Laptop laptop) throws SQLException {
        String sql = "UPDATE Laptop SET brand = ?, model = ?, gigabyte = ?, ram = ?, performance_type = ?, state = ? " +
                "WHERE laptop_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, laptop.getBrand());
            stmt.setString(2, laptop.getModel());
            stmt.setInt(3, laptop.getGigabyte());
            stmt.setInt(4, laptop.getRam());
            stmt.setString(5, laptop.getPerformanceType().name());
            stmt.setString(6, laptop.getStateClassName());
            stmt.setString(7, laptop.getId().toString());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Laptop [" + laptop.getBrand() + " " + laptop.getModel() +
                        ", ID: " + laptop.getId() + "] opdateret i database");

                // Post event
                eventBus.post(new SystemEvents.LaptopUpdatedEvent(laptop));
            } else {
                log.warning("Kunne ikke opdatere laptop i database: " + laptop.getId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved opdatering af laptop: " + laptop.getId(), e);
            throw e;
        }
    }

    /**
     * Opdaterer kun en laptops tilstand i databasen.
     *
     * @param laptop Laptop objekt med den nye tilstand
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean updateState(Laptop laptop) throws SQLException {
        String sql = "UPDATE Laptop SET state = ? WHERE laptop_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, laptop.getStateClassName());
            stmt.setString(2, laptop.getId().toString());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Laptop [" + laptop.getBrand() + " " + laptop.getModel() +
                        ", ID: " + laptop.getId() + "] tilstand ændret til " + laptop.getStateClassName());

                // Post event om tilstandsændring
                boolean isNowAvailable = laptop.isAvailable();
                eventBus.post(new SystemEvents.LaptopStateChangedEvent(
                        laptop,
                        isNowAvailable ? "LoanedState" : "AvailableState",
                        laptop.getStateClassName(),
                        isNowAvailable));
            } else {
                log.warning("Kunne ikke opdatere laptop tilstand i database: " + laptop.getId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved opdatering af laptop tilstand: " + laptop.getId(), e);
            throw e;
        }
    }

    /**
     * Sletter en laptop fra databasen.
     *
     * @param id Laptop UUID
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean delete(UUID id) throws SQLException {
        // Først hent lapptoppen så vi kan sende event efter sletning
        Laptop laptop = getById(id);
        if (laptop == null) {
            return false;
        }

        String sql = "DELETE FROM Laptop WHERE laptop_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Laptop [ID: " + id + "] slettet fra database");

                // Post event
                eventBus.post(new SystemEvents.LaptopDeletedEvent(laptop));
            } else {
                log.warning("Kunne ikke slette laptop fra database: " + id);
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved sletning af laptop: " + id, e);
            throw e;
        }
    }

    /**
     * Henter alle tilgængelige laptops med en specifik ydelsesfaktor.
     *
     * @param performanceType Ydelsesfaktor at søge efter
     * @return Liste af tilgængelige laptops med den angivne ydelsesfaktor
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Laptop> getAvailableLaptopsByPerformance(PerformanceTypeEnum performanceType) throws SQLException {
        List<Laptop> laptops = new ArrayList<>();
        String sql = "SELECT laptop_uuid, brand, model, gigabyte, ram, performance_type, state FROM Laptop " +
                "WHERE performance_type = ? AND state = 'AvailableState'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, performanceType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Laptop laptop = mapResultSetToLaptop(rs);
                    laptops.add(laptop);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af tilgængelige laptops med performance type " + performanceType, e);
            throw e;
        }
        return laptops;
    }

    /**
     * Tæller antal laptops i databasen.
     *
     * @return Antal laptops
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Laptop";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            handleSQLException("Fejl ved optælling af laptops", e);
            throw e;
        }
    }

    /**
     * Tjekker om en laptop eksisterer i databasen.
     *
     * @param id Laptop UUID
     * @return true hvis laptopen findes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean exists(UUID id) throws SQLException {
        String sql = "SELECT 1 FROM Laptop WHERE laptop_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved tjek af laptops eksistens: " + id, e);
            throw e;
        }
    }

    /**
     * Tæller antal laptops med en bestemt tilstand.
     *
     * @param state Tilstandsklassenavn at tælle
     * @return Antal laptops med den tilstand
     * @throws SQLException hvis der er problemer med databasen
     */
    public int countByState(String state) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Laptop WHERE state = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, state);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved optælling af laptops med tilstand " + state, e);
            throw e;
        }
    }

    /**
     * Tæller antal tilgængelige laptops.
     *
     * @return Antal tilgængelige laptops
     * @throws SQLException hvis der er problemer med databasen
     */
    public int countAvailable() throws SQLException {
        return countByState("AvailableState");
    }

    /**
     * Tæller antal udlånte laptops.
     *
     * @return Antal udlånte laptops
     * @throws SQLException hvis der er problemer med databasen
     */
    public int countLoaned() throws SQLException {
        return countByState("LoanedState");
    }

    /**
     * Konverterer ResultSet til Laptop objekt.
     *
     * @param rs ResultSet at konvertere
     * @return Laptop objektet
     * @throws SQLException hvis der er problemer med databasen
     */
    private Laptop mapResultSetToLaptop(ResultSet rs) throws SQLException {
        UUID laptopId = UUID.fromString(rs.getString("laptop_uuid"));
        String brand = rs.getString("brand");
        String model = rs.getString("model");
        int gigabyte = rs.getInt("gigabyte");
        int ram = rs.getInt("ram");
        PerformanceTypeEnum performanceType = PerformanceTypeEnum.valueOf(rs.getString("performance_type"));

        // Opret laptop uden at binde til ReservationManager - dette bør gøres af datamanageren
        Laptop laptop = new Laptop(laptopId, brand, model, gigabyte, ram, performanceType);

        // Sæt tilstanden baseret på databaseværdien
        String stateName = rs.getString("state");
        if (stateName != null) {
            laptop.setStateFromDatabase(stateName);
        }

        return laptop;
    }

    /**
     * Håndterer SQLException med logging og event posting.
     *
     * @param message Fejlbeskeden
     * @param e SQLException undtagelsen
     */
    private void handleSQLException(String message, SQLException e) {
        logger.log(Level.SEVERE, message + ": " + e.getMessage(), e);
        log.error(message + ": " + e.getMessage());

        // Post database error event
        eventBus.post(new SystemEvents.DatabaseErrorEvent(message, e.getSQLState(), e));
    }
}