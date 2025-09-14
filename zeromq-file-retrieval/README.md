# Distributed File Retrieval Engine (Java + ZeroMQ)

A **Client–Server** search system in Java using **ZeroMQ**.  
Clients can connect, **index** local datasets, and **search** terms (single or AND multi-term).  
The server maintains a global, thread-safe **inverted index** and scales with a pool of worker threads behind a **ROUTER/DEALER** broker.

**Server components**
- `ServerProcessingEngine`: sets up ROUTER/DEALER, launches `ServerWorker` threads, runs ZMQ proxy.
- `ServerWorker`: processes messages (`REGISTER`, `INDEX`, `SEARCH`, `QUIT`), updates/reads `IndexStore`.
- `IndexStore`: stores global index with locks.

**Client components**
- `ClientAppInterface`: terminal UI (reads commands).
- `ClientProcessingEngine`: ZeroMQ REQ socket, registers, indexes, searches.

---

## 🔌 Protocol (plain strings)

- **REGISTER**
  - Client → `REGISTER REQUEST`
  - Server → `REGISTER REPLY <clientId>`

- **INDEX**
  - Client → `INDEX|<clientId>|<documentPath>|<word1>:<freq1>|<word2>:<freq2>|...`
  - Server → `INDEX REPLY`

- **SEARCH**
  - Client → `SEARCH|term1|term2|...`
  - Server → `SEARCH REPLY:TOTAL=<N>|<path>=<freq>|<path>=<freq>|...`
    - `TOTAL` = number of matching docs (after AND intersection)
    - Response is pre-trimmed to **top-10** by frequency (desc)

- **QUIT**
  - Client → `QUIT`
  - Server → `BYE`

---

## 📦 Dependencies

- **Java 17+** (recommended)
- **JeroMQ** (pure Java ZeroMQ)
  - Maven coordinates: `org.zeromq:jeromq:0.5.4` (or latest)

### Maven (example)
```xml
<dependencies>
  <dependency>
    <groupId>org.zeromq</groupId>
    <artifactId>jeromq</artifactId>
    <version>0.5.4</version>
  </dependency>
</dependencies>
---

## ✨ Features

- **ZeroMQ messaging**
  - Client: **REQ**
  - Server: **ROUTER ⇄ DEALER** broker with worker **REP** sockets
- **Thread-safe inverted index** with `ReentrantLock`
- **CLI client** with commands:
  - `connect <ip> <port>`
  - `get_info`
  - `index <folder_path>`
  - `search <term1> [AND term2] [AND term3]`
  - `quit`
- **Benchmark driver** to simulate multiple clients and report indexing time + example queries

---

## 🧱 Architecture

### Java solution
#### How to build/compile

To build the Java solution use the following commands:
```
cd app-java
mvn compile
mvn package
```

#### How to run application

To run the Java server (after you build the project) use the following command:
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalServer <port> <number of worker threads>
> <list | quit>
```

To run the Java client (after you build the project) use the following command:
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalClient
> <connect | get_info | index | search | quit>
```

To run the Java benchmark (after you build the project) use the following command:
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalBenchmark <server IP> <server port> <number of clients> [<dataset path>]
```

#### Example (2 clients and 1 server)

**Step 1:** start the server with 2 worker threads:

Server
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalServer 12345 2
>
```

**Step 2:** start the clients and connect them to the server:

Client 1
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalClient
> connect 127.0.0.1 12345
Connection successful!
> get_info
Client ID: 1
```

