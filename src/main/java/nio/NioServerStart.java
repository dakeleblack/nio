package nio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioServerStart {
    private static final int port = 8888;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        NioServer nioServer = new NioServer(port);
        threadPool.execute(nioServer);
    }
}
