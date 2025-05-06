package MyApplication;

import javafx.application.Application;
import javafx.stage.Stage;
import model.util.ModelFactory;
import view.ViewHandler;
import viewModel.ViewModelFactory;

public class MyApplication extends Application {
  @Override
  public void start(Stage primaryStage) {
    try {
      // Opret ModelFactory
      ModelFactory modelFactory = ModelFactory.getInstance();

      // Opret ViewModelFactory med reference til model
      ViewModelFactory viewModelFactory = new ViewModelFactory(modelFactory.getModel());

      // Opret ViewHandler med reference til ViewModelFactory
      ViewHandler viewHandler = new ViewHandler(viewModelFactory);

      // Start applikationen
      viewHandler.start(primaryStage);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Fejl ved opstart af applikationen: " + e.getMessage());
    }
  }

  @Override
  public void stop() {
    // Luk forbindelsen til serveren
    try {
      ModelFactory.getInstance().closeConnection();
    } catch (Exception e) {
      System.err.println("Fejl ved lukning af forbindelse: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}