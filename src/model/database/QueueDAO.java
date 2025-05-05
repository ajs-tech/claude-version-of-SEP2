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
 * Data Access Object for køhåndtering med database persistering.
 * Implementerer forbedret fejlhåndtering og event notification.
 */
public class QueueDAO {
    private static final Logger logger = Logger.getLogger(QueueDAO.class.getName());
    private static final Log log = Log.getInstance();
    private static final EventBus eventBus = EventBus.getInstance();

    private final StudentDAO studentDAO;

    /**
     * Konstruktør der initialiserer DAO dependencies.
     */
    public QueueDAO() {
        this.studentDAO = new StudentDAO();
    }

    /**
     * Tilføj en student til ydelseskøen i databasen.
     *
     * @param student        Studenten der skal tilføjes
     * @param performanceType Ydelsestypen køen er for
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean addToQueue(Student student, PerformanceTypeEnum performanceType) throws SQLException {
        String sql = "INSERT INTO QueueEntry (student_via_id, performance_type, entry_date) VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, student.getViaId());
            stmt.setString(2, performanceType.name());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Student [" + student.getName() + ", VIA ID: " + student.getViaId() +
                        "] tilføjet til " + performanceType.getClass().getSimpleName() + "-ydelses kø");

                // Post event
                int newQueueSize = getQueueSize(performanceType);
                eventBus.post(new SystemEvents.StudentAddedToQueueEvent(student, performanceType, newQueueSize));
            } else {
                log.warning("Kunne ikke tilføje student til kø i databasen: VIA ID " + student.getViaId());
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved tilføjelse af student til kø: " + student.getViaId(), e);
            throw e;
        }
    }

    /**
     * Henter studerende i kø for en specifik ydelsestype, sorteret efter indgangsdato.
     *
     * @param performanceType Ydelsestypen at hente studerende for
     * @return Liste af studerende i kø
     * @throws SQLException hvis der er problemer med databasen
     */
    public List<Student> getStudentsInQueue(PerformanceTypeEnum performanceType) throws SQLException {
        List<Student> studentsInQueue = new ArrayList<>();
        String sql = "SELECT student_via_id FROM QueueEntry WHERE performance_type = ? ORDER BY entry_date ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, performanceType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int studentViaId = rs.getInt("student_via_id");
                    Student student = studentDAO.getById(studentViaId);
                    if (student != null) {
                        studentsInQueue.add(student);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af studerende i " + performanceType + " kø", e);
            throw e;
        }
        return studentsInQueue;
    }

