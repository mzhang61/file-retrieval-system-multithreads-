package csc435.app;

public class FileRetrievalServer
{
    public static void main( String[] args )
    {
        // TO-DO change server port to a non-privileged port from args[0]
        if (args.length != 1) {
            System.err.println("Usage: java FileRetrievalServer <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        IndexStore store = new IndexStore();
        ServerProcessingEngine engine = new ServerProcessingEngine(store);
        engine.initialize(port);

        ServerAppInterface app = new ServerAppInterface(engine);
        app.readCommands();
    }
}
