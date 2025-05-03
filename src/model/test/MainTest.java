package model.test;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.models.AvailableState;
import model.models.Laptop;
import model.models.LoanedState;
import model.models.Reservation;
import model.models.Student;
import model.util.EventBus;
import model.logic.reservationsLogic.GenericQueue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test class for the model layer without database dependencies.
 * Tests the core functionality of the model classes and their interactions.
 */
public class MainTest {

    public static void main(String[] args) {
        System.out.println("Starting Laptop Management System Model Tests");
        System.out.println("============================================");

        // Run all tests
        testStudentModel();
        testLaptopModel();
        testReservationModel();
        testGenericQueue();
        testLaptopStatePattern();
        testStudentLaptopInteraction();
        testCompleteReservationWorkflow();

        System.out.println("============================================");
        System.out.println("All tests completed successfully!");
    }

    /**
     * Test the Student model class functionality.
     */
    private static void testStudentModel() {
        System.out.println("\nTesting Student Model...");

        // Create test data
        String name = "John Doe";
        Date degreeEndDate = createFutureDate(2);
        String degreeTitle = "Computer Science";
        int viaId = 12345;
        String email = "john.doe@example.com";
        int phoneNumber = 12345678;
        PerformanceTypeEnum performanceNeeded = PerformanceTypeEnum.HIGH;

        // Create student
        Student student = new Student(name, degreeEndDate, degreeTitle, viaId,
                email, phoneNumber, performanceNeeded);

        // Verify initial state
        assert student.getName().equals(name) : "Student name does not match";
        assert student.getDegreeEndDate().equals(degreeEndDate) : "Student degree end date does not match";
        assert student.getDegreeTitle().equals(degreeTitle) : "Student degree title does not match";
        assert student.getViaId() == viaId : "Student VIA ID does not match";
        assert student.getEmail().equals(email) : "Student email does not match";
        assert student.getPhoneNumber() == phoneNumber : "Student phone number does not match";
        assert student.getPerformanceNeeded() == performanceNeeded : "Student performance needed does not match";
        assert !student.isHasLaptop() : "New student should not have a laptop";

        // Test property change events
        AtomicBoolean propertyChangeReceived = new AtomicBoolean(false);
        student.addPropertyChangeListener("hasLaptop", evt -> {
            propertyChangeReceived.set(true);
            assert (boolean)evt.getNewValue() : "New value should be true";
            assert !(boolean)evt.getOldValue() : "Old value should be false";
        });

        // Change student properties and check event
        student.setHasLaptop(true);
        assert student.isHasLaptop() : "hasLaptop should be true after change";
        assert propertyChangeReceived.get() : "Property change event was not received";

        System.out.println("Student Model tests passed!");
    }

    /**
     * Test the Laptop model class functionality.
     */
    private static void testLaptopModel() {
        System.out.println("\nTesting Laptop Model...");

        // Create test data
        String brand = "Dell";
        String model = "XPS 15";
        int gigabyte = 512;
        int ram = 16;
        PerformanceTypeEnum performanceType = PerformanceTypeEnum.HIGH;

        // Create laptop
        Laptop laptop = new Laptop(brand, model, gigabyte, ram, performanceType);

        // Verify initial state
        assert laptop.getBrand().equals(brand) : "Laptop brand does not match";
        assert laptop.getModel().equals(model) : "Laptop model does not match";
        assert laptop.getGigabyte() == gigabyte : "Laptop gigabyte does not match";
        assert laptop.getRam() == ram : "Laptop RAM does not match";
        assert laptop.getPerformanceType() == performanceType : "Laptop performance type does not match";
        assert laptop.isAvailable() : "New laptop should be available";
        assert !laptop.isLoaned() : "New laptop should not be loaned";
        assert laptop.getStateClassName().equals("AvailableState") : "New laptop should be in AvailableState";

        // Test state change and property change events
        AtomicBoolean stateChangeReceived = new AtomicBoolean(false);
        laptop.addPropertyChangeListener("state", evt -> {
            stateChangeReceived.set(true);
            assert evt.getOldValue() instanceof AvailableState : "Old state should be AvailableState";
            assert evt.getNewValue() instanceof LoanedState : "New state should be LoanedState";
        });

        // Change laptop state
        laptop.changeState(LoanedState.INSTANCE);
        assert !laptop.isAvailable() : "Laptop should not be available after state change";
        assert laptop.isLoaned() : "Laptop should be loaned after state change";
        assert laptop.getStateClassName().equals("LoanedState") : "Laptop should be in LoanedState after state change";
        assert stateChangeReceived.get() : "State change event was not received";

        System.out.println("Laptop Model tests passed!");
    }

