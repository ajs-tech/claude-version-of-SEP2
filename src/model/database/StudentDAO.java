package model.database;

import model.enums.PerformanceTypeEnum;
import model.events.SystemEvents;
import model.log.Log;
import model.models.Student;
import model.util.EventBus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Student entiteter med forbedret implementering.
 * Implementerer GenericDAO for standardiserede databaseoperationer.
 */
public class StudentDAO implements GenericDAO<Student, Integer> {
    private static final Logger logger = Logger.getLogger(StudentDAO.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    /**
     * Henter alle studerende fra databasen.
     *
     * @return Liste af studerende
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public List<Student> getAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
                students.add(student);
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af alle studerende", e);
            throw e;
        }
        return students;
    }

    /**
     * Henter en student baseret på VIA ID.
     *
     * @param id Student VIA ID
     * @return Student objekt eller null hvis ikke fundet
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public Student getById(Integer id) throws SQLException {
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student WHERE via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af student med ID " + id, e);
            throw e;
        }
        return null;
    }

    /**
     * Indsætter en ny student i databasen.
     *
     * @param student Student objekt
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean insert(Student student) throws SQLException {
        String sql = "INSERT INTO Student (via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, student.getViaId());
            stmt.setString(2, student.getName());
            stmt.setDate(3, new java.sql.Date(student.getDegreeEndDate().getTime()));
            stmt.setString(4, student.getDegreeTitle());
            stmt.setString(5, student.getEmail());
            stmt.setInt(6, student.getPhoneNumber());
            stmt.setString(7, student.getPerformanceNeeded().name());
            stmt.setBoolean(8, student.isHasLaptop());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Student [" + student.getName() + ", VIA ID: " + student.getViaId() + "] oprettet i database");

                // Post event
                eventBus.post(new SystemEvents.StudentCreatedEvent(student));
            } else {
                log.warning("Kunne ikke oprette student i database: " + student.getViaId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved indsættelse af student: " + student.getViaId(), e);
            throw e;
        }
    }

    /**
     * Opdaterer en eksisterende student.
     *
     * @param student Student objekt med opdaterede oplysninger
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean update(Student student) throws SQLException {
        String sql = "UPDATE Student SET name = ?, degree_end_date = ?, degree_title = ?, email = ?, " +
                "phone_number = ?, performance_needed = ?, has_laptop = ? WHERE via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, student.getName());
            stmt.setDate(2, new java.sql.Date(student.getDegreeEndDate().getTime()));
            stmt.setString(3, student.getDegreeTitle());
            stmt.setString(4, student.getEmail());
            stmt.setInt(5, student.getPhoneNumber());
            stmt.setString(6, student.getPerformanceNeeded().name());
            stmt.setBoolean(7, student.isHasLaptop());
            stmt.setInt(8, student.getViaId());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Student [" + student.getName() + ", VIA ID: " + student.getViaId() + "] opdateret i database");

                // Post event
                eventBus.post(new SystemEvents.StudentUpdatedEvent(student));
            } else {
                log.warning("Kunne ikke opdatere student i database: " + student.getViaId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved opdatering af student: " + student.getViaId(), e);
            throw e;
        }
    }

    /**
     * Sletter en student fra databasen.
     *
     * @param id Student VIA ID
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean delete(Integer id) throws SQLException {
        // Først hent studenten så vi kan sende event efter sletning
        Student student = getById(id);
        if (student == null) {
            return false;
        }

        // Først slet afhængige reservationer
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Reservation WHERE student_via_id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate(); // Vi ignorerer resultatet, da der måske ikke er nogen reservationer
        } catch (SQLException e) {
            log.warning("Advarsel: Kunne ikke slette reservationer for student " + id + ": " + e.getMessage());
            // Vi fortsætter alligevel
        }

        // Fjern fra eventuelle køer
        try {
            QueueDAO queueDAO = new QueueDAO();
            queueDAO.removeFromAllQueues(id);
        } catch (SQLException e) {
            log.warning("Advarsel: Kunne ikke fjerne student " + id + " fra køer: " + e.getMessage());
            // Vi fortsætter alligevel
        }

        // Derefter slet studenten
        String sql = "DELETE FROM Student WHERE via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Student [VIA ID: " + id + "] slettet fra database");

                // Post event
                eventBus.post(new SystemEvents.StudentDeletedEvent(student));
            } else {
                log.warning("Kunne ikke slette student fra database: " + id);
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved sletning af student: " + id, e);
            throw e;
        }
    }

    /**
     * Tæller antal studerende i databasen.
     *
     * @return Antal studerende
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Student";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            handleSQLException("Fejl ved optælling af studerende", e);
            throw e;
        }
    }

    /**
     * Tjekker om en student eksisterer i databasen.
     *
     * @param id Student VIA ID
     * @return true hvis studenten findes
     * @throws SQLException hvis der er problemer med databasen
     */
    @Override
    public boolean exists(Integer id) throws SQLException {
        String sql = "SELECT 1 FROM Student WHERE via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved tjek af students eksistens: " + id, e);
            throw e;
        }
    }

    /**
     * Finder studerende baseret på en performance type.
     *
     * @param performanceType Performance type at søge efter
     * @return Liste af studerende med den angivne performance type
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Student> getByPerformanceType(PerformanceTypeEnum performanceType) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student WHERE performance_needed = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, performanceType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Student student = mapResultSetToStudent(rs);
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af studerende med performance type " + performanceType, e);
            throw e;
        }

        return students;
    }

    /**
     * Finder studerende der har eller ikke har en laptop.
     *
     * @param hasLaptop true for studerende med laptop, false for dem uden
     * @return Liste af matchende studerende
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Student> getByHasLaptop(boolean hasLaptop) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT via_id, name, degree_end_date, degree_title, email, phone_number, " +
                "performance_needed, has_laptop FROM Student WHERE has_laptop = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, hasLaptop);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Student student = mapResultSetToStudent(rs);
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af studerende med hasLaptop=" + hasLaptop, e);
            throw e;
        }

        return students;
    }

    /**
     * Konverterer ResultSet til Student objekt.
     *
     * @param rs ResultSet at konvertere
     * @return Student objektet
     * @throws SQLException hvis der er problemer med databasen
     */
    public Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        int viaId = rs.getInt("via_id");
        String name = rs.getString("name");
        java.util.Date degreeEndDate = rs.getDate("degree_end_date");
        String degreeTitle = rs.getString("degree_title");
        String email = rs.getString("email");
        int phoneNumber = rs.getInt("phone_number");
        PerformanceTypeEnum performanceNeeded = PerformanceTypeEnum.valueOf(rs.getString("performance_needed"));
        boolean hasLaptop = rs.getBoolean("has_laptop");

        Student student = new Student(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);

        // Sæt hasLaptop baseret på databaseværdien
        if (hasLaptop != student.isHasLaptop()) {
            student.setHasLaptop(hasLaptop);
        }

        return student;
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