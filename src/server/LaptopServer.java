package server;

import model.database.DatabaseConnection;
import model.database.LaptopDAO;
import model.database.StudentDAO;
import model.database.ReservationDAO;
import model.enums.PerformanceTypeEnum;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class LaptopServer {
  private final int PORT = 6789;
  private ServerSocket serverSocket;
  private boolean running;

  // DAOs til database-kommunikation
  private LaptopDAO laptopDAO;
  private StudentDAO studentDAO;
  private ReservationDAO reservationDAO;

  public LaptopServer() {
    laptopDAO = new LaptopDAO();
    studentDAO = new StudentDAO();
    reservationDAO = new ReservationDAO();
  }

  public void startServer() {
    try {
      serverSocket = new ServerSocket(PORT);
      running = true;
      System.out.println("Server startet på port " + PORT);

      while (running) {
        System.out.println("Venter på klienter...");
        Socket clientSocket = serverSocket.accept();
        System.out.println("Klient forbundet: " + clientSocket.getInetAddress());

        // Start en ny tråd til at håndtere klienten
        ClientHandler handler = new ClientHandler(clientSocket, laptopDAO, studentDAO, reservationDAO);
        new Thread(handler).start();
      }
    } catch (IOException e) {
      System.err.println("Server fejl: " + e.getMessage());
    } finally {
      stopServer();
    }
  }

  public void stopServer() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      System.err.println("Fejl ved lukning af server: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    LaptopServer server = new LaptopServer();
    server.startServer();
  }

  private static class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private LaptopDAO laptopDAO;
    private StudentDAO studentDAO;
    private ReservationDAO reservationDAO;

    public ClientHandler(Socket socket, LaptopDAO laptopDAO, StudentDAO studentDAO, ReservationDAO reservationDAO) {
      this.socket = socket;
      this.laptopDAO = laptopDAO;
      this.studentDAO = studentDAO;
      this.reservationDAO = reservationDAO;
      try {
        // Vigtigt at oprette output stream først for at undgå deadlock
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
      } catch (IOException e) {
        System.err.println("Fejl ved oprettelse af streams: " + e.getMessage());
      }
    }

    @Override
    public void run() {
      try {
        while (true) {
          String operation = (String) in.readObject();

          switch (operation) {
            case "GET_ALL_LAPTOPS":
              handleGetAllLaptops();
              break;
            case "GET_ALL_STUDENTS":
              handleGetAllStudents();
              break;
            case "CREATE_STUDENT":
              handleCreateStudent();
              break;
            case "FIND_AVAILABLE_LAPTOP":
              handleFindAvailableLaptop();
              break;
            case "CREATE_RESERVATION":
              handleCreateReservation();
              break;
            case "GET_HIGH_PERFORMANCE_QUEUE":
              handleGetHighPerformanceQueue();
              break;
            case "GET_LOW_PERFORMANCE_QUEUE":
              handleGetLowPerformanceQueue();
              break;
            default:
              sendResponse("ERROR", "Ukendt operation: " + operation);
          }
        }
      } catch (IOException | ClassNotFoundException e) {
        System.out.println("Klient afbrudt: " + e.getMessage());
      } finally {
        closeConnection();
      }
    }

    private void handleGetAllLaptops() throws IOException {
      try {
        List<Laptop> laptops = laptopDAO.getAll();
        sendResponse("SUCCESS", laptops);
      } catch (SQLException e) {
        sendResponse("ERROR", "Fejl ved hentning af laptops: " + e.getMessage());
      }
    }

    private void handleGetAllStudents() throws IOException {
      try {
        List<Student> students = studentDAO.getAll();
        sendResponse("SUCCESS", students);
      } catch (SQLException e) {
        sendResponse("ERROR", "Fejl ved hentning af studerende: " + e.getMessage());
      }
    }

    private void handleCreateStudent() throws IOException, ClassNotFoundException {
      try {
        // Læs student data
        String name = (String) in.readObject();
        Date degreeEndDate = (Date) in.readObject();
        String degreeTitle = (String) in.readObject();
        int viaId = (int) in.readObject();
        String email = (String) in.readObject();
        int phoneNumber = (int) in.readObject();
        PerformanceTypeEnum performanceNeeded = (PerformanceTypeEnum) in.readObject();

        // Opret studenten
        Student student = new Student(name, degreeEndDate, degreeTitle, viaId, email, phoneNumber, performanceNeeded);
        boolean success = studentDAO.insert(student);

        if (success) {
          // Find en tilgængelig laptop til studenten
          Laptop availableLaptop = findAvailableLaptop(performanceNeeded);

          if (availableLaptop != null) {
            // Opret en reservation (udlån)
            Reservation reservation = new Reservation(student, availableLaptop);
            boolean reservationSuccess = reservationDAO.createReservationWithTransaction(reservation);

            if (reservationSuccess) {
              sendResponse("SUCCESS_WITH_LAPTOP", new Object[]{student, availableLaptop});
            } else {
              sendResponse("SUCCESS_NO_LAPTOP", student);
            }
          } else {
            // Ingen tilgængelig laptop - sæt studenten på venteliste
            // Dette ville typisk håndteres via en QueueDAO, men vi simulerer det her
            sendResponse("SUCCESS_WAITLIST", student);
          }
        } else {
          sendResponse("ERROR", "Kunne ikke oprette student i databasen");
        }
      } catch (SQLException e) {
        sendResponse("ERROR", "Database fejl: " + e.getMessage());
      } catch (Exception e) {
        sendResponse("ERROR", "General fejl: " + e.getMessage());
      }
    }

    private Laptop findAvailableLaptop(PerformanceTypeEnum performanceNeeded) {
      try {
        List<Laptop> laptops = laptopDAO.getAvailableLaptopsByPerformance(performanceNeeded);
        return laptops.isEmpty() ? null : laptops.get(0);
      } catch (SQLException e) {
        System.err.println("Fejl ved søgning efter tilgængelig laptop: " + e.getMessage());
        return null;
      }
    }

    private void handleFindAvailableLaptop() throws IOException, ClassNotFoundException {
      try {
        PerformanceTypeEnum performanceNeeded = (PerformanceTypeEnum) in.readObject();
        Laptop laptop = findAvailableLaptop(performanceNeeded);

        if (laptop != null) {
          sendResponse("SUCCESS", laptop);
        } else {
          sendResponse("NOT_FOUND", "Ingen tilgængelig laptop med den ønskede performance");
        }
      } catch (Exception e) {
        sendResponse("ERROR", "Fejl ved søgning efter laptop: " + e.getMessage());
      }
    }

    private void handleCreateReservation() throws IOException, ClassNotFoundException {
      try {
        UUID laptopId = (UUID) in.readObject();
        int studentViaId = (int) in.readObject();

        Student student = studentDAO.getById(studentViaId);
        Laptop laptop = laptopDAO.getById(laptopId);

        if (student != null && laptop != null && laptop.isAvailable()) {
          Reservation reservation = new Reservation(student, laptop);
          boolean success = reservationDAO.createReservationWithTransaction(reservation);

          if (success) {
            sendResponse("SUCCESS", reservation);
          } else {
            sendResponse("ERROR", "Kunne ikke oprette reservation");
          }
        } else {
          sendResponse("ERROR", "Student eller laptop ikke fundet eller laptop ikke tilgængelig");
        }
      } catch (SQLException e) {
        sendResponse("ERROR", "Database fejl: " + e.getMessage());
      }
    }

    private void handleGetHighPerformanceQueue() throws IOException {
      try {
        // Dette ville typisk hentes fra en QueueDAO
        // Her simulerer vi ved at finde studerende med høj-ydelses behov uden laptop
        List<Student> allStudents = studentDAO.getByPerformanceType(PerformanceTypeEnum.HIGH);
        List<Student> queuedStudents = new ArrayList<>();

        for (Student student : allStudents) {
          if (!student.isHasLaptop()) {
            queuedStudents.add(student);
          }
        }

        sendResponse("SUCCESS", queuedStudents);
      } catch (SQLException e) {
        sendResponse("ERROR", "Fejl ved hentning af høj-ydelses venteliste: " + e.getMessage());
      }
    }

    private void handleGetLowPerformanceQueue() throws IOException {
      try {
        // Dette ville typisk hentes fra en QueueDAO
        // Her simulerer vi ved at finde studerende med lav-ydelses behov uden laptop
        List<Student> allStudents = studentDAO.getByPerformanceType(PerformanceTypeEnum.LOW);
        List<Student> queuedStudents = new ArrayList<>();

        for (Student student : allStudents) {
          if (!student.isHasLaptop()) {
            queuedStudents.add(student);
          }
        }

        sendResponse("SUCCESS", queuedStudents);
      } catch (SQLException e) {
        sendResponse("ERROR", "Fejl ved hentning af lav-ydelses venteliste: " + e.getMessage());
      }
    }

    private void sendResponse(String status, Object data) throws IOException {
      out.writeObject(status);
      out.writeObject(data);
      out.flush();
    }

    private void closeConnection() {
      try {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
      } catch (IOException e) {
        System.err.println("Fejl ved lukning af forbindelse: " + e.getMessage());
      }
    }
  }
}