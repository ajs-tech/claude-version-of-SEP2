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

/**
 * Test for studerende-oprettelse med automatisk laptop-tildeling.
 * Viser både scenariet hvor ledige laptops tildeles direkte,
 * og scenariet hvor studerende uden laptop ender i kø.
 */
public class StudentCreationTest {

    public static void main(String[] args) {
        // Opret datamodel
        DataModel dataModel = new DataManager();

        // ===== 1. VIS SYSTEM-STATUS FØR VI STARTER =====
        System.out.println("===== INITIAL SYSTEM STATUS =====");

        // Vis alle laptops
        List<Laptop> laptops = dataModel.getAllLaptops();
        System.out.println("Total laptops in system: " + laptops.size());

        int availableLaptopsCount = dataModel.getAmountOfAvailableLaptops();
        int loanedLaptopsCount = dataModel.getAmountOfLoanedLaptops();

        System.out.println("Available laptops: " + availableLaptopsCount);
        System.out.println("Loaned laptops: " + loanedLaptopsCount);

        // Detaljer om tilgængelige laptops
        System.out.println("\n----- AVAILABLE LAPTOPS DETAILS -----");
        for (Laptop laptop : laptops) {
            if (laptop.isAvailable()) {
                printLaptopInfo(laptop);
            }
        }

        // Vis alle studerende
        List<Student> students = dataModel.getAllStudents();
        System.out.println("\nTotal students in system: " + students.size());
        System.out.println("Students with laptops: " + dataModel.getCountOfWhoHasLaptop());

        // Detaljer om studerende med laptops
        System.out.println("\n----- STUDENTS WITH LAPTOPS -----");
        List<Student> studentsWithLaptops = dataModel.getThoseWhoHaveLaptop();
        for (Student student : studentsWithLaptops) {
            printStudentInfo(student);
        }

        // Vis aktive reservationer
        List<Reservation> reservations = dataModel.getAllActiveReservations();
        System.out.println("\nActive reservations: " + reservations.size());

        for (Reservation reservation : reservations) {
            System.out.println("Reservation: " + reservation.getReservationId());
            System.out.println("  Student: " + reservation.getStudent().getName() + " (VIA ID: " +
                    reservation.getStudent().getViaId() + ")");
            System.out.println("  Laptop: " + reservation.getLaptop().getBrand() + " " +
                    reservation.getLaptop().getModel());
            System.out.println("  Status: " + reservation.getStatus());
            System.out.println("----------------------------");
        }

        // Vis køer
        System.out.println("\nStudents in HIGH performance queue: " + dataModel.getHighNeedingQueueSize());
        System.out.println("Students in LOW performance queue: " + dataModel.getLowNeedingQueueSize());

        // ===== 2. OPRET FØRSTE STUDENT (MED HIGH PERFORMANCE NEED) =====
        // Denne burde få tildelt en ledig laptop automatisk, hvis der er en ledig
        System.out.println("\n\n===== CREATING FIRST STUDENT (HIGH PERFORMANCE NEED) =====");

        // Opret uddannelsesslutdato (3 år fra nu)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 3);
        Date degreeEndDate = calendar.getTime();

        // Opret student
        Student student1 = dataModel.createStudent(
                "Anders Andersen",
                degreeEndDate,
                "Datamatiker",
                123456,
                "anders@via.dk",
                12345678,
                PerformanceTypeEnum.HIGH
        );

        if (student1 != null) {
            System.out.println("Student created successfully: " + student1.getName());
            System.out.println("VIA ID: " + student1.getViaId());
            System.out.println("Performance need: " + student1.getPerformanceNeeded());
            System.out.println("Has laptop: " + student1.isHasLaptop());

            System.out.println("\n----- SYSTEM STATUS AFTER FIRST STUDENT CREATION -----");

            // Tjek om antallet af ledige laptops er reduceret med 1
            int newAvailableLaptopsCount = dataModel.getAmountOfAvailableLaptops();
            System.out.println("Available laptops: " + newAvailableLaptopsCount +
                    " (was " + availableLaptopsCount + ")");

            if (newAvailableLaptopsCount < availableLaptopsCount) {
                System.out.println("✓ Success: Available laptops reduced by " +
                        (availableLaptopsCount - newAvailableLaptopsCount));
            } else {
                System.out.println("✗ Error: Available laptops count not reduced");
            }

            // Tjek om antallet af udlånte laptops er øget med 1
            int newLoanedLaptopsCount = dataModel.getAmountOfLoanedLaptops();
            System.out.println("Loaned laptops: " + newLoanedLaptopsCount +
                    " (was " + loanedLaptopsCount + ")");

            if (newLoanedLaptopsCount > loanedLaptopsCount) {
                System.out.println("✓ Success: Loaned laptops increased by " +
                        (newLoanedLaptopsCount - loanedLaptopsCount));
            } else {
                System.out.println("✗ Error: Loaned laptops count not increased");
            }

            // Opdater tællere til næste test
            availableLaptopsCount = newAvailableLaptopsCount;
            loanedLaptopsCount = newLoanedLaptopsCount;

            // Vis aktive reservationer igen
            reservations = dataModel.getAllActiveReservations();
            System.out.println("\nActive reservations: " + reservations.size());

            for (Reservation reservation : reservations) {
                System.out.println("Reservation: " + reservation.getReservationId());
                System.out.println("  Student: " + reservation.getStudent().getName() + " (VIA ID: " +
                        reservation.getStudent().getViaId() + ")");
                System.out.println("  Laptop: " + reservation.getLaptop().getBrand() + " " +
                        reservation.getLaptop().getModel());
                System.out.println("  Status: " + reservation.getStatus());
                System.out.println("----------------------------");
            }
        } else {
            System.out.println("Failed to create student");
        }

        // ===== 3. OPRET ANDEN STUDENT (MED LOW PERFORMANCE NEED) =====
        // Hvis der ikke er flere ledige laptops, burde denne ende i LOW performance køen
        System.out.println("\n\n===== CREATING SECOND STUDENT (LOW PERFORMANCE NEED) =====");

        Student student2 = dataModel.createStudent(
                "Bente Bentsen",
                degreeEndDate,
                "Multimediedesigner",
                654321,
                "bente@via.dk",
                87654321,
                PerformanceTypeEnum.LOW
        );

        if (student2 != null) {
            System.out.println("Student created successfully: " + student2.getName());
            System.out.println("VIA ID: " + student2.getViaId());
            System.out.println("Performance need: " + student2.getPerformanceNeeded());
            System.out.println("Has laptop: " + student2.isHasLaptop());

            System.out.println("\n----- SYSTEM STATUS AFTER SECOND STUDENT CREATION -----");

            // Tjek om antallet af ledige laptops er uændret (hvis der ikke var flere ledige)
            int newAvailableLaptopsCount = dataModel.getAmountOfAvailableLaptops();
            System.out.println("Available laptops: " + newAvailableLaptopsCount +
                    " (was " + availableLaptopsCount + ")");

            // Tjek om antallet af studerende i LOW-ydelses køen er øget
            int lowQueueSize = dataModel.getLowNeedingQueueSize();
            System.out.println("Students in LOW performance queue: " + lowQueueSize);

            if (student2.isHasLaptop()) {
                System.out.println("Student got a laptop automatically (there was one available)");
            } else if (lowQueueSize > 0) {
                System.out.println("✓ Success: Student was added to LOW performance queue");
            } else {
                System.out.println("✗ Error: Student neither got a laptop nor was added to queue");
            }

            // Vis køer igen
            System.out.println("\nStudents in HIGH performance queue: " + dataModel.getHighNeedingQueueSize());
            System.out.println("Students in LOW performance queue: " + dataModel.getLowNeedingQueueSize());

            // Vis studerende i køerne
            if (dataModel.getLowNeedingQueueSize() > 0) {
                System.out.println("\n----- STUDENTS IN LOW PERFORMANCE QUEUE -----");
                List<Student> lowQueueStudents = dataModel.getStudentsInLowPerformanceQueue();
                for (Student student : lowQueueStudents) {
                    printStudentInfo(student);
                }
            }

            if (dataModel.getHighNeedingQueueSize() > 0) {
                System.out.println("\n----- STUDENTS IN HIGH PERFORMANCE QUEUE -----");
                List<Student> highQueueStudents = dataModel.getStudentsInHighPerformanceQueue();
                for (Student student : highQueueStudents) {
                    printStudentInfo(student);
                }
            }
        } else {
            System.out.println("Failed to create student");
        }

        // ===== 4. OPDATER ALLE STUDERENDE OG LAPTOPS FRA DATABASE =====
        // Vis den endelige systemstatus
        System.out.println("\n\n===== FINAL SYSTEM STATUS =====");
        dataModel.refreshCaches(); // Genindlæs fra databasen

        // Vis alle laptops igen
        laptops = dataModel.getAllLaptops();
        System.out.println("Total laptops in system: " + laptops.size());
        System.out.println("Available laptops: " + dataModel.getAmountOfAvailableLaptops());
        System.out.println("Loaned laptops: " + dataModel.getAmountOfLoanedLaptops());

        // Vis alle studerende igen
        students = dataModel.getAllStudents();
        System.out.println("\nTotal students in system: " + students.size());
        System.out.println("Students with laptops: " + dataModel.getCountOfWhoHasLaptop());

        // Vis alle studerende med detaljer
        System.out.println("\n----- ALL STUDENTS -----");
        for (Student student : students) {
            printStudentInfo(student);
        }

        // Vis alle reservationer igen
        reservations = dataModel.getAllActiveReservations();
        System.out.println("\nActive reservations: " + reservations.size());

        for (Reservation reservation : reservations) {
            System.out.println("Reservation: " + reservation.getReservationId());
            System.out.println("  Student: " + reservation.getStudent().getName() + " (VIA ID: " +
                    reservation.getStudent().getViaId() + ")");
            System.out.println("  Laptop: " + reservation.getLaptop().getBrand() + " " +
                    reservation.getLaptop().getModel());
            System.out.println("  Status: " + reservation.getStatus());
            System.out.println("----------------------------");
        }
    }

    /**
     * Hjælpemetode til at printe laptop-information
     */
    private static void printLaptopInfo(Laptop laptop) {
        System.out.println("Laptop ID: " + laptop.getId());
        System.out.println("  Brand/Model: " + laptop.getBrand() + " " + laptop.getModel());
        System.out.println("  Specs: " + laptop.getGigabyte() + "GB HDD, " + laptop.getRam() + "GB RAM");
        System.out.println("  Performance: " + laptop.getPerformanceType());
        System.out.println("  Available: " + laptop.isAvailable());
        System.out.println("  State: " + laptop.getStateClassName());
        System.out.println("----------------------------");
    }

    /**
     * Hjælpemetode til at printe student-information
     */
    private static void printStudentInfo(Student student) {
        System.out.println("Student VIA ID: " + student.getViaId());
        System.out.println("  Name: " + student.getName());
        System.out.println("  Email: " + student.getEmail());
        System.out.println("  Degree: " + student.getDegreeTitle());
        System.out.println("  Performance need: " + student.getPerformanceNeeded());
        System.out.println("  Has laptop: " + student.isHasLaptop());
        System.out.println("----------------------------");
    }
}