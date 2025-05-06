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

public interface ServerModel {
  void connect() throws IOException;
  void disconnect() throws IOException;

  List<Laptop> getAllLaptops() throws IOException;
  List<Student> getAllStudents() throws IOException;

  Object[] createStudent(String name, Date degreeEndDate, String degreeTitle,
                         int viaId, String email, int phoneNumber,
                         PerformanceTypeEnum performanceNeeded) throws IOException;

  Laptop findAvailableLaptop(PerformanceTypeEnum performanceNeeded) throws IOException;

  Reservation createReservation(UUID laptopId, int studentViaId) throws IOException;

  List<Student> getHighPerformanceQueue() throws IOException;
  List<Student> getLowPerformanceQueue() throws IOException;
}

