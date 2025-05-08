package model.logic.studentLogic;

import model.enums.PerformanceTypeEnum;
import model.models.Student;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Interface that defines operations for Student data management.
 * Updated to support MVVM architecture.
 */
public interface StudentDataInterface {

    /**
     * Returns all students in the system.
     *
     * @return List of all students
     */
    ArrayList<Student> getAllStudents();

    /**
     * Returns the number of students.
     *
     * @return Student count
     */
    int getStudentCount();

    /**
     * Finds a student based on VIA ID.
     *
     * @param id VIA ID
     * @return The student if found, otherwise null
     */
    Student getStudentByID(int id);

    /**
     * Returns students who need high performance laptops.
     *
     * @return List of students with high-performance needs
     */
    ArrayList<Student> getStudentWithHighPowerNeeds();

    /**
     * Returns the number of students with high performance needs.
     *
     * @return Number of students with high-performance needs
     */
    int getStudentCountOfHighPowerNeeds();

    /**
     * Returns students who need low performance laptops.
     *
     * @return List of students with low-performance needs
     */
    ArrayList<Student> getStudentWithLowPowerNeeds();

    /**
     * Returns the number of students with low performance needs.
     *
     * @return Number of students with low-performance needs
     */
    int getStudentCountOfLowPowerNeeds();

    /**
     * Returns students who currently have a laptop.
     *
     * @return List of students with laptop
     */
    ArrayList<Student> getThoseWhoHaveLaptop();

    /**
     * Returns the number of students who have a laptop.
     *
     * @return Number of students with laptop
     */
    int getCountOfWhoHasLaptop();

    /**
     * Creates a new student.
     *
     * @param name              Student's name
     * @param degreeEndDate     End date of degree program
     * @param degreeTitle       Title of degree program
     * @param viaId             VIA ID
     * @param email             Email address
     * @param phoneNumber       Phone number
     * @param performanceNeeded Performance needs (HIGH/LOW)
     * @return The created student or null on error
     */
    Student createStudent(String name, Date degreeEndDate, String degreeTitle,
                          int viaId, String email, int phoneNumber,
                          PerformanceTypeEnum performanceNeeded);

    /**
     * Updates an existing student.
     *
     * @param student The updated student
     * @return true if the operation was successful
     */
    boolean updateStudent(Student student);

    /**
     * Deletes a student.
     *
     * @param viaId Student VIA ID
     * @return true if the operation was successful
     */
    boolean deleteStudent(int viaId);
}