package model.util;

import model.logic.DataManager;

/**
 * Factory-klasse til oprettelse og håndtering af model-komponenterne.
 * Implementerer Singleton pattern for at sikre én instans på tværs af systemet.
 * Dette er den primære factory der skal bruges af ViewModelFactory i MVVM-arkitekturen.
 */
public class ModelFactory {
    private static ModelFactory instance;
    private static final Object lock = new Object();

    private final DataManager dataManager;

    /**
     * Privat konstruktør, sikrer Singleton pattern
     */
    private ModelFactory() {
        this.dataManager = new DataManager();
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
     * Returnerer DataManager instansen
     *
     * @return DataManager instansen
     */
    public DataManager getDataManager() {
        return dataManager;
    }
}