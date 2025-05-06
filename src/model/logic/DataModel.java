package model.logic;

import model.enums.PerformanceTypeEnum;
import model.enums.ReservationStatusEnum;
import model.logic.reservationsLogic.ReservationManager;
import model.models.Laptop;
import model.models.Reservation;
import model.models.Student;
import model.util.PropertyChangeNotifier;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface DataModel extends PropertyChangeNotifier {

    void refreshCaches();

    List<Laptop> getAllLaptops();

    int getAmountOfAvailableLaptops();

    int getAmountOfLoanedLaptops();

    Laptop findAvailableLaptop(PerformanceTypeEnum performanceType);

    Laptop createLaptop(String brand, String model, int gigabyte, int ram, PerformanceTypeEnum performanceType);

    boolean updateLaptop(Laptop laptop);

    List<Student> getAllStudents();

    int getStudentCount();

    Student getStudentByID(int viaId);

    List<Student> getStudentWithHighPowerNeeds();

    int getStudentCountOfHighPowerNeeds();

    List<Student> getStudentWithLowPowerNeeds();

    int getStudentCountOfLowPowerNeeds();

    List<Student> getThoseWhoHaveLaptop();

    int getCountOfWhoHasLaptop();

    Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                          int viaId, String email, int phoneNumber,
                          PerformanceTypeEnum performanceNeeded);

    boolean updateStudent(Student student);

    ReservationManager getReservationManager();

    Reservation createReservation(Laptop laptop, Student student);

    boolean updateReservationStatus(UUID reservationId, ReservationStatusEnum newStatus);

    int getAmountOfActiveReservations();

    List<Reservation> getAllActiveReservations();

    void addToHighPerformanceQueue(Student student);

    void addToLowPerformanceQueue(Student student);

    int getHighNeedingQueueSize();

    int getLowNeedingQueueSize();

    List<Student> getStudentsInHighPerformanceQueue();

    List<Student> getStudentsInLowPerformanceQueue();
}