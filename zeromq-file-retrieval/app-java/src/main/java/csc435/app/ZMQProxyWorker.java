package csc435.app;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZMQProxyWorker implements Runnable {
    private final ZContext context;
    private final int serverPort;

    public ZMQProxyWorker(ZContext context, int serverPort) {
        this.context = context;
        this.serverPort = serverPort;
    }
    
    @Override
    public void run() {
        // TO-DO create and bind router and dealer sockets
        // TO-DO create and start the ZMQ Proxy that will forward messages between the router and dealer sockets
        // TO-DO close the router and dealer sockets
        // TO-DO close the context
        ZMQ.Socket frontend = context.createSocket(SocketType.ROUTER);
        frontend.bind("tcp://*:" + serverPort);
        ZMQ.Socket backend = context.createSocket(SocketType.DEALER);
        backend.bind("inproc://backend");

        ZMQ.proxy(frontend, backend, null);

        frontend.close();
        backend.close();
    }
}
