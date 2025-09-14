package csc435.app;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

class IndexResult {
    public double executionTime;
    public long totalBytesRead;

    public IndexResult(double executionTime, long totalBytesRead) {
        this.executionTime = executionTime;
        this.totalBytesRead = totalBytesRead;
    }
}

class DocPathFreqPair implements Serializable{
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
    public int totalCount;
    public SearchResult(double executionTime, ArrayList<DocPathFreqPair> documentFrequencies, int totalCount) {
        this.executionTime = executionTime;
        this.documentFrequencies = documentFrequencies;
        this.totalCount = totalCount;
    }
}

public class ClientProcessingEngine {
    // TO-DO keep track of the connection (socket)
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private long clientId = -1;
    public ClientProcessingEngine() { }

    public IndexResult indexFolder(String folderPath) {
        // IndexResult result = new IndexResult(0.0, 0);
        // TO-DO get the start time
        // TO-DO crawl the folder path and extrac all file paths
        // TO-DO for each file extract all words/terms and count their frequencies
        // TO-DO increment the total number of bytes read
        // TO-DO for each file prepare an INDEX REQUEST message and send to the server
        //       the document path, the client ID and the word frequencies
        // TO-DO receive for each INDEX REQUEST message an INDEX REPLY message
        // TO-DO get the stop time and calculate the execution time
        // TO-DO return the execution time and the total number of bytes read
        LocalTime start = LocalTime.now();
        Pattern pattern = Pattern.compile("[a-zA-Z0-9_-]+");
        File root = new File(folderPath);
        AtomicLong totalBytes = new AtomicLong(0);
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("Invalid folder path " + folderPath);
            return new IndexResult(0.0, 0);
        }
        ArrayDeque<File> stack = new ArrayDeque<>();
        List<File> allFiles = new ArrayList<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            File current = stack.pop();
            if (current.isDirectory()) {
                File[] children = current.listFiles();
                if (children != null) Collections.addAll(stack, children);
            } else {
                allFiles.add(current);
            }
        }
        for (File file : allFiles) {
            HashMap<String, Long> wordFreq = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalBytes.addAndGet(line.getBytes(StandardCharsets.UTF_8).length);
                    String[] tokens = line.split("[^a-zA-Z0-9_-]+");
                    for (String token : tokens) {
                        if (pattern.matcher(token).matches()) {
                            String word = token.toLowerCase();
                            wordFreq.put(word, wordFreq.getOrDefault(word, 0L) + 1);
                        }
                    }
                }

                out.writeObject("INDEX REQUEST");
                out.writeLong(clientId);
                out.writeObject(file.getPath());
                out.writeInt(wordFreq.size());
                for (Map.Entry<String, Long> entry : wordFreq.entrySet()) {
                    out.writeObject(entry.getKey());
                    out.writeLong(entry.getValue());
                }
                out.flush();

                Object reply = in.readObject();
                if (!"INDEX REPLY".equals(reply)) {
                    System.out.println("Invalid reply from server while indexing: " + file.getPath());
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error processing file: " + file.getPath());
            }
        }

        LocalTime end = LocalTime.now();
        Duration duration = Duration.between(start, end);
        return new IndexResult(duration.toNanos() / 1_000_000_000.0, totalBytes.get());

    }

    public SearchResult search(ArrayList<String> terms) {
        SearchResult result;
        // TO-DO get the start time
        // TO-DO prepare a SEARCH REQUEST message that includes the search terms and send it to the server
        // TO-DO receive one or more SEARCH REPLY messages with the results of the search query
        // TO-DO get the stop time and calculate the execution time
        // TO-DO return the execution time and the top 10 documents and frequencies
        LocalTime start = LocalTime.now();
        ArrayList<DocPathFreqPair> documentFrequencies = new ArrayList<>();
        ArrayList<DocPathFreqPair> allFrequencies = new ArrayList<>();

        int totalCount = 0;
        try {
            out.writeObject("SEARCH REQUEST");
            out.writeObject(terms);
            out.flush();

            Object reply = in.readObject();
            if (reply instanceof ArrayList<?> list) {
                for (Object obj : list) {
                    if (obj instanceof DocPathFreqPair pair) {
                        allFrequencies.add(pair);
                    }
                }
            }
            allFrequencies.sort((a, b) -> Long.compare(b.wordFrequency, a.wordFrequency));
            totalCount = allFrequencies.size();
            documentFrequencies = new ArrayList<>(allFrequencies.subList(0, Math.min(10, totalCount)));

        } catch (Exception e) {
            e.printStackTrace();
        }
        LocalTime end = LocalTime.now();
        double timeSec = Duration.between(start, end).toNanos() / 1_000_000_000.0;

        result = new SearchResult(timeSec, documentFrequencies, totalCount);
        return result;

    }

    public long getInfo() {
        // TO-DO return the client ID

        return clientId;
    }

    public void connect(String serverIP, String serverPort) {
        // TO-DO implement connect to server
        // TO-DO create a new TCP/IP socket and connect to the server
        // TO-DO send a REGISTER REQUEST message and receive a REGISTER REPLY message with the client ID
        try {
            socket = new Socket(serverIP, Integer.parseInt(serverPort));
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Second register request
            out.writeObject("REGISTER REQUEST");
            out.flush();

            // Receive register reply
            Object type = in.readObject();
            if ("REGISTER REPLY".equals(type)) {
                clientId = in.readLong();
                System.out.println("Connection successful!");
            } else {
                System.out.println("Invalid register reply from server");
            }
        } catch(Exception e) {
            System.out.println("Failed to connect: " + e.getMessage());
        }
    }

    public void disconnect() {
        // TO-DO implement disconnect from server
        // TO-DO send a QUIT message to the server
        // TO-DO close the TCP/IP socket
        try {
            if (out != null) {
                out.writeObject("QUIT");
                out.flush();
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Failed to connect: " + e.getMessage());
        }
    }
}
