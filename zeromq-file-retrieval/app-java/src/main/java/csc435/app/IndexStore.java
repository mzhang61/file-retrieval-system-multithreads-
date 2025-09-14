package csc435.app;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

// Data structure that stores a document number and the number of time a word/term appears in the document
class DocFreqPair {
    public long documentNumber;
    public long wordFrequency;

    public DocFreqPair(long documentNumber, long wordFrequency) {
        this.documentNumber = documentNumber;
        this.wordFrequency = wordFrequency;
    }
}

public class IndexStore {
    // TO-DO declare data structure that keeps track of the DocumentMap
    // TO-DO declare data structures that keeps track of the TermInvertedIndex
    // TO-DO declare two locks, one for the DocumentMap and one for the TermInvertedIndex
    private final Map<String, Long> documentMap = new HashMap<>();
    private final Map<Long, String> documentIdMap = new HashMap<>();
    private final Map<String, ArrayList<DocFreqPair>> termInvertedIndex = new HashMap<>();

    private final ReentrantLock docLock = new ReentrantLock();
    private final ReentrantLock indexLock = new ReentrantLock();

    private long nextDocId = 0;
    public IndexStore() {
        // TO-DO initialize the DocumentMap and TermInvertedIndex members

    }

    public long putDocument(String documentPath) {
        // TO-DO assign a unique number to the document path and return the number
        // IMPORTANT! you need to make sure that only one thread at a time can access this method

        docLock.lock();
        try {
            if (!documentMap.containsKey(documentPath)) {
                documentMap.put(documentPath, nextDocId);
                documentIdMap.put(nextDocId, documentPath);
                nextDocId++;
            }
            return documentMap.get(documentPath);
        } finally {
            docLock.unlock();
        }
    }

    public String getDocument(long documentNumber) {

        // TO-DO retrieve the document path that has the given document number
        docLock.lock();
        try {
            return documentIdMap.getOrDefault(documentNumber, "unknown");
        } finally {
            docLock.unlock();
        }
    }

    public void updateIndex(long documentNumber, HashMap<String, Long> wordFrequencies) {
        // TO-DO update the TermInvertedIndex with the word frequencies of the specified document
        // IMPORTANT! you need to make sure that only one thread at a time can access this method
        indexLock.lock();
        try {
            for (Map.Entry<String, Long> entry : wordFrequencies.entrySet()) {
                String term = entry.getKey();
                long freq = entry.getValue();

                termInvertedIndex
                        .computeIfAbsent(term, k -> new ArrayList<>())
                        .add(new DocFreqPair(documentNumber, freq));
            }
        } finally {
            indexLock.unlock();
        }
    }

    public ArrayList<DocFreqPair> lookupIndex(String term) {

        // TO-DO return the document and frequency pairs for the specified term
        indexLock.lock();
        try {
            return new ArrayList<>(termInvertedIndex.getOrDefault(term, new ArrayList<>()));
        } finally {
            indexLock.unlock();
        }
    }

    public Set<String> getAllTerms() {
        indexLock.lock();
        try {
            return new HashSet<>(termInvertedIndex.keySet());
        } finally {
            indexLock.unlock();
        }
    }
}