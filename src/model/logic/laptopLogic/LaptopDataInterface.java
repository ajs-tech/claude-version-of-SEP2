package model.logic.laptopLogic;

import model.enums.PerformanceTypeEnum;
import model.models.Laptop;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Interface der definerer operationer for Laptop data management.
 * Opdateret til MVVM-arkitektur med support for de nye metoder.
 */
public interface LaptopDataInterface {

    /**
     * Returnerer alle laptops i systemet.
     *
     * @return Liste af alle laptops
     */
    ArrayList<Laptop> getAllLaptops();

    /**
     * Returnerer antal tilgængelige laptops.
     *
     * @return Antal tilgængelige laptops
     */
    int getAmountOfAvailableLaptops();

    /**
     * Returnerer antal udlånte laptops.
     *
     * @return Antal udlånte laptops
     */
    int getAmountOfLoanedLaptops();

    /**
     * Returnerer antal laptops med en specifik tilstand.
     *
     * @param classSimpleName Navnet på tilstandsklassen (f.eks. "AvailableState")
     * @return Antal laptops med den tilstand
     */
    int getAmountOfLaptopsByState(String classSimpleName);

    /**
     * Finder en tilgængelig laptop med en specifik performance type.
     *
     * @param performanceTypeEnum Performance type at søge efter (HIGH/LOW)
     * @return En tilgængelig laptop eller null hvis ingen findes
     */
    Laptop findAvailableLaptop(PerformanceTypeEnum performanceTypeEnum);

    /**
     * Opretter en ny laptop.
     *
     * @param brand Brand/mærke
     * @param model Model
     * @param gigabyte Harddisk kapacitet i GB
     * @param ram RAM i GB
     * @param performanceType Performance type (HIGH/LOW)
     * @return Den oprettede laptop eller null ved fejl
     */
    Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType);

    /**
     * Finder en laptop baseret på ID.
     *
     * @param id Laptop UUID
     * @return Laptopen hvis fundet, ellers null
     */
    Laptop getLaptopById(UUID id);

    /**
     * Opdaterer en eksisterende laptop.
     *
     * @param laptop Laptop at opdatere
     * @return true hvis operationen lykkedes
     */
    boolean updateLaptop(Laptop laptop);

    /**
     * Sletter en laptop.
     *
     * @param id Laptop UUID
     * @return true hvis operationen lykkedes
     */
    boolean deleteLaptop(UUID id);
}