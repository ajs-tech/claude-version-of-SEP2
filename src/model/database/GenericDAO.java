package model.database;

import java.sql.SQLException;
import java.util.List;

/**
 * Generisk interface til Data Access Objects.
 * Definerer standardoperationer for alle DAO-klasser.
 *
 * @param <T> Entitetstypen
 * @param <ID> ID-typen for entiteten
 */
public interface GenericDAO<T, ID> {

    /**
     * Henter alle entiteter af denne type fra databasen.
     *
     * @return Liste af entiteter
     * @throws SQLException Hvis der opstår en database fejl
     */
    List<T> getAll() throws SQLException;

    /**
     * Henter en specifik entitet baseret på ID.
     *
     * @param id Entitetens ID
     * @return Entiteten hvis fundet, ellers null
     * @throws SQLException Hvis der opstår en database fejl
     */
    T getById(ID id) throws SQLException;

    /**
     * Indsætter en ny entitet i databasen.
     *
     * @param entity Entiteten der skal indsættes
     * @return true hvis operationen lykkedes
     * @throws SQLException Hvis der opstår en database fejl
     */
    boolean insert(T entity) throws SQLException;

    /**
     * Opdaterer en eksisterende entitet i databasen.
     *
     * @param entity Entiteten der skal opdateres
     * @return true hvis operationen lykkedes
     * @throws SQLException Hvis der opstår en database fejl
     */
    boolean update(T entity) throws SQLException;

    /**
     * Sletter en entitet fra databasen baseret på ID.
     *
     * @param id Entitetens ID
     * @return true hvis operationen lykkedes
     * @throws SQLException Hvis der opstår en database fejl
     */
    boolean delete(ID id) throws SQLException;

    /**
     * Tæller antal entiteter i databasen.
     *
     * @return Antal entiteter
     * @throws SQLException Hvis der opstår en database fejl
     */
    int count() throws SQLException;

    /**
     * Tjekker om en entitet eksisterer i databasen.
     *
     * @param id Entitetens ID
     * @return true hvis entiteten findes
     * @throws SQLException Hvis der opstår en database fejl
     */
    boolean exists(ID id) throws SQLException;
}