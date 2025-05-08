

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Network service for client-server communication using sockets.
 * Implements both request/reply and broadcast communication patterns.
 * Utilizes threading for concurrent client handling.
 */
public class NetworkService extends Observable {
    private static final Logger logger = Logger.getLogger(NetworkService.class.getName());
    
    // Event types for observer notifications
    public static final String EVENT_CLIENT_CONNECTED = "CLIENT_CONNECTED";
    public static final String EVENT_CLIENT_DISCONNECTED = "CLIENT_DISCONNECTED";
    public static final String EVENT_MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
    public static final String EVENT_ERROR = "ERROR";
    
    // Default port
    private static final int DEFAULT_PORT = 8080;
    
    // Multiton pattern - one instance per port
    private static final Map<Integer, NetworkService> instances = new ConcurrentHashMap<>();
    
    private final int port;
    private ServerSocket serverSocket;
    private final Set<ClientHandler> clients;
    private final ExecutorService executor;
    private boolean running;
    
    /**
     * Private constructor for Multiton pattern.
     * 
     * @param port Port to use for the server
     */
    private NetworkService(int port) {
        this.port = port;
        this.clients = ConcurrentHashMap.newKeySet();
        this.executor = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    /**
     * Gets a NetworkService instance for a specific port.
     * 
     * @param port Port to use
     * @return NetworkService instance
     */
    public static synchronized NetworkService getInstance(int port) {
        return instances.computeIfAbsent(port, NetworkService::new);
    }
    
    /**
     * Gets a NetworkService instance with the default port.
     * 
     * @return NetworkService instance
     */
    public static NetworkService getInstance() {
        return getInstance(DEFAULT_PORT);
    }
    
    /**
     * Starts the server.
     * 
     * @throws IOException if the server socket cannot be created
     */
    public void startServer() throws IOException {
        if (running) {
            return;
        }
        
        serverSocket = new ServerSocket(port);
        running = true;
        
        logger.info("Server started on port " + port);
        
        // Accept client connections in a separate thread
        executor.submit(() -> {
            while (running) {
                try {
                    // Wait for a client to connect
                    Socket clientSocket = serverSocket.accept();
                    
                    // Create and start a new client handler
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    executor.submit(clientHandler);
                    
                    logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // Notify observers
                    setChanged();
                    notifyObservers(new NetworkEvent(EVENT_CLIENT_CONNECTED, 
                            clientSocket.getInetAddress().getHostAddress()));
                    
                } catch (IOException e) {
                    if (running) {
                        logger.log(Level.SEVERE, "Error accepting client connection", e);
                        
                        // Notify observers
                        setChanged();
                        notifyObservers(new ErrorEvent(EVENT_ERROR, 
                                "Error accepting client connection", e));
                    }
                }
            }
        });
    }
    
    /**
     * Stops the server.
     */
    public void stopServer() {
        running = false;
        
        // Close all client connections
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();
        
        // Close the server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing server socket", e);
            }
        }
        
