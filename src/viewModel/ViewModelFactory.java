package viewModel;


import model.logic.DataModel;

public class ViewModelFactory {
  private StudentLaptopViewModel studentLaptopViewModel;

  public ViewModelFactory(DataModel model) {
    studentLaptopViewModel = new StudentLaptopViewModel(model);
  }

  public StudentLaptopViewModel getStudentLaptopViewModel() {
    return studentLaptopViewModel;
  }
}