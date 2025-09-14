# Distributed File Retrieval Engine

This project implements a **distributed, multithreaded file retrieval system** in Java.  
It is designed as a **Client/Server architecture** where clients can connect to a server, index datasets, and perform search queries across multiple files.

---

## 📌 Features

- **Client/Server Communication**  
  - Built with **TCP sockets** (`java.net.Socket`, `ServerSocket`)  
  - Custom protocol with messages like:
    - `REGISTER REQUEST / REPLY`
    - `INDEX REQUEST / REPLY`
    - `SEARCH REQUEST / REPLY`
    - `QUIT`

- **Multithreaded Server**  
  - The server uses a **Dispatcher** to accept incoming client connections.  
  - Each client is handled by a dedicated **ServerWorker thread**.  
  - Thread safety is ensured using **ReentrantLock** in `IndexStore`.

- **Indexing**  
  - Clients can index all text files inside a folder.  
  - Each document is tokenized into words/terms.  
  - Terms are stored in an **inverted index** (term → documents & frequencies).  
  - The server maintains a global index shared across clients.

- **Search**  
  - Clients can query 1–3 terms (`search term1 AND term2`).  
  - Server retrieves all documents containing those terms.  
  - Results are aggregated, sorted by frequency, and the **Top-10 matches** are returned.

- **Benchmarking**  
  - `FileRetrievalBenchmark` simulates multiple clients indexing in parallel.  
  - Reports:
    - Total bytes processed
    - Execution time
    - Throughput (MB/s)
  - Runs example queries (`the`, `child-like`, `moon AND vortex`, etc.)

---

## ⚙️ Architecture

### System Level
- **Client**
  - Connects to the server
  - Sends indexing and search requests
  - Displays results in the terminal
- **Server**
  - Accepts multiple clients (via Dispatcher + Worker threads)
  - Manages a global inverted index
  - Returns search results

### Concurrency
- Shared data (`DocumentMap`, `TermInvertedIndex`) is protected with:
  - `ReentrantLock` for document registration
  - `ReentrantLock` for updating the inverted index

### Network
- **TCP/IP sockets** guarantee reliable, ordered delivery of messages.  
- All messages are serialized/deserialized via `ObjectInputStream` and `ObjectOutputStream`.

---

## 🖥️ Usage

**Step 1:** start the server:

Server

```
java -cp target/app-java-1.0-SNAPSHOT.jar csc435.app.FileRetrievalServer 12345
>
```

**Step 2:**
Client 1
java -cp target/app-java-1.0-SNAPSHOT.jar csc435.app.FileRetrievalClient
> connect 127.0.0.1 12345

Client 2
java -cp target/app-java-1.0-SNAPSHOT.jar csc435.app.FileRetrievalClient
connect 127.0.0.1 12345
get_info

> index ../datasets/dataset1_client_server/2_clients/client_1
Completed indexing 68383239 bytes of data
Completed indexing in 2.974 seconds

> search the

Example (benchmark with 2 clients and 1 server)

java -cp target/app-java-1.0-SNAPSHOT.jar csc435.app.FileRetrievalServer 12345

Start benchmark
java -cp target/app-java-1.0-SNAPSHOT.jar csc435.app.FileRetrievalBenchmark 127.0.0.1 12345 2 ../datasets/dataset1_client_server/2_clients/client_1 ../datasets/dataset1_client_server/2_clients/client_2
