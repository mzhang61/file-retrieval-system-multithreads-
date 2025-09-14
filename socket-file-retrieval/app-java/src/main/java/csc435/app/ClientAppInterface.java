package csc435.app;

import java.lang.System;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientAppInterface {
    private ClientProcessingEngine engine;

    public ClientAppInterface(ClientProcessingEngine engine) {
        this.engine = engine;
    }

    public void readCommands() {
        // TO-DO implement the read commands method
        Scanner sc = new Scanner(System.in);
        String command;
        
        while (true) {
            System.out.print("> ");
            
            // read from command line
            command = sc.nextLine().trim();

            // if the command is quit, terminate the program       
            if (command.compareTo("quit") == 0) {
                engine.disconnect();
                break;
            }

            // if the command begins with connect, connect to the given server
            if (command.length() >= 7 && command.substring(0, 7).compareTo("connect") == 0) {
                // TO-DO parse command cand call connect on the processing engine
                String[] parts = command.split("\\s+");
                if (parts.length != 3) {
                    System.out.println("Usage: connect <serverIP> <port>");
                    continue;
                }
                engine.connect(parts[1], parts[2]);
                continue;
            }

            // if the command begins with get_info, print the client ID
            if (command.length() >= 7 && command.substring(0, 8).compareTo("get_info") == 0) {
                // TO-DO parse command cand call getInfo on the processing engine
                // TO-DO print the client ID
                long id = engine.getInfo();
                System.out.println("client ID: " + id);
                continue;
            }
            
            // if the command begins with index, index the files from the specified directory
            if (command.length() >= 5 && command.substring(0, 5).compareTo("index") == 0) {
                // TO-DO parse command and call indexFolder on the processing engine
                // TO-DO print the execution time and the total number of bytes read
                String[] parts = command.split("\\s+", 2);
                if (parts.length != 2) {
                    System.out.println("Usage: index <folder_path>");
                    continue;
                }
                IndexResult result = engine.indexFolder(parts[1]);
                System.out.printf("Completed indexing %d bytes of data\n", result.totalBytesRead);
                System.out.printf("Completed indexing in %.3f seconds\n", result.executionTime);
                continue;
            }

            // if the command begins with search, search for files that matches the query
            if (command.length() >= 6 && command.substring(0, 6).compareTo("search") == 0) {
                // TO-DO parse command and call search on the processing engine
                // TO-DO print the execution time and the top 10 search results
                String[] parts = command.split("\\s+", 2);
                if (parts.length != 2) {
                    System.out.println("Usage: search <term1> [AND term2] [AND term3]");
                    continue;
                }
                String query = parts[1];
                String[] rawTerms = query.split("\\s+");
                ArrayList<String> terms = new ArrayList<>();
                for (String term : rawTerms) {
                    if (!term.equalsIgnoreCase("AND")) {
                        terms.add(term.toLowerCase());
                    }
                }
                if (terms.size() == 0 || terms.size() > 3) {
                    System.out.println("Search query must include 1 to 3 terms");
                    continue;
                }
                SearchResult result = engine.search(terms);
                System.out.printf("Search completed in %.3f seconds\n", result.executionTime);
                if (result.documentFrequencies.isEmpty()) {
                    System.out.println("No matching documents found");
                } else {
                    System.out.printf("Search result (top 10 out of %d): \n", result.totalCount);
                    for (DocPathFreqPair pair : result.documentFrequencies) {
                        System.out.printf("* %s:%d\n", pair.documentPath, pair.wordFrequency );
                    }
                }
                continue;
            }

            System.out.println("unrecognized command!");
        }

        sc.close();
    }
}
