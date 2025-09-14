package csc435.app;

import io.grpc.ServerBuilder;

import java.io.IOException;

public class RPCServerWorker implements Runnable {
    private IndexStore store;
    // TO-DO keep track of the gRPC Server object
    private io.grpc.Server server;
    private final int port;
    public RPCServerWorker(IndexStore store, int port) {
        this.store = store;
        this.port = port;
    }

    @Override
    public void run() {
        // TO-DO build the gRPC Server
        // TO-DO register the FileRetrievalEngineService service with the gRPC Server
        // TO-DO start the gRPC Server
        try {
            server = ServerBuilder.forPort(port)
                    .addService(new FileRetrievalEngineService(store))
                    .build()
                    .start();

            System.out.println("Server started on port " + port);
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        // TO-DO shutdown the gRPC server
        // TO-DO wait for the gRPC server to shutdown
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
