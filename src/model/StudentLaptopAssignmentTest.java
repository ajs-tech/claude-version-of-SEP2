package model;

import model.enums.PerformanceTypeEnum;
import model.logic.DataManager;
import model.logic.DataModel;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Test program der demonstrerer oprettelse af en student med høj performance need
 * og automatisk tildeling af en laptop.
 */
public class StudentLaptopAssignmentTest {

    public static void main(String[] args) {
        // Opret datamodel
        DataModel dataModel = new DataManager();

        System.out.println("======== SYSTEMSTATUS FØR STUDENT-OPRETTELSE ========");

        // Vis eksisterende laptops
        List<Laptop> existingLaptops = dataModel.getAllLaptops();
        System.out.println("Antal laptops i systemet: " + existingLaptops.size());

        System.out.println("\nDetaljer om tilgængelige laptops:");
        int availableHighPerf = 0;
        int availableLowPerf = 0;

        for (Laptop laptop : existingLaptops) {
            if (laptop.isAvailable()) {
                printLaptopInfo(laptop);

                if (laptop.getPerformanceType() == PerformanceTypeEnum.HIGH) {
                    availableHighPerf++;
                } else {
                    availableLowPerf++;
                }
            }
        }

        System.out.println("\nStatistik om laptops:");
        System.out.println("- Tilgængelige laptops total: " + dataModel.getAvailableLaptopCount());
        System.out.println("- Tilgængelige HIGH performance laptops: " + availableHighPerf);
        System.out.println("- Tilgængelige LOW performance laptops: " + availableLowPerf);
        System.out.println("- Udlånte laptops: " + dataModel.getLoanedLaptopCount());

        // Vis eksisterende studerende
        List<Student> existingStudents = dataModel.getAllStudents();
        System.out.println("\nAntal studerende i systemet: " + existingStudents.size());
        System.out.println("Antal studerende med laptop: " + dataModel.getStudentsWithLaptopCount());

        // Vis aktive reservationer
        List<Reservation> activeReservations = dataModel.getActiveReservations();
        System.out.println("\nAktive reservationer: " + activeReservations.size());

        for (Reservation reservation : activeReservations) {
            System.out.println("- " + reservation.getStudentDetailsString() +
                    " -> " + reservation.getLaptopDetailsString());
        }

        // Opret en ny student med HIGH performance need
        System.out.println("\n======== OPRETTER NY STUDENT MED HIGH PERFORMANCE NEED ========");
        System.out.println("
