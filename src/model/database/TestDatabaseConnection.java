package model.database;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        boolean connected = DatabaseConnection.testConnection();
        System.out.println("Connection successful: " + connected);

        if (connected) {
            System.out.println(DatabaseConnection.getPoolStats());
        }

        // Luk pool før programmet afsluttes
        DatabaseConnection.closePool();
    }
}