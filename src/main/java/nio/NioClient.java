package nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioClient {
    private static final Logger logger = LoggerFactory.getLogger(NioClient.class);
    //分配buffer
    private ByteBuffer readBuf = ByteBuffer.allocate(1024);

    private ByteBuffer writeBuf = ByteBuffer.allocate(1024);

    private String host;

    private int port;

    public NioClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 启动NioClient，向服务端发送消息，并接收服务端返回的响应
     */
    public void start() {
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            //设置该SocketChannel为非阻塞模式，没有读到数据会直接返回继续下一次
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));
            // 判断是否已经连接成功，只有连接成功了才发送数据
            if (socketChannel.finishConnect()) {
                int i = 0;
                while (true) {
                    String msg = "hello,I'm the " + i++ + " msg from NioClient";
                    writeBuf.clear();
                    writeBuf.put(msg.getBytes());
                    //buffer切换为读模式
                    writeBuf.flip();
                    while (writeBuf.hasRemaining()) {
                        // 发送数据到服务端
                        socketChannel.write(writeBuf);
                    }
                    writeBuf.clear();
                    Thread.sleep(2000);

                    //TODO 接收服务端返回的响应
                    int count = socketChannel.read(readBuf);
                    if (count == -1) {
                        continue;
                    }
                    byte[] bytes = new byte[count];
                    //buffer切换为读模式
                    readBuf.flip();
                    readBuf.get(bytes);
                    logger.info("receive NioServer msg:{}", new String(bytes, StandardCharsets.UTF_8));
                    readBuf.clear();
                }
            }
        } catch (Exception e) {
            logger.error("send or receive msg from NioServer fail:", e);
        } finally {
            if (socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    logger.error("close socketChannel fail:", e);
                }
            }
        }
    }
}
