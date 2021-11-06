package nio.test2;

public class NioClientStart {
    private static final String host = "127.0.0.1";

    private static final int port = 8888;

    public static void main(String[] args) {
        NioClient nioClient = new NioClient(host, port);
        nioClient.start();
    }
}
