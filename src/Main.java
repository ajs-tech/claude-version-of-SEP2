

import model.database.DatabaseConnection;
import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.models.Laptop;
import network.NetworkService;
import viewmodel.LaptopViewModel;
import viewmodel.ReservationViewModel;
import viewmodel.StudentViewModel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main system application that demonstrates the laptop reservation system.
 * Integrates all components and demonstrates MVVM architecture in action.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final Log log = Log.getInstance();
    
    // MVVM ViewModels
    private static StudentViewModel studentViewModel;
    private static LaptopViewModel laptopViewModel;
    private static ReservationViewModel reservationViewModel;
    
    // Network services
    private static NetworkService networkService;
    private static final int PORT = 8080;
    
    // Thread management
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
    
    // Demo data parameters
    private static final int DEMO_HIGH_PERF_LAPTOPS = 3;
    private static final int DEMO_LOW_PERF_LAPTOPS = 3;
    private static final int DEMO_HIGH_PERF_STUDENTS = 3;
    private static final int DEMO_LOW_PERF_STUDENTS = 3;
    
    /**
     * Main entry point of the application.
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.log(Level.SEVERE, "Uncaught exception in thread: " + thread.getName(), throwable);
            log.critical("Uncaught exception in thread: " + thread.getName() + " - " + throwable.getMessage());
        });
        
        log.info("System starting...");
        
        try {
            // Setup shutdown hook for clean exit
            setupShutdownHook();
            
            // Initialize system components
            initializeSystem();
            
            // Run demonstration flow
            runDemonstrationFlow();
            
            // Wait for demonstration to complete or for system to be shut down
            waitForCompletion();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in main system execution: " + e.getMessage(), e);
            log.critical("Critical error in system execution: " + e.getMessage());
        } finally {
            // Ensure resources are released
            performCleanup();
        }
        
        log.info("System exited successfully.");
    }
    
    /**
     * Sets up a shutdown hook for clean exit when the JVM is terminated.
     */
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered - cleaning up resources...");
            performCleanup();
            log.info("Cleanup complete. Exiting...");
        }));
    }
    
    /**
     * Initializes all system components.
     */
    private static void initializeSystem() {
        log.info("Initializing system components...");
        
        try {
            // Test database connection
            if (!DatabaseConnection.getInstance().testConnection()) {
                log.critical("Cannot connect to database. Check connection settings.");
                throw new RuntimeException("Database connection failed");
            }
            log.info("Database connection successful");
            
            // Initialize ViewModels
            studentViewModel = new StudentViewModel();
            laptopViewModel = new LaptopViewModel();
            reservationViewModel = new ReservationViewModel();
            log.info("ViewModels initialized");
            
            // Initialize and start network service
            initializeNetworkService();
            
            // Add observers to display events
            addSystemObservers();
            
            // Check if we need to create demo data
            ensureDemoDataExists();
            
            // Show initial state
            displaySystemState("INITIAL");
            
            log.info("System initialization complete");
            printSeparator();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during system initialization: " + e.getMessage(), e);
            log.critical("Error during system initialization: " + e.getMessage());
            throw new RuntimeException("Failed to initialize system", e);
        }
    }
    
    /**
     * Initializes and starts the network service.
     */
    private static void initializeNetworkService() {
        try {
            networkService = NetworkService.getInstance(PORT);
            networkService.startServer();
            String hostname = InetAddress.getLocalHost().getHostName();
            log.info("Network service started on " + hostname + ":" + PORT);
            System.out.println("Network service listening on " + hostname + ":" + PORT);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not start network service: " + e.getMessage(), e);
            log.warning("Network service could not be started: " + e.getMessage());
            System.out.println("Warning: Network service unavailable. System will run without network capabilities.");
        }
    }
    
    /**
     * Adds observers to system components to display important events.
     */
    private static void addSystemObservers() {
        // Log observer for reservation events
        reservationViewModel.addObserver((observable, arg) -> {
            if (arg instanceof ReservationViewModel.ViewModelEvent) {
                ReservationViewModel.ViewModelEvent event = (ReservationViewModel.ViewModelEvent) arg;
                log.info("Reservation event: " + event.getEventType() + 
                        (event.getData() != null ? " - " + event.getData() : ""));
            }
        });
        
        // Log observer for network events
        if (networkService != null) {
            networkService.addObserver((observable, arg) -> {
                if (arg instanceof NetworkService.NetworkEvent) {
                    NetworkService.NetworkEvent event = (NetworkService.NetworkEvent) arg;
                    log.info("Network event: " + event.getEventType() + " - " + event.getData());
                }
            });
        }
    }
    
    /**
     * Creates demo data if the system has no data.
     */
    private static void ensureDemoDataExists() throws InterruptedException {
        // Wait a bit for initial data to load
        Thread.sleep(1000);
        
        // Check if we need to create laptops
        if (laptopViewModel.getAllLaptops().isEmpty()) {
            createDemoLaptops();
        }
        
        // Check if we need to create students
        if (studentViewModel.getAllStudents().isEmpty()) {
            createDemoStudents();
        }
        
        // Allow time for database operations to complete
        Thread.sleep(1000);
    }
    
    /**
     * Creates demo laptops with different performance levels.
     */
    private static void createDemoLaptops() {
        System.out.println("Creating demo laptops...");
        log.info("Creating demo laptops");
        
        // High performance laptops
        for (int i = 1; i <= DEMO_HIGH_PERF_LAPTOPS; i++) {
            String brand = getRandomBrand();
            String model = "Pro " + (i * 1000);
            int storage = 512 + (i * 256);
            int ram = 16 + (i * 8);
            
            laptopViewModel.createLaptop(brand, model, storage, ram, PerformanceTypeEnum.HIGH);
            log.info("Created high-performance laptop: " + brand + " " + model);
        }
        
        // Low performance laptops
        for (int i = 1; i <= DEMO_LOW_PERF_LAPTOPS; i++) {
            String brand = getRandomBrand();
            String model = "Basic " + (i * 100);
            int storage = 128 + (i * 128);
            int ram = 4 + (i * 2);
            
            laptopViewModel.createLaptop(brand, model, storage, ram, PerformanceTypeEnum.LOW);
            log.info("Created low-performance laptop: " + brand + " " + model);
        }
        
        System.out.println("Created " + (DEMO_HIGH_PERF_LAPTOPS + DEMO_LOW_PERF_LAPTOPS) + " demo laptops");
    }
    
    /**
     * Gets a random laptop brand for demo data.
     */
    private static String getRandomBrand() {
        String[] brands = {"Dell", "HP", "Lenovo", "Asus", "Acer", "Microsoft", "Apple"};
        int index = new Random().nextInt(brands.length);
        return brands[index];
    }
    
    /**
     * Creates demo students with different performance needs.
     */
    private static void createDemoStudents() {
        System.out.println("Creating demo students...");
        log.info("Creating demo students");
        
        // Degree end date - 3 years from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 3);
        Date degreeEndDate = calendar.getTime();
        
        // High performance need students
        for (int i = 1; i <= DEMO_HIGH_PERF_STUDENTS; i++) {
            String[] nameData = getRandomName();
            String name = nameData[0] + " " + nameData[1];
            String degreeTitle = getRandomHighPerfDegree();
            int viaId = 100000 + i;
            String email = nameData[0].toLowerCase() + "." + nameData[1].toLowerCase() + "@example.com";
            int phoneNumber = 10000000 + (i * 1111);
            
            try {
                Student student = new Student(name, degreeEndDate, degreeTitle, 
                        viaId, email, phoneNumber, PerformanceTypeEnum.HIGH);
                studentViewModel.createStudent(student);
                log.info("Created high-performance need student: " + name);
            } catch (IllegalArgumentException e) {
                log.warning("Could not create student: " + e.getMessage());
            }
        }
        
        // Low performance need students
        for (int i = 1; i <= DEMO_LOW_PERF_STUDENTS; i++) {
            String[] nameData = getRandomName();
            String name = nameData[0] + " " + nameData[1];
            String degreeTitle = getRandomLowPerfDegree();
            int viaId = 200000 + i;
            String email = nameData[0].toLowerCase() + "." + nameData[1].toLowerCase() + "@example.com";
            int phoneNumber = 20000000 + (i * 1111);
            
            try {
                Student student = new Student(name, degreeEndDate, degreeTitle, 
                        viaId, email, phoneNumber, PerformanceTypeEnum.LOW);
                studentViewModel.createStudent(student);
                log.info("Created low-performance need student: " + name);
            } catch (IllegalArgumentException e) {
                log.warning("Could not create student: " + e.getMessage());
            }
        }
        
        System.out.println("Created " + (DEMO_HIGH_PERF_STUDENTS + DEMO_LOW_PERF_STUDENTS) + " demo students");
    }
    
    /**
     * Gets a random name for demo data.
     */
    private static String[] getRandomName() {
        String[] firstNames = {"Anna", "Peter", "Maria", "Lars", "Sofie", "Thomas", "Emma", "Henrik", "Ida", "Christian"};
        String[] lastNames = {"Jensen", "Nielsen", "Hansen", "Pedersen", "Andersen", "Christensen", "Larsen", "Sørensen", "Rasmussen", "Jørgensen"};
        
        int firstIndex = new Random().nextInt(firstNames.length);
        int lastIndex = new Random().nextInt(lastNames.length);
        
        return new String[] {firstNames[firstIndex], lastNames[lastIndex]};
    }
    
    /**
     * Gets a random high-performance degree for demo data.
     */
    private static String getRandomHighPerfDegree() {
        String[] degrees = {
            "Computer Science", 
            "Software Engineering", 
            "Game Development", 
            "Data Science", 
            "Artificial Intelligence", 
            "Multimedia Design", 
            "3D Animation"
        };
        
        int index = new Random().nextInt(degrees.length);
        return degrees[index];
    }
    
    /**
     * Gets a random low-performance degree for demo data.
     */
    private static String getRandomLowPerfDegree() {
        String[] degrees = {
            "Business Administration", 
            "Marketing", 
            "Communication", 
            "Human Resources", 
            "Public Administration", 
            "International Business", 
            "Tourism Management"
        };
        
        int index = new Random().nextInt(degrees.length);
        return degrees[index];
    }
    
    /**
     * Runs the demonstration workflow.
     */
    private static void runDemonstrationFlow() {
        executor.submit(() -> {
            try {
                log.info("Starting demonstration workflow");
                
                // DEMO 1: Create a reservation
                demonstrateReservationCreation();
                Thread.sleep(2000);
                
                // DEMO 2: Add student to queue
                demonstrateQueueManagement();
                Thread.sleep(2000);
                
                // DEMO 3: Change reservation status
                demonstrateReservationStatusChange();
                Thread.sleep(2000);
                
                // DEMO 4: Network communication
                demonstrateNetworkCommunication();
                Thread.sleep(2000);
                
                // Show final system state
                displaySystemState("FINAL");
                
                log.info("Demonstration workflow completed successfully");
                
                // Signal completion
                shutdownLatch.countDown();
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in demonstration workflow: " + e.getMessage(), e);
                log.error("Error in demonstration workflow: " + e.getMessage());
                shutdownLatch.countDown();
            }
        });
    }
    
    /**
     * Demonstrates creating a reservation.
     */
    private static void demonstrateReservationCreation() {
        printSectionHeader("DEMONSTRATING RESERVATION CREATION");
        log.info("Demonstrating reservation creation");
        
        try {
            // Find an available laptop
            Laptop availableLaptop = findAvailableLaptop();
            
            // Find a student without a laptop
            Student studentWithoutLaptop = findStudentWithoutLaptop();
            
            if (availableLaptop != null && studentWithoutLaptop != null) {
                System.out.println("Creating reservation for:");
                System.out.println("  Student: " + studentWithoutLaptop.getName() + 
                        " (VIA ID: " + studentWithoutLaptop.getViaId() + ")");
                System.out.println("  model.models.Laptop: " + availableLaptop.getBrand() + " " +
                        availableLaptop.getModel() + " (" + availableLaptop.getPerformanceType() + ")");
                
                boolean success = reservationViewModel.createReservation(availableLaptop, studentWithoutLaptop);
                
                if (success) {
                    System.out.println("✓ Reservation created successfully!");
                    log.info("Demonstration: Reservation created successfully");
                } else {
                    System.out.println("✗ Failed to create reservation.");
                    log.warning("Demonstration: Failed to create reservation");
                }
            } else {
                System.out.println("Cannot demonstrate reservation creation:");
                if (availableLaptop == null) {
                    System.out.println("✗ No available laptops found");
                    log.warning("Demonstration: No available laptops for reservation demo");
                }
                if (studentWithoutLaptop == null) {
                    System.out.println("✗ No students without laptops found");
                    log.warning("Demonstration: No students without laptops for reservation demo");
                }
            }
        } catch (Exception e) {
            System.out.println("✗ Error during reservation creation: " + e.getMessage());
            log.error("Demonstration error: " + e.getMessage());
        }
        
        printSectionFooter();
    }
    
    /**
     * Finds an available laptop for demo.
     */
    private static Laptop findAvailableLaptop() {
        List<Laptop> laptops = laptopViewModel.getAllLaptops();
        for (Laptop laptop : laptops) {
            if (laptop.isAvailable()) {
                return laptop;
            }
        }
        return null;
    }
    
    /**
     * Finds a student without a laptop for demo.
     */
    private static Student findStudentWithoutLaptop() {
        List<Student> students = studentViewModel.getAllStudents();
        for (Student student : students) {
            if (!student.isHasLaptop()) {
                return student;
            }
        }
        return null;
    }
    
    /**
     * Demonstrates queue management.
     */
    private static void demonstrateQueueManagement() {
        printSectionHeader("DEMONSTRATING QUEUE MANAGEMENT");
        log.info("Demonstrating queue management");
        
        try {
            // Find a student without a laptop who isn't in a queue
            Student studentForQueue = findStudentForQueue();
            
            if (studentForQueue != null) {
                System.out.println("Adding student to queue:");
                System.out.println("  Student: " + studentForQueue.getName() + 
                        " (VIA ID: " + studentForQueue.getViaId() + ")");
                System.out.println("  Performance needs: " + studentForQueue.getPerformanceNeeded());
                
                if (studentForQueue.getPerformanceNeeded() == PerformanceTypeEnum.HIGH) {
                    reservationViewModel.addToHighPerformanceQueue(studentForQueue);
                    System.out.println("✓ Student added to high-performance queue");
                    log.info("Demonstration: Student added to high-performance queue");
                } else {
                    reservationViewModel.addToLowPerformanceQueue(studentForQueue);
                    System.out.println("✓ Student added to low-performance queue");
                    log.info("Demonstration: Student added to low-performance queue");
                }
                
                // Display updated queue sizes
                System.out.println("\nUpdated queue sizes:");
                System.out.println("  High-performance queue: " + reservationViewModel.getHighPerformanceQueueSize());
                System.out.println("  Low-performance queue: " + reservationViewModel.getLowPerformanceQueueSize());
                
            } else {
                System.out.println("✗ Cannot demonstrate queue management:");
                System.out.println("  No eligible students found for queue demonstration");
                log.warning("Demonstration: No students available for queue demo");
            }
        } catch (Exception e) {
            System.out.println("✗ Error during queue management: " + e.getMessage());
            log.error("Demonstration error: " + e.getMessage());
        }
        
        printSectionFooter();
    }
    
    /**
     * Finds a student who can be added to a queue for demo.
     */
    private static Student findStudentForQueue() {
        List<Student> students = studentViewModel.getAllStudents();
        List<Student> highQueueStudents = reservationViewModel.getStudentsInHighPerformanceQueue();
        List<Student> lowQueueStudents = reservationViewModel.getStudentsInLowPerformanceQueue();
        
        for (Student student : students) {
            if (!student.isHasLaptop() && 
                    !highQueueStudents.contains(student) && 
                    !lowQueueStudents.contains(student)) {
                return student;
            }
        }
        return null;
    }
    
    /**
     * Demonstrates changing a reservation status.
     */
    private static void demonstrateReservationStatusChange() {
        printSectionHeader("DEMONSTRATING RESERVATION STATUS CHANGE");
        log.info("Demonstrating reservation status change");
        
        try {
            // Get active reservations
            List<Reservation> reservations = reservationViewModel.getActiveReservations();
            
            if (!reservations.isEmpty()) {
                // Pick the first reservation for demo
                Reservation reservation = reservations.get(0);
                
                System.out.println("Changing status of reservation:");
                System.out.println("  ID: " + reservation.getReservationId());
                System.out.println("  Student: " + reservation.getStudent().getName());
                System.out.println("  model.models.Laptop: " + reservation.getLaptop().getBrand() + " " +
                        reservation.getLaptop().getModel());
                System.out.println("  Current status: " + reservation.getStatus());
                System.out.println("  New status: " + ReservationStatusEnum.COMPLETED);
                
                // Change status
                reservationViewModel.updateReservationStatus(
                        reservation.getReservationId(), ReservationStatusEnum.COMPLETED);
                
                System.out.println("✓ Reservation status change initiated");
                log.info("Demonstration: Reservation status change initiated");
                
                // Give time for the status change to be processed
                Thread.sleep(500);
                
                // Show updated reservations
                System.out.println("\nUpdated active reservations: " + 
                        reservationViewModel.getActiveReservationsCount());
                
            } else {
                System.out.println("✗ Cannot demonstrate reservation status change:");
                System.out.println("  No active reservations available");
                log.warning("Demonstration: No active reservations for status change demo");
            }
        } catch (Exception e) {
            System.out.println("✗ Error during reservation status change: " + e.getMessage());
            log.error("Demonstration error: " + e.getMessage());
        }
        
        printSectionFooter();
    }
    
    /**
     * Demonstrates network communication.
     */
    private static void demonstrateNetworkCommunication() {
        printSectionHeader("DEMONSTRATING NETWORK COMMUNICATION");
        log.info("Demonstrating network communication");
        
        if (networkService == null) {
            System.out.println("✗ Network service is not available. Skipping demonstration.");
            log.warning("Network service not available for demonstration");
            printSectionFooter();
            return;
        }
        
        // Process client requests in server
        networkService.addObserver((observable, arg) -> {
            if (arg instanceof NetworkService.MessageEvent) {
                NetworkService.MessageEvent event = (NetworkService.MessageEvent) arg;
                if (event.getMessage().startsWith("REQUEST:")) {
                    // Process request
                    String clientId = event.getSender();
                    String responseMessage = processClientRequest(event.getMessage());
                    
                    // Send response back to client
                    networkService.sendMessage(clientId, responseMessage);
                }
            }
        });
        
        // Create client thread
        Thread clientThread = new Thread(() -> {
            try {
                System.out.println("Starting client...");
                
                // Create client and connect to server
                NetworkService.Client client = new NetworkService.Client("localhost", PORT);
                
                if (client.connect()) {
                    System.out.println("✓ Client connected to server successfully");
                    
                    // Register client to handle responses
                    client.addObserver((observable, arg) -> {
                        if (arg instanceof NetworkService.MessageEvent) {
                            NetworkService.MessageEvent event = (NetworkService.MessageEvent) arg;
                            System.out.println("← Client received: " + event.getMessage());
                        }
                    });
                    
                    // Send requests
                    sendClientRequest(client, "REQUEST:SYSTEM_STATUS");
                    Thread.sleep(500);
                    
                    sendClientRequest(client, "REQUEST:AVAILABLE_LAPTOPS");
                    Thread.sleep(500);
                    
                    sendClientRequest(client, "REQUEST:QUEUE_STATUS");
                    Thread.sleep(500);
                    
                    // Disconnect
                    client.close();
                    System.out.println("✓ Client disconnected from server");
                    
                } else {
                    System.out.println("✗ Client could not connect to server");
                    log.warning("Demonstration: Client connection failed");
                }
                
            } catch (Exception e) {
                System.out.println("✗ Error in client: " + e.getMessage());
                log.error("Demonstration error in client: " + e.getMessage());
            }
        });
        
        try {
            // Start client and wait for completion
            clientThread.start();
            clientThread.join(5000);
            
            // Send broadcast to all clients
            if (networkService.getClientCount() > 0) {
                String broadcastMessage = "BROADCAST:System maintenance scheduled for tonight at 22:00";
                networkService.broadcast(broadcastMessage);
                System.out.println("↔ Server broadcast: " + broadcastMessage);
                log.info("Demonstration: Server broadcast sent");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error during network communication: " + e.getMessage());
            log.error("Demonstration error: " + e.getMessage());
        }
        
        printSectionFooter();
    }
    
    /**
     * Sends a client request with logging.
     */
    private static void sendClientRequest(NetworkService.Client client, String message) {
        System.out.println("→ Client sending: " + message);
        if (client.sendMessage(message)) {
            log.info("Demonstration: Client sent request: " + message);
        } else {
            log.warning("Demonstration: Failed to send client request: " + message);
        }
    }
    
    /**
     * Processes a client request and generates a response.
     */
    private static String processClientRequest(String requestMessage) {
        log.info("Processing client request: " + requestMessage);
        
        switch (requestMessage) {
            case "REQUEST:SYSTEM_STATUS":
                return "RESPONSE:System status - Active reservations: " + 
                        reservationViewModel.getActiveReservationsCount();
                
            case "REQUEST:AVAILABLE_LAPTOPS":
                return "RESPONSE:Available laptops: " + 
                        laptopViewModel.getAvailableLaptopsCount();
                
            case "REQUEST:QUEUE_STATUS":
                return "RESPONSE:Queue status - High: " + 
                        reservationViewModel.getHighPerformanceQueueSize() + 
                        ", Low: " + reservationViewModel.getLowPerformanceQueueSize();
                
            default:
                return "RESPONSE:Unknown request";
        }
    }
    
    /**
     * Displays the current system state.
     */
    private static void displaySystemState(String stateName) {
        printSectionHeader("SYSTEM STATE: " + stateName);
        
        // Get current data
        List<Laptop> laptops = laptopViewModel.getAllLaptops();
        List<Student> students = studentViewModel.getAllStudents();
        List<Reservation> reservations = reservationViewModel.getActiveReservations();
        
        // Display counts
        System.out.println("Laptops:");
        System.out.println("  Total: " + laptops.size());
        System.out.println("  Available: " + laptopViewModel.getAvailableLaptopsCount());
        System.out.println("  Loaned: " + laptopViewModel.getLoanedLaptopsCount());
        
        System.out.println("\nStudents:");
        System.out.println("  Total: " + students.size());
        System.out.println("  With laptops: " + studentViewModel.getCountOfStudentsWithLaptop());
        System.out.println("  High performance needs: " + studentViewModel.getCountOfStudentsWithHighPerformanceNeeds());
        System.out.println("  Low performance needs: " + studentViewModel.getCountOfStudentsWithLowPerformanceNeeds());
        
        System.out.println("\nQueues:");
        System.out.println("  High performance queue: " + reservationViewModel.getHighPerformanceQueueSize());
        System.out.println("  Low performance queue: " + reservationViewModel.getLowPerformanceQueueSize());
        
        System.out.println("\nReservations:");
        System.out.println("  Active reservations: " + reservations.size());
        
        if (!reservations.isEmpty()) {
            System.out.println("\nActive reservation details:");
            for (Reservation reservation : reservations) {
                System.out.println("  - Student: " + reservation.getStudent().getName() + 
                        " → model.models.Laptop: " + reservation.getLaptop().getBrand() + " " +
                        reservation.getLaptop().getModel());
            }
        }
        
        printSectionFooter();
    }
    
    /**
     * Waits for the demonstration to complete or system to be shut down.
     */
    private static void waitForCompletion() throws InterruptedException {
        shutdownLatch.await(60, TimeUnit.SECONDS);
    }
    
    /**
     * Performs cleanup operations before system shutdown.
     */
    private static void performCleanup() {
        System.out.println("Shutting down system...");
        
        // Shut down network service
        if (networkService != null) {
            networkService.stopServer();
            log.info("Network service stopped");
        }
        
        // Close ViewModels
        if (studentViewModel != null) {
            studentViewModel.close();
        }
        
        if (laptopViewModel != null) {
            laptopViewModel.close();
        }
        
        if (reservationViewModel != null) {
            reservationViewModel.close();
        }
        
        // Shut down thread pool
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("System shutdown complete");
        log.info("System resources released");
    }
    
    // Utility methods for formatting console output
    
    private static void printSectionHeader(String title) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(" " + title);
        System.out.println("=".repeat(50));
    }
    
    private static void printSectionFooter() {
        System.out.println("-".repeat(50) + "\n");
    }
    
    private static void printSeparator() {
        System.out.println("=".repeat(50) + "\n");
    }
}