package model.logic.studentLogic;

import model.enums.PerformanceTypeEnum;
import model.models.Student;

import java.util.ArrayList;
import java.util.Date;

/**
 * Interface der definerer operationer for Student data management.
 * Opdateret til MVVM-arkitektur med support for de nye metoder.
 */
public interface StudentDataInterface {

    /**
     * Returnerer alle studerende i systemet.
     *
     * @return Liste af alle studerende
     */
    ArrayList<Student> getAllStudents();

    /**
     * Returnerer det totale antal studerende.
     *
     * @return Antal studerende
     */
    int getStudentCount();

    /**
     * Finder en student baseret på VIA ID.
     *
     * @param id VIA ID
     * @return Studenten hvis fundet, ellers null
     */
    Student getStudentByID(int id);

    /**
     * Returnerer studerende med behov for høj ydelse.
     *
     * @return Liste af studerende med høj-ydelses behov
     */
    ArrayList<Student> getStudentWithHighPowerNeeds();

    /**
     * Returnerer antal studerende med behov for høj ydelse.
     *
     * @return Antal studerende med høj-ydelses behov
     */
    int getStudentCountOfHighPowerNeeds();

    /**
     * Returnerer studerende med behov for lav ydelse.
     *
     * @return Liste af studerende med lav-ydelses behov
     */
    ArrayList<Student> getStudentWithLowPowerNeeds();

    /**
     * Returnerer antal studerende med behov for lav ydelse.
     *
     * @return Antal studerende med lav-ydelses behov
     */
    int getStudentCountOfLowPowerNeeds();

    /**
     * Returnerer studerende der har en laptop.
     *
     * @return Liste af studerende med laptop
     */
    ArrayList<Student> getThoseWhoHaveLaptop();

    /**
     * Returnerer antal studerende der har en laptop.
     *
     * @return Antal studerende med laptop
     */
    int getCountOfWhoHasLaptop();

    /**
     * Opretter en ny student.
     *
     * @param name Name
     * @param degreeEndDate Uddannelses slutdato
     * @param degreeTitle Uddannelsestitel
     * @param viaId VIA ID
     * @param email Email
     * @param phoneNumber Telefonnummer
     * @param performanceNeeded Performance behov (HIGH/LOW)
     * @return Den oprettede student eller null ved fejl
     */
    Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                          int viaId, String email, int phoneNumber,
                          PerformanceTypeEnum performanceNeeded);

    /**
     * Opdaterer en eksisterende student.
     *
     * @param student Student at opdatere
     * @return true hvis operationen lykkedes
     */
    boolean updateStudent(Student student);

    /**
     * Sletter en student.
     *
     * @param viaId Student VIA ID
     * @return true hvis operationen lykkedes
     */
    boolean deleteStudent(int viaId);
}