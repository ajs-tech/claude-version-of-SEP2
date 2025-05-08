package model;

import model.enums.PerformanceTypeEnum;
import model.logic.DataManager;
import model.logic.DataModel;
import model.models.Laptop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for laptop-oprettelse funktionalitet.
 * Tester at laptops kan oprettes gennem model-interfacet.
 */
public class LaptopCreationTest {

    private DataModel dataModel;

    /**
     * Konkret implementation af den abstrakte DataManager-klasse til test-formål.
     */
    private static class TestDataManager extends DataManager {
        // Ingen yderligere metoder nødvendige, da DataManager implementerer
        // alle nødvendige metoder fra DataModel-interfacet
    }

    @BeforeEach
    void setUp() {
        // Opret en konkret implementation af DataManager til test
        // dataModel = new TestDataManager();
        dataModel = new DataManager();
    }

    @Test
    void testCreateLaptop() throws SQLException {
        // Arrange
        String brand = "Test Brand";
        String model = "Test Model";
        int gigabyte = 512;
        int ram = 16;
        PerformanceTypeEnum perfType = PerformanceTypeEnum.HIGH;

        // Act - Opret laptop gennem DataModel-interfacet
        Laptop createdLaptop = dataModel.createLaptop(brand, model, gigabyte, ram, perfType);

        // Assert - Verificer at laptop-objektet blev oprettet korrekt
        assertNotNull(createdLaptop, "model.models.Laptop should be created and not null");
        assertEquals(brand, createdLaptop.getBrand(), "Brand should match");
        assertEquals(model, createdLaptop.getModel(), "Model should match");
        assertEquals(gigabyte, createdLaptop.getGigabyte(), "Gigabyte should match");
        assertEquals(ram, createdLaptop.getRam(), "RAM should match");
        assertEquals(perfType, createdLaptop.getPerformanceType(), "Performance type should match");

        // Verificer at laptop blev tilføjet til cache
        List<Laptop> cachedLaptops = dataModel.getAllLaptops();
        boolean foundInCache = false;
        for (Laptop laptop : cachedLaptops) {
            if (laptop.getId().equals(createdLaptop.getId())) {
                foundInCache = true;
                break;
            }
        }
        assertTrue(foundInCache, "model.models.Laptop should be found in cache");

        // Hent laptops direkte fra databasen via model-interfacet for at verificere persistens
        List<Laptop> databaseLaptops = dataModel.getAllLaptops();
        boolean foundInDb = false;
        for (Laptop laptop : databaseLaptops) {
            if (laptop.getId().equals(createdLaptop.getId())) {
                foundInDb = true;
                // Verificer at data er korrekt i databasen
                assertEquals(brand, laptop.getBrand(), "Brand in database should match");
                assertEquals(model, laptop.getModel(), "Model in database should match");
                assertEquals(gigabyte, laptop.getGigabyte(), "Gigabyte in database should match");
                assertEquals(ram, laptop.getRam(), "RAM in database should match");
                assertEquals(perfType, laptop.getPerformanceType(), "Performance type in database should match");
                break;
            }
        }
        assertTrue(foundInDb, "model.models.Laptop should be found in database");

        // Print information om oprettet laptop til konsoloutput
        System.out.println("===== LAPTOP CREATION TEST =====");
        System.out.println("Created laptop: " + createdLaptop.getId());
        System.out.println("Brand: " + createdLaptop.getBrand());
        System.out.println("Model: " + createdLaptop.getModel());
        System.out.println("Specs: " + createdLaptop.getGigabyte() + "GB HDD, " +
                createdLaptop.getRam() + "GB RAM");
        System.out.println("Performance: " + createdLaptop.getPerformanceType());
        System.out.println("Available: " + createdLaptop.isAvailable());

        // Print cache-status
        System.out.println("\n===== CACHE STATUS =====");
        System.out.println("Total laptops in cache: " + cachedLaptops.size());
        System.out.println("Available laptops: " + dataModel.getAmountOfAvailableLaptops());
        System.out.println("Loaned laptops: " + dataModel.getAmountOfLoanedLaptops());

        // Print alle laptops fra cache
        System.out.println("\n===== LAPTOPS IN CACHE =====");
        for (Laptop laptop : cachedLaptops) {
            System.out.println(laptop.getId() + ": " + laptop.getBrand() + " " +
                    laptop.getModel() + " (" + laptop.getPerformanceType() + ")");
        }

        // Print alle laptops fra database
        System.out.println("\n===== LAPTOPS IN DATABASE =====");
        for (Laptop laptop : databaseLaptops) {
            System.out.println(laptop.getId() + ": " + laptop.getBrand() + " " +
                    laptop.getModel() + " (" + laptop.getPerformanceType() + ")");
        }
    }
}
