package model.models;

/**
 * Konkret implementation af LaptopState for laptops, der er udlånt.
 * Del af State Pattern.
 */
public class LoanedState implements LaptopState {

    /**
     * Singleton instance for at undgå unødvendig objektoprettelse.
     * Laptops kan dele samme instance af tilstanden.
     */
    public static final LaptopState INSTANCE = new LoanedState();

    /**
     * Når en laptop i udlånt tilstand "klikkes", ændres tilstanden til tilgængelig.
     *
     * @param laptop Laptop-objektet hvis tilstand skal ændres
     */
    @Override
    public void click(Laptop laptop) {
        laptop.changeState(AvailableState.INSTANCE);
    }

    /**
     * Returnerer en brugervenlig beskrivelse af tilstanden.
     *
     * @return Displaynavn for denne tilstand
     */
    @Override
    public String getDisplayName() {
        return "Udlånt";
    }

    /**
     * String-repræsentation af objektet, bruges primært til debugging.
     */
    @Override
    public String toString() {
        return "LoanedState";
    }
}