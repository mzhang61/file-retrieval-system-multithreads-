package csc435.app;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FileRetrievalEngineService extends FileRetrievalEngineGrpc.FileRetrievalEngineImplBase {
    private IndexStore store;
    // TO-DO keep track of the client IDs
    private AtomicInteger clientCounter = new AtomicInteger(1); // starts from 1
    
    public FileRetrievalEngineService(IndexStore store) {
        this.store = store;
    }

    @Override
    public void register(com.google.protobuf.Empty request, StreamObserver<RegisterRep> responseObserver) {
        responseObserver.onNext(doRegister());
        responseObserver.onCompleted();
    }

    @Override
    public void computeIndex(IndexReq request, StreamObserver<IndexRep> responseObserver) {
        responseObserver.onNext(doIndex(request));
        responseObserver.onCompleted();
    }

    @Override
    public void computeSearch(SearchReq request, StreamObserver<SearchRep> responseObserver) {
        responseObserver.onNext(doSearch(request));
        responseObserver.onCompleted();
    }

    private RegisterRep doRegister() {
        // TO-DO generate a client ID
        //       return the client ID as a RegisterRep reply
        int newClientId = clientCounter.getAndIncrement();
        return RegisterRep.newBuilder()
                .setClientId(newClientId)
                .build();
    }

    private IndexRep doIndex(IndexReq request) {
        // TO-DO update global index with temporary index received from the request
        // TO-DO send an OK message as the reply
        String docPath = request.getDocumentPath();
        int clientId = request.getClientId();

        // Store doc path and get a unique doc number
        long docNum = store.putDocument(docPath);

        // Convert frequencies from protobuf map to Java map
        HashMap<String, Long> freqMap = new HashMap<>();
        for (var entry : request.getWordFrequenciesMap().entrySet()) {
            freqMap.put(entry.getKey(), entry.getValue());
        }
        // Update global index
        store.updateIndex(docNum, freqMap);

        return IndexRep.newBuilder().setAck("OK").build();
    }

    private SearchRep doSearch(SearchReq request) {
        // TO-DO do lookup over the global index given the search term from the request
        // TO-DO send the results as the reply message

        List<String> terms = request.getTermsList();
        if (terms.isEmpty()) return SearchRep.newBuilder().build();

        SearchRep.Builder repBuilder = SearchRep.newBuilder();

        if (terms.size() == 1) {
            // Single-term search: just return all docs containing that term
            ArrayList<DocFreqPair> results = store.lookupIndex(terms.get(0));
            for (DocFreqPair pair : results) {
                String path = store.getDocument(pair.documentNumber);
                repBuilder.putSearchResults(path, pair.wordFrequency);
            }
        } else {
            // Multi-term search: AND semantics
            List<ArrayList<DocFreqPair>> termResults = new ArrayList<>();
            for (String term : terms) {
                termResults.add(store.lookupIndex(term));
            }

            HashMap<Long, Long> intersection = new HashMap<>();
            for (DocFreqPair pair : termResults.get(0)) {
                intersection.put(pair.documentNumber, pair.wordFrequency);
            }

            for (int i = 1; i < termResults.size(); i++) {
                HashMap<Long, Long> nextMap = new HashMap<>();
                for (DocFreqPair pair : termResults.get(i)) {
                    if (intersection.containsKey(pair.documentNumber)) {
                        long prev = intersection.get(pair.documentNumber);
                        nextMap.put(pair.documentNumber, prev + pair.wordFrequency);
                    }
                }
                intersection = nextMap;
            }

            for (Map.Entry<Long, Long> entry : intersection.entrySet()) {
                String path = store.getDocument(entry.getKey());
                repBuilder.putSearchResults(path, entry.getValue());
            }
        }
        return repBuilder.build();
    }

}
