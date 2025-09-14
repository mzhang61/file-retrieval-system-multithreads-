package csc435.app;

import java.util.ArrayList;
import java.util.List;

class BenchmarkWorker implements Runnable {
    // TO-DO declare a ClientProcessingEngine
    private final String serverIP;
    private final String serverPort;
    private final String datasetPath;
    private final int workerId;
    private ClientProcessingEngine engine;

    public BenchmarkWorker(String serverIP, String serverPort, String datasetPath, int workerId) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.datasetPath = datasetPath;
        this.workerId = workerId;
    }

    @Override
    public void run() {
        // TO-DO create a ClientProcessinEngine
        // TO-DO connect the ClientProcessingEngine to the server
        // TO-DO index the dataset
        engine = new ClientProcessingEngine();
        engine.connect(serverIP, serverPort);
        System.out.printf("Client %d connected.\n", workerId);

        IndexResult result = engine.indexFolder(datasetPath);
        System.out.printf("Client %d: Completed indexing %d bytes of data\n", workerId, result.totalBytesRead);
        System.out.printf("Client %d: Completed indexing in %.3f seconds\n", workerId, result.executionTime);
    }

    public void search() {
        // TO-DO perform search operations on the ClientProcessingEngine
        // TO-DO print the results and performance
        /*
        ArrayList<String> query1 = new ArrayList<>();
        query1.add("child-like");

        SearchResult result1 = engine.search(query1);
        System.out.printf("Search completed in %.2f seconds\n", result1.executionTime);
        System.out.println("Search results (top 10 out of " + result1.topDocuments.size() + "):");
        for (DocPathFreqPair doc : result1.topDocuments) {
            System.out.printf("* %s:%d\n", doc.documentPath, doc.wordFrequency);
        }

        ArrayList<String> query2 = new ArrayList<>();
        query2.add("moon");
        query2.add("vortex");

        SearchResult result2 = engine.search(query2);
        System.out.printf("Search completed in %.2f seconds\n", result2.executionTime);
        System.out.println("Search results (top 10 out of " + result2.topDocuments.size() + "):");
        for (DocPathFreqPair doc : result2.topDocuments) {
            System.out.printf("* %s:%d\n", doc.documentPath, doc.wordFrequency);
        }

         */
        runQuery("search the", List.of("the"));
        runQuery("search child-like", List.of("child-like"));
        runQuery("search vortex", List.of("vortex"));
        runQuery("search moon AND vortex", List.of("moon", "vortex"));
        runQuery("search distortion AND adaptation", List.of("distortion", "adaptation"));
    }

    private void runQuery(String label, List<String> terms) {
        System.out.println("Searching " + label);
        SearchResult result = engine.search(new ArrayList<>(terms));
        System.out.printf("Search completed in %.2f seconds\n", result.executionTime);
        System.out.printf("Search results (top 10 out of %d):\n", result.totalCount);
        for (DocPathFreqPair doc : result.topDocuments) {
            System.out.printf("* %s:%d\n", doc.documentPath, doc.wordFrequency);
        }
    }

    public void disconnect() {
        // TO-DO disconnect the ClientProcessingEngine from the server
        if (engine != null) {
            engine.disconnect();
        }
    }
}

public class FileRetrievalBenchmark {
    public static void main(String[] args)
    {
        if (args.length < 3) {
            System.out.println("Usage: <serverIP> <serverPort> <numClients> <datasetPath1> ...");
            return;
        }
        String serverIP = args[0];
        String serverPort = args[1];
        int numberOfClients = Integer.parseInt(args[2]);
        ArrayList<BenchmarkWorker> workers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        // TO-DO extract the arguments from args
        // TO-DO measure the execution start time
        // TO-DO create and start benchmark worker threads equal to the number of clients
        // TO-DO join the benchmark worker threads
        // TO-DO measure the execution stop time and print the performance
        // TO-DO run search queries on the first client (benchmark worker thread number 1)
        // TO-DO disconnect all clients (all benchmakr worker threads)
        // Start timer
        long start = System.nanoTime();

        for (int i = 0; i < numberOfClients; i++) {
            String datasetPath = args[3 + i];
            BenchmarkWorker worker = new BenchmarkWorker(serverIP, serverPort, datasetPath, i + 1);
            Thread t = new Thread(worker);
            workers.add(worker);
            threads.add(t);
            t.start();
        }

        // Wait for all threads
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // End timer
        long end = System.nanoTime();
        double totalTime = (end - start) / 1_000_000_000.0;
        System.out.printf("Total indexing time: %.3f seconds\n", totalTime);

        // Run search queries on first client
        if (!workers.isEmpty()) {
            workers.get(0).search();
        }

        // Clean up
        for (BenchmarkWorker worker : workers) {
            worker.disconnect();
        }
    }
}