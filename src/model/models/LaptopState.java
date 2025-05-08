package model.models;

/**
 * Interface for State Pattern applied to model.models.Laptop.
 * Defines behavior for different states a laptop can be in.
 */
public interface LaptopState {

    /**
     * Handles click/state change on a laptop.
     * Implemented specifically for each concrete state.
     *
     * @param laptop model.models.Laptop object whose state should be changed
     */
    void click(Laptop laptop);

    /**
     * Returns a user-friendly description of the state.
     *
     * @return User-friendly description of the state
     */
    String getDisplayName();
}
