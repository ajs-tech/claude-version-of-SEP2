package model.models;

/**
 * Concrete implementation of LaptopState for laptops that are available.
 * Part of State Pattern.
 */
class AvailableState implements LaptopState {

    /**
     * When a laptop in available state is "clicked", the state changes to loaned.
     *
     * @param laptop model.models.Laptop object whose state should be changed
     */
    @Override
    public void click(Laptop laptop) {
        laptop.changeState(new LoanedState());
    }

    /**
     * Returns a user-friendly description of the state.
     *
     * @return Display name for this state
     */
    @Override
    public String getDisplayName() {
        return "Available";
    }

    /**
     * String representation of the object, primarily used for debugging.
     */
    @Override
    public String toString() {
        return "model.models.AvailableState";
    }
}
