package csc435.app;

public class FileRetrievalServer
{
    public static void main(String[] args)
    {
        // TO-DO change server port to a non-privileged port from args[0]
        // TO-DO read the number of worker threads from args[1]
        if (args.length < 2) {
            System.out.println("Usage: java FileRetrievalServer <port> <numWorkerThreads>");
            return;
        }
        int serverPort = Integer.parseInt(args[0]);
        int numWorkerThreads = Integer.parseInt(args[1]);

        IndexStore store = new IndexStore();
        ServerProcessingEngine engine = new ServerProcessingEngine(store);
        ServerAppInterface appInterface = new ServerAppInterface(engine);

        engine.initialize(serverPort, numWorkerThreads);
        appInterface.readCommands();
    }
}