Client 2
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalClient
> connect 127.0.0.1 12345
Connection successful!
> get_info
Client ID: 2
```

**Step 3:** index files from the clients:

Client 1
```
> index ../datasets/dataset1_client_server/2_clients/client_1
Completed indexing 68383239 bytes of data
Completed indexing in 2.974 seconds
```

Client 2
```
> index ../datasets/dataset1_client_server/2_clients/client_2
Completed indexing 65864138 bytes of data
Completed indexing in 2.386 seconds
```

**Step 4:** search files from the clients:

Client 1
```
> search the
Search completed in 0.4 seconds
Search results (top 10 out of 0):
> search child-like
Search completed in 2.1 seconds
Search results (top 10 out of 15):
* client 2:folder7/Document10926.txt:4
* client 1:folder3/Document10379.txt:3
* client 2:folder6/Document10866.txt:2
* client 2:folder8/Document1108.txt:1
* client 2:folder7/folderD/Document11050.txt:1
* client 2:folder6/Document10848.txt:1
* client 2:folder6/Document1082.txt:1
* client 1:folder4/Document10681.txt:1
* client 1:folder4/Document10669.txt:1
* client 1:folder3/Document10387.txt:1
> search distortion AND adaptation
Search completed in 3.27 seconds
Search results (top 10 out of 4):
* client 2:folder7/folderC/Document10998.txt:6
* client 1:folder4/Document10516.txt:3
* client 2:folder8/Document11159.txt:2
* client 2:folder8/Document11157.txt:2
>
```

Client 2
```
> search vortex
Search completed in 2.8 seconds
Search results (top 10 out of 27):
* client 2:folder5/folderB/Document10706.txt:6
* client 2:folder5/folderB/Document10705.txt:4
* client 2:folder7/Document1091.txt:3
* client 1:folder4/Document10681.txt:3
* client 2:folder6/Document1082.txt:2
* client 1:folder4/Document1051.txt:2
* client 1:folder3/folderA/Document10422.txt:2
* client 1:folder2/Document1033.txt:2
* client 2:folder8/Document11159.txt:1
* client 2:folder8/Document11154.txt:1
> search moon AND vortex
Search completed in 3.8 seconds
Search results (top 10 out of 19):
* client 2:folder5/folderB/Document10706.txt:26
* client 1:folder4/Document10681.txt:19
* client 1:folder3/Document1043.txt:19
* client 1:folder4/Document10600.txt:17
* client 2:folder8/Document11154.txt:13
* client 1:folder3/folderA/Document10422.txt:6
* client 1:folder3/Document10379.txt:6
* client 1:folder3/folderA/Document10421.txt:6
* client 2:folder5/folderB/Document10705.txt:5
* client 1:folder4/Document1033.txt:5
>
```

**Step 5:** close and disconnect the clients:

Client 1
```
> quit
```

Client 2
```
> quit
```

**Step 6:** close the server:

Server
```
> quit
```

#### Example (benchmark with 2 clients and 1 server)

**Step 1:** start the server with 2 worker threads:

Server
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalServer 12345 2
>
```

**Step 2:** start the benchmark:

Benchmark
```
java -cp target/app-java-1.0-SNAPSHOT-jar-with-dependencies.jar csc435.app.FileRetrievalBenchmark 127.0.0.1 12345 2 ../datasets/dataset1_client_server/2_clients/client_1 ../datasets/dataset1_client_server/2_clients/client_2
Completed indexing 134247377 bytes of data
Completed indexing in 6.015 seconds
Searching search the
Search completed in 0.4 seconds
Search results (top 10 out of 0):
Searching search child-like
Search completed in 2.1 seconds
Search results (top 10 out of 15):
* client 2:folder7/Document10926.txt:4
* client 1:folder3/Document10379.txt:3
* client 2:folder6/Document10866.txt:2
* client 2:folder8/Document1108.txt:1
* client 2:folder7/folderD/Document11050.txt:1
* client 2:folder6/Document10848.txt:1
* client 2:folder6/Document1082.txt:1
* client 1:folder4/Document10681.txt:1
* client 1:folder4/Document10669.txt:1
* client 1:folder3/Document10387.txt:1
Searching search vortex
Search completed in 2.8 seconds
Search results (top 10 out of 27):
* client 2:folder5/folderB/Document10706.txt:6
* client 2:folder5/folderB/Document10705.txt:4
* client 2:folder7/Document1091.txt:3
* client 1:folder4/Document10681.txt:3
* client 2:folder6/Document1082.txt:2
* client 1:folder4/Document1051.txt:2
* client 1:folder3/folderA/Document10422.txt:2
* client 1:folder2/Document1033.txt:2
* client 2:folder8/Document11159.txt:1
* client 2:folder8/Document11154.txt:1
Searching search moon AND vortex
Search completed in 3.8 seconds
Search results (top 10 out of 19):
* client 2:folder5/folderB/Document10706.txt:26
* client 1:folder4/Document10681.txt:19
* client 1:folder3/Document1043.txt:19
* client 1:folder4/Document10600.txt:17
* client 2:folder8/Document11154.txt:13
* client 1:folder3/folderA/Document10422.txt:6
* client 1:folder3/Document10379.txt:6
* client 1:folder3/folderA/Document10421.txt:6
* client 2:folder5/folderB/Document10705.txt:5
* client 1:folder4/Document1033.txt:5
Searching search distortion AND adaptation
Search completed in 3.27 seconds
Search results (top 10 out of 4):
* client 2:folder7/folderC/Document10998.txt:6
* client 1:folder4/Document10516.txt:3
* client 2:folder8/Document11159.txt:2
* client 2:folder8/Document11157.txt:2
```

**Step 3:** close the server:

Server
```
> quit
```

// hint: due to poor program performance, may need to restart server when testing.