    /**
     * Test the Reservation model class functionality.
     */
    private static void testReservationModel() {
        System.out.println("\nTesting Reservation Model...");

        // Create test data
        Student student = createTestStudent();
        Laptop laptop = createTestLaptop();

        // Create reservation
        Reservation reservation = new Reservation(student, laptop);

        // Verify initial state
        assert reservation.getStudent() == student : "Reservation student does not match";
        assert reservation.getLaptop() == laptop : "Reservation laptop does not match";
        assert reservation.getStatus() == ReservationStatusEnum.ACTIVE : "New reservation should be ACTIVE";
        assert !laptop.isAvailable() : "Laptop should not be available after reservation";
        assert laptop.isLoaned() : "Laptop should be loaned after reservation";
        assert student.isHasLaptop() : "Student should have laptop after reservation";

        // Test status change
        AtomicBoolean statusChangeReceived = new AtomicBoolean(false);
        reservation.addPropertyChangeListener("status", evt -> {
            statusChangeReceived.set(true);
            assert evt.getOldValue() == ReservationStatusEnum.ACTIVE : "Old status should be ACTIVE";
            assert evt.getNewValue() == ReservationStatusEnum.COMPLETED : "New status should be COMPLETED";
        });

        // Change reservation status
        reservation.changeStatus(ReservationStatusEnum.COMPLETED);
        assert reservation.getStatus() == ReservationStatusEnum.COMPLETED : "Reservation status should be COMPLETED after change";
        assert statusChangeReceived.get() : "Status change event was not received";
        assert laptop.isAvailable() : "Laptop should be available after reservation completion";
        assert !laptop.isLoaned() : "Laptop should not be loaned after reservation completion";
        assert !student.isHasLaptop() : "Student should not have laptop after reservation completion";

        System.out.println("Reservation Model tests passed!");
    }

    /**
     * Test the GenericQueue class functionality.
     */
    private static void testGenericQueue() {
        System.out.println("\nTesting GenericQueue...");

        // Create queue
        GenericQueue highPerformanceQueue = new GenericQueue(PerformanceTypeEnum.HIGH);

        // Create test students
        Student student1 = createTestStudent();
        Student student2 = createTestStudent();
        Student student3 = createTestStudent();

        // Test queue operations
        assert highPerformanceQueue.getQueueSize() == 0 : "New queue should be empty";

        AtomicInteger queueSizeChanges = new AtomicInteger(0);
        highPerformanceQueue.addPropertyChangeListener("queueSize", evt -> {
            queueSizeChanges.incrementAndGet();
        });

        // Add students to queue
        highPerformanceQueue.addToQueue(student1);
        assert highPerformanceQueue.getQueueSize() == 1 : "Queue size should be 1 after adding one student";
        assert queueSizeChanges.get() == 1 : "Queue size change event was not received";

        highPerformanceQueue.addToQueue(student2);
        highPerformanceQueue.addToQueue(student3);
        assert highPerformanceQueue.getQueueSize() == 3 : "Queue size should be 3 after adding three students";
        assert queueSizeChanges.get() == 3 : "Queue size change events were not properly received";

        // Test peek and remove
        Student firstStudent = highPerformanceQueue.peekNextInLine();
        assert firstStudent == student1 : "First student in queue should be student1";

        Student removedStudent = highPerformanceQueue.getAndRemoveNextInLine();
        assert removedStudent == student1 : "Removed student should be student1";
        assert highPerformanceQueue.getQueueSize() == 2 : "Queue size should be 2 after removing one student";
        assert queueSizeChanges.get() == 4 : "Queue size change event was not received for removal";

        // Test containsStudent and removeStudentById
        assert highPerformanceQueue.containsStudent(student2.getViaId()) : "Queue should contain student2";
        boolean removed = highPerformanceQueue.removeStudentById(student2.getViaId());
        assert removed : "Removal by ID should return true for existing student";
        assert !highPerformanceQueue.containsStudent(student2.getViaId()) : "Queue should not contain student2 after removal";
        assert highPerformanceQueue.getQueueSize() == 1 : "Queue size should be 1 after removing student2";

        System.out.println("GenericQueue tests passed!");
    }

    /**
     * Test the State Pattern implementation for Laptop.
     */
    private static void testLaptopStatePattern() {
        System.out.println("\nTesting Laptop State Pattern...");

        // Create laptop
        Laptop laptop = createTestLaptop();

        // Initial state is AvailableState
        assert laptop.isAvailable() : "New laptop should be available";
        assert !laptop.isLoaned() : "New laptop should not be loaned";

        // Test state transitions through click
        laptop.getState().click(laptop);
        assert !laptop.isAvailable() : "Laptop should not be available after click in AvailableState";
        assert laptop.isLoaned() : "Laptop should be loaned after click in AvailableState";

        laptop.getState().click(laptop);
        assert laptop.isAvailable() : "Laptop should be available after click in LoanedState";
        assert !laptop.isLoaned() : "Laptop should not be loaned after click in LoanedState";

        // Test direct state change
        laptop.changeState(LoanedState.INSTANCE);
        assert !laptop.isAvailable() : "Laptop should not be available after direct state change";
        assert laptop.isLoaned() : "Laptop should be loaned after direct state change";

        laptop.changeState(AvailableState.INSTANCE);
        assert laptop.isAvailable() : "Laptop should be available after direct state change";
        assert !laptop.isLoaned() : "Laptop should not be loaned after direct state change";

        System.out.println("Laptop State Pattern tests passed!");
    }