    /**
     * Fjern en student fra køen.
     *
     * @param studentViaId    Studentens VIA ID
     * @param performanceType Ydelsestypen køen er for
     * @return true hvis operationen lykkedes
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean removeFromQueue(int studentViaId, PerformanceTypeEnum performanceType) throws SQLException {
        // Først hent studenten så vi kan sende event efter sletning
        Student student = studentDAO.getById(studentViaId);
        if (student == null) {
            return false;
        }

        String sql = "DELETE FROM QueueEntry WHERE student_via_id = ? AND performance_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentViaId);
            stmt.setString(2, performanceType.name());

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Student [" + student.getName() + ", VIA ID: " + studentViaId +
                        "] fjernet fra " + performanceType.getClass().getSimpleName() + "-ydelses kø");

                // Post event - vi antager ikke laptop tildeling her, det håndteres separat
                int newQueueSize = getQueueSize(performanceType);
                eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                        student, performanceType, newQueueSize, false));
            } else {
                log.warning("Kunne ikke fjerne student fra kø: VIA ID " + studentViaId);
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved fjernelse af student fra kø: " + studentViaId, e);
            throw e;
        }
    }

    /**
     * Fjern en student fra alle køer.
     *
     * @param studentViaId Studentens VIA ID
     * @return true hvis operationen lykkedes (studenten var i mindst én kø)
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean removeFromAllQueues(int studentViaId) throws SQLException {
        // Først hent studenten så vi kan sende event efter sletning
        Student student = studentDAO.getById(studentViaId);
        if (student == null) {
            return false;
        }

        // Tjek hvilke køer studenten er i før sletning
        boolean inHighQueue = isStudentInQueue(studentViaId, PerformanceTypeEnum.HIGH);
        boolean inLowQueue = isStudentInQueue(studentViaId, PerformanceTypeEnum.LOW);

        String sql = "DELETE FROM QueueEntry WHERE student_via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentViaId);

            int affectedRows = stmt.executeUpdate();

            boolean success = affectedRows > 0;
            if (success) {
                log.info("Student [" + student.getName() + ", VIA ID: " + studentViaId +
                        "] fjernet fra alle køer");

                // Post events for hver kø studenten var i
                if (inHighQueue) {
                    int newHighQueueSize = getQueueSize(PerformanceTypeEnum.HIGH);
                    eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                            student, PerformanceTypeEnum.HIGH, newHighQueueSize, false));
                }
                if (inLowQueue) {
                    int newLowQueueSize = getQueueSize(PerformanceTypeEnum.LOW);
                    eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                            student, PerformanceTypeEnum.LOW, newLowQueueSize, false));
                }
            } else {
                log.warning("Kunne ikke fjerne student fra køer eller studenten var ikke i nogen kø: VIA ID " + studentViaId);
            }

            return success;
        } catch (SQLException e) {
            handleSQLException("Fejl ved fjernelse af student fra alle køer: " + studentViaId, e);
            throw e;
        }
    }

    /**
     * Henter den næste student i kø for en ydelsestype og fjerner dem med transaktionssikkerhed.
     *
     * @param performanceType Ydelsestypen at hente fra
     * @return Studenten eller null hvis køen er tom
     * @throws SQLException hvis der er problemer med databasen
     */
    public Student getAndRemoveNextInQueue(PerformanceTypeEnum performanceType) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Få den næste student i kø
            String selectSql = "SELECT student_via_id, entry_id FROM QueueEntry WHERE performance_type = ? ORDER BY entry_date ASC LIMIT 1";
            Student student = null;
            int entryId = -1;

            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, performanceType.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int studentViaId = rs.getInt("student_via_id");
                        entryId = rs.getInt("entry_id");
                        student = studentDAO.getById(studentViaId);
                    }
                }
            }

            // Fjern fra kø hvis fundet
            if (student != null && entryId > 0) {
                String deleteSql = "DELETE FROM QueueEntry WHERE entry_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, entryId);
                    deleteStmt.executeUpdate();
                }

                log.info("Student [" + student.getName() + ", VIA ID: " + student.getViaId() +
                        "] hentet og fjernet fra " + performanceType.getClass().getSimpleName() + "-ydelses kø");

                // Event for fjernelse - laptop tildeling vil ske separat
                int newQueueSize = getQueueSize(performanceType);
                eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                        student, performanceType, newQueueSize, true));
            }

            conn.commit();
            return student;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warning("Transaktion rullet tilbage: " + e.getMessage());
                } catch (SQLException ex) {
                    log.error("Fejl under rollback: " + ex.getMessage());
                }
            }
            handleSQLException("Fejl ved hentning og fjernelse af studerende fra kø", e);
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
     * Tjekker om en student er i en specifik kø.
     *
     * @param studentViaId    Studentens VIA ID
     * @param performanceType Ydelsestypen køen er for
     * @return true hvis studenten er i køen
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean isStudentInQueue(int studentViaId, PerformanceTypeEnum performanceType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry WHERE student_via_id = ? AND performance_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentViaId);
            stmt.setString(2, performanceType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved tjek om student er i kø: " + studentViaId, e);
            throw e;
        }
        return false;
    }

    /**
     * Tjekker om en student er i en hvilken som helst kø.
     *
     * @param studentViaId Studentens VIA ID
     * @return true hvis studenten er i en kø
     * @throws SQLException hvis der er problemer med databasen
     */
    public boolean isStudentInAnyQueue(int studentViaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry WHERE student_via_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentViaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved tjek om student er i nogen kø: " + studentViaId, e);
            throw e;
        }
        return false;
    }

    /**
     * Henter størrelsen på en specifik kø.
     *
     * @param performanceType Ydelsestypen at tjekke for
     * @return Antallet af studerende i køen
     * @throws SQLException hvis der er problemer med databasen
     */
    public int getQueueSize(PerformanceTypeEnum performanceType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry WHERE performance_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, performanceType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af kø-størrelse for " + performanceType, e);
            throw e;
        }
        return 0;
    }

    /**
     * Henter det totale antal studerende i køer.
     *
     * @return Total antal studerende i alle køer
     * @throws SQLException hvis der er problemer med databasen
     */
    public int getTotalQueueSize() throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            handleSQLException("Fejl ved hentning af total kø-størrelse", e);
            throw e;
        }
        return 0;
    }

    /**
     * Tømmer en specifik kø helt.
     *
     * @param performanceType Ydelsestypen for køen der skal tømmes
     * @return Antal fjernede studerende
     * @throws SQLException hvis der er problemer med databasen
     */
    public int clearQueue(PerformanceTypeEnum performanceType) throws SQLException {
        // Først hent studerende i køen så vi kan sende events
        List<Student> studentsInQueue = getStudentsInQueue(performanceType);

        String sql = "DELETE FROM QueueEntry WHERE performance_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, performanceType.name());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                log.info("Ryddet " + performanceType.getClass().getSimpleName() + "-ydelses kø: " +
                        affectedRows + " studerende fjernet");

                // Post events for hver student
                for (Student student : studentsInQueue) {
                    eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                            student, performanceType, 0, false));
                }
            }

            return affectedRows;
        } catch (SQLException e) {
            handleSQLException("Fejl ved rydning af " + performanceType + " kø", e);
            throw e;
        }
    }

    /**
     * Tømmer alle køer.
     *
     * @return Antal fjernede studerende
     * @throws SQLException hvis der er problemer med databasen
     */
    public int clearAllQueues() throws SQLException {
        // Først hent studerende i køerne så vi kan sende events
        List<Student> highQueueStudents = getStudentsInQueue(PerformanceTypeEnum.HIGH);
        List<Student> lowQueueStudents = getStudentsInQueue(PerformanceTypeEnum.LOW);

        String sql = "DELETE FROM QueueEntry";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            int affectedRows = stmt.executeUpdate(sql);

            if (affectedRows > 0) {
                log.info("Ryddet alle køer: " + affectedRows + " studerende fjernet");

                // Post events for hver student i HIGH køen
                for (Student student : highQueueStudents) {
                    eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                            student, PerformanceTypeEnum.HIGH, 0, false));
                }

                // Post events for hver student i LOW køen
                for (Student student : lowQueueStudents) {
                    eventBus.post(new SystemEvents.StudentRemovedFromQueueEvent(
                            student, PerformanceTypeEnum.LOW, 0, false));
                }
            }

            return affectedRows;
        } catch (SQLException e) {
            handleSQLException("Fejl ved rydning af alle køer", e);
            throw e;
        }
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