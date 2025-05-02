package model.database;

import model.enums.ReservationStatusEnum;
import model.events.SystemEvents;
import model.log.Log;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;
import model.util.EventBus;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Reservation entiteter med forbedret implementering.
 * Implementerer GenericDAO for standardiserede databaseoperationer.
 * Inkluderer robust transaktionshåndtering.
 */
public class ReservationDAO implements GenericDAO<Reservation, UUID> {
    private static final Logger logger = Logger.getLogger(ReservationDAO.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    // DAO dependencies
    private final LaptopDAO laptopDAO;
    private final StudentDAO studentDAO;

    /**
     * Konstruktør der initialiserer DAO dependencies.
     */
    public ReservationDAO() {
        this.laptopDAO = new LaptopDAO();
        this.studentDAO = new StudentDAO();
    }

    /**
     * Henter alle reservationer fra databasen.
     *
     * @return Liste af reservationer
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.reservation_uuid, r.status, r.laptop_uuid, r.student_via_id, r.creation_date " +
                "FROM Reservation r";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Reservation reservation = mapResultSetToReservation(rs);
                if (reservation != null) {
                    reservations.add(reservation);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af alle reservationer", e);
            throw e;
        }
        return reservations;
    }

    /**
     * Henter en specifik reservation baseret på UUID.
     *
     * @param id Reservation UUID
     * @return Reservation objekt eller null hvis ikke fundet
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public Reservation getById(UUID id) throws SQLException {
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE reservation_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af reservation med ID " + id, e);
            throw e;
        }
        return null;
    }

    /**
     * Indsætter en ny reservation i databasen.
     *
     * @param reservation Reservation objekt
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean insert(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO Reservation (reservation_uuid, laptop_uuid, student_via_id, status, creation_date) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getReservationId().toString());
            stmt.setString(2, reservation.getLaptop().getId().toString());
            stmt.setInt(3, reservation.getStudent().getViaId());
            stmt.setString(4, reservation.getStatus().name());
            stmt.setTimestamp(5, new Timestamp(reservation.getCreationDate().getTime()));

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Reservation [ID: " + reservation.getReservationId() + "] oprettet: " +
                        reservation.getStudent().getName() + " -> " +
                        reservation.getLaptop().getBrand() + " " + reservation.getLaptop().getModel());

                // Post event
                eventBus.post(new SystemEvents.ReservationCreatedEvent(reservation));
            } else {
                log.warning("Kunne ikke oprette reservation i database: " + reservation.getReservationId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved indsættelse af reservation: " + reservation.getReservationId(), e);
            throw e;
        }
    }

    /**
     * Opdaterer en eksisterende reservation.
     *
     * @param reservation Reservation objekt med opdaterede oplysninger
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean update(Reservation reservation) throws SQLException {
        String sql = "UPDATE Reservation SET status = ? WHERE reservation_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setString(2, reservation.getReservationId().toString());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Reservation [ID: " + reservation.getReservationId() + "] opdateret til status: " +
                        reservation.getStatus().getDisplayName());

                // Post event om statusændring - vent med at hente den gamle status her
                // da det ville kræve en ekstra databaseforespørgsel
            } else {
                log.warning("Kunne ikke opdatere reservation i database: " + reservation.getReservationId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved opdatering af reservation: " + reservation.getReservationId(), e);
            throw e;
        }
    }

    /**
     * Sletter en reservation baseret på UUID.
     *
     * @param id Reservation UUID
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean delete(UUID id) throws SQLException {
        // Først hent reservationen så vi kan sende event efter sletning
        Reservation reservation = getById(id);
        if (reservation == null) {
            return false;
        }

        String sql = "DELETE FROM Reservation WHERE reservation_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Reservation [ID: " + id + "] slettet fra database");

                // Her kunne man poste et ReservationDeletedEvent hvis det var defineret
            } else {
                log.warning("Kunne ikke slette reservation fra database: " + id);
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved sletning af reservation: " + id, e);
            throw e;
        }
    }

    /**
     * Tæller antal reservationer i databasen.
     *
     * @return Antal reservationer
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Reservation";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            handleSQLException("Fejl ved optælling af reservationer", e);
            throw e;
        }
    }

    /**
     * Tjekker om en reservation eksisterer i databasen.
     *
     * @param id Reservation UUID
     * @return true hvis reservationen findes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean exists(UUID id) throws SQLException {
        String sql = "SELECT 1 FROM Reservation WHERE reservation_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved tjek af reservations eksistens: " + id, e);
            throw e;
        }
    }

    /**
     * Henter alle reservationer for en bestemt student.
     *
     * @param studentViaId Student VIA ID
     * @return Liste af reservationer for den pågældende student
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Reservation> getByStudentId(int studentViaId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE student_via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentViaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapResultSetToReservation(rs);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af reservationer for student " + studentViaId, e);
            throw e;
        }
        return reservations;
    }

    /**
     * Henter alle reservationer for en bestemt laptop.
     *
     * @param laptopId Laptop UUID
     * @return Liste af reservationer for den pågældende laptop
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Reservation> getByLaptopId(UUID laptopId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE laptop_uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, laptopId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapResultSetToReservation(rs);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af reservationer for laptop " + laptopId, e);
            throw e;
        }
        return reservations;
    }

    /**
     * Henter alle aktive reservationer.
     *
     * @return Liste af aktive reservationer
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Reservation> getActiveReservations() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT reservation_uuid, status, laptop_uuid, student_via_id, creation_date " +
                "FROM Reservation WHERE status = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ReservationStatusEnum.ACTIVE.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapResultSetToReservation(rs);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af aktive reservationer", e);
            throw e;
        }
        return reservations;
    }

    /**
     * Opret reservation med transaktionssupport, der opdaterer laptop og student status.
     *
     * @param reservation Reservationsobjekt at oprette
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean createReservationWithTransaction(Reservation reservation) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Indsæt reservation
            String sql = "INSERT INTO Reservation (reservation_uuid, laptop_uuid, student_via_id, status, creation_date) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reservation.getReservationId().toString());
                stmt.setString(2, reservation.getLaptop().getId().toString());
                stmt.setInt(3, reservation.getStudent().getViaId());
                stmt.setString(4, reservation.getStatus().name());
                stmt.setTimestamp(5, new Timestamp(reservation.getCreationDate().getTime()));
                stmt.executeUpdate();
            }

            // 2. Opdater laptop tilstand
            sql = "UPDATE Laptop SET state = 'LoanedState' WHERE laptop_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reservation.getLaptop().getId().toString());
                stmt.executeUpdate();
            }

            // 3. Opdater student has_laptop
            sql = "UPDATE Student SET has_laptop = TRUE WHERE via_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reservation.getStudent().getViaId());
                stmt.executeUpdate();
            }

            // Commit transaktionen
            conn.commit();

            log.info("Reservation [ID: " + reservation.getReservationId() + "] oprettet med transaktion: " +
                    reservation.getStudent().getName() + " -> " +
                    reservation.getLaptop().getBrand() + " " + reservation.getLaptop().getModel());

            // Post event
            eventBus.post(new SystemEvents.ReservationCreatedEvent(reservation));

            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warning("Transaktion rullet tilbage: " + e.getMessage());
                } catch (SQLException ex) {
                    log.error("Fejl under rollback: " + ex.getMessage());
                }
            }
            handleSQLException("Fejl ved oprettelse af reservation med transaktion: " +
                    reservation.getReservationId(), e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.warning("Fejl ved nulstilling af forbindelse: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Opdater reservationsstatus med transaktion, der også opdaterer laptop og student status.
     *
     * @param reservation Reservationsobjekt med den nye status
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean updateStatusWithTransaction(Reservation reservation) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Hent den nuværende status
            String selectSql = "SELECT status FROM Reservation WHERE reservation_uuid = ?";
            ReservationStatusEnum currentStatus;
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, reservation.getReservationId().toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return false; // Reservation findes ikke
                    }
                    currentStatus = ReservationStatusEnum.valueOf(rs.getString("status"));
                }
            }

            // 2. Opdater reservation status
            String updateSql = "UPDATE Reservation SET status = ? WHERE reservation_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, reservation.getStatus().name());
                stmt.setString(2, reservation.getReservationId().toString());
                stmt.executeUpdate();
            }

            // 3. Hvis status ændres fra Active til completed eller cancelled
            if (currentStatus == ReservationStatusEnum.ACTIVE &&
                    (reservation.getStatus() == ReservationStatusEnum.COMPLETED ||
                            reservation.getStatus() == ReservationStatusEnum.CANCELLED)) {

                // Opdater laptop tilstand til Available
                String laptopSql = "UPDATE Laptop SET state = 'AvailableState' WHERE laptop_uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(laptopSql)) {
                    stmt.setString(1, reservation.getLaptop().getId().toString());
                    stmt.executeUpdate();
                }

                // Opdater student has_laptop status
                String studentSql = "UPDATE Student SET has_laptop = FALSE WHERE via_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(studentSql)) {
                    stmt.setInt(1, reservation.getStudent().getViaId());
                    stmt.executeUpdate();
                }
            }

            // Commit transaktionen
            conn.commit();

            log.info("Reservation [ID: " + reservation.getReservationId() + "] status ændret fra " +
                    currentStatus.getDisplayName() + " til " + reservation.getStatus().getDisplayName());

            // Post event
            eventBus.post(new SystemEvents.ReservationStatusChangedEvent(
                    reservation, currentStatus, reservation.getStatus()));

            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warning("Transaktion rullet tilbage: " + e.getMessage());
                } catch (SQLException ex) {
                    log.error("Fejl under rollback: " + ex.getMessage());
                }
            }
            handleSQLException("Fejl ved opdatering af reservationsstatus med transaktion: " +
                    reservation.getReservationId(), e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.warning("Fejl ved nulstilling af forbindelse: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Konverterer ResultSet til Reservation objekt.
     */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        UUID reservationId = UUID.fromString(rs.getString("reservation_uuid"));
        ReservationStatusEnum status = ReservationStatusEnum.valueOf(rs.getString("status"));
        UUID laptopUUID = UUID.fromString(rs.getString("laptop_uuid"));
        int studentViaId = rs.getInt("student_via_id");
        Timestamp creationTimestamp = rs.getTimestamp("creation_date");
        Date creationDate = creationTimestamp != null ? new Date(creationTimestamp.getTime()) : new Date();

        // Hent tilknyttet Laptop og Student ved hjælp af deres DAOs
        Laptop laptop = laptopDAO.getById(laptopUUID);
        Student student = studentDAO.getById(studentViaId);

        // Tjek om Laptop og Student blev fundet
        if (laptop == null) {
            logger.log(Level.WARNING, "Laptop med UUID " + laptopUUID + " blev ikke fundet til reservation " + reservationId);
            return null;
        }
        if (student == null) {
            logger.log(Level.WARNING, "Student med VIA ID " + studentViaId + " blev ikke fundet til reservation " + reservationId);
            return null;
        }

        // Opret Reservation objekt med den korrekte konstruktør
        return new Reservation(reservationId, student, laptop, status, creationDate);
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