package viewModel;

import model.logic.DataModel;
import model.enums.PerformanceTypeEnum;
import model.logic.DataModel;
import model.models.Laptop;
import model.models.Student;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.beans.PropertyChangeEvent;
import java.util.Date;
import java.util.List;

public class StudentLaptopViewModel {
  // Reference til model laget
  private DataModel model;

  // Properties for student input
  private StringProperty name;
  private StringProperty email;
  private StringProperty viaId;
  private StringProperty phoneNumber;
  private StringProperty degreeTitle;
  private ObjectProperty<Date> degreeEndDate;
  private BooleanProperty isHighPerformance;

  // Properties for resultat visning
  private StringProperty resultStudent;
  private StringProperty resultStatus;
  private StringProperty resultLaptop;
  private StringProperty errorMessage;
  private StringProperty statusMessage;

  // Observable lister til tabeller
  private ObservableList<Laptop> allLaptops;
  private ObservableList<Student> highPerformanceQueue;
  private ObservableList<Student> lowPerformanceQueue;

  public StudentLaptopViewModel(DataModel model) {
    this.model = model;

    // Initialisér properties til student input
    name = new SimpleStringProperty("");
    email = new SimpleStringProperty("");
    viaId = new SimpleStringProperty("");
    phoneNumber = new SimpleStringProperty("");
    degreeTitle = new SimpleStringProperty("");
    degreeEndDate = new SimpleObjectProperty<>();
    isHighPerformance = new SimpleBooleanProperty(false);

    // Initialisér properties til resultat visning
    resultStudent = new SimpleStringProperty("Ingen handling endnu");
    resultStatus = new SimpleStringProperty("Ingen handling endnu");
    resultLaptop = new SimpleStringProperty("Ingen tildeling endnu");
    errorMessage = new SimpleStringProperty("");
    statusMessage = new SimpleStringProperty("Klar til at oprette studerende");

    // Initialisér observable lister
    allLaptops = FXCollections.observableArrayList();
    highPerformanceQueue = FXCollections.observableArrayList();
    lowPerformanceQueue = FXCollections.observableArrayList();

    // Tilføj lyttere til model events
    model.addPropertyChangeListener("laptopsRefreshed", this::onLaptopsRefreshed);
    model.addPropertyChangeListener("studentCreated", this::onStudentCreated);
    model.addPropertyChangeListener("reservationCreated", this::onReservationCreated);
    model.addPropertyChangeListener("studentsRefreshed", this::onStudentsRefreshed);
    model.addPropertyChangeListener("highQueueChanged", this::onHighQueueChanged);
    model.addPropertyChangeListener("lowQueueChanged", this::onLowQueueChanged);

    // Indlæs data ved start
    refreshLaptops();
    refreshQueues();
  }


  // Event handlers
  private void onLaptopsRefreshed(PropertyChangeEvent evt) {
    refreshLaptops();
  }

  private void onStudentCreated(PropertyChangeEvent evt) {
    Student createdStudent = (Student) evt.getNewValue();
    resultStudent.set(createdStudent.getName() + " (VIA ID: " + createdStudent.getViaId() + ")");

    // Studenten blev oprettet - vi venter på at se om de fik tildelt en laptop eller kom på venteliste
    resultStatus.set("Studerende oprettet, venter på tildeling...");
    statusMessage.set("Studerende " + createdStudent.getName() + " oprettet");
  }

  private void onReservationCreated(PropertyChangeEvent evt) {
    refreshLaptops(); // Opdater laptop tabel

    if (evt.getNewValue() != null) {
      // Der blev oprettet en reservation, så vi viser laptop detaljer
      Laptop laptop = (Laptop) evt.getNewValue();
      resultLaptop.set(laptop.getBrand() + " " + laptop.getModel() + " (" + laptop.getPerformanceType() + ")");
      resultStatus.set("Laptop tildelt");
      statusMessage.set("Laptop tildelt til studerende");
    }
  }

  private void onStudentsRefreshed(PropertyChangeEvent evt) {
    refreshQueues();
  }

  private void onHighQueueChanged(PropertyChangeEvent evt) {
    highPerformanceQueue.clear();
    if (evt.getNewValue() instanceof List) {
      List<Student> students = (List<Student>) evt.getNewValue();
      highPerformanceQueue.addAll(students);

      // Hvis resultatet er "venter på tildeling" og en student blev tilføjet til køen
      if (resultStatus.get().contains("venter på tildeling")) {
        resultStatus.set("Sat på høj-ydelses venteliste");
        resultLaptop.set("Ingen tilgængelig laptop");
        statusMessage.set("Studerende placeret i høj-ydelses venteliste");
      }
    }
  }

