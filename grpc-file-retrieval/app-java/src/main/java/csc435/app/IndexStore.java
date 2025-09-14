package csc435.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<String, Long> documentMap;
    private final Map<Long, String> reverseDocumentMap;
    private final Map<String, List<DocFreqPair>> invertedIndex;

    private final ReentrantLock docMapLock;
    private final ReentrantLock indexLock;

    private long nextDocumentNumber = 1;
    public IndexStore() {
        // TO-DO initialize the DocumentMap and TermInvertedIndex members
        documentMap = new HashMap<>();
        reverseDocumentMap = new HashMap<>();
        invertedIndex = new HashMap<>();

        docMapLock = new ReentrantLock();
        indexLock = new ReentrantLock();
    }

    public long putDocument(String documentPath) {
        // long documentNumber = 0;
        // TO-DO assign a unique number to the document path and return the number
        // IMPORTANT! you need to make sure that only one thread at a time can access this method
        docMapLock.lock();
        try {
            if (!documentMap.containsKey(documentPath)) {
                long docNum = nextDocumentNumber++;
                documentMap.put(documentPath, docNum);
                reverseDocumentMap.put(docNum, documentPath);
            }
            return documentMap.get(documentPath);
        } finally {
            docMapLock.unlock();
        }
    }

    public String getDocument(long documentNumber) {
        // TO-DO retrieve the document path that has the given document number
        docMapLock.lock();
        try {
            return reverseDocumentMap.getOrDefault(documentNumber, "unknown");
        } finally {
            docMapLock.unlock();
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

                invertedIndex.putIfAbsent(term, new ArrayList<>());
                invertedIndex.get(term).add(new DocFreqPair(documentNumber, freq));
            }
        } finally {
            indexLock.unlock();
        }
    }

    public ArrayList<DocFreqPair> lookupIndex(String term) {
        // TO-DO return the document and frequency pairs for the specified term
        indexLock.lock();
        try {
            return new ArrayList<>(invertedIndex.getOrDefault(term, new ArrayList<>()));
        } finally {
            indexLock.unlock();
        }
    }
}
