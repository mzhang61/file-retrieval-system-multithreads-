package csc435.app;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerWorker implements Runnable {
    private final IndexStore store;
    private final Socket socket;
    private static long clientCounter = 0;
    private long clientID;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final ServerProcessingEngine engine;
    public ServerWorker(IndexStore store, Socket socket, ServerProcessingEngine engine) throws IOException {
        this.store = store;
        this.socket = socket;
        this.engine = engine;
        this.socket.setKeepAlive(true);
        this.socket.setSoTimeout(30000000);
        System.out.println("Initializing streams for: " + socket.getRemoteSocketAddress());
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        
    }

    @Override
    public void run() {
        // TO-DO receive a message from the client
        // TO-DO if the message is a REGISTER REQUEST, then
        //       generate a new client ID and return a REGISTER REPLY message containing the client ID
        // TO-DO if the message is an INDEX REQUEST, then
        //       extract the document path, client ID and word frequencies from the message(s)
        //       get the document number associated with the document path (call putDocument)
        //       update the index store with the word frequencies and the document number
        //       return an acknowledgement INDEX REPLY message
        // TO-DO if the message is a SEARCH REQUEST, then
        //       extract the terms from the message
        //       for each term get the pairs of documents and frequencies from the index store
        //       combine the returned documents and frequencies from all of the specified terms
        //       sort the document and frequency pairs and keep only the top 10
        //       for each document number get from the index store the document path
        //       return a SEARCH REPLY message containing the top 10 results
        // TO-DO if the message is a QUIT message, then finish running
        System.out.println("Starting worker thread for client");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String messageType = (String) in.readObject();
                System.out.println("Received [" + messageType + "] from client");

                switch (messageType) {
                    case "REGISTER REQUEST":
                        handleRegisterRequest();
                        break;
                    case "INDEX REQUEST":
                        handleIndexRequest();
                        break;
                    case "SEARCH REQUEST":
                        handleSearchRequest();
                        break;
                    case "QUIT":
                        System.out.println("Client " + clientID + " requested disconnect");
                        return;
                    default:
                        System.out.println("Unknown message type: " + messageType);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling client " + clientID + ": " +
                    e.getClass().getSimpleName());
            e.printStackTrace();
        } finally {
            cleanupResources();
        }
    }

    private void handleRegisterRequest() throws IOException {
        synchronized (ServerWorker.class) {
            clientID = ++clientCounter;
        }
        out.writeObject("REGISTER REPLY");
        out.writeLong(clientID);
        out.flush();

        String clientInfoStr = "client " + clientID + ":" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        // ServerWorker.java
        engine.addClientInfo(clientInfoStr);

        System.out.println("Assigned client ID: " + clientID);
    }

    private void handleIndexRequest() throws IOException, ClassNotFoundException {
        long clientId = in.readLong();
        String documentPath = (String) in.readObject();
        int wordCount = in.readInt();

        HashMap<String, Long> wordFrequencies = new HashMap<>();
        for (int i = 0; i < wordCount; i++) {
            String word = (String) in.readObject();
            long frequency = in.readLong();
            wordFrequencies.put(word, frequency);
        }

        long docNumber = store.putDocument(documentPath);
        store.updateIndex(docNumber, wordFrequencies);

        out.writeObject("INDEX REPLY");
        out.flush();
        System.out.println("Processed index request for doc: " + documentPath);
    }

    private void handleSearchRequest() throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        ArrayList<String> terms = (ArrayList<String>) in.readObject();

        HashMap<Long, Long> docFrequencyMap = new HashMap<>();
        for (String term : terms) {
            ArrayList<DocFreqPair> freqPairs = store.lookupIndex(term);
            for (DocFreqPair pair : freqPairs) {
                docFrequencyMap.put(pair.documentNumber, docFrequencyMap.getOrDefault(pair.documentNumber, 0L) + pair.wordFrequency);
            }
        }
        ArrayList<DocPathFreqPair> results = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : docFrequencyMap.entrySet()) {
            String path = store.getDocument(entry.getKey());
            results.add(new DocPathFreqPair(path, entry.getValue()));
        }

        results.sort((a, b) -> Long.compare(b.wordFrequency, a.wordFrequency));


        out.writeObject(results);
        out.flush();
    }

    private void cleanupResources() {
        System.out.println("Cleaning up resources for client " + clientID);
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Error closing output stream");
        }
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            System.err.println("Error closing input stream");
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket");
        }
    }
}