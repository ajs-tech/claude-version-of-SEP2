package view;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import model.enums.PerformanceTypeEnum;
import model.models.Laptop;
import model.models.Student;
import viewModel.StudentLaptopViewModel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class StudentLaptopViewController {

  // Studerende input felter
  @FXML private TextField nameField;
  @FXML private TextField emailField;
  @FXML private TextField viaIdField;
  @FXML private TextField phoneNumberField;
  @FXML private TextField degreeTitleField;
  @FXML private DatePicker degreeEndDatePicker;
  @FXML private RadioButton lowPerformanceRadio;
  @FXML private RadioButton highPerformanceRadio;
  @FXML private ToggleGroup performanceGroup;
  @FXML private Label studentErrorLabel;

  // Resultat visning
  @FXML private VBox assignmentResultPanel;
  @FXML private Label resultStudentLabel;
  @FXML private Label resultStatusLabel;
  @FXML private Label resultLaptopLabel;

  // Laptop tabel
  @FXML private TableView<Laptop> allLaptopsTable;
  @FXML private TableColumn<Laptop, String> laptopBrandColumn;
  @FXML private TableColumn<Laptop, String> laptopModelColumn;
  @FXML private TableColumn<Laptop, Integer> laptopRamColumn;
  @FXML private TableColumn<Laptop, Integer> laptopDiskColumn;
  @FXML private TableColumn<Laptop, PerformanceTypeEnum> laptopPerformanceColumn;
  @FXML private TableColumn<Laptop, String> laptopStatusColumn;
  @FXML private TableColumn<Laptop, String> laptopStudentColumn;

  // Høj-ydelses venteliste tabel
  @FXML private TableView<Student> highPerformanceQueueTable;
  @FXML private TableColumn<Student, Integer> highQueueViaIdColumn;
  @FXML private TableColumn<Student, String> highQueueNameColumn;
  @FXML private TableColumn<Student, String> highQueueEmailColumn;
  @FXML private TableColumn<Student, String> highQueuePhoneColumn;
  @FXML private TableColumn<Student, Date> highQueueDateColumn;

  // Lav-ydelses venteliste tabel
  @FXML private TableView<Student> lowPerformanceQueueTable;
  @FXML private TableColumn<Student, Integer> lowQueueViaIdColumn;
  @FXML private TableColumn<Student, String> lowQueueNameColumn;
  @FXML private TableColumn<Student, String> lowQueueEmailColumn;
  @FXML private TableColumn<Student, String> lowQueuePhoneColumn;
  @FXML private TableColumn<Student, Date> lowQueueDateColumn;

  // Knapper og status
  @FXML private Button createStudentButton;
  @FXML private Button clearStudentButton;
  @FXML private Button refreshLaptopsButton;
  @FXML private Button refreshQueuesButton;
  @FXML private Button exitButton;
  @FXML private Label statusLabel;

  private StudentLaptopViewModel viewModel;
  private ViewHandler viewHandler;

  public void init(ViewHandler viewHandler, StudentLaptopViewModel viewModel) {
    this.viewHandler = viewHandler;
    this.viewModel = viewModel;

    // Bindings til student input felter
    nameField.textProperty().bindBidirectional(viewModel.nameProperty());
    emailField.textProperty().bindBidirectional(viewModel.emailProperty());
    viaIdField.textProperty().bindBidirectional(viewModel.viaIdProperty());
    phoneNumberField.textProperty().bindBidirectional(viewModel.phoneNumberProperty());
    degreeTitleField.textProperty().bindBidirectional(viewModel.degreeTitleProperty());
    studentErrorLabel.textProperty().bind(viewModel.errorMessageProperty());

    // DatePicker binding (konvertering mellem LocalDate og Date)
    degreeEndDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        Date date = Date.from(newVal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        viewModel.degreeEndDateProperty().setValue(date);
      }
    });
    viewModel.degreeEndDateProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        LocalDate localDate = newVal.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        degreeEndDatePicker.setValue(localDate);
      } else {
        degreeEndDatePicker.setValue(null);
      }
    });

    // RadioButton binding
    highPerformanceRadio.selectedProperty().bindBidirectional(viewModel.isHighPerformanceProperty());
    lowPerformanceRadio.selectedProperty().bind(viewModel.isHighPerformanceProperty().not());

    // Bindings til resultat visning
    resultStudentLabel.textProperty().bind(viewModel.resultStudentProperty());
    resultStatusLabel.textProperty().bind(viewModel.resultStatusProperty());
    resultLaptopLabel.textProperty().bind(viewModel.resultLaptopProperty());
    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    // Setup af laptop tabel
    setupLaptopTable();
    allLaptopsTable.setItems(viewModel.getAllLaptops());

    // Setup af venteliste tabeller
    setupHighPerformanceQueueTable();
    setupLowPerformanceQueueTable();
    highPerformanceQueueTable.setItems(viewModel.getHighPerformanceQueue());
    lowPerformanceQueueTable.setItems(viewModel.getLowPerformanceQueue());

    // Refresh data ved start
    viewModel.refreshLaptops();
    viewModel.refreshQueues();
  }

  private void setupLaptopTable() {
    laptopBrandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
    laptopModelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
    laptopRamColumn.setCellValueFactory(new PropertyValueFactory<>("ram"));
    laptopDiskColumn.setCellValueFactory(new PropertyValueFactory<>("gigabyte"));
    laptopPerformanceColumn.setCellValueFactory(new PropertyValueFactory<>("performanceType"));

    // Custom cell factories for status og student kolonner
    laptopStatusColumn.setCellValueFactory(cellData -> {
      Laptop laptop = cellData.getValue();
      String status = laptop.isAvailable() ? "Tilgængelig" : "Udlånt";
      return new javafx.beans.property.SimpleStringProperty(status);
    });

    laptopStudentColumn.setCellValueFactory(cellData -> {
      Laptop laptop = cellData.getValue();
      String studentInfo = "-";
      // Her ville du typisk tjekke om laptopen er udlånt og vise information
      // om studenten der har den. Dette kræver dog adgang til reservation data.
      return new javafx.beans.property.SimpleStringProperty(studentInfo);
    });
  }

  private void setupHighPerformanceQueueTable() {
    highQueueViaIdColumn.setCellValueFactory(new PropertyValueFactory<>("viaId"));
    highQueueNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
    highQueueEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
    highQueuePhoneColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getPhoneNumber())));
    highQueueDateColumn.setCellValueFactory(cellData -> {
      // Her ville du typisk have en createDate property i Student klassen
      // Dette er en placeholder
      return new javafx.beans.property.SimpleObjectProperty<>(new Date());
    });
  }

  private void setupLowPerformanceQueueTable() {
    lowQueueViaIdColumn.setCellValueFactory(new PropertyValueFactory<>("viaId"));
    lowQueueNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
    lowQueueEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
    lowQueuePhoneColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getPhoneNumber())));
    lowQueueDateColumn.setCellValueFactory(cellData -> {
      // Her ville du typisk have en createDate property i Student klassen
      // Dette er en placeholder
      return new javafx.beans.property.SimpleObjectProperty<>(new Date());
    });
  }

  // Event handlers
  @FXML
  private void onCreateStudent() {
    viewModel.createStudent();
  }

  @FXML
  private void onClearStudentForm() {
    viewModel.clearForm();
  }

  @FXML
  private void onRefreshLaptops() {
    viewModel.refreshLaptops();
  }

  @FXML
  private void onRefreshQueues() {
    viewModel.refreshQueues();
  }

  @FXML
  private void onExit() {
    System.exit(0);
  }
}