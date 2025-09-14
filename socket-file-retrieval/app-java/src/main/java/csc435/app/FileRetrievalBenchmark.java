package csc435.app;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

class BenchmarkWorker implements Runnable {
    // TO-DO declare a ClientProcessingEngine
    private ClientProcessingEngine engine;
    private final String serverIP;
    private final String serverPort;
    private final String datasetPath;
    private long totalBytes;

    public BenchmarkWorker(String serverIP, String serverPort, String datasetPath) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.datasetPath = datasetPath;
        this.engine = new ClientProcessingEngine();
        this.totalBytes = 0;
    }
    @Override
    public void run() {
        // TO-DO create a ClientProcessinEngine
        // TO-DO connect the ClientProcessingEngine to the server
        // TO-DO index the dataset
        engine.connect(serverIP, serverPort);
        IndexResult result = engine.indexFolder(datasetPath);
        this.totalBytes = result.totalBytesRead;
    }

    public void search(String query) {
        // TO-DO perform search operations on the ClientProcessingEngine
        // TO-DO print the results and performance

        String[] rawTerms = query.trim().split("\\s+AND\\s+|");
        ArrayList<String> terms = new ArrayList<>();
        for (String term : rawTerms) {
            if (!term.isBlank()) {
                terms.add(term.toLowerCase());
            }
        }
        SearchResult result = engine.search(terms);
        System.out.printf("Searching search %s\n", query);
        System.out.printf("Search completed in %.1f seconds\n", result.executionTime);
        System.out.printf("Search results (top 10 out of %d):\n", result.documentFrequencies.size());
        for (DocPathFreqPair pair : result.documentFrequencies) {
            System.out.printf("* %s:%d\n", pair.documentPath, pair.wordFrequency);
        }
    }

    public void disconnect() {
        // TO-DO disconnect the ClientProcessingEngine from the server
        engine.disconnect();
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}

public class FileRetrievalBenchmark {
    public static void main(String[] args)
    {
        if (args.length < 4) {
            System.out.println("Usage: java FileRetrievalBenchmark <serverIP> <serverPort> <numberOfClients> <dataset1> ... <datasetN>");
            return;
        }
        String serverIP = args[0];
        String serverPort = args[1];
        int numberOfClients = Integer.parseInt(args[2]);

        ArrayList<String> clientsDatasetPath = new ArrayList<>();

        // TO-DO extract the arguments from args
        for (int i = 3; i < args.length; i++) {
            clientsDatasetPath.add(args[i]);
        }

        if (clientsDatasetPath.size() != numberOfClients) {
            System.out.println("Error: number of dataset paths must match the number of clients.");
            return;
        }

        ArrayList<BenchmarkWorker> workers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();

        // TO-DO measure the execution start time
        LocalTime start = LocalTime.now();

        // TO-DO create and start benchmark worker threads equal to the number of clients
        for (int i = 0; i < numberOfClients; i++) {
            BenchmarkWorker worker = new BenchmarkWorker(serverIP, serverPort, clientsDatasetPath.get(i));
            Thread thread = new Thread(worker);
            workers.add(worker);
            threads.add(thread);
            thread.start();
        }

        // TO-DO join the benchmark worker threads
        for (Thread t : threads) {
            try {
                t.join();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        // TO-DO measure the execution stop time and print the performance
        LocalTime end = LocalTime.now();
        Duration duration = Duration.between(start, end);

        long totalBytes = 0;
        for (BenchmarkWorker worker : workers) {
            totalBytes += worker.getTotalBytes();
        }

        double totalSeconds = duration.toMillis() / 1000.0;
        double throughput = totalBytes / 1_000_000.0 / totalSeconds;

        System.out.printf("Completed indexing %d bytes of data\n", totalBytes);
        System.out.printf("Completed indexing in %.3f seconds\n", totalSeconds);
        System.out.printf("Indexing throughput: %.3f MB/s\n", throughput);

        // TO-DO run search queries on the first client (benchmark worker thread number 1)
        if (!workers.isEmpty()) {
            BenchmarkWorker first = workers.get(0);
            first.search("the");
            first.search("child-like");
            first.search("vortex");
            first.search("moon AND vortex");
            first.search("distortion AND adaptation");
        }

        // TO-DO disconnect all clients (all benchmakr worker threads)
        for (BenchmarkWorker worker : workers) {
            worker.disconnect();
        }
    }
}