    /**
     * Test the interaction between Student and Laptop models.
     */
    private static void testStudentLaptopInteraction() {
        System.out.println("\nTesting Student-Laptop Interaction...");

        // Create test data
        Student student = createTestStudent();
        Laptop laptop = createTestLaptop();

        // Verify initial state
        assert !student.isHasLaptop() : "New student should not have a laptop";
        assert laptop.isAvailable() : "New laptop should be available";

        // Create reservation to establish relationship
        Reservation reservation = new Reservation(student, laptop);

        // Verify state after reservation
        assert student.isHasLaptop() : "Student should have laptop after reservation";
        assert !laptop.isAvailable() : "Laptop should not be available after reservation";
        assert laptop.isLoaned() : "Laptop should be loaned after reservation";

        // Complete reservation
        reservation.changeStatus(ReservationStatusEnum.COMPLETED);

        // Verify state after completion
        assert !student.isHasLaptop() : "Student should not have laptop after reservation completion";
        assert laptop.isAvailable() : "Laptop should be available after reservation completion";
        assert !laptop.isLoaned() : "Laptop should not be loaned after reservation completion";

        System.out.println("Student-Laptop Interaction tests passed!");
    }

    /**
     * Test a complete reservation workflow.
     */
    private static void testCompleteReservationWorkflow() {
        System.out.println("\nTesting Complete Reservation Workflow...");

        // Create test data
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            students.add(createTestStudent());
        }

        List<Laptop> laptops = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            laptops.add(createTestLaptop());
        }

        // Create queues
        GenericQueue highQueue = new GenericQueue(PerformanceTypeEnum.HIGH);

        // Add students to queue
        for (Student student : students) {
            highQueue.addToQueue(student);
        }

        assert highQueue.getQueueSize() == 5 : "Queue should contain 5 students";

        // Allocate laptops to first 3 students (manual simulation of what ReservationManager would do)
        List<Reservation> reservations = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Student student = highQueue.getAndRemoveNextInLine();
            Laptop laptop = laptops.get(i);
            reservations.add(new Reservation(student, laptop));
        }

        assert highQueue.getQueueSize() == 2 : "Queue should have 2 students left";
        assert reservations.size() == 3 : "Should have created 3 reservations";

        // Verify all first 3 students got laptops
        for (int i = 0; i < 3; i++) {
            assert students.get(i).isHasLaptop() : "Student " + i + " should have a laptop";
            assert !laptops.get(i).isAvailable() : "Laptop " + i + " should not be available";
        }

        // Return a laptop (complete a reservation)
        reservations.get(0).changeStatus(ReservationStatusEnum.COMPLETED);
        assert !students.get(0).isHasLaptop() : "Student 0 should not have a laptop after return";
        assert laptops.get(0).isAvailable() : "Laptop 0 should be available after return";

        // Assign the returned laptop to the next student in queue (manual simulation)
        Student nextStudent = highQueue.getAndRemoveNextInLine();
        reservations.add(new Reservation(nextStudent, laptops.get(0)));

        assert nextStudent.isHasLaptop() : "Next student should have a laptop";
        assert !laptops.get(0).isAvailable() : "Laptop 0 should not be available after reallocation";
        assert highQueue.getQueueSize() == 1 : "Queue should have 1 student left";

        System.out.println("Complete Reservation Workflow tests passed!");
    }

    /**
     * Creates a test Student object with unique ID.
     * @return A new Student object
     */
    private static Student createTestStudent() {
        int id = (int)(Math.random() * 10000) + 10000;
        return new Student("Student " + id,
                createFutureDate(2),
                "Test Degree",
                id,
                "student" + id + "@example.com",
                12345678,
                PerformanceTypeEnum.HIGH);
    }

    /**
     * Creates a test Laptop object with unique ID.
     * @return A new Laptop object
     */
    private static Laptop createTestLaptop() {
        UUID id = UUID.randomUUID();
        return new Laptop("Test Brand",
                "Model " + id.toString().substring(0, 8),
                512,
                16,
                PerformanceTypeEnum.HIGH);
    }

    /**
     * Creates a date in the future.
     * @param years Years in the future
     * @return A future date
     */
    private static Date createFutureDate(int years) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, years);
        return cal.getTime();
    }
}