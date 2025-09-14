package csc435.app;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final HashMap<String, Long> documentMap;
    private final HashMap<Long, String> reverseDocumentMap;
    private long docCounter = 0;

    private final HashMap<String, HashMap<Long, Long>> termInvertedIndex;

    // TO-DO declare two locks, one for the DocumentMap and one for the TermInvertedIndex
    private final ReentrantLock docLock = new ReentrantLock();
    private final ReentrantLock indexLock = new ReentrantLock();
    public IndexStore() {
        // TO-DO initialize the DocumentMap and TermInvertedIndex members
        documentMap = new HashMap<>();
        reverseDocumentMap = new HashMap<>();
        termInvertedIndex = new HashMap<>();
    }

    public long putDocument(String documentPath) {
        long documentNumber;
        // TO-DO assign a unique number to the document path and return the number
        // IMPORTANT! you need to make sure that only one thread at a time can access this method

        docLock.lock();
        try {
            if (!documentMap.containsKey(documentPath)) {
                docCounter++;
                documentMap.put(documentPath, docCounter);
                reverseDocumentMap.put(docCounter, documentPath);
            }
            documentNumber = documentMap.get(documentPath);
        } finally {
            docLock.unlock();
        }

        return documentNumber;
    }

    public String getDocument(long documentNumber) {
        String documentPath;
        // TO-DO retrieve the document path that has the given document number
        docLock.lock();
        try {
            documentPath = reverseDocumentMap.get(documentNumber);
        } finally {
            docLock.unlock();
        }
        return documentPath;
    }

    public void updateIndex(long documentNumber, HashMap<String, Long> wordFrequencies) {
        // TO-DO update the TermInvertedIndex with the word frequencies of the specified document
        // IMPORTANT! you need to make sure that only one thread at a time can access this method
        indexLock.lock();
        try {
            for (String term : wordFrequencies.keySet()) {
                termInvertedIndex.computeIfAbsent(term, k -> new HashMap<>())
                        .put(documentNumber, wordFrequencies.get(term));
            }
        } finally {
            indexLock.unlock();
        }
    }

    public ArrayList<DocFreqPair> lookupIndex(String term) {

        // TO-DO return the document and frequency pairs for the specified term
        ArrayList<DocFreqPair> results = new ArrayList<>();

        indexLock.lock();
        try {
            HashMap<Long, Long> docMap = termInvertedIndex.get(term);
            if (docMap != null) {
                for (var entry : docMap.entrySet()) {
                    results.add(new DocFreqPair(entry.getKey(), entry.getValue()));
                }
            }
        } finally {
            indexLock.unlock();
        }
        return results;
    }

    }
