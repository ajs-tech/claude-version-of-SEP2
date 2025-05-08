package model.logic.reservationsLogic;

import model.enums.PerformanceTypeEnum;
import model.log.Log;
import model.models.Student;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Generic implementation of a queue system for students.
 * Uses Java's built-in Observable pattern and thread-safe operations.
 */
public class GenericQueue extends Observable {
    private static final Logger logger = Logger.getLogger(GenericQueue.class.getName());
    private static final Log log = Log.getInstance();

    // Event types for observer notifications
    public static final String EVENT_STUDENT_ADDED = "STUDENT_ADDED";
    public static final String EVENT_STUDENT_REMOVED = "STUDENT_REMOVED";
    public static final String EVENT_QUEUE_SIZE_CHANGED = "QUEUE_SIZE_CHANGED";
    public static final String EVENT_QUEUE_CLEARED = "QUEUE_CLEARED";

    private final Queue<Student> queue;
    private final PerformanceTypeEnum performanceType;
    private final Lock queueLock;

    /**
     * Creates a new queue for a specified performance type.
     *
     * @param performanceType The type of queue (HIGH/LOW)
     */
    public GenericQueue(PerformanceTypeEnum performanceType) {
        this.queue = new ArrayDeque<>();
        this.performanceType = performanceType;
        this.queueLock = new ReentrantLock();
    }

    /**
     * Returns the size of the queue.
     *
     * @return Number of students in the queue
     */
    public int getQueueSize() {
        queueLock.lock();
        try {
            return queue.size();
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Gets the next student in the queue without removing them.
     *
     * @return The next student in the queue, or null if the queue is empty
     */
    public Student peekNextInLine() {
        queueLock.lock();
        try {
            return queue.peek();
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Gets and removes the next student in the queue.
     *
     * @return The next student in the queue, or null if the queue is empty
     */
    public Student getAndRemoveNextInLine() {
        queueLock.lock();
        try {
            Student student = queue.poll();

            if (student != null) {
                logger.info("Student [" + student.getName() + ", VIA ID: " + student.getViaId() +
                        "] removed from " + getQueueTypeName() + " performance queue");

                // Notify observers
                setChanged();
                notifyObservers(new QueueEvent(EVENT_STUDENT_REMOVED, student));

                int newSize = queue.size();
                setChanged();
                notifyObservers(new QueueSizeEvent(EVENT_QUEUE_SIZE_CHANGED, newSize + 1, newSize));
            }

            return student;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Adds a student to the queue.
     *
     * @param student The student to add
     * @throws IllegalArgumentException if student is null
     */
    public void addToQueue(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }

        queueLock.lock();
        try {
            int oldSize = queue.size();
            queue.offer(student);

            logger.info("Student [" + student.getName() + ", VIA ID: " + student.getViaId() +
                    "] added to " + getQueueTypeName() + " performance queue");

            // Notify observers
            setChanged();
            notifyObservers(new QueueEvent(EVENT_STUDENT_ADDED, student));

            int newSize = queue.size();
            setChanged();
            notifyObservers(new QueueSizeEvent(EVENT_QUEUE_SIZE_CHANGED, oldSize, newSize));
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Returns a list of all students in the queue without removing them.
     *
     * @return List of students in the queue
     */
    public List<Student> getAllStudentsInQueue() {
        queueLock.lock();
        try {
            return new ArrayList<>(queue);
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Removes a specific student from the queue based on VIA ID.
     *
     * @param viaId VIA ID of the student to remove
     * @return true if the student was removed, false otherwise
     */
    public boolean removeStudentById(int viaId) {
        queueLock.lock();
        try {
            Student studentToRemove = null;

            // Find the student with the given ID
            for (Student student : queue) {
                if (student.getViaId() == viaId) {
                    studentToRemove = student;
                    break;
                }
            }

            // Remove the student if found
            if (studentToRemove != null) {
                int oldSize = queue.size();
                boolean removed = queue.remove(studentToRemove);

                if (removed) {
                    logger.info("Student [" + studentToRemove.getName() + ", VIA ID: " +
                            studentToRemove.getViaId() + "] removed from " + getQueueTypeName() + " performance queue");

                    // Notify observers
                    setChanged();
                    notifyObservers(new QueueEvent(EVENT_STUDENT_REMOVED, studentToRemove));

                    int newSize = queue.size();
                    setChanged();
                    notifyObservers(new QueueSizeEvent(EVENT_QUEUE_SIZE_CHANGED, oldSize, newSize));
                }

                return removed;
            }

            return false;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Checks if the queue contains a student with a specific VIA ID.
     *
     * @param viaId VIA ID to check for
     * @return true if the student is in the queue, false otherwise
     */
    public boolean containsStudent(int viaId) {
        queueLock.lock();
        try {
            for (Student student : queue) {
                if (student.getViaId() == viaId) {
                    return true;
                }
            }
            return false;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Returns the type of queue (HIGH/LOW).
     *
     * @return The queue's performance type
     */
    public PerformanceTypeEnum getPerformanceType() {
        return performanceType;
    }

    /**
     * Returns a human-readable string for the queue's type.
     *
     * @return "high" for HIGH performance type, "low" for LOW
     */
    private String getQueueTypeName() {
        return performanceType == PerformanceTypeEnum.HIGH ? "high" : "low";
    }

    /**
     * Clears all students from the queue.
     */
    public void clear() {
        queueLock.lock();
        try {
            int oldSize = queue.size();

            if (oldSize > 0) {
                List<Student> removedStudents = new ArrayList<>(queue);
                queue.clear();

                logger.info(getQueueTypeName() + " performance queue cleared: " +
                        oldSize + " students removed");
                log.info(getQueueTypeName() + " performance queue cleared");

                // Notify about the clear event
                setChanged();
                notifyObservers(new ClearEvent(EVENT_QUEUE_CLEARED,
                        performanceType, oldSize, removedStudents));

                // Notify about size change
                setChanged();
                notifyObservers(new QueueSizeEvent(EVENT_QUEUE_SIZE_CHANGED, oldSize, 0));
            }
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Event class for queue operations.
     */
    public static class QueueEvent {
        private final String eventType;
        private final Student student;

        public QueueEvent(String eventType, Student student) {
            this.eventType = eventType;
            this.student = student;
        }

        public String getEventType() {
            return eventType;
        }

        public Student getStudent() {
            return student;
        }
    }

    /**
     * Event class for queue size changes.
     */
    public static class QueueSizeEvent {
        private final String eventType;
        private final int oldSize;
        private final int newSize;

        public QueueSizeEvent(String eventType, int oldSize, int newSize) {
            this.eventType = eventType;
            this.oldSize = oldSize;
            this.newSize = newSize;
        }

        public String getEventType() {
            return eventType;
        }

        public int getOldSize() {
            return oldSize;
        }

        public int getNewSize() {
            return newSize;
        }
    }

    /**
     * Event class for queue clearing.
     */
    public static class ClearEvent {
        private final String eventType;
        private final PerformanceTypeEnum queueType;
        private final int oldSize;
        private final List<Student> removedStudents;

        public ClearEvent(String eventType, PerformanceTypeEnum queueType,
                          int oldSize, List<Student> removedStudents) {
            this.eventType = eventType;
            this.queueType = queueType;
            this.oldSize = oldSize;
            this.removedStudents = removedStudents;
        }

        public String getEventType() {
            return eventType;
        }

        public PerformanceTypeEnum getQueueType() {
            return queueType;
        }

        public int getOldSize() {
            return oldSize;
        }

        public List<Student> getRemovedStudents() {
            return removedStudents;
        }
    }
}