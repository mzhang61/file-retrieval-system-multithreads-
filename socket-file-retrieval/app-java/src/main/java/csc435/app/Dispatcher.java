package csc435.app;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Dispatcher implements Runnable {
    private final ServerProcessingEngine engine;
    private final int serverPort;

    public Dispatcher(ServerProcessingEngine engine, int serverPort) {
        this.engine = engine;
        this.serverPort = serverPort;
    }
    
    @Override
    public void run() {
        // TO-DO create a TCP/IP socket and listen for new connections
        // TO-DO When new connection comes through create a new Index Worker thread for the new connection
        // TO-DO Use the engine spawnWorker method to create a new Index Worker thread
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Dispatcher started. Listening for client connections...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ServerWorker worker = new ServerWorker(engine.getIndexStore(), clientSocket, engine);
                    engine.spawnWorker(worker);
                } catch (IOException e) {
                    System.out.println("Failed to accept connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server socket: " + e.getMessage());
        }
    }
}