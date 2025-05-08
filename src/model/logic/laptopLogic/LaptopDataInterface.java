package model.logic.laptopLogic;

import model.enums.PerformanceTypeEnum;
import model.models.Laptop;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Interface that defines operations for Laptop data management.
 * Updated to support MVVM architecture.
 */
public interface LaptopDataInterface {

    /**
     * Returns all laptops in the system.
     *
     * @return List of all laptops
     */
    ArrayList<Laptop> getAllLaptops();

    /**
     * Returns the number of available laptops.
     *
     * @return Number of available laptops
     */
    int getAmountOfAvailableLaptops();

    /**
     * Returns the number of loaned laptops.
     *
     * @return Number of loaned laptops
     */
    int getAmountOfLoanedLaptops();

    /**
     * Returns the number of laptops with a specific state.
     *
     * @param classSimpleName The state class name (e.g., "AvailableState")
     * @return Number of laptops with the state
     */
    int getAmountOfLaptopsByState(String classSimpleName);

    /**
     * Finds an available laptop with a specific performance type.
     *
     * @param performanceTypeEnum Performance type to search for (HIGH/LOW)
     * @return An available laptop or null if none found
     */
    Laptop findAvailableLaptop(PerformanceTypeEnum performanceTypeEnum);

    /**
     * Creates a new laptop.
     *
     * @param brand Brand/manufacturer
     * @param model Model
     * @param gigabyte Hard disk capacity in GB
     * @param ram RAM in GB
     * @param performanceType Performance type (HIGH/LOW)
     * @return The created laptop or null on error
     */
    Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType);

    /**
     * Finds a laptop based on ID.
     *
     * @param id Laptop UUID
     * @return The laptop if found, otherwise null
     */
    Laptop getLaptopById(UUID id);

    /**
     * Updates an existing laptop.
     *
     * @param laptop Laptop to update
     * @return true if the operation was successful
     */
    boolean updateLaptop(Laptop laptop);

    /**
     * Deletes a laptop.
     *
     * @param id Laptop UUID
     * @return true if the operation was successful
     */
    boolean deleteLaptop(UUID id);
}