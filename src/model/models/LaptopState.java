package model.models;

/**
 * Interface for State Pattern anvendt på Laptop.
 * Definerer adfærd for forskellige tilstande en laptop kan være i.
 */
public interface LaptopState {

    /**
     * Håndterer klik/state-ændring på en laptop.
     * Implementeres specifikt for hver konkret state.
     *
     * @param laptop Laptop-objektet hvis state skal ændres
     */
    void click(Laptop laptop);

    /**
     * Returnerer en tekstuel beskrivelse af tilstanden.
     *
     * @return Læsevenlig beskrivelse af tilstanden
     */
    String getDisplayName();
}