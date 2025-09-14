package csc435.app;

import csc435.app.FileRetrievalEngineGrpc.FileRetrievalEngineBlockingStub;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class IndexResult {
    public double executionTime;
    public long totalBytesRead;

    public IndexResult(double executionTime, long totalBytesRead) {
        this.executionTime = executionTime;
        this.totalBytesRead = totalBytesRead;
    }
}

class DocPathFreqPair {
    public String documentPath;
    public long wordFrequency;

    public DocPathFreqPair(String documentPath, long wordFrequency) {
        this.documentPath = documentPath;
        this.wordFrequency = wordFrequency;
    }
}

class SearchResult {
    public double executionTime;
    public ArrayList<DocPathFreqPair> topDocuments;
    public int totalCount;

    public SearchResult(double executionTime, ArrayList<DocPathFreqPair> topDocuments, int totalCount) {
        this.executionTime = executionTime;
        this.topDocuments = topDocuments;
        this.totalCount = totalCount;
    }
}

public class ClientProcessingEngine {
    // TO-DO keep track of the connection
    ManagedChannel channel;
    FileRetrievalEngineBlockingStub stub;
    private long clientId = -1;

    public ClientProcessingEngine() { }

    public IndexResult indexFolder(String folderPath) {

        // TO-DO get the start time
        // TO-DO crawl the folder path and extrac all file paths
        // TO-DO for each file extract all terms/words and count their frequencies
        // TO-DO increment the total number of bytes read
        // TO-DO for each file perform a remote procedure call to the server by calling the gRPC client stub
        // TO-DO get the stop time and calculate the execution time
        // TO-DO return the execution time and the total number of bytes read
        long totalBytes = 0;
        long startTime = System.nanoTime();

        File folder = new File(folderPath);
        List<File> files = listFilesRecursively(folder);

        for (File file : files) {
            if (!file.isFile()) continue;

            HashMap<String, Long> wordFreq = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalBytes += line.getBytes().length;

                    // Tokenize
                    String[] tokens = line.split("[^a-zA-Z0-9_-]+");
                    for (String token : tokens) {
                        if (token.length() > 3) {
                            wordFreq.put(token, wordFreq.getOrDefault(token, 0L) + 1);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            // Send index request to server
            Path folderAbs = folder.getAbsoluteFile().toPath();
            IndexReq.Builder request = IndexReq.newBuilder()
                    .setClientId((int) clientId)
                    .setDocumentPath("client " + clientId + ":" + folderAbs.relativize(file.getAbsoluteFile().toPath()).toString())
                    .putAllWordFrequencies(wordFreq);

            stub.computeIndex(request.build());
        }

        long endTime = System.nanoTime();
        double execTimeSec = (endTime - startTime) / 1_000_000_000.0;

        return new IndexResult(execTimeSec, totalBytes);

    }

    private List<File> listFilesRecursively(File folder) {
        List<File> result = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    result.addAll(listFilesRecursively(file));
                } else {
                    result.add(file);
                }
            }
        }
        return result;
    }
    
    public SearchResult search(ArrayList<String> terms) {
        // TO-DO get the start time
        // TO-DO perform a remote procedure call to the server by calling the gRPC client stub
        // TO-DO get the stop time and calculate the execution time
        // TO-DO return the execution time and the top 10 documents and frequencies
        long startTime = System.nanoTime();

        SearchReq.Builder reqBuilder = SearchReq.newBuilder();
        for (String term : terms) {
            if (!term.equalsIgnoreCase("AND")) {
                reqBuilder.addTerms(term);
            }
        }

        SearchRep response = stub.computeSearch(reqBuilder.build());

        ArrayList<DocPathFreqPair> allResults = new ArrayList<>();
        for (var entry : response.getSearchResultsMap().entrySet()) {
            allResults.add(new DocPathFreqPair(entry.getKey(), entry.getValue()));
        }
        int totalCount = allResults.size();
        allResults.sort((a, b) -> Long.compare(b.wordFrequency, a.wordFrequency));

        ArrayList<DocPathFreqPair> topResults = new ArrayList<>();
        int countToShow = Math.min(10, totalCount);
        for (int i = 0; i < countToShow; i++) {
            topResults.add(allResults.get(i));
        }

        long endTime = System.nanoTime();
        double execTime = (endTime - startTime) / 1_000_000_000.0;

        return new SearchResult(execTime, topResults, totalCount);

    }

    public long getInfo() {
        // TO-DO return the client ID

        return clientId;
    }

    public void disconnect() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    public void connect(String serverIP, String serverPort) {
        // TO-DO create communication channel with the gRPC Server
        // TO-DO create gRPC client stub
        // TO-DO perform a remote procedure call to the server by calling the gRPC client stub
        // TO-DO store the client ID
        channel = Grpc.newChannelBuilder(serverIP + ":" + serverPort, InsecureChannelCredentials.create())
                .build();
        stub = FileRetrievalEngineGrpc.newBlockingStub(channel);

        // Register and get client ID
        RegisterRep response = stub.register(com.google.protobuf.Empty.getDefaultInstance());
        clientId = response.getClientId();
    }
}
