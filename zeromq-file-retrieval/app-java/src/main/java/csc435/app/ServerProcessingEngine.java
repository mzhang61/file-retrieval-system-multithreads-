package csc435.app;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ServerProcessingEngine {
    private IndexStore store;
    private ZContext context;
    private ZMQ.Socket frontend;
    private Thread[] workers;
    private boolean running = false;

    public ServerProcessingEngine(IndexStore store) {
        this.store = store;
        this.context = new ZContext();
    }

    public void initialize(int port, int numWorkerThreads) {
        // TO-DO initialize the ZMQ context
        // TO-DO create a router socket and bind it to the specified port
        // TO-DO create a dealer socket and bind it to the inproc://backend endpoint
        // TO-DO create worker threads and start them
        frontend = context.createSocket(SocketType.ROUTER);
        frontend.bind("tcp://*:" + port);

        ZMQ.Socket backend = context.createSocket(SocketType.DEALER);
        backend.bind("inproc://backend");

        workers = new Thread[numWorkerThreads];
        for (int i = 0; i < numWorkerThreads; i++) {
            ServerWorker worker = new ServerWorker(store, context);
            workers[i] = new Thread(worker);
            workers[i].start();
        }

        // Start proxy thread
        running = true;
        new Thread(() -> {
            try {
                ZMQ.proxy(frontend, backend, null);
            } catch (Exception e) {
                // Proxy terminated
            }
        }).start();

        System.out.println("Server initialized with " + numWorkerThreads + " worker threads on port " + port);
    }

    public void shutdown() {
        // TO-DO close the router socket
        // TO-DO close the dealer socket
        // TO-DO interrupt the worker threads
        // TO-DO join the worker threads
        // TO-DO close the ZMQ context
        running = false;

        if (frontend != null) {
            frontend.close();
        }

        if (workers != null) {
            for (Thread worker : workers) {
                if (worker != null) {
                    worker.interrupt();
                    try {
                        worker.join(1000);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }

        if (context != null) {
            context.close();
        }

        System.out.println("Server shutdown complete");
    }
}
