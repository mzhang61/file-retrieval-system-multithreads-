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
                System.out.println("Exiting client...");
                engine.disconnect();
                break;
            }

            // if the command begins with connect, connect to the given server
            if (command.length() >= 7 && command.substring(0, 7).compareTo("connect") == 0) {
                // TO-DO parse command cand call connect on the processing engine
                String[] tokens = command.split("\\s+");
                if (tokens.length == 3) {
                    engine.connect(tokens[1], tokens[2]);
                    System.out.println("Connection successful!");
                } else {
                    System.out.println("Usage: connect <ip> <port>");
                }
                continue;
            }

            // if the command begins with get_info, print the client ID
            if (command.length() >= 7 && command.substring(0, 8).compareTo("get_info") == 0) {
                // TO-DO parse command cand call getInfo on the processing engine
                // TO-DO print the client ID
                long clientId = engine.getInfo();
                System.out.println("Client ID: " + clientId);
                continue;
            }
            
            // if the command begins with index, index the files from the specified directory
            if (command.length() >= 5 && command.substring(0, 5).compareTo("index") == 0) {
                // TO-DO parse command and call indexFolder on the processing engine
                // TO-DO print the execution time and the total number of bytes read
                String folderPath = command.substring(5).trim();
                if (folderPath.isEmpty()) {
                    System.out.println("Usage: index <folder_path>");
                    continue;
                }
                IndexResult result = engine.indexFolder(folderPath);
                System.out.println("Completed indexing " + result.totalBytesRead + " bytes of data");
                System.out.printf("Completed indexing in %.3f seconds\n", result.executionTime);
                continue;
            }

            // if the command begins with search, search for files that matches the query
            if (command.length() >= 6 && command.substring(0, 6).compareTo("search") == 0) {
                // TO-DO parse command and call search on the processing engine
                // TO-DO print the execution time and the top 10 search results
                String[] tokens = command.split("\\s+");
                ArrayList<String> terms = new ArrayList<>();
                for (String t : tokens) {
                    if (!t.equalsIgnoreCase("AND") && !t.equalsIgnoreCase("search")) {
                        terms.add(t.toLowerCase());
                    }
                }
                if (terms.isEmpty()) {
                    System.out.println("Usage: search <term1> [term2] [...]");
                    continue;
                }
                SearchResult result = engine.search(terms);
                System.out.printf("Search completed in %.2f seconds\n", result.executionTime);
                System.out.printf("Search results (top 10 out of %d):\n",
                         result.totalMatchingDocuments);
                for (DocPathFreqPair doc : result.documentFrequencies) {
                    System.out.printf("* %s:%d\n", doc.documentPath, doc.wordFrequency);
                }
                continue;
            }
            System.out.println("unrecognized command!");
        }
        sc.close();
    }
}
