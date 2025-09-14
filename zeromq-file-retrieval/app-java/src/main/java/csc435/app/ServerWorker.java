package csc435.app;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ServerWorker implements Runnable {
    private IndexStore store;
    private ZContext context;
    private static final AtomicLong clientIdCounter = new AtomicLong(1);

    public ServerWorker(IndexStore store, ZContext context) {
        this.store = store;
        this.context = context;
    }

    @Override
    public void run() {
        // TO-DO create a reply socket and connect it to the dealer
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
        // TO-DO close the reply socket
        ZMQ.Socket socket = context.createSocket(SocketType.REP);
        socket.connect("inproc://backend");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String request = socket.recvStr();
                if (request == null) break;

                if (request.startsWith("REGISTER")) {
                    long newClientId = clientIdCounter.getAndIncrement();
                    socket.send("REGISTER REPLY " + newClientId);
                    continue;
                }

                if (request.startsWith("INDEX")) {
                    // Format: INDEX|<clientId>|<documentPath>|word1:freq1|word2:freq2|...
                    String[] parts = request.split("\\|");
                    if (parts.length < 4) {
                        socket.send("ERROR: Malformed INDEX");
                        continue;
                    }

                    String documentPath = parts[2];
                    long docNum = store.putDocument(documentPath);
                    HashMap<String, Long> wordFreq = new HashMap<>();

                    for (int i = 3; i < parts.length; i++) {
                        int colonIdx = parts[i].indexOf(':');
                        if (colonIdx != -1) {
                            String word = parts[i].substring(0, colonIdx);
                            long freq = Long.parseLong(parts[i].substring(colonIdx + 1));
                            wordFreq.put(word, freq);
                        }
                    }

                    store.updateIndex(docNum, wordFreq);
                    socket.send("INDEX REPLY");
                    continue;
                }

                if (request.startsWith("SEARCH")) {
                    // Format: SEARCH|term1|term2|...
                    String[] parts = request.split("\\|");
                    if (parts.length < 2) {
                        socket.send("SEARCH REPLY:EMPTY");
                        continue;
                    }

                    // For AND queries, we need documents that contain ALL terms
                    Map<Long, Long> docFreqMap = null;

                    for (int i = 1; i < parts.length; i++) {
                        String searchTerm = parts[i];
                        if (searchTerm.length() <= 3) continue;
                        Map<Long, Long> termResults = new HashMap<>();

                        // Look for exact matches and partial matches
                        for (String indexedTerm : store.getAllTerms()) {
                            if (indexedTerm.length() <= 3) continue;
                            if (indexedTerm.equals(searchTerm)) {
                                for (DocFreqPair pair : store.lookupIndex(indexedTerm)) {
                                    termResults.put(pair.documentNumber,
                                            termResults.getOrDefault(pair.documentNumber, 0L) + pair.wordFrequency);
                                }
                            }
                        }

                        if (docFreqMap == null) {
                            // First term - initialize with all documents containing this term
                            docFreqMap = new HashMap<>(termResults);
                        } else {
                            // For AND queries, keep only documents that appear in both maps
                            Map<Long, Long> intersection = new HashMap<>();
                            for (Map.Entry<Long, Long> entry : docFreqMap.entrySet()) {
                                Long docId = entry.getKey();
                                if (termResults.containsKey(docId)) {
                                    // Sum frequencies for documents containing both terms
                                    intersection.put(docId, entry.getValue() + termResults.get(docId));
                                }
                            }
                            docFreqMap = intersection;
                        }
                    }

                    if (docFreqMap == null || docFreqMap.isEmpty()) {
                        socket.send("SEARCH REPLY:EMPTY");
                        continue;
                    }

                    List<Map.Entry<Long, Long>> sorted = docFreqMap.entrySet()
                            .stream()
                            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                            .limit(10)
                            .collect(Collectors.toList());
                    int totalMatchCount = docFreqMap.size();

                    StringBuilder reply = new StringBuilder("SEARCH REPLY:");
                    reply.append("TOTAL=").append(totalMatchCount).append("|");
                    for (Map.Entry<Long, Long> entry : sorted) {
                        String path = store.getDocument(entry.getKey());
                        reply.append(path).append("=").append(entry.getValue()).append("|");
                    }

                    // Remove trailing '|' if present
                    if (reply.charAt(reply.length() - 1) == '|') {
                        reply.setLength(reply.length() - 1);
                    }

                    socket.send(reply.toString());
                    continue;
                }

                if (request.equals("QUIT")) {
                    socket.send("BYE");
                    break;
                }

                socket.send("ERROR: Unknown request");
            } catch (Exception e) {
                socket.send("ERROR: " + e.getMessage());
            }
        }
        socket.close();
    }
}
