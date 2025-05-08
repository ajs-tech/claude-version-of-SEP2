package model.models;

/**
 * Concrete implementation of LaptopState for laptops that are loaned.
 * Part of State Pattern.
 */
public class LoanedState implements LaptopState {

    /**
     * When a laptop in loaned state is "clicked", the state changes to available.
     *
     * @param laptop model.models.Laptop object whose state should be changed
     */
    @Override
    public void click(Laptop laptop) {
        laptop.changeState(new AvailableState());
    }

    /**
     * Returns a user-friendly description of the state.
     *
     * @return Display name for this state
     */
    @Override
    public String getDisplayName() {
        return "Loaned";
    }

    /**
     * String representation of the object, primarily used for debugging.
     */
    @Override
    public String toString() {
        return "LoanedState";
    }
}