        logger.info("Server stopped");
    }
    
    /**
     * Broadcasts a message to all connected clients.
     * 
     * @param message Message to broadcast
     */
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        
        logger.info("Broadcast message to " + clients.size() + " clients: " + message);
    }
    
    /**
     * Sends a message to a specific client.
     * 
     * @param clientId Client identifier (typically IP address)
     * @param message Message to send
     * @return true if the message was sent, false if the client wasn't found
     */
    public boolean sendMessage(String clientId, String message) {
        for (ClientHandler client : clients) {
            if (client.getClientId().equals(clientId)) {
                client.sendMessage(message);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the number of connected clients.
     * 
     * @return Number of connected clients
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Gets a list of connected client IDs.
     * 
     * @return List of client IDs
     */
    public List<String> getConnectedClients() {
        List<String> clientIds = new ArrayList<>();
        for (ClientHandler client : clients) {
            clientIds.add(client.getClientId());
        }
        return clientIds;
    }
    
    /**
     * Client handler for individual client connections.
     * Implements Runnable for thread execution.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;
        private PrintWriter out;
        private BufferedReader in;
        private boolean connected;
        
        /**
         * Creates a new client handler.
         * 
         * @param socket Client socket
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientId = socket.getInetAddress().getHostAddress();
            this.connected = true;
        }
        
        /**
         * Gets the client ID.
         * 
         * @return Client ID (IP address)
         */
        public String getClientId() {
            return clientId;
        }
        
        /**
         * Sends a message to the client.
         * 
         * @param message Message to send
         */
        public void sendMessage(String message) {
            if (connected && out != null) {
                out.println(message);
                out.flush();
            }
        }
        
        /**
         * Closes the client connection.
         */
        public void close() {
            connected = false;
            
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (!socket.isClosed()) socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing client connection", e);
            }
        }
        
        /**
         * Runs the client handler thread.
         */
        @Override
        public void run() {
            try {
                // Set up input and output streams
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                String inputLine;
                while (connected && (inputLine = in.readLine()) != null) {
                    final String message = inputLine;
                    
                    logger.info("Received message from " + clientId + ": " + message);
                    
                    // Notify observers
                    setChanged();
                    notifyObservers(new MessageEvent(EVENT_MESSAGE_RECEIVED, clientId, message));
                }
            } catch (IOException e) {
                if (connected) {
                    logger.log(Level.WARNING, "Error in client connection", e);
                    
                    // Notify observers
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_ERROR, 
                            "Error in client connection: " + clientId, e));
                }
            } finally {
                close();
                clients.remove(this);
                
                logger.info("Client disconnected: " + clientId);
                
                // Notify observers
                setChanged();
                notifyObservers(new NetworkEvent(EVENT_CLIENT_DISCONNECTED, clientId));
            }
        }
    }
    
    /**
     * Client for connecting to a server.
     */
    public static class Client extends Observable implements Observer {
        private static final Logger logger = Logger.getLogger(Client.class.getName());
        
        // Event types for observer notifications
        public static final String EVENT_CONNECTED = "CONNECTED";
        public static final String EVENT_DISCONNECTED = "DISCONNECTED";
        public static final String EVENT_MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
        public static final String EVENT_ERROR = "ERROR";
        
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean connected;
        private final String host;
        private final int port;
        private final ExecutorService executor;
        
        /**
         * Creates a new client.
         * 
         * @param host Server host
         * @param port Server port
         */
        public Client(String host, int port) {
            this.host = host;
            this.port = port;
            this.connected = false;
            this.executor = Executors.newSingleThreadExecutor();
        }
        
        /**
         * Creates a new client with the default port.
         * 
         * @param host Server host
         */
        public Client(String host) {
            this(host, DEFAULT_PORT);
        }
        
        /**
         * Connects to the server.
         * 
         * @return true if the connection was successful
         */
        public boolean connect() {
            if (connected) {
                return true;
            }
            
            try {
                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                
                logger.info("Connected to server: " + host + ":" + port);
                
                // Notify observers
                setChanged();
                notifyObservers(new NetworkEvent(EVENT_CONNECTED, host + ":" + port));
                
                // Start listening for messages
                executor.submit(this::listenForMessages);
                
                return true;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error connecting to server", e);
                
                // Notify observers
                setChanged();
                notifyObservers(new ErrorEvent(EVENT_ERROR, "Error connecting to server", e));
                
                return false;
            }
        }
        
        /**
         * Disconnects from the server.
         */
        public void disconnect() {
            if (!connected) {
                return;
            }
            
            connected = false;
            
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error disconnecting from server", e);
            }
            
            logger.info("Disconnected from server");
            
            // Notify observers
            setChanged();
            notifyObservers(new NetworkEvent(EVENT_DISCONNECTED, host + ":" + port));
        }
        
        /**
         * Sends a message to the server.
         * 
         * @param message Message to send
         * @return true if the message was sent
         */
        public boolean sendMessage(String message) {
            if (!connected || out == null) {
                return false;
            }
            
            out.println(message);
            out.flush();
            
            logger.info("Sent message to server: " + message);
            
            return true;
        }
        
        /**
         * Listens for messages from the server.
         */
        private void listenForMessages() {
            try {
                String inputLine;
                while (connected && (inputLine = in.readLine()) != null) {
                    final String message = inputLine;
                    
                    logger.info("Received message from server: " + message);
                    
                    // Notify observers
                    setChanged();
                    notifyObservers(new MessageEvent(EVENT_MESSAGE_RECEIVED, host + ":" + port, message));
                }
            } catch (IOException e) {
                if (connected) {
                    logger.log(Level.WARNING, "Error in server connection", e);
                    
                    // Notify observers
                    setChanged();
                    notifyObservers(new ErrorEvent(EVENT_ERROR, "Error in server connection", e));
                    
                    // Disconnect
                    disconnect();
                }
            }
        }
        
        /**
         * Checks if the client is connected.
         * 
         * @return true if connected
         */
        public boolean isConnected() {
            return connected;
        }
        
        /**
         * Closes the client and releases resources.
         */
        public void close() {
            disconnect();
            executor.shutdown();
        }
        
        /**
         * Handles updates from observed objects.
         */
        @Override
        public void update(Observable o, Object arg) {
            // Forward events to observers
            if (arg instanceof NetworkEvent) {
                setChanged();
                notifyObservers(arg);
            }
        }
    }
    
    /**
     * Base event class for network operations.
     */
    public static class NetworkEvent {
        private final String eventType;
        private final Object data;
        
        public NetworkEvent(String eventType, Object data) {
            this.eventType = eventType;
            this.data = data;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public Object getData() {
            return data;
        }
    }
    
    /**
     * Event class for message events.
     */
    public static class MessageEvent extends NetworkEvent {
        private final String sender;
        private final String message;
        
        public MessageEvent(String eventType, String sender, String message) {
            super(eventType, message);
            this.sender = sender;
            this.message = message;
        }
        
        public String getSender() {
            return sender;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Event class for error events.
     */
    public static class ErrorEvent extends NetworkEvent {
        private final Exception exception;
        
        public ErrorEvent(String eventType, String message, Exception exception) {
            super(eventType, message);
            this.exception = exception;
        }
        
        public Exception getException() {
            return exception;
        }
    }
}