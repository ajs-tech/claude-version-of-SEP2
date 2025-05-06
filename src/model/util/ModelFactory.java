//package model.util;
//
//import model.logic.DataManager;
//import model.logic.DataModel;
//
///**
// * Factory-klasse til oprettelse og håndtering af model-komponenter.
// * Implementerer Singleton pattern for at sikre én instans på tværs af systemet.
// */
//public class ModelFactory {
//    private static ModelFactory instance;
//    private static final Object lock = new Object();
//
//    private final DataModel model; // Brug interface i stedet for konkret klasse
//
//    /**
//     * Privat konstruktør, sikrer Singleton pattern
//     */
//    private ModelFactory() {
//        this.model = new DataManager(); // Konkret implementation
//    }
//
//    /**
//     * Returnerer den eksisterende instans eller opretter en ny
//     *
//     * @return ModelFactory instansen
//     */
//    public static ModelFactory getInstance() {
//        if (instance == null) {
//            synchronized (lock) {
//                if (instance == null) {
//                    instance = new ModelFactory();
//                }
//            }
//        }
//        return instance;
//    }
//
//    /**
//     * Returnerer model-interface instansen
//     *
//     * @return ILaptopSystemModel instansen
//     */
//    public DataModel getModel() {
//        return model;
//    }
//}

package model.util;

import model.logic.DataManager;
import model.logic.DataModel;
import model.mediator.LaptopClient;
import model.mediator.ServerModel;

public class ModelFactory {
    private static ModelFactory instance;
    private static final Object lock = new Object();

    private final DataModel model;
    private final ServerModel serverModel;

    /**
     * Privat konstruktør, sikrer Singleton pattern
     */
    private ModelFactory() {
        // Opret ServerModel (socket-klient)
        this.serverModel = new LaptopClient();

        // Opret DataModel med reference til ServerModel
        this.model = new DataManager(serverModel);
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
     * @return DataModel instansen
     */
    public DataModel getModel() {
        return model;
    }

    /**
     * Returnerer ServerModel instansen
     *
     * @return ServerModel instansen
     */
    public ServerModel getServerModel() {
        return serverModel;
    }

    /**
     * Lukker forbindelsen til serveren
     */
    public void closeConnection() {
        try {
            if (model instanceof DataManager) {
                ((DataManager) model).closeConnection();
            }
        } catch (Exception e) {
            System.err.println("Fejl ved lukning af forbindelse: " + e.getMessage());
        }
    }
}