  private void onLowQueueChanged(PropertyChangeEvent evt) {
    lowPerformanceQueue.clear();
    if (evt.getNewValue() instanceof List) {
      List<Student> students = (List<Student>) evt.getNewValue();
      lowPerformanceQueue.addAll(students);

      // Hvis resultatet er "venter på tildeling" og en student blev tilføjet til køen
      if (resultStatus.get().contains("venter på tildeling")) {
        resultStatus.set("Sat på lav-ydelses venteliste");
        resultLaptop.set("Ingen tilgængelig laptop");
        statusMessage.set("Studerende placeret i lav-ydelses venteliste");
      }
    }
  }

  // Metoder til at opdatere data fra model
  public void refreshLaptops() {
    allLaptops.clear();
    allLaptops.addAll(model.getAllLaptops());
  }

  public void refreshQueues() {
    // Opdater høj-ydelses venteliste
    highPerformanceQueue.clear();
    highPerformanceQueue.addAll(model.getStudentWithHighPowerNeeds());

    // Opdater lav-ydelses venteliste
    lowPerformanceQueue.clear();
    lowPerformanceQueue.addAll(model.getStudentWithLowPowerNeeds());
  }

  // Action metoder kaldt fra View
  public void createStudent() {
    try {
      errorMessage.set("");
      if (!validateInput()) {
        return;
      }

      // Konverter input til korrekte datatyper
      int viaIdInt = Integer.parseInt(viaId.get());
      int phoneNumberInt = Integer.parseInt(phoneNumber.get());
      PerformanceTypeEnum performanceType = isHighPerformance.get() ?
              PerformanceTypeEnum.HIGH : PerformanceTypeEnum.LOW;

      // Opret studerende via modellen - laptop tildeling og evt. køplacering håndteres automatisk
      model.createStudent(
              name.get(),
              degreeEndDate.get(),
              degreeTitle.get(),
              viaIdInt,
              email.get(),
              phoneNumberInt,
              performanceType
      );

      // Reset input felter
      resetForm();

    } catch (NumberFormatException e) {
      errorMessage.set("Fejl i numeriske felter: " + e.getMessage());
    } catch (Exception e) {
      errorMessage.set("Fejl ved oprettelse: " + e.getMessage());
    }
  }

  public void clearForm() {
    resetForm();
    errorMessage.set("");
  }

  // Validering af input
  private boolean validateInput() {
    if (name.get() == null || name.get().isEmpty()) {
      errorMessage.set("Navn skal udfyldes");
      return false;
    }

    if (email.get() == null || email.get().isEmpty() || !email.get().contains("@")) {
      errorMessage.set("Email skal være en gyldig email-adresse");
      return false;
    }

    if (viaId.get() == null || viaId.get().isEmpty()) {
      errorMessage.set("VIA ID skal udfyldes");
      return false;
    }

    try {
      Integer.parseInt(viaId.get());
    } catch (NumberFormatException e) {
      errorMessage.set("VIA ID skal være et tal");
      return false;
    }

    if (phoneNumber.get() == null || phoneNumber.get().isEmpty()) {
      errorMessage.set("Telefonnummer skal udfyldes");
      return false;
    }

    try {
      Integer.parseInt(phoneNumber.get());
    } catch (NumberFormatException e) {
      errorMessage.set("Telefonnummer skal være et tal");
      return false;
    }

    if (degreeTitle.get() == null || degreeTitle.get().isEmpty()) {
      errorMessage.set("Uddannelsestitel skal udfyldes");
      return false;
    }

    if (degreeEndDate.get() == null) {
      errorMessage.set("Uddannelse slutdato skal vælges");
      return false;
    }

    return true;
  }

  private void resetForm() {
    name.set("");
    email.set("");
    viaId.set("");
    phoneNumber.set("");
    degreeTitle.set("");
    degreeEndDate.set(null);
    isHighPerformance.set(false);
  }

  // Property getters til binding i View
  public StringProperty nameProperty() { return name; }
  public StringProperty emailProperty() { return email; }
  public StringProperty viaIdProperty() { return viaId; }
  public StringProperty phoneNumberProperty() { return phoneNumber; }
  public StringProperty degreeTitleProperty() { return degreeTitle; }
  public ObjectProperty<Date> degreeEndDateProperty() { return degreeEndDate; }
  public BooleanProperty isHighPerformanceProperty() { return isHighPerformance; }

  public StringProperty resultStudentProperty() { return resultStudent; }
  public StringProperty resultStatusProperty() { return resultStatus; }
  public StringProperty resultLaptopProperty() { return resultLaptop; }
  public StringProperty errorMessageProperty() { return errorMessage; }
  public StringProperty statusMessageProperty() { return statusMessage; }

  public ObservableList<Laptop> getAllLaptops() { return allLaptops; }
  public ObservableList<Student> getHighPerformanceQueue() { return highPerformanceQueue; }
  public ObservableList<Student> getLowPerformanceQueue() { return lowPerformanceQueue; }
}