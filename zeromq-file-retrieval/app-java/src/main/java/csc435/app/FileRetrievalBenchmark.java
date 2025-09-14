package csc435.app;

import java.util.ArrayList;
import java.util.List;

class BenchmarkWorker implements Runnable {
    // TO-DO keep track of a ClientProcessingEngine object
    private String datasetPath;
    private String serverIP;
    private String serverPort;
    private ClientProcessingEngine engine;

    public BenchmarkWorker(String datasetPath, String serverIP, String serverPort) {
        this.datasetPath = datasetPath;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        // TO-DO create a ClientProcessinEngine object
        // TO-DO connect the ClientProcessingEngine to the server
        // TO-DO index the dataset
        engine = new ClientProcessingEngine();
        engine.connect(serverIP, serverPort);

        IndexResult result = engine.indexFolder(datasetPath);
        System.out.println("Completed indexing " + result.totalBytesRead + " bytes of data");
        System.out.println("Completed indexing in " + result.executionTime + " seconds");
    }

    public void search() {
        // TO-DO perform search on the ClientProcessingEngine object
        // TO-DO print the results and performance
        ArrayList<String> queries = new ArrayList<>(List.of(
                "the", "child-like", "vortex", "moon AND vortex", "distortion AND adaptation"
        ));
        for (String q : queries) {
            ArrayList<String> terms = new ArrayList<>(List.of(q.split("\\s+")));
            SearchResult s = engine.search(terms);
            System.out.printf("Searching search %s\n", q);
            System.out.printf("Search completed in %.2f seconds\n", s.executionTime);
            System.out.printf("Search results (top 10 out of %d):\n", s.totalMatchingDocuments);
            for (DocPathFreqPair doc : s.documentFrequencies) {
                System.out.printf("* %s:%d\n", doc.documentPath, doc.wordFrequency);
            }
        }
    }

    public void disconnect() {
        // TO-DO disconnect the ClientProcessingEngine object from the server
        if (engine != null) {
            engine.disconnect();
        }
    }
}

public class FileRetrievalBenchmark {
    public static void main(String[] args)
    {
        // TO-DO extract the arguments from args
        // TO-DO measure the execution start time
        // TO-DO create Benchmark Worker objects equal to the number of clients
        // TO-DO create and start benchmark worker threads equal to the number of clients
        // TO-DO join the benchmark worker threads
        // TO-DO measure the execution stop time and print the performance
        // TO-DO run search queries on the first client
        // TO-DO disconnect all clients
        if (args.length < 3) {
            System.out.println("Usage: java FileRetrievalBenchmark <serverIP> <serverPort> <numberOfClients> <client1Path> <client2Path> ...");
            return;
        }

        String serverIP = args[0];
        String serverPort = args[1];
        int numberOfClients = Integer.parseInt(args[2]);

        if (args.length < 3 + numberOfClients) {
            System.out.println("Error: Not enough client dataset paths provided.");
            return;
        }

        ArrayList<BenchmarkWorker> workers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();

        long start = System.nanoTime();

        for (int i = 0; i < numberOfClients; i++) {
            String datasetPath = args[3 + i];
            BenchmarkWorker worker = new BenchmarkWorker(datasetPath, serverIP, serverPort);
            Thread t = new Thread(worker);
            workers.add(worker);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long end = System.nanoTime();
        double execTime = (end - start) / 1e9;
        System.out.printf("Total indexing time: %.3f seconds\n", execTime);

        if (!workers.isEmpty()) {
            System.out.println("\nRunning search queries on client 1:");
            workers.get(0).search();
        }

        // disconnect all workers
        for (BenchmarkWorker worker : workers) {
            worker.disconnect();
        }
    }
    }

