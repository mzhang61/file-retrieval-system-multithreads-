package csc435.app;

import java.lang.System;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientAppInterface {
    private ClientProcessingEngine engine;

    public ClientAppInterface(ClientProcessingEngine engine) {
        this.engine = engine;

        // TO-DO implement constructor
        // keep track of the connection with the client
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
                break;
            }

            // if the command begins with connect, connect to the given server
            if (command.length() >= 7 && command.substring(0, 7).compareTo("connect") == 0) {
                // TO-DO implement index operation
                // call the connect method from the server side engine
                String[] tokens = command.split(" ");
                if (tokens.length == 3) {
                    String ip = tokens[1];
                    String port = tokens[2];
                    engine.connect(ip, port);
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
                // TO-DO implement index operation
                // call the index method on the serve side engine and pass the folder to be indexed
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
                // TO-DO implement index operation
                // extract the terms and call the server side engine method to search the terms for files
                String queryPart = command.substring(6).trim();
                if (queryPart.isEmpty()) {
                    System.out.println("Usage: search <term1> [AND term2 ...]");
                    continue;
                }
                String[] queryTerms = queryPart.split("\\s+AND\\s+|\\s+");
                ArrayList<String> terms = new ArrayList<>();
                for (String term : queryTerms) {
                    if (!term.isEmpty()) {
                        terms.add(term);
                    }
                }
                if (terms.isEmpty()) {
                    System.out.println("No valid terms provided.");
                    continue;
                }
                SearchResult result = engine.search(terms);
                System.out.printf("Search completed in %.2f seconds\n", result.executionTime);
                System.out.println("Search results (top " + result.topDocuments.size() +
                        " out of " + result.totalCount + "):");
                for (DocPathFreqPair pair : result.topDocuments) {
                    System.out.printf("* %s:%d\n", pair.documentPath, pair.wordFrequency);
                }
                continue;
            }

            System.out.println("unrecognized command!");
        }

        sc.close();
    }
}
