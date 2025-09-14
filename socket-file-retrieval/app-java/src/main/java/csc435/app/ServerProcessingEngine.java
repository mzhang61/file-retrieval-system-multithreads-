package csc435.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerProcessingEngine {
    // TO-DO keep track of the Dispatcher thread
    // TO-DO keep track of the server worker threads
    // TO-DO keep track of the clients information
    private IndexStore store;
    private Thread dispatcherThread;
    private final List<Thread> workerThreads = Collections.synchronizedList(new ArrayList<>());
    private final List<String> clientInfo = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public ServerProcessingEngine(IndexStore store) {
        this.store = store;
    }

    public void initialize(int serverPort) {
        // TO-DO create and start the Dispatcher thread
        dispatcherThread = new Thread(new Dispatcher(this, serverPort));
        dispatcherThread.start();
    }

    public void spawnWorker(ServerWorker worker) {
        // TO-DO create and start a new Index Worker thread
        Thread workerThread = new Thread(worker);
        workerThreads.add(workerThread);
        workerThread.start();
    }

    public void shutdown() {
        // TO-DO signal the Dispatcher thread to shutdown
        // TO-DO join the Dispatcher and Index Worker threads
        isRunning.set(false);
        try {
            dispatcherThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Thread t : workerThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> getConnectedClients() {
        // TO-DO return the connected clients information
        return new ArrayList<String>(clientInfo);
    }

    public IndexStore getIndexStore() {
        return this.store;
    }

     // ServerProcessingEngine.java
    public void addClientInfo(String info) {
        clientInfo.add(info);
    }

}
