package model.util;

import model.logic.DataManager;
import model.logic.DataModel;

/**
 * Factory-klasse til oprettelse og håndtering af model-komponenter.
 * Implementerer Singleton pattern for at sikre én instans på tværs af systemet.
 */
public class ModelFactory {
    private static ModelFactory instance;
    private static final Object lock = new Object();

    private final DataModel model; // Brug interface i stedet for konkret klasse

    /**
     * Privat konstruktør, sikrer Singleton pattern
     */
    private ModelFactory() {
        this.model = new DataManager(); // Konkret implementation
    }

    /**
     * Returnerer den eksisterende instans eller opretter en ny
     *
     * @return ModelFactory instansen
     */
    public static ModelFactory getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ModelFactory();
                }
            }
        }
        return instance;
    }

    /**
     * Returnerer model-interface instansen
     *
     * @return ILaptopSystemModel instansen
     */
    public DataModel getModel() {
        return model;
    }
}