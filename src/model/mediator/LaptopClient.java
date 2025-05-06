package model.mediator;

import model.enums.PerformanceTypeEnum;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// Implementation
public class LaptopClient implements ServerModel {
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 6789;

  private String host;
  private int port;
  private Socket socket;
  private ObjectInputStream in;
  private ObjectOutputStream out;

  public LaptopClient() {
    this(DEFAULT_HOST, DEFAULT_PORT);
  }

  public LaptopClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public void connect() throws IOException {
    socket = new Socket(host, port);
    // Vigtigt at oprette output stream først for at undgå deadlock
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());
  }

  @Override
  public void disconnect() throws IOException {
    if (out != null) out.close();
    if (in != null) in.close();
    if (socket != null) socket.close();
  }

  @Override
  public List<Laptop> getAllLaptops() throws IOException {
    try {
      out.writeObject("GET_ALL_LAPTOPS");
      out.flush();

      String status = (String) in.readObject();
      if ("SUCCESS".equals(status)) {
        return (List<Laptop>) in.readObject();
      } else {
        String errorMsg = (String) in.readObject();
        throw new IOException("Fejl ved hentning af laptops: " + errorMsg);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }

  @Override
  public List<Student> getAllStudents() throws IOException {
    try {
      out.writeObject("GET_ALL_STUDENTS");
      out.flush();

      String status = (String) in.readObject();
      if ("SUCCESS".equals(status)) {
        return (List<Student>) in.readObject();
      } else {
        String errorMsg = (String) in.readObject();
        throw new IOException("Fejl ved hentning af studerende: " + errorMsg);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }

  @Override
  public Object[] createStudent(String name, Date degreeEndDate, String degreeTitle,
                                int viaId, String email, int phoneNumber,
                                PerformanceTypeEnum performanceNeeded) throws IOException {
    try {
      out.writeObject("CREATE_STUDENT");
      out.writeObject(name);
      out.writeObject(degreeEndDate);
      out.writeObject(degreeTitle);
      out.writeObject(viaId);
      out.writeObject(email);
      out.writeObject(phoneNumber);
      out.writeObject(performanceNeeded);
      out.flush();

      String status = (String) in.readObject();
      Object data = in.readObject();

      if ("SUCCESS_WITH_LAPTOP".equals(status)) {
        // Studenten fik tildelt en laptop
        Object[] result = (Object[]) data;
        return new Object[] { "LAPTOP_ASSIGNED", result[0], result[1] };
      } else if ("SUCCESS_WAITLIST".equals(status)) {
        // Studenten blev sat på venteliste
        return new Object[] { "WAITLIST", data, null };
      } else if ("SUCCESS_NO_LAPTOP".equals(status)) {
        // Studenten blev oprettet, men der skete en fejl ved tildeling af laptop
        return new Object[] { "ERROR_ASSIGNING", data, null };
      } else {
        // Der skete en fejl
        throw new IOException("Fejl ved oprettelse af student: " + data);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }

  @Override
  public Laptop findAvailableLaptop(PerformanceTypeEnum performanceNeeded) throws IOException {
    try {
      out.writeObject("FIND_AVAILABLE_LAPTOP");
      out.writeObject(performanceNeeded);
      out.flush();

      String status = (String) in.readObject();
      if ("SUCCESS".equals(status)) {
        return (Laptop) in.readObject();
      } else if ("NOT_FOUND".equals(status)) {
        return null;
      } else {
        String errorMsg = (String) in.readObject();
        throw new IOException("Fejl ved søgning efter laptop: " + errorMsg);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }

  @Override
  public Reservation createReservation(UUID laptopId, int studentViaId) throws IOException {
    try {
      out.writeObject("CREATE_RESERVATION");
      out.writeObject(laptopId);
      out.writeObject(studentViaId);
      out.flush();

      String status = (String) in.readObject();
      if ("SUCCESS".equals(status)) {
        return (Reservation) in.readObject();
      } else {
        String errorMsg = (String) in.readObject();
        throw new IOException("Fejl ved oprettelse af reservation: " + errorMsg);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }

  @Override
  public List<Student> getHighPerformanceQueue() throws IOException {
    try {
      out.writeObject("GET_HIGH_PERFORMANCE_QUEUE");
      out.flush();

      String status = (String) in.readObject();
      if ("SUCCESS".equals(status)) {
        return (List<Student>) in.readObject();
      } else {
        String errorMsg = (String) in.readObject();
        throw new IOException("Fejl ved hentning af høj-ydelses venteliste: " + errorMsg);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }

  @Override
  public List<Student> getLowPerformanceQueue() throws IOException {
    try {
      out.writeObject("GET_LOW_PERFORMANCE_QUEUE");
      out.flush();

      String status = (String) in.readObject();
      if ("SUCCESS".equals(status)) {
        return (List<Student>) in.readObject();
      } else {
        String errorMsg = (String) in.readObject();
        throw new IOException("Fejl ved hentning af lav-ydelses venteliste: " + errorMsg);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Kommunikationsfejl: " + e.getMessage());
    }
  }
}
