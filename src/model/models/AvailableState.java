package model.models;

/**
 * Konkret implementation af LaptopState for laptops, der er tilgængelige.
 * Del af State Pattern.
 */
public class AvailableState implements LaptopState {

    /**
     * Singleton instance for at undgå unødvendig objektoprettelse.
     * Laptops kan dele samme instance af tilstanden.
     */
    public static final LaptopState INSTANCE = new AvailableState();

    /**
     * Når en laptop i tilgængelig tilstand "klikkes", ændres tilstanden til udlånt.
     *
     * @param laptop Laptop-objektet hvis tilstand skal ændres
     */
    @Override
    public void click(Laptop laptop) {
        laptop.changeState(LoanedState.INSTANCE);
    }

    /**
     * Returnerer en brugervenlig beskrivelse af tilstanden.
     *
     * @return Displaynavn for denne tilstand
     */
    @Override
    public String getDisplayName() {
        return "Tilgængelig";
    }

    /**
     * String-repræsentation af objektet, bruges primært til debugging.
     */
    @Override
    public String toString() {
        return "AvailableState";
    }
}