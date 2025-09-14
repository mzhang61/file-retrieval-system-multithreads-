package csc435.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

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
    public ArrayList<DocPathFreqPair> documentFrequencies;
    public int totalMatchingDocuments;

    public SearchResult(double executionTime, ArrayList<DocPathFreqPair> documentFrequencies, int totalMatchingDocuments) {
        this.executionTime = executionTime;
        this.documentFrequencies = documentFrequencies;
        this.totalMatchingDocuments = totalMatchingDocuments;
    }
}

public class ClientProcessingEngine {
    // TO-DO keep track of the ZMQ context
    // TO-DO keep track of the request socket
    private ZContext context;
    private ZMQ.Socket socket;
    private long clientId = -1;

    public ClientProcessingEngine() { }

    public IndexResult indexFolder(String folderPath) {
        // TO-DO get the start time
        // TO-DO crawl the folder path and extrac all file paths
        // TO-DO for each file extract all words/terms and count their frequencies
        // TO-DO increment the total number of bytes read
        // TO-DO for each file prepare an INDEX REQUEST message and send to the server
        //       the document path, the client ID and the word frequencies
        // TO-DO receive for each INDEX REQUEST message an INDEX REPLY message
        // TO-DO get the stop time and calculate the execution time
        // TO-DO return the execution time and the total number of bytes read
        Instant start = Instant.now();
        AtomicLong totalBytes = new AtomicLong(0);
        Pattern wordPattern = Pattern.compile("[a-zA-Z0-9_-]+");

        try {
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        if (file.getFileName().toString().equals(".DS_Store")) return;
                        try {
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            totalBytes.addAndGet(content.getBytes(StandardCharsets.UTF_8).length);

                            Matcher matcher = wordPattern.matcher(content.toLowerCase());
                            Map<String, Long> wordFreq = new HashMap<>();
                            while (matcher.find()) {
                                String word = matcher.group();
                                if (word.length() > 3) {
                                    wordFreq.put(word, wordFreq.getOrDefault(word, 0L) + 1);
                                }
                            }


                            Path root = Paths.get(folderPath).toAbsolutePath();
                            Path relative = root.relativize(file.toAbsolutePath());
                            String documentPath = "client " + clientId + ":" + relative.toString();
                            StringBuilder msg = new StringBuilder("INDEX|" + clientId + "|" + documentPath);
                            for (Map.Entry<String, Long> entry : wordFreq.entrySet()) {
                                msg.append("|").append(entry.getKey()).append(":").append(entry.getValue());
                            }

                            socket.send(msg.toString());
                            socket.recvStr(); // Wait for ACK
                        } catch (IOException e) {
                            System.err.println("Failed to read file: " + file + " - " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        Instant end = Instant.now();
        double execTime = Duration.between(start, end).toMillis() / 1000.0;
        return new IndexResult(execTime, totalBytes.get());
    }

    public SearchResult search(ArrayList<String> terms) {
        // TO-DO get the start time
        // TO-DO prepare a SEARCH REQUEST message that includes the search terms and send it to the server
        // TO-DO receive one or more SEARCH REPLY messages with the results of the search query
        // TO-DO get the stop time and calculate the execution time
        // TO-DO return the execution time and the top 10 documents and frequencies

        Instant start = Instant.now();
        StringBuilder msg = new StringBuilder("SEARCH");
        for (String term : terms) {
            String normalized = term.toLowerCase();
            msg.append("|").append(normalized);
        }
        socket.send(msg.toString());

        String reply = socket.recvStr();
        ArrayList<DocPathFreqPair> fullResults = new ArrayList<>();

        int totalMatchCount = 0;

        if (reply != null && reply.startsWith("SEARCH REPLY:")) {
            reply = reply.substring("SEARCH REPLY:".length());
            if (!reply.equals("EMPTY")) {
                String[] parts = reply.split("\\|");
                for (String part : parts) {
                    if (part.startsWith("TOTAL=")) {
                        try {
                            totalMatchCount = Integer.parseInt(part.substring(6));
                        } catch (NumberFormatException ignored) { }
                    } else if (part.contains("=")) {
                        String[] docFreq = part.split("=", 2);
                        if (docFreq.length == 2) {
                            try {
                                String path = docFreq[0];
                                long freq = Long.parseLong(docFreq[1]);
                                fullResults.add(new DocPathFreqPair(path, freq));
                            } catch (NumberFormatException ignored) { }
                        }
                    }
                }
            }
        }

        ArrayList<DocPathFreqPair> top10Results = new ArrayList<>();
        for (int i = 0; i < Math.min(10, fullResults.size()); i++) {
            top10Results.add(fullResults.get(i));
        }

        Instant end = Instant.now();
        double execTime = Duration.between(start, end).toMillis() / 1000.0;

        return new SearchResult(execTime, top10Results, totalMatchCount);
    }

    public long getInfo() {
        // TO-DO return the client ID
        return clientId;
    }

    public void connect(String serverIP, String serverPort) {
        // TO-DO initialize the ZMQ context
        // TO-DO create the request socket and connect it to the server
        // send a REGISTER REQUEST message and receive a REGISTER reply message with the client ID
        context = new ZContext();
        socket = context.createSocket(SocketType.REQ);
        socket.connect("tcp://" + serverIP + ":" + serverPort);

        socket.send("REGISTER REQUEST");
        String reply = socket.recvStr();

        if (reply.startsWith("REGISTER REPLY")) {
            clientId = Integer.parseInt(reply.split(" ")[2]);
        }
    }

    public void disconnect() {
        // TO-DO implement disconnect from server
        // TO-DO send a QUIT message to the server
        // close the request socket and the context
        if (socket != null) {
            socket.send("QUIT");
            socket.recvStr();
            socket.close();
        }
        if (context != null) context.close();
    }
}
