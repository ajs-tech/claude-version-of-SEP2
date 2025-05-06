package view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import viewModel.ViewModelFactory;

import java.io.IOException;

public class ViewHandler {
  private Scene currentScene;
  private Stage primaryStage;
  private ViewModelFactory viewModelFactory;
  private StudentLaptopViewController studentLaptopViewController;

  public ViewHandler(ViewModelFactory viewModelFactory) {
    this.viewModelFactory = viewModelFactory;
    this.currentScene = new Scene(new Region());
  }

  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    openStudentLaptopView();
  }

  public void openStudentLaptopView() {
    Region root = loadStudentLaptopView("StudentLaptopView.fxml");
    primaryStage.setTitle("VIA Laptop Udlånssystem");
    primaryStage.setScene(currentScene);
    primaryStage.setWidth(1000);
    primaryStage.setHeight(800);
    primaryStage.show();
  }

  private Region loadStudentLaptopView(String fxmlFile) {
    Region root = null;
    if (studentLaptopViewController == null) {
      try {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(fxmlFile));
        root = loader.load();
        studentLaptopViewController = loader.getController();
        studentLaptopViewController.init(this, viewModelFactory.getStudentLaptopViewModel());
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      studentLaptopViewController.init(this, viewModelFactory.getStudentLaptopViewModel());
    }
    currentScene.setRoot(root);
    return root;
  }
}