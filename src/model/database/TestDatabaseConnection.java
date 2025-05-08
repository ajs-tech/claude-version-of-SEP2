package model.database;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        boolean connected = DatabaseConnection.getInstance().testConnection();
        System.out.println("Connection successful: " + connected);

        if (connected) {
            System.out.println(DatabaseConnection.getInstance().getConnectionStats());
        }

        // Luk pool før programmet afsluttes
        DatabaseConnection.closePool();
    }
}