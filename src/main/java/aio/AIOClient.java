package aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author bitao
 * @date 2021/11/6 下午2:10
 */

public class AIOClient {
    private static final Logger logger = LoggerFactory.getLogger(AIOClient.class);

    public static void main(String... args) throws Exception {
        //实例化客户端，并连接至服务端
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();
        //connect得到一个Future对象，get得到连接建立后的客户端Channel，get是阻塞的
        socketChannel.connect(new InetSocketAddress("localhost", 9000)).get();
        logger.info("客户端连接服务端成功。。。");
        //异步向服务端发送数据
        socketChannel.write(ByteBuffer.wrap("HelloServer".getBytes()));
        ByteBuffer buffer = ByteBuffer.allocate(512);
        //异步读取服务端发送的消息，get()方法会阻塞等待
        Integer len = socketChannel.read(buffer).get();
        if (len != -1) {
            logger.info("客户端收到信息：" + new String(buffer.array(), 0, len));
        }
    }
}
