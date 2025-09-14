package csc435.app;

public class FileRetrievalServer
{
    public static void main( String[] args )
    {
        // TO-DO change server port to a non-privileged port from args[0]
        int serverPort = 12345;
        if (args.length > 0) {
            try {
                serverPort = Integer.parseInt(args[0]);
                if (serverPort < 1024 || serverPort > 65535) {
                    System.out.println("Please provide a non-priviledge port number");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number, Please provide a valid integer.");
                return;
            }
        } else {
            System.out.println("Usage: java FileRetrievalServer");
            return;
        }
        IndexStore store = new IndexStore();
        ServerProcessingEngine engine = new ServerProcessingEngine(store);
        ServerAppInterface appInterface = new ServerAppInterface(engine);
        
        // create a thread that creates and server TCP/IP socket and listenes to connections
        engine.initialize(serverPort);

        // read commands from the user
        appInterface.readCommands();
    }
}
